// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.report.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.rule.engine.api.DashboardReportService;
import org.thingsboard.server.report.util.NonProductionImageNotice;
import org.thingsboard.server.report.util.NonProductionPdfNotice;
import org.thingsboard.server.report.util.WebReportClient;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * The single place where a standalone dashboard report acquires the non-production notice: both callers that
 * ask for one - the download endpoints in {@code DashboardReportController} and {@code TbGenerateReportNode} -
 * go through the one method they share, so neither can forget to mark its own copy.
 * <p>
 * This is not the only way into {@link WebReportClient}: {@code PdfReportService} requests a dashboard capture
 * from the client directly, to embed in a PDF report it stamps itself, and any new direct caller would likewise
 * go unmarked here - making that impossible means enforcing in the client instead. Callers of this service must
 * not mark the bytes again themselves: the raster notice is drawn with translucent colours (see
 * {@link NonProductionImageNotice}), so a second pass darkens the tiles and re-encodes an already lossy JPEG.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultDashboardReportService implements DashboardReportService {

    @Value("${reports.rate_limits.enabled:false}")
    private boolean rateLimitsEnabled;

    @Value("${reports.rate_limits.configuration:5:300}")
    private String rateLimitsConfiguration;

    private final WebReportClient webReportClient;
    private final SystemSecurityService systemSecurityService;
    private final RateLimitService rateLimitService;
    private final SubscriptionService subscriptionService;

    private void checkLimits(TenantId tenantId) {
        if (rateLimitsEnabled) {
            if (!rateLimitService.checkRateLimit(LimitedApi.REPORTS, (Object) tenantId, rateLimitsConfiguration)) {
                log.trace("[{}] Report generation limits exceeded!", tenantId);
                throw new RuntimeException("Failed to generate report due to rate limits!");
            }
        }
    }

    @Override
    public void generateDashboardReport(String baseUrl, DashboardId dashboardId, TenantId tenantId, UserId userId, String reportName,
                                        JsonNode reportParams, String accessToken, long accessTokenExpiration,
                                        Consumer<DashboardReportData> onSuccess, Consumer<Throwable> onFailure) {
        checkLimits(tenantId);
        log.trace("[{}] Executing generateDashboardReport, baseUrl [{}], dashboardId [{}], userId [{}]", tenantId, baseUrl, dashboardId, userId);
        boolean nonProduction = subscriptionService.isDevelopment(tenantId);

        ObjectNode dashboardReportRequest = JacksonUtil.newObjectNode();
        dashboardReportRequest.put("baseUrl", baseUrl);
        dashboardReportRequest.put("dashboardId", dashboardId.toString());
        dashboardReportRequest.set("reportParams", reportParams);
        dashboardReportRequest.put("name", reportName);
        dashboardReportRequest.put("token", accessToken);
        dashboardReportRequest.put("expiration", accessTokenExpiration);
        webReportClient.requestDashboardReport(dashboardReportRequest, null, withNonProductionNotice(nonProduction, onSuccess, onFailure), onFailure);
    }

    @Override
    public void generateReport(TenantId tenantId, DashboardReportConfig reportConfig, String reportsServerEndpointUrl, Consumer<DashboardReportData> onSuccess, Consumer<Throwable> onFailure) throws ThingsboardException {
        checkLimits(tenantId);
        log.trace("[{}] Executing generateReport, reportConfig [{}]", tenantId, reportConfig);
        boolean nonProduction = subscriptionService.isDevelopment(tenantId);
        AccessJwtToken accessToken = systemSecurityService.createUserAccessToken(tenantId, new UserId(UUID.fromString(reportConfig.getUserId())));
        webReportClient.requestDashboardReport(reportConfig, reportsServerEndpointUrl, accessToken.getToken(),
                accessToken.getClaims().getExpiration().getTime(), withNonProductionNotice(nonProduction, onSuccess, onFailure), onFailure);
    }

    /**
     * Wraps the caller's success consumer in one that marks the report before handing it on.
     * <p>
     * {@code nonProduction} is resolved by the two methods above, before the report is requested: this consumer
     * runs on the reactive thread the report service's HTTP response completes on, where there is no security
     * context and no message processing context. A failure to mark takes the caller's own {@code onFailure}
     * path and not the success path, so an unmarked report fails exactly as one the report service itself
     * failed to produce.
     */
    private Consumer<DashboardReportData> withNonProductionNotice(boolean nonProduction, Consumer<DashboardReportData> onSuccess,
                                                                  Consumer<Throwable> onFailure) {
        if (!nonProduction) {
            return onSuccess;
        }
        return reportData -> {
            try {
                // Replacing the bytes in place keeps the consumer contract unchanged and leaves no unmarked copy
                // for a caller to reach for. Safe: WebReportClient builds this object per response.
                reportData.setData(addNonProductionNotice(reportData));
            } catch (Exception e) {
                log.warn("Failed to apply the non-production notice to dashboard report [{}]", reportData.getName(), e);
                onFailure.accept(e);
                return;
            }
            onSuccess.accept(reportData);
        };
    }

    /**
     * Stamps a finished dashboard report with the non-production notice. These bytes come from the external
     * report service, which screenshots the dashboard in a headless browser, so nothing the platform's own PDF
     * renderer stamps reaches them.
     * <p>
     * PDF goes through {@link NonProductionPdfNotice#addNonProductionNotice(byte[], boolean)}, the same call the
     * platform's own PDF reports use; the raster formats cannot share it and go through the sibling
     * {@link NonProductionImageNotice}. An unrecognised, absent or malformed content type is a failure rather
     * than a pass-through, which would produce an unmarked artifact nobody realised was unmarked.
     */
    private byte[] addNonProductionNotice(DashboardReportData reportData) throws Exception {
        MediaType contentType = parseContentType(reportData.getContentType());
        if (MediaType.APPLICATION_PDF.isCompatibleWith(contentType)) {
            return NonProductionPdfNotice.addNonProductionNotice(reportData.getData(), true);
        } else if (MediaType.IMAGE_PNG.isCompatibleWith(contentType)) {
            return NonProductionImageNotice.addNonProductionNotice(reportData.getData(), NonProductionImageNotice.PNG_IMAGE_FORMAT, true);
        } else if (MediaType.IMAGE_JPEG.isCompatibleWith(contentType)) {
            return NonProductionImageNotice.addNonProductionNotice(reportData.getData(), NonProductionImageNotice.JPEG_IMAGE_FORMAT, true);
        }
        throw unexpectedContentType(reportData.getContentType(), null);
    }

    private static MediaType parseContentType(String contentType) throws ThingsboardException {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            // An absent or malformed content type reaches the same outcome as a recognisably wrong one; the
            // parse error is kept as the cause.
            throw unexpectedContentType(contentType, e);
        }
    }

    private static ThingsboardException unexpectedContentType(String contentType, Throwable cause) {
        return new ThingsboardException("Unable to mark the report as non-production: unexpected report content type "
                + contentType, cause, ThingsboardErrorCode.GENERAL);
    }

}
