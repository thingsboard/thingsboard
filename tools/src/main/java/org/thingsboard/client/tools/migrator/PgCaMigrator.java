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
import java.util.function.Function;
import java.util.stream.Collectors;

public class PgCaMigrator {

    private final long LOG_BATCH = 1000000;
    private final long rowPerFile = 1000000;

    private long linesTsMigrated = 0;
    private long linesLatestMigrated = 0;
    private long castErrors = 0;
    private long castedOk = 0;

    private long currentWriterCount = 1;

    private boolean willLatestMigrate;
    private boolean willTsMigrate;

    private final File sourceFile;
    private final boolean castStringIfPossible;

    private final RelatedEntitiesParser entityIdsAndTypes;
    private final DictionaryParser keyParser;
    private CQLSSTableWriter currentTsWriter;
    private CQLSSTableWriter currentPartitionsWriter;
    private CQLSSTableWriter currentTsLatestWriter;
    private final Set<List<Object>> partitions = new HashSet<>();

    private File outTsDir;
    private File outTsLatestDir;

    public PgCaMigrator(File sourceFile,
                        File ourTsDir,
                        File outTsPartitionDir,
                        File outTsLatestDir,
                        RelatedEntitiesParser allEntityIdsAndTypes,
                        DictionaryParser dictionaryParser,
                        boolean castStringsIfPossible) {
        this.sourceFile = sourceFile;
        this.entityIdsAndTypes = allEntityIdsAndTypes;
        this.keyParser = dictionaryParser;
        this.castStringIfPossible = castStringsIfPossible;
        if(outTsLatestDir != null) {
            this.currentTsLatestWriter = WriterBuilder.getLatestWriter(outTsLatestDir);
            this.outTsLatestDir = outTsLatestDir;
            this.willLatestMigrate = true;
        }
        if(ourTsDir != null) {
            this.currentTsWriter = WriterBuilder.getTsWriter(ourTsDir);
            this.currentPartitionsWriter = WriterBuilder.getPartitionWriter(outTsPartitionDir);
            this.outTsDir = ourTsDir;
            this.willTsMigrate = true;
        }
    }

    public void migrate() throws IOException {
        boolean isTsDone = false;
        boolean isLatestDone = false;
        String line;
        LineIterator iterator = FileUtils.lineIterator(this.sourceFile);

        try {
            while(iterator.hasNext()) {
                line = iterator.nextLine();
                if(isLatestDone && isTsDone) break;
                if(willLatestMigrate && !isLatestDone && isBlockLatestStarted(line)) {
                    System.out.println("START TO MIGRATE LATEST");
                    long start = System.currentTimeMillis();
                    processBlock(iterator, currentTsLatestWriter, WriterBuilder::getLatestWriter, outTsLatestDir, this::toValuesLatest);
                    System.out.println("TOTAL LINES MIGRATED: " + linesLatestMigrated + ", FORMING OF SSL FOR LATEST TS FINISHED WITH TIME: " + (System.currentTimeMillis() - start) + " ms.");
                    isLatestDone = true;
                }

                if(willTsMigrate && !isTsDone && isBlockTsStarted(line)) {
                    System.out.println("START TO MIGRATE TS");
                    long start = System.currentTimeMillis();
                    processBlock(iterator, currentTsWriter, WriterBuilder::getTsWriter, outTsDir, this::toValuesTs);
                    System.out.println("TOTAL LINES MIGRATED: " + linesTsMigrated + ", FORMING OF SSL FOR TS FINISHED WITH TIME: " + (System.currentTimeMillis() - start) + " ms.");
                    isTsDone = true;
                }
            }

            if(willTsMigrate) {
                System.out.println("Partitions collected " + partitions.size());
                long startTs = System.currentTimeMillis();
                for (var partition : partitions) {
                    currentPartitionsWriter.addRow(partition);
                }

                System.out.println(new Date() + " Migrated partitions " + partitions.size() + " in " + (System.currentTimeMillis() - startTs) + "(ms)");
            }

            System.out.println();
            System.out.println("Finished migrate Telemetry");

        } finally {
            iterator.close();
            if(willLatestMigrate) {
                currentTsLatestWriter.close();
            }
            if(willTsMigrate) {
                currentTsWriter.close();
                currentPartitionsWriter.close();
            }
        }
    }

    private void logLinesProcessed(long lines) {
        if (lines % LOG_BATCH == 0) {
            System.out.println(new Date() + " lines processed = " + lines + " in, castOk " + castedOk + "  castErr " + castErrors);
        }
    }

    private void logLinesMigrated(long lines) {
        if(lines % LOG_BATCH == 0) {
            System.out.println(new Date() + " lines migrated = " + lines + " in, castOk " + castedOk + "  castErr " + castErrors);
        }
    }

    private void addTypeIdKey(List<Object> result, List<String> raw) {
        result.add(entityIdsAndTypes.getEntityType(raw.get(0)));
        result.add(UUID.fromString(raw.get(0)));
        result.add(keyParser.getKeyByKeyId(raw.get(1)));
    }

    private void addPartitions(List<Object> result, List<String> raw) {
        long ts = Long.parseLong(raw.get(2));
        long partition = toPartitionTs(ts);
        result.add(partition);
        result.add(ts);
    }

    private void addTimeseries(List<Object> result, List<String> raw) {
        result.add(Long.parseLong(raw.get(2)));
    }

    private void addValues(List<Object> result, List<String> raw) {
        result.add(raw.get(3).equals("\\N") ? null : raw.get(3).equals("t") ? Boolean.TRUE : Boolean.FALSE);
        result.add(raw.get(4).equals("\\N") ? null : raw.get(4));
        result.add(raw.get(5).equals("\\N") ? null : Long.parseLong(raw.get(5)));
        result.add(raw.get(6).equals("\\N") ? null : Double.parseDouble(raw.get(6)));
        result.add(raw.get(7).equals("\\N") ? null : raw.get(7));
    }

    private List<Object> toValuesTs(List<String> raw) {

        logLinesMigrated(linesTsMigrated++);

        List<Object> result = new ArrayList<>();

        if(entityIdsAndTypes.getEntityType(raw.get(0)) == null) {
            return null;
        }
        addTypeIdKey(result, raw);
        addPartitions(result, raw);
        addValues(result, raw);

        processPartitions(result);

        return result;
    }

    private List<Object> toValuesLatest(List<String> raw) {
        logLinesMigrated(linesLatestMigrated++);
        List<Object> result = new ArrayList<>();

        addTypeIdKey(result, raw);
        addTimeseries(result, raw);
        addValues(result, raw);

        return result;
    }

    private long toPartitionTs(long ts) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
        return time.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private void processPartitions(List<Object> values) {
        partitions.add(List.of(values.get(0),
                values.get(1),
                values.get(2),
                values.get(4)));
    }

    private void processBlock(LineIterator iterator, CQLSSTableWriter writer, Function<File, CQLSSTableWriter> writerCreator, File outDir, Function<List<String>, List<Object>> function) {
        String currentLine;
        long linesProcessed = 0;
        while(iterator.hasNext()) {
            logLinesProcessed(linesProcessed++);
            currentLine = iterator.nextLine();
            if(isBlockFinished(currentLine)) {
                return;
            }

            try {
                List<String> raw = Arrays.stream(currentLine.trim().split("\t"))
                        .map(String::trim)
                        .collect(Collectors.toList());
                List<Object> values = function.apply(raw);

                if (this.currentWriterCount == 0) {
                    System.out.println(new Date() + " close writer " + new Date());
                    writer.close();
                    writer = writerCreator.apply(outDir);
                }

                if(values != null) {
                    if (this.castStringIfPossible) {
                        writer.addRow(castToNumericIfPossible(values));
                    } else {
                        writer.addRow(values);
                    }
                }

                currentWriterCount++;
                if (currentWriterCount >= rowPerFile) {
                    currentWriterCount = 0;
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage() + " -> " + currentLine);
                ex.printStackTrace();
                System.out.println("current writer: " + outDir.getAbsolutePath());
            }
        }
    }

    private List<Object> castToNumericIfPossible(List<Object> values) {
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

        processPartitions(values);

        return values;
    }

    private boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }

    private boolean isBlockTsStarted(String line) {
        return line.startsWith("COPY public.ts_kv (");
    }

    private boolean isBlockLatestStarted(String line) {
        return line.startsWith("COPY public.ts_kv_latest (");
    }

}
