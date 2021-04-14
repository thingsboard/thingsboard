/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.client.tools.migrator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DictionaryParser {
    private boolean isKeyFirst;
    private Pattern pattern = Pattern.compile(".*[(](.*)[)].*");
    private final Map<String, String> dictionaryParsed = new HashMap<>();

    public DictionaryParser(File sourceFile) throws IOException {
        parseDictionaryDump(FileUtils.lineIterator(sourceFile));
    }

    public String getKeyByKeyId(String keyId) {
        return dictionaryParsed.get(keyId);
    }

    private boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }

    private boolean isBlockStarted(String line) {
        return line.startsWith("COPY public.ts_kv_dictionary (");
    }

    private void parseDictionaryDump(LineIterator iterator) {
        try {
            String tempLine;
            while (iterator.hasNext()) {
                tempLine = iterator.nextLine();

                if (isBlockStarted(tempLine)) {
                    isKeyFirst(tempLine);
                    processBlock(iterator);
                }
            }
        } finally {
            iterator.close();
        }
    }

    private void processBlock(LineIterator lineIterator) {
        String tempLine;
        String[] lineSplit;
        while(lineIterator.hasNext()) {
            tempLine = lineIterator.nextLine();
            if(isBlockFinished(tempLine)) {
                return;
            }

            lineSplit = tempLine.split("\t");
            if(this.isKeyFirst) {
                dictionaryParsed.put(lineSplit[1], lineSplit[0]);
            } else {
                dictionaryParsed.put(lineSplit[0], lineSplit[1]);
            }
        }
    }

    private void isKeyFirst(String startOfBlock) {
        Matcher matcher = pattern.matcher(startOfBlock);
        if(matcher.find()) {
            String[] splitBlockParams = matcher.group(1).split(",");
            Arrays.stream(splitBlockParams).forEach(line -> line = line.trim());
            this.isKeyFirst = splitBlockParams[0].equals("key");
            return;
        }
        throw new RuntimeException("Cant process signature of ts_kv_dictionary table.");
    }
}
