package com.iot.thingmodel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.auth.AccessGuard;
import com.iot.thingmodel.auth.AuthInterceptor;
import com.iot.thingmodel.auth.CurrentUser;
import com.iot.thingmodel.common.GlobalExceptionHandler;
import com.iot.thingmodel.product.CatalogView;
import com.iot.thingmodel.product.CreateProductCommand;
import com.iot.thingmodel.product.ProductDetail;
import com.iot.thingmodel.product.ProductResponse;
import com.iot.thingmodel.product.ProductService;
import com.iot.thingmodel.product.ThingModelView;
import com.iot.thingmodel.product.ValidationResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID tenantId = UUID.randomUUID();
    private final ProductService productService = mock(ProductService.class);
    private final ProductController controller = new ProductController(new AccessGuard(), productService);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @BeforeEach
    void setUp() {
        ProductResponse response = new ProductResponse(
                "P1", "1.0", "profile", "asset", "chain", 1, 0, 0, 100,
                "PUBLISHED", List.of());
        when(productService.create(any())).thenReturn(response);
        when(productService.list(any(), any())).thenReturn(List.of());
        when(productService.get(any(), eq("P1"))).thenReturn(
                new ProductDetail(null, null, null));
        when(productService.sync(any(), eq("P1"))).thenReturn(response);
        when(productService.validate(any(), eq("P1"), any())).thenReturn(
                new ValidationResponse(true, List.of(), List.of()));
        when(productService.getThingModel(any(), eq("P1"))).thenReturn(
                new ThingModelView("P1", "1.0", "PUBLISHED", false,
                        objectMapper.createObjectNode()));
        when(productService.catalog(any(), eq("P1"))).thenReturn(
                new CatalogView("P1", "1.0", List.of(), List.of(), List.of()));
    }

    @Test
    void assignsTenantFromCurrentUser() throws Exception {
        CurrentUser user = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");

        mockMvc.perform(post("/api/products")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"P1","name":"Product","tsl":{}}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<CreateProductCommand> captor = ArgumentCaptor.forClass(CreateProductCommand.class);
        verify(productService).create(captor.capture());
        assertEquals(tenantId, captor.getValue().tenantId());
    }

    @Test
    void systemAdminMustSpecifyTenant() throws Exception {
        CurrentUser user = new CurrentUser("root", null, "SYS_ADMIN", "root@b");

        mockMvc.perform(post("/api/products")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"P1","name":"Product","tsl":{}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tenantAdminCannotSpecifyAnotherTenant() throws Exception {
        CurrentUser user = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");
        String otherTenant = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/products")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "tenantId", otherTenant,
                                "productId", "P1",
                                "name", "Product",
                                "tsl", java.util.Map.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsMissingTsl() throws Exception {
        CurrentUser user = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");

        mockMvc.perform(post("/api/products")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"P1","name":"Product"}
                                """))
                .andExpect(status().isBadRequest());

        verify(productService, never()).create(any());
    }

    @Test
    void listsProducts() throws Exception {
        CurrentUser user = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");

        mockMvc.perform(get("/api/products")
                        .param("keyword", "wifi")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user))
                .andExpect(status().isOk());

        verify(productService).list(any(), eq("wifi"));
    }

    @Test
    void getsProduct() throws Exception {
        CurrentUser user = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");

        mockMvc.perform(get("/api/products/{productId}", "P1")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user))
                .andExpect(status().isOk());

        verify(productService).get(any(), eq("P1"));
    }

    @Test
    void syncsProduct() throws Exception {
        CurrentUser user = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");

        mockMvc.perform(post("/api/products/{productId}/sync", "P1")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tbRuleChainId").value("chain"));

        verify(productService).sync(any(), eq("P1"));
    }

    @Test
    void validatesThingModel() throws Exception {
        CurrentUser user = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");

        mockMvc.perform(post("/api/products/{productId}/thing-model/validate", "P1")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tsl":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        verify(productService).validate(any(), eq("P1"), any());
    }

    @Test
    void getsThingModel() throws Exception {
        CurrentUser user = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");

        mockMvc.perform(get("/api/products/{productId}/thing-model", "P1")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("P1"));

        verify(productService).getThingModel(any(), eq("P1"));
    }

    @Test
    void getsCatalog() throws Exception {
        CurrentUser user = new CurrentUser("u1", tenantId.toString(), "TENANT_ADMIN", "a@b");

        mockMvc.perform(get("/api/products/{productId}/catalog", "P1")
                        .requestAttr(AuthInterceptor.CURRENT_USER_ATTRIBUTE, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("P1"));

        verify(productService).catalog(any(), eq("P1"));
    }
}
