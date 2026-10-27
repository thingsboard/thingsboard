// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SysAdminAllowlistPropertiesTest {

    @Test
    void emptyListMeansNotEnforced() {
        SysAdminAllowlistProperties props = new SysAdminAllowlistProperties(List.of(), List.of());
        props.init();
        assertThat(props.isEnforced()).isFalse();
        assertThat(props.getAllowedRanges()).isEmpty();
    }

    @Test
    void blankEntriesIgnored() {
        SysAdminAllowlistProperties props = new SysAdminAllowlistProperties(List.of(""), List.of(" "));
        props.init();
        assertThat(props.isEnforced()).isFalse();
    }

    @Test
    void parsesMixedV4V6Entries() {
        SysAdminAllowlistProperties props = new SysAdminAllowlistProperties(
                List.of("203.0.113.10", "198.51.100.0/24", "2001:db8::/48"), List.of());
        props.init();
        assertThat(props.isEnforced()).isTrue();
        assertThat(props.getAllowedRanges()).hasSize(3);
    }

    @Test
    void invalidEntryFailsStartup() {
        SysAdminAllowlistProperties props = new SysAdminAllowlistProperties(
                List.of("203.0.113.10", "example.com"), List.of());
        assertThatThrownBy(props::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("example.com");
    }

    @Test
    void invalidTrustedProxyFailsStartup() {
        SysAdminAllowlistProperties props = new SysAdminAllowlistProperties(
                List.of(), List.of("not-an-ip"));
        assertThatThrownBy(props::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not-an-ip");
    }

    @Test
    void trustedProxiesParsedIndependently() {
        SysAdminAllowlistProperties props = new SysAdminAllowlistProperties(
                List.of(), List.of("10.0.0.0/8"));
        props.init();
        assertThat(props.isEnforced()).isFalse();
        assertThat(props.getTrustedProxyRanges()).hasSize(1);
    }

}
