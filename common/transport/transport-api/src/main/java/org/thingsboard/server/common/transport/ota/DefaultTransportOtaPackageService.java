/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.ota;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.ota.files.OtaFileState;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbTransportComponent;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

import static org.thingsboard.server.common.data.CacheConstants.OTA_PACKAGE_DATA_CACHE;

@Slf4j
@Service
@TbTransportComponent
@RequiredArgsConstructor
public class DefaultTransportOtaPackageService implements TransportOtaPackageService {

    private final static String FILE_NAME_TEMPLATE = "%s.tmp";

    @Value("${files.temporary_files_directory:}")
    private String tmpDir;
    @Value("${java.io.tmpdir}")
    private String defaultTmpDir;

    private final ConcurrentMap<OtaPackageId, TransportOtaFileState> files = new ConcurrentHashMap<>();
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TransportService transportService;

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(tmpDir)) {
            tmpDir = defaultTmpDir;
        }
        createTempDirectoryIfNotExist();
        cleanDirectory();
        log.info("Directory {} with temporary ota files cleaned", tmpDir);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ListenableFuture<byte[]> get(TenantId tenantId, OtaPackageId otaId, int chunkSize, int chunk) {
        var state = files.computeIfAbsent(otaId, tmp ->
                new TransportOtaFileState(tenantId, otaId, Paths.get(tmpDir, "tb-transport", "ota",
                        String.format(FILE_NAME_TEMPLATE, otaId.getId().toString()))));
        Lock lock = state.getLock();
        lock.lock();
        try {
            if (state.isLoaded() && state.exists()) {
                return readChunk(state, chunkSize, chunk);
            }
            if (state.getLoadFuture() == null) {
                state.setLoaded(false);
                state.setLoadFuture(requestFile(state));
            }

            state.updateLastActivityTime();
            return Futures.transformAsync(state.getLoadFuture(), tmp -> readChunk(state, chunkSize, chunk), transportService.getCallbackExecutor());
        } finally {
            lock.unlock();
        }
    }

    private SettableFuture<Void> requestFile(TransportOtaFileState state) {
        SettableFuture<Void> result = SettableFuture.create();
        transportService.process(TransportProtos.SendOtaPackageBodyRequestMsg.newBuilder()
                .setTenantIdMSB(state.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(state.getTenantId().getId().getLeastSignificantBits())
                .setOtaPackageIdMSB(state.getOtaId().getId().getMostSignificantBits())
                .setOtaPackageIdMSB(state.getOtaId().getId().getLeastSignificantBits())
                .setServiceId(serviceInfoProvider.getServiceId())
                .build(), new TransportServiceCallback<>() {
            @Override
            public void onSuccess(TransportProtos.SendOtaPackageBodyResponseMsg msg) {
                if (msg.getResponseStatus() == TransportProtos.ResponseStatus.SUCCESS) {
                    log.debug("[{}] Requested ota file content", state.getOtaId());
                } else {
                    log.warn("[{}] Failed to request ota file content due to: {}", state.getOtaId(), msg.getResponseStatus());
                    state.getLoadFuture().setException(new RuntimeException("Failed to request ota file content due to: " + msg.getResponseStatus()));
                }

            }

            @Override
            public void onError(Throwable e) {
                log.warn("[{}] Failed to request ota file content", state.getOtaId(), e);
                state.getLoadFuture().setException(e);
            }
        });
        return result;
    }

    private ListenableFuture<byte[]> readChunk(TransportOtaFileState state, int chunkSize, int chunk) {
        //TODO: implement;
//        byte[] data = cacheManager.getCache(OTA_PACKAGE_DATA_CACHE).get(key, byte[].class);
//
//        if (chunkSize < 1) {
//            return data;
//        }
//
//        if (data != null && data.length > 0) {
//            int startIndex = chunkSize * chunk;
//
//            int size = Math.min(data.length - startIndex, chunkSize);
//
//            if (startIndex < data.length && size > 0) {
//                byte[] result = new byte[size];
//                System.arraycopy(data, startIndex, result, 0, size);
//                return result;
//            }
//        }
//        return new byte[0];

        return Futures.immediateFuture(null);
    }

    private void createTempDirectoryIfNotExist() {
        File directory = Paths.get(tmpDir, "tb-transport", "ota").toFile();
        if (!directory.exists()) {
            try {
                FileUtils.forceMkdir(directory);
            } catch (IOException e) {
                log.error("Failed to create directory for temporary files ", e);
            }
        }
    }

    private void cleanDirectory() {
        File directory = Paths.get(tmpDir, "tb-transport", "ota").toFile();
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files == null) return;
            Arrays.stream(files).forEach(
                    file -> {
                        try {
                            FileUtils.delete(file);
                        } catch (Exception e) {
                            log.error("Failed to delete file {}", file.getName(), e);
                        }
                    }
            );
        }
    }
}
