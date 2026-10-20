// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.trendz.TrendzPaginationData;
import org.thingsboard.server.common.data.trendz.TrendzSummary;
import org.thingsboard.server.common.data.trendz.TrendzUsage;
import org.thingsboard.server.common.data.trendz.TrendzViewConfig;
import org.thingsboard.server.common.data.trendz.TrendzViewConfigLite;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.trendz.TrendzClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DaoSqlTest
public class TrendzApiControllerTest extends AbstractControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TrendzClient trendzClient;

    @Test
    public void testGetTrendzViews_asTenantAdmin() throws Exception {
        loginTenantAdmin();

        int pageSize = 10;
        int page = 2;
        String textSearch = "Trendz View";
        String sortProperty = "name";
        String sortOrder = "DESC";

        PageLink expectedPageLink = new PageLink(pageSize, page, textSearch, new SortOrder(sortProperty, SortOrder.Direction.valueOf(sortOrder)));
        List<TrendzViewConfigLite> trendzViews = List.of(
                new TrendzViewConfigLite(UUID.fromString("36dacff7-3282-4300-a7fe-a3e31259ab06"), "Trendz View 1"),
                new TrendzViewConfigLite(UUID.fromString("1d537953-bbb8-4961-a0ec-764d7d05ce9a"), "Trendz View 2"),
                new TrendzViewConfigLite(UUID.fromString("9d1381e1-05db-4e79-8c49-c4851701b0bc"), "Trendz View 3")
        );
        int totalPages = 5;
        long totalElements = 45;

        PageData<TrendzViewConfigLite> expected = new PageData<>(trendzViews, totalPages, totalElements, true);


        when(trendzClient.getAllTrendzViews(eq(expectedPageLink), any()))
                .thenReturn(new TrendzPaginationData<>(trendzViews, page, totalPages, totalElements));

        doGet("/api/trendz/view/all?page={page}&pageSize={pageSize}&textSearch={textSearch}&sortProperty={sortProperty}&sortOrder={sortOrder}",
                page, pageSize, textSearch, sortProperty, sortOrder
        ).andExpectAll(
                r -> assertEquals(200, r.getResponse().getStatus()),
                r -> assertEquals(expected, read(r, new TypeReference<PageData<TrendzViewConfigLite>>() {}))
        );
    }

    @Test
    public void testGetTrendzViewById_asTenantAdmin() throws Exception {
        loginTenantAdmin();

        UUID viewId = UUID.fromString("36dacff7-3282-4300-a7fe-a3e31259ab06");

        Map<String, Object> filter = Map.of(
                "name", "thermostat",
                "options", List.of("Thermostat T1")
        );
        TrendzViewConfig expected = new TrendzViewConfig(viewId, "Trendz View 1", List.of(filter));

        when(trendzClient.getTrendzViewById(eq(viewId), any()))
                .thenReturn(expected);

        doGet("/api/trendz/view/{viewId}", viewId)
                .andExpectAll(
                        r -> assertEquals(200, r.getResponse().getStatus()),
                        r -> assertEquals(expected, read(r, new TypeReference<TrendzViewConfig>() {}))
                );
    }

    @Test
    public void testGetTrendzSummary() throws Exception {
        loginTenantAdmin();

        TrendzSummary expected = new TrendzSummary(
            List.of(Map.of("itemName", "Thermostat T1")),
            List.of(Map.of("modelName", "Thermostat Anomaly Model")),
            List.of(Map.of("calculationName", "Thermostat Calculation Field")),
            List.of(Map.of("modelName", "Thermostat Prediction Model")),
            List.of(Map.of("viewName", "Trendz View 1")),
            List.of(Map.of("chatSummary", "Thermostat view builder"))
        );

        when(trendzClient.getTrendzSummary(any()))
                .thenReturn(expected);

        doGet("/api/trendz/summary")
                .andExpectAll(
                        r -> assertEquals(200, r.getResponse().getStatus()),
                        r -> assertEquals(expected, read(r, new TypeReference<TrendzSummary>() {}))
                );
    }

    @Test
    public void testGetTrendzUsage() throws Exception {
        loginSysAdmin();

        TrendzUsage expected = new TrendzUsage(
                true,
                new TrendzUsage.Entity(true, 4, 10),
                new TrendzUsage.Entity(true, 2, 3),
                new TrendzUsage.Entity(false, 0, 0),
                new TrendzUsage.SimpleEntity(true, 15),
                new TrendzUsage.SimpleEntity(false, 2),
                new TrendzUsage.SimpleEntity(true, 0)
        );

        when(trendzClient.getTrendzUsage(any()))
                .thenReturn(expected);

        doGet("/api/trendz/usage")
                .andExpectAll(
                        r -> assertEquals(200, r.getResponse().getStatus()),
                        r -> assertEquals(expected, read(r, new TypeReference<TrendzUsage>() {}))
                );
    }

    private <T> T read(MvcResult r, TypeReference<T> typeReference) throws IOException {
        return objectMapper.readValue(
                r.getResponse().getContentAsString(),
                typeReference
        );
    }
}
