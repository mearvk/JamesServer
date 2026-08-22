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

package com.mearvk.james.mpt.host;

import com.mearvk.james.core.Username;
import com.mearvk.james.core.quota.QuotaSizeLimit;
import com.mearvk.james.managesieve.core.CoreProcessor;
import com.mearvk.james.managesieve.jsieve.Parser;
import com.mearvk.james.managesieve.transcode.ArgumentParser;
import com.mearvk.james.managesieve.transcode.ManageSieveProcessor;
import com.mearvk.james.mpt.api.Continuation;
import com.mearvk.james.mpt.api.Session;
import com.mearvk.james.sieverepository.api.SieveRepository;
import com.mearvk.james.user.api.UsersRepository;
import org.apache.jsieve.ConfigurationManager;

public abstract class JamesManageSieveHostSystem implements ManageSieveHostSystem {

    private UsersRepository usersRepository;
    private SieveRepository sieveRepository;
    private ManageSieveProcessor processor;

    @Override
    public void beforeTest() throws Exception {
        this.usersRepository = createUsersRepository();
        this.sieveRepository = createSieveRepository();
        this.processor = new ManageSieveProcessor(new ArgumentParser(new CoreProcessor(sieveRepository, usersRepository, new Parser(new ConfigurationManager()))));
    }

    @Override
    public void afterTest() throws Exception {
    }
    
    protected abstract SieveRepository createSieveRepository() throws Exception;

    protected abstract UsersRepository createUsersRepository();

    @Override
    public boolean addUser(Username username, String password) throws Exception {
        usersRepository.addUser(username, password);
        return true;
    }

    @Override
    public void setMaxQuota(String user, long value) throws Exception {
        sieveRepository.setQuota(Username.of(user), QuotaSizeLimit.size(value));
    }

    @Override
    public Session newSession(Continuation continuation) {
        return new ManageSieveSession(processor, continuation);
    }

}
