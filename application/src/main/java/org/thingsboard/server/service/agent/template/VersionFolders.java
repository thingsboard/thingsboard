// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.template;

import org.thingsboard.common.util.TbVersionUtils;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Version-named folders of the templates repo (e.g. {@code templates/since/4.4.0.1}, {@code compose/edge/4.4}): a
 * folder applies to its own version and every later one, until a newer folder exists.
 */
final class VersionFolders {

    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+)*");

    private VersionFolders() {
    }

    /**
     * The folder names that are plain versions not above {@code version}, newest first; other names are ignored.
     */
    static List<String> applicableTo(Collection<String> folders, String version) {
        return folders.stream()
                .filter(folder -> VERSION.matcher(folder).matches())
                .filter(folder -> TbVersionUtils.compare(folder, version) <= 0)
                .sorted((a, b) -> TbVersionUtils.compare(b, a))
                .toList();
    }
}
