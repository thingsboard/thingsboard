// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

/**
 * Loads the real thingsboard.yml, so the test fails if its static resource chain keys stop binding again.
 */
@SpringBootTest(classes = StaticResourceCompressionTest.WebMvcConfig.class, properties = "spring.config.name=thingsboard")
@AutoConfigureMockMvc(addFilters = false)
public class StaticResourceCompressionTest {

    private static final byte[] JS = "console.log('ThingsBoard');\n".repeat(100).getBytes(StandardCharsets.UTF_8);
    private static final byte[] JS_GZ = gzip(JS);
    private static final byte[] CSS = "@font-face { src: url(font.woff2); }\n".getBytes(StandardCharsets.UTF_8);

    @TempDir
    static Path webRoot;

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void staticLocations(DynamicPropertyRegistry registry) {
        registry.add("spring.web.resources.static-locations", () -> webRoot.toUri().toString());
    }

    @BeforeAll
    static void createStaticFiles() throws IOException {
        Files.write(webRoot.resolve("main.js"), JS);
        Files.write(webRoot.resolve("main.js.gz"), JS_GZ);
        Files.write(webRoot.resolve("styles.css"), CSS);
        Files.write(webRoot.resolve("font.woff2"), new byte[]{1, 2, 3});
    }

    @Test
    public void servesPrebuiltGzipWhenClientAcceptsIt() throws Exception {
        mvc.perform(get("/main.js").header(ACCEPT_ENCODING, "gzip, deflate, br"))
                .andExpect(status().isOk())
                .andExpect(header().string(CONTENT_ENCODING, "gzip"))
                .andExpect(header().string(VARY, containsString(ACCEPT_ENCODING)))
                .andExpect(content().contentTypeCompatibleWith("text/javascript"))
                .andExpect(content().bytes(JS_GZ));
    }

    @Test
    public void servesRawFileWhenClientDoesNotAcceptGzip() throws Exception {
        mvc.perform(get("/main.js"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(CONTENT_ENCODING))
                .andExpect(content().bytes(JS));
    }

    @Test
    public void servesStylesheetUnchanged() throws Exception {
        // the content version strategy would rewrite url(font.woff2) to url(font-<md5>.woff2)
        mvc.perform(get("/styles.css").header(ACCEPT_ENCODING, "gzip"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(CONTENT_ENCODING))
                .andExpect(content().bytes(CSS));
    }

    // not @Configuration: @ComponentScan("org.thingsboard.server") in other tests would pick it up
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
