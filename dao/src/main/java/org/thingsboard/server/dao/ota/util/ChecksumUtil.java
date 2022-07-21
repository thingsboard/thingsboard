package org.thingsboard.server.dao.ota.util;

import com.datastax.oss.driver.shaded.guava.common.io.ByteStreams;
import com.google.common.hash.Funnels;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.zip.CRC32;

@Slf4j
public class ChecksumUtil {
    private static final int ONE_MEGA_BYTE = 1_000_000;

    public static String generateChecksum(ChecksumAlgorithm checksumAlgorithm, InputStream fileData) {
        try {
            switch (checksumAlgorithm) {
                case CRC32:
                    return checksumCRC32(fileData);
                case MURMUR3_128:
                    return checksumMurmur3_128(fileData);
                case MURMUR3_32:
                    return checksumMurmur3_32(fileData);
                default:
                    MessageDigest md = MessageDigest.getInstance(checksumAlgorithm.getName());
                    return checksum(fileData, md);
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("No such checksum algorithm {}", checksumAlgorithm, e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("Failed to calculate checksum", e);
            throw new RuntimeException(e);
        }
    }

    private static String checksum(InputStream inputStream, MessageDigest md) throws IOException {
        byte[] buffer = new byte[ONE_MEGA_BYTE];
        int count = 0;
        while ((count = inputStream.read(buffer)) != -1) {
            md.update(buffer, 0, count);
        }
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static String checksumCRC32(InputStream inputStream) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[ONE_MEGA_BYTE];
        int count = 0;
        while ((count = inputStream.read(buffer)) != -1) {
            crc.update(buffer, 0, count);
        }
        return Long.toHexString(crc.getValue());
    }

    private static String checksumMurmur3_32(InputStream stream) throws IOException {
        Hasher hasher = Hashing.murmur3_32().newHasher();
        ByteStreams.copy(stream, Funnels.asOutputStream(hasher));
        String hash = new String(hasher.hash().asBytes());
        return new String(Base64.getEncoder().encode(hash.getBytes()));
    }

    private static String checksumMurmur3_128(InputStream stream) throws IOException {
        Hasher hasher = Hashing.murmur3_128().newHasher();
        ByteStreams.copy(stream, Funnels.asOutputStream(hasher));
        String hash = new String(hasher.hash().asBytes());
        return new String(Base64.getEncoder().encode(hash.getBytes()));
    }
}
