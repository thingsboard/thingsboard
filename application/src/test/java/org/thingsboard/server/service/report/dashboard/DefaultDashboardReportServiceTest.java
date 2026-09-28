// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.report.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.report.util.WebReportClient;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the reason the non-production notice for externally rendered dashboard reports lives in the service
 * rather than in its callers: {@code TbGenerateReportNode} calls
 * {@link DefaultDashboardReportService#generateReport} directly, without passing through the download
 * controller, and its reports are stored as blobs and emailed out without anyone looking at them first. So
 * these tests drive the service's own entry points and assert on what it hands to the success consumer - which
 * is the thing both the controller and the rule node receive - rather than on either caller.
 * <p>
 * What the mark itself looks like is not re-tested here; that is
 * {@code NonProductionImageNoticeTest}/{@code ReportUtilsTest}'s subject. These tests assert only that a
 * report cannot leave the service unmarked - by any route, in any of the three formats, or by failing quietly.
 */
public class DefaultDashboardReportServiceTest {

    private static final int IMAGE_WIDTH = 400;
    private static final int IMAGE_HEIGHT = 300;

    private final WebReportClient webReportClient = mock(WebReportClient.class);
    private final SystemSecurityService systemSecurityService = mock(SystemSecurityService.class);
    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);

    // rateLimitsEnabled stays at its field default of false without Spring to inject @Value, so checkLimits is
    // a no-op and the rate limiter never has to be stubbed.
    private final DefaultDashboardReportService reportService = new DefaultDashboardReportService(
            webReportClient, systemSecurityService, rateLimitService, subscriptionService);

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final List<DashboardReportData> deliveredReports = new ArrayList<>();
    private final List<Throwable> reportedFailures = new ArrayList<>();

    @Test
    public void testPdfFromTheRuleEngineEntryPointCarriesTheNotice() throws Exception {
        Consumer<DashboardReportData> reportConsumer = whenRuleEngineRequestsReport(true);

        reportConsumer.accept(reportData(blankPdf(), "application/pdf"));

        assertThat(reportedFailures).isEmpty();
        assertThat(deliveredReports).hasSize(1);
        // Read back with PDFBox, an independent library from the OpenPDF one the stamp is written with, so this
        // is evidence the notice is really on the page rather than that one library agrees with itself.
        assertThat(extractPdfText(deliveredReports.get(0).getData())).contains(DataConstants.NON_PRODUCTION_NOTICE);
    }

    @Test
    public void testPngFromTheRuleEngineEntryPointCarriesTheNotice() throws Exception {
        byte[] blankImage = blankImage("png");
        Consumer<DashboardReportData> reportConsumer = whenRuleEngineRequestsReport(true);

        reportConsumer.accept(reportData(blankImage, "image/png"));

        assertThat(reportedFailures).isEmpty();
        assertThat(deliveredReports).hasSize(1);
        assertThat(markedPixels(deliveredReports.get(0).getData())).isGreaterThan(0);
    }

    @Test
    public void testJpegFromTheRuleEngineEntryPointCarriesTheNotice() throws Exception {
        byte[] blankImage = blankImage("jpg");
        Consumer<DashboardReportData> reportConsumer = whenRuleEngineRequestsReport(true);

        reportConsumer.accept(reportData(blankImage, "image/jpeg"));

        assertThat(reportedFailures).isEmpty();
        assertThat(deliveredReports).hasSize(1);
        assertThat(markedPixels(deliveredReports.get(0).getData())).isGreaterThan(0);
    }

    @Test
    public void testDownloadEntryPointCarriesTheNoticeAsWell() throws Exception {
        byte[] blankImage = blankImage("png");
        Consumer<DashboardReportData> reportConsumer = whenDashboardDownloadRequestsReport(true);

        reportConsumer.accept(reportData(blankImage, "image/png"));

        assertThat(reportedFailures).isEmpty();
        assertThat(deliveredReports).hasSize(1);
        assertThat(markedPixels(deliveredReports.get(0).getData())).isGreaterThan(0);
    }

    @Test
    public void testReportIsHandedOnUntouchedOnAProductionInstance() throws Exception {
        // A licensed deployment must not pay a lossy re-encode - or any change at all - for this wiring
        // existing, so the very same array the report service returned has to come out the other side.
        byte[] blankImage = blankImage("png");
        Consumer<DashboardReportData> reportConsumer = whenRuleEngineRequestsReport(false);

        reportConsumer.accept(reportData(blankImage, "image/png"));

        assertThat(reportedFailures).isEmpty();
        assertThat(deliveredReports).hasSize(1);
        assertThat(deliveredReports.get(0).getData()).isSameAs(blankImage);
    }

    @Test
    public void testUnmarkableReportTakesTheFailurePathRatherThanTheSuccessPath() throws Exception {
        // The whole point of the failure policy, and the reason it is worth a test of its own for the rule
        // engine caller specifically: the success consumer is what saves the blob the mail node then attaches,
        // so running it with unmarked bytes would email out a clean-looking export of a development instance.
        // Routing to the failure consumer instead puts the message on the node's Failure relation, where a
        // report that never came back would have gone anyway.
        Consumer<DashboardReportData> reportConsumer = whenRuleEngineRequestsReport(true);

        reportConsumer.accept(reportData("not an image".getBytes(), "image/png"));

        assertThat(deliveredReports).isEmpty();
        assertThat(reportedFailures).hasSize(1);
    }

    @Test
    public void testUnexpectedContentTypeTakesTheFailurePathRatherThanPassingThrough() throws Exception {
        // A response in a format there is no way to mark is the case where returning the bytes anyway would
        // produce an unmarked artifact nobody realised was unmarked.
        Consumer<DashboardReportData> reportConsumer = whenRuleEngineRequestsReport(true);

        reportConsumer.accept(reportData("id,name".getBytes(), "text/csv"));

        assertThat(deliveredReports).isEmpty();
        assertThat(reportedFailures).hasSize(1);
    }

    /**
     * Drives {@link DefaultDashboardReportService#generateReport}, the entry point the rule node uses, and
     * returns the success consumer the service actually handed to the report client - which is the wrapper
     * under test, not the one passed in by the caller.
     */
    @SuppressWarnings("unchecked")
    private Consumer<DashboardReportData> whenRuleEngineRequestsReport(boolean nonProduction) throws Exception {
        givenDevelopmentMode(nonProduction);
        givenUserAccessToken();

        DashboardReportConfig reportConfig = new DashboardReportConfig();
        reportConfig.setUserId(UUID.randomUUID().toString());
        reportService.generateReport(tenantId, reportConfig, null, deliveredReports::add, reportedFailures::add);

        ArgumentCaptor<Consumer<DashboardReportData>> successConsumer = ArgumentCaptor.forClass(Consumer.class);
        verify(webReportClient).requestDashboardReport(any(DashboardReportConfig.class), any(), any(), anyLong(),
                successConsumer.capture(), any());
        return successConsumer.getValue();
    }

    /**
     * The same, for {@link DefaultDashboardReportService#generateDashboardReport} - the entry point behind the
     * download endpoints - so that neither entry point can be marked without the other.
     */
    @SuppressWarnings("unchecked")
    private Consumer<DashboardReportData> whenDashboardDownloadRequestsReport(boolean nonProduction) {
        givenDevelopmentMode(nonProduction);

        reportService.generateDashboardReport("http://localhost:8080", new DashboardId(UUID.randomUUID()), tenantId,
                new UserId(UUID.randomUUID()), "report", JacksonUtil.newObjectNode(), "token", 0L,
                deliveredReports::add, reportedFailures::add);

        ArgumentCaptor<Consumer<DashboardReportData>> successConsumer = ArgumentCaptor.forClass(Consumer.class);
        verify(webReportClient).requestDashboardReport(any(JsonNode.class), any(), successConsumer.capture(), any());
        return successConsumer.getValue();
    }

    private void givenDevelopmentMode(boolean nonProduction) {
        when(subscriptionService.isDevelopment(tenantId)).thenReturn(nonProduction);
    }

    private void givenUserAccessToken() throws ThingsboardException {
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date());
        AccessJwtToken accessToken = mock(AccessJwtToken.class);
        when(accessToken.getToken()).thenReturn("token");
        when(accessToken.getClaims()).thenReturn(claims);
        when(systemSecurityService.createUserAccessToken(eq(tenantId), any(UserId.class))).thenReturn(accessToken);
    }

    private static DashboardReportData reportData(byte[] data, String contentType) {
        DashboardReportData reportData = new DashboardReportData();
        reportData.setData(data);
        reportData.setContentType(contentType);
        reportData.setName("dashboard-report");
        return reportData;
    }

    private static byte[] blankPdf() throws Exception {
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(encoded);
        }
        return encoded.toByteArray();
    }

    private static String extractPdfText(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    /**
     * A uniformly white image, so that "marked" can be defined as simply "no longer white" - any pixel the
     * notice touched moves off pure white, and nothing else in the image can.
     */
    private static byte[] blankImage(String imageFormatName) throws Exception {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        ImageIO.write(image, imageFormatName, encoded);
        return encoded.toByteArray();
    }

    /**
     * Counts pixels that differ from white by more than a JPEG's worth of ringing, so the same measure is
     * usable for both raster formats.
     */
    private static int markedPixels(byte[] imageBytes) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(IMAGE_WIDTH);
        assertThat(image.getHeight()).isEqualTo(IMAGE_HEIGHT);
        int markedPixels = 0;
        for (int x = 0; x < IMAGE_WIDTH; x++) {
            for (int y = 0; y < IMAGE_HEIGHT; y++) {
                int rgb = image.getRGB(x, y);
                if (((rgb >> 16) & 0xFF) < 220 || ((rgb >> 8) & 0xFF) < 220 || (rgb & 0xFF) < 220) {
                    markedPixels++;
                }
            }
        }
        return markedPixels;
    }

}
