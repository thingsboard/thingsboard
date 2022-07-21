package org.thingsboard.server.dao.ota.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.thingsboard.server.common.data.ota.ChecksumAlgorithm.*;

import java.io.ByteArrayInputStream;

class ChecksumUtilTest {
    ByteArrayInputStream inputStream;
    @BeforeEach
    void setUp() {
      inputStream =
                new ByteArrayInputStream("ggvgavsckvawyefgkashcbweyfguas;cnewgfayegfdhvzhxeyrgfzkdfgybycefygkzbvdrgzksdfbfvxjchvksydfvdkzsf".getBytes());
    }

    @Test
    void testSha256Checksum() {
        String generateChecksum = ChecksumUtil.generateChecksum(SHA256, inputStream);
        assertEquals("8098be8d5e22198e69316b1eef0b2863d75973b708f2ec4849132122b45fea71", generateChecksum);
    }

    @Test
    void testMd5Checksum() {
        String generateChecksum = ChecksumUtil.generateChecksum(MD5, inputStream);
        assertEquals("64c03752176a3a66e627db31ad4d3a9d", generateChecksum);
    }

    @Test
    void testSha384Checksum() {
        String generateChecksum = ChecksumUtil.generateChecksum(SHA384, inputStream);
        assertEquals("f7a88d89699d5fb9d4fb3b84b606bb1cac6641ed04e51f598fb7729e2142d5b0cec0c4019801d239749ff6ea0a872604", generateChecksum);
    }
    @Test
    void testSha512Checksum() {
        String generateChecksum = ChecksumUtil.generateChecksum(SHA512, inputStream);
        assertEquals("5709738c21dfc8ea5b80a291a0e11c4c38bc8393d01b075e712877bf4b6329f822ab1a6d8eecd0d578e7ae9a51fff852f3c9849d23f5109a8068f92a858a88f0",
                generateChecksum);
    }

    @Test
    void testCrc32Checksum() {
        String generateChecksum = ChecksumUtil.generateChecksum(CRC32, inputStream);
        assertEquals("666124cf", generateChecksum);
    }

    @Test
    void testMurmur3_32Checksum(){
        assertDoesNotThrow(()->ChecksumUtil.generateChecksum(MURMUR3_32, inputStream));
    }

    @Test
    void testMurmur3_128Checksum(){
        assertDoesNotThrow(()->ChecksumUtil.generateChecksum(MURMUR3_128, inputStream));
    }

}