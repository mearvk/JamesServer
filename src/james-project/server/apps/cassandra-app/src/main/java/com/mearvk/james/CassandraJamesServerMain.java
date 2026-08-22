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

import java.util.Set;

import com.mearvk.james.data.UsersRepositoryModuleChooser;
import com.mearvk.james.eventsourcing.eventstore.EventNestedTypes;
import com.mearvk.james.jmap.JMAPListenerModule;
import com.mearvk.james.jmap.JMAPModule;
import com.mearvk.james.json.DTOModule;
import com.mearvk.james.modules.BlobExportMechanismModule;
import com.mearvk.james.modules.CassandraConsistencyTaskSerializationModule;
import com.mearvk.james.modules.MailboxModule;
import com.mearvk.james.modules.MailetProcessingModule;
import com.mearvk.james.modules.data.CassandraDLPConfigurationStoreModule;
import com.mearvk.james.modules.data.CassandraDelegationStoreModule;
import com.mearvk.james.modules.data.CassandraDomainListModule;
import com.mearvk.james.modules.data.CassandraJmapModule;
import com.mearvk.james.modules.data.CassandraRecipientRewriteTableModule;
import com.mearvk.james.modules.data.CassandraSieveQuotaLegacyModule;
import com.mearvk.james.modules.data.CassandraSieveQuotaModule;
import com.mearvk.james.modules.data.CassandraSieveRepositoryModule;
import com.mearvk.james.modules.data.CassandraUsersRepositoryModule;
import com.mearvk.james.modules.data.CassandraVacationModule;
import com.mearvk.james.modules.eventstore.CassandraEventStoreModule;
import com.mearvk.james.modules.mailbox.CassandraBlobStoreDependenciesModule;
import com.mearvk.james.modules.mailbox.CassandraDeletedMessageVaultModule;
import com.mearvk.james.modules.mailbox.CassandraMailboxModule;
import com.mearvk.james.modules.mailbox.CassandraMailboxQuotaLegacyModule;
import com.mearvk.james.modules.mailbox.CassandraMailboxQuotaModule;
import com.mearvk.james.modules.mailbox.CassandraQuotaMailingModule;
import com.mearvk.james.modules.mailbox.CassandraSessionModule;
import com.mearvk.james.modules.mailbox.DefaultBucketModule;
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
import com.mearvk.james.modules.queue.activemq.ActiveMQQueueModule;
import com.mearvk.james.modules.server.DKIMMailetModule;
import com.mearvk.james.modules.server.DLPRoutesModule;
import com.mearvk.james.modules.server.DataRoutesModules;
import com.mearvk.james.modules.server.InconsistencyQuotasSolvingRoutesModule;
import com.mearvk.james.modules.server.JMXServerModule;
import com.mearvk.james.modules.server.JmapTasksModule;
import com.mearvk.james.modules.server.JmapUploadCleanupModule;
import com.mearvk.james.modules.server.MailQueueRoutesModule;
import com.mearvk.james.modules.server.MailRepositoriesRoutesModule;
import com.mearvk.james.modules.server.MailboxRoutesModule;
import com.mearvk.james.modules.server.MailboxesExportRoutesModule;
import com.mearvk.james.modules.server.MessagesRoutesModule;
import com.mearvk.james.modules.server.SieveRoutesModule;
import com.mearvk.james.modules.server.TaskManagerModule;
import com.mearvk.james.modules.server.UserIdentityModule;
import com.mearvk.james.modules.server.VacationRoutesModule;
import com.mearvk.james.modules.server.WebAdminMailOverWebModule;
import com.mearvk.james.modules.server.WebAdminReIndexingTaskSerializationModule;
import com.mearvk.james.modules.server.WebAdminServerModule;
import com.mearvk.james.modules.vault.DeletedMessageVaultRoutesModule;
import com.mearvk.james.modules.webadmin.CassandraRoutesModule;
import com.mearvk.james.modules.webadmin.InconsistencySolvingRoutesModule;
import com.mearvk.james.vault.VaultConfiguration;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

/**
 * See https://issues.apache.org/jira/browse/JAMES-3767
 *
 * Cassandra APP will be removed after 3.8.0 release.
 *
 * Please migrate to the distributed APP.
 */
@Deprecated(forRemoval = true)
public class CassandraJamesServerMain implements JamesServerMain {

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
        binder.bind(new TypeLiteral<Set<DTOModule<?, ? extends org.apache.james.json.DTO>>>() {}).annotatedWith(Names.named(EventNestedTypes.EVENT_NESTED_TYPES_INJECTION_NAME))
            .toInstance(ImmutableSet.of());

    public static final Module CASSANDRA_SERVER_CORE_MODULE = Modules.combine(
        new ActiveMQQueueModule(),
        new CassandraDelegationStoreModule(),
        new CassandraBlobStoreDependenciesModule(),
        new CassandraDomainListModule(),
        new CassandraDLPConfigurationStoreModule(),
        new CassandraEventStoreModule(),
        new CassandraMailRepositoryModule(),
        new CassandraMetricsModule(),
        new CassandraRecipientRewriteTableModule(),
        new CassandraSessionModule(),
        new CassandraSieveRepositoryModule(),
        BLOB_MODULE,
        CASSANDRA_EVENT_STORE_JSON_SERIALIZATION_DEFAULT_MODULE);

    public static final Module CASSANDRA_MAILBOX_MODULE = Modules.combine(
        new CassandraConsistencyTaskSerializationModule(),
        new CassandraMailboxModule(),
        new MailboxModule(),
        new TikaMailboxModule());

    public static final Module REQUIRE_TASK_MANAGER_MODULE = Modules.combine(
        CASSANDRA_SERVER_CORE_MODULE,
        CASSANDRA_MAILBOX_MODULE,
        PROTOCOLS,
        PLUGINS,
        new DKIMMailetModule());

    protected static final Module ALL_BUT_JMX_CASSANDRA_MODULE = Modules.combine(
        new MailetProcessingModule(),
        new DefaultBucketModule(),
        new CassandraBlobStoreModule(),
        REQUIRE_TASK_MANAGER_MODULE,
        new TaskManagerModule(),
        CASSANDRA_EVENT_STORE_JSON_SERIALIZATION_DEFAULT_MODULE
    );

    public static void main(String[] args) throws Exception {
        ExtraProperties.initialize();

        CassandraJamesServerConfiguration configuration = CassandraJamesServerConfiguration.builder()
            .useWorkingDirectoryEnvProperty()
            .build();

        LOGGER.info("Loading configuration {}", configuration.toString());
        GuiceJamesServer server = createServer(configuration)
            .combineWith(new JMXServerModule());

        JamesServerMain.main(server);
    }

    public static GuiceJamesServer createServer(CassandraJamesServerConfiguration configuration) {
        return GuiceJamesServer.forConfiguration(configuration)
            .combineWith(ALL_BUT_JMX_CASSANDRA_MODULE)
            .combineWith(BlobStoreModulesChooser.chooseModules(configuration.getBlobStoreConfiguration()))
            .combineWith(SearchModuleChooser.chooseModules(configuration.searchConfiguration()))
            .combineWith(new UsersRepositoryModuleChooser(new CassandraUsersRepositoryModule())
                .chooseModules(configuration.getUsersRepositoryImplementation()))
            .combineWith(chooseDeletedMessageVault(configuration.getVaultConfiguration()))
            .combineWith(chooseQuotaModule(configuration))
            .combineWith(chooseJmapModule(configuration));
    }

    private static Module chooseDeletedMessageVault(VaultConfiguration vaultConfiguration) {
        if (vaultConfiguration.isEnabled()) {
            return Modules.combine(
                new CassandraDeletedMessageVaultModule(),
                new DeletedMessageVaultRoutesModule());
        }
        return binder -> {

        };
    }

    private static Module chooseJmapModule(CassandraJamesServerConfiguration configuration) {
        if (configuration.isJmapEnabled()) {
            return new JMAPListenerModule();
        }
        return binder -> {

        };
    }

    private static Module chooseQuotaModule(CassandraJamesServerConfiguration configuration) {
        if (configuration.isQuotaCompatibilityMode()) {
            return Modules.combine(new CassandraMailboxQuotaLegacyModule(), new CassandraSieveQuotaLegacyModule());
        } else {
            return Modules.combine(new CassandraMailboxQuotaModule(), new CassandraSieveQuotaModule());
        }
    }
}
