/******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                 *
 * or more contributor license agreements.  See the NOTICE file               *
 * distributed with this work for additional information                      *
 * regarding copyright ownership.  The ASF licenses this file                 *
 * to you under the Apache License, Version 2.0 (the                          *
 * "License"); you may not use this file except in compliance                 *
 * with the License.  You may obtain a copy of the License at                 *
 *                                                                            *
 *   http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing,                 *
 * software distributed under the License is distributed on an                *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                     *
 * KIND, either express or implied.  See the License for the                  *
 * specific language governing permissions and limitations                    *
 * under the License.                                                         *
 ******************************************************************************/

package com.mearvk.james.modules.data;

import com.mearvk.james.dlp.api.DLPConfigurationStore;
import com.mearvk.james.dlp.eventsourcing.EventSourcingDLPConfigurationStore;
import com.mearvk.james.dlp.eventsourcing.cassandra.DLPConfigurationModules;
import com.mearvk.james.eventsourcing.Event;
import com.mearvk.james.eventsourcing.eventstore.dto.EventDTO;
import com.mearvk.james.eventsourcing.eventstore.dto.EventDTOModule;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

//FIXME - I don't see what this has to do with postgres when it pulls stuff from data-cassandra ...
public class PostgresDLPConfigurationStoreModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EventSourcingDLPConfigurationStore.class).in(Scopes.SINGLETON);
        bind(DLPConfigurationStore.class).to(EventSourcingDLPConfigurationStore.class);
        Multibinder<EventDTOModule<? extends Event, ? extends EventDTO>> eventDTOModuleBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<>() {});
        eventDTOModuleBinder.addBinding().toInstance(DLPConfigurationModules.DLP_CONFIGURATION_STORE);
        eventDTOModuleBinder.addBinding().toInstance(DLPConfigurationModules.DLP_CONFIGURATION_CLEAR);
    }
}
