/*
 * SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
 * SPDX-License-Identifier: BUSL-1.1
 */
package org.thingsboard.packaging

import com.netflix.gradle.plugins.packaging.SystemPackagingBasePlugin
import com.netflix.gradle.plugins.packaging.SystemPackagingPlugin
import com.netflix.gradle.plugins.rpm.Rpm
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

public class PackagingPlugin implements Plugin<Project> {
    private static Logger logger = Logging.getLogger(SystemPackagingPlugin);

    Project project
    DebPackaging debTask
    Rpm rpmTask

    void apply(Project project) {

        this.project = project

        project.plugins.apply(SystemPackagingBasePlugin.class)
        debTask = project.tasks.create('buildDeb', DebPackaging)
        rpmTask = project.tasks.create('buildRpm', Rpm)
    }

}
