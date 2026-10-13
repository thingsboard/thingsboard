// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import com.lowagie.text.Image;
import org.xhtmlrenderer.extend.Size;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageUtils {

    public static final String EMPTY_IMAGE_URI = "tb-empty-image";

    static {
        Logger.getLogger("com.github.weisj.jsvg").setLevel(Level.OFF);
    }

    public static Size getOriginalImageSize(byte[] pngImage, int dotsPerPixel) throws IOException {
        Image img = Image.getInstance(pngImage);
        return new Size((int) img.getPlainWidth() * dotsPerPixel, (int) img.getPlainHeight() * dotsPerPixel);
    }

    public static boolean isTbImage(String uri) {
        return isInternalTbImage(uri) || isPublicTbImage(uri);
    }

    public static boolean isInternalTbImage(String uri) {
        return uri.startsWith("/api/images/tenant/") ||
               uri.startsWith("/api/images/system/");
    }

    public static boolean isPublicTbImage(String uri) {
        return uri.startsWith("/api/images/public/");
    }

    public static boolean isEmptyImage(String uri) {
        return EMPTY_IMAGE_URI.equals(uri);
    }
}
