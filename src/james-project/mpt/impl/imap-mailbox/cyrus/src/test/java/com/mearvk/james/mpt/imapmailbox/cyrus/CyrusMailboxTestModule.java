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

package com.mearvk.james.mpt.imapmailbox.cyrus;

import com.mearvk.james.mpt.api.HostSystem;
import com.mearvk.james.mpt.api.ImapHostSystem;
import com.mearvk.james.mpt.api.UserAdder;
import com.mearvk.james.mpt.host.ExternalHostSystem;
import com.mearvk.james.mpt.imapmailbox.GrantRightsOnHost;
import com.mearvk.james.mpt.imapmailbox.MailboxMessageAppender;
import com.mearvk.james.mpt.imapmailbox.cyrus.host.CyrusHostSystem;
import com.mearvk.james.mpt.imapmailbox.cyrus.host.CyrusUserAdder;
import com.mearvk.james.mpt.imapmailbox.cyrus.host.Docker;
import com.mearvk.james.mpt.imapmailbox.cyrus.host.GrantRightsOnCyrusHost;
import com.mearvk.james.mpt.imapmailbox.cyrus.host.MailboxMessageAppenderOnCyrusHost;

import com.google.inject.AbstractModule;
import com.spotify.docker.client.messages.ContainerCreation;

public class CyrusMailboxTestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Docker.class).toInstance(new Docker("linagora/cyrus-imap"));
        bind(ContainerCreation.class).toProvider(CyrusHostSystem.class);
        bind(ImapHostSystem.class).to(CyrusHostSystem.class);
        bind(HostSystem.class).to(CyrusHostSystem.class);
        bind(ExternalHostSystem.class).to(CyrusHostSystem.class);
        bind(UserAdder.class).to(CyrusUserAdder.class);
        bind(GrantRightsOnHost.class).to(GrantRightsOnCyrusHost.class);
        bind(MailboxMessageAppender.class).to(MailboxMessageAppenderOnCyrusHost.class);
    }
}
