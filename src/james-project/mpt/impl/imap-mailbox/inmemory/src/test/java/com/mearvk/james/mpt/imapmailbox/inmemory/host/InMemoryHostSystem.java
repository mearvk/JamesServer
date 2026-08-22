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


package com.mearvk.james.mpt.imapmailbox.inmemory.host;

import com.mearvk.james.core.quota.QuotaCountLimit;
import com.mearvk.james.core.quota.QuotaSizeLimit;
import com.mearvk.james.imap.api.process.ImapProcessor;
import com.mearvk.james.imap.encode.main.DefaultImapEncoderFactory;
import com.mearvk.james.imap.main.DefaultImapDecoderFactory;
import com.mearvk.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.apache.james.mailbox.inmemory.quota.InMemoryPerUserMaxQuotaManager;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.StoreSubscriptionManager;
import com.mearvk.james.metrics.logger.DefaultMetricFactory;
import com.mearvk.james.mpt.api.ImapFeatures;
import com.mearvk.james.mpt.api.ImapFeatures.Feature;
import com.mearvk.james.mpt.host.JamesImapHostSystem;

public class InMemoryHostSystem extends JamesImapHostSystem {

    private static final ImapFeatures SUPPORTED_FEATURES = ImapFeatures.of(Feature.NAMESPACE_SUPPORT,
        Feature.MOVE_SUPPORT,
        Feature.USER_FLAGS_SUPPORT,
        Feature.QUOTA_SUPPORT,
        Feature.ANNOTATION_SUPPORT,
        Feature.MOD_SEQ_SEARCH);

    private StoreMailboxManager mailboxManager;
    private InMemoryPerUserMaxQuotaManager perUserMaxQuotaManager;
    
    @Override
    public void beforeTest() throws Exception {
        super.beforeTest();
        InMemoryIntegrationResources resources = InMemoryIntegrationResources.builder()
            .authenticator(authenticator)
            .authorizator(authorizator)
            .inVmEventBus()
            .defaultAnnotationLimits()
            .defaultMessageParser()
            .scanningSearchIndex()
            .noPreDeletionHooks()
            .storeQuotaManager()
            .build();
        this.mailboxManager = resources.getMailboxManager();
        this.perUserMaxQuotaManager = resources.getMaxQuotaManager();

        ImapProcessor defaultImapProcessorFactory = DefaultImapProcessorFactory.createDefaultProcessor(mailboxManager,  mailboxManager.getEventBus(), new StoreSubscriptionManager(mailboxManager.getMapperFactory(),
                mailboxManager.getMapperFactory(),
                mailboxManager.getEventBus()),
            mailboxManager.getQuotaComponents().getQuotaManager(), mailboxManager.getQuotaComponents().getQuotaRootResolver(), new DefaultMetricFactory());

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
        perUserMaxQuotaManager.setGlobalMaxMessage(maxMessageQuota);
        perUserMaxQuotaManager.setGlobalMaxStorage(maxStorageQuota);
    }

    @Override
    protected void await() {

    }
}
