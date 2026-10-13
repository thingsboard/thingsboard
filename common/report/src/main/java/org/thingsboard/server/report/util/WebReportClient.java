// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.thingsboard.server.report.util.ReportUtils.prepareReportName;

@Slf4j
@Component
public class WebReportClient {

    public static final Pattern CONTENT_DISPOSITION_REGEX = Pattern.compile("(?i)^.*filename=\"?([^\"]+)\"?.*filename\\*=UTF-8''([^\"]+).*$");

    @Value("${reports.web_report.base_url}")
    private String webReportServerBaseUrl;

    @Value("${reports.web_report.max_response_size:52428800}")
    private int maxResponseSize;

    private EventLoopGroup eventLoopGroup;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        try {
            this.eventLoopGroup = new NioEventLoopGroup();
            HttpClient httpClient = HttpClient.create()
                    .runOn(eventLoopGroup)
                    .secure(t -> {
                        try {
                            t.sslContext(SslContextBuilder.forClient().build());
                        } catch (SSLException e) {
                            throw new RuntimeException(e);
                        }
                    });

            this.webClient = WebClient.builder()
                    .filter(ExchangeFilterFunctions.limitResponseSize(maxResponseSize))
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .exchangeStrategies(ExchangeStrategies.builder()
                            .codecs(configurer -> configurer.defaultCodecs()
                                    .maxInMemorySize(maxResponseSize))
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Can't initialize dashboard report service due to {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    public void requestDashboardReport(DashboardReportConfig reportConfig, String reportsServerEndpointUrl,
                                       String accessToken, long accessTokenExpiration,
                                       Consumer<DashboardReportData> onSuccess, Consumer<Throwable> onFailure) {
        JsonNode dashboardReportRequest = createDashboardReportRequest(reportConfig, accessToken, accessTokenExpiration, null);
        requestDashboardReport(dashboardReportRequest, reportsServerEndpointUrl, onSuccess, onFailure);
    }

    public void requestDashboardReport(DashboardReportConfig reportConfig, String reportsServerEndpointUrl,
                                       String accessToken, long accessTokenExpiration, Integer pageWidth,
                                       Consumer<DashboardReportData> onSuccess, Consumer<Throwable> onFailure) {
        JsonNode dashboardReportRequest = createDashboardReportRequest(reportConfig, accessToken, accessTokenExpiration, pageWidth);
        requestDashboardReport(dashboardReportRequest, reportsServerEndpointUrl, onSuccess, onFailure);
    }

    public void requestDashboardReport(JsonNode dashboardReportRequest, String reportsServerEndpointUrl,
                                       Consumer<DashboardReportData> onSuccess, Consumer<Throwable> onFailure) {
        if (StringUtils.isEmpty(reportsServerEndpointUrl)) {
            reportsServerEndpointUrl = this.webReportServerBaseUrl;
        }
        String endpointUrl = reportsServerEndpointUrl + "/dashboardReport";

        byte[] requestBody = JacksonUtil.writeValueAsBytes(dashboardReportRequest);

        webClient.post()
                .uri(endpointUrl)
                .headers(headers -> prepareHeaders(headers, requestBody))
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(byte[].class)
                .subscribe(responseEntity -> {
                    try {
                        onSuccess.accept(extractResponse(responseEntity));
                    } catch (Throwable t) {
                        processError(onFailure, t);
                    }
                }, t -> {
                    processError(onFailure, t);
                });
    }

    private JsonNode createDashboardReportRequest(DashboardReportConfig reportConfig, String accessToken, long accessTokenExpiration, Integer pageWidth) {
        String reportName = prepareReportName(reportConfig.getNamePattern(), new Date(), reportConfig.getTimezone());
        ObjectNode dashboardReportRequest = JacksonUtil.newObjectNode();
        dashboardReportRequest.put("baseUrl", reportConfig.getBaseUrl());
        dashboardReportRequest.put("dashboardId", reportConfig.getDashboardId());
        dashboardReportRequest.put("token", accessToken);
        dashboardReportRequest.put("expiration", accessTokenExpiration);
        dashboardReportRequest.put("name", reportName);
        dashboardReportRequest.set("reportParams", createReportParams(reportConfig, pageWidth));
        return dashboardReportRequest;
    }

    private JsonNode createReportParams(DashboardReportConfig reportConfig, Integer pageWidth) {
        ObjectNode reportParams = JacksonUtil.newObjectNode();
        reportParams.put("type", reportConfig.getType());
        reportParams.put("state", reportConfig.getState());
        if (!reportConfig.isUseDashboardTimewindow()) {
            reportParams.set("timewindow", reportConfig.getTimewindow());
        }
        reportParams.put("timezone", reportConfig.getTimezone());
        if (pageWidth != null) {
            reportParams.put("pageWidth", pageWidth);
        }
        return reportParams;
    }

    private void processError(Consumer<Throwable> onFailure, Throwable t) {
        if (t instanceof RestClientResponseException) {
            onFailure.accept(new ThingsboardException(((RestClientResponseException) t).getStatusText(), ThingsboardErrorCode.GENERAL));
        } else if (t instanceof WebClientResponseException) {
            WebClientResponseException webClientResponseException = (WebClientResponseException) t;
            String error = webClientResponseException.getResponseBodyAsString();
            if (StringUtils.isBlank(error)) {
                error = webClientResponseException.getStatusText();
            }
            HttpStatusCode httpStatusCode = webClientResponseException.getStatusCode();
            HttpStatus httpStatus = HttpStatus.resolve(httpStatusCode.value());
            ThingsboardErrorCode errorCode = ThingsboardErrorCode.GENERAL;
            if (HttpStatus.BAD_REQUEST.equals(httpStatus)) {
                errorCode = ThingsboardErrorCode.BAD_REQUEST_PARAMS;
            }
            onFailure.accept(new ThingsboardException(error, errorCode));
        } else {
            onFailure.accept(t);
        }
    }

    private DashboardReportData extractResponse(ResponseEntity<byte[]> responseEntity) throws UnsupportedEncodingException {
        DashboardReportData reportData = new DashboardReportData();
        reportData.setData(responseEntity.getBody());
        reportData.setContentType(responseEntity.getHeaders().getContentType().toString());
        String disposition = responseEntity.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        Matcher matcher = CONTENT_DISPOSITION_REGEX.matcher(disposition);
        if (matcher.find()) {
            String utf8FileName = matcher.group(2);
            if (!utf8FileName.isBlank()) {
                reportData.setName(URLDecoder.decode(utf8FileName, StandardCharsets.UTF_8));
            } else {
                String fileName = matcher.group(1);
                reportData.setName(URLDecoder.decode(fileName, "ISO_8859_1"));
            }
        }
        return reportData;
    }

    private void prepareHeaders(HttpHeaders headers, byte[] json) {
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(json.length);
        headers.setConnection("keep-alive");
    }

}
