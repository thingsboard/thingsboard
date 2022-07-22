package org.thingsboard.server.cache.ota.service;

import lombok.SneakyThrows;
import org.hibernate.engine.jdbc.BlobProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.io.*;
import java.security.MessageDigest;
import java.sql.Blob;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class BaseFileCacheServiceTest {
    private final static String PATH = "/home/anastasiia/IdeaProjects/thingsboard/common/cache";
    private final static String FILE_FILLING = "Hello, testing environment";
    private static final int ONE_MEGA_BYTE = 1_000_000;
    private final static OtaPackageId OTA_PACKAGE_ID = new OtaPackageId(UUID.randomUUID());
    private final static Blob DATA = BlobProxy.generateProxy(FILE_FILLING.getBytes());
    File file;
    BaseFileCacheService baseFileCacheService = new BaseFileCacheService(new TemporaryFileCleaner());

    @BeforeEach
    void setUp() {
        file = Mockito.mock(File.class);
//        fileCacheService = Mockito.mock(FileCacheService.class);
    }

    @Test
    void testNotThrowExceptionsWhenDeleteFile() {
        Mockito.when(file.exists()).thenReturn(true);
        assertDoesNotThrow(() -> baseFileCacheService.deleteFile(new File("/download/alarm_rule.js")));
    }

    @Test
    void testDataSavingWithNullBlob() {
        assertThrows(NullPointerException.class, () -> baseFileCacheService.loadToFile(OTA_PACKAGE_ID, null));
    }

    @Test
    void testDataSavingWithNullOtaPackageId() {
        assertThrows(NullPointerException.class, () -> baseFileCacheService.loadToFile(null, DATA));
    }

    @Test
    @SneakyThrows
    void testMultiSavingDataToFile() {
        File directory = new File(PATH);
        int beginning = Objects.requireNonNull(directory.list()).length;
        Thread thread1 = new Thread(() -> baseFileCacheService.loadToFile(OTA_PACKAGE_ID, DATA));
        Thread thread2 = new Thread(() -> baseFileCacheService.loadToFile(OTA_PACKAGE_ID, DATA));
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        File directory1 = new File(PATH);
        int ending = Objects.requireNonNull(directory1.list()).length;
        assertEquals(1, ending - beginning);
    }

    @Test
    @SneakyThrows
    void testCorrectDataSavingToFile() {
        String sha256 = calculateChecksumSHA256(new ByteArrayInputStream(FILE_FILLING.getBytes()));
        File file = baseFileCacheService.loadToFile(OTA_PACKAGE_ID, DATA);
        assertEquals(sha256, calculateChecksumSHA256(new FileInputStream(file)));
    }

    @Test
    @SneakyThrows
    void testNotThrowExceptionWhenCreatingTemporaryFiles() {
        BaseFileCacheService mock = Mockito.mock(BaseFileCacheService.class);
        assertDoesNotThrow(() -> mock.saveDataTemporaryFile(DATA.getBinaryStream()));
    }

    @Test
    @SneakyThrows
    void testGettingStreamWhenFileNotExist(){
       assertThrows(FileNotFoundException.class, ()->baseFileCacheService.getOtaDataStream(OTA_PACKAGE_ID));
    }

    @SneakyThrows
    String calculateChecksumSHA256(InputStream stream) {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[ONE_MEGA_BYTE];
        int count = 0;
        while ((count = stream.read(buffer)) != -1) {
            md.update(buffer, 0, count);
        }
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}