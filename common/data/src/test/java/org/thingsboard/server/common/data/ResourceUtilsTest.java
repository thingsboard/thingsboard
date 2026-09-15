// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceUtilsTest {

    @Test
    public void givenNonExistentResource_whenGetUri_thenThrowsRuntimeException() {
        assertThatThrownBy(() -> ResourceUtils.getUri(ResourceUtilsTest.class.getClassLoader(), "non/existent/resource/path.txt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to find resource");
    }

    @Test
    public void givenExistingClasspathResource_whenGetUri_thenReturnsNonNullUri() {
        String result = ResourceUtils.getUri(ResourceUtilsTest.class.getClassLoader(), "org/thingsboard/server/common/data/ResourceUtilsTest.class");

        assertThat(result).isNotNull();
        assertThat(result).contains("ResourceUtilsTest");
    }

}
