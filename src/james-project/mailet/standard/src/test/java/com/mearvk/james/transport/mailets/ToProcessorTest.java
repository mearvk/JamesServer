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
import static org.mockito.Mockito.mock;

import jakarta.mail.MessagingException;

import com.mearvk.james.core.MailAddress;
import com.mearvk.mailet.Mail;
import com.mearvk.mailet.Mailet;
import com.mearvk.mailet.MailetException;
import com.mearvk.mailet.base.test.FakeMail;
import com.mearvk.mailet.base.test.FakeMailContext;
import com.mearvk.mailet.base.test.FakeMailetConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class ToProcessorTest {

    private Mailet mailet;
    private FakeMailContext mailContext;

    @BeforeEach
    void setup() {
        mailet = new ToProcessor();
        Logger logger = mock(Logger.class);
        mailContext = FakeMailContext.builder().logger(logger).build();
    }

    @Test
    void getMailetInfoShouldReturnValue() {
        assertThat(mailet.getMailetInfo()).isEqualTo("ToProcessor Mailet");
    }

    @Test
    void initShouldThrowWhenProcessorIsNotGiven() {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .mailetContext(mailContext)
                .setProperty("notice", "error in message")
                .build();
        assertThatThrownBy(() -> mailet.init(mailetConfig))
            .isInstanceOf(MailetException.class);
    }

    @Test
    void serviceShouldSetTheStateOfTheMail() throws MessagingException {
        String processor = "error";
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .mailetContext(mailContext)
                .setProperty("processor", processor)
                .build();
        mailet.init(mailetConfig);

        Mail mail = FakeMail.builder()
                .name("mail")
                .recipients(new MailAddress("test@james.apache.org"), new MailAddress("test2@james.apache.org"))
                .build();
        mailet.service(mail);

        assertThat(mail.getState()).isEqualTo(processor);
    }

    @Test
    void serviceShouldSetTheErrorMessageOfTheMailWhenNotAlreadySet() throws MessagingException {
        String processor = "error";
        String notice = "error in message";
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .mailetContext(mailContext)
                .setProperty("processor", processor)
                .setProperty("notice", notice)
                .build();
        mailet.init(mailetConfig);

        Mail mail = FakeMail.builder()
                .name("mail")
                .recipients(new MailAddress("test@james.apache.org"), new MailAddress("test2@james.apache.org"))
                .build();
        mailet.service(mail);

        assertThat(mail.getErrorMessage()).isEqualTo(notice);
    }

    @Test
    void serviceShouldAppendTheErrorMessageOfTheMailWhenSomeErrorMessageOnMail() throws MessagingException {
        String processor = "error";
        String notice = "error in message";
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .mailetContext(mailContext)
                .setProperty("processor", processor)
                .setProperty("notice", notice)
                .build();
        mailet.init(mailetConfig);

        Mail mail = FakeMail.builder()
                .name("mail")
                .recipients(new MailAddress("test@james.apache.org"), new MailAddress("test2@james.apache.org"))
                .build();
        String initialErrorMessage = "first";
        mail.setErrorMessage(initialErrorMessage);
        mailet.service(mail);

        assertThat(mail.getErrorMessage()).isEqualTo(initialErrorMessage + "\r\n" + notice);
    }
}
