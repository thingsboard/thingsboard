package org.thingsboard.server.dao.sql.widget;

import com.datastax.driver.core.utils.UUIDs;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
public class JpaWidgetTypeDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Test
    @DatabaseSetup(("classpath:dbunit/widget_type.xml"))
    public void testFindByTenantIdAndBundleAlias() {
        UUID tenantId = UUID.fromString("2b7e4c90-2dfe-11e7-94aa-f7f6dbfb4833");
        List<WidgetType> widgetTypes = widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(tenantId, "BUNDLE_ALIAS_1");
        assertEquals(3, widgetTypes.size());
    }

    @Test
    @DatabaseSetup(("classpath:dbunit/widget_type.xml"))
    public void testFindByTenantIdAndBundleAliasAndAlias() {
        UUID tenantId = UUID.fromString("2b7e4c90-2dfe-11e7-94aa-f7f6dbfb4833");
        WidgetType widgetType = widgetTypeDao.findByTenantIdBundleAliasAndAlias(tenantId, "BUNDLE_ALIAS_1", "ALIAS3");
        UUID id = UUID.fromString("2b7e4c93-2dfe-11e7-94aa-f7f6dbfb4833");
        assertEquals(id, widgetType.getId().getId());
    }
}
