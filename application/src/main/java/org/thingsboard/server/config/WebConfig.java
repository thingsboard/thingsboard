// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thingsboard.server.utils.MiscUtils;

import java.io.IOException;

@Controller
public class WebConfig {

    @RequestMapping(value = {"/assets", "/assets/", "/{path:^(?!api$)(?!assets$)(?!static$)(?!webjars$)(?!swagger-ui$)[^\\.]*}/**"})
    public String redirect() {
        return "forward:/index.html";
    }

    @RequestMapping("/swagger-ui.html")
    public void redirectSwagger(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String baseUrl = MiscUtils.constructBaseUrl(request);
        response.sendRedirect(baseUrl + "/swagger-ui/");
    }

    @RequestMapping("/swagger-ui/")
    public String redirectSwaggerIndex() throws IOException {
        return "forward:/swagger-ui/index.html";
    }

}
