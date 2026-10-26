package com.iot.thingmodel.sync;

import com.iot.thingmodel.common.Constants;
import org.springframework.stereotype.Component;
import org.thingsboard.client.model.DefaultDeviceProfileConfiguration;
import org.thingsboard.client.model.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.client.model.DeviceProfile;
import org.thingsboard.client.model.DeviceProfileData;
import org.thingsboard.client.model.DeviceProfileProvisionType;
import org.thingsboard.client.model.DeviceProfileType;
import org.thingsboard.client.model.DeviceTransportType;
import org.thingsboard.client.model.DisabledDeviceProfileProvisionConfiguration;

@Component
public class ProfileGenerator {

    public String profileName(String name, String model) {
        String base = name == null || name.isBlank()
                ? Constants.Product.DEFAULT_PROFILE_BASE_NAME
                : name.trim();
        if (model == null || model.isBlank() || model.equalsIgnoreCase(base)) {
            return base;
        }
        return base + Constants.Product.PROFILE_NAME_SEPARATOR + model.trim();
    }

    public DeviceProfile generate(
            String name,
            String model,
            String productId,
            String categoryId,
            String tslVersion,
            int propertyCount,
            int eventCount,
            int actionCount) {
        DeviceProfile profile = new DeviceProfile();
        profile.setName(profileName(name, model));
        profile.setDescription(String.format(
                Constants.Product.PROFILE_DESCRIPTION_FORMAT,
                productId,
                categoryId == null ? "" : categoryId,
                tslVersion,
                propertyCount,
                eventCount,
                actionCount));
        profile.setType(DeviceProfileType.DEFAULT);
        profile.setTransportType(DeviceTransportType.DEFAULT);
        profile.setProvisionType(DeviceProfileProvisionType.DISABLED);

        DeviceProfileData data = new DeviceProfileData();
        data.setConfiguration(new DefaultDeviceProfileConfiguration());
        data.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        data.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration());
        profile.setProfileData(data);
        return profile;
    }
}
