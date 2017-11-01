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

import javax.tools.JavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DynamicClassLoader extends ClassLoader{
    private final Map<String, JavaFileObject> classes = new HashMap<String, JavaFileObject>();
    private DynamicClassLoader(ClassLoader classLoader) {
        super(classLoader);

    }

    Collection<JavaFileObject> files() {
        return Collections.unmodifiableCollection(classes.values());
    }

    @Override
    protected Class<?> findClass(final String qualifiedClassName)
            throws ClassNotFoundException {
        JavaFileObject file = classes.get(qualifiedClassName);
        if (file != null) {
            byte[] bytes = ((JavaClassObject) file).getBytes();
            return defineClass(qualifiedClassName, bytes, 0, bytes.length);
        }
        // Workaround for "feature" in Java 6
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6434149
        try {
            Class<?> c = Class.forName(qualifiedClassName);
            return c;
        } catch (ClassNotFoundException nf) {
            // Ignore and fall through
        }
        return super.findClass(qualifiedClassName);
    }

    @Override
    protected synchronized Class<?> loadClass(final String name, final boolean resolve)
            throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    public void add(final String qualifiedClassName, final JavaFileObject javaFile) {
        classes.put(qualifiedClassName, javaFile);
    }

    @Override
    public InputStream getResourceAsStream(final String name) {
        if (name.endsWith(".class")) {
            String qualifiedClassName = name.substring(0,
                    name.length() - ".class".length()).replace('/', '.');
            JavaClassObject file = (JavaClassObject) classes.get(qualifiedClassName);
            if (file != null) {
                return new ByteArrayInputStream(file.getBytes());
            }
        }
        return super.getResourceAsStream(name);
    }
}
