// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.converter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.LibraryConvertersInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.GitSyncService;
import org.thingsboard.server.service.sync.vc.GitRepository.FileType;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.converter.ConverterType.DOWNLINK;
import static org.thingsboard.server.common.data.converter.ConverterType.UPLINK;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultConverterLibraryService implements ConverterLibraryService {

    private final GitSyncService gitSyncService;

    @Value("${integrations.converters.library.enabled:true}")
    private boolean enabled;
    @Value("${integrations.converters.library.url:https://github.com/thingsboard/data-converters.git}")
    private String repoUrl;
    @Value("${integrations.converters.library.branch}")
    private String branch;
    @Value("${integrations.converters.library.fetch_frequency:24}")
    private int fetchFrequencyHours;

    private Map<String, LibraryConvertersInfo> convertersInfo;

    private static final String REPO_KEY = "data-converters-library";

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() throws Exception {
        if (!enabled) {
            return;
        }
        gitSyncService.registerSync(REPO_KEY, repoUrl, branch, TimeUnit.HOURS.toMillis(fetchFrequencyHours), this::updateConvertersInfo);
    }

    @Override
    public List<Vendor> getVendors(IntegrationType integrationType, String converterType, int page, int pageSize, boolean loadImages) {
        log.trace("Executing getVendors [{}]", integrationType);
        return listFiles("VENDORS", 1, true).stream()
                .filter(vendorDir -> {
                    Set<String> integrationTypes = listFiles(vendorDir.path(), 3, true).stream()
                            .filter(integrationDir -> hasConverter(integrationDir, integrationType, converterType))
                            .map(RepoFile::name)
                            .collect(Collectors.toSet());
                    return integrationTypes.contains(integrationType.getDirectory());
                })
                .sorted(Comparator.comparing(RepoFile::name))
                .skip((long) page * pageSize)
                .limit(pageSize)
                .map(vendorDir -> {
                    String logo = null;
                    if (loadImages) {
                        String logoFile = findFile(vendorDir.path(), 2, "logo");
                        logo = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(gitSyncService.getFileContent(REPO_KEY, logoFile));
                    }
                    return new Vendor(vendorDir.name(), logo);
                })
                .toList();
    }

    @Override
    public List<Model> getVendorModels(IntegrationType integrationType, String converterType, String vendorName, int page, int pageSize, boolean loadImages) {
        log.trace("Executing getVendorModels [{}][{}][{}]", integrationType, converterType, vendorName);
        return listFiles("VENDORS/" + vendorName, 3, true).stream()
                .filter(integrationDir -> hasConverter(integrationDir, integrationType, converterType))
                .map(integrationDir -> Path.of(integrationDir.path()).getParent())
                .sorted(Comparator.comparing(Path::toString))
                .skip((long) page * pageSize)
                .limit(pageSize)
                .map(modelPath -> {
                    String name = modelPath.getFileName().toString();
                    JsonNode modelInfo = JacksonUtil.toJsonNode(getFileContent(modelPath + "/info.json"));
                    String photo = null;
                    if (loadImages) {
                        String photoFile = findFile(modelPath.toString(), 3, "photo");
                        photo = "data:image/png;base64," + Base64.getEncoder().encodeToString(gitSyncService.getFileContent(REPO_KEY, photoFile));
                    }
                    return new Model(name, modelInfo, photo);
                })
                .toList();
    }

    private boolean hasConverter(RepoFile integrationDir, IntegrationType integrationType, String converterType) {
        if (!integrationDir.name().equals(integrationType.getDirectory())) {
            return false;
        }
        if (StringUtils.isEmpty(converterType)) {
            return true;
        }
        return listFiles(integrationDir.path(), 4, true)
                .stream()
                .map(RepoFile::name)
                .anyMatch(name -> name.equals(converterType));
    }

    @Override
    public String getConverter(IntegrationType integrationType, String converterType, String vendorName, String model) {
        log.trace("Executing getConverter [{}][{}][{}][{}]", integrationType, converterType, vendorName, model);
        return getFileContent(getConverterDir(integrationType, converterType, vendorName, model) + "/converter.json");
    }

    @Override
    public String getConverterMetadata(IntegrationType integrationType, String converterType, String vendorName, String model) {
        log.trace("Executing getConverterMetadata [{}][{}][{}][{}]", integrationType, converterType, vendorName, model);
        return getFileContent(getConverterDir(integrationType, converterType, vendorName, model) + "/metadata.json");
    }

    @Override
    public String getPayload(IntegrationType integrationType, String converterType, String vendorName, String model) {
        log.trace("Executing getPayload [{}][{}][{}][{}]", integrationType, converterType, vendorName, model);
        List<RepoFile> payloadFiles = listFiles(getConverterDir(integrationType, converterType, vendorName, model), -1, false).stream()
                .filter(file -> file.name().startsWith("payload"))
                .sorted(Comparator.comparing(RepoFile::name))
                .toList();
        if (payloadFiles.isEmpty()) {
            return "{}";
        }

        RepoFile payloadFile = null;
        for (RepoFile file : payloadFiles) {
            if (file.name().endsWith("json")) { // preferring json payload
                payloadFile = file;
                break;
            }
        }
        if (payloadFile == null) {
            payloadFile = payloadFiles.get(0);
        }
        return getFileContent(payloadFile.path());
    }

    @Override
    public Map<String, LibraryConvertersInfo> getConvertersInfo() {
        return convertersInfo != null ? convertersInfo : Collections.emptyMap();
    }

    private String findFile(String dir, int depth, String prefix) {
        return listFiles(dir, depth, false).stream()
                .filter(file -> file.name().startsWith(prefix))
                .findFirst().map(RepoFile::path).orElse(null);
    }

    private List<RepoFile> listFiles(String path, int depth, boolean isDirectory) {
        if (!enabled) {
            throw new IllegalArgumentException("Data converters library is disabled");
        }
        return gitSyncService.listFiles(REPO_KEY, path, depth, isDirectory ? FileType.DIRECTORY : FileType.FILE);
    }

    private String getFileContent(String path) {
        if (!enabled) {
            throw new IllegalArgumentException("Data converters library is disabled");
        }
        try {
            return new String(gitSyncService.getFileContent(REPO_KEY, path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to get file content for path {}: {}", path, e.getMessage());
            return "{}";
        }
    }

    private String getConverterDir(IntegrationType integrationType, String converterType, String vendorName, String model) {
        return Path.of("VENDORS", vendorName, model, integrationType.getDirectory(), converterType).toString();
    }

    private void updateConvertersInfo() {
        log.debug("Updating converters info");
        convertersInfo = listFiles("VENDORS", 4, true).stream()
                .collect(Collectors.groupingBy(
                        repoFile -> Path.of(repoFile.path()).getParent().getFileName().toString(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                converters -> {
                                    boolean hasUplink = converters.stream()
                                            .anyMatch(converterPath -> UPLINK.getDirectory().equals(converterPath.name()));
                                    boolean hasDownlink = converters.stream()
                                            .anyMatch(converterPath -> DOWNLINK.getDirectory().equals(converterPath.name()));
                                    return new LibraryConvertersInfo(hasUplink, hasDownlink);
                                }
                        )
                ));
    }

}
