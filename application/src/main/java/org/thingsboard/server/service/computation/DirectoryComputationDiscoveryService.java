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

import akka.dispatch.ExecutionContexts;
import akka.dispatch.OnComplete;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.msg.computation.ComputationActionCompiled;
import org.thingsboard.server.common.msg.computation.ComputationActionDeleted;
import org.thingsboard.server.dao.plugin.PluginService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.computation.annotation.AnnotationsProcessor;
import org.thingsboard.server.service.computation.classloader.RuntimeJavaCompiler;
import org.thingsboard.server.utils.MiscUtils;
import scala.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service("computationDiscoveryService")
@Slf4j
public class DirectoryComputationDiscoveryService implements ComputationDiscoveryService{

    private static final Executor executor = Executors.newSingleThreadExecutor();
    private final String PLUGIN_CLAZZ = "org.thingsboard.server.extensions.spark.computation.plugin.SparkComputationPlugin";

    @Value("${spark.jar_path}")
    private String libraryPath;

    @Value("${spark.polling_interval}")
    private Long pollingInterval;

    @Autowired
    private ComponentDiscoveryService componentDiscoveryService;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private ApplicationContext context;

    private RuntimeJavaCompiler compiler;
    private FileAlterationMonitor monitor;
    private Map<String, Set<String>> processedJarsSources = new HashMap<>();

    @PostConstruct
    public void init() {
        log.warn("Initializing bean Directory Computation discovery.");
        Assert.hasLength(libraryPath, MiscUtils.missingProperty("spark.jar_path"));
        Assert.notNull(pollingInterval, MiscUtils.missingProperty("spark.polling_interval"));
        this.compiler = new RuntimeJavaCompiler();
        discoverDynamicComponents();
        startPolling();
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
                        processedJarsSources.put(j.toFile().getName(),
                                c.stream().map(ComputationActionCompiled::getActionClazz).collect(Collectors.toSet()));
                    }
                }
            }
            componentDiscoveryService.updateActionsForPlugin(compiledActions, PLUGIN_CLAZZ);
        } catch (IOException e) {
            log.error("Error while reading jars from directory.", e);
        }
    }

    private void startPolling(){
        try {
            final FileSystem fs = FileSystems.getDefault();
            FileAlterationObserver observer = new FileAlterationObserver(fs.getPath(libraryPath).toFile());
            monitor = new FileAlterationMonitor(pollingInterval * 1000);
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
                log.debug("File {} is created", file.getAbsolutePath());
                processComponent(file);
            }

            @Override
            public void onFileChange(File file) {
                log.debug("File {} is modified", file.getAbsolutePath());
                processComponent(file);
            }

            @Override
            public void onFileDelete(File file){
                log.debug("File {} is deleted", file.getAbsolutePath());
                Set<String> actionsToRemove = processedJarsSources.get(file.getName());
                if(actionsToRemove != null && !actionsToRemove.isEmpty()){
                    log.debug("Actions to remove are {}", actionsToRemove);
                    ComputationMsgListener listener = context.getBean(ComputationMsgListener.class);
                    Optional<ComponentDescriptor> plugin = componentDiscoveryService.getComponent(PLUGIN_CLAZZ);
                    if(plugin.isPresent() && listener != null) {
                        PluginMetaData pluginMetadata = pluginService.findPluginByClass(PLUGIN_CLAZZ);
                        if(pluginMetadata != null) {
                            log.debug("Plugin Metadata found {}", pluginMetadata);
                            final ComputationActionDeleted msg = new ComputationActionDeleted(actionsToRemove, pluginMetadata.getApiToken());
                            Future<Object> fut = listener.onMsg(msg);

                            fut.onComplete(new OnComplete<Object>(){
                                public void onComplete(Throwable t, Object result){
                                    if(t != null){
                                        log.error("Error occurred while trying to delete rules", t);
                                    }else {
                                        log.info("Deleting action descriptors from plugin");
                                        componentDiscoveryService.deleteActionsFromPlugin(msg, PLUGIN_CLAZZ);
                                    }
                                }
                            }, ExecutionContexts.fromExecutor(executor));
                        }
                    }
                }
            }

            private void processComponent(File file) {
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

    @PreDestroy
    public void destroy(){
        log.debug("Cleaning up resources from Computation discovery service");
        try {
            if (monitor != null) {
                monitor.stop();
            }
            if(compiler != null){
                compiler.destroy();
            }
        } catch (Exception e) {
            log.error("Error while cleaning up resources for Directory Polling service");
        }
    }
}
