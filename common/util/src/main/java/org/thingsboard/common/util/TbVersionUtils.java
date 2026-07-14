/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

public class TbVersionUtils {

    private TbVersionUtils() {
    }

    /**
     * Compares two dot-separated numeric version strings component-wise, treating missing trailing components as
     * zero (so {@code "3.6"} equals {@code "3.6.0"}). A {@code null} or empty string is treated as the lowest version.
     *
     * @return a negative int, zero, or a positive int as {@code v1} is less than, equal to, or greater than {@code v2}
     */
    public static int compare(String v1, String v2) {
        String[] parts1 = split(v1);
        String[] parts2 = split(v2);
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    /**
     * Strips any suffix that follows the leading dotted-numeric part of a version string
     * (e.g. {@code "4.3.1.1PE-SNAPSHOT"} -> {@code "4.3.1.1"}). Returns an empty string for a {@code null} input.
     */
    public static String extractStartingDigits(String version) {
        return version == null ? "" : version.replaceAll("[^0-9.].*$", "");
    }

    private static String[] split(String version) {
        return version == null || version.isEmpty() ? new String[0] : version.split("\\.");
    }

}
