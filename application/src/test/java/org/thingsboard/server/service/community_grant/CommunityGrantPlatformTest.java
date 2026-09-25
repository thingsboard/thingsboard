// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommunityGrantPlatformTest {

    @ParameterizedTest
    @CsvSource({
            "Mac OS X, aarch64, darwin, arm64",
            "Mac OS X, x86_64, darwin, amd64",
            "Windows 10, amd64, windows, amd64",
            "Windows Server 2022, aarch64, windows, arm64",
            "Linux, amd64, linux, amd64",
            "Linux, x86_64, linux, amd64",
            "Linux, aarch64, linux, arm64",
            "SunOS, x64, linux, amd64"
    })
    void testPlatformIsMappedToThePortalsVocabulary(String osName, String osArch, String os, String arch) {
        assertThat(CommunityGrantPlatform.of(osName, osArch)).isEqualTo(new CommunityGrantPlatform(os, arch));
    }

    @ParameterizedTest
    @CsvSource({"Linux, ppc64le", "Linux, s390x", "Linux, i386"})
    void testAnUnsupportedArchitectureIsRefused(String osName, String osArch) {
        assertThatThrownBy(() -> CommunityGrantPlatform.of(osName, osArch))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported architecture");
    }

    @Test
    void testOnlyWindowsGetsAnExeSuffix() {
        assertThat(new CommunityGrantPlatform("windows", "amd64").checkerFileName()).isEqualTo("tb-instance-check.exe");
        assertThat(new CommunityGrantPlatform("linux", "amd64").checkerFileName()).isEqualTo("tb-instance-check");
        assertThat(new CommunityGrantPlatform("darwin", "arm64").checkerFileName()).isEqualTo("tb-instance-check");
    }

    @Test
    void testOnlyAFileNameNamingAnotherPlatformIsRefused() {
        CommunityGrantPlatform platform = new CommunityGrantPlatform("linux", "amd64");

        assertThat(platform.namesAnotherPlatform("tb-instance-check_windows_amd64")).isTrue();
        assertThat(platform.namesAnotherPlatform("tb-instance-check_linux_arm64")).isTrue();
        assertThat(platform.namesAnotherPlatform("TB-Instance-Check_Darwin_ARM64")).isTrue();
        assertThat(platform.namesAnotherPlatform("tb-instance-check")).isFalse();
        assertThat(platform.namesAnotherPlatform("tb-instance-check_linux_amd64")).isFalse();
        assertThat(platform.namesAnotherPlatform("tb-instance-check (1)")).isFalse();
        assertThat(platform.namesAnotherPlatform("tb-instance-check_solaris_sparc")).isFalse();
        assertThat(platform.namesAnotherPlatform(null)).isFalse();
        assertThat(platform.namesAnotherPlatform("")).isFalse();
    }

}
