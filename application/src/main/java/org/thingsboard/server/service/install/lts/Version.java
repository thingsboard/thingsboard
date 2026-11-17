// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install.lts;

public record Version(int major, int minor, int maintenance, int patch) implements Comparable<Version> {

    public static Version parse(String version) {
        if (version == null) {
            throw new IllegalArgumentException("Version is null");
        }
        String[] parts = version.split("\\.");
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int maintenance = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            int patch = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
            return new Version(major, minor, maintenance, patch);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version: " + version, e);
        }
    }

    public boolean sameFamily(Version other) {
        return major == other.major && minor == other.minor;
    }

    /**
     * Half-open range check: {@code from < this <= to}. The lower bound is exclusive (a source already at
     * {@code from} has applied that version) and the upper bound inclusive (the target version's own migration runs).
     */
    public boolean isInRange(Version from, Version to) {
        return compareTo(from) > 0 && compareTo(to) <= 0;
    }

    /**
     * Closed range check: {@code from <= this <= to}. Only for a run an operator has forced past its version check,
     * where the source's own migration has to be re-run (see {@link MigrationService}).
     */
    public boolean isInClosedRange(Version from, Version to) {
        return compareTo(from) >= 0 && compareTo(to) <= 0;
    }

    @Override
    public int compareTo(Version o) {
        int c = Integer.compare(major, o.major);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(minor, o.minor);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(maintenance, o.maintenance);
        if (c != 0) {
            return c;
        }
        return Integer.compare(patch, o.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + maintenance + "." + patch;
    }
}
