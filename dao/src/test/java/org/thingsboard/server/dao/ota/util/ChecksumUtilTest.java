package org.thingsboard.server.dao.ota.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.thingsboard.server.common.data.ota.ChecksumAlgorithm.*;

class ChecksumUtilTest {
    private static final int SIZE = 1_050_000;
    ByteArrayInputStream inputStream;

    @BeforeEach
    void setUp() {
        char[] chars = new char[SIZE];
        Arrays.fill(chars, 'f');
        String s = new String(chars);
        inputStream = new ByteArrayInputStream(s.getBytes());
    }

    @Test
    void testSha256Checksum() {
        String generateChecksum = ChecksumUtil.generateChecksum(SHA256, inputStream);
        assertEquals("b23c6c2faa06fe3f9a47b86914d08a77c9db9ce9b87fc8b6b2758db756159b2e", generateChecksum);
    }

    @Test
    void testMd5Checksum() {
        String generateChecksum = ChecksumUtil.generateChecksum(MD5, inputStream);
        assertEquals("352b53342cc1ef2a21c480656ef6ffe6", generateChecksum);
    }

    @Test
    void testSha384Checksum() {
        String generateChecksum = ChecksumUtil.generateChecksum(SHA384, inputStream);
        assertEquals("bd04277a8fc6ace52123d6d6214d2612ccdec802f361974198c44fd3df857b79f2343f010943340aeb7c51321a2d32e9", generateChecksum);
    }

    @Test
    void testSha512Checksum() {
        String generateChecksum = ChecksumUtil.generateChecksum(SHA512, inputStream);
        assertEquals("2efb6e7ef97eca294e8cc4bf731615622199fba59c2b5ed4f9a56c1f17be3522abac4f5d9fa0f95f39ff10a59c28597a263697b2d794e6686d260ffaa078da7e",
                generateChecksum);
    }

    @Test
    void testCrc32Checksum() {
        String generateChecksum = ChecksumUtil.generateChecksum(CRC32, inputStream);
        assertEquals("4b8f2fda", generateChecksum);
    }

    @Test
    void testMurmur3_32Checksum() {
        assertEquals("3d45a1dd", ChecksumUtil.generateChecksum(MURMUR3_32, inputStream));
        assertDoesNotThrow(() -> ChecksumUtil.generateChecksum(MURMUR3_32, inputStream));
    }

    @Test
    void testMurmur3_128Checksum() {
        assertEquals("9dbf0ffe5f6ecbb5edf207717e7870b7", ChecksumUtil.generateChecksum(MURMUR3_128, inputStream));
        assertDoesNotThrow(() -> ChecksumUtil.generateChecksum(MURMUR3_128, inputStream));
    }

}