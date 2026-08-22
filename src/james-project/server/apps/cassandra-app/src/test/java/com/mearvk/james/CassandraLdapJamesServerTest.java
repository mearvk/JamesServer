/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package com.mearvk.james;

import static com.mearvk.james.user.ldap.DockerLdapSingleton.JAMES_USER;
import static com.mearvk.james.user.ldap.DockerLdapSingleton.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;

import java.time.Duration;

import org.apache.commons.net.imap.IMAPClient;
import com.mearvk.james.core.Domain;
import com.mearvk.james.data.LdapTestExtension;
import com.mearvk.james.data.UsersRepositoryModuleChooser;
import com.mearvk.james.junit.categories.BasicFeature;
import com.mearvk.james.modules.TestJMAPServerModule;
import com.mearvk.james.modules.protocols.ImapGuiceProbe;
import com.mearvk.james.modules.protocols.SmtpGuiceProbe;
import com.mearvk.james.utils.SMTPMessageSender;
import com.mearvk.james.utils.SMTPSendingException;
import com.mearvk.james.utils.SpoolerProbe;
import com.mearvk.james.utils.TestIMAPClient;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Tag(BasicFeature.TAG)
class CassandraLdapJamesServerTest implements JamesServerConcreteContract {
    private static Duration slowPacedPollInterval = ONE_HUNDRED_MILLISECONDS;
    private static ConditionFactory calmlyAwait = Awaitility.with()
        .pollInterval(slowPacedPollInterval)
        .and()
        .with()
        .pollDelay(slowPacedPollInterval)
        .await();
    private IMAPClient imapClient = new IMAPClient();

    @RegisterExtension
    TestIMAPClient testIMAPClient = new TestIMAPClient();
    SMTPMessageSender messageSender = new SMTPMessageSender(Domain.LOCALHOST.asString());
    static LdapTestExtension ldap = new LdapTestExtension();

    @RegisterExtension
    static JamesServerExtension testExtension = TestingDistributedJamesServerBuilder
        .forConfiguration(configuration -> configuration
            .searchConfiguration(SearchConfiguration.openSearch())
            .usersRepository(UsersRepositoryModuleChooser.Implementation.LDAP))
        .extension(new DockerOpenSearchExtension())
        .extension(new CassandraExtension())
        .extension(ldap)
        .server(configuration -> CassandraJamesServerMain.createServer(configuration)
            .overrideWith(new TestJMAPServerModule()))
        .build();

    @Test
    void userFromLdapShouldLoginViaImapProtocol(GuiceJamesServer server) throws Exception {
        imapClient.connect(JAMES_SERVER_HOST, server.getProbe(ImapGuiceProbe.class).getImapPort());

        assertThat(imapClient.login(JAMES_USER.asString(), PASSWORD)).isTrue();
    }

    @Test
    void mailsShouldBeWellReceivedBeforeFirstUserConnectionWithLdap(GuiceJamesServer server) throws Exception {
        messageSender.connect(JAMES_SERVER_HOST, server.getProbe(SmtpGuiceProbe.class).getSmtpPort())
            .sendMessage("bob@any.com", JAMES_USER.asString() + "@localhost");

        calmlyAwait.until(() -> server.getProbe(SpoolerProbe.class).processingFinished());

        testIMAPClient.connect(JAMES_SERVER_HOST, server.getProbe(ImapGuiceProbe.class).getImapPort())
            .login(JAMES_USER, PASSWORD)
            .select("INBOX")
            .awaitMessage(calmlyAwait);
    }

    @Test
    void receivingMailShouldIssueAnSmtpErrorWhenLdapIsNotAvailable(GuiceJamesServer server) {
        try {
            ldap.getLdapRule().stop();
            assertThatThrownBy(() -> messageSender.connect(JAMES_SERVER_HOST, server.getProbe(SmtpGuiceProbe.class).getSmtpPort())
                    .sendMessage("bob@any.com", JAMES_USER.asString() + "@localhost"))
                .isInstanceOf(SMTPSendingException.class);
        } finally {
            ldap.getLdapRule().start();
        }
    }
}
