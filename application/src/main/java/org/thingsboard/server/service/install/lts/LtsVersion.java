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
package org.thingsboard.server.service.install.lts;

public record LtsVersion(int major, int minor, int maintenance, int patch) implements Comparable<LtsVersion> {

    public static LtsVersion parse(String version) {
        if (version == null) {
            throw new IllegalArgumentException("Version is null");
        }
        String[] parts = version.split("\\.");
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int maintenance = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            int patch = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
            return new LtsVersion(major, minor, maintenance, patch);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version: " + version, e);
        }
    }

    public boolean sameFamily(LtsVersion other) {
        return major == other.major && minor == other.minor;
    }

    /**
     * Half-open range check: {@code from < this <= to}. The lower bound is exclusive (a source already at
     * {@code from} has applied that version) and the upper bound inclusive (the target version's own migration runs).
     */
    public boolean isInRange(LtsVersion from, LtsVersion to) {
        return compareTo(from) > 0 && compareTo(to) <= 0;
    }

    @Override
    public int compareTo(LtsVersion o) {
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
