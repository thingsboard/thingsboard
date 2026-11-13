// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.DeleteCustomTranslationArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteCustomTranslationKeyArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomTranslationArgs;
import org.thingsboard.client.api.ThingsboardApi.GetMergedCustomTranslationArgs;
import org.thingsboard.client.api.ThingsboardApi.PatchCustomTranslationArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveCustomTranslationArgs;
import org.thingsboard.client.api.ThingsboardApi.UploadCustomTranslationArgs;
import org.thingsboard.client.model.TranslationInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class CustomTranslationApiClientTest extends AbstractApiClientTest {

    private static final String TEST_LOCALE = "en_US";

    @Test
    public void testUploadAndDeleteCustomTranslation() throws Exception {
        File translationFile = createTranslationFile("{\"testGreeting\":\"Hello\"}");

        client.uploadCustomTranslation(UploadCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                ._file(translationFile)
                .build());

        TranslationInfo localeInfo = findLocaleInfo(TEST_LOCALE);
        assertTrue("Locale should be customized after upload",
                localeInfo.getCustomized());

        client.deleteCustomTranslation(DeleteCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                .build());

        assertFalse("Locale should no longer be customized after delete",
                findLocaleInfo(TEST_LOCALE).getCustomized());
    }

    @Test
    public void testDeleteCustomTranslationKey() throws Exception {
        File translationFile = createTranslationFile(
                "{\"testGreeting\":\"Hello\",\"testFarewell\":\"Goodbye\"}");
        client.uploadCustomTranslation(UploadCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                ._file(translationFile)
                .build());

        assertTrue(findLocaleInfo(TEST_LOCALE).getCustomized());

        client.deleteCustomTranslationKey(DeleteCustomTranslationKeyArgs.builder()
                .localeCode(TEST_LOCALE)
                .keyPath("testGreeting")
                .build());

        assertTrue("Locale should still be customized after deleting just one of two keys",
                findLocaleInfo(TEST_LOCALE).getCustomized());

        client.deleteCustomTranslation(DeleteCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                .build());
    }

    @Test
    public void testSaveAndGetCustomTranslation() throws Exception {
        Map<String, Object> translations = Map.of("testGreeting", "Hello", "testFarewell", "Goodbye");

        client.saveCustomTranslation(SaveCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                .body(translations)
                .build());

        JsonNode fetched = client.getCustomTranslation(GetCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                .build());
        assertNotNull(fetched);
        assertEquals("Hello", fetched.get("testGreeting").asText());
        assertEquals("Goodbye", fetched.get("testFarewell").asText());

        client.deleteCustomTranslation(DeleteCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                .build());
    }

    @Test
    public void testPatchCustomTranslation() throws Exception {
        client.saveCustomTranslation(SaveCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                .body(Map.of("testGreeting", "Hello", "testFarewell", "Goodbye"))
                .build());

        client.patchCustomTranslation(PatchCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                .body(Map.of("testGreeting", "Hi"))
                .build());

        JsonNode patched = client.getCustomTranslation(GetCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                .build());
        assertNotNull(patched);
        assertEquals("Patched key should be updated", "Hi", patched.get("testGreeting").asText());
        assertEquals("Unpatched key should remain unchanged", "Goodbye", patched.get("testFarewell").asText());

        client.deleteCustomTranslation(DeleteCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                .build());
    }

    @Test
    public void testGetMergedCustomTranslation() throws Exception {
        client.saveCustomTranslation(SaveCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                .body(Map.of("testGreeting", "Hello"))
                .build());

        JsonNode merged = client.getMergedCustomTranslation(GetMergedCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                .build());
        assertNotNull(merged);
        assertFalse("Merged translation should contain at least the built-in keys", merged.isEmpty());
        assertEquals("Custom key should appear in merged result", "Hello", merged.get("testGreeting").asText());

        client.deleteCustomTranslation(DeleteCustomTranslationArgs.builder()
                .localeCode(TEST_LOCALE)
                .build());
    }

    private File createTranslationFile(String jsonContent) throws Exception {
        Path tempFile = Files.createTempFile("translation_", ".json");
        Files.writeString(tempFile, jsonContent);
        tempFile.toFile().deleteOnExit();
        return tempFile.toFile();
    }

    private TranslationInfo findLocaleInfo(String localeCode) throws ApiException {
        List<TranslationInfo> infos = client.getTranslationInfos();
        assertNotNull(infos);
        return infos.stream()
                .filter(t -> localeCode.equals(t.getLocaleCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Locale '" + localeCode + "' not found in translation infos"));
    }

}
