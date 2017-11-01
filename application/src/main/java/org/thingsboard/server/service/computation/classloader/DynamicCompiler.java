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
package org.thingsboard.server.service.computation.classloader;

import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import javax.tools.*;

@Slf4j
public class DynamicCompiler {
    private JavaFileManager fileManager ;
    private JavaCompiler compiler;
    private String classPath;

    public DynamicCompiler(){
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Cannot find the system Java compiler. "
                    + "Check that your class path includes tools.jar");
        }
        fileManager = initFileManager();
        //classPath = initClassPath();
    }

    private JavaFileManager initFileManager(){
        log.warn("Initializing file manager");
        if(fileManager!=null)
            return fileManager;
        else {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            fileManager = new SourceFileManager(
                    compiler.getStandardFileManager(null, null, null));
            return fileManager;
        }

    }

    private String initClassPath(){
        if(classPath == null) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            String cp = "";
            if (contextClassLoader instanceof URLClassLoader) {
                URLClassLoader load = (URLClassLoader) contextClassLoader;
                StringBuilder builder = new StringBuilder();
                for (URL s : load.getURLs()) {
                    builder.append(File.pathSeparator);
                    builder.append(s.getPath());
                }
                cp = builder.toString();
            }
            classPath = System.getProperty("java.class.path") + cp;
        }
        return classPath;
    }

    public void compile(String fullName, CharSequence sourceCode){
        initClassPath();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        System.setProperty("lombok.disable", "true");
        JavaFileObject source = new CharSequenceJavaFileObject(fullName, sourceCode);
        List<JavaFileObject> jfiles = Collections.singletonList(source);

        List<String> options = new ArrayList<String>();
        options.addAll(Arrays.asList("-classpath", classPath));
        log.warn("Starting to compile source code {}", sourceCode);

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options,
                null, jfiles);
        if (!task.call()) {
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                log.warn("Error on line {} in {}", diagnostic.getLineNumber(), diagnostic.getMessage(Locale.ENGLISH));
            }
        }
        log.warn("Compilation completed");
    }

    public void load(String fullName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        // Creating an instance of our compiled class and
        // running its toString() method
        fileManager.getClassLoader(StandardLocation.CLASS_PATH)
                .loadClass(fullName).newInstance();
    }
}
