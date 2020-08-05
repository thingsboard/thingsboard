/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.client.tools.migrator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeyDictionaryReader {

    private static final long LOG_BATCH = 1000000;

    private static long linesProcessed = 0;
    private static long linesMigrated = 0;

    public static Map<Long, String> read(File sourceFile) throws IOException {
        Map<Long, String> keyDictionary = new HashMap<>();
        long startTs = System.currentTimeMillis();
        long stepLineTs = System.currentTimeMillis();
        long stepOkLineTs = System.currentTimeMillis();
        LineIterator iterator = FileUtils.lineIterator(sourceFile);

        boolean isBlockStarted = false;
        boolean isBlockFinished = false;

        String line;
        while (iterator.hasNext()) {
            if (linesProcessed++ % LOG_BATCH == 0) {
                System.out.println(new Date() + " linesProcessed = " + linesProcessed + " in " + (System.currentTimeMillis() - stepLineTs));
                stepLineTs = System.currentTimeMillis();
            }

            line = iterator.nextLine();

            if (isBlockFinished) {
                break;
            }

            if (!isBlockStarted) {
                if (isBlockStarted(line)) {
                    System.out.println();
                    System.out.println();
                    System.out.println(line);
                    System.out.println();
                    System.out.println();
                    isBlockStarted = true;
                }
                continue;
            }

            if (isBlockFinished(line)) {
                isBlockFinished = true;
            } else {
                try {
                    List<String> raw = Arrays.stream(line.trim().split("\t"))
                            .map(String::trim)
                            .filter(StringUtils::isNotEmpty)
                            .collect(Collectors.toList());

                    addToDictionary(raw, keyDictionary);

                    if (linesMigrated++ % LOG_BATCH == 0) {
                        System.out.println(new Date() + " migrated = " + linesMigrated + " in " + (System.currentTimeMillis() - stepOkLineTs));
                        stepOkLineTs = System.currentTimeMillis();
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage() + " -> " + line);
                }

            }
        }

        long endTs = System.currentTimeMillis();
        System.out.println();
        System.out.println(new Date() + " Dictionary processed: rows " + linesMigrated + " in " + (endTs - startTs));

        System.out.println();
        System.out.println("Finished key dictionary read: " + keyDictionary.size());

        return keyDictionary;
    }

    private static void addToDictionary(List<String> raw, Map<Long, String> dictionary) {
        String key = raw.get(0);
        Long keyId = Long.parseLong(raw.get(1));
        dictionary.put(keyId, key);
    }

    private static boolean isBlockStarted(String line) {
        return line.startsWith("COPY public.ts_kv_dictionary");
    }

    private static boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }
}
