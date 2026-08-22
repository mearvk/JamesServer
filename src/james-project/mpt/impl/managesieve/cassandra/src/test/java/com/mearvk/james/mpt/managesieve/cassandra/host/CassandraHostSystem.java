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

package com.mearvk.james.mpt.managesieve.cassandra.host;

import com.mearvk.james.backends.cassandra.CassandraCluster;
import com.mearvk.james.backends.cassandra.components.CassandraQuotaCurrentValueDao;
import com.mearvk.james.backends.cassandra.components.CassandraQuotaLimitDao;
import com.mearvk.james.domainlist.api.DomainList;
import com.mearvk.james.mpt.host.JamesManageSieveHostSystem;
import com.mearvk.james.sieve.cassandra.CassandraActiveScriptDAO;
import com.mearvk.james.sieve.cassandra.CassandraSieveDAO;
import com.mearvk.james.sieve.cassandra.CassandraSieveQuotaDAOV2;
import com.mearvk.james.sieve.cassandra.CassandraSieveRepository;
import com.mearvk.james.sieverepository.api.SieveRepository;
import com.mearvk.james.user.api.UsersRepository;
import com.mearvk.james.user.cassandra.CassandraUsersDAO;
import com.mearvk.james.user.lib.UsersRepositoryImpl;

public class CassandraHostSystem extends JamesManageSieveHostSystem {
    private static final DomainList NO_DOMAIN_LIST = null;
    private final CassandraCluster cassandra;

    public CassandraHostSystem(CassandraCluster cassandra) {
        this.cassandra = cassandra;
    }

    @Override
    protected SieveRepository createSieveRepository() {
        return new CassandraSieveRepository(
            new CassandraSieveDAO(cassandra.getConf()),
            new CassandraSieveQuotaDAOV2(new CassandraQuotaCurrentValueDao(cassandra.getConf()), new CassandraQuotaLimitDao(cassandra.getConf())),
            new CassandraActiveScriptDAO(cassandra.getConf()));
    }

    @Override
    protected UsersRepository createUsersRepository() {
        CassandraUsersDAO usersDAO = new CassandraUsersDAO(cassandra.getConf());
        UsersRepositoryImpl usersRepository = new UsersRepositoryImpl(NO_DOMAIN_LIST, usersDAO);
        usersRepository.setEnableVirtualHosting(false);
        return usersRepository;
    }
}
