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

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;

public class MigratorTool {

    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);


        try {
            File latestSource = new File(cmd.getOptionValue("latestTelemetryFrom"));
            File latestSaveDir = new File(cmd.getOptionValue("latestTelemetryOut"));
            File tsSource = new File(cmd.getOptionValue("telemetryFrom"));
            File tsSaveDir = new File(cmd.getOptionValue("telemetryOut"));
            File partitionsSaveDir = new File(cmd.getOptionValue("partitionsOut"));
            boolean castEnable = Boolean.parseBoolean(cmd.getOptionValue("castEnable"));

            PgCaLatestMigrator.migrateLatest(latestSource, latestSaveDir, castEnable);
            PostgresToCassandraTelemetryMigrator.migrateTs(tsSource, tsSaveDir, partitionsSaveDir, castEnable);

        } catch (Throwable th) {
            th.printStackTrace();
            throw new IllegalStateException("failed", th);
        }

    }

    private static CommandLine parseArgs(String[] args) {
        Options options = new Options();

        Option latestTsOpt = new Option("latestFrom", "latestTelemetryFrom", true, "latest telemetry source file path");
        latestTsOpt.setRequired(true);
        options.addOption(latestTsOpt);

        Option latestTsOutOpt = new Option("latestOut", "latestTelemetryOut", true, "latest telemetry save dir");
        latestTsOutOpt.setRequired(true);
        options.addOption(latestTsOutOpt);

        Option tsOpt = new Option("tsFrom", "telemetryFrom", true, "telemetry source file path");
        tsOpt.setRequired(true);
        options.addOption(tsOpt);

        Option tsOutOpt = new Option("tsOut", "telemetryOut", true, "sstable save dir");
        tsOutOpt.setRequired(true);
        options.addOption(tsOutOpt);

        Option partitionOutOpt = new Option("partitionsOut", "partitionsOut", true, "partitions save dir");
        partitionOutOpt.setRequired(true);
        options.addOption(partitionOutOpt);

        Option castOpt = new Option("castEnable", "castEnable", true, "cast String to Double if possible");
        castOpt.setRequired(true);
        options.addOption(castOpt);

        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new BasicParser();

        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }
        return null;
    }

}
