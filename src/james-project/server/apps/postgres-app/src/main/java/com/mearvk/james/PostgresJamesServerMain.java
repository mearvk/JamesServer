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

package com.mearvk.james;

import static com.mearvk.james.PostgresJamesConfiguration.EventBusImpl.RABBITMQ;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.mearvk.james.data.UsersRepositoryModuleChooser;
import com.mearvk.james.eventsourcing.eventstore.EventNestedTypes;
import com.mearvk.james.jmap.JMAPListenerModule;
import com.mearvk.james.jmap.JMAPModule;
import com.mearvk.james.json.DTO;
import com.mearvk.james.json.DTOModule;
import com.mearvk.james.modules.BlobExportMechanismModule;
import com.mearvk.james.modules.DistributedTaskSerializationModule;
import com.mearvk.james.modules.MailboxModule;
import com.mearvk.james.modules.MailetProcessingModule;
import com.mearvk.james.modules.RunArgumentsModule;
import com.mearvk.james.modules.TasksCleanupTaskSerializationModule;
import com.mearvk.james.modules.blobstore.BlobStoreCacheModulesChooser;
import com.mearvk.james.modules.blobstore.BlobStoreModulesChooser;
import com.mearvk.james.modules.data.PostgresDLPConfigurationStoreModule;
import com.mearvk.james.modules.data.PostgresDataJmapModule;
import com.mearvk.james.modules.data.PostgresDataModule;
import com.mearvk.james.modules.data.PostgresDelegationStoreModule;
import com.mearvk.james.modules.data.PostgresDropListsModule;
import com.mearvk.james.modules.data.PostgresEventStoreModule;
import com.mearvk.james.modules.data.PostgresUsersRepositoryModule;
import com.mearvk.james.modules.data.PostgresVacationModule;
import com.mearvk.james.modules.data.SievePostgresRepositoryModules;
import com.mearvk.james.modules.event.JMAPEventBusModule;
import com.mearvk.james.modules.event.MailboxEventBusModule;
import com.mearvk.james.modules.events.PostgresDeadLetterModule;
import com.mearvk.james.modules.mailbox.DefaultEventModule;
import com.mearvk.james.modules.mailbox.PostgresDeletedMessageVaultModule;
import com.mearvk.james.modules.mailbox.PostgresMailboxModule;
import com.mearvk.james.modules.mailbox.RLSSupportPostgresMailboxModule;
import com.mearvk.james.modules.mailbox.TikaMailboxModule;
import com.mearvk.james.modules.plugins.QuotaMailingModule;
import com.mearvk.james.modules.protocols.IMAPServerModule;
import com.mearvk.james.modules.protocols.JMAPServerModule;
import com.mearvk.james.modules.protocols.JmapEventBusModule;
import com.mearvk.james.modules.protocols.LMTPServerModule;
import com.mearvk.james.modules.protocols.ManageSieveServerModule;
import com.mearvk.james.modules.protocols.POP3ServerModule;
import com.mearvk.james.modules.protocols.ProtocolHandlerModule;
import com.mearvk.james.modules.protocols.SMTPServerModule;
import com.mearvk.james.modules.queue.activemq.ActiveMQQueueModule;
import com.mearvk.james.modules.queue.rabbitmq.FakeMailQueueViewModule;
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
import com.mearvk.james.modules.server.RabbitMailQueueRoutesModule;
import com.mearvk.james.modules.server.ReIndexingModule;
import com.mearvk.james.modules.server.SieveRoutesModule;
import com.mearvk.james.modules.server.TaskManagerModule;
import com.mearvk.james.modules.server.UserIdentityModule;
import com.mearvk.james.modules.server.WebAdminReIndexingTaskSerializationModule;
import com.mearvk.james.modules.server.WebAdminServerModule;
import com.mearvk.james.modules.task.DistributedTaskManagerModule;
import com.mearvk.james.modules.task.PostgresTaskExecutionDetailsProjectionGuiceModule;
import com.mearvk.james.modules.vault.DeletedMessageVaultRoutesModule;
import com.mearvk.james.modules.webadmin.TasksCleanupRoutesModule;
import com.mearvk.james.vault.VaultConfiguration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

public class PostgresJamesServerMain implements JamesServerMain {

    private static final Module EVENT_STORE_JSON_SERIALIZATION_DEFAULT_MODULE = binder ->
        binder.bind(new TypeLiteral<Set<DTOModule<?, ? extends DTO>>>() {
            }).annotatedWith(Names.named(EventNestedTypes.EVENT_NESTED_TYPES_INJECTION_NAME))
            .toInstance(ImmutableSet.of());

    private static final Module WEBADMIN = Modules.combine(
        new WebAdminServerModule(),
        new DataRoutesModules(),
        new InconsistencyQuotasSolvingRoutesModule(),
        new MailboxRoutesModule(),
        new MailQueueRoutesModule(),
        new MailRepositoriesRoutesModule(),
        new ReIndexingModule(),
        new SieveRoutesModule(),
        new WebAdminReIndexingTaskSerializationModule(),
        new MailboxesExportRoutesModule(),
        new UserIdentityModule(),
        new DLPRoutesModule(),
        new JmapUploadCleanupModule(),
        new JmapTasksModule(),
        new TasksCleanupRoutesModule(),
        new TasksCleanupTaskSerializationModule());

    private static final Module PROTOCOLS = Modules.combine(
        new IMAPServerModule(),
        new LMTPServerModule(),
        new ManageSieveServerModule(),
        new POP3ServerModule(),
        new ProtocolHandlerModule(),
        new SMTPServerModule(),
        WEBADMIN);

    private static final Module POSTGRES_SERVER_MODULE = Modules.combine(
        new BlobExportMechanismModule(),
        new PostgresDelegationStoreModule(),
        new PostgresMailboxModule(),
        new PostgresDeadLetterModule(),
        new PostgresDataModule(),
        new MailboxModule(),
        new SievePostgresRepositoryModules(),
        new PostgresEventStoreModule(),
        new TikaMailboxModule(),
        new PostgresDLPConfigurationStoreModule(),
        new PostgresVacationModule(),
        EVENT_STORE_JSON_SERIALIZATION_DEFAULT_MODULE);

    public static final Module JMAP = Modules.combine(
        new PostgresJmapModule(),
        new PostgresDataJmapModule(),
        new JmapEventBusModule(),
        new JMAPServerModule(),
        JMAPModule.INSTANCE);

    public static final Module PLUGINS = new QuotaMailingModule();

    private static final Function<PostgresJamesConfiguration, Module> POSTGRES_MODULE_AGGREGATE = configuration ->
        Modules.override(Modules.combine(
                new MailetProcessingModule(),
                new DKIMMailetModule(),
                POSTGRES_SERVER_MODULE,
                JMAP,
                PROTOCOLS,
                PLUGINS))
            .with(chooseEventBusModules(configuration));

    public static void main(String[] args) throws Exception {
        ExtraProperties.initialize();

        PostgresJamesConfiguration configuration = PostgresJamesConfiguration.builder()
            .useWorkingDirectoryEnvProperty()
            .build();

        LOGGER.info("Loading configuration {}", configuration.toString());
        GuiceJamesServer server = createServer(configuration)
            .combineWith(new JMXServerModule())
            .overrideWith(new RunArgumentsModule(args));

        JamesServerMain.main(server);
    }

    public static GuiceJamesServer createServer(PostgresJamesConfiguration configuration) {
        SearchConfiguration searchConfiguration = configuration.searchConfiguration();

        return GuiceJamesServer.forConfiguration(configuration)
            .combineWith(POSTGRES_MODULE_AGGREGATE.apply(configuration))
            .combineWith(SearchModuleChooser.chooseModules(searchConfiguration))
            .combineWith(chooseUsersRepositoryModule(configuration))
            .combineWith(chooseBlobStoreModules(configuration))
            .combineWith(chooseDeletedMessageVaultModules(configuration.getDeletedMessageVaultConfiguration()))
            .combineWith(chooseRLSSupportPostgresMailboxModule(configuration))
            .overrideWith(chooseJmapModules(configuration))
            .overrideWith(chooseTaskManagerModules(configuration))
            .overrideWith(chooseDropListsModule(configuration));
    }

    private static List<Module> chooseUsersRepositoryModule(PostgresJamesConfiguration configuration) {
        return List.of(PostgresUsersRepositoryModule.USER_CONFIGURATION_MODULE,
            Modules.combine(new UsersRepositoryModuleChooser(new PostgresUsersRepositoryModule())
                .chooseModules(configuration.getUsersRepositoryImplementation())));
    }

    private static List<Module> chooseBlobStoreModules(PostgresJamesConfiguration configuration) {
        ImmutableList.Builder<Module> builder = ImmutableList.<Module>builder()
            .addAll(BlobStoreModulesChooser.chooseModules(configuration.blobStoreConfiguration()))
            .add(new BlobStoreCacheModulesChooser.CacheDisabledModule());

        return builder.build();
    }

    public static List<Module> chooseTaskManagerModules(PostgresJamesConfiguration configuration) {
        switch (configuration.eventBusImpl()) {
            case IN_MEMORY:
                return List.of(new TaskManagerModule(), new PostgresTaskExecutionDetailsProjectionGuiceModule());
            case RABBITMQ:
                return List.of(new DistributedTaskManagerModule());
            default:
                throw new RuntimeException("Unsupported event-bus implementation " + configuration.eventBusImpl().name());
        }
    }

    public static List<Module> chooseEventBusModules(PostgresJamesConfiguration configuration) {
        switch (configuration.eventBusImpl()) {
            case IN_MEMORY:
                return List.of(
                    new DefaultEventModule(),
                    new ActiveMQQueueModule());
            case RABBITMQ:
                return List.of(
                    Modules.override(new DefaultEventModule()).with(new MailboxEventBusModule()),
                    new RabbitMQModule(),
                    new RabbitMQMailQueueModule(),
                    new FakeMailQueueViewModule(),
                    new RabbitMailQueueRoutesModule(),
                    new DistributedTaskSerializationModule());
            default:
                throw new RuntimeException("Unsupported event-bus implementation " + configuration.eventBusImpl().name());
        }
    }

    private static Module chooseDeletedMessageVaultModules(VaultConfiguration vaultConfiguration) {
        if (vaultConfiguration.isEnabled()) {
            return Modules.combine(new PostgresDeletedMessageVaultModule(), new DeletedMessageVaultRoutesModule());
        }

        return Modules.EMPTY_MODULE;
    }

    private static Module chooseJmapModules(PostgresJamesConfiguration configuration) {
        if (configuration.isJmapEnabled()) {
            if (configuration.eventBusImpl() == RABBITMQ) {
                return Modules.combine(new JMAPEventBusModule(), new JMAPListenerModule());
            }
            return new JMAPListenerModule();
        }
        return binder -> {
        };
    }

    private static Module chooseDropListsModule(PostgresJamesConfiguration configuration) {
        if (configuration.isDropListsEnabled()) {
            return Modules.combine(new PostgresDropListsModule(), new DropListsRoutesModule());
        }
        return binder -> {

        };
    }

    private static Module chooseRLSSupportPostgresMailboxModule(PostgresJamesConfiguration configuration) {
        if (configuration.isRlsEnabled()) {
            return new RLSSupportPostgresMailboxModule();
        }
        return Modules.EMPTY_MODULE;
    }
}
