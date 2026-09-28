/*
 * SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
 * SPDX-License-Identifier: BUSL-1.1
 */
package org.thingsboard.packaging

import com.netflix.gradle.plugins.deb.control.MultiArch
import com.netflix.gradle.plugins.deb.validation.DebTaskPropertiesValidator
import com.netflix.gradle.plugins.packaging.AbstractPackagingCopyAction
import com.netflix.gradle.plugins.packaging.Dependency
import com.netflix.gradle.plugins.packaging.Directory
import com.netflix.gradle.plugins.packaging.Link
import com.netflix.gradle.plugins.utils.ApacheCommonsFileSystemActions
import com.netflix.gradle.plugins.utils.DeprecationLoggerUtils
import com.netflix.gradle.plugins.utils.FilePermissionUtil
import com.netflix.gradle.plugins.deb.InstallLineGenerator
import com.netflix.gradle.plugins.deb.Deb
import com.netflix.gradle.plugins.deb.DebFileVisitorStrategy
import com.netflix.gradle.plugins.deb.TemplateHelper
import groovy.transform.Canonical
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.gradle.api.GradleException
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.vafer.jdeb.Compression
import org.vafer.jdeb.Console
import org.vafer.jdeb.DataProducer
import org.vafer.jdeb.DebMaker
import org.vafer.jdeb.mapping.Mapper
import org.vafer.jdeb.mapping.PermMapper
import org.vafer.jdeb.producers.DataProducerLink
import org.vafer.jdeb.producers.DataProducerPathTemplate
import org.redline_rpm.payload.Directive

import static com.netflix.gradle.plugins.utils.GradleUtils.lookup
/**
 * Forked and modified from org.jamel.pkg4j.gradle.tasks.BuildDebTask
 */
class DebPackagingCopyAction extends AbstractPackagingCopyAction<Deb> {
    static final Logger logger = LoggerFactory.getLogger(DebPackagingCopyAction.class)

    File debianDir
    List<String> dependencies
    List<String> conflicts
    List<String> recommends
    List<String> suggests
    List<String> enhances
    List<String> preDepends
    List<String> breaks
    List<String> replaces
    List<String> provides
    List<DataProducer> dataProducers
    List<InstallDir> installDirs
    private final DebTaskPropertiesValidator debTaskPropertiesValidator = new DebTaskPropertiesValidator()
    private DebFileVisitorStrategy debFileVisitorStrategy
    private final DebMaintainerScriptsGenerator maintainerScriptsGenerator
    private final InstallLineGenerator installLineGenerator

    DebPackagingCopyAction(Deb debTask, File debianDir) {
        super(debTask)
        debTaskPropertiesValidator.validate(debTask)
        dependencies = []
        conflicts = []
        recommends = []
        suggests = []
        enhances = []
        preDepends = []
        breaks = []
        replaces = []
        dataProducers = []
        installDirs = []
        provides = []
        this.debianDir = debianDir
        debFileVisitorStrategy = new DebFileVisitorStrategy(dataProducers, installDirs)
        maintainerScriptsGenerator = new DebMaintainerScriptsGenerator(debTask, new TemplateHelper(debianDir, '/deb'), debianDir, new ApacheCommonsFileSystemActions())
        installLineGenerator = new InstallLineGenerator()
    }

    @Canonical
    static class InstallDir {
        String name
        String user
        String group
    }

    @Override
    void startVisit(CopyAction action) {
        super.startVisit(action)

        debianDir.deleteDir()
        debianDir.mkdirs()

    }

    @Override
    void visitFile(FileCopyDetailsInternal fileDetails, def specToLookAt) {
        logger.debug "adding file {}", fileDetails.relativePath.pathString

        def inputFile = extractFile(fileDetails)

        Directive fileType = lookup(specToLookAt, 'fileType')
        if (fileType == 'CONFIG') {
            logger.debug "mark {} as configuration file", fileDetails.relativePath.pathString
            task.configurationFile(fileDetails.relativePath.pathString)
        }

        String user = lookup(specToLookAt, 'user') ?: task.user
        Integer uid = (Integer) lookup(specToLookAt, 'uid') ?: task.uid ?: 0
        String group = lookup(specToLookAt, 'permissionGroup') ?: task.permissionGroup
        Integer gid = (Integer) lookup(specToLookAt, 'gid') ?: task.gid ?: 0

        Integer explicitMode = FilePermissionUtil.getFileMode(specToLookAt)
        int fileMode = explicitMode != null ? explicitMode : FilePermissionUtil.getUnixPermission(fileDetails)

        debFileVisitorStrategy.addFile(fileDetails, inputFile, user, uid, group, gid, fileMode)
    }

    @Override
    void visitDir(FileCopyDetailsInternal dirDetails, def specToLookAt) {
        def specCreateDirectoryEntry = lookup(specToLookAt, 'createDirectoryEntry')
        boolean createDirectoryEntry = specCreateDirectoryEntry!=null ? specCreateDirectoryEntry : task.createDirectoryEntry
        if (createDirectoryEntry) {

            logger.debug "adding directory {}", dirDetails.relativePath.pathString
            String user = lookup(specToLookAt, 'user') ?: task.user
            Integer uid = (Integer) lookup(specToLookAt, 'uid') ?: task.uid ?: 0
            String group = lookup(specToLookAt, 'permissionGroup') ?: task.permissionGroup
            Integer gid = (Integer) lookup(specToLookAt, 'gid') ?: task.gid ?: 0
            Boolean setgid = lookup(specToLookAt, 'setgid')

            Integer explicitDirMode = FilePermissionUtil.getDirMode(specToLookAt)
            int dirMode = explicitDirMode != null ? explicitDirMode : FilePermissionUtil.getUnixPermission(dirDetails)
            if (setgid == null) {
                setgid = task.setgid
            }
            if (setgid) {
                dirMode = dirMode | 02000
            }

            debFileVisitorStrategy.addDirectory(dirDetails, user, uid, group, gid, dirMode)
        }
    }

    @Override
    protected void addLink(Link link) {
        dataProducers << new DataProducerLink(link.path, link.target, true, null, null, null);
    }

    @Override
    protected void addDependency(Dependency dep) {
        dependencies << dep.toDebString()
    }

    @Override
    protected void addConflict(Dependency dep) {
        conflicts << dep.toDebString()
    }

    @Override
    protected void addProvides(Dependency dep) {
        String providesString = dep.packageName
        if (dep.version) {
            providesString += " (= ${dep.version})"
        }
        provides << providesString
    }

    @Override
    protected void addObsolete(Dependency dep) {
        logger.warn "Obsoletes functionality not implemented for deb files"
    }

    protected void addRecommends(Dependency dep) {
        recommends << dep.toDebString()
    }

    protected void addSuggests(Dependency dep) {
        suggests << dep.toDebString()
    }

    protected void addEnhances(Dependency dep) {
        enhances << dep.toDebString()
    }

    protected void addPreDepends(Dependency dep) {
        preDepends << dep.toDebString()
    }

    protected void addBreaks(Dependency dep) {
        breaks << dep.toDebString()
    }

    protected void addReplaces(Dependency dep) {
        replaces << dep.toDebString()
    }

    @Override
    protected void addDirectory(Directory directory) {
        def user = directory.user ? directory.user : task.user
        def permissionGroup = directory.permissionGroup ? directory.permissionGroup : task.permissionGroup
        dataProducers << new DataProducerPathTemplate(
                [directory.path] as String[], null, null,
                [ new PermMapper(-1, -1, user, permissionGroup,
                        directory.permissions, -1, 0, null) ] as Mapper[])
    }

    protected String getMultiArch() {
        def archString = task.getArchString()
        def multiArch = task.getMultiArch()
        if (('all' == archString) && (MultiArch.SAME == multiArch)) {
            throw new IllegalArgumentException('Deb packages with Architecture: all cannot declare Multi-Arch: same')
        }
        def multiArchString = multiArch?.name()?.toLowerCase() ?: ''
        return multiArchString
    }

    protected Map<String,String> getCustomFields() {
        task.getAllCustomFields().collectEntries { String key, String val ->
            // in the deb control file, header XB-Foo becomes Foo in the binary package
            ['XB-' + key.capitalize(), val]
        } as Map<String, String>
    }

    @Override
    protected void end() {
        for (Dependency recommends : task.allRecommends) {
            logger.debug "adding recommends on {} {}", recommends.packageName, recommends.version
            addRecommends(recommends)
        }

        for (Dependency suggests : task.allSuggests) {
            logger.debug "adding suggests on {} {}", suggests.packageName, suggests.version
            addSuggests(suggests)
        }

        for (Dependency enhances : task.allEnhances) {
            logger.debug "adding enhances on {} {}", enhances.packageName, enhances.version
            addEnhances(enhances)
        }

        for (Dependency preDepends : task.allPreDepends) {
            logger.debug "adding preDepends on {} {}", preDepends.packageName, preDepends.version
            addPreDepends(preDepends)
        }

        for (Dependency breaks : task.getAllBreaks()) {
            logger.debug "adding breaks on {} {}", breaks.packageName, breaks.version
            addBreaks(breaks)
        }

        for (Dependency replaces : task.allReplaces) {
            logger.debug "adding replaces on {} {}", replaces.packageName, replaces.version
            addReplaces(replaces)
        }

        maintainerScriptsGenerator.generate(toContext())

        task.allSupplementaryControlFiles.each { supControl ->
            File supControlFile = supControl instanceof File ? supControl as File : task.project.file(supControl)
            new File(debianDir, supControlFile.name).bytes = supControlFile.bytes
        }

        DebMaker maker = new DebMaker(new GradleLoggerConsole(), dataProducers, null)
        File debFile = task.getArchiveFile().get().asFile
        maker.setControl(debianDir)
        maker.setDeb(debFile)
        if (StringUtils.isNotBlank(task.getSigningKeyId())
                && StringUtils.isNotBlank(task.getSigningKeyPassphrase())
                && task.getSigningKeyRingFile().exists()) {
            maker.setKey(task.getSigningKeyId())
            maker.setPassphrase(task.getSigningKeyPassphrase())
            maker.setKeyring(task.getSigningKeyRingFile())
            maker.setSignPackage(true)
        }

        try {
            logger.info("Creating debian package: ${debFile}")
            maker.setCompression(Compression.GZIP.toString())
            maker.makeDeb()
        } catch (Exception e) {
            throw new GradleException("Can't build debian package ${debFile}", e)
        }

        logger.info 'Created deb {}', debFile
    }

    private static class GradleLoggerConsole implements Console {
        @Override
        void debug(String message) {
            logger.debug(message)
        }

        @Override
        void info(String message) {
            logger.info(message)
        }

        @Override
        void warn(String message) {
            logger.warn(message)
        }
    }

    /**
     * Map to be consumed by generateFile when transforming template
     */
    def Map toContext() {
        Map context = [:]
        DeprecationLoggerUtils.whileDisabled {
            context = [
                    name: task.getPackageName(),
                    version: task.getVersion(),
                    release: task.getRelease(),
                    maintainer: task.getMaintainer(),
                    uploaders: task.getUploaders(),
                    priority: task.getPriority(),
                    epoch: task.getEpoch(),
                    description: task.getPackageDescription() ?: '',
                    distribution: task.getDistribution(),
                    summary: task.getSummary(),
                    section: task.getPackageGroup(),
                    time: DateFormatUtils.SMTP_DATETIME_FORMAT.format(new Date()),
                    provides: StringUtils.join(provides, ", "),
                    depends: StringUtils.join(dependencies, ", "),
                    url: task.getUrl(),
                    arch: task.getArchString(),
                    multiArch: getMultiArch(),
                    conflicts: StringUtils.join(conflicts, ", "),
                    recommends: StringUtils.join(recommends, ", "),
                    suggests: StringUtils.join(suggests, ", "),
                    enhances: StringUtils.join(enhances, ", " ),
                    preDepends: StringUtils.join(preDepends, ", "),
                    breaks: StringUtils.join(breaks, ", "),
                    replaces: StringUtils.join(replaces, ", "),
                    fullVersion: buildFullVersion(),
                    customFields: getCustomFields(),

                    // Uses install command for directory
                    dirs: installDirs.collect { InstallDir dir -> [install: installLineGenerator.generate(dir)] }
            ]
        }
        return context
    }

    private String buildFullVersion() {
        StringBuilder fullVersion = new StringBuilder()

        DeprecationLoggerUtils.whileDisabled {
            if (task.getEpoch() != 0) {
                fullVersion <<= task.getEpoch()
                fullVersion <<= ':'
            }

            fullVersion <<= task.getVersion()

            if(task.getRelease()) {
                fullVersion <<= '-'
                fullVersion <<= task.getRelease()
            }

        }

        fullVersion.toString()
    }
}