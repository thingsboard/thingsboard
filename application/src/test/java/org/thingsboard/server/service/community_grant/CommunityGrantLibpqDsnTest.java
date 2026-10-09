// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommunityGrantLibpqDsnTest {

    private static final String URL = "jdbc:postgresql://localhost:5432/thingsboard";

    @Test
    void testTheJdbcUrlIsRewrittenAsALibpqConnectionString() {
        assertThat(CommunityGrantLibpqDsn.from(URL, "postgres", "secret"))
                .isEqualTo("postgres://postgres:secret@localhost:5432/thingsboard");
        assertThat(CommunityGrantLibpqDsn.from(URL, "postgres", "p@ss/word?x"))
                .isEqualTo("postgres://postgres:p%40ss%2Fword%3Fx@localhost:5432/thingsboard");
        // JDBC driver parameters are not libpq's, so they are dropped.
        assertThat(CommunityGrantLibpqDsn.from(
                "jdbc:postgresql://db.internal:5432/tb?ApplicationName=ThingsBoard&ssl=false", "postgres", "secret"))
                .isEqualTo("postgres://postgres:secret@db.internal:5432/tb");
    }

    // URI leaves ':' in userinfo unescaped, but libpq splits user and password at the first colon.
    @Test
    void testAUsernameContainingAColonIsEncodedSeparately() {
        assertThat(CommunityGrantLibpqDsn.from(URL, "a:b", "c"))
                .isEqualTo("postgres://a%3Ab:c@localhost:5432/thingsboard");
    }

    // The url may carry credentials, so the refusal quotes neither it nor a cause that holds it.
    @Test
    void testAMalformedDatasourceUrlIsRejectedWithoutQuotingTheUrl() {
        assertThatThrownBy(() -> CommunityGrantLibpqDsn.from(
                "jdbc:postgresql://host:5432/tb?user=admin&password=hunter2^{}", "postgres", "secret"))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(e -> {
                    assertThat(e.getMessage()).doesNotContain("hunter2");
                    assertThat(e.getMessage()).doesNotContain("host:5432");
                    assertThat(e).hasNoCause();
                });
    }

    @Test
    void testANonPostgresqlJdbcUrlIsRejected() {
        assertThatThrownBy(() -> CommunityGrantLibpqDsn.from("jdbc:mysql://localhost:3306/thingsboard",
                "postgres", "secret")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testAUrlWithNoDatabaseNameIsRejected() {
        assertThatThrownBy(() -> CommunityGrantLibpqDsn.from("jdbc:postgresql://localhost:5432",
                "postgres", "secret")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testABlankUsernameIsRejected() {
        assertThatThrownBy(() -> CommunityGrantLibpqDsn.from(URL, "", "secret"))
                .isInstanceOf(IllegalStateException.class);
    }

}
