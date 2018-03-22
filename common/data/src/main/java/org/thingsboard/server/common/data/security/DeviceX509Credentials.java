/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.security;

/**
 * @author Valerii Sosliuk
 */
public class DeviceX509Credentials implements DeviceCredentialsFilter {

    private final String sha3Hash;

    public DeviceX509Credentials(String sha3Hash) {
        this.sha3Hash = sha3Hash;
    }

    @Override
    public String getCredentialsId() { return sha3Hash; }

    @Override
    public DeviceCredentialsType getCredentialsType() { return DeviceCredentialsType.X509_CERTIFICATE; }

    @Override
    public String toString() {
        return "DeviceX509Credentials [SHA3=" + sha3Hash + "]";
    }
}
