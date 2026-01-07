/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.api.NodeDefinition;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode;
import org.thingsboard.rule.engine.filter.TbOriginatorTypeSwitchNode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.component.ComponentDescriptorService;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnnotationComponentDiscoveryService implements ComponentDiscoveryService {

    public static final int MAX_OPTIMISITC_RETRIES = 3;

    @Value("${plugins.scan_packages}")
    private String[] scanPackages;

    @Autowired
    private Environment environment;

    @Autowired
    private ComponentDescriptorService componentDescriptorService;

    private final Map<String, RuleNodeClassInfo> ruleNodeClasses = new HashMap<>();

    private final Map<String, ComponentDescriptor> components = new HashMap<>();

    private final Map<ComponentType, List<ComponentDescriptor>> coreComponentsMap = new HashMap<>();

    private final Map<ComponentType, List<ComponentDescriptor>> edgeComponentsMap = new HashMap<>();

    private boolean isInstall() {
        return environment.acceptsProfiles(Profiles.of("install"));
    }

    @PostConstruct
    public void init() {
        for (var def : discoverBeansByAnnotationType(RuleNode.class)) {
            String clazzName = def.getBeanClassName();
            try {
                var clazz = Class.forName(clazzName);
                RuleNode annotation = clazz.getAnnotation(RuleNode.class);
                ruleNodeClasses.put(clazzName, new RuleNodeClassInfo(clazz, annotation));
            } catch (Exception e) {
                log.warn("Failed to create instance of rule node type: {} due to: ", clazzName, e);
            }
        }
        if (!isInstall()) {
            discoverComponents();
        }
    }

    private Set<BeanDefinition> discoverBeansByAnnotationType(Class<? extends Annotation> annotationType) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));
        Set<BeanDefinition> defs = new HashSet<>();
        for (String scanPackage : scanPackages) {
            defs.addAll(scanner.findCandidateComponents(scanPackage));
        }
        return defs;
    }

    @Override
    public Optional<RuleNodeClassInfo> getRuleNodeInfo(String clazz) {
        return Optional.ofNullable(ruleNodeClasses.get(clazz));
    }

    @Override
    public List<RuleNodeClassInfo> getVersionedNodes() {
        return ruleNodeClasses.values().stream().filter(RuleNodeClassInfo::isVersioned).collect(Collectors.toList());
    }

    private void registerRuleNodeComponents() {
        for (RuleNodeClassInfo def : ruleNodeClasses.values()) {
            int retryCount = 0;
            Exception cause = null;
            while (retryCount < MAX_OPTIMISITC_RETRIES) {
                try {
                    ComponentType type = def.getAnnotation().type();
                    ComponentDescriptor component = scanAndPersistComponent(def, type);
                    components.put(component.getClazz(), component);
                    putComponentIntoMaps(type, def.getAnnotation(), component);
                    break;
                } catch (Exception e) {
                    log.trace("Can't initialize component {}, due to {}", def.getClassName(), e.getMessage(), e);
                    cause = e;
                    retryCount++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            }
            if (cause != null && retryCount == MAX_OPTIMISITC_RETRIES) {
                log.error("Can't initialize component {}, due to {}", def.getClassName(), cause.getMessage(), cause);
                throw new RuntimeException(cause);
            }
        }
    }

    private void putComponentIntoMaps(ComponentType type, RuleNode ruleNodeAnnotation, ComponentDescriptor component) {
        boolean ruleChainTypesMethodAvailable;
        try {
            ruleNodeAnnotation.getClass().getMethod("ruleChainTypes");
            ruleChainTypesMethodAvailable = true;
        } catch (NoSuchMethodException exception) {
            log.warn("[{}] does not have ruleChainTypes. Probably extension class compiled before 3.3 release. " +
                    "Please update your extensions and compile using latest 3.3 release dependency", ruleNodeAnnotation.name());
            ruleChainTypesMethodAvailable = false;
        }
        if (ruleChainTypesMethodAvailable) {
            if (ruleChainTypeContainsArray(RuleChainType.CORE, ruleNodeAnnotation.ruleChainTypes())) {
                coreComponentsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
            }
            if (ruleChainTypeContainsArray(RuleChainType.EDGE, ruleNodeAnnotation.ruleChainTypes())) {
                edgeComponentsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
            }
        } else {
            coreComponentsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
        }
    }

    private boolean ruleChainTypeContainsArray(RuleChainType ruleChainType, RuleChainType[] array) {
        for (RuleChainType tmp : array) {
            if (ruleChainType.equals(tmp)) {
                return true;
            }
        }
        return false;
    }

    private ComponentDescriptor scanAndPersistComponent(RuleNodeClassInfo def, ComponentType type) {
        ComponentDescriptor scannedComponent = new ComponentDescriptor();
        String clazzName = def.getClassName();
        try {
            scannedComponent.setType(type);
            Class<?> clazz = def.getClazz();
            RuleNode ruleNodeAnnotation = clazz.getAnnotation(RuleNode.class);
            scannedComponent.setConfigurationVersion(def.getCurrentVersion());
            scannedComponent.setName(ruleNodeAnnotation.name());
            scannedComponent.setScope(ruleNodeAnnotation.scope());
            scannedComponent.setClusteringMode(ruleNodeAnnotation.clusteringMode());
            scannedComponent.setHasQueueName(ruleNodeAnnotation.hasQueueName());
            NodeDefinition nodeDefinition = prepareNodeDefinition(clazz, ruleNodeAnnotation);
            ObjectNode configurationDescriptor = JacksonUtil.newObjectNode();
            JsonNode node = JacksonUtil.valueToTree(nodeDefinition);
            configurationDescriptor.set("nodeDefinition", node);
            scannedComponent.setConfigurationDescriptor(configurationDescriptor);
            scannedComponent.setClazz(clazzName);
            log.debug("Processing scanned component: {}", scannedComponent);
        } catch (Exception e) {
            log.error("Can't initialize component {}, due to {}", clazzName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        ComponentDescriptor persistedComponent = componentDescriptorService.findByClazz(TenantId.SYS_TENANT_ID, clazzName);
        if (persistedComponent == null) {
            log.debug("Persisting new component: {}", scannedComponent);
            scannedComponent = componentDescriptorService.saveComponent(TenantId.SYS_TENANT_ID, scannedComponent);
        } else if (scannedComponent.equals(persistedComponent)) {
            log.debug("Component is already persisted: {}", persistedComponent);
            scannedComponent = persistedComponent;
        } else {
            log.debug("Component {} will be updated to {}", persistedComponent, scannedComponent);
            componentDescriptorService.deleteByClazz(TenantId.SYS_TENANT_ID, persistedComponent.getClazz());
            scannedComponent.setId(persistedComponent.getId());
            scannedComponent = componentDescriptorService.saveComponent(TenantId.SYS_TENANT_ID, scannedComponent);
        }
        return scannedComponent;
    }

    private NodeDefinition prepareNodeDefinition(Class<?> clazz, RuleNode nodeAnnotation) throws Exception {
        NodeDefinition nodeDefinition = new NodeDefinition();
        nodeDefinition.setDetails(nodeAnnotation.nodeDetails());
        nodeDefinition.setDescription(nodeAnnotation.nodeDescription());
        nodeDefinition.setInEnabled(nodeAnnotation.inEnabled());
        nodeDefinition.setOutEnabled(nodeAnnotation.outEnabled());
        nodeDefinition.setRelationTypes(getRelationTypesWithFailureRelation(clazz, nodeAnnotation));
        nodeDefinition.setCustomRelations(nodeAnnotation.customRelations());
        nodeDefinition.setRuleChainNode(nodeAnnotation.ruleChainNode());
        Class<? extends NodeConfiguration> configClazz = nodeAnnotation.configClazz();
        NodeConfiguration config = configClazz.getDeclaredConstructor().newInstance();
        NodeConfiguration defaultConfiguration = config.defaultConfiguration();
        nodeDefinition.setDefaultConfiguration(JacksonUtil.valueToTree(defaultConfiguration));
        nodeDefinition.setUiResources(nodeAnnotation.uiResources());
        nodeDefinition.setConfigDirective(nodeAnnotation.configDirective());
        nodeDefinition.setIcon(nodeAnnotation.icon());
        nodeDefinition.setIconUrl(nodeAnnotation.iconUrl());
        nodeDefinition.setDocUrl(nodeAnnotation.docUrl());
        return nodeDefinition;
    }

    private String[] getRelationTypesWithFailureRelation(Class<?> clazz, RuleNode nodeAnnotation) {
        List<String> relationTypes = new ArrayList<>(Arrays.asList(nodeAnnotation.relationTypes()));
        if (TbOriginatorTypeSwitchNode.class.equals(clazz)) {
            relationTypes.addAll(EntityType.NORMAL_NAMES);
        }
        if (TbMsgTypeSwitchNode.class.equals(clazz)) {
            relationTypes.addAll(TbMsgType.NODE_CONNECTIONS);
            relationTypes.add(TbNodeConnectionType.OTHER);
        }
        if (!relationTypes.contains(TbNodeConnectionType.FAILURE)) {
            relationTypes.add(TbNodeConnectionType.FAILURE);
        }
        return relationTypes.toArray(new String[relationTypes.size()]);
    }

    @Override
    public void discoverComponents() {
        registerRuleNodeComponents();
        log.debug("Found following definitions: {}", components.values());
    }

    @Override
    public List<ComponentDescriptor> getComponents(ComponentType type, RuleChainType ruleChainType) {
        if (RuleChainType.CORE.equals(ruleChainType)) {
            if (coreComponentsMap.containsKey(type)) {
                return Collections.unmodifiableList(coreComponentsMap.get(type));
            } else {
                return Collections.emptyList();
            }
        } else if (RuleChainType.EDGE.equals(ruleChainType)) {
            if (edgeComponentsMap.containsKey(type)) {
                return Collections.unmodifiableList(edgeComponentsMap.get(type));
            } else {
                return Collections.emptyList();
            }
        } else {
            log.error("Unsupported rule chain type {}", ruleChainType);
            throw new RuntimeException("Unsupported rule chain type " + ruleChainType);
        }
    }

    @Override
    public List<ComponentDescriptor> getComponents(Set<ComponentType> types, RuleChainType ruleChainType) {
        if (RuleChainType.CORE.equals(ruleChainType)) {
            return getComponents(types, coreComponentsMap);
        } else if (RuleChainType.EDGE.equals(ruleChainType)) {
            return getComponents(types, edgeComponentsMap);
        } else {
            log.error("Unsupported rule chain type {}", ruleChainType);
            throw new RuntimeException("Unsupported rule chain type " + ruleChainType);
        }
    }

    @Override
    public Optional<ComponentDescriptor> getComponent(String clazz) {
        return Optional.ofNullable(components.get(clazz));
    }

    private List<ComponentDescriptor> getComponents(Set<ComponentType> types, Map<ComponentType, List<ComponentDescriptor>> componentsMap) {
        List<ComponentDescriptor> result = new ArrayList<>();
        types.stream().filter(componentsMap::containsKey).forEach(type -> {
            result.addAll(componentsMap.get(type));
        });
        return Collections.unmodifiableList(result);
    }
}
