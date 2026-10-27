package com.iot.thingmodel.sync;

import com.iot.thingmodel.common.Constants;
import org.junit.jupiter.api.Test;
import org.thingsboard.client.model.DeviceProfile;
import org.thingsboard.client.model.DeviceProfileProvisionType;
import org.thingsboard.client.model.DeviceProfileType;
import org.thingsboard.client.model.DeviceTransportType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileGeneratorTest {

    private final ProfileGenerator generator = new ProfileGenerator();

    @Test
    void generatesManagedDeviceProfile() {
        DeviceProfile profile = generator.generate(
                "WiFi诊断终端", "9HYN9PJNOY", "9HYN9PJNOY", "1", "1.0", 7, 0, 0);

        assertEquals("WiFi诊断终端 9HYN9PJNOY", profile.getName());
        assertTrue(profile.getDescription().contains("ProductId=9HYN9PJNOY"));
        assertTrue(profile.getDescription().contains("属性 7 个"));
        assertEquals(DeviceProfileType.DEFAULT, profile.getType());
        assertEquals(DeviceTransportType.DEFAULT, profile.getTransportType());
        assertEquals(DeviceProfileProvisionType.DISABLED, profile.getProvisionType());
        assertNotNull(profile.getProfileData());
        assertNotNull(profile.getProfileData().getConfiguration());
        assertNotNull(profile.getProfileData().getTransportConfiguration());
        assertNotNull(profile.getProfileData().getProvisionConfiguration());
    }

    @Test
    void usesDefaultBaseNameWhenNameIsBlank() {
        assertEquals("product M1", generator.profileName(" ", "M1"));
    }

    @Test
    void doesNotAppendModelWhenItMatchesName() {
        assertEquals("Device", generator.profileName("Device", "device"));
    }

    @Test
    void trimsNameAndModel() {
        assertEquals("Device M1", generator.profileName(" Device ", " M1 "));
    }

    @Test
    void formatsDescriptionWithCounts() {
        DeviceProfile profile = generator.generate(
                "Device", "M1", "P1", "C1", "2.0", 3, 4, 5);

        assertEquals(String.format(
                Constants.Product.PROFILE_DESCRIPTION_FORMAT,
                "P1", "C1", "2.0", 3, 4, 5),
                profile.getDescription());
    }
}
