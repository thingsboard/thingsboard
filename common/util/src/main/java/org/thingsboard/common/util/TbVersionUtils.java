// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.common.util;

public class TbVersionUtils {

    private TbVersionUtils() {
    }

    /**
     * Compares two dot-separated numeric version strings component-wise, treating missing trailing components as
     * zero (so {@code "3.6"} equals {@code "3.6.0"}). A {@code null} or empty string is treated as the lowest version.
     * Never throws on unparseable input: a component contributes only its leading digits, and a component with none
     * (a docker tag such as {@code "latest"} or {@code "stable"}) counts as zero.
     *
     * @return a negative int, zero, or a positive int as {@code v1} is less than, equal to, or greater than {@code v2}
     */
    public static int compare(String v1, String v2) {
        String[] parts1 = split(v1);
        String[] parts2 = split(v2);
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? toNumber(parts1[i]) : 0;
            int num2 = i < parts2.length ? toNumber(parts2[i]) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    private static int toNumber(String part) {
        int end = 0;
        while (end < part.length() && Character.isDigit(part.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(part.substring(0, end));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
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
