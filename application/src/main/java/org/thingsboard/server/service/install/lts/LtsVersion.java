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
