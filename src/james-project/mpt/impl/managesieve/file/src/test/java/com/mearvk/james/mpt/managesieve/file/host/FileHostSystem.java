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

package com.mearvk.james.mpt.managesieve.file.host;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import com.mearvk.james.domainlist.api.DomainList;
import com.mearvk.james.filesystem.api.FileSystem;
import com.mearvk.james.mpt.host.JamesManageSieveHostSystem;
import com.mearvk.james.sieverepository.api.SieveRepository;
import com.mearvk.james.sieverepository.file.SieveFileRepository;
import com.mearvk.james.user.api.UsersRepository;
import com.mearvk.james.user.memory.MemoryUsersRepository;

public class FileHostSystem extends JamesManageSieveHostSystem {

    private static final String SIEVE_ROOT = FileSystem.FILE_PROTOCOL + "sieve";
    private static final FileSystem fileSystem = getFileSystem();
    private static final DomainList NO_DOMAIN_LIST = null;

    private static FileSystem getFileSystem() {
        return new FileSystem() {
            @Override
            public File getBasedir() throws FileNotFoundException {
                return new File(System.getProperty("java.io.tmpdir"));
            }
            
            @Override
            public InputStream getResource(String url) throws IOException {
                return new FileInputStream(getFile(url));
            }
            
            @Override
            public File getFile(String fileURL) throws FileNotFoundException {
                return new File(getBasedir(), fileURL.substring(FileSystem.FILE_PROTOCOL.length()));
            }
        };
    }

    @Override
    protected UsersRepository createUsersRepository() {
        return MemoryUsersRepository.withoutVirtualHosting(NO_DOMAIN_LIST);
    }

    @Override
    protected SieveRepository createSieveRepository() throws Exception {
        return new SieveFileRepository(fileSystem);
    }
    
    @Override
    public void afterTest() throws Exception {
        super.afterTest();
        File root = fileSystem.getFile(SIEVE_ROOT);
        // Remove files from the previous test, if any
        if (root.exists()) {
            FileUtils.forceDelete(root);
        }
    }
}
