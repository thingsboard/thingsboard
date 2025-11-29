/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

@Slf4j
    /**
 * Utility class that provides helper methods for retrieving 
 * system and environment information such as OS name, CPU details, 
 * and memory usage. 
 * 
 * These methods are used across ThingsBoard to avoid repeating 
 * low-level system code in multiple places, improving maintainability 
 * and readability.
 */

public class SystemUtil {

    private static final HardwareAbstractionLayer HARDWARE;

    static {
        HARDWARE = new SystemInfo().getHardware();
    }
/**
 * Returns the percentage of used physical memory on the system.
 *
 * @return Optional containing memory usage as an integer percentage (0–100);
 *         Optional.empty() if the value cannot be retrieved
 */

    public static Optional<Integer> getMemoryUsage() {
        try {
            GlobalMemory memory = HARDWARE.getMemory();
            long total = memory.getTotal();
            long available = memory.getAvailable();
            return Optional.of(toPercent(total - available, total));
        } catch (Throwable e) {
            log.debug("Failed to get memory usage!!!", e);
        }
        return Optional.empty();
    }
/**
 * Returns the total amount of physical memory available on the machine.
 *
 * @return Optional containing total memory in bytes;
 *         Optional.empty() if the value cannot be retrieved
 */

    public static Optional<Long> getTotalMemory() {
        try {
            return Optional.of(HARDWARE.getMemory().getTotal());
        } catch (Throwable e) {
            log.debug("Failed to get total memory!!!", e);
        }
        return Optional.empty();
    }
/**
 * Returns the current system-wide CPU usage.
 *
 * @return Optional containing CPU usage as a percentage (0–100);
 *         Optional.empty() if the value cannot be retrieved
 */

    public static Optional<Integer> getCpuUsage() {
        try {
            return Optional.of((int) (HARDWARE.getProcessor().getSystemCpuLoad(1000) * 100.0));
        } catch (Throwable e) {
            log.debug("Failed to get cpu usage!!!", e);
        }
        return Optional.empty();
    }
/**
 * Returns the number of logical CPU cores available on the system.
 *
 * @return Optional containing logical processor count;
 *         Optional.empty() if it cannot be retrieved
 */

    public static Optional<Integer> getCpuCount() {
        try {
            return Optional.of(HARDWARE.getProcessor().getLogicalProcessorCount());
        } catch (Throwable e) {
            log.debug("Failed to get total cpu count!!!", e);
        }
        return Optional.empty();
    }
/**
 * Returns the percentage of disk space currently used on the root file system.
 *
 * @return Optional containing disk usage as an integer percentage (0–100);
 *         Optional.empty() if the value cannot be retrieved
 */

    public static Optional<Integer> getDiscSpaceUsage() {
        try {
            FileStore store = Files.getFileStore(Paths.get("/"));
            long total = store.getTotalSpace();
            long available = store.getUsableSpace();
            return Optional.of(toPercent(total - available, total));
        } catch (Throwable e) {
            log.debug("Failed to get free disc space!!!", e);
        }
        return Optional.empty();
    }
/**
 * Returns the total disk capacity of the root file system.
 *
 * @return Optional containing total disk space in bytes;
 *         Optional.empty() if the value cannot be retrieved
 */

    public static Optional<Long> getTotalDiscSpace() {
        try {
            FileStore store = Files.getFileStore(Paths.get("/"));
            return Optional.of(store.getTotalSpace());
        } catch (Throwable e) {
            log.debug("Failed to get total disc space!!!", e);
        }
        return Optional.empty();
    }

    private static int toPercent(long used, long total) {
        BigDecimal u = new BigDecimal(used);
        BigDecimal t = new BigDecimal(total);
        BigDecimal i = new BigDecimal(100);
        return u.multiply(i).divide(t, RoundingMode.HALF_UP).intValue();
    }
}
