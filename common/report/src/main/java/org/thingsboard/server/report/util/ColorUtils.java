// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import org.thingsboard.server.common.data.StringUtils;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HSL_PATTERN = Pattern.compile("hsla?\\(\\s*(\\d+)\\s*,\\s*(\\d+)%\\s*,\\s*(\\d+)%\\s*(,\\s*([0-1]\\.\\d+))?\\s*\\)");

    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    public static Color applyOpacity(Color originalColor, float opacity) {
        int currentAlpha = originalColor.getAlpha();
        int newAlpha = Math.min(255, Math.round(currentAlpha * opacity));
        return new Color(
                originalColor.getRed(),
                originalColor.getGreen(),
                originalColor.getBlue(),
                newAlpha
        );
    }

    public static Color setOpacity(Color originalColor, float opacity) {
        int newAlpha = Math.min(255, Math.round(255 * opacity));
        return new Color(
                originalColor.getRed(),
                originalColor.getGreen(),
                originalColor.getBlue(),
                newAlpha
        );
    }

    public static String normalizeCssColor(String color) {
        Color c = parseCssColor(color);
        if (c.getAlpha() == 255) {
            return "rgb(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")";
        } else {
            return "rgba(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "," + Math.round(c.getAlpha() / 255.0f * 100.0) / 100.0 + ")";
        }
    }

    public static Object normalizeCssColorOrDefault(String color, String defaultColor) {
        return color != null ? ColorUtils.normalizeCssColor(color) : defaultColor;
    }

    public static Color safeParseCssColor(String cssColor) {
        return safeParseCssColor(cssColor, Color.BLACK);
    }

    public static Color safeParseCssColor(String cssColor, Color fallbackColor) {
        if (StringUtils.isBlank(cssColor)) {
            return fallbackColor;
        }
        try {
            return parseCssColor(cssColor);
        } catch (Exception e) {
            return fallbackColor;
        }
    }

    public static Color parseCssColor(String cssColor) {
        if (StringUtils.isBlank(cssColor)) {
            throw new IllegalArgumentException("CSS color cannot be null or empty");
        }

        cssColor = cssColor.trim().toLowerCase();

        Color namedColor = getNamedColor(cssColor);
        if (namedColor != null) {
            return namedColor;
        }

        if (cssColor.startsWith("#")) {
            return parseHexColor(cssColor);
        }

        if (cssColor.startsWith("rgb")) {
            return parseRgbColor(cssColor);
        }

        if (cssColor.startsWith("hsl")) {
            return parseHslColor(cssColor);
        }

        throw new IllegalArgumentException("Unsupported CSS color format: " + cssColor);
    }

    public static String getMaterialColor(int index) {
       int colorIndex = index % MATERIAL_COLORS.length;
       return MATERIAL_COLORS[colorIndex];
    }

    private static Color getNamedColor(String name) {
        return switch (name) {
            case "red" -> Color.RED;
            case "blue" -> Color.BLUE;
            case "green" -> Color.GREEN;
            case "black" -> Color.BLACK;
            case "white" -> Color.WHITE;
            case "yellow" -> Color.YELLOW;
            case "cyan" -> Color.CYAN;
            case "magenta" -> Color.MAGENTA;
            case "gray" -> Color.GRAY;
            case "lightgray" -> Color.LIGHT_GRAY;
            default -> null;
        };
    }

    private static Color parseHexColor(String hex) {
        String hexClean = hex.replace("#", "");
        if (hexClean.length() == 3) {
            hexClean = "" + hexClean.charAt(0) + hexClean.charAt(0) +
                    hexClean.charAt(1) + hexClean.charAt(1) +
                    hexClean.charAt(2) + hexClean.charAt(2);
        }
        if (hexClean.length() == 6 || hexClean.length() == 8) {
            try {
                int r = Integer.parseInt(hexClean.substring(0, 2), 16);
                int g = Integer.parseInt(hexClean.substring(2, 4), 16);
                int b = Integer.parseInt(hexClean.substring(4, 6), 16);
                float a = hexClean.length() == 8 ? Integer.parseInt(hexClean.substring(6, 8), 16) / 255.0f : 1.0f;
                return new Color(r / 255.0f, g / 255.0f, b / 255.0f, a);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid HEX color: " + hex, e);
            }
        }
        throw new IllegalArgumentException("Invalid HEX color length: " + hex);
    }

    private static Color parseRgbColor(String rgb) {
        Pattern pattern = Pattern.compile("rgba?\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*(,\\s*([0-1]\\.\\d+))?\\s*\\)");
        Matcher matcher = pattern.matcher(rgb);
        if (matcher.matches()) {
            int r = Integer.parseInt(matcher.group(1));
            int g = Integer.parseInt(matcher.group(2));
            int b = Integer.parseInt(matcher.group(3));
            float a = matcher.group(5) != null ? Float.parseFloat(matcher.group(5)) : 1.0f;
            validateRgbValues(r, g, b, a);
            return new Color(r / 255.0f, g / 255.0f, b / 255.0f, a);
        }
        throw new IllegalArgumentException("Invalid RGB/RGBA color: " + rgb);
    }

    private static Color parseHslColor(String hsl) {
        Matcher matcher = HSL_PATTERN.matcher(hsl);
        if (matcher.matches()) {
            float h = Float.parseFloat(matcher.group(1)) / 360.0f; // Нормалізація до [0, 1]
            float s = Float.parseFloat(matcher.group(2)) / 100.0f;
            float l = Float.parseFloat(matcher.group(3)) / 100.0f;
            float a = matcher.group(5) != null ? Float.parseFloat(matcher.group(5)) : 1.0f;
            validateHslValues(h * 360, s * 100, l * 100, a);
            return hslToRgb(h, s, l, a);
        }
        throw new IllegalArgumentException("Invalid HSL/HSLA color: " + hsl);
    }

    private static void validateRgbValues(int r, int g, int b, float a) {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255 || a < 0 || a > 1) {
            throw new IllegalArgumentException("RGB values must be in [0, 255], alpha in [0, 1]");
        }
    }

    private static void validateHslValues(float h, float s, float l, float a) {
        if (h < 0 || h > 360 || s < 0 || s > 100 || l < 0 || l > 100 || a < 0 || a > 1) {
            throw new IllegalArgumentException("HSL values must be: H in [0, 360], S/L in [0, 100], alpha in [0, 1]");
        }
    }

    private static Color hslToRgb(float h, float s, float l, float a) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = l - c / 2;

        float r, g, b;
        int sector = (int) (h * 6);
        switch (sector) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }

        r = (r + m) * 255;
        g = (g + m) * 255;
        b = (b + m) * 255;

        return new Color(r / 255.0f, g / 255.0f, b / 255.0f, a);
    }

    private static final String[] MATERIAL_COLORS = new String[]{
            "#2196f3",
            "#4caf50",
            "#f44336",
            "#ffc107",
            "#607d8b",
            "#9c27b0",
            "#8bc34a",
            "#3f51b5",
            "#e91e63",
            "#ffeb3b",
            "#03a9f4",
            "#ff9800",
            "#673ab7",
            "#cddc39",
            "#009688",
            "#795548",
            "#00bcd4",
            "#ff5722",
            "#9e9e9e",
            "#2962ff",
            "#00c853",
            "#d50000",
            "#ffab00",
            "#455a64",
            "#aa00ff",
            "#64dd17",
            "#304ffe",
            "#c51162",
            "#ffd600",
            "#0091ea",
            "#ff6d00",
            "#6200ea",
            "#aeea00",
            "#00bfa5",
            "#5d4037",
            "#00b8d4",
            "#dd2c00",
            "#616161",
            "#1e88e5",
            "#43a047",
            "#e53935",
            "#ffb300",
            "#546e7a",
            "#8e24aa",
            "#7cb342",
            "#3949ab",
            "#d81b60",
            "#fdd835",
            "#039be5",
            "#fb8c00",
            "#5e35b1",
            "#c0ca33",
            "#00897b",
            "#6d4c41",
            "#00acc1",
            "#f4511e",
            "#757575",
            "#1976d2",
            "#388e3c",
            "#d32f2f",
            "#ffa000",
            "#455a64",
            "#7b1fa2",
            "#689f38",
            "#303f9f",
            "#c2185b",
            "#fbc02d",
            "#0288d1",
            "#f57c00",
            "#512da8",
            "#afb42b",
            "#00796b",
            "#5d4037",
            "#0097a7",
            "#e64a19",
            "#616161",
            "#1565c0",
            "#2e7d32",
            "#c62828",
            "#ff8f00",
            "#37474f",
            "#6a1b9a",
            "#558b2f",
            "#283593",
            "#ad1457",
            "#f9a825",
            "#0277bd",
            "#ef6c00",
            "#4527a0",
            "#9e9d24",
            "#00695c",
            "#4e342e",
            "#00838f",
            "#d84315",
            "#424242",
            "#0d47a1",
            "#1b5e20",
            "#b71c1c",
            "#ff6f00",
            "#263238",
            "#4a148c",
            "#33691e",
            "#1a237e",
            "#880e4f",
            "#f57f17",
            "#01579b",
            "#e65100",
            "#311b92",
            "#827717",
            "#004d40",
            "#3e2723",
            "#006064",
            "#bf360c",
            "#212121",
            "#64b5f6",
            "#81c784",
            "#e57373",
            "#ffd54f",
            "#90a4ae",
            "#ba68c8",
            "#aed581",
            "#7986cb",
            "#f06292",
            "#fff176",
            "#4fc3f7",
            "#ffb74d",
            "#9575cd",
            "#dce775",
            "#4db6ac",
            "#a1887f",
            "#4dd0e1",
            "#ff8a65",
            "#e0e0e0",
            "#42a5f5",
            "#66bb6a",
            "#ef5350",
            "#ffca28",
            "#78909c",
            "#ab47bc",
            "#9ccc65",
            "#5c6bc0",
            "#ec407a",
            "#ffee58",
            "#29b6f6",
            "#ffa726",
            "#7e57c2",
            "#d4e157",
            "#26a69a",
            "#8d6e63",
            "#26c6da",
            "#ff7043",
            "#bdbdbd",
            "#448aff",
            "#69f0ae",
            "#ff5252",
            "#ffd740",
            "#b0bec5",
            "#e040fb",
            "#b2ff59",
            "#536dfe",
            "#ff4081",
            "#ffff00",
            "#40c4ff",
            "#ffab40",
            "#7c4dff",
            "#eeff41",
            "#64ffda",
            "#bcaaa4",
            "#18ffff",
            "#ff6e40",
            "#000000",
            "#2979ff",
            "#00e676",
            "#ff1744",
            "#ffc400",
            "#78909c",
            "#d500f9",
            "#76ff03",
            "#3d5afe",
            "#f50057",
            "#ffea00",
            "#00b0ff",
            "#ff9100",
            "#651fff",
            "#c6ff00",
            "#1de9b6",
            "#8d6e63",
            "#00e5ff",
            "#ff3d00",
            "#303030"
    };

}
