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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class JavaClassObject extends SimpleJavaFileObject {

    /**
     * Byte code created by the compiler will be stored in this
     * ByteArrayOutputStream so that we can later get the
     * byte array out of it
     * and put it in the memory as an instance of our class.
     */
    protected final ByteArrayOutputStream bos =
            new ByteArrayOutputStream();

    /**
     * Registers the compiled class object under URI
     * containing the class full name
     *
     * @param name
     *            Full name of the compiled class
     * @param kind
     *            Kind of the data. It will be CLASS in our case
     */
    public JavaClassObject(String name, Kind kind) {
        super(URI.create("bytes:///" + name.replace('.', '/')
                + kind.extension), kind);
    }

    /**
     * Will be used by our file manager to get the byte code that
     * can be put into memory to instantiate our class
     *
     * @return compiled byte code
     */
    public byte[] getBytes() {
        return bos.toByteArray();
    }

    /**
     * Will provide the compiler with an output stream that leads
     * to our byte array. This way the compiler will write everything
     * into the byte array that we will instantiate later
     */
    @Override
    public OutputStream openOutputStream() throws IOException {
        return bos;
    }
}
