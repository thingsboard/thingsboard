/*
 * SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
 * SPDX-License-Identifier: BUSL-1.1
 */
package org.thingsboard.packaging

import com.netflix.gradle.plugins.deb.Deb
import com.netflix.gradle.plugins.packaging.AbstractPackagingCopyAction
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

import javax.inject.Inject

abstract class DebPackaging extends Deb {

    @Inject
    DebPackaging(ProjectLayout projectLayout) {
        super(projectLayout)
    }

    @InputFile
    @Optional
    File configFile

    @InputFile
    @Optional
    File templatesFile

    @Override
    AbstractPackagingCopyAction createCopyAction() {
        return new DebPackagingCopyAction(this, new File(projectLayout.buildDirectory.getAsFile().get(), "debian"))
    }

}
