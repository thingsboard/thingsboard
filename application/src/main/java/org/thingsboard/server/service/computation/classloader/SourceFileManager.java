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

import javax.tools.ForwardingJavaFileManager;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
public class SourceFileManager extends
        ForwardingJavaFileManager {
    /**
     * Instance of JavaClassObject that will store the
     * compiled bytecode of our class
     */
    /*private JavaClassObject jclassObject;
    private final Map<URI, JavaFileObject> fileObjects = new HashMap<URI, JavaFileObject>();*/
    Path dir;

    /**
     * Will initialize the manager with the specified
     * standard java file manager and Parent classloader
     *
     */
    public SourceFileManager(StandardJavaFileManager standardManager) {
        super(standardManager);
        setClasspathLocation(standardManager);
    }

    private void setClasspathLocation(StandardJavaFileManager standardManager){
        try{
            log.warn("Temporary Directory is: {} ", System.getProperty("java.io.tmpdir"));
            dir = Files.createTempDirectory("spark");
            dir.toFile().deleteOnExit();
            standardManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(dir.toFile()));
            ArrayList<File> classPaths = new ArrayList<>();
            for(File classPath : standardManager.getLocation((StandardLocation.CLASS_PATH)))
                classPaths.add(classPath);
            classPaths.add(dir.toFile());
            standardManager.setLocation(StandardLocation.CLASS_PATH, classPaths);
        } catch (IOException e) {
            log.error("Error while creating output directory", e);
        }
    }
    /**
     * Will be used by us to get the class loader for our
     * compiled class. It creates an anonymous class
     * extending the SecureClassLoader which uses the
     * byte code created by the compiler and stored in
     * the JavaClassObject, and returns the Class for it
     */
    /*@Override
    public ClassLoader getClassLoader(Location location) {
        return new SecureClassLoader(Thread.currentThread().getContextClassLoader()) {
            @Override
            protected Class<?> findClass(String name)
                    throws ClassNotFoundException {
                byte[] b = jclassObject.getBytes();
                return super.defineClass(name, jclassObject
                        .getBytes(), 0, b.length);
            }
        };
    }*/

    /**
     * Gives the compiler an instance of the JavaClassObject
     * so that the compiler can write the byte code into it.
     */
    /*public JavaFileObject getJavaFileForOutput(Location location,
                                               String className, Kind kind, FileObject sibling)
            throws IOException {
        jclassObject = new JavaClassObject(className, kind);
        return jclassObject;
    }*/
}
