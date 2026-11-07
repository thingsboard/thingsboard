// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.extension;

import org.thingsboard.server.exception.DataValidationException;

import java.security.SecureRandom;
import java.util.Set;
import java.util.regex.Pattern;

public final class ExtensionSlugUtils {

    // Keeps tb-ext-{slug} object names within the 63-char DNS limit.
    public static final int MAX_BASE_LENGTH = 40;

    public static final Pattern BASE_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,38}[a-z0-9])?$");

    // Slugs whose rendered object name (tb-ext-{slug}) would collide with an operator-owned shared
    // object. The data-plane router is deployed as tb-ext-router (k8s Deployment/Service, Docker
    // container) — a tenant slug "router" would make the reconciler create/update/delete it by name.
    // Forbidden in every mode so the collision is impossible even where slugs carry no random suffix.
    public static final Set<String> RESERVED_SLUGS = Set.of("router");

    /** Whether a slug base would collide with an operator-owned object name; see {@link #RESERVED_SLUGS}. */
    public static boolean isReserved(String slug) {
        return slug != null && RESERVED_SLUGS.contains(slug);
    }

    private static final String DEFAULT_BASE = "ext";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern LEADING_HYPHENS = Pattern.compile("^-+");
    private static final Pattern TRAILING_HYPHENS = Pattern.compile("-+$");

    private ExtensionSlugUtils() {}

    public static String normalizeBase(String base) {
        if (base == null) {
            throw new DataValidationException("Slug base must not be null!");
        }
        String normalized = base.trim().toLowerCase();
        if (!BASE_PATTERN.matcher(normalized).matches()) {
            throw new DataValidationException("Invalid slug base '" + base + "'. Expected a DNS-label-safe " +
                    "token: lowercase letters, digits and single internal hyphens, up to " + MAX_BASE_LENGTH + " characters.");
        }
        return normalized;
    }

    public static String slugify(String name) {
        if (name == null) {
            return DEFAULT_BASE;
        }
        String slug = NON_ALNUM.matcher(name.trim().toLowerCase()).replaceAll("-");
        slug = LEADING_HYPHENS.matcher(slug).replaceAll("");
        slug = TRAILING_HYPHENS.matcher(slug).replaceAll("");
        if (slug.length() > MAX_BASE_LENGTH) {
            slug = TRAILING_HYPHENS.matcher(slug.substring(0, MAX_BASE_LENGTH)).replaceAll("");
        }
        return slug.isEmpty() ? DEFAULT_BASE : slug;
    }

    public static String random4Hex() {
        return String.format("%04x", RANDOM.nextInt(0x10000));
    }

}
