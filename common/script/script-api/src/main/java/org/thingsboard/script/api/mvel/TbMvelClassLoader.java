/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.script.api.mvel;

import org.mvel2.compiler.AbstractParser;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

public class TbMvelClassLoader extends URLClassLoader {

    private static final Set<String> allowedClasses = new HashSet<>();
    private static final Set<String> allowedPackages = new HashSet<>();

    static {

        AbstractParser.LITERALS.remove("System");
        AbstractParser.LITERALS.remove("Runtime");
        AbstractParser.LITERALS.remove("Class");
        AbstractParser.LITERALS.remove("ClassLoader");
        AbstractParser.LITERALS.remove("Thread");
        AbstractParser.LITERALS.remove("Compiler");
        AbstractParser.LITERALS.remove("ThreadLocal");
        AbstractParser.LITERALS.remove("SecurityManager");

        AbstractParser.CLASS_LITERALS.remove("System");
        AbstractParser.CLASS_LITERALS.remove("Runtime");
        AbstractParser.CLASS_LITERALS.remove("Class");
        AbstractParser.CLASS_LITERALS.remove("ClassLoader");
        AbstractParser.CLASS_LITERALS.remove("Thread");
        AbstractParser.CLASS_LITERALS.remove("Compiler");
        AbstractParser.CLASS_LITERALS.remove("ThreadLocal");
        AbstractParser.CLASS_LITERALS.remove("SecurityManager");
        AbstractParser.CLASS_LITERALS.put("JSON", TbJson.class);
        AbstractParser.LITERALS.put("JSON", TbJson.class);

        AbstractParser.CLASS_LITERALS.values().forEach(val -> allowedClasses.add(((Class) val).getName()));
    }

    static {
        allowedPackages.add("org.mvel2");
        allowedPackages.add("java.util");
    }

    public TbMvelClassLoader() {
        super(new URL[0], Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!classNameAllowed(name)) {
            throw new ClassNotFoundException();
        }
        return super.loadClass(name, resolve);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (!classNameAllowed(name)) {
            throw new ClassNotFoundException();
        }
        return super.loadClass(name);
    }

    private boolean classNameAllowed(String name) {
        if (allowedClasses.contains(name)) {
            return true;
        }
        for (String pkgName : allowedPackages) {
            if (name.startsWith(pkgName)) {
                return true;
            }
        }
        return false;
    }

}
