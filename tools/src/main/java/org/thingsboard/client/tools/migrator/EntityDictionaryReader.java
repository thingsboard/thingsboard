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
import java.util.UUID;
import java.util.stream.Collectors;

public class EntityDictionaryReader {


    private static final String UNKNOWN_BLOCK = "unknown";
    private static final String ASSET_BLOCK = "ASSET";
    private static final String DEVICE_BLOCK = "DEVICE";
    private static final String INTERGRATION_BLOCK = "INTEGRATION";
    private static final String CUSTOMER_BLOCK = "CUSTOMER";
    private static final String USER_BLOCK = "USER";
    private static final String TENANT_BLOCK = "TENANT";

    private static final long LOG_BATCH = 1000000;

    private static long linesProcessed = 0;
    private static long linesMigrated = 0;

    public static Map<UUID, String> read(File sourceFile) throws IOException {
        Map<UUID, String> entityDictionary = new HashMap<>();
        long startTs = System.currentTimeMillis();
        long stepLineTs = System.currentTimeMillis();
        long stepOkLineTs = System.currentTimeMillis();
        LineIterator iterator = FileUtils.lineIterator(sourceFile);

        String currentBlock = UNKNOWN_BLOCK;
        boolean isBlockStarted = false;
        boolean isBlockFinished = false;
        long blockCount = 0;

        String line;
        while (iterator.hasNext()) {
            if (linesProcessed++ % LOG_BATCH == 0) {
                System.out.println(new Date() + " linesProcessed = " + linesProcessed + " in " + (System.currentTimeMillis() - stepLineTs));
                stepLineTs = System.currentTimeMillis();
            }

            line = iterator.nextLine();

//            if (isBlockFinished) {
//                break;
//            }

            if (!isBlockStarted) {
                currentBlock = getCurrentBlock(line);
                if (!currentBlock.equals(UNKNOWN_BLOCK)) {
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
                isBlockStarted = false;
                System.out.println("Found " + blockCount + " entities for block " + currentBlock);
                blockCount = 0;
            } else {
                try {
                    List<String> raw = Arrays.stream(line.trim().split("\t"))
                            .map(String::trim)
                            .filter(StringUtils::isNotEmpty)
                            .collect(Collectors.toList());

                    addToDictionary(raw, entityDictionary, currentBlock);
                    blockCount++;

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
        System.out.println("Finished entity dictionary read: " + entityDictionary.size());

        return entityDictionary;
    }

    private static void addToDictionary(List<String> raw, Map<UUID, String> dictionary, String currentType) {
        UUID uuid = fromString(raw.get(0));
        dictionary.put(uuid, currentType);
    }

    public static UUID fromString(String src) {
        return UUID.fromString(src.substring(7, 15) + "-" + src.substring(3, 7) + "-1"
                + src.substring(0, 3) + "-" + src.substring(15, 19) + "-" + src.substring(19));
    }

    private static String getCurrentBlock(String line) {
        if (line.startsWith("COPY public.asset")) {
            return ASSET_BLOCK;
        } else if (line.startsWith("COPY public.device")) {
            return DEVICE_BLOCK;
        } else if (line.startsWith("COPY public.integration")) {
            return INTERGRATION_BLOCK;
        } else if (line.startsWith("COPY public.customer")) {
            return CUSTOMER_BLOCK;
        } else if (line.startsWith("COPY public.tb_user")) {
            return USER_BLOCK;
        } else if (line.startsWith("COPY public.tenant")) {
            return TENANT_BLOCK;
        }
        return UNKNOWN_BLOCK;
    }

    private static boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }
}
