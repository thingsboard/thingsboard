/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.common.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class SystemUtil {

    public static Long getMemoryUsage() {
        try {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            return memoryMXBean.getHeapMemoryUsage().getUsed();
        } catch (Exception e) {
            log.debug("Failed to get memory usage!!!", e);
        }
        return null;
    }

    public static Double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            return osBean.getSystemLoadAverage();
        } catch (Exception e) {
            log.debug("Failed to get cpu usage!!!", e);
        }
        return null;
    }

    public static Long getFreeDiscSpace() {
        try {
            FileStore store = Files.getFileStore(Paths.get("/"));
            return store.getUsableSpace();
        } catch (Exception e) {
            log.debug("Failed to get free disc space!!!", e);
        }
        return null;
    }
}
