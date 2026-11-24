// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.controller.JsonHttpIntegrationMsg;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.exception.ThingsboardRuntimeException;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AbstractHttpIntegrationTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(AbstractHttpIntegration.class);
    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    private Level originalLevel;
    private FailingHttpIntegration integration;

    @BeforeEach
    void setUp() throws Exception {
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        logAppender.start();
        logger.addAppender(logAppender);

        integration = new FailingHttpIntegration();
        integration.init(new TbIntegrationInitParams(mock(IntegrationContext.class), integrationConfig(), null, null));
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logger.setLevel(originalLevel);
    }

    @Test
    void errorShownInDebugEventIsLoggedAtDebugOnly() {
        integration.failure = new ThingsboardRuntimeException("Creating devices is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);

        integration.process(uplink());

        assertThat(levels()).containsExactly(Level.DEBUG);
    }

    @Test
    void unexpectedErrorIsLoggedAtWarnWithoutStackTrace() {
        integration.failure = new RuntimeException("Queue is unavailable");

        integration.process(uplink());

        assertThat(logAppender.list).filteredOn(event -> event.getLevel() == Level.WARN).singleElement().satisfies(event -> {
            assertThat(event.getFormattedMessage()).isEqualTo("[" + integration.getConfiguration().getId() + "] Failed to process uplink: Queue is unavailable");
            assertThat(event.getThrowableProxy()).isNull();
        });
        assertThat(logAppender.list).filteredOn(event -> event.getLevel() == Level.DEBUG).singleElement()
                .satisfies(event -> assertThat(event.getThrowableProxy().getMessage()).isEqualTo("Queue is unavailable"));
    }

    @Test
    void repeatedUnexpectedErrorsAreLoggedAtWarnOnce() {
        integration.failure = new RuntimeException("Queue is unavailable");

        integration.process(uplink());
        integration.process(uplink());

        assertThat(levels()).containsExactly(Level.WARN, Level.DEBUG, Level.DEBUG);
    }

    @Test
    void lineBreaksInWarnMessageAreReplaced() {
        integration.failure = new ParseException("Unparseable date: \"x\r\n2026-11-24 INFO forged\"", 0);

        integration.process(uplink());

        assertThat(logAppender.list).filteredOn(event -> event.getLevel() == Level.WARN).singleElement()
                .satisfies(event -> assertThat(event.getFormattedMessage()).endsWith("Unparseable date: \"x  2026-11-24 INFO forged\""));
    }

    private List<Level> levels() {
        return logAppender.list.stream().map(ILoggingEvent::getLevel).toList();
    }

    private static JsonHttpIntegrationMsg uplink() {
        return new JsonHttpIntegrationMsg(Map.of(), JacksonUtil.newObjectNode(), new DeferredResult<>());
    }

    private static Integration integrationConfig() {
        ObjectNode root = JacksonUtil.newObjectNode();
        root.putObject("metadata");
        Integration integration = new Integration();
        integration.setId(new IntegrationId(UUID.randomUUID()));
        integration.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        integration.setName("HTTP check");
        integration.setConfiguration(root);
        integration.setDebugSettings(DebugSettings.off());
        return integration;
    }

    private static class FailingHttpIntegration extends AbstractHttpIntegration<JsonHttpIntegrationMsg> {

        private Exception failure;

        @Override
        protected ResponseEntity doProcess(JsonHttpIntegrationMsg msg) throws Exception {
            if (failure != null) {
                throw failure;
            }
            return fromStatus(HttpStatus.OK);
        }

        @Override
        protected String getTypeUplink(JsonHttpIntegrationMsg msg) {
            return "Uplink";
        }

    }

}
