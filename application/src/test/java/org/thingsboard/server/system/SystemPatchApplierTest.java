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
package org.thingsboard.server.system;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.system.SystemPatchApplier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SystemPatchApplierTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private InstallScripts installScripts;

    @Mock
    private WidgetTypeService widgetTypeService;

    @InjectMocks
    private SystemPatchApplier reconciler;

    @TempDir
    Path tempDir;

    @ParameterizedTest(name = "Parse version {0} should return major={1}, minor={2}, patch={3}")
    @CsvSource({
            "4.2.1, 4, 2, 1, 0",
            "4.2.0, 4, 2, 0, 0",
            "4.2, 4, 2, 0, 0",
            "4.0.1.2, 4, 0, 1, 2",
            "4, 4, 0, 0, 0",
            "1.0.5.7, 1, 0, 5, 7",
            "10.20.30.40, 10, 20, 30, 40",
            "0.0.1, 0, 0, 1, 0"
    })
    void testParseVersion(String versionString, int expectedMajor, int expectedMinor, int expectedMaintenance, int expectedPatch) {
        SystemPatchApplier.VersionInfo version = ReflectionTestUtils.invokeMethod(reconciler, "parseVersion", versionString);

        assertNotNull(version, "Version should not be null for: " + versionString);
        assertEquals(expectedMajor, version.major(), "Major version mismatch");
        assertEquals(expectedMinor, version.minor(), "Minor version mismatch");
        assertEquals(expectedMaintenance, version.maintenance(), "Maintenance version mismatch");
        assertEquals(expectedPatch, version.patch(), "Patch version mismatch");
    }

    @ParameterizedTest(name = "Parse invalid version: {0}")
    @CsvSource({
            "invalid",
            "a.b.c",
            "1.2.y.x",
            "''",
            "1.x.3"
    })
    void testParseInvalidVersion(String invalidVersion) {
        SystemPatchApplier.VersionInfo version = ReflectionTestUtils.invokeMethod(reconciler, "parseVersion", invalidVersion);
        assertNull(version, "Version should be null for invalid input: " + invalidVersion);
    }

    @Test
    void whenLockIsNotAcquired_thenAcquiredIsSuccess() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyLong())).thenReturn(true);

        Boolean acquired = ReflectionTestUtils.invokeMethod(reconciler, "acquireAdvisoryLock");

        assertEquals(Boolean.TRUE, acquired);
        verify(jdbcTemplate).queryForObject(contains("pg_try_advisory_lock"), eq(Boolean.class), anyLong());
    }

    @Test
    void whenLockIsAlreadyAcquired_thenAcquiredIsFailed() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyLong())).thenReturn(false);

        Boolean acquired = ReflectionTestUtils.invokeMethod(reconciler, "acquireAdvisoryLock");

        assertNotEquals(Boolean.TRUE, acquired);
    }

    @Test
    void testReleaseAdvisoryLock() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyLong()))
                .thenReturn(true);

        ReflectionTestUtils.invokeMethod(reconciler, "releaseAdvisoryLock");

        verify(jdbcTemplate).queryForObject(
                contains("pg_advisory_unlock"), eq(Boolean.class), anyLong());
    }

    @Test
    void whenWidgetNotFound_thenThrowException() throws Exception {
        Path widgetTypesDir = tempDir.resolve("widget_types");
        Files.createDirectories(widgetTypesDir);
        when(installScripts.getWidgetTypesDir()).thenReturn(widgetTypesDir);

        WidgetTypeDetails testWidget = createTestWidgetType("test_widget", "Test Widget");
        String json = JacksonUtil.toString(testWidget);
        assertNotNull(json);
        Files.writeString(widgetTypesDir.resolve("test_widget.json"), json);

        when(widgetTypeService.findWidgetTypeDetailsByTenantIdAndFqn(TenantId.SYS_TENANT_ID, "test_widget")).thenReturn(null);

        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes"));
    }

    @Test
    void whenDescriptorChanged_thenUpdateTheExistingWidget() throws Exception {
        Path widgetTypesDir = tempDir.resolve("widget_types");
        Files.createDirectories(widgetTypesDir);
        when(installScripts.getWidgetTypesDir()).thenReturn(widgetTypesDir);

        WidgetTypeDetails fileWidget = createTestWidgetType("test_widget", "Test Widget");
        fileWidget.setDescriptor(JacksonUtil.toJsonNode("{\"type\":\"latest\",\"version\":2}"));
        String json = JacksonUtil.toString(fileWidget);
        assertNotNull(json);
        Files.writeString(widgetTypesDir.resolve("test_widget.json"), json);

        WidgetTypeDetails existingWidget = createTestWidgetType("test_widget", "Test Widget");
        existingWidget.setId(new WidgetTypeId(UUID.randomUUID()));
        existingWidget.setDescriptor(JacksonUtil.toJsonNode("{\"type\":\"latest\",\"version\":1}"));

        when(widgetTypeService.findWidgetTypeDetailsByTenantIdAndFqn(TenantId.SYS_TENANT_ID, "test_widget"))
                .thenReturn(existingWidget);

        Integer updated = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes");

        assertEquals(1, updated);
        verify(widgetTypeService).saveWidgetType(argThat(w ->
                w.getDescriptor().get("version").asInt() == 2
        ));
    }

    @Test
    void whenNameChanged_thenUpdateTheExistingWidget() throws Exception {
        Path widgetTypesDir = tempDir.resolve("widget_types");
        Files.createDirectories(widgetTypesDir);
        when(installScripts.getWidgetTypesDir()).thenReturn(widgetTypesDir);

        WidgetTypeDetails fileWidget = createTestWidgetType("test_widget", "New Name");
        String json = JacksonUtil.toString(fileWidget);
        assertNotNull(json);
        Files.writeString(widgetTypesDir.resolve("test_widget.json"), json);

        WidgetTypeDetails existingWidget = createTestWidgetType("test_widget", "Old Name");
        existingWidget.setId(new WidgetTypeId(UUID.randomUUID()));

        when(widgetTypeService.findWidgetTypeDetailsByTenantIdAndFqn(TenantId.SYS_TENANT_ID, "test_widget"))
                .thenReturn(existingWidget);

        Integer updated = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes");

        assertEquals(1, updated);
        verify(widgetTypeService).saveWidgetType(argThat(w -> "New Name".equals(w.getName())));
    }

    @Test
    void whenNothingChanged_thenSkipTheUpdateOfTheExistingWidget() throws Exception {
        Path widgetTypesDir = tempDir.resolve("widget_types");
        Files.createDirectories(widgetTypesDir);
        when(installScripts.getWidgetTypesDir()).thenReturn(widgetTypesDir);

        WidgetTypeDetails fileWidget = createTestWidgetType("test_widget", "Test Widget");
        String json = JacksonUtil.toString(fileWidget);
        assertNotNull(json);
        Files.writeString(widgetTypesDir.resolve("test_widget.json"), json);

        WidgetTypeDetails existingWidget = createTestWidgetType("test_widget", "Test Widget");
        existingWidget.setId(new WidgetTypeId(UUID.randomUUID()));

        when(widgetTypeService.findWidgetTypeDetailsByTenantIdAndFqn(TenantId.SYS_TENANT_ID, "test_widget"))
                .thenReturn(existingWidget);

        Integer updated = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes");

        assertEquals(0, updated);
        verify(widgetTypeService, never()).saveWidgetType(any());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDescriptorComparisonTestCases")
    void testIfDescriptorsAreEqual(String testName, JsonNode desc1, JsonNode desc2, boolean expectedEqual) {
        Boolean result = ReflectionTestUtils.invokeMethod(reconciler, "isDescriptorEqual", desc1, desc2);
        assertEquals(expectedEqual, result, testName);
    }

    @Test
    void whenDescriptorChanged_thenReturnWidgetTypeChanged() {
        WidgetTypeDetails existing = createTestWidgetType("test", "Test");
        existing.setDescriptor(JacksonUtil.toJsonNode("{\"version\":1}"));

        WidgetTypeDetails file = createTestWidgetType("test", "Test");
        file.setDescriptor(JacksonUtil.toJsonNode("{\"version\":2}"));

        boolean result = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(reconciler, "isWidgetTypeChanged", existing, file));
        assertTrue(result);
    }

    @Test
    void whenNameChanged_thenReturnWidgetTypeChanged() {
        WidgetTypeDetails existing = createTestWidgetType("test", "Old Name");
        WidgetTypeDetails file = createTestWidgetType("test", "New Name");

        boolean result = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(reconciler, "isWidgetTypeChanged", existing, file));
        assertTrue(result);
    }

    @Test
    void whenDescriptionChanged_thenReturnWidgetTypeChanged() {
        WidgetTypeDetails existing = createTestWidgetType("test", "Test");
        existing.setDescription("Old description");

        WidgetTypeDetails file = createTestWidgetType("test", "Test");
        file.setDescription("New description");

        boolean result = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(reconciler, "isWidgetTypeChanged", existing, file));
        assertTrue(result);
    }

    @Test
    void whenWidgetTypeAreIdentical_thenNoUpdateIsPerformed() {
        WidgetTypeDetails existing = createTestWidgetType("test", "Test");
        WidgetTypeDetails file = createTestWidgetType("test", "Test");

        boolean result = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(reconciler, "isWidgetTypeChanged", existing, file));
        assertFalse(result);
    }

    @Test
    void whenLockIsHeldByOneThread_thenSecondThreadCannotAcquireLock() throws Exception {
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch startSecondThread = new CountDownLatch(1);
        CountDownLatch testComplete = new CountDownLatch(1);

        AtomicBoolean firstThreadAcquiredLock = new AtomicBoolean(false);
        AtomicBoolean secondThreadAcquiredLock = new AtomicBoolean(false);
        AtomicBoolean firstThreadSavedWidget = new AtomicBoolean(false);
        AtomicBoolean secondThreadSavedWidget = new AtomicBoolean(false);

        Path widgetTypesDir = tempDir.resolve("widget_types");
        Files.createDirectories(widgetTypesDir);
        when(installScripts.getWidgetTypesDir()).thenReturn(widgetTypesDir);

        WidgetTypeDetails fileWidget = createTestWidgetType("test_widget", "Test Widget");
        fileWidget.setDescriptor(JacksonUtil.toJsonNode("{\"type\":\"latest\",\"version\":2}"));
        String toString = JacksonUtil.toCanonicalString(fileWidget);
        assertNotNull(toString);
        Files.writeString(widgetTypesDir.resolve("test_widget.json"), toString);

        WidgetTypeDetails existingWidget = createTestWidgetType("test_widget", "Test Widget");
        existingWidget.setId(new WidgetTypeId(UUID.randomUUID()));
        existingWidget.setDescriptor(JacksonUtil.toJsonNode("{\"type\":\"latest\",\"version\":1}"));

        when(widgetTypeService.findWidgetTypeDetailsByTenantIdAndFqn(TenantId.SYS_TENANT_ID, "test_widget")).thenReturn(existingWidget);

        when(jdbcTemplate.queryForObject(contains("pg_try_advisory_lock"), eq(Boolean.class), anyLong()))
                .thenReturn(true)
                .thenReturn(false);

        when(jdbcTemplate.queryForObject(contains("pg_advisory_unlock"), eq(Boolean.class), anyLong()))
                .thenReturn(true);

        // The first thread-acquires lock and performs update
        Thread firstThread = new Thread(() -> {
            try {
                Boolean acquired = ReflectionTestUtils.invokeMethod(reconciler, "acquireAdvisoryLock");
                firstThreadAcquiredLock.set(Boolean.TRUE.equals(acquired));

                if (firstThreadAcquiredLock.get()) {
                    lockAcquired.countDown();
                    startSecondThread.await(5, TimeUnit.SECONDS);

                    // Simulate work while holding lock
                    Thread.sleep(100);

                    Integer updated = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes");
                    firstThreadSavedWidget.set(updated != null && updated > 0);

                    ReflectionTestUtils.invokeMethod(reconciler, "releaseAdvisoryLock");
                }
            } catch (Exception ignored) {
            } finally {
                testComplete.countDown();
            }
        });

        // Second thread - attempts to acquire lock but fails
        Thread secondThread = new Thread(() -> {
            try {
                lockAcquired.await(5, TimeUnit.SECONDS);
                startSecondThread.countDown();

                Boolean acquired = ReflectionTestUtils.invokeMethod(reconciler, "acquireAdvisoryLock");
                secondThreadAcquiredLock.set(Boolean.TRUE.equals(acquired));

                if (secondThreadAcquiredLock.get()) {
                    Integer updated = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes");
                    secondThreadSavedWidget.set(updated != null && updated > 0);

                    ReflectionTestUtils.invokeMethod(reconciler, "releaseAdvisoryLock");
                }
            } catch (Exception ignored) {}
        });

        firstThread.start();
        secondThread.start();

        assertTrue(testComplete.await(10, TimeUnit.SECONDS), "Test should complete within timeout");
        firstThread.join(1000);
        secondThread.join(1000);

        assertTrue(firstThreadAcquiredLock.get(), "First thread should acquire lock");
        assertFalse(secondThreadAcquiredLock.get(), "Second thread should NOT acquire lock");
        assertTrue(firstThreadSavedWidget.get(), "First thread should save widget");
        assertFalse(secondThreadSavedWidget.get(), "Second thread should NOT save widget");

        verify(widgetTypeService, times(1)).saveWidgetType(any());
    }

    private static Stream<Arguments> provideDescriptorComparisonTestCases() {
        return Stream.of(
                Arguments.of("Both null", null, null, true),
                Arguments.of("First null", null, JacksonUtil.newObjectNode(), false),
                Arguments.of("Second null", JacksonUtil.newObjectNode(), null, false),
                Arguments.of("Same content",
                        JacksonUtil.toJsonNode("{\"type\":\"latest\",\"version\":1}"),
                        JacksonUtil.toJsonNode("{\"type\":\"latest\",\"version\":1}"),
                        true),
                Arguments.of("Different content",
                        JacksonUtil.toJsonNode("{\"type\":\"latest\",\"version\":1}"),
                        JacksonUtil.toJsonNode("{\"type\":\"latest\",\"version\":2}"),
                        false),
                Arguments.of("Different key order but same content",
                        JacksonUtil.toJsonNode("{\"version\":1,\"type\":\"latest\"}"),
                        JacksonUtil.toJsonNode("{\"type\":\"latest\",\"version\":1}"),
                        true),
                Arguments.of("Empty objects",
                        JacksonUtil.toJsonNode("{}"),
                        JacksonUtil.toJsonNode("{}"),
                        true)
        );
    }

    private WidgetTypeDetails createTestWidgetType(String fqn, String name) {
        WidgetTypeDetails widget = new WidgetTypeDetails();
        widget.setFqn(fqn);
        widget.setName(name);
        widget.setDescription("Test description");
        widget.setTenantId(TenantId.SYS_TENANT_ID);
        widget.setDescriptor(JacksonUtil.toJsonNode("{\"type\":\"latest\"}"));
        return widget;
    }

}
