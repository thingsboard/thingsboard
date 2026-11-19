// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

class LowercaseHeaderNamesFilterTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HeadersController())
            .addFilters(new LowercaseHeaderNamesFilter())
            .build();

    @Test
    void requestHeaderMapHasLowercaseNames() throws Exception {
        mockMvc.perform(post("/headers").header("testHeader", "testValue").header("Stripe-Signature", "t=1"))
                .andExpect(content().string("testheader=testValue,stripe-signature=t=1"));
    }

    @Test
    void multipartRequestHeadersHaveLowercaseNames() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/multipart");
        request.addHeader("testHeader", "testValue");
        MockFilterChain chain = new MockFilterChain();

        new LowercaseHeaderNamesFilter().doFilter(request, new MockHttpServletResponse(), chain);

        StandardMultipartHttpServletRequest multipart = new StandardMultipartHttpServletRequest((HttpServletRequest) chain.getRequest(), true);
        assertThat(multipart.getRequestHeaders().toSingleValueMap()).containsEntry("testheader", "testValue");
    }

    @Test
    void registeredFirstInServletApplications() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LowercaseHeaderNamesAutoConfiguration.class))
                .run(context -> assertThat(context.getBean(FilterRegistrationBean.class).getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE));
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LowercaseHeaderNamesAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
    }

    @RestController
    static class HeadersController {

        @PostMapping("/headers")
        String headers(@RequestHeader Map<String, String> headers) {
            return "testheader=" + headers.get("testheader") + ",stripe-signature=" + headers.get("stripe-signature");
        }

    }

}
