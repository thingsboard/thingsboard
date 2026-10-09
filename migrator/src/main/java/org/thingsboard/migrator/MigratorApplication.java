// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.thingsboard.migrator.utils.Storage;

import java.util.List;

@SpringBootApplication
public class MigratorApplication {

    public static void main(String[] args) {
        var ctx = SpringApplication.run(MigratorApplication.class, args);

        boolean exitAfterFinish = ctx.getEnvironment().getProperty("exit_after_finish", Boolean.class, true);
        if (exitAfterFinish) {
            int exitCode = SpringApplication.exit(ctx, () -> 0);
            System.exit(exitCode);
        }
    }

    @Bean
    public ApplicationRunner runner(List<MigrationService> migrationServices, Storage storage) {
        return args -> {
            storage.open();
            for (MigrationService migrationService : migrationServices) {
                migrationService.run();
            }
            storage.close();
        };
    }

}
