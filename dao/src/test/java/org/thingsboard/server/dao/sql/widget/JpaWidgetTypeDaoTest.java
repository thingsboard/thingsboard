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
package org.thingsboard.server.dao.sql.widget;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetBundleInfo;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeFilter;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JpaWidgetTypeDaoTest extends AbstractJpaDaoTest {

    // given search text should find a widget with tags, when searching by tags
    final Map<String, String[]> SHOULD_FIND_SEARCH_TO_TAGS_MAP = Map.of(
            "A",             new String[]{"a", "b", "c"},
            "test A test",   new String[]{"a", "b", "c"},
            "test x y test", new String[]{"x y", "b", "c"},
            "x y test",      new String[]{"x y", "b", "c"},
            "x y",           new String[]{"x y", "b", "c"},
            "test x y",      new String[]{"x y", "b", "c"}
    );

    // given search text should not find a widget with tags, when searching by tags
    final Map<String, String[]> SHOULDNT_FIND_SEARCH_TO_TAGS_MAP = Map.of(
            "testA test",   new String[]{"a", "b", "c"},
            "testx y test", new String[]{"x y", "b", "c"},
            "testx ytest",  new String[]{"x y", "b", "c"},
            "x ytest",      new String[]{"x y", "b", "c"},
            "testx y",      new String[]{"x y", "b", "c"},
            "x",            new String[]{"x y", "b", "c"}
    );

    final String BUNDLE_ALIAS = "BUNDLE_ALIAS";
    final int WIDGET_TYPE_COUNT = 3;
    List<WidgetTypeInfo> widgetTypeList;
    WidgetsBundle widgetsBundle;

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Autowired
    private WidgetsBundleDao widgetsBundleDao;

    @Before
    public void setUp() throws InterruptedException {
        widgetTypeList = new ArrayList<>();

        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setAlias(BUNDLE_ALIAS);
        widgetsBundle.setTitle(BUNDLE_ALIAS);
        widgetsBundle.setId(new WidgetsBundleId(Uuids.timeBased()));
        widgetsBundle.setTenantId(TenantId.SYS_TENANT_ID);
        this.widgetsBundle = widgetsBundleDao.save(TenantId.SYS_TENANT_ID, widgetsBundle);

        for (int i = 0; i < WIDGET_TYPE_COUNT; i++) {
            Thread.sleep(2);
            var widgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, i);
            widgetTypeList.add(widgetType);
            widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(this.widgetsBundle.getId(), widgetType.getId(), i));
        }
        widgetTypeList.sort(Comparator.comparing(BaseWidgetType::getName));
    }

    WidgetTypeInfo createAndSaveWidgetType(TenantId tenantId, int number) {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("WIDGET_TYPE_" + number);
        widgetType.setDescription("WIDGET_TYPE_DESCRIPTION" + number);
        widgetType.setFqn("FQN_" + number);
        widgetType.setImage("/image/system/logo.png");
        var descriptor = JacksonUtil.newObjectNode();
        descriptor.put("type", number % 2 == 0 ? "latest" : "static");
        widgetType.setDescriptor(descriptor);
        String[] tags = new String[]{"Tag1_" + number, "Tag2_" + number, "TEST_" + number};
        widgetType.setTags(tags);
        WidgetTypeDetails saved = widgetTypeDao.save(TenantId.SYS_TENANT_ID, widgetType);
        List<WidgetBundleInfo> bundles = new ArrayList<>();
        bundles.add(new WidgetBundleInfo(widgetsBundle.getUuidId(), widgetsBundle.getName()));
        return new WidgetTypeInfo(saved, bundles);
    }

    WidgetTypeDetails createAndSaveWidgetType(TenantId tenantId, int number, String[] tags) {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("WIDGET_TYPE_" + number);
        widgetType.setDescription("WIDGET_TYPE_DESCRIPTION" + number);
        widgetType.setFqn("FQN_" + number);
        widgetType.setImage("/image/tenant/logo.png");
        var descriptor = JacksonUtil.newObjectNode();
        descriptor.put("type", number % 2 == 0 ? "latest" : "static");
        widgetType.setDescriptor(descriptor);
        widgetType.setTags(tags);
        return widgetTypeDao.save(TenantId.SYS_TENANT_ID, widgetType);
    }

    @After
    public void tearDown() {
        widgetsBundleDao.removeById(TenantId.SYS_TENANT_ID, widgetsBundle.getUuidId());
        for (WidgetTypeInfo widgetType : widgetTypeList) {
            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, widgetType.getUuidId());
        }
    }

    @Test
    public void testFindByWidgetsBundleId() {
        List<WidgetType> widgetTypes = widgetTypeDao.findWidgetTypesByWidgetsBundleId(TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId());
        assertEquals(WIDGET_TYPE_COUNT, widgetTypes.size());
    }

    @Test
    public void testFindSystemWidgetTypes() {
        PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findSystemWidgetTypes(
                WidgetTypeFilter.builder()
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .fullSearch(true)
                        .deprecatedFilter(DeprecatedFilter.ALL)
                        .widgetTypes(Collections.singletonList("static")).build(),
                new PageLink(1024, 0, "TYPE_DESCRIPTION", new SortOrder("createdTime")));
        assertEquals(1, widgetTypes.getData().size());
        assertEquals(widgetTypeList.get(1), widgetTypes.getData().get(0));

        widgetTypes = widgetTypeDao.findSystemWidgetTypes(
                WidgetTypeFilter.builder()
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .fullSearch(true)
                        .deprecatedFilter(DeprecatedFilter.ALL)
                        .widgetTypes(Collections.emptyList()).build(),
                new PageLink(1024, 0, "hfgfd tag2_2 ghg", new SortOrder("createdTime")));
        assertEquals(1, widgetTypes.getData().size());
        assertEquals(widgetTypeList.get(2), widgetTypes.getData().get(0));
    }

    @Test
    public void testFindSystemWidgetTypesForSameName() throws InterruptedException {
        List<WidgetTypeDetails> sameNameList = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            Thread.sleep(2);
            var widgetType = saveWidgetType(TenantId.SYS_TENANT_ID, "widgetName");
            sameNameList.add(widgetType);
            widgetTypeList.add(new WidgetTypeInfo(widgetType));
        }
        sameNameList.sort(Comparator.comparing(BaseWidgetType::getName).thenComparing((BaseWidgetType baseWidgetType) -> baseWidgetType.getId().getId()));
        List<WidgetTypeInfo> expected = sameNameList.stream().map(WidgetTypeInfo::new).collect(Collectors.toList());

        PageData<WidgetTypeInfo> widgetTypesFirstPage = widgetTypeDao.findSystemWidgetTypes(
                WidgetTypeFilter.builder()
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .fullSearch(true)
                        .deprecatedFilter(DeprecatedFilter.ALL)
                        .widgetTypes(Collections.singletonList("static")).build(),
                new PageLink(10, 0, null, new SortOrder("name")));
        assertEquals(10, widgetTypesFirstPage.getData().size());
        assertThat(widgetTypesFirstPage.getData()).containsExactlyElementsOf(expected.subList(0, 10));

        PageData<WidgetTypeInfo> widgetTypesSecondPage = widgetTypeDao.findSystemWidgetTypes(WidgetTypeFilter.builder()
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .fullSearch(true)
                        .deprecatedFilter(DeprecatedFilter.ALL)
                        .widgetTypes(Collections.singletonList("static")).build(),
                new PageLink(10, 1, null, new SortOrder("name")));
        assertEquals(10, widgetTypesSecondPage.getData().size());
        assertThat(widgetTypesSecondPage.getData()).containsExactlyElementsOf(expected.subList(10, 20));
    }

    @Test
    public void testTagsSearchInFindBySystemWidgetTypes() {
        for (var entry : SHOULD_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findSystemWidgetTypes(
                    WidgetTypeFilter.builder()
                            .tenantId(TenantId.SYS_TENANT_ID)
                            .fullSearch(true)
                            .deprecatedFilter(DeprecatedFilter.ALL)
                            .widgetTypes(null).build(),
                    new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(1);
            assertThat(widgetTypes.getData().get(0).getId()).isEqualTo(savedWidgetType.getId());

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }

        for (var entry : SHOULDNT_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findSystemWidgetTypes(
                    WidgetTypeFilter.builder()
                            .tenantId(TenantId.SYS_TENANT_ID)
                            .fullSearch(true)
                            .deprecatedFilter(DeprecatedFilter.ALL)
                            .widgetTypes(null).build(),
                    new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(0);

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }
    }

    @Test
    public void testFindTenantWidgetTypesByTenantId() {
        UUID tenantId = Uuids.timeBased();
        for (int i = 0; i < WIDGET_TYPE_COUNT; i++) {
            var widgetType = createAndSaveWidgetType(TenantId.fromUUID(tenantId), i);
            widgetTypeList.add(widgetType);
        }
        PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findTenantWidgetTypesByTenantId(
                WidgetTypeFilter.builder()
                        .tenantId(TenantId.fromUUID(tenantId))
                        .fullSearch(true)
                        .deprecatedFilter(DeprecatedFilter.ALL)
                        .widgetTypes(null).build(),
                new PageLink(10, 0, "", new SortOrder("createdTime")));
        assertEquals(WIDGET_TYPE_COUNT, widgetTypes.getData().size());
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(3)), widgetTypes.getData().get(0));
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(4)), widgetTypes.getData().get(1));
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(5)), widgetTypes.getData().get(2));
    }

    @Test
    public void testTagsSearchInFindTenantWidgetTypesByTenantId() {
        for (var entry : SHOULD_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findTenantWidgetTypesByTenantId(
                    WidgetTypeFilter.builder()
                            .tenantId(TenantId.SYS_TENANT_ID)
                            .fullSearch(true)
                            .deprecatedFilter(DeprecatedFilter.ALL)
                            .widgetTypes(null).build(),
                    new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(1);
            assertThat(widgetTypes.getData().get(0).getId()).isEqualTo(savedWidgetType.getId());

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }

        for (var entry : SHOULDNT_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findTenantWidgetTypesByTenantId(
                    WidgetTypeFilter.builder()
                            .tenantId(TenantId.SYS_TENANT_ID)
                            .fullSearch(true)
                            .deprecatedFilter(DeprecatedFilter.ALL)
                            .widgetTypes(null).build(),
                    new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(0);

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }
    }

    @Test
    public void testTagsSearchInFindAllTenantWidgetTypesByTenantId() {
        for (var entry : SHOULD_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findAllTenantWidgetTypesByTenantId(
                    WidgetTypeFilter.builder()
                            .tenantId(TenantId.SYS_TENANT_ID)
                            .fullSearch(true)
                            .deprecatedFilter(DeprecatedFilter.ALL)
                            .widgetTypes(null).build(),
                    new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(1);
            assertThat(widgetTypes.getData().get(0).getId()).isEqualTo(savedWidgetType.getId());

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }

        for (var entry : SHOULDNT_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findAllTenantWidgetTypesByTenantId(
                    WidgetTypeFilter.builder()
                            .tenantId(TenantId.SYS_TENANT_ID)
                            .fullSearch(true)
                            .deprecatedFilter(DeprecatedFilter.ALL)
                            .widgetTypes(null).build(),
                    new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(0);

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }
    }

    @Test
    public void testFindByWidgetTypeInfosByBundleId() {
        PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId(), true, DeprecatedFilter.ALL, Collections.singletonList("latest"),
                new PageLink(1024, 0, "TYPE_DESCRIPTION", new SortOrder("createdTime")));
        assertEquals(2, widgetTypes.getData().size());
        assertEquals(widgetTypeList.get(0), widgetTypes.getData().get(0));
        assertEquals(widgetTypeList.get(2), widgetTypes.getData().get(1));

        widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId(), true, DeprecatedFilter.ALL, Collections.emptyList(),
                new PageLink(1024, 0, "hfgfd TEST_0 ghg", new SortOrder("createdTime")));
        assertEquals(1, widgetTypes.getData().size());
        assertEquals(widgetTypeList.get(0), widgetTypes.getData().get(0));
    }

    @Test
    public void testTagsSearchInFindByWidgetTypeInfosByBundleId() {
        for (var entry : SHOULD_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);
            widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(this.widgetsBundle.getId(), savedWidgetType.getId(), WIDGET_TYPE_COUNT + 1));

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(
                    TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId(), true, DeprecatedFilter.ALL, null, new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(1);
            assertThat(widgetTypes.getData().get(0).getId()).isEqualTo(savedWidgetType.getId());

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }

        for (var entry : SHOULDNT_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);
            widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(this.widgetsBundle.getId(), savedWidgetType.getId(), WIDGET_TYPE_COUNT + 1));

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(
                    TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId(), true, DeprecatedFilter.ALL, null, new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(0);

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }
    }

    @Test
    public void testFindByTenantIdAndFqn() {
        WidgetTypeInfo result = widgetTypeList.get(0);
        assertNotNull(result);
        WidgetType widgetType = widgetTypeDao.findByTenantIdAndFqn(TenantId.SYS_TENANT_ID.getId(), "FQN_0");
        assertEquals(result.getId(), widgetType.getId());
    }

    @Test
    public void testFindByTenantAndImageLink() {
        var result = widgetTypeDao.findByImageLink("/image/system/logo.png", 5);
        assertEquals(3, result.size());
        result = widgetTypeDao.findByImageLink("/image/system/logo2.png", 5);
        assertEquals(0, result.size());
    }

    @Test
    public void testFindByImageLink() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        WidgetTypeDetails details = createAndSaveWidgetType(tenantId, 0, new String[]{"a"});
        details.setDescriptor(JacksonUtil.newObjectNode().put("bg", "/image/tenant/widget.png"));
        widgetTypeDao.save(tenantId, details);
        var result = widgetTypeDao.findByTenantAndImageLink(tenantId, "/image/tenant/widget.png", 3);
        assertEquals(1, result.size());
        result = widgetTypeDao.findByTenantAndImageLink(tenantId, "/image/tenant/widget2.png", 3);
        assertEquals(0, result.size());
        widgetTypeDao.removeById(tenantId, details.getUuidId());
    }

    @Test
    public void testFindWidgetTypesWithBundles() {
        PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(
                TenantId.SYS_TENANT_ID.getId(),
                widgetsBundle.getUuidId(),
                true,
                DeprecatedFilter.ALL,
                Collections.singletonList("latest"),
                new PageLink(1024, 0, "TYPE_DESCRIPTION", new SortOrder("createdTime"))
        );

        assertEquals(2, widgetTypes.getData().size());
        for (var widgetType : widgetTypes.getData()) {
            assertFalse("Bundles should not be empty", widgetType.getBundles().isEmpty());
            assertEquals(BUNDLE_ALIAS, widgetType.getBundles().get(0).getName());
        }
    }

    @Test
    public void testAddWidgetTypeToNewBundleAndVerifyBundles() {
        String newBundleTitle = "New Bundle Title";
        WidgetsBundle newWidgetsBundle = new WidgetsBundle();
        newWidgetsBundle.setAlias("NewBundle");
        newWidgetsBundle.setTitle(newBundleTitle);
        newWidgetsBundle.setId(new WidgetsBundleId(UUID.randomUUID()));
        newWidgetsBundle.setTenantId(TenantId.SYS_TENANT_ID);
        newWidgetsBundle = widgetsBundleDao.save(TenantId.SYS_TENANT_ID, newWidgetsBundle);

        for (int i = 0; i < widgetTypeList.size(); i++) {
            WidgetTypeInfo widgetType = widgetTypeList.get(i);
            widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(newWidgetsBundle.getId(), widgetType.getId(), i));
        }

        PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(
                TenantId.SYS_TENANT_ID.getId(),
                newWidgetsBundle.getUuidId(),
                true,
                DeprecatedFilter.ALL,
                Collections.singletonList("latest"),
                new PageLink(1024, 0, "TYPE_DESCRIPTION", new SortOrder("createdTime"))
        );

        assertEquals(2, widgetTypes.getData().size());
        WidgetTypeInfo widgetTypeInfo1 = widgetTypes.getData().get(0);
        WidgetTypeInfo widgetTypeInfo2 = widgetTypes.getData().get(1);

        assertEquals(2, widgetTypeInfo1.getBundles().size());
        assertTrue("Bundles should contain 'BUNDLE_ALIAS'", widgetTypeInfo1.getBundles().stream().anyMatch(bundle -> BUNDLE_ALIAS.equals(bundle.getName())));
        assertTrue("Bundles should contain 'New Bundle Title'", widgetTypeInfo1.getBundles().stream().anyMatch(bundle -> newBundleTitle.equals(bundle.getName())));

        assertEquals(2, widgetTypeInfo2.getBundles().size());
        assertTrue("Bundles should contain 'BUNDLE_ALIAS'", widgetTypeInfo2.getBundles().stream().anyMatch(bundle -> "BUNDLE_ALIAS".equals(bundle.getName())));
        assertTrue("Bundles should contain 'New Bundle Title'", widgetTypeInfo2.getBundles().stream().anyMatch(bundle -> newBundleTitle.equals(bundle.getName())));

        // cleanup and verify
        widgetsBundleDao.removeById(newWidgetsBundle.getTenantId(), newWidgetsBundle.getUuidId());
        widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(
                TenantId.SYS_TENANT_ID.getId(),
                newWidgetsBundle.getUuidId(),
                true,
                DeprecatedFilter.ALL,
                Collections.singletonList("latest"),
                new PageLink(1024, 0, "TYPE_DESCRIPTION", new SortOrder("createdTime"))
        );
        widgetTypes.getData().forEach(widgetTypeInfo -> assertEquals(1, widgetTypeInfo.getBundles().size()));
    }

    private WidgetTypeDetails saveWidgetType(TenantId tenantId, String name) {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setDescription("WIDGET_TYPE_DESCRIPTION" + StringUtils.randomAlphabetic(7));
        widgetType.setName(name);
        var descriptor = JacksonUtil.newObjectNode();
        descriptor.put("type", "static");
        widgetType.setDescriptor(descriptor);
        return widgetTypeDao.save(TenantId.SYS_TENANT_ID, widgetType);
    }

}
