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

package com.mearvk.james.modules.task;

import static com.mearvk.james.modules.queue.rabbitmq.RabbitMQModule.RABBITMQ_CONFIGURATION_NAME;

import java.io.FileNotFoundException;

import jakarta.inject.Singleton;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import com.mearvk.james.backends.rabbitmq.SimpleConnectionPool;
import com.mearvk.james.core.healthcheck.HealthCheck;
import com.mearvk.james.modules.server.HostnameModule;
import com.mearvk.james.modules.server.TaskSerializationModule;
import com.mearvk.james.task.TaskManager;
import com.mearvk.james.task.eventsourcing.EventSourcingTaskManager;
import com.mearvk.james.task.eventsourcing.TerminationSubscriber;
import com.mearvk.james.task.eventsourcing.WorkQueueSupplier;
import com.mearvk.james.task.eventsourcing.distributed.CancelRequestQueueName;
import com.mearvk.james.task.eventsourcing.distributed.DistributedTaskManagerHealthCheck;
import com.mearvk.james.task.eventsourcing.distributed.RabbitMQTerminationSubscriber;
import com.mearvk.james.task.eventsourcing.distributed.RabbitMQWorkQueue;
import com.mearvk.james.task.eventsourcing.distributed.RabbitMQWorkQueueConfiguration;
import com.mearvk.james.task.eventsourcing.distributed.RabbitMQWorkQueueConfiguration$;
import com.mearvk.james.task.eventsourcing.distributed.RabbitMQWorkQueueReconnectionHandler;
import com.mearvk.james.task.eventsourcing.distributed.RabbitMQWorkQueueSupplier;
import com.mearvk.james.task.eventsourcing.distributed.TerminationQueueName;
import com.mearvk.james.task.eventsourcing.distributed.TerminationReconnectionHandler;
import com.mearvk.james.utils.InitializationOperation;
import com.mearvk.james.utils.InitilizationOperationBuilder;
import com.mearvk.james.utils.PropertiesProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.ProvidesIntoSet;

public class DistributedTaskManagerModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new HostnameModule());
        install(new TaskSerializationModule());
        install(new PostgresTaskExecutionDetailsProjectionGuiceModule());

        bind(EventSourcingTaskManager.class).in(Scopes.SINGLETON);
        bind(RabbitMQWorkQueueSupplier.class).in(Scopes.SINGLETON);
        bind(RabbitMQTerminationSubscriber.class).in(Scopes.SINGLETON);
        bind(TerminationSubscriber.class).to(RabbitMQTerminationSubscriber.class);
        bind(TaskManager.class).to(EventSourcingTaskManager.class);
        bind(WorkQueueSupplier.class).to(RabbitMQWorkQueueSupplier.class);
        bind(CancelRequestQueueName.class).toInstance(CancelRequestQueueName.generate());
        bind(TerminationQueueName.class).toInstance(TerminationQueueName.generate());

        Multibinder<SimpleConnectionPool.ReconnectionHandler> reconnectionHandlerMultibinder = Multibinder.newSetBinder(binder(), SimpleConnectionPool.ReconnectionHandler.class);
        reconnectionHandlerMultibinder.addBinding().to(RabbitMQWorkQueueReconnectionHandler.class);
        reconnectionHandlerMultibinder.addBinding().to(TerminationReconnectionHandler.class);

        Multibinder.newSetBinder(binder(), HealthCheck.class)
            .addBinding()
            .to(DistributedTaskManagerHealthCheck.class);
    }

    @Provides
    @Singleton
    private RabbitMQWorkQueueConfiguration getWorkQueueConfiguration(PropertiesProvider propertiesProvider) throws ConfigurationException {
        try {
            Configuration configuration = propertiesProvider.getConfiguration(RABBITMQ_CONFIGURATION_NAME);
            return RabbitMQWorkQueueConfiguration$.MODULE$.from(configuration);
        } catch (FileNotFoundException e) {
            return RabbitMQWorkQueueConfiguration$.MODULE$.enabled();
        }
    }

    @ProvidesIntoSet
    InitializationOperation terminationSubscriber(RabbitMQTerminationSubscriber instance) {
        return InitilizationOperationBuilder
            .forClass(RabbitMQTerminationSubscriber.class)
            .init(instance::start);
    }

    @ProvidesIntoSet
    InitializationOperation workQueue(EventSourcingTaskManager instance) {
        return InitilizationOperationBuilder
            .forClass(RabbitMQWorkQueue.class)
            .init(instance::start);
    }

}
