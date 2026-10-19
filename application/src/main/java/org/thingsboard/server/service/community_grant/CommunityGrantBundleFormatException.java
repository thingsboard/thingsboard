// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import lombok.Getter;

/**
 * Why {@link CommunityGrantOfflineBundle#parse} refused a {@code TBICBDL1} file. The message is shown to the
 * uploader and stored in an audit row, so it must never carry secrets or the uploaded bytes.
 */
@Getter
public class CommunityGrantBundleFormatException extends IllegalArgumentException {

    /** In the order the checks are made. */
    public enum Reason {
        TOO_SHORT,
        BAD_MAGIC,
        UNSUPPORTED_VERSION,
        MANIFEST_LEN_RANGE,
        CHECKER_LEN_RANGE,
        LENGTH_MISMATCH,
        MANIFEST_NOT_UTF8,
        MANIFEST_NOT_JSON,
        MANIFEST_FIELD
    }

    private final Reason reason;

    public CommunityGrantBundleFormatException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

}
