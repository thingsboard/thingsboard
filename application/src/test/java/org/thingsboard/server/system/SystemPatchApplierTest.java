/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.service.install.DatabaseSchemaSettingsService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.system.SystemPatchApplier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    private DatabaseSchemaSettingsService schemaSettingsService;

    @Mock
    private WidgetTypeService widgetTypeService;

    @Mock
    private WidgetsBundleService widgetsBundleService;

    @Mock
    private ImageService imageService;

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
    void whenWidgetNotFound_thenCreateNewWidget() throws Exception {
        Path widgetTypesDir = tempDir.resolve("widget_types");
        Files.createDirectories(widgetTypesDir);
        when(installScripts.getWidgetTypesDir()).thenReturn(widgetTypesDir);

        WidgetTypeDetails fileWidget = createTestWidgetType("new_widget", "New Widget");
        String json = JacksonUtil.toString(fileWidget);
        assertNotNull(json);
        Files.writeString(widgetTypesDir.resolve("new_widget.json"), json);

        when(widgetTypeService.findWidgetTypeDetailsByTenantIdAndFqn(TenantId.SYS_TENANT_ID, "new_widget")).thenReturn(null);

        SystemPatchApplier.WidgetTypeStats stats = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes");

        assertNotNull(stats);
        assertEquals(1, stats.created());
        assertEquals(0, stats.updated());
        verify(widgetTypeService).saveWidgetType(argThat(w -> "new_widget".equals(w.getFqn())));
    }

    @Test
    void whenFqnIsBlank_thenThrowException() throws Exception {
        Path widgetTypesDir = tempDir.resolve("widget_types");
        Files.createDirectories(widgetTypesDir);
        when(installScripts.getWidgetTypesDir()).thenReturn(widgetTypesDir);

        WidgetTypeDetails brokenWidget = createTestWidgetType("", "Broken Widget");
        String json = JacksonUtil.toString(brokenWidget);
        assertNotNull(json);
        Files.writeString(widgetTypesDir.resolve("broken.json"), json);

        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes"));
        verify(widgetTypeService, never()).saveWidgetType(any());
    }

    @Test
    void whenMixOfCreatedAndUpdated_thenStatsAreCorrect() throws Exception {
        Path widgetTypesDir = tempDir.resolve("widget_types");
        Files.createDirectories(widgetTypesDir);
        when(installScripts.getWidgetTypesDir()).thenReturn(widgetTypesDir);

        WidgetTypeDetails newFileWidget = createTestWidgetType("widget_new", "Widget New");
        Files.writeString(widgetTypesDir.resolve("widget_new.json"), JacksonUtil.toString(newFileWidget));

        WidgetTypeDetails changedFileWidget = createTestWidgetType("widget_changed", "Widget Changed New Name");
        Files.writeString(widgetTypesDir.resolve("widget_changed.json"), JacksonUtil.toString(changedFileWidget));

        WidgetTypeDetails sameFileWidget = createTestWidgetType("widget_same", "Widget Same");
        Files.writeString(widgetTypesDir.resolve("widget_same.json"), JacksonUtil.toString(sameFileWidget));

        WidgetTypeDetails existingChanged = createTestWidgetType("widget_changed", "Widget Changed Old Name");
        existingChanged.setId(new WidgetTypeId(UUID.randomUUID()));

        WidgetTypeDetails existingSame = createTestWidgetType("widget_same", "Widget Same");
        existingSame.setId(new WidgetTypeId(UUID.randomUUID()));

        when(widgetTypeService.findWidgetTypeDetailsByTenantIdAndFqn(TenantId.SYS_TENANT_ID, "widget_new")).thenReturn(null);
        when(widgetTypeService.findWidgetTypeDetailsByTenantIdAndFqn(TenantId.SYS_TENANT_ID, "widget_changed")).thenReturn(existingChanged);
        when(widgetTypeService.findWidgetTypeDetailsByTenantIdAndFqn(TenantId.SYS_TENANT_ID, "widget_same")).thenReturn(existingSame);

        SystemPatchApplier.WidgetTypeStats stats = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes");

        assertNotNull(stats);
        assertEquals(1, stats.created());
        assertEquals(1, stats.updated());
        verify(widgetTypeService, times(2)).saveWidgetType(any());
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

        SystemPatchApplier.WidgetTypeStats stats = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes");

        assertNotNull(stats);
        assertEquals(0, stats.created());
        assertEquals(1, stats.updated());
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

        SystemPatchApplier.WidgetTypeStats stats = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes");

        assertNotNull(stats);
        assertEquals(0, stats.created());
        assertEquals(1, stats.updated());
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

        SystemPatchApplier.WidgetTypeStats stats = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes");

        assertNotNull(stats);
        assertEquals(0, stats.created());
        assertEquals(0, stats.updated());
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

                    SystemPatchApplier.WidgetTypeStats stats = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes");
                    firstThreadSavedWidget.set(stats != null && stats.updated() > 0);

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
                    SystemPatchApplier.WidgetTypeStats stats = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetTypes");
                    secondThreadSavedWidget.set(stats != null && stats.updated() > 0);

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

    // --- isVersionIncreased tests ---

    @ParameterizedTest(name = "isVersionIncreased: {0} (package={1}, db={2}) -> {3}")
    @MethodSource("provideVersionComparisonTestCases")
    void testIsVersionIncreased(String testName, SystemPatchApplier.VersionInfo packageVersion,
                                SystemPatchApplier.VersionInfo dbVersion, boolean expected) {
        Boolean result = ReflectionTestUtils.invokeMethod(reconciler, "isVersionIncreased", packageVersion, dbVersion);
        assertEquals(expected, result, testName);
    }

    private static Stream<Arguments> provideVersionComparisonTestCases() {
        return Stream.of(
                // Maintenance digit increases within same LTS family
                Arguments.of("maintenance increased",
                        new SystemPatchApplier.VersionInfo(4, 3, 1, 0),
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 0), true),
                Arguments.of("maintenance increased by more than one",
                        new SystemPatchApplier.VersionInfo(4, 3, 3, 0),
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 0), true),

                // Patch digit increases within same maintenance
                Arguments.of("patch increased",
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 1),
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 0), true),
                Arguments.of("patch increased by more than one",
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 5),
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 2), true),

                // Both maintenance and patch increased
                Arguments.of("maintenance and patch both increased",
                        new SystemPatchApplier.VersionInfo(4, 3, 1, 1),
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 0), true),

                // Maintenance increased, patch value is lower (irrelevant — maintenance wins)
                Arguments.of("maintenance increased, patch is lower",
                        new SystemPatchApplier.VersionInfo(4, 3, 2, 0),
                        new SystemPatchApplier.VersionInfo(4, 3, 1, 5), true),

                // Same version — no increase
                Arguments.of("same version",
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 0),
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 0), false),
                Arguments.of("same version with non-zero parts",
                        new SystemPatchApplier.VersionInfo(4, 3, 1, 2),
                        new SystemPatchApplier.VersionInfo(4, 3, 1, 2), false),

                // Decreased versions — no increase
                Arguments.of("maintenance decreased",
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 0),
                        new SystemPatchApplier.VersionInfo(4, 3, 1, 0), false),
                Arguments.of("patch decreased",
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 0),
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 1), false),

                // Different major — different family, skip
                Arguments.of("different major",
                        new SystemPatchApplier.VersionInfo(5, 3, 0, 0),
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 0), false),
                Arguments.of("major decreased",
                        new SystemPatchApplier.VersionInfo(3, 3, 0, 0),
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 0), false),

                // Different minor — different LTS family, skip
                Arguments.of("minor increased (different LTS family)",
                        new SystemPatchApplier.VersionInfo(4, 4, 0, 0),
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 0), false),
                Arguments.of("minor decreased",
                        new SystemPatchApplier.VersionInfo(4, 2, 0, 0),
                        new SystemPatchApplier.VersionInfo(4, 3, 0, 0), false)
        );
    }

    // --- isVersionChanged tests ---

    @Test
    void whenVersionIncreased_thenVersionChangedReturnsTrue() {
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn("4.3.1.0");
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn("4.3.0.0");

        Boolean result = ReflectionTestUtils.invokeMethod(reconciler, "isVersionChanged");

        assertTrue(result);
    }

    @Test
    void whenVersionNotIncreased_thenVersionChangedReturnsFalse() {
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn("4.3.0.0");
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn("4.3.0.0");

        Boolean result = ReflectionTestUtils.invokeMethod(reconciler, "isVersionChanged");

        assertFalse(result);
    }

    @Test
    void whenVersionUnparseable_thenVersionChangedReturnsFalse() {
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn("invalid");
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn("4.3.0.0");

        Boolean result = ReflectionTestUtils.invokeMethod(reconciler, "isVersionChanged");

        assertFalse(result);
    }

    @Test
    void whenDbVersionUnparseable_thenVersionChangedReturnsFalse() {
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn("4.3.1.0");
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn("bad");

        Boolean result = ReflectionTestUtils.invokeMethod(reconciler, "isVersionChanged");

        assertFalse(result);
    }

    // --- updateLtsSqlSchema tests ---

    @Test
    void whenLtsSqlFileExists_thenExecutesSql() throws Exception {
        Path dataDir = tempDir.resolve("data");
        Path ltsDir = dataDir.resolve("upgrade").resolve("lts");
        Files.createDirectories(ltsDir);
        Files.writeString(ltsDir.resolve("schema_update.sql"), "ALTER TABLE device ADD COLUMN IF NOT EXISTS test_col VARCHAR(255);");
        when(installScripts.getDataDir()).thenReturn(dataDir.toString());

        ReflectionTestUtils.invokeMethod(reconciler, "updateLtsSqlSchema");

        verify(jdbcTemplate).execute("ALTER TABLE device ADD COLUMN IF NOT EXISTS test_col VARCHAR(255);");
    }

    @Test
    void whenLtsSqlFileDoesNotExist_thenSkips() {
        Path dataDir = tempDir.resolve("data");
        // Don't create the file
        when(installScripts.getDataDir()).thenReturn(dataDir.toString());

        ReflectionTestUtils.invokeMethod(reconciler, "updateLtsSqlSchema");

        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void whenLtsSqlFileHasMultipleStatements_thenExecutesAll() throws Exception {
        Path dataDir = tempDir.resolve("data");
        Path ltsDir = dataDir.resolve("upgrade").resolve("lts");
        Files.createDirectories(ltsDir);
        String sql = "DO $$ BEGIN\n" +
                "  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'test_type') THEN\n" +
                "    CREATE TYPE test_type AS ENUM ('A', 'B');\n" +
                "  END IF;\n" +
                "END $$;\n" +
                "ALTER TABLE device ADD COLUMN IF NOT EXISTS test_col VARCHAR(255);";
        Files.writeString(ltsDir.resolve("schema_update.sql"), sql);
        when(installScripts.getDataDir()).thenReturn(dataDir.toString());

        ReflectionTestUtils.invokeMethod(reconciler, "updateLtsSqlSchema");

        verify(jdbcTemplate).execute(sql);
    }

    // --- applyPatchIfNeeded flow tests ---

    @Test
    void whenVersionIncreased_thenAppliesLtsSqlBeforeViewsAndWidgets() throws Exception {
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn("4.3.1.0");
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn("4.3.0.0");
        when(jdbcTemplate.queryForObject(contains("pg_try_advisory_lock"), eq(Boolean.class), anyLong())).thenReturn(true);
        when(jdbcTemplate.queryForObject(contains("pg_advisory_unlock"), eq(Boolean.class), anyLong())).thenReturn(true);

        Path dataDir = tempDir.resolve("data");
        Path ltsDir = dataDir.resolve("upgrade").resolve("lts");
        Files.createDirectories(ltsDir);
        Files.writeString(ltsDir.resolve("schema_update.sql"), "SELECT 1;");
        when(installScripts.getDataDir()).thenReturn(dataDir.toString());

        Path widgetTypesDir = tempDir.resolve("widget_types");
        Files.createDirectories(widgetTypesDir);
        when(installScripts.getWidgetTypesDir()).thenReturn(widgetTypesDir);
        when(installScripts.getWidgetBundlesDir()).thenReturn(tempDir.resolve("widget_bundles_missing"));

        ReflectionTestUtils.invokeMethod(reconciler, "applyPatchIfNeeded");

        // LTS SQL was executed
        verify(jdbcTemplate).execute("SELECT 1;");
        // Schema version was updated
        verify(schemaSettingsService).updateSchemaVersion();
    }

    @Test
    void whenVersionNotIncreased_thenSkipsEverything() {
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn("4.3.0.0");
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn("4.3.0.0");

        ReflectionTestUtils.invokeMethod(reconciler, "applyPatchIfNeeded");

        // No lock acquired
        verify(jdbcTemplate, never()).queryForObject(contains("pg_try_advisory_lock"), eq(Boolean.class), anyLong());
        // No schema update
        verify(schemaSettingsService, never()).updateSchemaVersion();
    }

    @Test
    void whenLockNotAcquired_thenSkipsPatchApplication() {
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn("4.3.1.0");
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn("4.3.0.0");
        when(jdbcTemplate.queryForObject(contains("pg_try_advisory_lock"), eq(Boolean.class), anyLong())).thenReturn(false);

        ReflectionTestUtils.invokeMethod(reconciler, "applyPatchIfNeeded");

        verify(schemaSettingsService, never()).updateSchemaVersion();
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void whenMaintenanceVersionIncreased_thenAppliesPatch() throws Exception {
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn("4.3.2.0");
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn("4.3.1.0");
        when(jdbcTemplate.queryForObject(contains("pg_try_advisory_lock"), eq(Boolean.class), anyLong())).thenReturn(true);
        when(jdbcTemplate.queryForObject(contains("pg_advisory_unlock"), eq(Boolean.class), anyLong())).thenReturn(true);

        Path dataDir = tempDir.resolve("data");
        when(installScripts.getDataDir()).thenReturn(dataDir.toString());

        Path widgetTypesDir = tempDir.resolve("widget_types");
        Files.createDirectories(widgetTypesDir);
        when(installScripts.getWidgetTypesDir()).thenReturn(widgetTypesDir);
        when(installScripts.getWidgetBundlesDir()).thenReturn(tempDir.resolve("widget_bundles_missing"));

        ReflectionTestUtils.invokeMethod(reconciler, "applyPatchIfNeeded");

        verify(schemaSettingsService).updateSchemaVersion();
    }

    @Test
    void whenDifferentLtsFamily_thenSkipsPatch() {
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn("4.4.0.0");
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn("4.3.0.0");

        ReflectionTestUtils.invokeMethod(reconciler, "applyPatchIfNeeded");

        verify(jdbcTemplate, never()).queryForObject(contains("pg_try_advisory_lock"), eq(Boolean.class), anyLong());
        verify(schemaSettingsService, never()).updateSchemaVersion();
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

    // --- createMissingSystemImages tests ---

    @Test
    void whenImagesDirDoesNotExist_thenReturnsZeroAndDoesNotCallImageService() {
        Path dataDir = tempDir.resolve("data");
        // Intentionally do not create resources/images dir
        when(installScripts.getDataDir()).thenReturn(dataDir.toString());

        Integer created = ReflectionTestUtils.invokeMethod(reconciler, "createMissingSystemImages");

        assertEquals(0, created);
        verify(imageService, never()).getAllImageKeysByTenantId(any());
        verify(imageService, never()).createOrUpdateSystemImage(anyString(), any(byte[].class));
    }

    @Test
    void whenImagesDirIsEmpty_thenReturnsZeroAndDoesNotCallImageService() throws Exception {
        Path imagesDir = tempDir.resolve("data").resolve(InstallScripts.RESOURCES_DIR).resolve("images");
        Files.createDirectories(imagesDir);
        when(installScripts.getDataDir()).thenReturn(tempDir.resolve("data").toString());
        when(imageService.getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID)).thenReturn(Collections.emptySet());

        Integer created = ReflectionTestUtils.invokeMethod(reconciler, "createMissingSystemImages");

        assertEquals(0, created);
        verify(imageService, never()).createOrUpdateSystemImage(anyString(), any(byte[].class));
    }

    @Test
    void whenSystemImageDoesNotExistInDb_thenCreateIt() throws Exception {
        Path imagesDir = tempDir.resolve("data").resolve(InstallScripts.RESOURCES_DIR).resolve("images");
        Files.createDirectories(imagesDir);
        when(installScripts.getDataDir()).thenReturn(tempDir.resolve("data").toString());

        byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};
        Files.write(imagesDir.resolve("gateway.png"), imageBytes);

        when(imageService.getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID)).thenReturn(Collections.emptySet());

        Integer created = ReflectionTestUtils.invokeMethod(reconciler, "createMissingSystemImages");

        assertEquals(1, created);
        verify(imageService).getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID);
        verify(imageService).createOrUpdateSystemImage(eq("gateway.png"), eq(imageBytes));
    }

    @Test
    void whenSystemImageExistsInDb_thenSkipIt() throws Exception {
        Path imagesDir = tempDir.resolve("data").resolve(InstallScripts.RESOURCES_DIR).resolve("images");
        Files.createDirectories(imagesDir);
        when(installScripts.getDataDir()).thenReturn(tempDir.resolve("data").toString());

        Files.write(imagesDir.resolve("gateway.png"), new byte[]{1, 2, 3});

        when(imageService.getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID)).thenReturn(Set.of("gateway.png"));

        Integer created = ReflectionTestUtils.invokeMethod(reconciler, "createMissingSystemImages");

        assertEquals(0, created);
        verify(imageService).getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID);
        verify(imageService, never()).createOrUpdateSystemImage(anyString(), any(byte[].class));
    }

    @Test
    void whenMixOfNewAndExistingImages_thenOnlyCreateMissingOnes() throws Exception {
        Path imagesDir = tempDir.resolve("data").resolve(InstallScripts.RESOURCES_DIR).resolve("images");
        Files.createDirectories(imagesDir);
        when(installScripts.getDataDir()).thenReturn(tempDir.resolve("data").toString());

        byte[] newImageBytes = new byte[]{9, 9, 9};
        byte[] existingImageBytes = new byte[]{1, 1, 1};
        Files.write(imagesDir.resolve("new.png"), newImageBytes);
        Files.write(imagesDir.resolve("existing.svg"), existingImageBytes);

        when(imageService.getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID)).thenReturn(Set.of("existing.svg"));

        Integer created = ReflectionTestUtils.invokeMethod(reconciler, "createMissingSystemImages");

        assertEquals(1, created);
        verify(imageService, times(1)).getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID);
        verify(imageService).createOrUpdateSystemImage(eq("new.png"), eq(newImageBytes));
        verify(imageService, never()).createOrUpdateSystemImage(eq("existing.svg"), any(byte[].class));
    }

    @Test
    void whenImagesDirContainsSubdirectory_thenSubdirectoryIsIgnored() throws Exception {
        Path imagesDir = tempDir.resolve("data").resolve(InstallScripts.RESOURCES_DIR).resolve("images");
        Files.createDirectories(imagesDir);
        Files.createDirectories(imagesDir.resolve("nested"));
        when(installScripts.getDataDir()).thenReturn(tempDir.resolve("data").toString());

        byte[] imageBytes = new byte[]{5, 6, 7};
        Files.write(imagesDir.resolve("logo.png"), imageBytes);

        when(imageService.getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID)).thenReturn(Collections.emptySet());

        Integer created = ReflectionTestUtils.invokeMethod(reconciler, "createMissingSystemImages");

        assertEquals(1, created);
        verify(imageService).createOrUpdateSystemImage(eq("logo.png"), eq(imageBytes));
        verify(imageService, never()).createOrUpdateSystemImage(eq("nested"), any(byte[].class));
    }

    @Test
    void whenMultipleNewImages_thenCreatesAll() throws Exception {
        Path imagesDir = tempDir.resolve("data").resolve(InstallScripts.RESOURCES_DIR).resolve("images");
        Files.createDirectories(imagesDir);
        when(installScripts.getDataDir()).thenReturn(tempDir.resolve("data").toString());

        Files.write(imagesDir.resolve("a.png"), new byte[]{1});
        Files.write(imagesDir.resolve("b.svg"), new byte[]{2});
        Files.write(imagesDir.resolve("c.jpg"), new byte[]{3});

        when(imageService.getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID)).thenReturn(Collections.emptySet());

        Integer created = ReflectionTestUtils.invokeMethod(reconciler, "createMissingSystemImages");

        assertEquals(3, created);
        verify(imageService, times(1)).getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID);
        verify(imageService).createOrUpdateSystemImage(eq("a.png"), any(byte[].class));
        verify(imageService).createOrUpdateSystemImage(eq("b.svg"), any(byte[].class));
        verify(imageService).createOrUpdateSystemImage(eq("c.jpg"), any(byte[].class));
    }

    @Test
    void whenImageServiceThrows_thenWrapsAndPropagates() throws Exception {
        Path imagesDir = tempDir.resolve("data").resolve(InstallScripts.RESOURCES_DIR).resolve("images");
        Files.createDirectories(imagesDir);
        when(installScripts.getDataDir()).thenReturn(tempDir.resolve("data").toString());

        Files.write(imagesDir.resolve("broken.png"), new byte[]{1, 2});

        when(imageService.getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID)).thenReturn(Collections.emptySet());
        when(imageService.createOrUpdateSystemImage(eq("broken.png"), any(byte[].class)))
                .thenThrow(new RuntimeException("DB error"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(reconciler, "createMissingSystemImages"));
        assertTrue(thrown.getMessage().contains("broken.png"));
    }

    @Test
    void whenExistingKeysLookupFails_thenDoesNotCreateImage() throws Exception {
        Path imagesDir = tempDir.resolve("data").resolve(InstallScripts.RESOURCES_DIR).resolve("images");
        Files.createDirectories(imagesDir);
        when(installScripts.getDataDir()).thenReturn(tempDir.resolve("data").toString());

        Files.write(imagesDir.resolve("img.png"), new byte[]{1});

        when(imageService.getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID))
                .thenThrow(new RuntimeException("lookup failed"));

        assertThrows(RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(reconciler, "createMissingSystemImages"));
        verify(imageService, never()).createOrUpdateSystemImage(anyString(), any(byte[].class));
    }

    // --- applyPatchIfNeeded integration with createMissingSystemImages ---

    @Test
    void whenApplyPatchIfNeededRuns_thenCreatesMissingImagesAfterWidgets() throws Exception {
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn("4.3.1.0");
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn("4.3.0.0");
        when(jdbcTemplate.queryForObject(contains("pg_try_advisory_lock"), eq(Boolean.class), anyLong())).thenReturn(true);
        when(jdbcTemplate.queryForObject(contains("pg_advisory_unlock"), eq(Boolean.class), anyLong())).thenReturn(true);

        Path dataDir = tempDir.resolve("data");
        Path imagesDir = dataDir.resolve(InstallScripts.RESOURCES_DIR).resolve("images");
        Files.createDirectories(imagesDir);
        byte[] imgBytes = new byte[]{7, 7, 7};
        Files.write(imagesDir.resolve("new_icon.svg"), imgBytes);
        when(installScripts.getDataDir()).thenReturn(dataDir.toString());

        Path widgetTypesDir = tempDir.resolve("widget_types");
        Files.createDirectories(widgetTypesDir);
        when(installScripts.getWidgetTypesDir()).thenReturn(widgetTypesDir);
        when(installScripts.getWidgetBundlesDir()).thenReturn(tempDir.resolve("widget_bundles_missing"));

        when(imageService.getAllImageKeysByTenantId(TenantId.SYS_TENANT_ID)).thenReturn(Collections.emptySet());

        ReflectionTestUtils.invokeMethod(reconciler, "applyPatchIfNeeded");

        verify(imageService).createOrUpdateSystemImage(eq("new_icon.svg"), eq(imgBytes));
        verify(schemaSettingsService).updateSchemaVersion();
    }

    @Test
    void whenVersionNotIncreased_thenImagesAreNotTouched() {
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn("4.3.0.0");
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn("4.3.0.0");

        ReflectionTestUtils.invokeMethod(reconciler, "applyPatchIfNeeded");

        verify(imageService, never()).getAllImageKeysByTenantId(any());
        verify(imageService, never()).createOrUpdateSystemImage(anyString(), any(byte[].class));
    }

    // --- updateWidgetBundles tests ---

    @Test
    void whenWidgetBundlesDirDoesNotExist_thenReturnsZero() {
        when(installScripts.getWidgetBundlesDir()).thenReturn(tempDir.resolve("missing_bundles"));

        Integer updated = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetBundles");

        assertEquals(0, updated);
        verify(widgetsBundleService, never()).saveWidgetsBundle(any());
        verify(widgetTypeService, never()).updateWidgetsBundleWidgetFqns(any(), any(), any());
    }

    @Test
    void whenBundleNotInDb_thenSkipWithoutCreation() throws Exception {
        Path bundlesDir = tempDir.resolve("widget_bundles");
        Files.createDirectories(bundlesDir);
        when(installScripts.getWidgetBundlesDir()).thenReturn(bundlesDir);

        Files.writeString(bundlesDir.resolve("charts.json"),
                "{\"widgetsBundle\":{\"alias\":\"charts\",\"title\":\"Charts\",\"order\":10}," +
                        "\"widgetTypeFqns\":[\"line_chart\"]}");

        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "charts")).thenReturn(null);

        Integer updated = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetBundles");

        assertEquals(0, updated);
        verify(widgetsBundleService, never()).saveWidgetsBundle(any());
        verify(widgetTypeService, never()).updateWidgetsBundleWidgetFqns(any(), any(), any());
    }

    @Test
    void whenBundleExistsAndHasNewFqn_thenMergeFqns() throws Exception {
        Path bundlesDir = tempDir.resolve("widget_bundles");
        Files.createDirectories(bundlesDir);
        when(installScripts.getWidgetBundlesDir()).thenReturn(bundlesDir);

        Files.writeString(bundlesDir.resolve("charts.json"),
                "{\"widgetsBundle\":{\"alias\":\"charts\",\"title\":\"Charts\",\"description\":\"d\",\"order\":10}," +
                        "\"widgetTypeFqns\":[\"line_chart\",\"bar_chart\",\"new_chart\"]}");

        WidgetsBundle existingBundle = createTestBundle("charts", "Charts");
        existingBundle.setDescription("d");
        existingBundle.setOrder(10);
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "charts")).thenReturn(existingBundle);
        when(widgetTypeService.findWidgetFqnsByWidgetsBundleId(TenantId.SYS_TENANT_ID, existingBundle.getId()))
                .thenReturn(List.of("line_chart", "bar_chart"));

        Integer updated = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetBundles");

        assertEquals(1, updated);
        verify(widgetsBundleService, never()).saveWidgetsBundle(any());
        verify(widgetTypeService).updateWidgetsBundleWidgetFqns(
                eq(TenantId.SYS_TENANT_ID),
                eq(existingBundle.getId()),
                argThat(fqns -> fqns.size() == 3
                        && fqns.get(0).equals("line_chart")
                        && fqns.get(1).equals("bar_chart")
                        && fqns.get(2).equals("new_chart"))
        );
    }

    @Test
    void whenBundleExistsAndAllFqnsAlreadyLinked_thenNoLinkUpdate() throws Exception {
        Path bundlesDir = tempDir.resolve("widget_bundles");
        Files.createDirectories(bundlesDir);
        when(installScripts.getWidgetBundlesDir()).thenReturn(bundlesDir);

        Files.writeString(bundlesDir.resolve("charts.json"),
                "{\"widgetsBundle\":{\"alias\":\"charts\",\"title\":\"Charts\",\"description\":\"d\",\"order\":10}," +
                        "\"widgetTypeFqns\":[\"line_chart\",\"bar_chart\"]}");

        WidgetsBundle existingBundle = createTestBundle("charts", "Charts");
        existingBundle.setDescription("d");
        existingBundle.setOrder(10);
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "charts")).thenReturn(existingBundle);
        when(widgetTypeService.findWidgetFqnsByWidgetsBundleId(TenantId.SYS_TENANT_ID, existingBundle.getId()))
                .thenReturn(List.of("line_chart", "bar_chart"));

        Integer updated = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetBundles");

        assertEquals(0, updated);
        verify(widgetsBundleService, never()).saveWidgetsBundle(any());
        verify(widgetTypeService, never()).updateWidgetsBundleWidgetFqns(any(), any(), any());
    }

    @Test
    void whenOnlyBundleImageFormatDiffers_thenNoUpdate() throws Exception {
        Path bundlesDir = tempDir.resolve("widget_bundles");
        Files.createDirectories(bundlesDir);
        when(installScripts.getWidgetBundlesDir()).thenReturn(bundlesDir);

        // File carries a base64 data URI; DB has the resolved system-image URL — same content, different format.
        Files.writeString(bundlesDir.resolve("charts.json"),
                "{\"widgetsBundle\":{\"alias\":\"charts\",\"title\":\"Charts\",\"description\":\"d\",\"order\":10," +
                        "\"image\":\"data:image/png;base64,iVBORw0KGgo\"}," +
                        "\"widgetTypeFqns\":[]}");

        WidgetsBundle existingBundle = createTestBundle("charts", "Charts");
        existingBundle.setDescription("d");
        existingBundle.setOrder(10);
        existingBundle.setImage("tb-image;/api/images/system/charts.png");
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "charts")).thenReturn(existingBundle);
        when(widgetTypeService.findWidgetFqnsByWidgetsBundleId(TenantId.SYS_TENANT_ID, existingBundle.getId()))
                .thenReturn(List.of());

        Integer updated = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetBundles");

        assertEquals(0, updated);
        verify(widgetsBundleService, never()).saveWidgetsBundle(any());
        verify(widgetTypeService, never()).updateWidgetsBundleWidgetFqns(any(), any(), any());
    }

    @Test
    void whenBundleMetadataChanged_thenUpdateBundle() throws Exception {
        Path bundlesDir = tempDir.resolve("widget_bundles");
        Files.createDirectories(bundlesDir);
        when(installScripts.getWidgetBundlesDir()).thenReturn(bundlesDir);

        Files.writeString(bundlesDir.resolve("charts.json"),
                "{\"widgetsBundle\":{\"alias\":\"charts\",\"title\":\"New Title\",\"description\":\"new\",\"order\":20}," +
                        "\"widgetTypeFqns\":[\"line_chart\"]}");

        WidgetsBundle existingBundle = createTestBundle("charts", "Old Title");
        existingBundle.setDescription("old");
        existingBundle.setOrder(10);
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "charts")).thenReturn(existingBundle);
        when(widgetTypeService.findWidgetFqnsByWidgetsBundleId(TenantId.SYS_TENANT_ID, existingBundle.getId()))
                .thenReturn(List.of("line_chart"));

        Integer updated = ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetBundles");

        assertEquals(1, updated);
        verify(widgetsBundleService).saveWidgetsBundle(argThat(b ->
                "New Title".equals(b.getTitle()) && "new".equals(b.getDescription()) && b.getOrder() == 20
        ));
        verify(widgetTypeService, never()).updateWidgetsBundleWidgetFqns(any(), any(), any());
    }

    @Test
    void whenBundleAliasIsBlank_thenThrowException() throws Exception {
        Path bundlesDir = tempDir.resolve("widget_bundles");
        Files.createDirectories(bundlesDir);
        when(installScripts.getWidgetBundlesDir()).thenReturn(bundlesDir);

        Files.writeString(bundlesDir.resolve("broken.json"),
                "{\"widgetsBundle\":{\"alias\":\"\",\"title\":\"Broken\"}}");

        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetBundles"));
        verify(widgetsBundleService, never()).saveWidgetsBundle(any());
    }

    @Test
    void whenBundleJsonMissingWidgetsBundleField_thenThrowException() throws Exception {
        Path bundlesDir = tempDir.resolve("widget_bundles");
        Files.createDirectories(bundlesDir);
        when(installScripts.getWidgetBundlesDir()).thenReturn(bundlesDir);

        Files.writeString(bundlesDir.resolve("broken.json"), "{\"foo\":\"bar\"}");

        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetBundles"));
        verify(widgetsBundleService, never()).saveWidgetsBundle(any());
    }

    @Test
    void whenBundleHasInlineWidgetTypes_thenThrowException() throws Exception {
        Path bundlesDir = tempDir.resolve("widget_bundles");
        Files.createDirectories(bundlesDir);
        when(installScripts.getWidgetBundlesDir()).thenReturn(bundlesDir);

        Files.writeString(bundlesDir.resolve("charts.json"),
                "{\"widgetsBundle\":{\"alias\":\"charts\",\"title\":\"Charts\",\"description\":\"d\",\"order\":10}," +
                        "\"widgetTypes\":[" +
                        "{\"fqn\":\"inline_chart\",\"name\":\"Inline\",\"descriptor\":{\"type\":\"latest\"}}" +
                        "]}");

        WidgetsBundle existingBundle = createTestBundle("charts", "Charts");
        existingBundle.setDescription("d");
        existingBundle.setOrder(10);
        when(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, "charts")).thenReturn(existingBundle);

        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(reconciler, "updateWidgetBundles"));
        verify(widgetTypeService, never()).saveWidgetType(any());
        verify(widgetTypeService, never()).updateWidgetsBundleWidgetFqns(any(), any(), any());
        verify(widgetsBundleService, never()).saveWidgetsBundle(any());
    }

    private WidgetsBundle createTestBundle(String alias, String title) {
        WidgetsBundle bundle = new WidgetsBundle();
        bundle.setId(new WidgetsBundleId(UUID.randomUUID()));
        bundle.setAlias(alias);
        bundle.setTitle(title);
        bundle.setTenantId(TenantId.SYS_TENANT_ID);
        return bundle;
    }

}
