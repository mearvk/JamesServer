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

package com.mearvk.james.transport.mailets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.mail.MessagingException;

import com.mearvk.james.core.MailAddress;
import com.mearvk.james.core.builder.MimeMessageBuilder;
import com.mearvk.james.transport.mailets.utils.MimeMessageUtils;
import com.mearvk.mailet.Mail;
import com.mearvk.mailet.PerRecipientHeaders.Header;
import com.mearvk.mailet.base.GenericMailet;
import com.mearvk.mailet.base.test.FakeMail;
import com.mearvk.mailet.base.test.FakeMailetConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoveMimeHeaderByPrefixTest {

    private static final String PREFIX = "X-OPENPAAS-";
    private static final String HEADER_NAME_PREFIX_1 = "X-OPENPAAS-FEATURE-A";
    private static final String HEADER_NAME_PREFIX_2 = "X-OPENPAAS-FEATURE-B";
    private static final String HEADER_NAME_NO_PREFIX = "X-OTHER-BUSINESS";
    private static final String RECIPIENT1 = "r1@example.com";
    private static final String RECIPIENT2 = "r2@example.com";
    private static final String RECIPIENT3 = "r3@example.com";

    private GenericMailet mailet;

    @BeforeEach
    void setup() {
        mailet = new RemoveMimeHeaderByPrefix();
    }

    @Test
    void getMailetInfoShouldReturnValue() {
        assertThat(mailet.getMailetInfo()).isEqualTo("RemoveMimeHeaderByPrefix Mailet");
    }

    @Test
    void serviceShouldRemoveHeaderWhenPrefixed() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("prefix", PREFIX)
                .build();
        mailet.init(mailetConfig);

        Mail mail = FakeMail.fromMessage(MimeMessageBuilder.mimeMessageBuilder()
            .addHeader(HEADER_NAME_PREFIX_1, "true"));

        mailet.service(mail);

        assertThat(new MimeMessageUtils(mail.getMessage()).toHeaderList())
            .extracting("name")
            .doesNotContain(HEADER_NAME_PREFIX_1);
    }

    @Test
    void serviceShouldRemoveAllPrefixedHeaders() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty("prefix", PREFIX)
            .build();
        mailet.init(mailetConfig);

        Mail mail = FakeMail.fromMessage(MimeMessageBuilder.mimeMessageBuilder()
            .addHeader(HEADER_NAME_PREFIX_1, "true")
            .addHeader(HEADER_NAME_PREFIX_2, "true"));

        mailet.service(mail);

        assertThat(new MimeMessageUtils(mail.getMessage()).toHeaderList())
            .extracting("name")
            .doesNotContain(HEADER_NAME_PREFIX_1, HEADER_NAME_PREFIX_2);
    }
    
    @Test
    void serviceShouldRemoveAllPrefixedHeadersMixed() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("prefix", PREFIX)
                .build();
        mailet.init(mailetConfig);
        
        Mail mail = FakeMail.fromMessage(MimeMessageBuilder.mimeMessageBuilder()
                .addHeader(HEADER_NAME_PREFIX_1, "true"));
        mail.addSpecificHeaderForRecipient(Header.builder().name(HEADER_NAME_PREFIX_2).value("1").build(), new MailAddress(RECIPIENT1));
        mail.addSpecificHeaderForRecipient(Header.builder().name(PREFIX).value("1").build(), new MailAddress(RECIPIENT2));
        mail.addSpecificHeaderForRecipient(Header.builder().name(HEADER_NAME_NO_PREFIX).value("1").build(), new MailAddress(RECIPIENT3));
        
        mailet.service(mail);
        
        assertThat(new MimeMessageUtils(mail.getMessage()).toHeaderList())
                .extracting("name")
                .doesNotContain(PREFIX, HEADER_NAME_PREFIX_1, HEADER_NAME_PREFIX_2);

        assertThat(mail.getPerRecipientSpecificHeaders().getHeaderNamesForRecipient(new MailAddress(RECIPIENT1))).isEmpty();
        assertThat(mail.getPerRecipientSpecificHeaders().getHeaderNamesForRecipient(new MailAddress(RECIPIENT2))).isEmpty();
        assertThat(mail.getPerRecipientSpecificHeaders().getHeaderNamesForRecipient(new MailAddress(RECIPIENT3))).isNotEmpty();
    }

    @Test
    void serviceShouldNotRemoveNonPrefixedHeaders() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty("prefix", PREFIX)
            .build();
        mailet.init(mailetConfig);

        Mail mail = FakeMail.fromMessage(MimeMessageBuilder.mimeMessageBuilder()
            .addHeader(HEADER_NAME_PREFIX_1, "true")
            .addHeader(HEADER_NAME_NO_PREFIX, "true"));

        mailet.service(mail);

        assertThat(new MimeMessageUtils(mail.getMessage()).toHeaderList())
            .extracting("name")
            .contains(HEADER_NAME_NO_PREFIX)
            .doesNotContain(HEADER_NAME_PREFIX_1);
    }

    @Test
    void exactMatchOfPrefixShouldBeAllowed() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty("prefix", PREFIX)
            .build();
        mailet.init(mailetConfig);

        Mail mail = FakeMail.fromMessage(MimeMessageBuilder.mimeMessageBuilder()
            .addHeader(PREFIX, "true")
            .addHeader(HEADER_NAME_NO_PREFIX, "true"));

        mailet.service(mail);

        assertThat(new MimeMessageUtils(mail.getMessage()).toHeaderList())
            .extracting("name")
            .doesNotContain(PREFIX);
        assertThat(mail.getMessage().getHeader(PREFIX)).isNull();
    }


    @Test
    void initShouldThrowWhenInvalidConfig() {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .build();
        assertThatThrownBy(() -> mailet.init(mailetConfig)).isInstanceOf(MessagingException.class);
    }

    @Test
    void initShouldThrowWhenPrefixEmpty() {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty(RemoveMimeHeaderByPrefix.PREFIX, "")
            .build();
        assertThatThrownBy(() -> mailet.init(mailetConfig)).isInstanceOf(MessagingException.class);
    }
}
