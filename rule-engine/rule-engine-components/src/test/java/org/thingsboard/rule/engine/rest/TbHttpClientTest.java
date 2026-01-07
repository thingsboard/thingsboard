/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.rest;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.util.LinkedMultiValueMap;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class TbHttpClientTest {

    EventLoopGroup eventLoop;
    TbHttpClient client;

    @BeforeEach
    public void setUp() throws Exception {
        client = mock(TbHttpClient.class);
        when(client.getSharedOrCreateEventLoopGroup(any())).thenCallRealMethod();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (eventLoop != null) {
            eventLoop.shutdownGracefully();
        }
    }

    @Test
    public void givenSharedEventLoop_whenGetEventLoop_ThenReturnShared() {
        eventLoop = mock(EventLoopGroup.class);
        assertThat(client.getSharedOrCreateEventLoopGroup(eventLoop), is(eventLoop));
    }

    @Test
    public void givenNull_whenGetEventLoop_ThenReturnShared() {
        eventLoop = client.getSharedOrCreateEventLoopGroup(null);
        assertThat(eventLoop, instanceOf(NioEventLoopGroup.class));
    }

    @Test
    public void testBuildSimpleUri() {
        Mockito.when(client.buildEncodedUri(any(), any())).thenCallRealMethod();
        String url = "http://localhost:8080/";
        URI uri = client.buildEncodedUri(url, null);
        Assertions.assertEquals(url, uri.toString());
    }

    @Test
    public void testBuildUriWithoutProtocol() {
        Mockito.when(client.buildEncodedUri(any(), any())).thenCallRealMethod();
        String url = "localhost:8080/";
        assertThatThrownBy(() -> client.buildEncodedUri(url, null));
    }

    @Test
    public void testBuildInvalidUri() {
        Mockito.when(client.buildEncodedUri(any(), any())).thenCallRealMethod();
        String url = "aaa";
        assertThatThrownBy(() -> client.buildEncodedUri(url, null));
    }

    @Test
    public void testBuildUriWithSpecialSymbols() {
        Mockito.when(client.buildEncodedUri(any(), any())).thenCallRealMethod();
        String url = "http://192.168.1.1/data?d={\"a\": 12}";
        String expected = "http://192.168.1.1/data?d=%7B%22a%22:%2012%7D";
        URI uri = client.buildEncodedUri(url, null);
        Assertions.assertEquals(expected, uri.toString());
    }

    @Test
    public void testProcessMessageWithQueryParamsPatternProcessing() throws Exception {
        // GIVEN
        String path = "/api/notify";
        try (var server = startClientAndServer("localhost", 0)) {
            server.when(
                    request()
                            .withMethod("GET")
                            .withPath(path)
                            .withQueryStringParameter("email", "user+tag@test.com")
                            .withQueryStringParameter("device", "sensor-1")
                            .withQueryStringParameter("custom-header", "header-value")
                            .withQueryStringParameter("temp", "25.5")
                            .withQueryStringParameter("location", "room-1/zone-A")
            ).respond(
                    response().withStatusCode(200)
            );

            var config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
            config.setRestEndpointUrlPattern("http://localhost:" + server.getPort() + path);
            config.setRequestMethod("GET");
            config.setUseSimpleClientHttpFactory(true);
            config.setQueryParams(List.of(
                    new QueryParam("email", "${userEmail}"),              // ${} from metadata
                    new QueryParam("device", "${deviceName}"),            // ${} from metadata
                    new QueryParam("${dynamicParam}", "${dynamicValue}"), // ${} in both key and value
                    new QueryParam("temp", "$[temperature]"),             // $[] from data
                    new QueryParam("location", "$[sensor.location]")      // $[] from nested data
            ));

            var metaData = new TbMsgMetaData();
            metaData.putValue("userEmail", "user+tag@test.com");
            metaData.putValue("deviceName", "sensor-1");
            metaData.putValue("dynamicParam", "custom-header");
            metaData.putValue("dynamicValue", "header-value");

            var msg = TbMsg.newMsg()
                    .type(TbMsgType.POST_TELEMETRY_REQUEST)
                    .originator(new DeviceId(EntityId.NULL_UUID))
                    .metaData(metaData)
                    .data("""
                            {"temperature": 25.5, "sensor": {"location": "room-1/zone-A"}}
                            """)
                    .build();

            // WHEN-THEN
            processMessageAndWait(config, msg);
        }
    }

    @Test
    public void testProcessMessageWithJsonInUrlVariable() throws Exception {
        // GIVEN
        String path = "/api";
        String paramValue = "[{\"test\":\"test\"}]";

        try (var server = startClientAndServer("localhost", 0)) {
            server.when(
                    request()
                            .withMethod("GET")
                            .withPath(path)
                            .withQueryStringParameter("data", paramValue)
            ).respond(
                    response().withStatusCode(200)
            );

            var config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
            config.setRestEndpointUrlPattern("http://localhost:" + server.getPort() + path + "?data=" + paramValue);
            config.setRequestMethod("GET");
            config.setQueryParams(null);

            var msg = TbMsg.newMsg()
                    .type(TbMsgType.POST_TELEMETRY_REQUEST)
                    .originator(new DeviceId(UUID.randomUUID()))
                    .metaData(new TbMsgMetaData())
                    .data(TbMsg.EMPTY_JSON_OBJECT)
                    .build();

            // WHEN-THEN
            processMessageAndWait(config, msg);
        }
    }

    private void processMessageAndWait(TbRestApiCallNodeConfiguration config, TbMsg msg) throws Exception {
        var httpClient = new TbHttpClient(config, eventLoop);
        var ctx = mock(TbContext.class);
        when(ctx.transformMsg(eq(msg), any(), any())).thenReturn(msg);

        var latch = new CountDownLatch(1);
        var error = new AtomicReference<Throwable>();

        httpClient.processMessage(ctx, msg,
                m -> {
                    ctx.tellSuccess(m);
                    latch.countDown();
                },
                (m, t) -> {
                    ctx.tellFailure(m, t);
                    error.set(t);
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Request should complete within timeout");
        assertNull(error.get(), "Request should succeed, but got: " + error.get());

        verify(ctx).tellSuccess(any());
        verify(ctx, never()).tellFailure(any(), any());
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testQueryParamsEncoding(String endpointUrl, List<QueryParam> queryParams, String expectedEncodedUrl) {
        // GIVEN
        Mockito.when(client.buildEncodedUri(any(), any())).thenCallRealMethod();

        // WHEN
        URI uri = client.buildEncodedUri(endpointUrl, queryParams);

        // THEN
        Assertions.assertEquals(expectedEncodedUrl, uri.toASCIIString());
    }

    private static Stream<Arguments> testQueryParamsEncoding() {
        return Stream.of(
                Arguments.of(
                        Named.named("ISO 8601 date-time in value", "http://somecompany/api/data/fetch"),
                        List.of(new QueryParam("ts", "2016-08-01T09:06:06.0+02:00")),
                        "http://somecompany/api/data/fetch?ts=2016-08-01T09%3A06%3A06.0%2B02%3A00"
                ),
                Arguments.of(
                        Named.named("email with plus sign in value", "http://localhost:8080/api/user/sendActivationMail"),
                        List.of(new QueryParam("email", "someperson+test1289@thingsboard.io")),
                        "http://localhost:8080/api/user/sendActivationMail?email=someperson%2Btest1289%40thingsboard.io"
                ),
                Arguments.of(
                        Named.named("plus mixed with spaces in value", "http://url/api"),
                        List.of(new QueryParam("q", "a + b")),
                        "http://url/api?q=a%20%2B%20b"
                ),
                Arguments.of(
                        Named.named("colon in value", "http://url/api"),
                        List.of(new QueryParam("time", "12:00")),
                        "http://url/api?time=12%3A00"
                ),
                Arguments.of(
                        Named.named("slash in value", "http://url"),
                        List.of(new QueryParam("ref", "/home/user")),
                        "http://url?ref=%2Fhome%2Fuser"
                ),
                Arguments.of(
                        Named.named("comma and semicolon in value", "http://url"),
                        List.of(new QueryParam("l", "a,b;c")),
                        "http://url?l=a%2Cb%3Bc"
                ),
                Arguments.of(
                        Named.named("ampersand and equals in value", "http://url"),
                        List.of(new QueryParam("q", "key1=value1&key2=value2")),
                        "http://url?q=key1%3Dvalue1%26key2%3Dvalue2"
                ),
                Arguments.of(
                        Named.named("JSON in value", "http://url"),
                        List.of(new QueryParam("json", """
                                {
                                    "string": "hello",
                                    "integer": 42,
                                    "float": 3.14,
                                    "boolTrue": true,
                                    "boolFalse": false,
                                    "null": null,
                                    "array": [
                                        1,
                                        "two",
                                        true,
                                        null
                                    ],
                                    "object": {
                                        "nested": "value"
                                    }
                                }""")),
                        "http://url?json=%7B%0A%20%20%20%20%22string%22%3A%20%22hello%22%2C%0A%20%20%20%20%22integer%22%3A%2042%2C%0A%20%20%20%20%22float%22%3A%203.14%2C%0A%20%20%20%20%22boolTrue%22%3A%20true%2C%0A%20%20%20%20%22boolFalse%22%3A%20false%2C%0A%20%20%20%20%22null%22%3A%20null%2C%0A%20%20%20%20%22array%22%3A%20%5B%0A%20%20%20%20%20%20%20%201%2C%0A%20%20%20%20%20%20%20%20%22two%22%2C%0A%20%20%20%20%20%20%20%20true%2C%0A%20%20%20%20%20%20%20%20null%0A%20%20%20%20%5D%2C%0A%20%20%20%20%22object%22%3A%20%7B%0A%20%20%20%20%20%20%20%20%22nested%22%3A%20%22value%22%0A%20%20%20%20%7D%0A%7D"
                ),
                Arguments.of(
                        Named.named("UTF-8 in query", "http://url/cafes"),
                        List.of(
                                new QueryParam("nom", "Le Goût Moderne"),
                                new QueryParam("назва", "У Миколи \uD83D\uDE0B")
                        ),
                        "http://url/cafes?nom=Le%20Go%C3%BBt%20Moderne&%D0%BD%D0%B0%D0%B7%D0%B2%D0%B0=%D0%A3%20%D0%9C%D0%B8%D0%BA%D0%BE%D0%BB%D0%B8%20%F0%9F%98%8B"
                ),
                Arguments.of(
                        Named.named("empty value", "http://url/empty"),
                        List.of(new QueryParam("name", "")),
                        "http://url/empty?name="
                ),
                Arguments.of(
                        Named.named("blank value (spaces)", "http://url/empty"),
                        List.of(new QueryParam("name", "  ")),
                        "http://url/empty?name=%20%20"
                ),
                Arguments.of(
                        Named.named("empty key", "http://url/empty"),
                        List.of(new QueryParam("", "value")),
                        "http://url/empty?=value"
                ),
                Arguments.of(
                        Named.named("blank key (spaces)", "http://url/empty"),
                        List.of(new QueryParam("  ", "value")),
                        "http://url/empty?%20%20=value"
                ),
                Arguments.of(
                        Named.named("blank key with value that needs to be encoded", "http://url/empty"),
                        List.of(new QueryParam("  ", "value1+value2")),
                        "http://url/empty?%20%20=value1%2Bvalue2"
                ),
                Arguments.of(
                        Named.named("blank key and value", "http://url/empty"),
                        List.of(new QueryParam("  ", "  ")),
                        "http://url/empty?%20%20=%20%20"
                ),
                Arguments.of(
                        Named.named("fragment with query params", "http://url#frag"),
                        List.of(new QueryParam("docs", "مستندات عقدة القاعدة\n")),
                        "http://url?docs=%D9%85%D8%B3%D8%AA%D9%86%D8%AF%D8%A7%D8%AA%20%D8%B9%D9%82%D8%AF%D8%A9%20%D8%A7%D9%84%D9%82%D8%A7%D8%B9%D8%AF%D8%A9%0A#frag"
                ),
                Arguments.of(
                        Named.named("fragment only", "http://url#frag"),
                        Collections.emptyList(),
                        "http://url#frag"
                ),
                Arguments.of(
                        Named.named("multiple query params (ordered)", "http://url/api"),
                        List.of(
                                new QueryParam("param1", "value1"),
                                new QueryParam("param2", "value2"),
                                new QueryParam("param3", "value3")
                        ),
                        "http://url/api?param1=value1&param2=value2&param3=value3"
                ),
                Arguments.of(
                        Named.named("hash (#) in value", "http://url/api"),
                        List.of(new QueryParam("color", "#ff0000")),
                        "http://url/api?color=%23ff0000"
                ),
                Arguments.of(
                        Named.named("question mark (?) in value", "http://url/api"),
                        List.of(new QueryParam("query", "what?")),
                        "http://url/api?query=what%3F"
                ),
                Arguments.of(
                        Named.named("percent sign (%) in value", "http://url/api"),
                        List.of(new QueryParam("discount", "50%")),
                        "http://url/api?discount=50%25"
                ),
                Arguments.of(
                        Named.named("already encoded string - double encoding", "http://url/api"),
                        List.of(new QueryParam("encoded", "%20")),
                        "http://url/api?encoded=%2520"
                ),
                Arguments.of(
                        Named.named("URL with port number", "http://localhost:8080/api/v1/data"),
                        List.of(new QueryParam("key", "value")),
                        "http://localhost:8080/api/v1/data?key=value"
                ),
                Arguments.of(
                        Named.named("URL with userinfo (auth)", "http://user:password@hostname/path"),
                        List.of(new QueryParam("secure", "true")),
                        "http://user:password@hostname/path?secure=true"
                ),
                Arguments.of(
                        Named.named("IPv4 address in URL", "http://192.168.1.100:9090/endpoint"),
                        List.of(new QueryParam("ip", "test")),
                        "http://192.168.1.100:9090/endpoint?ip=test"
                ),
                Arguments.of(
                        Named.named("IPv6 address in URL", "http://[::1]:8080/api"),
                        List.of(new QueryParam("ipv6", "true")),
                        "http://[::1]:8080/api?ipv6=true"
                ),
                Arguments.of(
                        Named.named("HTTPS protocol", "https://secure.example.com/api"),
                        List.of(new QueryParam("token", "abc123")),
                        "https://secure.example.com/api?token=abc123"
                ),
                Arguments.of(
                        Named.named("pipe (|) in value", "http://url/api"),
                        List.of(new QueryParam("filter", "a|b|c")),
                        "http://url/api?filter=a%7Cb%7Cc"
                ),
                Arguments.of(
                        Named.named("caret (^) and backtick (`) in value", "http://url/api"),
                        List.of(new QueryParam("special", "a^b`c")),
                        "http://url/api?special=a%5Eb%60c"
                ),
                Arguments.of(
                        Named.named("tab character in value", "http://url/api"),
                        List.of(new QueryParam("data", "col1\tcol2\tcol3")),
                        "http://url/api?data=col1%09col2%09col3"
                ),
                Arguments.of(
                        Named.named("CRLF in value", "http://url/api"),
                        List.of(new QueryParam("text", "line1\r\nline2")),
                        "http://url/api?text=line1%0D%0Aline2"
                ),
                Arguments.of(
                        Named.named("square brackets in value", "http://url/api"),
                        List.of(new QueryParam("array", "[1,2,3]")),
                        "http://url/api?array=%5B1%2C2%2C3%5D"
                ),
                Arguments.of(
                        Named.named("single and double quotes in value", "http://url/api"),
                        List.of(new QueryParam("quoted", "He said \"Hello\" and 'Hi'")),
                        "http://url/api?quoted=He%20said%20%22Hello%22%20and%20%27Hi%27"
                ),
                Arguments.of(
                        Named.named("angle brackets in value (XSS test)", "http://url/api"),
                        List.of(new QueryParam("html", "<script>alert('xss')</script>")),
                        "http://url/api?html=%3Cscript%3Ealert%28%27xss%27%29%3C%2Fscript%3E"
                ),
                Arguments.of(
                        Named.named("backslash in value", "http://url/api"),
                        List.of(new QueryParam("path", "C:\\Users\\test")),
                        "http://url/api?path=C%3A%5CUsers%5Ctest"
                ),
                Arguments.of(
                        Named.named("at sign (@) in value", "http://url/api"),
                        List.of(new QueryParam("contact", "user@domain.com")),
                        "http://url/api?contact=user%40domain.com"
                ),
                Arguments.of(
                        Named.named("URL as value", "http://url/api"),
                        List.of(new QueryParam("redirect", "https://example.com/path?foo=bar")),
                        "http://url/api?redirect=https%3A%2F%2Fexample.com%2Fpath%3Ffoo%3Dbar"
                ),
                Arguments.of(
                        Named.named("empty list (not null)", "http://url/api"),
                        Collections.emptyList(),
                        "http://url/api"
                ),
                Arguments.of(
                        Named.named("underscore and hyphen in key/value", "http://url/api"),
                        List.of(new QueryParam("my-key_name", "my-value_data")),
                        "http://url/api?my-key_name=my-value_data"
                ),
                Arguments.of(
                        Named.named("dots in key and value", "http://url/api"),
                        List.of(new QueryParam("version.major", "1.2.3")),
                        "http://url/api?version.major=1.2.3"
                ),
                Arguments.of(
                        Named.named("multiple reserved chars combined", "http://url/api"),
                        List.of(new QueryParam("complex", "a=1&b=2#section?query")),
                        "http://url/api?complex=a%3D1%26b%3D2%23section%3Fquery"
                ),
                Arguments.of(
                        Named.named("trailing slash in URL", "http://url/api/"),
                        List.of(new QueryParam("param", "value")),
                        "http://url/api/?param=value"
                ),
                Arguments.of(
                        Named.named("long value (1000 chars)", "http://url/api"),
                        List.of(new QueryParam("long", "a".repeat(1000))),
                        "http://url/api?long=" + "a".repeat(1000)
                ),
                Arguments.of(
                        Named.named("Chinese characters in value", "http://url/api"),
                        List.of(new QueryParam("greeting", "你好世界")),
                        "http://url/api?greeting=%E4%BD%A0%E5%A5%BD%E4%B8%96%E7%95%8C"
                ),
                Arguments.of(
                        Named.named("Japanese characters in value", "http://url/api"),
                        List.of(new QueryParam("text", "こんにちは")),
                        "http://url/api?text=%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF"
                ),
                Arguments.of(
                        Named.named("existing query params in URL", "http://url/api?existing=param"),
                        List.of(new QueryParam("new", "value")),
                        "http://url/api?existing=param&new=value"
                ),
                Arguments.of(
                        Named.named("existing query and fragment in URL", "http://url/api?existing=param#section"),
                        List.of(new QueryParam("additional", "data")),
                        "http://url/api?existing=param&additional=data#section"
                ),
                Arguments.of(
                        Named.named("null byte in value", "http://url/api"),
                        List.of(new QueryParam("data", "before\u0000after")),
                        "http://url/api?data=before%00after"
                ),
                Arguments.of(
                        Named.named("form feed and vertical tab in value", "http://url/api"),
                        List.of(new QueryParam("whitespace", "a\fb\u000Bc")),
                        "http://url/api?whitespace=a%0Cb%0Bc"
                ),
                Arguments.of(
                        Named.named("tilde (unreserved, not encoded)", "http://url/api"),
                        List.of(new QueryParam("pattern", "~user")),
                        "http://url/api?pattern=~user"
                ),
                Arguments.of(
                        Named.named("asterisk in value", "http://url/api"),
                        List.of(new QueryParam("wildcard", "*.txt")),
                        "http://url/api?wildcard=%2A.txt"
                ),
                Arguments.of(
                        Named.named("curly braces in key", "http://url/api"),
                        List.of(new QueryParam("{key}", "value")),
                        "http://url/api?%7Bkey%7D=value"
                ),
                Arguments.of(
                        Named.named("curly braces in value", "http://url/api"),
                        List.of(new QueryParam("data", "{value}")),
                        "http://url/api?data=%7Bvalue%7D"
                ),
                Arguments.of(
                        Named.named("curly braces in both key and value", "http://url/api"),
                        List.of(new QueryParam("{param}", "{data}")),
                        "http://url/api?%7Bparam%7D=%7Bdata%7D"
                ),
                Arguments.of(
                        Named.named("duplicate param names with different values", "http://url/api"),
                        List.of(
                                new QueryParam("test", "a+b"),
                                new QueryParam("test", "b c")
                        ),
                        "http://url/api?test=a%2Bb&test=b%20c"
                )
        );
    }

    @Test
    public void testHeadersToMetaData() {
        Map<String, List<String>> headers = new LinkedMultiValueMap<>();
        headers.put("Content-Type", List.of("binary"));
        headers.put("Set-Cookie", List.of("sap-context=sap-client=075; path=/", "sap-token=sap-client=075; path=/"));

        TbMsgMetaData metaData = new TbMsgMetaData();

        willCallRealMethod().given(client).headersToMetaData(any(), any());

        client.headersToMetaData(headers, metaData::putValue);

        Map<String, String> data = metaData.getData();

        Assertions.assertEquals(2, data.size());
        Assertions.assertEquals(data.get("Content-Type"), "binary");
        Assertions.assertEquals(data.get("Set-Cookie"), "[\"sap-context=sap-client=075; path=/\",\"sap-token=sap-client=075; path=/\"]");
    }

}
