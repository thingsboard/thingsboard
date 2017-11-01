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
package org.thingsboard.server.actors.computation;

import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.common.msg.computation.SparkComputationAdded;
import org.thingsboard.server.service.computation.annotation.AnnotationsProcessor;
import org.thingsboard.server.service.computation.classloader.DynamicCompiler;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class SparkComputationActor extends ContextAwareActor{

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private DynamicCompiler compiler;

    public SparkComputationActor(ActorSystemContext systemContext) {
        super(systemContext);
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        this.compiler = new DynamicCompiler();
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        logger.debug("Received message: {}", msg);
        if(msg instanceof SparkComputationAdded){
            logger.warning("Message received {} ", msg);
            processResource(((SparkComputationAdded)msg).getJarPath());
        }else{
            logger.warning("Unknown message: {}!", msg);
        }
    }

    private void processResource(Path jarPath) throws Exception {
        logger.warning("adding jar {} to classpath", jarPath.toFile().getName());
        URLClassLoader classLoader = (URLClassLoader) jarClassloader(jarPath);
        logger.warning("Classpath is {}", System.getProperty("java.class.path").split(File.pathSeparator));
        logger.warning("starting annotation processing for jar {}", jarPath.toFile().getName());
        AnnotationsProcessor processor = new AnnotationsProcessor(classLoader, compiler);
        processor.processAnnotations(jarPath);
    }

    private ClassLoader jarClassloader(Path jarPath) throws MalformedURLException {
        return new URLClassLoader(new URL[]{jarPath.toFile().toURI().toURL()}, this.getClass().getClassLoader());
    }

    public static class ActorCreator extends ContextBasedCreator<SparkComputationActor> {
        private static final long serialVersionUID = 1L;

        public ActorCreator(ActorSystemContext context) {
            super(context);
        }

        @Override
        public SparkComputationActor create() throws Exception {
            return new SparkComputationActor(context);
        }
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create("1 minute"), new Function<Throwable, SupervisorStrategy.Directive>() {
        @Override
        public SupervisorStrategy.Directive apply(Throwable t) {
            logger.error(t, "Unknown failure");
            if (t instanceof RuntimeException) {
                return SupervisorStrategy.restart();
            } else {
                return SupervisorStrategy.stop();
            }
        }
    });
}
