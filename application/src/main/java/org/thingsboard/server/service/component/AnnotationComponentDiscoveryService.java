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
package org.thingsboard.server.service.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.api.NodeDefinition;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.component.ComponentDescriptorService;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    private Map<String, ComponentDescriptor> components = new HashMap<>();

    private Map<ComponentType, List<ComponentDescriptor>> componentsMap = new HashMap<>();

    private ObjectMapper mapper = new ObjectMapper();

    private boolean isInstall() {
        return environment.acceptsProfiles("install");
    }

    @PostConstruct
    public void init() {
        if (!isInstall()) {
            discoverComponents();
        }
    }

    private void registerRuleNodeComponents() {
        Set<BeanDefinition> ruleNodeBeanDefinitions = getBeanDefinitions(RuleNode.class);
        for (BeanDefinition def : ruleNodeBeanDefinitions) {
            int retryCount = 0;
            Exception cause = null;
            while (retryCount < MAX_OPTIMISITC_RETRIES) {
                try {
                    String clazzName = def.getBeanClassName();
                    Class<?> clazz = Class.forName(clazzName);
                    RuleNode ruleNodeAnnotation = clazz.getAnnotation(RuleNode.class);
                    ComponentType type = ruleNodeAnnotation.type();
                    ComponentDescriptor component = scanAndPersistComponent(def, type);
                    components.put(component.getClazz(), component);
                    componentsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
                    break;
                } catch (Exception e) {
                    log.trace("Can't initialize component {}, due to {}", def.getBeanClassName(), e.getMessage(), e);
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
                log.error("Can't initialize component {}, due to {}", def.getBeanClassName(), cause.getMessage(), cause);
                throw new RuntimeException(cause);
            }
        }
    }

    private void registerComponents(ComponentType type, Class<? extends Annotation> annotation) {
        List<ComponentDescriptor> components = persist(getBeanDefinitions(annotation), type);
        componentsMap.put(type, components);
        registerComponents(components);
    }

    private void registerComponents(Collection<ComponentDescriptor> comps) {
        comps.forEach(c -> components.put(c.getClazz(), c));
    }

    private List<ComponentDescriptor> persist(Set<BeanDefinition> filterDefs, ComponentType type) {
        List<ComponentDescriptor> result = new ArrayList<>();
        for (BeanDefinition def : filterDefs) {
            result.add(scanAndPersistComponent(def, type));
        }
        return result;
    }

    private ComponentDescriptor scanAndPersistComponent(BeanDefinition def, ComponentType type) {
        ComponentDescriptor scannedComponent = new ComponentDescriptor();
        String clazzName = def.getBeanClassName();
        try {
            scannedComponent.setType(type);
            Class<?> clazz = Class.forName(clazzName);
            switch (type) {
                case ENRICHMENT:
                case FILTER:
                case TRANSFORMATION:
                case ACTION:
                case EXTERNAL:
                    RuleNode ruleNodeAnnotation = clazz.getAnnotation(RuleNode.class);
                    scannedComponent.setName(ruleNodeAnnotation.name());
                    scannedComponent.setScope(ruleNodeAnnotation.scope());
                    NodeDefinition nodeDefinition = prepareNodeDefinition(ruleNodeAnnotation);
                    ObjectNode configurationDescriptor = mapper.createObjectNode();
                    JsonNode node = mapper.valueToTree(nodeDefinition);
                    configurationDescriptor.set("nodeDefinition", node);
                    scannedComponent.setConfigurationDescriptor(configurationDescriptor);
                    break;
                default:
                    throw new RuntimeException(type + " is not supported yet!");
            }
            scannedComponent.setClazz(clazzName);
            log.info("Processing scanned component: {}", scannedComponent);
        } catch (Exception e) {
            log.error("Can't initialize component {}, due to {}", def.getBeanClassName(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
        ComponentDescriptor persistedComponent = componentDescriptorService.findByClazz(TenantId.SYS_TENANT_ID, clazzName);
        if (persistedComponent == null) {
            log.info("Persisting new component: {}", scannedComponent);
            scannedComponent = componentDescriptorService.saveComponent(TenantId.SYS_TENANT_ID, scannedComponent);
        } else if (scannedComponent.equals(persistedComponent)) {
            log.info("Component is already persisted: {}", persistedComponent);
            scannedComponent = persistedComponent;
        } else {
            log.info("Component {} will be updated to {}", persistedComponent, scannedComponent);
            componentDescriptorService.deleteByClazz(TenantId.SYS_TENANT_ID, persistedComponent.getClazz());
            scannedComponent.setId(persistedComponent.getId());
            scannedComponent = componentDescriptorService.saveComponent(TenantId.SYS_TENANT_ID, scannedComponent);
        }
        return scannedComponent;
    }

    private NodeDefinition prepareNodeDefinition(RuleNode nodeAnnotation) throws Exception {
        NodeDefinition nodeDefinition = new NodeDefinition();
        nodeDefinition.setDetails(nodeAnnotation.nodeDetails());
        nodeDefinition.setDescription(nodeAnnotation.nodeDescription());
        nodeDefinition.setInEnabled(nodeAnnotation.inEnabled());
        nodeDefinition.setOutEnabled(nodeAnnotation.outEnabled());
        nodeDefinition.setRelationTypes(getRelationTypesWithFailureRelation(nodeAnnotation));
        nodeDefinition.setCustomRelations(nodeAnnotation.customRelations());
        Class<? extends NodeConfiguration> configClazz = nodeAnnotation.configClazz();
        NodeConfiguration config = configClazz.newInstance();
        NodeConfiguration defaultConfiguration = config.defaultConfiguration();
        nodeDefinition.setDefaultConfiguration(mapper.valueToTree(defaultConfiguration));
        nodeDefinition.setUiResources(nodeAnnotation.uiResources());
        nodeDefinition.setConfigDirective(nodeAnnotation.configDirective());
        nodeDefinition.setIcon(nodeAnnotation.icon());
        nodeDefinition.setIconUrl(nodeAnnotation.iconUrl());
        nodeDefinition.setDocUrl(nodeAnnotation.docUrl());
        return nodeDefinition;
    }

    private String[] getRelationTypesWithFailureRelation(RuleNode nodeAnnotation) {
        List<String> relationTypes = new ArrayList<>(Arrays.asList(nodeAnnotation.relationTypes()));
        if (!relationTypes.contains(TbRelationTypes.FAILURE)) {
            relationTypes.add(TbRelationTypes.FAILURE);
        }
        return relationTypes.toArray(new String[relationTypes.size()]);
    }

    private Set<BeanDefinition> getBeanDefinitions(Class<? extends Annotation> componentType) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(componentType));
        Set<BeanDefinition> defs = new HashSet<>();
        for (String scanPackage : scanPackages) {
            defs.addAll(scanner.findCandidateComponents(scanPackage));
        }
        return defs;
    }

    @Override
    public void discoverComponents() {
        registerRuleNodeComponents();
        log.info("Found following definitions: {}", components.values());
    }

    @Override
    public List<ComponentDescriptor> getComponents(ComponentType type) {
        if (componentsMap.containsKey(type)) {
            return Collections.unmodifiableList(componentsMap.get(type));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<ComponentDescriptor> getComponents(Set<ComponentType> types) {
        List<ComponentDescriptor> result = new ArrayList<>();
        types.stream().filter(type -> componentsMap.containsKey(type)).forEach(type -> {
            result.addAll(componentsMap.get(type));
        });
        return Collections.unmodifiableList(result);
    }

    @Override
    public Optional<ComponentDescriptor> getComponent(String clazz) {
        return Optional.ofNullable(components.get(clazz));
    }
}
