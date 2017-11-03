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
import org.apache.commons.lang.StringUtils;
import org.thingsboard.server.service.computation.classloader.exception.CompilationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import javax.tools.*;

@Slf4j
public class RuntimeJavaCompiler {
    private static final String BANG = "!";
    private static final String JAR = ".jar";
    private StandardJavaFileManager fileManager;
    private JavaCompiler compiler;
    private File tempDir;
    private final Object mutex = new Object();
    private boolean classPathInitialized = false;

    public RuntimeJavaCompiler(){
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Cannot find the system Java compiler. "
                    + "Check that your class path includes tools.jar");
        }
        initTempDirectory();
    }

    private void initTempDirectory(){
        try {
            Path dir = Files.createTempDirectory("spark");
            dir.toFile().deleteOnExit();
            this.tempDir = dir.toFile();
        } catch (IOException e) {
            log.error("Error occurred while creating temp directory", e);
        }
    }

    private void initFileManager(){
        if(fileManager == null) {
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            try {
                fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(tempDir));
                this.fileManager = fileManager;
            } catch (Exception e) {
                log.error("Error while setting class path", e);
            }
        }
    }

    private void initClassPath() throws IOException, URISyntaxException {
        if(!classPathInitialized) {
            Set<File> classPaths = new HashSet<>();
            appendExistingClasspath(classPaths);
            classPaths.add(tempDir);
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader instanceof URLClassLoader) {
                URLClassLoader load = (URLClassLoader) contextClassLoader;
                addClassDirectoryToClassPath(load);
                classPaths.addAll(parseUrlsAsClasspath(load.getURLs()));
            }
            fileManager.setLocation(StandardLocation.CLASS_PATH, classPaths);
            classPathInitialized = true;
        }
    }

    private void appendExistingClasspath(Set<File> classPaths) {
        for(File classPath : fileManager.getLocation((StandardLocation.CLASS_PATH)))
            classPaths.add(classPath);
        classPaths.add(tempDir);
    }

    private void addClassDirectoryToClassPath(URLClassLoader loader){
        try {
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(loader, new Object[]{tempDir.toURI().toURL()});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void compile(String fullName, CharSequence sourceCode) throws CompilationException {
        try {
            if(!tempDir.exists()){
                throw new CompilationException("Source directory not created");
            }
            synchronized(mutex){
                initFileManager();
                initClassPath();
            }
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            File sourceFile = createSourceFile(fullName, sourceCode.toString());
            log.debug("Starting to compile source code.");

            Iterable<? extends JavaFileObject> sourceObject = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile));
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null,
                    null, sourceObject);
            if (!task.call()) {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    log.error("Error on line {} in {}", diagnostic.getLineNumber(), diagnostic.getMessage(Locale.ENGLISH));
                }
                throw new CompilationException("Error occurred while compiling source "+ diagnostics.getDiagnostics());
            }
        } catch (Exception e) {
            log.error("Error occurred while compiling source {}", fullName, e);
            throw new CompilationException("Error occurred while compiling source", e);
        }
    }

    public Class<?> load(String fullName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<?> aClass = Class.forName(fullName);
        log.debug("Loading class completed {}", aClass.getCanonicalName());
        return aClass;
    }

    private File createSourceFile(String qualifiedClassName, String sourceCode) throws IOException {
        int dotPos = qualifiedClassName.lastIndexOf(".");
        String packageName = dotPos == -1 ? "" : qualifiedClassName.substring(0, dotPos);
        String className = dotPos == -1 ? qualifiedClassName : qualifiedClassName.substring(dotPos + 1);
        File packageDir = new File(tempDir.getPath() + File.separator + packageName.replace(".", File.separator));
        if(!packageDir.exists() && !packageDir.mkdirs()){
            throw new IOException("Error while creating a package directory for Source.");
        }
        File source = new File(packageDir, className + JavaFileObject.Kind.SOURCE.extension);
        if(!source.exists() && !source.createNewFile()){
            throw new IOException("Error while writing source file.");
        }
        FileWriter writer = new FileWriter(source);
        writer.write(sourceCode);
        writer.close();
        return source;
    }

    private List<File> parseUrlsAsClasspath(URL[] urls) throws IOException {
        Map<String, File> jarsProcessed = new HashMap<>();
        List<File> classPath = new ArrayList<>();
        String protocol = "file:";
        for(URL u: urls) {
            String c = u.getFile();
            if (c.startsWith(protocol)) {
                String path = c.substring(protocol.length(), c.lastIndexOf(BANG));
                if (path.contains(BANG)) {
                    String[] nestedCp = path.split(BANG);
                    String cpName = nestedCp[0];
                    log.debug("Nested classpath entry is {}", Arrays.toString(nestedCp));
                    if (isJar(cpName) && jarsProcessed.get(cpName) == null) {
                        JarUnpacker unpacker = new JarUnpacker(new JarFile(new File(cpName)));
                        File jarDir = unpacker.extractJar();
                        log.debug("Jar {} extracted in directory {}", cpName, jarDir.getAbsolutePath());
                        jarsProcessed.put(cpName, jarDir);
                        classPath.add(new File(jarsProcessed.get(cpName), nestedCp[1]));
                    }else if(isJar(cpName) && jarsProcessed.get(cpName) != null){
                        log.debug("Jar {} already extracted, adding to {} classpath", cpName, nestedCp[1]);
                        classPath.add(new File(jarsProcessed.get(cpName), nestedCp[1]));
                    }
                }else{
                    log.debug("Classpath entry {} is not nested jar/dir", path);
                    classPath.add(new File(path));
                }
            }else{
                log.debug("Adding normal file path {} to classpath", c);
                classPath.add(new File(c));
            }
        }
        return classPath;
    }

    private boolean isJar(String jarName) {
        return StringUtils.isNotEmpty(jarName) &&
                jarName.endsWith(JAR);
    }
}
