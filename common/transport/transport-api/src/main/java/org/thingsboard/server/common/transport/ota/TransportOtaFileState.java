/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.ota;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class TransportOtaFileState {

    private final Lock lock = new ReentrantLock();
    private final TenantId tenantId;
    private final OtaPackageId otaId;
    private final Path filePath;

    private boolean loaded;
    private SettableFuture<Void> loadFuture;

    private long lastActivityTime;

    public void updateLastActivityTime() {
        lastActivityTime = System.currentTimeMillis();
    }

    public boolean exists() {
        return filePath.toFile().exists();
    }

    public File getFile() {
        return filePath.toFile();
    }

}
