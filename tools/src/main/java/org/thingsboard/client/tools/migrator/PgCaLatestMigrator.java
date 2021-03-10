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

import com.google.common.collect.Lists;
import org.apache.cassandra.io.sstable.CQLSSTableWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PgCaLatestMigrator {

    private static final long LOG_BATCH = 1000000;
    private static final long rowPerFile = 1000000;


    private static long linesProcessed = 0;
    private static long linesMigrated = 0;
    private static long castErrors = 0;
    private static long castedOk = 0;

    private static long currentWriterCount = 1;
    private static RelatedEntitiesParser allIdsAndTypes;
    private static DictionaryParser keyPairs;

    public static void migrateLatest(File sourceFile,
                                     File outDir,
                                     RelatedEntitiesParser allEntityIdsAndTypes,
                                     DictionaryParser dictionaryParser,
                                     boolean castStringsIfPossible) throws IOException {
        long startTs = System.currentTimeMillis();
        long stepLineTs = System.currentTimeMillis();
        long stepOkLineTs = System.currentTimeMillis();
        LineIterator iterator = FileUtils.lineIterator(sourceFile);
        CQLSSTableWriter currentTsWriter = WriterBuilder.getLatestWriter(outDir);
        allIdsAndTypes = allEntityIdsAndTypes;
        keyPairs = dictionaryParser;

        boolean isBlockStarted = false;
        boolean isBlockFinished = false;

        String line;
        while (iterator.hasNext()) {
            if (linesProcessed++ % LOG_BATCH == 0) {
                System.out.println(new Date() + " linesProcessed = " + linesProcessed + " in " + (System.currentTimeMillis() - stepLineTs) + "   castOk " + castedOk + "  castErr " + castErrors);
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
                    List<Object> values = toValues(raw);

                    if (currentWriterCount == 0) {
                        System.out.println(new Date() + " close writer " + new Date());
                        currentTsWriter.close();
                        currentTsWriter = WriterBuilder.getLatestWriter(outDir);
                    }

                    if (castStringsIfPossible) {
                        currentTsWriter.addRow(castToNumericIfPossible(values));
                    } else {
                        currentTsWriter.addRow(values);
                    }
                    currentWriterCount++;
                    if (currentWriterCount >= rowPerFile) {
                        currentWriterCount = 0;
                    }

                    if (linesMigrated++ % LOG_BATCH == 0) {
                        System.out.println(new Date() + " migrated = " + linesMigrated + " in " + (System.currentTimeMillis() - stepOkLineTs) + " ms.");
                        stepOkLineTs = System.currentTimeMillis();
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage() + " -> " + line);
                }

            }
        }

        long endTs = System.currentTimeMillis();
        System.out.println();
        System.out.println(new Date() + " Migrated rows " + linesMigrated + " in " + (endTs - startTs) + " ts");

        currentTsWriter.close();
        System.out.println();
        System.out.println("Finished migrate Latest Telemetry");
    }


    private static List<Object> castToNumericIfPossible(List<Object> values) {
        try {
            if (values.get(6) != null && NumberUtils.isNumber(values.get(6).toString())) {
                Double casted = NumberUtils.createDouble(values.get(6).toString());
                List<Object> numeric = Lists.newArrayList();
                numeric.addAll(values);
                numeric.set(6, null);
                numeric.set(8, casted);
                castedOk++;
                return numeric;
            }
        } catch (Throwable th) {
            castErrors++;
        }
        return values;
    }

    private static List<Object> toValues(List<String> raw) {
        //expected Table structure:
        //COPY public.ts_kv_latest (entity_type, entity_id, key, ts, bool_v, str_v, long_v, dbl_v) FROM stdin;

        List<Object> result = new ArrayList<>();
        result.add(allIdsAndTypes.getEntityType(raw.get(0)));
        result.add(UUID.fromString(raw.get(0)));
        result.add(keyPairs.getKeyByKeyId(raw.get(1)));

        long ts = Long.parseLong(raw.get(2));
        result.add(3, ts);

        result.add(raw.get(3).equals("\\N") ? null : raw.get(3).equals("t") ? Boolean.TRUE : Boolean.FALSE);
        result.add(raw.get(4).equals("\\N") ? null : raw.get(4));
        result.add(raw.get(5).equals("\\N") ? null : Long.parseLong(raw.get(5)));
        result.add(raw.get(6).equals("\\N") ? null : Double.parseDouble(raw.get(6)));
        result.add(raw.get(7).equals("\\N") ? null : raw.get(7));

        return result;
    }

    private static boolean isBlockStarted(String line) {
        return line.startsWith("COPY public.ts_kv_latest (");
    }

    private static boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }

}
