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
import static com.mearvk.mailet.base.MailAddressFixture.OTHER_AT_JAMES;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.MessagingException;

import com.mearvk.mailet.Mail;
import com.mearvk.mailet.Matcher;
import com.mearvk.mailet.base.test.FakeMail;
import com.mearvk.mailet.base.test.FakeMatcherConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AllTest {

    private Matcher matcher;

    @BeforeEach
    public void setupMatcher() throws MessagingException {
        matcher = new All();
        FakeMatcherConfig mci = FakeMatcherConfig.builder()
                .matcherName("All")
                .build();

        matcher.init(mci);
    }

    @Test
    public void testAllRecipientsReturned() throws MessagingException {
        Mail mail = FakeMail.builder()
            .name("mail")
            .recipients(ANY_AT_JAMES, OTHER_AT_JAMES)
            .build();

        assertThat(matcher.match(mail)).containsOnly(ANY_AT_JAMES, OTHER_AT_JAMES);
    }

}
