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
package com.mearvk.james.mpt.smtp;

import java.util.Optional;

import com.mearvk.james.GuiceJamesServer;
import com.mearvk.james.JamesServerExtension;
import com.mearvk.james.RegistrableExtension;
import com.mearvk.james.core.Domain;
import com.mearvk.james.core.Username;
import com.mearvk.james.modules.protocols.SmtpGuiceProbe;
import com.mearvk.james.mpt.api.Continuation;
import com.mearvk.james.mpt.api.Session;
import com.mearvk.james.mpt.monitor.SystemLoggingMonitor;
import com.mearvk.james.mpt.session.ExternalSessionFactory;
import com.mearvk.james.util.Port;
import com.mearvk.james.utils.DataProbeImpl;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import com.google.common.base.Preconditions;


public class SmtpTestExtension implements RegistrableExtension {

    private final SmtpGuiceProbe.SmtpServerConnectedType smtpServerConnectedType;
    private final JamesServerExtension testExtension;
    private SmtpHostSystem smtpHostSystem;

    public SmtpTestExtension(SmtpGuiceProbe.SmtpServerConnectedType smtpServerConnectedType, JamesServerExtension testExtension) {
        this.smtpServerConnectedType = smtpServerConnectedType;
        this.testExtension = testExtension;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        smtpHostSystem = new HostSystem(testExtension.getGuiceJamesServer(), smtpServerConnectedType);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return (parameterContext.getParameter().getType().isAssignableFrom(SmtpHostSystem.class));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return smtpHostSystem;
    }


    public static class HostSystem implements SmtpHostSystem {

        private final ExternalSessionFactory sessionFactory;
        private final SmtpGuiceProbe.SmtpServerConnectedType smtpServerConnectedType;
        private final GuiceJamesServer jamesServer;

        public HostSystem(GuiceJamesServer jamesServer, SmtpGuiceProbe.SmtpServerConnectedType smtpServerConnectedType) {
            this.jamesServer = jamesServer;
            this.smtpServerConnectedType = smtpServerConnectedType;
            SmtpGuiceProbe smtpProbe = jamesServer.getProbe(SmtpGuiceProbe.class);
            Port smtpPort = this.smtpServerConnectedType.getPortExtractor().apply(smtpProbe);
            sessionFactory = new ExternalSessionFactory("localhost", smtpPort, new SystemLoggingMonitor(), "220 mydomain.tld smtp");
        }

        @Override
        public boolean addUser(Username userAtDomain, String password) throws Exception {
            Optional<Domain> domain = userAtDomain.getDomainPart();
            Preconditions.checkArgument(domain.isPresent(), "The 'user' should contain the 'domain'");
            createDomainIfNeeded(domain.get().asString());
            jamesServer.getProbe(DataProbeImpl.class).addUser(userAtDomain.asString(), password);
            return true;
        }

        @Override
        public Session newSession(Continuation continuation) throws Exception {
            return sessionFactory.newSession(continuation);
        }

        private void createDomainIfNeeded(String domain) throws Exception {
            if (!jamesServer.getProbe(DataProbeImpl.class).containsDomain(domain)) {
                jamesServer.getProbe(DataProbeImpl.class).addDomain(domain);
            }
        }

        @Override
        public void addAddressMapping(String user, String domain, String address) throws Exception {
            jamesServer.getProbe(DataProbeImpl.class).addAddressMapping(user, domain, address);
        }

        @Override
        public void beforeTest() throws Exception {

        }

        @Override
        public void afterTest() throws Exception {

        }
    }
}
