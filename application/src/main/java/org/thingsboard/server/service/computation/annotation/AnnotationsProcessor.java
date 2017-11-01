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
package org.thingsboard.server.service.computation.annotation;

import akka.actor.ActorRef;
import com.hashmap.annotations.ConfigurationMapping;
import com.hashmap.annotations.Configurations;
import com.hashmap.annotations.SparkAction;
import com.hashmap.annotations.SparkRequest;
import com.hashmap.models.SparkActionConfigurationType;
import com.hashmap.models.SparkActionRequestType;
import com.hashmap.models.SparkActionType;
import eu.infomas.annotation.AnnotationDetector;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.thingsboard.server.service.computation.classloader.DynamicCompiler;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Properties;

@Slf4j
public class AnnotationsProcessor {
    private final URLClassLoader classLoader;
    private final DynamicCompiler compiler;
    private final ActorRef parent;

    public AnnotationsProcessor(URLClassLoader classLoader,
                                DynamicCompiler compiler,
                                ActorRef parentActor){
        this.classLoader = classLoader;
        this.compiler = compiler;
        this.parent = parentActor;
    }

    public void processAnnotations(Path jar) throws IOException {
        AnnotationDetector detector = new AnnotationDetector(newReporter());
        detector.detect(jar.toFile());
    }

    private AnnotationDetector.TypeReporter newReporter(){
        return new AnnotationDetector.TypeReporter() {
            @Override
            public void reportTypeAnnotation(Class<? extends Annotation> aClass, String s) {
                if(aClass.isAssignableFrom(SparkAction.class)){
                    try {
                        Class<?> clazz = classLoader.loadClass(s);
                        SparkActionType model = action(clazz);
                        model.setConfiguration(configurations(clazz));
                        model.setRequest(request(clazz));
                        log.warn("model created is {} generating Java Sources",model);
                        processModel(model);
                        //parent.tell("acd", ActorRef.noSender());
                        log.warn("Java Source creation and loading completed for {} ", clazz.getCanonicalName());
                    } catch (ClassNotFoundException e) {
                        log.error("Class not found", e);
                    }
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class<? extends Annotation>[] annotations() {
                return new Class[]{SparkAction.class};
            }
        };
    }

    private SparkActionType action(Class<?> clazz){
        SparkAction action = clazz.getAnnotation(SparkAction.class);
        SparkActionType model = new SparkActionType();
        model.setPackageName(clazz.getPackage().getName());
        model.setClassName(action.actionClass());
        model.setName(action.name());
        model.setDescriptor(action.descriptor());
        return model;
    }

    private SparkActionConfigurationType configurations(Class<?> clazz){
        Configurations configs = clazz.getAnnotation(Configurations.class);
        SparkActionConfigurationType actionConfiguration = null;
        if(configs != null) {
            actionConfiguration = new SparkActionConfigurationType();
            actionConfiguration.setClassName(configs.className());
            for (ConfigurationMapping mapping : configs.mappings()) {
                try {
                    actionConfiguration.addField(mapping.field(), mapping.type().getCanonicalName());
                } catch (MirroredTypeException me) {
                    TypeMirror typeMirror = me.getTypeMirror();
                    log.warn("found config {} for {}", mapping.field(), typeMirror.toString());
                    actionConfiguration.addField(mapping.field(), typeMirror.toString());
                }
            }
        }
        return actionConfiguration;
    }

    private SparkActionRequestType request(Class<?> clazz){
        SparkRequest request = clazz.getAnnotation(SparkRequest.class);
        SparkActionRequestType actionRequest = null;
        if(request != null){
            actionRequest = new SparkActionRequestType();
            actionRequest.setMainClass(request.main());
            actionRequest.setJar(request.jar());
            actionRequest.setArgs(request.args());
        }
        return actionRequest;
    }

    private void processModel(SparkActionType model){
        try {
            Properties props = new Properties();
            URL url = this.getClass().getClassLoader().getResource("velocity.properties");
            props.load(url.openStream());

            VelocityEngine ve = new VelocityEngine(props);
            ve.init();
            VelocityContext vc = new VelocityContext();
            vc.put("model", model);
            Template ct = ve.getTemplate("templates/config.vm");
            Class<?> configClass = generateSource(ct, vc, model.getPackageName() + "." + model.getConfiguration().getClassName());
            Template at = ve.getTemplate("templates/action.vm");
            Class<?> actionClass = generateSource(at, vc, model.getPackageName() + "." + model.getClassName());
        } catch (IOException e) {
            log.error("Exception occurred while generating java source", e);
        } catch (ClassNotFoundException e) {
            log.error("Exception while loading class", e);
        }
    }

    private Class<?> generateSource(Template vt, VelocityContext vc, String sourceName) throws IOException, ClassNotFoundException {
        log.warn("Generating source for {}", sourceName);
        StringWriter writer = new StringWriter();

        vt.merge(vc, writer);
        Class<?> clazz = null;

        try {
            compiler.compile(sourceName, writer.toString());
            clazz = compiler.load(sourceName);
        }catch(Exception e){
            log.warn("Error occurred {}", e);
        }
        writer.close();
        return clazz;
    }
}
