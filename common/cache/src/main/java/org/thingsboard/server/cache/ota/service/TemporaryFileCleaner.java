package org.thingsboard.server.cache.ota.service;

import lombok.extern.apachecommons.CommonsLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.OtaPackageId;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TemporaryFileCleaner {
    @Value("${java.io.tmpdir}/ota/")
    private String PATH;
    private final static long TEMPORARY_FILE_INACTIVITY_TIME = 900_000;
    private final ConcurrentMap<OtaPackageId, Long> lastActivityTimes = new ConcurrentHashMap<>();

    public void updateFileUsageStatus(OtaPackageId otaPackageId){
        lastActivityTimes.put(otaPackageId, System.currentTimeMillis());
    }

    @PostConstruct
    public void createScheduledTaskToClear() {
        createTempDirectoryIfNotExist();
        cleanDirectoryWithTemporaryFiles();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::deleteUnusedTemporaryFiles, 10, 10, TimeUnit.MINUTES);
    }

    private void createTempDirectoryIfNotExist() {
        File directory = new File(PATH);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    private void cleanDirectoryWithTemporaryFiles() {
        File directory = new File(PATH);
        if (directory.isDirectory()) {
            try {
                FileUtils.cleanDirectory(directory);
                log.info("Directory with temporary files cleaned");
            } catch (IOException e) {
                log.error("Failed to clean directory with temporary files", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void deleteUnusedTemporaryFiles() {
        long currentTime = System.currentTimeMillis();
        List<OtaPackageId> toBeDeleted = lastActivityTimes.entrySet()
                .stream()
                .filter(entry -> currentTime - entry.getValue() > TEMPORARY_FILE_INACTIVITY_TIME)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        try {
            toBeDeleted.forEach(otaId -> {
                deleteFile(otaId.getId().toString());
                lastActivityTimes.remove(otaId);
            });
        } catch (Exception e) {
            log.error("Failed to delete unused files", e);
        }
        log.info("Deleted {} unused temporary files", toBeDeleted.size());
    }

    private void deleteFile(String otaId) {
        String fileName = PATH + otaId;
        File file = new File(fileName);
        try {
            if (file.exists()) {
                FileUtils.delete(file);
                log.info("System file {} was deleted", file.getName());
            }
        } catch (IOException e) {
            log.error("Failed to delete file {}", file.getName(), e);
        }
    }
}
