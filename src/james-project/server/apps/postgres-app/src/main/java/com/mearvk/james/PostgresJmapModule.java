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
import com.mearvk.james.eventsourcing.Event;
import com.mearvk.james.eventsourcing.eventstore.dto.EventDTO;
import com.mearvk.james.eventsourcing.eventstore.dto.EventDTOModule;
import com.mearvk.james.jmap.api.change.EmailChangeRepository;
import com.mearvk.james.jmap.api.change.Limit;
import com.mearvk.james.jmap.api.change.MailboxChangeRepository;
import com.mearvk.james.jmap.api.change.State;
import com.mearvk.james.jmap.api.filtering.FilteringRuleSetDefineDTOModules;
import com.mearvk.james.jmap.api.pushsubscription.PushSubscriptionRepository;
import com.mearvk.james.jmap.api.upload.UploadUsageRepository;
import com.mearvk.james.jmap.postgres.PostgresDataJMapAggregateDataDefinition;
import com.mearvk.james.jmap.postgres.change.PostgresEmailChangeRepository;
import com.mearvk.james.jmap.postgres.change.PostgresMailboxChangeRepository;
import com.mearvk.james.jmap.postgres.change.PostgresStateFactory;
import com.mearvk.james.jmap.postgres.pushsubscription.PostgresPushSubscriptionRepository;
import com.mearvk.james.jmap.postgres.upload.PostgresUploadUsageRepository;
import org.apache.james.mailbox.AttachmentManager;
import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.RightManager;
import org.apache.james.mailbox.store.StoreAttachmentManager;
import org.apache.james.mailbox.store.StoreMessageIdManager;
import org.apache.james.mailbox.store.StoreRightManager;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class PostgresJmapModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), PostgresDataDefinition.class).addBinding().toInstance(PostgresDataJMapAggregateDataDefinition.MODULE);

        bind(EmailChangeRepository.class).to(PostgresEmailChangeRepository.class);
        bind(PostgresEmailChangeRepository.class).in(Scopes.SINGLETON);

        bind(MailboxChangeRepository.class).to(PostgresMailboxChangeRepository.class);
        bind(PostgresMailboxChangeRepository.class).in(Scopes.SINGLETON);

        bind(Limit.class).annotatedWith(Names.named(PostgresEmailChangeRepository.LIMIT_NAME)).toInstance(Limit.of(256));
        bind(Limit.class).annotatedWith(Names.named(PostgresMailboxChangeRepository.LIMIT_NAME)).toInstance(Limit.of(256));

        bind(UploadUsageRepository.class).to(PostgresUploadUsageRepository.class);

        bind(MessageIdManager.class).to(StoreMessageIdManager.class);
        bind(AttachmentManager.class).to(StoreAttachmentManager.class);
        bind(StoreMessageIdManager.class).in(Scopes.SINGLETON);
        bind(StoreAttachmentManager.class).in(Scopes.SINGLETON);
        bind(RightManager.class).to(StoreRightManager.class);
        bind(StoreRightManager.class).in(Scopes.SINGLETON);

        bind(State.Factory.class).to(PostgresStateFactory.class);

        bind(PushSubscriptionRepository.class).to(PostgresPushSubscriptionRepository.class);

        Multibinder<EventDTOModule<? extends Event, ? extends EventDTO>> eventDTOModuleBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<>() {});
        eventDTOModuleBinder.addBinding().toInstance(FilteringRuleSetDefineDTOModules.FILTERING_RULE_SET_DEFINED);
        eventDTOModuleBinder.addBinding().toInstance(FilteringRuleSetDefineDTOModules.FILTERING_INCREMENT);
    }
}
