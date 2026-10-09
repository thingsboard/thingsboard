// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.function.Resolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.community_grant.CommunityGrantMode;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CommunityGrantPortalClientTest {

    private static final String BASE_URL = "https://portal.example";
    private static final String SIGN_UP_PATH = "/communityGrant";
    private static final String TOKEN = "AAAA-BBBB_CCCC";
    private static final String CLAIM_TOKEN_HEADER = "X-TB-Claim-Token";

    private CommunityGrantPortalClient client;
    private MockRestServiceServer mockServer;

    /** Captured before {@link MockRestServiceServer#createServer} replaces the request factory with its own. */
    private ClientHttpRequestFactory realRequestFactory;

    @BeforeEach
    void setUp() {
        client = new CommunityGrantPortalClient();
        // Without it initRestClient() skips the base-URL validation.
        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(client, "connectTimeoutSec", 5);
        ReflectionTestUtils.setField(client, "readTimeoutSec", 10);
        ReflectionTestUtils.setField(client, "maxCheckerSizeBytes", 1024L);
        client.initRestClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        realRequestFactory = restTemplate.getRequestFactory();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testSignUpUrlPinsItsParameterNamesAndOrder() {
        UUID clusterId = UUID.randomUUID();

        onPlatform("Linux", "amd64", () -> {
            assertThat(client.buildSignUpUrl(clusterId, TOKEN, CommunityGrantMode.ONLINE))
                    .isEqualTo(BASE_URL + SIGN_UP_PATH + "?clusterId=" + clusterId + "&claimToken=" + TOKEN
                            + "&os=linux&arch=amd64");
            assertThat(client.buildSignUpUrl(clusterId, TOKEN, CommunityGrantMode.OFFLINE))
                    .isEqualTo(BASE_URL + SIGN_UP_PATH + "?clusterId=" + clusterId + "&claimToken=" + TOKEN
                            + "&offline=true&os=linux&arch=amd64");
        });
    }

    @Test
    void testSignUpUrlCarriesThePlatformEvenWhenTheDeploymentIsOnline() {
        onPlatform("Mac OS X", "aarch64", () ->
                assertThat(client.buildSignUpUrl(UUID.randomUUID(), TOKEN, CommunityGrantMode.ONLINE))
                        .endsWith("&os=darwin&arch=arm64"));
    }

    // The portal refuses a link carrying only one of os and arch, so both are dropped.
    @Test
    void testSignUpUrlOmitsThePlatformOnAnUnsupportedArchitecture() {
        UUID clusterId = UUID.randomUUID();

        onPlatform("Linux", "s390x", () ->
                assertThat(client.buildSignUpUrl(clusterId, TOKEN, CommunityGrantMode.OFFLINE))
                        .isEqualTo(BASE_URL + SIGN_UP_PATH + "?clusterId=" + clusterId + "&claimToken=" + TOKEN
                                + "&offline=true"));
    }

    @Test
    void testSignUpUrlHasNoDoubleSlashWhenTheBaseUrlEndsInOne() {
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL + "/");
        UUID clusterId = UUID.randomUUID();

        onPlatform("Linux", "amd64", () ->
                assertThat(client.buildSignUpUrl(clusterId, TOKEN, CommunityGrantMode.ONLINE))
                        .isEqualTo(BASE_URL + SIGN_UP_PATH + "?clusterId=" + clusterId + "&claimToken=" + TOKEN
                                + "&os=linux&arch=amd64"));
    }

    private static void onPlatform(String osName, String osArch, Runnable body) {
        String originalName = System.getProperty("os.name");
        String originalArch = System.getProperty("os.arch");
        System.setProperty("os.name", osName);
        System.setProperty("os.arch", osArch);
        try {
            body.run();
        } finally {
            restoreProperty("os.name", originalName);
            restoreProperty("os.arch", originalArch);
        }
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Test
    void testSignUpUrlRefusesAMalformedBaseUrl() {
        ReflectionTestUtils.setField(client, "baseUrl", "ht!tp://bad host");

        assertThatThrownBy(() -> client.buildSignUpUrl(UUID.randomUUID(), TOKEN, CommunityGrantMode.ONLINE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testClusterStatus() {
        UUID clusterId = UUID.randomUUID();
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/clusterStatus?clusterId=" + clusterId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"alreadyRegistered\":true}", MediaType.APPLICATION_JSON));

        assertThat(client.isAlreadyRegistered(clusterId)).isTrue();
        mockServer.verify();
    }

    @Test
    void testClusterStatusReturnsFalseWhenNotRegistered() {
        UUID clusterId = UUID.randomUUID();
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/clusterStatus?clusterId=" + clusterId))
                .andRespond(withSuccess("{\"alreadyRegistered\":false}", MediaType.APPLICATION_JSON));

        assertThat(client.isAlreadyRegistered(clusterId)).isFalse();
        mockServer.verify();
    }

    @Test
    void testStatusIsParsedWithTheCheckerInput() {
        UUID clusterId = UUID.randomUUID();
        String checkerInput = "{\"clusterId\":\"" + clusterId + "\",\"keyId\":7,\"publicKey\":\"cHVi\","
                + "\"challenge\":\"Y2hh\",\"referenceMillis\":1787788800000}";
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/status?clusterId=" + clusterId))
                .andExpect(header(CLAIM_TOKEN_HEADER, TOKEN))
                .andRespond(withSuccess("{\"status\":\"VERIFICATION_REQUIRED\",\"checkerInput\": "
                        + checkerInput.replace(",", ", ") + ",\"retryAfterSec\":15}", MediaType.APPLICATION_JSON));

        CommunityGrantPortalStatus status = client.getStatus(TOKEN, clusterId);

        assertThat(status.status()).isEqualTo("VERIFICATION_REQUIRED");
        assertThat(CommunityGrantCheckerInput.from(status.checkerInput()))
                .contains(new CommunityGrantCheckerInput(checkerInput));
        assertThat(status.retryAfterSec()).isEqualTo(15);
        mockServer.verify();
    }

    @Test
    void testStatusToleratesNullsAndUnknownFields() {
        UUID clusterId = UUID.randomUUID();
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/status?clusterId=" + clusterId))
                .andExpect(header(CLAIM_TOKEN_HEADER, TOKEN))
                .andRespond(withSuccess("{\"status\":\"AWAITING_SIGNUP\",\"checkerInput\":null,"
                        + "\"retryAfterSec\":null,\"somethingNew\":123}", MediaType.APPLICATION_JSON));

        CommunityGrantPortalStatus status = client.getStatus(TOKEN, clusterId);

        assertThat(status.status()).isEqualTo("AWAITING_SIGNUP");
        assertThat(CommunityGrantCheckerInput.from(status.checkerInput())).isEmpty();
        assertThat(status.retryAfterSec()).isNull();
        mockServer.verify();
    }

    @Test
    void testUploadReportSendsTokenAndReportAndReturnsStatus() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/report"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"token\":\"AAAA-BBBB_CCCC\",\"report\":\"-----BEGIN TB INSTANCE CHECK-----\\nbody\"}"))
                .andRespond(withSuccess("{\"status\":\"VALIDATING\"}", MediaType.APPLICATION_JSON));

        String status = client.uploadReport(TOKEN, "-----BEGIN TB INSTANCE CHECK-----\nbody");

        assertThat(status).isEqualTo("VALIDATING");
        mockServer.verify();
    }

    @Test
    void testUploadReportReturnsNullOnEmptyBody() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/report"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        String status = client.uploadReport(TOKEN, "-----BEGIN TB INSTANCE CHECK-----\nbody");

        assertThat(status).isNull();
        mockServer.verify();
    }

    @Test
    void testClaimLicenseSendsTheTokenInTheBodyAndReturnsTheKey() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/freeLicense/claim"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"claimToken\":\"" + TOKEN + "\"}"))
                .andExpect(headerDoesNotExist(CLAIM_TOKEN_HEADER))
                .andRespond(withSuccess("{\"activated\":true,\"secret\":\"granted-key\"}", MediaType.APPLICATION_JSON));

        assertThat(client.claimLicense(TOKEN)).isEqualTo("granted-key");
        mockServer.verify();
    }

    @Test
    void testClaimLicenseReturnsNothingWhileThePortalHasNoKeyYet() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/freeLicense/claim"))
                .andRespond(withSuccess("{\"activated\":false}", MediaType.APPLICATION_JSON));

        assertThat(client.claimLicense(TOKEN)).isNull();
        mockServer.verify();
    }

    @Test
    void testClaimLicenseTurnsAClientErrorIntoARefusal() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/freeLicense/claim"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.claimLicense(TOKEN))
                .isInstanceOf(CommunityGrantLicenseClaimRefusedException.class);
        mockServer.verify();
    }

    @Test
    void testClaimLicenseLeavesARetryableAnswerToTheCaller() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/freeLicense/claim"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/freeLicense/claim"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.claimLicense(TOKEN))
                .isInstanceOf(HttpStatusCodeException.class)
                .isNotInstanceOf(CommunityGrantLicenseClaimRefusedException.class);
        assertThatThrownBy(() -> client.claimLicense(TOKEN))
                .isInstanceOf(HttpStatusCodeException.class)
                .isNotInstanceOf(CommunityGrantLicenseClaimRefusedException.class);
        mockServer.verify();
    }

    @Test
    void testDownloadChecker() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/checker?os=linux&arch=amd64"))
                .andExpect(header(CLAIM_TOKEN_HEADER, TOKEN))
                .andRespond(withSuccess("binary-bytes".getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_OCTET_STREAM));

        byte[] checker = client.downloadChecker(TOKEN, "linux", "amd64");

        assertThat(new String(checker, StandardCharsets.UTF_8)).isEqualTo("binary-bytes");
        mockServer.verify();
    }

    @Test
    void testDownloadCheckerSignature() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/checkerSignature?os=linux&arch=amd64"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(CLAIM_TOKEN_HEADER, TOKEN))
                .andRespond(withSuccess(CommunityGrantSigningFixtures.SIGNATURE, MediaType.APPLICATION_OCTET_STREAM));

        assertThat(client.downloadCheckerSignature(TOKEN, "linux", "amd64"))
                .isEqualTo(CommunityGrantSigningFixtures.SIGNATURE);
        mockServer.verify();
    }

    @Test
    void testDownloadCheckerSignatureFailsWhenThePortalHasNoneStaged() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/checkerSignature?os=linux&arch=amd64"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.downloadCheckerSignature(TOKEN, "linux", "amd64"))
                .isInstanceOf(HttpStatusCodeException.class);
        mockServer.verify();
    }

    @Test
    void testDownloadCheckerSignatureIsBoundedLikeTheDownloadItAccompanies() {
        ReflectionTestUtils.setField(client, "maxCheckerSizeBytes", 4L);
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/checkerSignature?os=linux&arch=amd64"))
                .andRespond(withSuccess("way-too-many-bytes".getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_OCTET_STREAM));

        assertThatThrownBy(() -> client.downloadCheckerSignature(TOKEN, "linux", "amd64"))
                .isInstanceOf(CommunityGrantCheckerTooLargeException.class)
                .hasMessageContaining("release signature stream exceeded");
        mockServer.verify();
    }

    @Test
    void testDownloadCheckerRejectsAnOversizedContentLength() {
        ReflectionTestUtils.setField(client, "maxCheckerSizeBytes", 4L);
        byte[] body = "way-too-many-bytes".getBytes(StandardCharsets.UTF_8);
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/checker?os=linux&arch=amd64"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length)));

        assertThatThrownBy(() -> client.downloadChecker(TOKEN, "linux", "amd64"))
                .isInstanceOf(CommunityGrantCheckerTooLargeException.class)
                .hasMessageContaining("checker size " + body.length + " bytes exceeds");
        mockServer.verify();
    }

    @Test
    void testDownloadCheckerRejectsAnOversizedStreamWithNoContentLength() {
        ReflectionTestUtils.setField(client, "maxCheckerSizeBytes", 4L);
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/checker?os=linux&arch=amd64"))
                .andRespond(withSuccess("way-too-many-bytes".getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_OCTET_STREAM));

        assertThatThrownBy(() -> client.downloadChecker(TOKEN, "linux", "amd64"))
                .isInstanceOf(CommunityGrantCheckerTooLargeException.class)
                .hasMessageContaining("checker stream exceeded");
        mockServer.verify();
    }

    @Test
    void testDownloadCheckerWrapsAStreamReadFailureAsResourceAccessException() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/checker?os=linux&arch=amd64"))
                .andRespond(respondWith(new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("connection reset");
                    }
                }));

        assertThatThrownBy(() -> client.downloadChecker(TOKEN, "linux", "amd64"))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("Failed to read the instance checker")
                // The un-expanded url template; the claim token travels in a header.
                .hasMessageContaining("GET " + BASE_URL + "/api/noauth/communityGrant/checker?os={os}")
                .hasMessageNotContaining(TOKEN)
                .hasCauseInstanceOf(IOException.class);
        mockServer.verify();
    }

    @Test
    void testDownloadCheckerKeepsACompletedDownloadWhenOnlyCloseFails() {
        byte[] body = "a-complete-checker".getBytes(StandardCharsets.UTF_8);
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/checker?os=linux&arch=amd64"))
                .andRespond(respondWith(new InputStream() {
                    private int position;

                    @Override
                    public int read() {
                        return position < body.length ? body[position++] & 0xFF : -1;
                    }

                    @Override
                    public void close() throws IOException {
                        throw new IOException("failed to release the connection");
                    }
                }));

        assertThat(client.downloadChecker(TOKEN, "linux", "amd64")).isEqualTo(body);
        mockServer.verify();
    }

    @Test
    void testRequestAccessSendsTheClusterId() {
        UUID clusterId = UUID.randomUUID();
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/requestAccess?clusterId=" + clusterId))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        client.requestAccess(clusterId);

        mockServer.verify();
    }

    @Test
    void testRequestAccessPropagatesATransportFailure() {
        UUID clusterId = UUID.randomUUID();
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/requestAccess?clusterId=" + clusterId))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new SocketTimeoutException("connect timed out");
                });

        assertThatThrownBy(() -> client.requestAccess(clusterId)).isInstanceOf(ResourceAccessException.class);
        mockServer.verify();
    }

    @Test
    void testProbeTreatsAnyHttpAnswerAsReachable() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/clusterStatus?clusterId=00000000-0000-0000-0000-000000000000"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.isPortalReachable()).isTrue();
        mockServer.verify();
    }

    @Test
    void testProbeTreatsAServerErrorAsReachable() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/clusterStatus?clusterId=00000000-0000-0000-0000-000000000000"))
                .andRespond(withServerError());

        assertThat(client.isPortalReachable()).isTrue();
        mockServer.verify();
    }

    @Test
    void testProbeFailsOnAConnectionError() {
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/clusterStatus?clusterId=00000000-0000-0000-0000-000000000000"))
                .andRespond(request -> {
                    throw new SocketTimeoutException("connect timed out");
                });

        assertThat(client.isPortalReachable()).isFalse();
        mockServer.verify();
    }

    @Test
    void testProbeDrainsOnlyABoundedPrefixOfTheBody() {
        AtomicInteger bytesRead = new AtomicInteger();
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/clusterStatus?clusterId=00000000-0000-0000-0000-000000000000"))
                .andRespond(respondWith(new InputStream() {
                    @Override
                    public int read() {
                        return bytesRead.incrementAndGet() <= 65536 ? 'x' : -1;
                    }
                }));

        assertThat(client.isPortalReachable()).isTrue();
        assertThat(bytesRead).hasValueLessThan(8192);
        mockServer.verify();
    }

    @Test
    void testStatusReturnsNullOnEmptyBody() {
        UUID clusterId = UUID.randomUUID();
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/status?clusterId=" + clusterId))
                .andExpect(header(CLAIM_TOKEN_HEADER, TOKEN))
                .andRespond(withSuccess());

        assertThat(client.getStatus(TOKEN, clusterId)).isNull();
        mockServer.verify();
    }

    @Test
    void testClusterStatusReturnsFalseOnEmptyBody() {
        UUID clusterId = UUID.randomUUID();
        mockServer.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/clusterStatus?clusterId=" + clusterId))
                .andRespond(withSuccess());

        assertThat(client.isAlreadyRegistered(clusterId)).isFalse();
        mockServer.verify();
    }

    @Test
    void testATrailingSlashInTheBaseUrlIsTrimmedAtStartup() {
        CommunityGrantPortalClient trailingSlashClient = newClient(BASE_URL + "/");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(trailingSlashClient, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        UUID clusterId = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/noauth/communityGrant/clusterStatus?clusterId=" + clusterId))
                .andRespond(withSuccess("{\"alreadyRegistered\":false}", MediaType.APPLICATION_JSON));

        trailingSlashClient.isAlreadyRegistered(clusterId);

        server.verify();
    }

    @Test
    void testABaseUrlThatIsNotAnAbsoluteHttpUrlIsRefusedAtStartup() {
        assertThatThrownBy(() -> newClient("license.thingsboard.io")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> newClient("ftp://license.thingsboard.io")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testADisabledClientAcceptsABlankBaseUrlAtStartup() {
        CommunityGrantPortalClient disabledClient = new CommunityGrantPortalClient();
        ReflectionTestUtils.setField(disabledClient, "enabled", false);
        ReflectionTestUtils.setField(disabledClient, "baseUrl", "");
        ReflectionTestUtils.setField(disabledClient, "connectTimeoutSec", 5);
        ReflectionTestUtils.setField(disabledClient, "readTimeoutSec", 10);

        assertThatNoException().isThrownBy(disabledClient::initRestClient);
    }

    @Test
    void testTimeoutsAreAppliedToTheBuiltRequestFactory() {
        assertThat(extractConnectTimeout(realRequestFactory)).isEqualTo(Duration.ofSeconds(5));
        assertThat(extractReadTimeout(realRequestFactory)).isEqualTo(Duration.ofSeconds(10));
    }

    private static Duration extractConnectTimeout(ClientHttpRequestFactory requestFactory) {
        if (requestFactory instanceof SimpleClientHttpRequestFactory) {
            Integer millis = (Integer) ReflectionTestUtils.getField(requestFactory, "connectTimeout");
            return Duration.ofMillis(millis);
        }
        if (requestFactory instanceof HttpComponentsClientHttpRequestFactory) {
            return Duration.ofMillis(httpComponentsDefaultConnectionConfig(requestFactory).getConnectTimeout().toMilliseconds());
        }
        if (requestFactory instanceof JdkClientHttpRequestFactory) {
            HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient");
            return httpClient.connectTimeout()
                    .orElseThrow(() -> new AssertionError("JdkClientHttpRequestFactory's HttpClient has no connect timeout configured"));
        }
        throw new AssertionError("Unrecognized request factory type, extend this test: " + requestFactory.getClass());
    }

    private static Duration extractReadTimeout(ClientHttpRequestFactory requestFactory) {
        if (requestFactory instanceof SimpleClientHttpRequestFactory) {
            Integer millis = (Integer) ReflectionTestUtils.getField(requestFactory, "readTimeout");
            return Duration.ofMillis(millis);
        }
        if (requestFactory instanceof HttpComponentsClientHttpRequestFactory) {
            return Duration.ofMillis(httpComponentsDefaultConnectionConfig(requestFactory).getSocketTimeout().toMilliseconds());
        }
        if (requestFactory instanceof JdkClientHttpRequestFactory) {
            return (Duration) ReflectionTestUtils.getField(requestFactory, "readTimeout");
        }
        throw new AssertionError("Unrecognized request factory type, extend this test: " + requestFactory.getClass());
    }

    /** Spring Boot's builder puts the timeouts on the connection manager's {@link ConnectionConfig}, not the factory. */
    private static ConnectionConfig httpComponentsDefaultConnectionConfig(ClientHttpRequestFactory requestFactory) {
        Object httpClient = ReflectionTestUtils.getField(requestFactory, "httpClient");
        Object connManager = ReflectionTestUtils.getField(httpClient, "connManager");
        if (!(connManager instanceof PoolingHttpClientConnectionManager)) {
            throw new AssertionError("Unrecognized connection manager type, extend this test: " + connManager);
        }
        @SuppressWarnings("unchecked")
        Resolver<Object, ConnectionConfig> resolver =
                (Resolver<Object, ConnectionConfig>) ReflectionTestUtils.getField(connManager, "connectionConfigResolver");
        return resolver.resolve(null);
    }

    private static CommunityGrantPortalClient newClient(String baseUrl) {
        CommunityGrantPortalClient client = new CommunityGrantPortalClient();
        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "baseUrl", baseUrl);
        ReflectionTestUtils.setField(client, "connectTimeoutSec", 5);
        ReflectionTestUtils.setField(client, "readTimeoutSec", 10);
        client.initRestClient();
        return client;
    }

    private static ResponseCreator respondWith(InputStream body) {
        return request -> new ClientHttpResponse() {
            @Override
            public HttpStatusCode getStatusCode() {
                return HttpStatus.OK;
            }

            @Override
            public String getStatusText() {
                return "OK";
            }

            @Override
            public void close() {
            }

            @Override
            public InputStream getBody() {
                return body;
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };
    }

}
