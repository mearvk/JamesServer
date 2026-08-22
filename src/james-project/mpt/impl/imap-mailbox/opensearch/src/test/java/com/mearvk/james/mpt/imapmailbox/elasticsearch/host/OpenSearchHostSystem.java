/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package com.mearvk.james.mpt.imapmailbox.elasticsearch.host;

import java.io.IOException;
import java.time.ZoneId;

import org.apache.commons.lang3.NotImplementedException;
import com.mearvk.james.backends.opensearch.DockerOpenSearch;
import com.mearvk.james.backends.opensearch.DockerOpenSearchSingleton;
import com.mearvk.james.backends.opensearch.OpenSearchConfiguration;
import com.mearvk.james.backends.opensearch.OpenSearchIndexer;
import com.mearvk.james.backends.opensearch.ReactorOpenSearchClient;
import com.mearvk.james.core.quota.QuotaCountLimit;
import com.mearvk.james.core.quota.QuotaSizeLimit;
import com.mearvk.james.imap.api.process.ImapProcessor;
import com.mearvk.james.imap.encode.main.DefaultImapEncoderFactory;
import com.mearvk.james.imap.main.DefaultImapDecoderFactory;
import com.mearvk.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.inmemory.InMemoryMessageId;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.apache.james.mailbox.opensearch.IndexAttachments;
import org.apache.james.mailbox.opensearch.IndexHeaders;
import org.apache.james.mailbox.opensearch.MailboxIdRoutingKeyFactory;
import org.apache.james.mailbox.opensearch.MailboxIndexCreationUtil;
import org.apache.james.mailbox.opensearch.MailboxOpenSearchConstants;
import org.apache.james.mailbox.opensearch.OpenSearchMailboxConfiguration;
import org.apache.james.mailbox.opensearch.events.OpenSearchListeningMessageSearchIndex;
import org.apache.james.mailbox.opensearch.json.MessageToOpenSearchJson;
import org.apache.james.mailbox.opensearch.query.DefaultCriterionConverter;
import org.apache.james.mailbox.opensearch.query.QueryConverter;
import org.apache.james.mailbox.opensearch.search.OpenSearchSearcher;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.StoreSubscriptionManager;
import org.apache.james.mailbox.store.extractor.DefaultTextExtractor;
import org.apache.james.mailbox.store.quota.NoQuotaManager;
import com.mearvk.james.metrics.logger.DefaultMetricFactory;
import com.mearvk.james.metrics.tests.RecordingMetricFactory;
import com.mearvk.james.mpt.api.ImapFeatures;
import com.mearvk.james.mpt.api.ImapFeatures.Feature;
import com.mearvk.james.mpt.host.JamesImapHostSystem;

import com.google.common.collect.ImmutableSet;

public class OpenSearchHostSystem extends JamesImapHostSystem {

    private static final ImapFeatures SUPPORTED_FEATURES = ImapFeatures.of(Feature.NAMESPACE_SUPPORT,
        Feature.MOD_SEQ_SEARCH);

    private DockerOpenSearch dockerOpenSearch;
    private StoreMailboxManager mailboxManager;
    private ReactorOpenSearchClient client;

    @Override
    public void beforeTest() throws Exception {
        super.beforeTest();
        this.dockerOpenSearch = DockerOpenSearchSingleton.INSTANCE;
        dockerOpenSearch.start();
        initFields();
    }

    @Override
    public void afterTest() throws IOException {
        client.close();
        dockerOpenSearch.cleanUpData();
    }

    private void initFields() throws Exception {
        client = MailboxIndexCreationUtil.prepareDefaultClient(
            dockerOpenSearch.clientProvider().get(),
            OpenSearchConfiguration.builder()
                .addHost(dockerOpenSearch.getHttpHost())
                .build());

        InMemoryMessageId.Factory messageIdFactory = new InMemoryMessageId.Factory();
        MailboxIdRoutingKeyFactory routingKeyFactory = new MailboxIdRoutingKeyFactory();

        InMemoryIntegrationResources resources = InMemoryIntegrationResources.builder()
            .authenticator(authenticator)
            .authorizator(authorizator)
            .inVmEventBus()
            .defaultAnnotationLimits()
            .defaultMessageParser()
            .listeningSearchIndex(preInstanciationStage -> new OpenSearchListeningMessageSearchIndex(
                preInstanciationStage.getMapperFactory(),
                ImmutableSet.of(),
                new OpenSearchIndexer(client,
                    MailboxOpenSearchConstants.DEFAULT_MAILBOX_WRITE_ALIAS),
                new OpenSearchSearcher(client, new QueryConverter(new DefaultCriterionConverter()), OpenSearchSearcher.DEFAULT_SEARCH_SIZE,
                    MailboxOpenSearchConstants.DEFAULT_MAILBOX_READ_ALIAS, routingKeyFactory),
                new MessageToOpenSearchJson(new DefaultTextExtractor(), ZoneId.of("Europe/Paris"), IndexAttachments.YES, IndexHeaders.YES),
                preInstanciationStage.getSessionProvider(), routingKeyFactory, messageIdFactory,
                OpenSearchMailboxConfiguration.builder().build(), new RecordingMetricFactory(),
                ImmutableSet.of()))
            .noPreDeletionHooks()
            .storeQuotaManager()
            .build();

        mailboxManager = resources.getMailboxManager();

        ImapProcessor defaultImapProcessorFactory =
            DefaultImapProcessorFactory.createDefaultProcessor(mailboxManager,
                resources.getMailboxManager().getEventBus(),
                new StoreSubscriptionManager(mailboxManager.getMapperFactory(), mailboxManager.getMapperFactory(), mailboxManager.getEventBus()),
                new NoQuotaManager(),
                resources.getDefaultUserQuotaRootResolver(),
                new DefaultMetricFactory());
        configure(new DefaultImapDecoderFactory().buildImapDecoder(),
            new DefaultImapEncoderFactory().buildImapEncoder(),
            defaultImapProcessorFactory);
    }

    @Override
    protected MailboxManager getMailboxManager() {
        return mailboxManager;
    }

    @Override
    public boolean supports(Feature... features) {
        return SUPPORTED_FEATURES.supports(features);
    }

    @Override
    public void setQuotaLimits(QuotaCountLimit maxMessageQuota, QuotaSizeLimit maxStorageQuota) {
        throw new NotImplementedException("not implemented");
    }

    @Override
    protected void await() {
        dockerOpenSearch.flushIndices();
    }
}