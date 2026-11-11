// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.subscription.SubscriptionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The development notice is not a preference. It must stay reachable and keep answering the same way no matter
 * how the deployment is branded, so the combination pinned here is the one that can actually occur: a licensed
 * instance that HAS white-labeling and is also running in development mode. The trivial case - a development
 * plan, which withholds white-labeling in the first place - proves nothing.
 */
@DaoSqlTest
public class NonProductionNoticeIntegrityTest extends AbstractControllerTest {

    /**
     * A rule that would still reach the notice element, so that the white-labeling case stores a stylesheet
     * genuinely aimed at it rather than an unrelated one that would make the test pass for the wrong reason.
     * It is written structurally, because the element no longer carries a predictable identifier to name, and
     * it selects one trailing child rather than every one: the notice is appended to the body and re-appended
     * every few seconds, so it sits at the end, while a rule matching every body-level div would also blank
     * the overlay container that dialogs, menus and tooltips are rendered into.
     */
    private static final String NOTICE_TARGETING_CSS = "body > div:last-of-type { opacity: 0; }";

    @MockitoSpyBean
    private SubscriptionService subscriptionService;

    @Test
    public void whiteLabelingSettingsCannotSuppressTheProbeTheNoticeIsDerivedFrom() throws Exception {
        // Named for what it actually pins, which is narrower than "white labeling cannot hide the notice".
        // What is verified here: branding a deployment - including storing a stylesheet written specifically
        // to target the notice element - does not change the answer the client asks for before it renders
        // anything. Suppressing the notice would have to start by making this probe say false, so an answer
        // that survives the most hostile white-labeling the API accepts is worth pinning on its own.
        //
        // What is NOT verified here, deliberately and unavoidably: whether that stored CSS can paint over or
        // hide the notice once it is rendered. Answering that needs a browser, and this project has no
        // front-end test runner at all. The defence against it lives elsewhere and is pinned elsewhere - the
        // notice's declarations are written with RendererStyleFlags2.Important, which outranks any author
        // stylesheet, and DevelopmentNoticeStyleGuardTest is what fails if that flag or any pinned
        // declaration is dropped. Do not read a pass here as cover for that.
        givenDevelopmentMode();
        givenWhiteLabelingConfigured();

        resetTokens();
        assertThat(doGet(FrontEndSource.DEVELOPMENT_PROBE_URL, Boolean.class)).isTrue();
    }

    @Test
    public void theNoticeProbeIsReachableWithoutAuthentication() throws Exception {
        // The UI reads it before a session exists; if it ever required auth the notice would silently
        // stop rendering on the login screen.
        givenDevelopmentMode();

        resetTokens();
        doGet(FrontEndSource.DEVELOPMENT_PROBE_URL).andExpect(status().isOk());
    }

    @Test
    public void theProbeAnswersFalseOnAProductionInstance() throws Exception {
        // Without this the two assertions above would still hold if the probe were hardwired to true.
        when(subscriptionService.isDevelopment(any())).thenReturn(false);

        resetTokens();
        assertThat(doGet(FrontEndSource.DEVELOPMENT_PROBE_URL, Boolean.class)).isFalse();
    }

    private void givenDevelopmentMode() {
        when(subscriptionService.isDevelopment(any())).thenReturn(true);
    }

    /**
     * Brands the deployment as heavily as the API allows, including a stylesheet written specifically to hide
     * the notice element, and confirms the branding really was stored - otherwise a white-labeling save that
     * silently failed would leave the test asserting nothing.
     */
    private void givenWhiteLabelingConfigured() throws Exception {
        // A development plan carries no white-labeling entitlement, so the licensed half of the combination
        // has to be stated explicitly rather than inherited from the plan under test.
        doNothing().when(subscriptionService).whiteLabelingAllowed(any());

        loginTenantAdmin();
        WhiteLabelingParams whiteLabelingParams = doGet("/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);
        whiteLabelingParams.setAppTitle("Rebranded Platform");
        whiteLabelingParams.setPlatformName("Rebranded Platform");
        whiteLabelingParams.setShowNameVersion(false);
        whiteLabelingParams.setCustomCss(NOTICE_TARGETING_CSS);
        doPost("/api/whiteLabel/whiteLabelParams", whiteLabelingParams, WhiteLabelingParams.class);

        WhiteLabelingParams stored = doGet("/api/whiteLabel/whiteLabelParams", WhiteLabelingParams.class);
        assertThat(stored.getAppTitle()).isEqualTo("Rebranded Platform");
        assertThat(stored.getCustomCss()).contains(NOTICE_TARGETING_CSS);
    }

}
