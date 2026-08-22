/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.mearvk.james;

import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import com.mearvk.james.data.UsersRepositoryModuleChooser;
import com.mearvk.james.eventsourcing.eventstore.EventNestedTypes;
import com.mearvk.james.jmap.JMAPListenerModule;
import com.mearvk.james.jmap.JMAPModule;
import com.mearvk.james.json.DTO;
import com.mearvk.james.json.DTOModule;
import com.mearvk.james.modules.BlobExportMechanismModule;
import com.mearvk.james.modules.CassandraConsistencyTaskSerializationModule;
import com.mearvk.james.modules.DistributedTaskManagerModule;
import com.mearvk.james.modules.DistributedTaskSerializationModule;
import com.mearvk.james.modules.MailboxModule;
import com.mearvk.james.modules.MailetProcessingModule;
import com.mearvk.james.modules.RunArgumentsModule;
import com.mearvk.james.modules.TasksCleanupTaskSerializationModule;
import com.mearvk.james.modules.blobstore.BlobStoreCacheModulesChooser;
import com.mearvk.james.modules.blobstore.BlobStoreConfiguration;
import com.mearvk.james.modules.blobstore.BlobStoreModulesChooser;
import com.mearvk.james.modules.data.CassandraDLPConfigurationStoreModule;
import com.mearvk.james.modules.data.CassandraDelegationStoreModule;
import com.mearvk.james.modules.data.CassandraDomainListModule;
import com.mearvk.james.modules.data.CassandraDropListsModule;
import com.mearvk.james.modules.data.CassandraJmapModule;
import com.mearvk.james.modules.data.CassandraRecipientRewriteTableModule;
import com.mearvk.james.modules.data.CassandraSieveQuotaLegacyModule;
import com.mearvk.james.modules.data.CassandraSieveQuotaModule;
import com.mearvk.james.modules.data.CassandraSieveRepositoryModule;
import com.mearvk.james.modules.data.CassandraUsersRepositoryModule;
import com.mearvk.james.modules.data.CassandraVacationModule;
import com.mearvk.james.modules.event.JMAPEventBusModule;
import com.mearvk.james.modules.event.MailboxEventBusModule;
import com.mearvk.james.modules.eventstore.CassandraEventStoreModule;
import com.mearvk.james.modules.mailbox.CassandraDeletedMessageVaultModule;
import com.mearvk.james.modules.mailbox.CassandraMailboxModule;
import com.mearvk.james.modules.mailbox.CassandraMailboxQuotaLegacyModule;
import com.mearvk.james.modules.mailbox.CassandraMailboxQuotaModule;
import com.mearvk.james.modules.mailbox.CassandraQuotaMailingModule;
import com.mearvk.james.modules.mailbox.CassandraSessionModule;
import com.mearvk.james.modules.mailbox.DistributedDeletedMessageVaultModule;
import com.mearvk.james.modules.mailbox.TikaMailboxModule;
import com.mearvk.james.modules.mailrepository.CassandraMailRepositoryModule;
import com.mearvk.james.modules.metrics.CassandraMetricsModule;
import com.mearvk.james.modules.protocols.IMAPServerModule;
import com.mearvk.james.modules.protocols.JMAPServerModule;
import com.mearvk.james.modules.protocols.JmapEventBusModule;
import com.mearvk.james.modules.protocols.LMTPServerModule;
import com.mearvk.james.modules.protocols.ManageSieveServerModule;
import com.mearvk.james.modules.protocols.POP3ServerModule;
import com.mearvk.james.modules.protocols.ProtocolHandlerModule;
import com.mearvk.james.modules.protocols.SMTPServerModule;
import com.mearvk.james.modules.queue.rabbitmq.MailQueueViewChoice;
import com.mearvk.james.modules.queue.rabbitmq.RabbitMQMailQueueModule;
import com.mearvk.james.modules.queue.rabbitmq.RabbitMQModule;
import com.mearvk.james.modules.server.DKIMMailetModule;
import com.mearvk.james.modules.server.DLPRoutesModule;
import com.mearvk.james.modules.server.DataRoutesModules;
import com.mearvk.james.modules.server.DropListsRoutesModule;
import com.mearvk.james.modules.server.InconsistencyQuotasSolvingRoutesModule;
import com.mearvk.james.modules.server.JMXServerModule;
import com.mearvk.james.modules.server.JmapTasksModule;
import com.mearvk.james.modules.server.JmapUploadCleanupModule;
import com.mearvk.james.modules.server.MailQueueRoutesModule;
import com.mearvk.james.modules.server.MailRepositoriesRoutesModule;
import com.mearvk.james.modules.server.MailboxRoutesModule;
import com.mearvk.james.modules.server.MailboxesExportRoutesModule;
import com.mearvk.james.modules.server.MessagesRoutesModule;
import com.mearvk.james.modules.server.RabbitMailQueueRoutesModule;
import com.mearvk.james.modules.server.SieveRoutesModule;
import com.mearvk.james.modules.server.UserIdentityModule;
import com.mearvk.james.modules.server.VacationRoutesModule;
import com.mearvk.james.modules.server.WebAdminMailOverWebModule;
import com.mearvk.james.modules.server.WebAdminReIndexingTaskSerializationModule;
import com.mearvk.james.modules.server.WebAdminServerModule;
import com.mearvk.james.modules.vault.DeletedMessageVaultRoutesModule;
import com.mearvk.james.modules.webadmin.CassandraRoutesModule;
import com.mearvk.james.modules.webadmin.InconsistencySolvingRoutesModule;
import com.mearvk.james.modules.webadmin.TasksCleanupRoutesModule;
import com.mearvk.james.queue.pulsar.module.PulsarQueueModule;
import com.mearvk.james.vault.VaultConfiguration;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

public class CassandraRabbitMQJamesServerMain implements JamesServerMain {
    public static final Module WEBADMIN = Modules.combine(
        new CassandraRoutesModule(),
        new DataRoutesModules(),
        new VacationRoutesModule(),
        new DLPRoutesModule(),
        new InconsistencyQuotasSolvingRoutesModule(),
        new InconsistencySolvingRoutesModule(),
        new JmapUploadCleanupModule(),
        new UserIdentityModule(),
        new JmapTasksModule(),
        new MailboxesExportRoutesModule(),
        new MailboxRoutesModule(),
        new MailQueueRoutesModule(),
        new MailRepositoriesRoutesModule(),
        new SieveRoutesModule(),
        new TasksCleanupRoutesModule(),
        new WebAdminServerModule(),
        new WebAdminReIndexingTaskSerializationModule(),
        new MessagesRoutesModule(),
        new WebAdminMailOverWebModule());

    public static final Module PROTOCOLS = Modules.combine(
        new CassandraJmapModule(),
        new CassandraVacationModule(),
        new IMAPServerModule(),
        new LMTPServerModule(),
        new ManageSieveServerModule(),
        new POP3ServerModule(),
        new ProtocolHandlerModule(),
        new SMTPServerModule(),
        new JMAPServerModule(),
        JMAPModule.INSTANCE,
        new JmapEventBusModule(),
        WEBADMIN);

    public static final Module PLUGINS = new CassandraQuotaMailingModule();

    private static final Module BLOB_MODULE = new BlobExportMechanismModule();

    private static final Module CASSANDRA_EVENT_STORE_JSON_SERIALIZATION_DEFAULT_MODULE = binder ->
        binder.bind(new TypeLiteral<Set<DTOModule<?, ? extends DTO>>>() {}).annotatedWith(Names.named(EventNestedTypes.EVENT_NESTED_TYPES_INJECTION_NAME))
            .toInstance(ImmutableSet.of());

    public static final Module CASSANDRA_SERVER_CORE_MODULE = Modules.combine(
        new CassandraDelegationStoreModule(),
        new CassandraDomainListModule(),
        new CassandraDLPConfigurationStoreModule(),
        new CassandraEventStoreModule(),
        new CassandraMailRepositoryModule(),
        new CassandraMetricsModule(),
        new CassandraRecipientRewriteTableModule(),
        new CassandraSessionModule(),
        new CassandraSieveRepositoryModule(),
        new TasksCleanupTaskSerializationModule(),
        BLOB_MODULE,
        CASSANDRA_EVENT_STORE_JSON_SERIALIZATION_DEFAULT_MODULE);

    public static final Module CASSANDRA_MAILBOX_MODULE = Modules.combine(
        new CassandraConsistencyTaskSerializationModule(),
        new CassandraMailboxModule(),
        new MailboxModule(),
        new TikaMailboxModule());

    public static final Module REQUIRE_TASK_MANAGER_MODULE = Modules.combine(
        new MailetProcessingModule(),
        CASSANDRA_SERVER_CORE_MODULE,
        CASSANDRA_MAILBOX_MODULE,
        PROTOCOLS,
        PLUGINS,
        new DKIMMailetModule());

    protected static final Module MODULES = Modules.override(REQUIRE_TASK_MANAGER_MODULE, new DistributedTaskManagerModule())
        .with(new RabbitMQModule(),
            new MailboxEventBusModule(),
            new DistributedTaskSerializationModule());

    public static void main(String[] args) throws Exception {
        ExtraProperties.initialize();

        CassandraRabbitMQJamesConfiguration configuration = CassandraRabbitMQJamesConfiguration.builder()
            .useWorkingDirectoryEnvProperty()
            .build();

        LOGGER.info("Loading configuration {}", configuration.toString());
        GuiceJamesServer server = createServer(configuration)
            .combineWith(new JMXServerModule())
            .overrideWith(new RunArgumentsModule(args));

        JamesServerMain.main(server);
    }

    public static GuiceJamesServer createServer(CassandraRabbitMQJamesConfiguration configuration) {
        BlobStoreConfiguration blobStoreConfiguration = configuration.blobStoreConfiguration();
        SearchConfiguration searchConfiguration = configuration.searchConfiguration();

        return GuiceJamesServer.forConfiguration(configuration)
            .combineWith(MODULES)
            .combineWith(chooseMailQueue(configuration))
            .combineWith(BlobStoreModulesChooser.chooseModules(blobStoreConfiguration))
            .combineWith(BlobStoreCacheModulesChooser.chooseModules(blobStoreConfiguration))
            .combineWith(SearchModuleChooser.chooseModules(searchConfiguration))
            .combineWith(new UsersRepositoryModuleChooser(new CassandraUsersRepositoryModule())
                .chooseModules(configuration.getUsersRepositoryImplementation()))
            .combineWith(chooseDeletedMessageVault(configuration.getVaultConfiguration()))
            .combineWith(chooseQuotaModule(configuration))
            .overrideWith(chooseJmapModules(configuration))
            .overrideWith(chooseDropListsModule(configuration));
    }

    private static Module chooseMailQueue(CassandraRabbitMQJamesConfiguration configuration) {
        switch (configuration.getMailQueueChoice()) {
            case PULSAR:
                return new PulsarQueueModule();
            case RABBITMQ:
                return Modules.combine(
                    new RabbitMailQueueRoutesModule(),
                    new RabbitMQMailQueueModule(),
                    MailQueueViewChoice.ModuleChooser.choose(configuration.getMailQueueViewChoice()));
            default:
                throw new NotImplementedException();
        }
    }

    private static Module chooseDeletedMessageVault(VaultConfiguration vaultConfiguration) {
        if (vaultConfiguration.isEnabled() && vaultConfiguration.isWorkQueueEnabled()) {
            return Modules.combine(
                new DistributedDeletedMessageVaultModule(),
                new DeletedMessageVaultRoutesModule());
        }
        if (vaultConfiguration.isEnabled()) {
            return Modules.combine(
                new CassandraDeletedMessageVaultModule(),
                new DeletedMessageVaultRoutesModule());
        }
        return binder -> {

        };
    }

    private static Module chooseJmapModules(CassandraRabbitMQJamesConfiguration configuration) {
        if (configuration.isJmapEnabled()) {
            return Modules.combine(new JMAPEventBusModule(), new JMAPListenerModule());
        }
        return binder -> {

        };
    }

    private static Module chooseQuotaModule(CassandraRabbitMQJamesConfiguration configuration) {
        if (configuration.isQuotaCompatibilityMode()) {
            return Modules.combine(new CassandraMailboxQuotaLegacyModule(), new CassandraSieveQuotaLegacyModule());
        } else {
            return Modules.combine(new CassandraMailboxQuotaModule(), new CassandraSieveQuotaModule());
        }
    }

    private static Module chooseDropListsModule(CassandraRabbitMQJamesConfiguration configuration) {
        if (configuration.isDropListsEnabled()) {
            return Modules.combine(new CassandraDropListsModule(), new DropListsRoutesModule());
        }
        return binder -> {

        };
    }

}
