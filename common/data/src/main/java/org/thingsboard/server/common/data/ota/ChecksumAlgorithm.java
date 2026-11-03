// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ota;

public enum ChecksumAlgorithm {
    MD5,
    SHA256,
    SHA384,
    SHA512,
    CRC32,
    MURMUR3_32,
    MURMUR3_128
}
