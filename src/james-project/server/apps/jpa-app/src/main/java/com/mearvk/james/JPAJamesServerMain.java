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

import com.mearvk.james.data.UsersRepositoryModuleChooser;
import org.apache.james.mailbox.extractor.TextExtractor;
import org.apache.james.mailbox.store.extractor.JsoupTextExtractor;
import com.mearvk.james.modules.MailboxModule;
import com.mearvk.james.modules.MailetProcessingModule;
import com.mearvk.james.modules.RunArgumentsModule;
import com.mearvk.james.modules.data.JPADataModule;
import com.mearvk.james.modules.data.JPADropListsModule;
import com.mearvk.james.modules.data.JPAUsersRepositoryModule;
import com.mearvk.james.modules.data.SieveJPARepositoryModules;
import com.mearvk.james.modules.mailbox.DefaultEventModule;
import com.mearvk.james.modules.mailbox.JPAMailboxModule;
import com.mearvk.james.modules.mailbox.LuceneSearchMailboxModule;
import com.mearvk.james.modules.mailbox.MemoryDeadLetterModule;
import com.mearvk.james.modules.mailbox.ReIndexingTaskSerializationModule;
import com.mearvk.james.modules.protocols.IMAPServerModule;
import com.mearvk.james.modules.protocols.LMTPServerModule;
import com.mearvk.james.modules.protocols.ManageSieveServerModule;
import com.mearvk.james.modules.protocols.POP3ServerModule;
import com.mearvk.james.modules.protocols.ProtocolHandlerModule;
import com.mearvk.james.modules.protocols.SMTPServerModule;
import com.mearvk.james.modules.queue.activemq.ActiveMQQueueModule;
import com.mearvk.james.modules.server.DataRoutesModules;
import com.mearvk.james.modules.server.DefaultProcessorsConfigurationProviderModule;
import com.mearvk.james.modules.server.DropListsRoutesModule;
import com.mearvk.james.modules.server.InconsistencyQuotasSolvingRoutesModule;
import com.mearvk.james.modules.server.JMXServerModule;
import com.mearvk.james.modules.server.MailQueueRoutesModule;
import com.mearvk.james.modules.server.MailRepositoriesRoutesModule;
import com.mearvk.james.modules.server.MailboxRoutesModule;
import com.mearvk.james.modules.server.NoJwtModule;
import com.mearvk.james.modules.server.RawPostDequeueDecoratorModule;
import com.mearvk.james.modules.server.ReIndexingModule;
import com.mearvk.james.modules.server.SieveRoutesModule;
import com.mearvk.james.modules.server.TaskManagerModule;
import com.mearvk.james.modules.server.WebAdminMailOverWebModule;
import com.mearvk.james.modules.server.WebAdminReIndexingTaskSerializationModule;
import com.mearvk.james.modules.server.WebAdminServerModule;

import com.google.inject.Module;
import com.google.inject.util.Modules;

public class JPAJamesServerMain implements JamesServerMain {

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
        new WebAdminMailOverWebModule());

    private static final Module PROTOCOLS = Modules.combine(
        new IMAPServerModule(),
        new LMTPServerModule(),
        new ManageSieveServerModule(),
        new POP3ServerModule(),
        new ProtocolHandlerModule(),
        new SMTPServerModule(),
        WEBADMIN);

    private static final Module SEARCH_MODULE = Modules.combine(
        new LuceneSearchMailboxModule(),
        binder -> binder.bind(TextExtractor.class).toInstance(new JsoupTextExtractor()));

    private static final Module JPA_SERVER_MODULE = Modules.combine(
        SEARCH_MODULE,
        new ActiveMQQueueModule(),
        new NaiveDelegationStoreModule(),
        new DefaultProcessorsConfigurationProviderModule(),
        new JPADataModule(),
        new JPAMailboxModule(),
        new MailboxModule(),
        new ReIndexingTaskSerializationModule(),
        new NoJwtModule(),
        new RawPostDequeueDecoratorModule(),
        new SieveJPARepositoryModules(),
        new DefaultEventModule(),
        new TaskManagerModule(),
        new MemoryDeadLetterModule());

    private static final Module JPA_MODULE_AGGREGATE = Modules.combine(
        new MailetProcessingModule(), JPA_SERVER_MODULE, PROTOCOLS);

    public static void main(String[] args) throws Exception {
        ExtraProperties.initialize();

        JPAJamesConfiguration configuration = JPAJamesConfiguration.builder()
            .useWorkingDirectoryEnvProperty()
            .build();

        LOGGER.info("Loading configuration {}", configuration.toString());
        GuiceJamesServer server = createServer(configuration)
            .combineWith(new JMXServerModule())
            .overrideWith(new RunArgumentsModule(args))
            .overrideWith(chooseDropListsModule(configuration));

        JamesServerMain.main(server);
    }

    static GuiceJamesServer createServer(JPAJamesConfiguration configuration) {
        return GuiceJamesServer.forConfiguration(configuration)
            .combineWith(JPA_MODULE_AGGREGATE)
            .combineWith(new UsersRepositoryModuleChooser(new JPAUsersRepositoryModule())
                .chooseModules(configuration.getUsersRepositoryImplementation()));
    }

    private static Module chooseDropListsModule(JPAJamesConfiguration configuration) {
        if (configuration.isDropListsEnabled()) {
            return Modules.combine(new JPADropListsModule(), new DropListsRoutesModule());
        }
        return binder -> {

        };
    }
}
