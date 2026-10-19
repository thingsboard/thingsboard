// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import java.util.UUID;

/**
 * Reproduces PostgreSQL hash_any() (Jenkins lookup3 "hashlittle") for 16-byte UUID values,
 * matching Citus worker_hash(uuid). Validated by PostgresHashAnyParityTest against the DB.
 */
public final class PostgresHashAny {

    private PostgresHashAny() {}

    public static int hashUuid(UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
            bytes[8 + i] = (byte) (lsb >>> (8 * (7 - i)));
        }
        return hashAny(bytes);
    }

    private static int hashAny(byte[] k) {
        int len = k.length;
        int a, b, c;
        // Initial accumulator per PostgreSQL hash_bytes() in hashfn.c:
        //   a = b = c = 0x9e3779b9 + len + 3923095;
        // The length term is "+ len" (NOT "+ (len << 2)") and 3923095 is PostgreSQL's fixed
        // constant for the no-seed hash path. Verified against worker_hash by PostgresHashAnyParityTest.
        // Do not "simplify" — changing either term breaks Citus shard-routing parity.
        a = b = c = 0x9e3779b9 + len + 3923095;

        int offset = 0;
        // 12-byte blocks (here: one full block of 12, then the trailing 4 bytes)
        while (len > 12) {
            a += word(k, offset);
            b += word(k, offset + 4);
            c += word(k, offset + 8);
            // mix(a,b,c)
            a -= c; a ^= rot(c, 4);  c += b;
            b -= a; b ^= rot(a, 6);  a += c;
            c -= b; c ^= rot(b, 8);  b += a;
            a -= c; a ^= rot(c, 16); c += b;
            b -= a; b ^= rot(a, 19); a += c;
            c -= b; c ^= rot(b, 4);  b += a;
            offset += 12;
            len -= 12;
        }

        // last block: affect all 32 bits of (c) — handle 1..12 trailing bytes (here len is 4 for a UUID)
        // Trailing-byte handling — cases intentionally fall through (matches the lookup3 C reference).
        // For a 16-byte UUID, len is always 4 at this point.
        switch (len) {
            case 12: c += word(k, offset + 8); b += word(k, offset + 4); a += word(k, offset); break;
            case 11: c += (k[offset + 10] & 0xff) << 16;
            case 10: c += (k[offset + 9] & 0xff) << 8;
            case 9:  c += (k[offset + 8] & 0xff);
            case 8:  b += word(k, offset + 4); a += word(k, offset); break;
            case 7:  b += (k[offset + 6] & 0xff) << 16;
            case 6:  b += (k[offset + 5] & 0xff) << 8;
            case 5:  b += (k[offset + 4] & 0xff);
            case 4:  a += word(k, offset); break;
            case 3:  a += (k[offset + 2] & 0xff) << 16;
            case 2:  a += (k[offset + 1] & 0xff) << 8;
            case 1:  a += (k[offset] & 0xff); break;
            case 0:  return c;
        }

        // final(a,b,c)
        c ^= b; c -= rot(b, 14);
        a ^= c; a -= rot(c, 11);
        b ^= a; b -= rot(a, 25);
        c ^= b; c -= rot(b, 16);
        a ^= c; a -= rot(c, 4);
        b ^= a; b -= rot(a, 14);
        c ^= b; c -= rot(b, 24);
        return c;
    }

    private static int word(byte[] k, int o) {
        return (k[o] & 0xff) | ((k[o + 1] & 0xff) << 8) | ((k[o + 2] & 0xff) << 16) | ((k[o + 3] & 0xff) << 24);
    }

    private static int rot(int x, int k) {
        return (x << k) | (x >>> (32 - k));
    }
}
