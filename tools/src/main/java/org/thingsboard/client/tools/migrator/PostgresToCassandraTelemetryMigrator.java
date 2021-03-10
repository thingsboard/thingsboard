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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PostgresToCassandraTelemetryMigrator {

    private static final long LOG_BATCH = 1000000;
    private static final long rowPerFile = 1000000;

    private static long linesProcessed = 0;
    private static long linesMigrated = 0;
    private static long castErrors = 0;
    private static long castedOk = 0;

    private static long currentWriterCount = 1;
    private static CQLSSTableWriter currentTsWriter = null;
    private static CQLSSTableWriter currentPartitionWriter = null;

    private static Set<String> partitions = new HashSet<>();
    private static RelatedEntitiesParser entityIdsAndTypes;
    private static DictionaryParser keyParser;

    public static void migrateTs(File sourceFile,
                                 File outTsDir,
                                 File outPartitionDir,
                                 RelatedEntitiesParser allEntityIdsAndTypes,
                                 DictionaryParser dictionaryParser,
                                 boolean castStringsIfPossible) throws IOException {
        long startTs = System.currentTimeMillis();
        long stepLineTs = System.currentTimeMillis();
        long stepOkLineTs = System.currentTimeMillis();
        LineIterator iterator = FileUtils.lineIterator(sourceFile);
        currentTsWriter = WriterBuilder.getTsWriter(outTsDir);
        currentPartitionWriter = WriterBuilder.getPartitionWriter(outPartitionDir);
        entityIdsAndTypes = allEntityIdsAndTypes;
        keyParser = dictionaryParser;

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
                        currentTsWriter = WriterBuilder.getTsWriter(outTsDir);
                    }

                    if (castStringsIfPossible) {
                        currentTsWriter.addRow(castToNumericIfPossible(values));
                    } else {
                        currentTsWriter.addRow(values);
                    }
                    processPartitions(values);
                    currentWriterCount++;
                    if (currentWriterCount >= rowPerFile) {
                        currentWriterCount = 0;
                    }

                    if (linesMigrated++ % LOG_BATCH == 0) {
                        System.out.println(new Date() + " migrated = " + linesMigrated + " in " + (System.currentTimeMillis() - stepOkLineTs) + "   partitions = " + partitions.size());
                        stepOkLineTs = System.currentTimeMillis();
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage() + " -> " + line);
                }

            }
        }

        long endTs = System.currentTimeMillis();
        System.out.println();
        System.out.println(new Date() + " Migrated rows " + linesMigrated + " in " + (endTs - startTs));
        System.out.println("Partitions collected " + partitions.size());

        startTs = System.currentTimeMillis();
        for (String partition : partitions) {
            String[] split = partition.split("\\|");
            List<Object> values = Lists.newArrayList();
            values.add(split[0]);
            values.add(UUID.fromString(split[1]));
            values.add(split[2]);
            values.add(Long.parseLong(split[3]));
            currentPartitionWriter.addRow(values);
        }
        currentPartitionWriter.close();
        endTs = System.currentTimeMillis();
        System.out.println();
        System.out.println();
        System.out.println(new Date() + " Migrated partitions " + partitions.size() + " in " + (endTs - startTs));


        currentTsWriter.close();
        System.out.println();
        System.out.println("Finished migrate Telemetry");
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

    private static void processPartitions(List<Object> values) {
        String key = values.get(0) + "|" + values.get(1) + "|" + values.get(2) + "|" + values.get(3);
        partitions.add(key);
    }

    private static List<Object> toValues(List<String> raw) {
        //expected Table structure:
//               COPY public.ts_kv (entity_type, entity_id, key, ts, bool_v, str_v, long_v, dbl_v) FROM stdin;

        List<Object> result = new ArrayList<>();
        result.add(entityIdsAndTypes.getEntityType(raw.get(0)));
        result.add(UUID.fromString(raw.get(0)));
        result.add(keyParser.getKeyByKeyId(raw.get(1)));

        long ts = Long.parseLong(raw.get(2));
        long partition = toPartitionTs(ts);
        result.add(partition);
        result.add(ts);

        result.add(raw.get(3).equals("\\N") ? null : raw.get(3).equals("t") ? Boolean.TRUE : Boolean.FALSE);
        result.add(raw.get(4).equals("\\N") ? null : raw.get(4));
        result.add(raw.get(5).equals("\\N") ? null : Long.parseLong(raw.get(5)));
        result.add(raw.get(6).equals("\\N") ? null : Double.parseDouble(raw.get(6)));
        result.add(raw.get(7).equals("\\N") ? null : raw.get(7));
        return result;
    }

    private static long toPartitionTs(long ts) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
        return time.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant(ZoneOffset.UTC).toEpochMilli();
//        return TsPartitionDate.MONTHS.truncatedTo(time).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private static boolean isBlockStarted(String line) {
        return line.startsWith("COPY public.ts_kv (");
    }

    private static boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }

}
