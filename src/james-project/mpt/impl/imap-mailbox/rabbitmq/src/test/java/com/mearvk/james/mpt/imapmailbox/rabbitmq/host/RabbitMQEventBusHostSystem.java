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


package com.mearvk.james.mpt.imapmailbox.rabbitmq.host;

import static com.mearvk.james.events.NamingStrategy.MAILBOX_EVENT_NAMING_STRATEGY;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.mearvk.james.backends.rabbitmq.DockerRabbitMQ;
import com.mearvk.james.backends.rabbitmq.ReactorRabbitMQChannelPool;
import com.mearvk.james.backends.rabbitmq.SimpleConnectionPool;
import com.mearvk.james.core.quota.QuotaCountLimit;
import com.mearvk.james.core.quota.QuotaSizeLimit;
import com.mearvk.james.event.json.MailboxEventSerializer;
import com.mearvk.james.events.EventBusId;
import com.mearvk.james.events.MemoryEventDeadLetters;
import com.mearvk.james.events.RabbitMQEventBus;
import com.mearvk.james.events.RetryBackoffConfiguration;
import com.mearvk.james.events.RoutingKeyConverter;
import com.mearvk.james.imap.api.process.ImapProcessor;
import com.mearvk.james.imap.encode.main.DefaultImapEncoderFactory;
import com.mearvk.james.imap.main.DefaultImapDecoderFactory;
import com.mearvk.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.events.MailboxIdRegistrationKey;
import org.apache.james.mailbox.inmemory.InMemoryId;
import org.apache.james.mailbox.inmemory.InMemoryMessageId;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.apache.james.mailbox.store.StoreSubscriptionManager;
import org.apache.james.mailbox.store.quota.DefaultUserQuotaRootResolver;
import com.mearvk.james.metrics.api.NoopGaugeRegistry;
import com.mearvk.james.metrics.logger.DefaultMetricFactory;
import com.mearvk.james.metrics.tests.RecordingMetricFactory;
import com.mearvk.james.mpt.api.ImapFeatures;
import com.mearvk.james.mpt.api.ImapFeatures.Feature;
import com.mearvk.james.mpt.host.JamesImapHostSystem;

import com.google.common.collect.ImmutableSet;

public class RabbitMQEventBusHostSystem extends JamesImapHostSystem {
    private static final ImapFeatures SUPPORTED_FEATURES = ImapFeatures.of(Feature.NAMESPACE_SUPPORT,
        Feature.MOVE_SUPPORT,
        Feature.USER_FLAGS_SUPPORT,
        Feature.QUOTA_SUPPORT,
        Feature.ANNOTATION_SUPPORT,
        Feature.MOD_SEQ_SEARCH);

    private final DockerRabbitMQ dockerRabbitMQ;
    private RabbitMQEventBus eventBus;
    private SimpleConnectionPool connectionPool;
    private InMemoryIntegrationResources resources;
    private ReactorRabbitMQChannelPool reactorRabbitMQChannelPool;

    RabbitMQEventBusHostSystem(DockerRabbitMQ dockerRabbitMQ) {
        this.dockerRabbitMQ = dockerRabbitMQ;
    }

    @Override
    public void beforeTest() throws Exception {
        super.beforeTest();

        connectionPool = new SimpleConnectionPool(dockerRabbitMQ.createRabbitConnectionFactory(), SimpleConnectionPool.Configuration.builder()
                .retries(2)
                .initialDelay(Duration.ofMillis(5)));
        reactorRabbitMQChannelPool = new ReactorRabbitMQChannelPool(connectionPool.getResilientConnection(),
            ReactorRabbitMQChannelPool.Configuration.DEFAULT,
            new DefaultMetricFactory(), new NoopGaugeRegistry());
        reactorRabbitMQChannelPool.start();
        eventBus = createEventBus();
        eventBus.start();

        resources = InMemoryIntegrationResources.builder()
            .authenticator(authenticator)
            .authorizator(authorizator)
            .eventBus(eventBus)
            .defaultAnnotationLimits()
            .defaultMessageParser()
            .scanningSearchIndex()
            .noPreDeletionHooks()
            .storeQuotaManager()
            .build();

        ImapProcessor defaultImapProcessorFactory =
            DefaultImapProcessorFactory.createDefaultProcessor(
                resources.getMailboxManager(),
                eventBus,
                new StoreSubscriptionManager(resources.getMailboxManager().getMapperFactory(), resources.getMailboxManager().getMapperFactory(), resources.getMailboxManager().getEventBus()),
                resources.getQuotaManager(),
                resources.getDefaultUserQuotaRootResolver(),
                new DefaultMetricFactory());

        configure(new DefaultImapDecoderFactory().buildImapDecoder(),
            new DefaultImapEncoderFactory().buildImapEncoder(),
            defaultImapProcessorFactory);
    }

    private RabbitMQEventBus createEventBus() throws Exception {
        InMemoryMessageId.Factory messageIdFactory = new InMemoryMessageId.Factory();
        InMemoryId.Factory mailboxIdFactory = new InMemoryId.Factory();
        MailboxEventSerializer eventSerializer = new MailboxEventSerializer(mailboxIdFactory, messageIdFactory, new DefaultUserQuotaRootResolver.DefaultQuotaRootDeserializer());
        RoutingKeyConverter routingKeyConverter = new RoutingKeyConverter(ImmutableSet.of(new MailboxIdRegistrationKey.Factory(mailboxIdFactory)));
        return new RabbitMQEventBus(MAILBOX_EVENT_NAMING_STRATEGY, reactorRabbitMQChannelPool.getSender(), reactorRabbitMQChannelPool::createReceiver,
            eventSerializer, RetryBackoffConfiguration.DEFAULT, routingKeyConverter, new MemoryEventDeadLetters(),
            new RecordingMetricFactory(),
            reactorRabbitMQChannelPool, EventBusId.random(), dockerRabbitMQ.getConfiguration());
    }

    @Override
    public void afterTest() {
        eventBus.stop();
        reactorRabbitMQChannelPool.close();
        connectionPool.close();
    }

    @Override
    protected MailboxManager getMailboxManager() {
        return resources.getMailboxManager();
    }

    @Override
    public boolean supports(Feature... features) {
        return SUPPORTED_FEATURES.supports(features);
    }

    @Override
    public void setQuotaLimits(QuotaCountLimit maxMessageQuota, QuotaSizeLimit maxStorageQuota) {
        resources.getMaxQuotaManager().setGlobalMaxMessage(maxMessageQuota);
        resources.getMaxQuotaManager().setGlobalMaxStorage(maxStorageQuota);
    }

    @Override
    protected void await() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
