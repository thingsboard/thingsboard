// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.spi;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.function.ToIntFunction;

/**
 * Substrate-agnostic exception classification shared by every {@link ExtensionReconciler}.
 * Each backend layers its own client-specific HTTP-status check on top of this.
 */
public final class ReconcilerExceptions {

    private ReconcilerExceptions() {}

    private static final int MAX_MESSAGE_LENGTH = 256;
    private static final Pattern URL_PATTERN = Pattern.compile("\\bhttps?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERNAL_HOST_PATTERN =
            Pattern.compile("\\b[a-z0-9.-]+\\.(?:svc|cluster\\.local|internal)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern IP_PATTERN = Pattern.compile("\\b\\d{1,3}(?:\\.\\d{1,3}){3}\\b");

    /**
     * Tenant-safe rendering of a substrate exception: strips cluster-internal endpoints (URLs, IPs,
     * *.svc / cluster.local / .internal hostnames) and truncates. Every message that can reach a tenant
     * — the stored lastError, a "(could not read logs: ...)" section — must go through here; a raw
     * fabric8 or docker-java message carries the API-server or daemon URL verbatim.
     */
    public static String sanitizeMessage(Throwable e) {
        if (e == null) {
            return "Reconcile failed.";
        }
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return e.getClass().getSimpleName();
        }
        String redacted = URL_PATTERN.matcher(msg).replaceAll("[redacted-url]");
        redacted = INTERNAL_HOST_PATTERN.matcher(redacted).replaceAll("[redacted-host]");
        redacted = IP_PATTERN.matcher(redacted).replaceAll("[redacted-ip]");
        if (redacted.length() > MAX_MESSAGE_LENGTH) {
            redacted = redacted.substring(0, MAX_MESSAGE_LENGTH) + "…";
        }
        return redacted;
    }

    /**
     * True if any cause in the chain is an {@link IOException} — a socket/connect/handshake failure
     * that is by nature transient and worth retrying on every substrate (the API server / Docker
     * daemon was briefly unreachable).
     */
    public static boolean isTransportError(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof IOException) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether an HTTP status code returned by a substrate client is transient: no response
     * ({@code <= 0}), request timeout (408), throttling (429), or any server-side error (5xx).
     * Everything else (4xx: bad request, forbidden, not found, conflict) is terminal.
     */
    public static boolean isRetryableHttpStatus(int code) {
        return code <= 0 || code == 408 || code == 429 || code >= 500;
    }

    /**
     * Shared retry skeleton: walk the cause chain once, and if a {@code clientType} exception is found
     * classify it by its HTTP status ({@link #isRetryableHttpStatus}); otherwise fall back to the
     * transport-error check. Each backend differs only in its client exception type and status getter.
     */
    public static <T extends Throwable> boolean isRetryable(Throwable e, Class<T> clientType, ToIntFunction<T> statusOf) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (clientType.isInstance(t)) {
                return isRetryableHttpStatus(statusOf.applyAsInt(clientType.cast(t)));
            }
        }
        return isTransportError(e);
    }

}
