// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.thingsboard.server.common.data.Version;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * The release date every licence client is built with. The client compares it against the validity window
 * encoded in the licence, so the format and the time zone are part of that contract and must be identical
 * everywhere a client is built.
 */
public class LicenseReleaseDate {

    private static final String RELEASE_DATE_FORMAT = "yyyy-MM-dd";

    private LicenseReleaseDate() {
    }

    public static long resolveReleaseDate() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(RELEASE_DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.parse(Version.PROJECT_BUILD_DATE).getTime();
    }

}
