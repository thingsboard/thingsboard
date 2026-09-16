// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionUtilTest {

    @Test
    void getSha3Hash256Size() {
        assertThat(EncryptionUtil.getSha3Hash("ThingsBoard"))
                .isEqualTo("281c7ba06d0ac2ff651fa968572f26a38c96225ea54644522837b54b9c6144f7");
    }

}
