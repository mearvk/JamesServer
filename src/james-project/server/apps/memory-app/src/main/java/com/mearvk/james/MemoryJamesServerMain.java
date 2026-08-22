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

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import com.mearvk.james.data.UsersRepositoryModuleChooser;
import com.mearvk.james.jmap.JMAPListenerModule;
import com.mearvk.james.jmap.JMAPModule;
import com.mearvk.james.jmap.api.identity.CustomIdentityDAO;
import com.mearvk.james.jmap.memory.identity.MemoryCustomIdentityDAO;
import com.mearvk.james.jmap.memory.pushsubscription.MemoryPushSubscriptionModule;
import com.mearvk.james.jwt.JwtConfiguration;
import com.mearvk.james.modules.BlobExportMechanismModule;
import com.mearvk.james.modules.BlobMemoryModule;
import com.mearvk.james.modules.MailboxModule;
import com.mearvk.james.modules.MailetProcessingModule;
import com.mearvk.james.modules.RunArgumentsModule;
import com.mearvk.james.modules.data.MemoryDataJmapModule;
import com.mearvk.james.modules.data.MemoryDataModule;
import com.mearvk.james.modules.data.MemoryDelegationStoreModule;
import com.mearvk.james.modules.data.MemoryDropListsModule;
import com.mearvk.james.modules.data.MemoryUsersRepositoryModule;
import com.mearvk.james.modules.eventstore.MemoryEventStoreModule;
import com.mearvk.james.modules.mailbox.MemoryMailboxModule;
import com.mearvk.james.modules.protocols.IMAPServerModule;
import com.mearvk.james.modules.protocols.JMAPServerModule;
import com.mearvk.james.modules.protocols.JmapEventBusModule;
import com.mearvk.james.modules.protocols.LMTPServerModule;
import com.mearvk.james.modules.protocols.ManageSieveServerModule;
import com.mearvk.james.modules.protocols.POP3ServerModule;
import com.mearvk.james.modules.protocols.ProtocolHandlerModule;
import com.mearvk.james.modules.protocols.SMTPServerModule;
import com.mearvk.james.modules.queue.memory.MemoryMailQueueModule;
import com.mearvk.james.modules.server.DKIMMailetModule;
import com.mearvk.james.modules.server.DLPRoutesModule;
import com.mearvk.james.modules.server.DataRoutesModules;
import com.mearvk.james.modules.server.DropListsRoutesModule;
import com.mearvk.james.modules.server.InconsistencyQuotasSolvingRoutesModule;
import com.mearvk.james.modules.server.JMXServerModule;
import com.mearvk.james.modules.server.JmapTasksModule;
import com.mearvk.james.modules.server.MailQueueRoutesModule;
import com.mearvk.james.modules.server.MailRepositoriesRoutesModule;
import com.mearvk.james.modules.server.MailboxRoutesModule;
import com.mearvk.james.modules.server.MailboxesExportRoutesModule;
import com.mearvk.james.modules.server.MailetContainerModule;
import com.mearvk.james.modules.server.NoJwtModule;
import com.mearvk.james.modules.server.RawPostDequeueDecoratorModule;
import com.mearvk.james.modules.server.SieveRoutesModule;
import com.mearvk.james.modules.server.TaskManagerModule;
import com.mearvk.james.modules.server.UserIdentityModule;
import com.mearvk.james.modules.server.VacationRoutesModule;
import com.mearvk.james.modules.server.WebAdminMailOverWebModule;
import com.mearvk.james.modules.server.WebAdminServerModule;
import com.mearvk.james.modules.vault.DeletedMessageVaultModule;
import com.mearvk.james.modules.vault.DeletedMessageVaultRoutesModule;
import com.mearvk.james.webadmin.WebAdminConfiguration;
import com.mearvk.james.webadmin.authentication.AuthenticationFilter;
import com.mearvk.james.webadmin.authentication.NoAuthenticationFilter;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.util.Modules;

public class MemoryJamesServerMain implements JamesServerMain {

    public static final Module WEBADMIN = Modules.combine(
        new WebAdminServerModule(),
        new DataRoutesModules(),
        new VacationRoutesModule(),
        new DeletedMessageVaultRoutesModule(),
        new DLPRoutesModule(),
        new InconsistencyQuotasSolvingRoutesModule(),
        new MailboxesExportRoutesModule(),
        new MailboxRoutesModule(),
        new MailQueueRoutesModule(),
        new MailRepositoriesRoutesModule(),
        new SieveRoutesModule(),
        new UserIdentityModule(),
        new WebAdminMailOverWebModule());

    public static final JwtConfiguration NO_JWT_CONFIGURATION = new JwtConfiguration(ImmutableList.of());

    public static final Module WEBADMIN_NO_AUTH_MODULE = Modules.combine(binder -> binder.bind(JwtConfiguration.class).toInstance(NO_JWT_CONFIGURATION),
        binder -> binder.bind(AuthenticationFilter.class).to(NoAuthenticationFilter.class),
        binder -> binder.bind(WebAdminConfiguration.class).toInstance(WebAdminConfiguration.TEST_CONFIGURATION));

    private static final Module CUSTOM_IDENTITY_DAO_TESTING = binder -> binder.bind(CustomIdentityDAO.class)
        .to(MemoryCustomIdentityDAO.class)
        .in(Scopes.SINGLETON);

    public static final Module WEBADMIN_TESTING = Modules.override(WEBADMIN)
        .with(WEBADMIN_NO_AUTH_MODULE, new NoJwtModule(), CUSTOM_IDENTITY_DAO_TESTING);

    public static final Module PROTOCOLS = Modules.combine(
        new IMAPServerModule(),
        new LMTPServerModule(),
        new ManageSieveServerModule(),
        new POP3ServerModule(),
        new ProtocolHandlerModule(),
        new SMTPServerModule());

    public static final Module JMAP = Modules.combine(
        new JmapEventBusModule(),
        new JmapTasksModule(),
        new MemoryDataJmapModule(),
        new MemoryPushSubscriptionModule(),
        JMAPModule.INSTANCE,
        new JMAPServerModule());

    public static final Module IN_MEMORY_SERVER_MODULE = Modules.combine(
        new MailetProcessingModule(),
        new MemoryDelegationStoreModule(),
        new BlobMemoryModule(),
        new DeletedMessageVaultModule(),
        new BlobExportMechanismModule(),
        new MailboxModule(),
        new MemoryDataModule(),
        new MemoryEventStoreModule(),
        new MemoryMailboxModule(),
        new MemoryMailQueueModule(),
        new TaskManagerModule());

    public static final Module SMTP_ONLY_MODULE = Modules.combine(
        MemoryJamesServerMain.IN_MEMORY_SERVER_MODULE,
        new ProtocolHandlerModule(),
        new SMTPServerModule(),
        new RawPostDequeueDecoratorModule(),
        binder -> binder.bind(MailetContainerModule.DefaultProcessorsConfigurationSupplier.class)
            .toInstance(BaseHierarchicalConfiguration::new));


    public static final Module SMTP_AND_IMAP_MODULE = Modules.combine(
        SMTP_ONLY_MODULE,
        new IMAPServerModule());

    public static final Module IN_MEMORY_SERVER_AGGREGATE_MODULE = Modules.combine(
        IN_MEMORY_SERVER_MODULE,
        PROTOCOLS,
        JMAP,
        WEBADMIN,
        new DKIMMailetModule());

    public static void main(String[] args) throws Exception {
        ExtraProperties.initialize();

        MemoryJamesConfiguration configuration = MemoryJamesConfiguration.builder()
            .useWorkingDirectoryEnvProperty()
            .build();

        LOGGER.info("Loading configuration {}", configuration.toString());
        GuiceJamesServer server = createServer(configuration)
            .combineWith(new JMXServerModule())
            .overrideWith(new RunArgumentsModule(args));

        JamesServerMain.main(server);
    }

    public static GuiceJamesServer createServer(MemoryJamesConfiguration configuration) {
        return GuiceJamesServer.forConfiguration(configuration)
            .combineWith(IN_MEMORY_SERVER_AGGREGATE_MODULE)
            .combineWith(new UsersRepositoryModuleChooser(new MemoryUsersRepositoryModule())
                .chooseModules(configuration.getUsersRepositoryImplementation()))
            .combineWith(chooseJmapModule(configuration))
            .combineWith(chooseDropListsModule(configuration));
    }

    private static Module chooseJmapModule(MemoryJamesConfiguration configuration) {
        if (configuration.isJmapEnabled()) {
            return new JMAPListenerModule();
        }
        return binder -> {

        };
    }

    private static Module chooseDropListsModule(MemoryJamesConfiguration configuration) {
        if (configuration.isDropListsEnabled()) {
            return Modules.combine(new MemoryDropListsModule(), new DropListsRoutesModule());
        }
        return binder -> {

        };
    }

}
