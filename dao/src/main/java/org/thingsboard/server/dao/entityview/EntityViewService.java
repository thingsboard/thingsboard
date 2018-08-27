package org.thingsboard.server.dao.entityview;

import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;

public interface EntityViewService {
    EntityView findEntityViewById(EntityViewId entityViewId);


    EntityView findEntityViewByTenantIdAndName(TenantId tenantId, String name);

    EntityView saveEntityView(EntityView entityView);

    EntityView assignEntityViewToCustomer(EntityViewId entityViewId, CustomerId customerId);

    void deleteEntityView(EntityViewId entityViewId);

    TextPageData<EntityView> findEntityViewByTenantId(TenantId tenantId, TextPageLink pageLink);

    TextPageData<EntityView> findEntityViewByTenantIdAndType(TenantId tenantId, String type, TextPageLink pageLink);

    void deleteEntityViewByTenantId(TenantId tenantId);

    TextPageData<EntityView> findEntityViewByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink);

    TextPageData<EntityView> findEntityViewByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, TextPageLink pageLink);
}
