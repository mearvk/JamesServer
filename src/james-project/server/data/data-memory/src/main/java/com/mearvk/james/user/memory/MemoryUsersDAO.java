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

package com.mearvk.james.user.memory;

import static com.mearvk.james.user.lib.model.Algorithm.HashingMode.PLAIN;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import com.mearvk.james.core.Username;
import com.mearvk.james.lifecycle.api.Configurable;
import com.mearvk.james.user.api.UsersRepositoryException;
import com.mearvk.james.user.api.model.User;
import com.mearvk.james.user.lib.UsersDAO;
import com.mearvk.james.user.lib.model.Algorithm;
import com.mearvk.james.user.lib.model.DefaultUser;

public class MemoryUsersDAO implements UsersDAO, Configurable {
    private final Map<String, User> userByName;
    private Algorithm algo;

    MemoryUsersDAO() {
        this.userByName = new HashMap<>();
        this.algo = Algorithm.of("PBKDF2");
    }

    @Override
    public void configure(HierarchicalConfiguration<ImmutableNode> config) {
        algo = Algorithm.of(config.getString("algorithm", "PBKDF2"), config.getString("hashingMode", PLAIN.name()));
    }

    public void clear() {
        userByName.clear();
    }

    @Override
    public void addUser(Username username, String password) {
        DefaultUser user = new DefaultUser(username, algo, algo);
        user.setPassword(password);
        userByName.put(username.asString(), user);
    }

    @Override
    public Optional<User> getUserByName(Username name) throws UsersRepositoryException {
        return Optional.ofNullable(userByName.get(name.asString()));
    }

    @Override
    public void updateUser(User user) throws UsersRepositoryException {
        getUserByName(user.getUserName())
            .orElseThrow(() -> new UsersRepositoryException("Please provide an existing user to update"));
        userByName.put(user.getUserName().asString(), user);
    }

    @Override
    public void removeUser(Username name) throws UsersRepositoryException {
        if (userByName.remove(name.asString()) == null) {
            throw new UsersRepositoryException("unable to remove unknown user " + name.asString());
        }
    }

    @Override
    public boolean contains(Username name) {
        return userByName.containsKey(name.asString());
    }

    @Override
    public int countUsers() {
        return userByName.size();
    }

    @Override
    public Iterator<Username> list() {
        return userByName.keySet()
            .stream()
            .map(Username::of)
            .iterator();
    }
}
