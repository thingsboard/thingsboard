package com.iot.thingmodel.common;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void mapsApiException() throws Exception {
        mockMvc.perform(post("/api/test/api-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TSL_INVALID"))
                .andExpect(jsonPath("$.message").value("bad tsl"))
                .andExpect(jsonPath("$.issues[0].code").value("ID_DUP"))
                .andExpect(jsonPath("$.issues[0].path").value("/id"))
                .andExpect(jsonPath("$.retry").value("/api/products/P1/sync"));
    }

    @Test
    void mapsUnexpectedException() throws Exception {
        mockMvc.perform(post("/api/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("服务内部错误"));
    }

    @RestController
    static class TestController {

        @PostMapping("/api/test/api-error")
        public void apiError() {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "TSL_INVALID",
                    "bad tsl",
                    List.of(new Issue("ID_DUP", "/id", "duplicate")),
                    "/api/products/P1/sync");
        }

        @PostMapping("/api/test/generic-error")
        public void genericError() {
            throw new IllegalStateException("boom");
        }
    }
}
