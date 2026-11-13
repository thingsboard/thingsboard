// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.VARY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StaticResourceChainConfigTest.WebMvcConfig.class, properties = "spring.config.name=thingsboard")
@AutoConfigureMockMvc(addFilters = false)
public class StaticResourceChainConfigTest {

    private static final byte[] RAW = "console.log('raw');\n".repeat(100).getBytes(StandardCharsets.UTF_8);
    private static final byte[] GZ = gzip(RAW);
    private static final byte[] BR = "not-really-brotli".getBytes(StandardCharsets.UTF_8);

    @TempDir
    static Path root;

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void staticLocations(DynamicPropertyRegistry registry) {
        registry.add("spring.web.resources.static-locations", () -> root.toUri().toString());
    }

    @BeforeAll
    static void createStaticFiles() throws IOException {
        Files.write(root.resolve("app.js"), RAW);
        Files.write(root.resolve("app.js.gz"), GZ);
        Files.write(root.resolve("app.js.br"), BR);
    }

    @Test
    public void servesPrebuiltGzip() throws Exception {
        mvc.perform(get("/app.js").header(ACCEPT_ENCODING, "gzip"))
                .andExpect(status().isOk())
                .andExpect(header().string(CONTENT_ENCODING, "gzip"))
                .andExpect(header().string(VARY, containsString(ACCEPT_ENCODING)))
                .andExpect(content().bytes(GZ));
    }

    @Test
    public void servesPrebuiltBrotli() throws Exception {
        mvc.perform(get("/app.js").header(ACCEPT_ENCODING, "br, gzip"))
                .andExpect(status().isOk())
                .andExpect(header().string(CONTENT_ENCODING, "br"))
                .andExpect(header().string(VARY, containsString(ACCEPT_ENCODING)))
                .andExpect(content().bytes(BR));
    }

    @Test
    public void servesRawFileWithoutAcceptEncoding() throws Exception {
        mvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(CONTENT_ENCODING))
                .andExpect(content().bytes(RAW));
    }

    // not @Configuration: the app's and AbstractControllerTest's @ComponentScan("org.thingsboard.server") would pick it up
    @ImportAutoConfiguration({DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
    static class WebMvcConfig {
    }

    private static byte[] gzip(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

}
