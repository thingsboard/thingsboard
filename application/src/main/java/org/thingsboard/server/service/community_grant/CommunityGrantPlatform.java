// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import org.thingsboard.server.common.data.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * The host platform in the vocabulary the portal's {@code /checker} endpoint expects.
 */
public record CommunityGrantPlatform(String os, String arch) {

    private static final List<String> KNOWN_OS = List.of("darwin", "windows", "linux");
    private static final List<String> KNOWN_ARCH = List.of("amd64", "arm64");

    public static CommunityGrantPlatform current() {
        return of(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
    }

    static CommunityGrantPlatform of(String rawOsName, String rawOsArch) {
        String osName = rawOsName.toLowerCase(Locale.ROOT);
        String osArch = rawOsArch.toLowerCase(Locale.ROOT);
        String os;
        if (osName.contains("mac") || osName.contains("darwin")) {
            os = "darwin";
        } else if (osName.contains("win")) {
            os = "windows";
        } else {
            os = "linux";
        }
        String arch;
        if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            arch = "arm64";
        } else if (osArch.contains("amd64") || osArch.contains("x86_64") || osArch.contains("x64")) {
            arch = "amd64";
        } else {
            throw new IllegalStateException("Unsupported architecture for the instance checker: " + osArch);
        }
        return new CommunityGrantPlatform(os, arch);
    }

    public String checkerFileName() {
        return "windows".equals(os) ? "tb-instance-check.exe" : "tb-instance-check";
    }

    /** A legibility check, not a security one: true only for a name that spells out another os or arch. */
    public boolean namesAnotherPlatform(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return false;
        }
        String normalized = fileName.toLowerCase(Locale.ROOT);
        return namesAnother(normalized, KNOWN_OS, os) || namesAnother(normalized, KNOWN_ARCH, arch);
    }

    private static boolean namesAnother(String fileName, List<String> known, String mine) {
        return known.stream().anyMatch(token -> !token.equals(mine) && fileName.contains(token));
    }

}
