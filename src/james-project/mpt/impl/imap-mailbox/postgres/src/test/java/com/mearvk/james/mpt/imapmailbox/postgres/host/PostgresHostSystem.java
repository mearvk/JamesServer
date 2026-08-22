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

package com.mearvk.james.mpt.imapmailbox.postgres.host;

import java.time.Clock;
import java.time.Instant;

import com.mearvk.james.backends.postgres.PostgresExtension;
import com.mearvk.james.backends.postgres.quota.PostgresQuotaCurrentValueDAO;
import com.mearvk.james.backends.postgres.quota.PostgresQuotaLimitDAO;
import com.mearvk.james.blob.api.BlobId;
import com.mearvk.james.blob.api.BucketName;
import com.mearvk.james.blob.api.PlainBlobId;
import com.mearvk.james.blob.memory.MemoryBlobStoreDAO;
import com.mearvk.james.core.quota.QuotaCountLimit;
import com.mearvk.james.core.quota.QuotaSizeLimit;
import com.mearvk.james.events.EventBusTestFixture;
import com.mearvk.james.events.InVMEventBus;
import com.mearvk.james.events.MemoryEventDeadLetters;
import com.mearvk.james.events.delivery.InVmEventDelivery;
import com.mearvk.james.imap.api.process.ImapProcessor;
import com.mearvk.james.imap.encode.main.DefaultImapEncoderFactory;
import com.mearvk.james.imap.main.DefaultImapDecoderFactory;
import com.mearvk.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.mailbox.AttachmentContentLoader;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.StringBackedAttachmentIdFactory;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.postgres.PostgresMailboxManager;
import org.apache.james.mailbox.postgres.PostgresMailboxSessionMapperFactory;
import org.apache.james.mailbox.postgres.PostgresMessageId;
import org.apache.james.mailbox.postgres.quota.PostgresCurrentQuotaManager;
import org.apache.james.mailbox.postgres.quota.PostgresPerUserMaxQuotaManager;
import org.apache.james.mailbox.quota.CurrentQuotaManager;
import org.apache.james.mailbox.quota.QuotaChangeNotifier;
import org.apache.james.mailbox.store.PreDeletionHooks;
import org.apache.james.mailbox.store.SessionProviderImpl;
import org.apache.james.mailbox.store.StoreMailboxAnnotationManager;
import org.apache.james.mailbox.store.StoreRightManager;
import org.apache.james.mailbox.store.StoreSubscriptionManager;
import org.apache.james.mailbox.store.event.MailboxAnnotationListener;
import org.apache.james.mailbox.store.extractor.DefaultTextExtractor;
import org.apache.james.mailbox.store.mail.AttachmentIdAssignationStrategy;
import org.apache.james.mailbox.store.mail.NaiveThreadIdGuessingAlgorithm;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.apache.james.mailbox.store.mail.model.impl.MessageParserImpl;
import org.apache.james.mailbox.store.quota.DefaultUserQuotaRootResolver;
import org.apache.james.mailbox.store.quota.ListeningCurrentQuotaUpdater;
import org.apache.james.mailbox.store.quota.QuotaComponents;
import org.apache.james.mailbox.store.quota.StoreQuotaManager;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.mailbox.store.search.SimpleMessageSearchIndex;
import com.mearvk.james.metrics.logger.DefaultMetricFactory;
import com.mearvk.james.metrics.tests.RecordingMetricFactory;
import com.mearvk.james.mpt.api.ImapFeatures;
import com.mearvk.james.mpt.api.ImapFeatures.Feature;
import com.mearvk.james.mpt.host.JamesImapHostSystem;
import com.mearvk.james.server.blob.deduplication.DeDuplicationBlobStore;
import com.mearvk.james.utils.UpdatableTickingClock;

import com.google.common.base.Preconditions;

public class PostgresHostSystem extends JamesImapHostSystem {

    private static final ImapFeatures SUPPORTED_FEATURES = ImapFeatures.of(Feature.NAMESPACE_SUPPORT,
        Feature.USER_FLAGS_SUPPORT,
        Feature.ANNOTATION_SUPPORT,
        Feature.QUOTA_SUPPORT,
        Feature.MOVE_SUPPORT,
        Feature.MOD_SEQ_SEARCH);


    static PostgresHostSystem build(PostgresExtension postgresExtension) {
        return new PostgresHostSystem(postgresExtension);
    }

    private PostgresPerUserMaxQuotaManager maxQuotaManager;
    private PostgresMailboxManager mailboxManager;
    private final PostgresExtension postgresExtension;

    public PostgresHostSystem(PostgresExtension postgresExtension) {
        this.postgresExtension = postgresExtension;
    }

    public void beforeAll() {
        Preconditions.checkNotNull(postgresExtension.getConnectionFactory());
    }

    @Override
    public void beforeTest() throws Exception {
        super.beforeTest();

        BlobId.Factory blobIdFactory = new PlainBlobId.Factory();
        DeDuplicationBlobStore blobStore = new DeDuplicationBlobStore(new MemoryBlobStoreDAO(), BucketName.DEFAULT, blobIdFactory);

        PostgresMailboxSessionMapperFactory mapperFactory = new PostgresMailboxSessionMapperFactory(postgresExtension.getExecutorFactory(), Clock.systemUTC(), blobStore, blobIdFactory,
            postgresExtension.getPostgresConfiguration(),
            new AttachmentIdAssignationStrategy.Default(new StringBackedAttachmentIdFactory()));

        MailboxACLResolver aclResolver = new UnionMailboxACLResolver();
        MessageParser messageParser = new MessageParserImpl();


        InVMEventBus eventBus = new InVMEventBus(new InVmEventDelivery(new RecordingMetricFactory()), EventBusTestFixture.RETRY_BACKOFF_CONFIGURATION, new MemoryEventDeadLetters());
        StoreRightManager storeRightManager = new StoreRightManager(mapperFactory, aclResolver, eventBus);
        StoreMailboxAnnotationManager annotationManager = new StoreMailboxAnnotationManager(mapperFactory, storeRightManager);
        SessionProviderImpl sessionProvider = new SessionProviderImpl(authenticator, authorizator);
        DefaultUserQuotaRootResolver quotaRootResolver = new DefaultUserQuotaRootResolver(sessionProvider, mapperFactory);
        CurrentQuotaManager currentQuotaManager = new PostgresCurrentQuotaManager(new PostgresQuotaCurrentValueDAO(postgresExtension.getDefaultPostgresExecutor()));
        maxQuotaManager = new PostgresPerUserMaxQuotaManager(new PostgresQuotaLimitDAO(postgresExtension.getDefaultPostgresExecutor()), QuotaChangeNotifier.NOOP);
        StoreQuotaManager storeQuotaManager = new StoreQuotaManager(currentQuotaManager, maxQuotaManager);
        ListeningCurrentQuotaUpdater quotaUpdater = new ListeningCurrentQuotaUpdater(currentQuotaManager, quotaRootResolver, eventBus, storeQuotaManager);
        QuotaComponents quotaComponents = new QuotaComponents(maxQuotaManager, storeQuotaManager, quotaRootResolver);
        AttachmentContentLoader attachmentContentLoader = null;
        MessageSearchIndex index = new SimpleMessageSearchIndex(mapperFactory, mapperFactory, new DefaultTextExtractor(), attachmentContentLoader);

        mailboxManager = new PostgresMailboxManager(mapperFactory, sessionProvider, messageParser,
            new PostgresMessageId.Factory(),
            eventBus, annotationManager, storeRightManager, quotaComponents, index, new NaiveThreadIdGuessingAlgorithm(),
            PreDeletionHooks.NO_PRE_DELETION_HOOK, new UpdatableTickingClock(Instant.now()));

        eventBus.register(quotaUpdater);
        eventBus.register(new MailboxAnnotationListener(mapperFactory, sessionProvider));

        SubscriptionManager subscriptionManager = new StoreSubscriptionManager(mapperFactory, mapperFactory, eventBus);

        ImapProcessor defaultImapProcessorFactory =
            DefaultImapProcessorFactory.createDefaultProcessor(
                mailboxManager,
                eventBus,
                subscriptionManager,
                storeQuotaManager,
                quotaRootResolver,
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
        maxQuotaManager.setGlobalMaxMessage(maxMessageQuota);
        maxQuotaManager.setGlobalMaxStorage(maxStorageQuota);
    }

    @Override
    protected void await() {

    }
}
