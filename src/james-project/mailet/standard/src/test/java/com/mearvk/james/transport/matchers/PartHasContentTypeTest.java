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
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import com.mearvk.james.core.MailAddress;
import com.mearvk.james.core.builder.MimeMessageBuilder;
import com.mearvk.mailet.Mail;
import com.mearvk.mailet.base.test.FakeMail;
import com.mearvk.mailet.base.test.FakeMatcherConfig;
import org.junit.jupiter.api.Test;

class PartHasContentTypeTest {
    @Test
    void shouldNotMatchWhenNoContentType() throws Exception {
        PartHasContentType testee = new PartHasContentType();
        testee.init(FakeMatcherConfig.builder()
            .matcherName("PartHasContentType")
            .condition("image/png")
            .build());

        Mail mail = FakeMail.builder()
            .name("mail")
            .recipient(ANY_AT_JAMES)
            .mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
                .setMultipartWithBodyParts(
                    MimeMessageBuilder.bodyPartBuilder()
                        .disposition("attachment")
                        .filename("xxx.zip")))
            .build();
        Collection<MailAddress> matched = testee.match(mail);

        assertThat(matched)
            .isNull();
    }

    @Test
    void shouldNotMatchWhenWrongContentType() throws Exception {
        PartHasContentType testee = new PartHasContentType();
        testee.init(FakeMatcherConfig.builder()
            .matcherName("PartHasContentType")
            .condition("image/png")
            .build());

        Mail mail = FakeMail.builder()
            .name("mail")
            .recipient(ANY_AT_JAMES)
            .mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
                .setMultipartWithBodyParts(
                    MimeMessageBuilder.bodyPartBuilder()
                        .type("text/plain")
                        .disposition("attachment")
                        .filename("xxx.zip")))
            .build();
        Collection<MailAddress> matched = testee.match(mail);

        assertThat(matched)
            .isNull();
    }

    @Test
    void shouldMatchWhenGoodContentType() throws Exception {
        PartHasContentType testee = new PartHasContentType();
        testee.init(FakeMatcherConfig.builder()
            .matcherName("PartHasContentType")
            .condition("image/png")
            .build());

        Mail mail = FakeMail.builder()
            .name("mail")
            .recipient(ANY_AT_JAMES)
            .mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
                .setMultipartWithBodyParts(
                    MimeMessageBuilder.bodyPartBuilder()
                        .type("image/png")
                        .disposition("attachment")
                        .filename("xxx.zip")))
            .build();
        Collection<MailAddress> matched = testee.match(mail);

        assertThat(matched)
            .isNotEmpty();
    }
}