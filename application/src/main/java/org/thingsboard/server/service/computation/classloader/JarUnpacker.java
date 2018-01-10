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
import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class JarUnpacker {
    private final JarFile jar;

    public JarUnpacker(JarFile jar){
        this.jar = jar;
    }

    public File extractJar() throws IOException {
        File parentDir = createJarDirectory();
        Enumeration en = jar.entries();
        while (en.hasMoreElements()) {
            JarEntry entry = (JarEntry)en.nextElement();
            String ent = entry.getName();
            int dir = ent.lastIndexOf(File.separator);
            if(dir == ent.length() - 1){
                String filePath = extractEntryPath(ent, dir);
                File local = new File(parentDir, filePath);
                if(!local.exists() && !local.mkdirs())
                    throw new IOException("Error while extracting jar, creating directory "+ filePath);
            }else{
                String pathname = extractEntryPath(ent, ent.length());
                File f = new File(parentDir, pathname);
                if(!f.exists() && !f.createNewFile()){
                    throw new IOException("Error while extracting jar, creating file "+ ent);
                }else{
                    writeFile(entry, f);
                }
            }
        }
        return parentDir;
    }

    private File createJarDirectory() throws IOException {
        String extractDirectory = jar.getName().substring(0, jar.getName().lastIndexOf(".jar"));
        File dir = new File(extractDirectory);
        if(!dir.exists() && !dir.mkdirs()){
            throw new IOException("Error while creating parent directory for jar");
        }
        return dir;
    }

    private String extractEntryPath(String entryName, int endIndex){
        int beginIndex = 0;
        if(entryName.startsWith(File.separator)){
            beginIndex = 1;
        }
        return entryName.substring(beginIndex, endIndex);
    }

    private void writeFile(JarEntry entry, File toWrite)throws IOException {
        if (entry != null) {
            InputStream entryStream = new BufferedInputStream(jar.getInputStream(entry));

            try(OutputStream file = new BufferedOutputStream(new FileOutputStream(toWrite))) {
                byte[] buffer = new byte[2048];
                int bytesRead;
                while ((bytesRead = entryStream.read(buffer)) != -1) {
                    file.write(buffer, 0, bytesRead);
                }
            } finally {
                entryStream.close();
            }
        }
        else {
            throw new IOException("Entry name is not found in jar");
        }
    }
}
