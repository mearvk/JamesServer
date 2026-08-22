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


package com.mearvk.james.transport.matchers;

import static com.mearvk.mailet.base.MailAddressFixture.ANY_AT_JAMES;
import static com.mearvk.mailet.base.MailAddressFixture.ANY_AT_JAMES2;
import static com.mearvk.mailet.base.MailAddressFixture.JAMES2_APACHE_ORG;
import static com.mearvk.mailet.base.MailAddressFixture.JAMES_APACHE_ORG;
import static com.mearvk.mailet.base.MailAddressFixture.OTHER_AT_JAMES;
import static com.mearvk.mailet.base.MailAddressFixture.OTHER_AT_JAMES2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.mail.MessagingException;

import com.mearvk.james.core.Domain;
import com.mearvk.mailet.Mail;
import com.mearvk.mailet.MailetContext;
import com.mearvk.mailet.Matcher;
import com.mearvk.mailet.base.test.FakeMail;
import com.mearvk.mailet.base.test.FakeMatcherConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HostIsLocalTest {

    private Matcher matcher;

    @BeforeEach
    public void setUp() throws Exception {
        MailetContext mailContext = mock(MailetContext.class);
        when(mailContext.isLocalServer(Domain.of(JAMES_APACHE_ORG))).thenReturn(true);
        when(mailContext.isLocalServer(Domain.of(JAMES2_APACHE_ORG))).thenReturn(false);

        matcher = new HostIsLocal();
        FakeMatcherConfig mci = FakeMatcherConfig.builder()
                .matcherName("HostIsLocal")
                .mailetContext(mailContext)
                .build();

        matcher.init(mci);
    }

    @Test
    public void shouldMatchAddressesFromLocalDomain() throws MessagingException {
        Mail mail = FakeMail.builder()
            .name("mail")
            .recipients(ANY_AT_JAMES, OTHER_AT_JAMES)
            .build();

        assertThat(matcher.match(mail)).containsExactly(ANY_AT_JAMES, OTHER_AT_JAMES);
    }

    @Test
    public void shouldMatchOnlyAddressesFromLocalDomain() throws MessagingException {
        Mail mail = FakeMail.builder()
            .name("mail")
            .recipients(ANY_AT_JAMES, ANY_AT_JAMES2)
            .build();

        assertThat(matcher.match(mail)).containsExactly(ANY_AT_JAMES);
    }

    @Test
    public void shouldNotMatchAddressesFromDistantDomains() throws MessagingException {
        Mail mail = FakeMail.builder()
            .name("mail")
            .recipients(ANY_AT_JAMES2, OTHER_AT_JAMES2)
            .build();

        assertThat(matcher.match(mail)).isEmpty();
    }
}
