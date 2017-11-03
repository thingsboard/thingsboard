/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.service.computation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.thingsboard.server.common.msg.computation.ComputationActionCompiled;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.computation.annotation.AnnotationsProcessor;
import org.thingsboard.server.service.computation.classloader.RuntimeJavaCompiler;
import org.thingsboard.server.utils.MiscUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DirectoryComputationDiscoveryService implements ComputationDiscoveryService{

    @Value("${spark.jar_path}")
    private String libraryPath;

    @Value("${spark.polling_interval}")
    private Long pollingInterval;

    private RuntimeJavaCompiler compiler;
    private final String PLUGIN_CLAZZ = "org.thingsboard.server.extensions.livy.plugin.LivyPlugin";

    private ComponentDiscoveryService componentDiscoveryService;

    @Override
    public void init(ComponentDiscoveryService service) {
        Assert.hasLength(libraryPath, MiscUtils.missingProperty("spark.jar_path"));
        Assert.notNull(pollingInterval, MiscUtils.missingProperty("spark.polling_interval"));
        this.compiler = new RuntimeJavaCompiler();
        this.componentDiscoveryService = service;
    }

    public void discoverDynamicComponents(){
        final FileSystem fs = FileSystems.getDefault();
        final List<ComputationActionCompiled> compiledActions = new ArrayList<>();
        try {
            List<Path> jars = Files.walk(fs.getPath(libraryPath)).collect(Collectors.toList());
            for(Path j: jars){
                if(isJar(j)) {
                    AnnotationsProcessor processor = new AnnotationsProcessor(j, compiler);
                    List<ComputationActionCompiled> c = processor.processAnnotations();
                    if(c != null && !c.isEmpty()) {
                        compiledActions.addAll(c);
                    }
                }
            }
            componentDiscoveryService.updateActionsForPlugin(compiledActions, PLUGIN_CLAZZ);
            startPolling();
        } catch (IOException e) {
            log.error("Error while reading jars from directory.", e);
        }
    }

    private void startPolling(){
        try {
            final FileSystem fs = FileSystems.getDefault();
            FileAlterationObserver observer = new FileAlterationObserver(fs.getPath(libraryPath).toFile());
            FileAlterationMonitor monitor = new FileAlterationMonitor(pollingInterval * 1000);
            observer.addListener(newListener());
            monitor.addObserver(observer);
            monitor.start();
        } catch (Exception e) {
            log.error("Error while starting poller to scan dynamic components", e);
        }
    }

    private boolean isJar(Path jarPath) throws IOException {
        File file = jarPath.toFile();
        return file.getCanonicalPath().endsWith(".jar") && file.canRead();
    }

    private FileAlterationListener newListener(){
        return new FileAlterationListenerAdaptor(){
            @Override
            public void onFileCreate(File file) {
                processComponent(file);
            }

            @Override
            public void onFileChange(File file) {
                processComponent(file);
            }

            private void processComponent(File file) {
                log.debug("File {} is created", file.getAbsolutePath());
                Path j = file.toPath();
                try{
                    if(isJar(j)){
                        AnnotationsProcessor processor = new AnnotationsProcessor(j, compiler);
                        List<ComputationActionCompiled> actions = processor.processAnnotations();
                        componentDiscoveryService.updateActionsForPlugin(actions, PLUGIN_CLAZZ);
                    }
                } catch (IOException e) {
                    log.error("Error while accessing jar to scan dynamic components", e);
                }
            }
        };
    }
}
