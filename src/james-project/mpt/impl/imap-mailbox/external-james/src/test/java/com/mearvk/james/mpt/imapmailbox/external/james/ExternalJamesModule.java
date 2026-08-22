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

package com.mearvk.james.mpt.imapmailbox.external.james;

import com.mearvk.james.mpt.api.DomainAdder;
import com.mearvk.james.mpt.api.HostSystem;
import com.mearvk.james.mpt.api.ImapHostSystem;
import com.mearvk.james.mpt.api.UserAdder;
import com.mearvk.james.mpt.host.ExternalHostSystem;
import com.mearvk.james.mpt.imapmailbox.external.james.host.ProvisioningAPI;
import com.mearvk.james.mpt.imapmailbox.external.james.host.SmtpHostSystem;
import com.mearvk.james.mpt.imapmailbox.external.james.host.external.ExternalJamesConfiguration;
import com.mearvk.james.mpt.imapmailbox.external.james.host.external.ExternalJamesImapHostSystem;
import com.mearvk.james.mpt.imapmailbox.external.james.host.external.ExternalJamesSmtpHostSystem;

import com.google.inject.AbstractModule;

public class ExternalJamesModule extends AbstractModule {

    private final ExternalJamesConfiguration configuration;
    private final ProvisioningAPI provisioningAPI;

    public ExternalJamesModule(ExternalJamesConfiguration configuration, ProvisioningAPI provisioningAPI) {
        this.configuration = configuration;
        this.provisioningAPI = provisioningAPI;
    }

    @Override
    protected void configure() {
        bind(ExternalJamesConfiguration.class).toInstance(configuration);
        bind(ImapHostSystem.class).to(ExternalJamesImapHostSystem.class);
        bind(HostSystem.class).to(ExternalJamesImapHostSystem.class);
        bind(ExternalHostSystem.class).to(ExternalJamesImapHostSystem.class);
        bind(SmtpHostSystem.class).to(ExternalJamesSmtpHostSystem.class);
        bind(DomainAdder.class).toInstance(provisioningAPI);
        bind(UserAdder.class).toInstance(provisioningAPI);
    }

}
