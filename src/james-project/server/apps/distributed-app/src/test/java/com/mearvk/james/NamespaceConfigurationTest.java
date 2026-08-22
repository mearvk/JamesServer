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

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import com.mearvk.james.blob.api.BlobStore;
import com.mearvk.james.blob.api.BucketName;
import com.mearvk.james.modules.AwsS3BlobStoreExtension;
import com.mearvk.james.modules.RabbitMQExtension;
import com.mearvk.james.modules.TestJMAPServerModule;
import com.mearvk.james.modules.blobstore.BlobStoreConfiguration;
import com.mearvk.james.utils.GuiceProbe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.inject.multibindings.Multibinder;

class NamespaceConfigurationTest {
    static class DefaultBucketProbe implements GuiceProbe {
        private final BlobStore blobStore;

        @Inject
        DefaultBucketProbe(BlobStore blobStore) {
            this.blobStore = blobStore;
        }

        public BucketName getDefaultBucket() {
            return blobStore.getDefaultBucketName();
        }
    }

    @RegisterExtension
    static JamesServerExtension jamesServerExtension = new JamesServerBuilder<CassandraRabbitMQJamesConfiguration>(tmpDir ->
        CassandraRabbitMQJamesConfiguration.builder()
            .workingDirectory(tmpDir)
            .configurationFromClasspath()
            .blobStore(BlobStoreConfiguration.builder()
                .s3()
                .disableCache()
                .deduplication()
                .noCryptoConfig())
            .searchConfiguration(SearchConfiguration.openSearch())
            .build())
        .extension(new DockerOpenSearchExtension())
        .extension(new CassandraExtension())
        .extension(new RabbitMQExtension())
        .server(configuration -> CassandraRabbitMQJamesServerMain.createServer(configuration)
            .overrideWith(new TestJMAPServerModule())
            .overrideWith(binder -> Multibinder.newSetBinder(binder, GuiceProbe.class)
                .addBinding()
                .to(DefaultBucketProbe.class)))
        .extension(new AwsS3BlobStoreExtension())
        .lifeCycle(JamesServerExtension.Lifecycle.PER_TEST)
        .build();

    @Test
    void defaultBucketShouldBeTheConfiguredOne(GuiceJamesServer server) {
        // AwsS3BlobStoreExtension relies on a randomly generated bucket for isolation purposes
        assertThat(server.getProbe(DefaultBucketProbe.class)
                .getDefaultBucket())
            .isNotEqualTo(BucketName.DEFAULT);
    }
}