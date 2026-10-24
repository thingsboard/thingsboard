// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import java.util.Base64;

/**
 * Test-only keypairs; only the public halves appear here.
 */
final class CommunityGrantSigningFixtures {

    static final String PUBLIC_KEY_A = "jHpPieuz8lNtJdrhzz0vi0hzXAxljhABo1FfIRtjBb0=";
    static final String PUBLIC_KEY_B = "aqbhyeoLeaeAqQhjO3vDN+PX3U66ptqY8rHSbfrbpMI=";

    static final String TRUSTS_A = "1:" + PUBLIC_KEY_A;
    static final String TRUSTS_A_AND_B = "1:" + PUBLIC_KEY_A + ",2:" + PUBLIC_KEY_B;

    /** A shell script that prints a well-formed report, so a runner can execute it. */
    static final byte[] CHECKER = decode(
            "IyEvYmluL3NoCmNhdCA8PCdFT0YnCi0tLS0tQkVHSU4gVEIgSU5TVEFOQ0UgQ0hFQ0stLS0tLQpabUZyWlMxeVpYQnZjb"
                    + "lF0WW05a2VRPT0KLS0tLS1FTkQgVEIgSU5TVEFOQ0UgQ0hFQ0stLS0tLQpFT0YK");

    static final byte[] OTHER_CHECKER = decode(
            "IyEvYmluL3NoCmVjaG8gIi0tLS0tQkVHSU4gVEIgSU5TVEFOQ0UgQ0hFQ0stLS0tLSIKZWNobyAiYSBkaWZmZXJlbnQgY"
                    + "2hlY2tlciBidWlsZCIK");

    static final byte[] SIGNATURE = decode(
            "VEJJQ1NJRzEBAWRjh7y4j1vNYnedESE5qBZss7R7LeimEWHJcX9du8OyGFpbWAxdi49A40GotXjo7EhXw9sUFxDiPuz1p"
                    + "wAjzgs=");

    static final byte[] OTHER_SIGNATURE = decode(
            "VEJJQ1NJRzEBATCrpGddBA5MxIwkJOTMQZq/pg6b4IYEwA8a29TxsyYL2H52QRENSnlUCpl4y+IVgkKFdTMEp+0AmNft/"
                    + "FE64wc=");

    static final byte[] SIGNATURE_FROM_WRONG_KEY_UNDER_KEY_ID_1 = decode(
            "VEJJQ1NJRzEBASiAzItE5P7weSOkVkY8W6V9aOQ45jlwt6IW5POZkctc9inj2C5jrMaHNRK3tJ03ljhAOl9Ugsq3oYtCG"
                    + "9JFIgo=");

    static final byte[] SIGNATURE_UNDER_KEY_ID_2 = decode(
            "VEJJQ1NJRzEBAiiAzItE5P7weSOkVkY8W6V9aOQ45jlwt6IW5POZkctc9inj2C5jrMaHNRK3tJ03ljhAOl9Ugsq3oYtCG"
                    + "9JFIgo=");

    private CommunityGrantSigningFixtures() {
    }

    private static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

}
