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
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.tools.*;

@Slf4j
public class DynamicCompiler {
    private StandardJavaFileManager fileManager ;
    private JavaCompiler compiler;
    private Path tempDir;
    private String classPath;
    private final Object mutex = new Object();

    public DynamicCompiler(){
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Cannot find the system Java compiler. "
                    + "Check that your class path includes tools.jar");
        }
        initTempDirectory();
        fileManager = initFileManager();
        //classPath = initClassPath();
    }

    private void initTempDirectory(){
        Path dir = null;
        try {
            dir = Files.createTempDirectory("spark");
            dir.toFile().deleteOnExit();
        } catch (IOException e) {
            log.error("Error occurred while creating temp directory", e);
        }
        this.tempDir = dir;
    }

    private StandardJavaFileManager initFileManager(){
        if(fileManager == null) {
            log.warn("Initializing file manager");
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            try {
                fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(tempDir.toFile()));
                this.fileManager = fileManager;
            } catch (Exception e) {
                log.error("Error while setting class path", e);
            }
        }
        return fileManager;
    }

    private void initClassPath() throws IOException {
        if(classPath == null) {
            ArrayList<File> classPaths = new ArrayList<>();
            addExistingManagerClasspath(classPaths);
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            String cp = "";
            if (contextClassLoader instanceof URLClassLoader) {
                URLClassLoader load = (URLClassLoader) contextClassLoader;
                addClassDirectoryToClassPath(load);
                StringBuilder builder = new StringBuilder();
                for (URL s : load.getURLs()) {
                    classPaths.add(new File(s.getPath()));
                    builder.append(File.pathSeparator);
                    builder.append(s.getPath());
                }
                cp = builder.toString();
            }
            fileManager.setLocation(StandardLocation.CLASS_PATH, classPaths);
            classPath = System.getProperty("java.class.path") + cp;
        }
    }

    private void addExistingManagerClasspath(ArrayList<File> classPaths) {
        for(File classPath : fileManager.getLocation((StandardLocation.CLASS_PATH)))
            classPaths.add(classPath);
        classPaths.add(tempDir.toFile());
    }

    private void addClassDirectoryToClassPath(URLClassLoader loader){
        try {
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(loader, new Object[]{tempDir.toUri().toURL()});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void compile(String fullName, CharSequence sourceCode){
        try {
            synchronized(mutex){
                initFileManager();
                initClassPath();
            }
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            File sourceFile = createSourceFile(fullName, sourceCode.toString());

            List<String> options = new ArrayList<String>();
            options.addAll(Arrays.asList("-classpath", classPath));
            log.warn("Starting to compile source code.");

            Iterable<? extends JavaFileObject> sourceObject = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile));
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options,
                    null, sourceObject);
            if (!task.call()) {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    log.warn("Error on line {} in {}", diagnostic.getLineNumber(), diagnostic.getMessage(Locale.ENGLISH));
                }
            }
            log.warn("Compilation completed");
        } catch (Exception e) {
            log.error("Error occured while compiling source {}", fullName, e);
        }
    }

    public Class<?> load(String fullName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<?> aClass = Class.forName(fullName);
        log.warn("Loading class completed {}", aClass.getCanonicalName());
        return aClass;
    }

    private File createSourceFile(String qualifiedClassName, String sourceCode) throws IOException {
        int dotPos = qualifiedClassName.lastIndexOf(".");
        String packageName = dotPos == -1 ? "" : qualifiedClassName.substring(0, dotPos);
        String className = dotPos == -1 ? qualifiedClassName : qualifiedClassName.substring(dotPos + 1);
        File packageDir = new File(tempDir.toFile().getPath() + File.separator + packageName.replace(".", File.separator));
        if(!packageDir.exists()){
            packageDir.mkdirs();
        }
        File source = new File(packageDir, className + JavaFileObject.Kind.SOURCE.extension);
        if(!source.exists()){
            source.createNewFile();
        }
        FileWriter writer = new FileWriter(source);
        writer.write(sourceCode);
        writer.close();
        return source;
    }
}
