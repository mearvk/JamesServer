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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;

import jakarta.mail.MessagingException;

import com.mearvk.james.core.MailAddress;
import com.mearvk.james.core.builder.MimeMessageBuilder;
import com.mearvk.mailet.base.test.FakeMail;
import com.mearvk.mailet.base.test.FakeMatcherConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TooManyLinesTest {

    private TooManyLines testee;

    @BeforeEach
    void setUp() {
        testee = new TooManyLines();
    }

    @Test
    void initShouldThrowOnAbsentCondition() {
        assertThatThrownBy(() ->
            testee.init(FakeMatcherConfig.builder().matcherName("name").build()))
        .isInstanceOf(MessagingException.class);
    }

    @Test
    void initShouldThrowOnInvalidCondition() {
        assertThatThrownBy(() ->
            testee.init(
                FakeMatcherConfig.builder()
                    .condition("a")
                    .matcherName("name")
                    .build()))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    void initShouldThrowOnEmptyCondition() {
        assertThatThrownBy(() ->
            testee.init(FakeMatcherConfig.builder()
                .condition("")
                .matcherName("name")
                .build()))
        .isInstanceOf(MessagingException.class);
    }

    @Test
    void initShouldThrowOnZeroCondition() {
        assertThatThrownBy(() ->
            testee.init(FakeMatcherConfig.builder()
                .condition("0")
                .matcherName("name")
                .build()))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    void initShouldThrowOnNegativeCondition() {
        assertThatThrownBy(() ->
            testee.init(FakeMatcherConfig.builder()
                .condition("-10")
                .matcherName("name")
                .build()))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    void matchShouldReturnNoRecipientWhenMailHaveNoMimeMessageAndConditionIs100() throws Exception {
        testee.init(FakeMatcherConfig.builder()
            .condition("100")
            .matcherName("name")
            .build());

        Collection<MailAddress> result = testee.match(FakeMail.builder().name("mail").build());

        assertThat(result).isEmpty();

    }

    @Test
    void matchShouldAcceptMailsUnderLimit() throws Exception {
        testee.init(FakeMatcherConfig.builder()
            .condition("100")
            .matcherName("name")
            .build());

        FakeMail fakeMail = FakeMail.builder()
            .name("mail")
            .mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
                .setMultipartWithBodyParts(MimeMessageBuilder.bodyPartBuilder()
                    .data("content")))
            .build();

        Collection<MailAddress> result = testee.match(fakeMail);

        assertThat(result).isEmpty();
    }

    @Test
    void matchShouldRejectMailsOverLimit() throws Exception {
        testee.init(FakeMatcherConfig.builder().condition("10").matcherName("name").build());

        FakeMail fakeMail = FakeMail.builder()
            .name("mail")
            .mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
                .setMultipartWithBodyParts(
                    MimeMessageBuilder.bodyPartBuilder()
                        .data("1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11")))
            .build();

        Collection<MailAddress> result = testee.match(fakeMail);

        assertThat(result).containsAll(fakeMail.getRecipients());
    }

}
