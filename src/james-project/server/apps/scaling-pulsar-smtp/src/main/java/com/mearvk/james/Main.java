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

import com.mearvk.james.backends.postgres.PostgresDataDefinition;
import com.mearvk.james.blob.api.BlobStore;
import com.mearvk.james.blob.api.BlobStoreDAO;
import com.mearvk.james.blob.api.MetricableBlobStore;
import com.mearvk.james.blob.objectstorage.aws.S3BlobStoreDAO;
import com.mearvk.james.blob.objectstorage.aws.S3RequestOption;
import com.mearvk.james.mailrepository.api.MailRepositoryFactory;
import com.mearvk.james.mailrepository.api.MailRepositoryUrlStore;
import com.mearvk.james.mailrepository.postgres.PostgresMailRepositoryFactory;
import com.mearvk.james.mailrepository.postgres.PostgresMailRepositoryUrlStore;
import com.mearvk.james.modules.RunArgumentsModule;
import com.mearvk.james.modules.data.MemoryDelegationStoreModule;
import com.mearvk.james.modules.data.PostgresCommonModule;
import com.mearvk.james.modules.data.PostgresDomainListModule;
import com.mearvk.james.modules.data.PostgresDropListsModule;
import com.mearvk.james.modules.data.PostgresRecipientRewriteTableModule;
import com.mearvk.james.modules.data.PostgresUsersRepositoryModule;
import com.mearvk.james.modules.mailbox.BlobStoreAPIModule;
import com.mearvk.james.modules.mailrepository.BlobstoreMailRepositoryModule;
import com.mearvk.james.modules.objectstorage.S3BlobStoreModule;
import com.mearvk.james.modules.objectstorage.S3BucketModule;
import com.mearvk.james.modules.protocols.ProtocolHandlerModule;
import com.mearvk.james.modules.protocols.SMTPServerModule;
import com.mearvk.james.modules.server.DataRoutesModules;
import com.mearvk.james.modules.server.DefaultProcessorsConfigurationProviderModule;
import com.mearvk.james.modules.server.MailQueueRoutesModule;
import com.mearvk.james.modules.server.MailRepositoriesRoutesModule;
import com.mearvk.james.modules.server.MailetContainerModule;
import com.mearvk.james.modules.server.NoJwtModule;
import com.mearvk.james.modules.server.RawPostDequeueDecoratorModule;
import com.mearvk.james.modules.server.TaskManagerModule;
import com.mearvk.james.modules.server.WebAdminMailOverWebModule;
import com.mearvk.james.modules.server.WebAdminServerModule;
import com.mearvk.james.queue.pulsar.module.PulsarQueueModule;
import com.mearvk.james.server.blob.deduplication.PassThroughBlobStore;

import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

public class Main implements JamesServerMain {
    public static final Module WEBADMIN = Modules.combine(
            new DataRoutesModules(),
            new MailQueueRoutesModule(),
            new MailRepositoriesRoutesModule(),
            new NoJwtModule(),
            new WebAdminServerModule(),
            new WebAdminMailOverWebModule());
    public static final Module PROTOCOLS = Modules.combine(
            new SMTPServerModule(),
            new ProtocolHandlerModule());
    private static final Module BLOB_MODULE = Modules.combine(
            new BlobStoreAPIModule(),
            new S3BlobStoreModule(),
            new S3BucketModule(),
            binder -> {
                binder.bind(S3RequestOption.class).toInstance(S3RequestOption.DEFAULT);
                binder.bind(BlobStoreDAO.class).to(S3BlobStoreDAO.class)
                        .in(Scopes.SINGLETON);
                binder.bind(BlobStore.class)
                        .annotatedWith(Names.named(MetricableBlobStore.BLOB_STORE_IMPLEMENTATION))
                        .to(PassThroughBlobStore.class);
            });


    public static final Module QUEUE_MODULES = Modules.combine(
            new RawPostDequeueDecoratorModule(),
            new PulsarQueueModule());

    public static final Module SERVER_CORE_MODULES = Modules.combine(
            new DefaultProcessorsConfigurationProviderModule(),
            new MailStoreRepositoryModule(),
            new MailetContainerModule(),
            new BlobstoreMailRepositoryModule(),
            new PostgresCommonModule(),
            new PostgresDomainListModule(),
            new PostgresRecipientRewriteTableModule(),
            new PostgresUsersRepositoryModule(),
            PostgresUsersRepositoryModule.USER_CONFIGURATION_MODULE,
            new PostgresDropListsModule(),
            binder -> {
                Multibinder.newSetBinder(binder, MailRepositoryFactory.class)
                        .addBinding().to(PostgresMailRepositoryFactory.class);
                Multibinder.newSetBinder(binder, PostgresDataDefinition.class)
                        .addBinding().toInstance(org.apache.james.mailrepository.postgres.PostgresMailRepositoryDataDefinition.MODULE);
                binder.bind(MailRepositoryUrlStore.class).to(PostgresMailRepositoryUrlStore.class).in(Scopes.SINGLETON);
            },
            new CoreDataModule(),
            new MemoryDelegationStoreModule(),
            new TaskManagerModule()

    );

    public static void main(String[] args) throws Exception {
        SMTPRelayConfiguration configuration = SMTPRelayConfiguration.builder()
                .useWorkingDirectoryEnvProperty()
                .build();

        LOGGER.info("Loading configuration {}", configuration.toString());
        GuiceJamesServer server = createServer(configuration)
                .overrideWith(new RunArgumentsModule(args));

        try {
            JamesServerMain.main(server);
        } catch (Exception e) {
            LOGGER.error("Failed to start", e);
            throw e;
        }
    }

    public static GuiceJamesServer createServer(SMTPRelayConfiguration configuration) {
        return GuiceJamesServer.forConfiguration(configuration)
                .combineWith(SERVER_CORE_MODULES, BLOB_MODULE, QUEUE_MODULES, PROTOCOLS, WEBADMIN);
    }
}
