/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Slf4j
@UtilityClass
public class ImageUtils {

    public static String getEmbeddedBase64EncodedImg(String colorStr) {
        try {
            Color color = parseColor(colorStr); // Support for hex, rgb, hsla
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, color.getRGB());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String base64String = Base64.getEncoder().encodeToString(imageBytes);

            return "data:image/png;base64," + base64String;
        } catch (Exception e) {
            log.warn("Failed to generate embedded image for color: {}", colorStr, e);
            return null;
        }
    }

    private static Color parseColor(String colorStr) {
        if (colorStr.startsWith("#")) {
            return Color.decode(colorStr);
        }

        if (colorStr.startsWith("rgb")) {
            return parseRgbColor(colorStr);
        }

        if (colorStr.startsWith("hsl")) {
            return parseHslaColor(colorStr);
        }

        throw new IllegalArgumentException("Unsupported color format: " + colorStr);
    }

    private static Color parseRgbColor(String rgb) {
        String[] rgbValues = rgb.replaceAll("[^0-9,]", "").split(",");
        int r = Integer.parseInt(rgbValues[0]);
        int g = Integer.parseInt(rgbValues[1]);
        int b = Integer.parseInt(rgbValues[2]);
        return new Color(r, g, b);
    }

    private static Color parseHslaColor(String hsla) {
        String[] hslaValues = hsla.replaceAll("[^0-9.,]", "").split(",");
        float h = Float.parseFloat(hslaValues[0]);
        float s = Float.parseFloat(hslaValues[1]) / 100;
        float l = Float.parseFloat(hslaValues[2]) / 100;
        float a = hslaValues.length > 3 ? Float.parseFloat(hslaValues[3]) : 1.0f;
        return hslaToColor(h, s, l, a);
    }

    private static Color hslaToColor(float h, float s, float l, float alpha) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h / 60) % 2 - 1));
        float m = l - c / 2;

        float r = 0, g = 0, b = 0;
        if (h < 60) {
            r = c;
            g = x;
        } else if (h < 120) {
            r = x;
            g = c;
        } else if (h < 180) {
            g = c;
            b = x;
        } else if (h < 240) {
            g = x;
            b = c;
        } else if (h < 300) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }

        r += m;
        g += m;
        b += m;

        return new Color(clamp(r), clamp(g), clamp(b), clamp(alpha));
    }

    private static float clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }

}
