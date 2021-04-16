package org.thingsboard.server.service.apiusage;

import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;

public class CustomerApiUsageState extends BaseApiUsageState {
    public CustomerApiUsageState(ApiUsageState apiUsageState) {
        super(apiUsageState);
    }

    @Override
    public Pair<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThreshold(ApiFeature feature) {
        ApiUsageStateValue featureValue = ApiUsageStateValue.ENABLED;
        return setFeatureValue(feature, featureValue) ? Pair.of(feature, featureValue) : null;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }
}
