// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The routing slug of one extension, and nothing else.
 *
 * <p>Returned by the name-to-slug lookup that dashboard widgets use to build their
 * {@code /api/extension/route/{slug}/...} URLs. The slug is environment-specific (it is
 * allocated per instance and, in public cloud, carries a random suffix), so a dashboard
 * cannot hardcode it and stay portable. The extension <em>name</em> is stable across
 * environments, which makes it the right thing for a widget to hold.</p>
 *
 * <p>Deliberately minimal: a caller that only needs to address the data plane must not
 * receive the container spec, the state, or the internal id.</p>
 */
@Schema(description = "The routing slug of an extension.")
public record ExtensionSlugInfo(
        @Schema(description = "Immutable routing slug.", example = "acme-billing-9f3a") String slug) {
}
