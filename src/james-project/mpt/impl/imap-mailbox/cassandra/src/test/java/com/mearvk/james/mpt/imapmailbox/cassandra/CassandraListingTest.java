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

package com.mearvk.james.mpt.imapmailbox.cassandra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import com.mearvk.james.backends.cassandra.StatementRecorder;
import com.mearvk.james.mpt.api.ImapHostSystem;
import com.mearvk.james.mpt.imapmailbox.cassandra.host.CassandraHostSystem;
import com.mearvk.james.mpt.imapmailbox.cassandra.host.CassandraHostSystemExtension;
import com.mearvk.james.mpt.imapmailbox.suite.Listing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CassandraListingTest extends Listing {
    @RegisterExtension
    static CassandraHostSystemExtension hostSystemExtension = new CassandraHostSystemExtension();

    @Override
    protected ImapHostSystem createImapHostSystem() {
        return hostSystemExtension.getImapHostSystem();
    }

    @Test
    void listShouldNotReadCounters() throws Exception {
        CassandraHostSystem cassandraHostSystem = (CassandraHostSystem) this.system;
        StatementRecorder statementRecorder = cassandraHostSystem.getCassandra()
            .getConf()
            .recordStatements();

        simpleScriptedTestProtocol
            .withLocale(Locale.US)
            .run("ListOnly");

        assertThat(statementRecorder.listExecutedStatements(StatementRecorder.Selector.preparedStatement(
                "SELECT unseen,count FROM mailboxCounters WHERE mailboxId=:mailboxId;")))
            .isEmpty();
    }
}
