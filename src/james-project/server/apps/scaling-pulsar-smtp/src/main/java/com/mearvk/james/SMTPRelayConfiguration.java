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

package com.mearvk.james;

import java.io.File;
import java.util.Optional;

import com.mearvk.james.filesystem.api.FileSystem;
import com.mearvk.james.filesystem.api.JamesDirectoriesProvider;
import com.mearvk.james.server.core.JamesServerResourceLoader;
import com.mearvk.james.server.core.MissingArgumentException;
import com.mearvk.james.server.core.configuration.Configuration;

import com.google.common.base.MoreObjects;

public class SMTPRelayConfiguration implements Configuration {
    public static class Builder {
        private Optional<String> rootDirectory;
        private Optional<ConfigurationPath> configurationPath;

        private Builder() {
            rootDirectory = Optional.empty();
            configurationPath = Optional.empty();
        }

        public Builder workingDirectory(String path) {
            rootDirectory = Optional.of(path);
            return this;
        }

        public Builder workingDirectory(File file) {
            rootDirectory = Optional.of(file.getAbsolutePath());
            return this;
        }

        public Builder useWorkingDirectoryEnvProperty() {
            rootDirectory = Optional.ofNullable(System.getProperty(WORKING_DIRECTORY));
            if (!rootDirectory.isPresent()) {
                throw new MissingArgumentException("Server needs a working.directory env entry");
            }
            return this;
        }

        public Builder configurationPath(ConfigurationPath path) {
            configurationPath = Optional.of(path);
            return this;
        }

        public Builder configurationFromClasspath() {
            configurationPath = Optional.of(new ConfigurationPath(FileSystem.CLASSPATH_PROTOCOL));
            return this;
        }


        public SMTPRelayConfiguration build() {
            ConfigurationPath configurationPath = this.configurationPath.orElse(new ConfigurationPath(FileSystem.FILE_PROTOCOL_AND_CONF));
            JamesServerResourceLoader directories = new JamesServerResourceLoader(rootDirectory
                .orElseThrow(() -> new MissingArgumentException("Server needs a working.directory env entry")));

            return new SMTPRelayConfiguration(configurationPath, directories);
        }
    }

    static SMTPRelayConfiguration.Builder builder() {
        return new Builder();
    }

    private final ConfigurationPath configurationPath;
    private final JamesDirectoriesProvider directories;

    private SMTPRelayConfiguration(ConfigurationPath configurationPath, JamesDirectoriesProvider directories) {
        this.configurationPath = configurationPath;
        this.directories = directories;
    }

    @Override
    public ConfigurationPath configurationPath() {
        return configurationPath;
    }

    @Override
    public JamesDirectoriesProvider directories() {
        return directories;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("configurationPath", configurationPath)
                .add("directories", directories)
                .toString();
    }
}
