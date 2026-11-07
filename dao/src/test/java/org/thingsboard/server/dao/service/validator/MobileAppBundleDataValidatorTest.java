// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.selfregistration.MobileSelfRegistrationParams;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.dao.mobile.MobileAppDao;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.thingsboard.server.dao.service.validator.GroupPermissionDataValidator.SELF_REGISTRATION_USER_GROUP_ERROR;

@SpringBootTest(classes = MobileAppBundleDataValidator.class)
class MobileAppBundleDataValidatorTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.fromString("5f8b4e1c-9a6b-4d0e-8c2f-4a5b6c7d8e9f"));

    @MockitoBean
    MobileAppDao mobileAppDao;
    @MockitoBean
    EntityGroupDao entityGroupDao;

    @Autowired
    MobileAppBundleDataValidator validator;

    @Test
    void givenBundleWithMatchingApps_thenAccepted() {
        MobileAppBundle bundle = bundle();
        bundle.setAndroidAppId(app(PlatformType.ANDROID));
        bundle.setIosAppId(app(PlatformType.IOS));

        assertThatCode(() -> validator.validateDataImpl(tenantId, bundle)).doesNotThrowAnyException();
    }

    @Test
    void givenNonExistingAndroidApp_thenRejected() {
        MobileAppBundle bundle = bundle();
        bundle.setAndroidAppId(new MobileAppId(UUID.randomUUID()));

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, bundle))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Mobile app bundle refers to non-existing android app!");
    }

    @Test
    void givenIosAppAsAndroidApp_thenRejected() {
        MobileAppBundle bundle = bundle();
        bundle.setAndroidAppId(app(PlatformType.IOS));

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, bundle))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Mobile app bundle refers to wrong android app! Platform type of specified app is IOS");
    }

    @Test
    void givenNonExistingIosApp_thenRejected() {
        MobileAppBundle bundle = bundle();
        bundle.setIosAppId(new MobileAppId(UUID.randomUUID()));

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, bundle))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Mobile app bundle refers to non-existing ios app!");
    }

    @Test
    void givenAndroidAppAsIosApp_thenRejected() {
        MobileAppBundle bundle = bundle();
        bundle.setIosAppId(app(PlatformType.ANDROID));

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, bundle))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Mobile app bundle refers to wrong ios app! Platform type of specified app is ANDROID");
    }

    @Test
    void givenSelfRegistrationPermissionTargetingUserGroup_thenRejected() {
        MobileAppBundle bundle = bundle();
        bundle.setSelfRegistrationParams(selfRegistrationParams(List.of(permission(EntityType.DEVICE), permission(EntityType.USER))));

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, bundle))
                .isInstanceOf(DataValidationException.class)
                .hasMessage(SELF_REGISTRATION_USER_GROUP_ERROR);
    }

    @Test
    void givenSelfRegistrationPermissionsWithoutUserGroups_thenAccepted() {
        MobileAppBundle bundle = bundle();
        bundle.setSelfRegistrationParams(selfRegistrationParams(List.of(permission(EntityType.DEVICE), permission(EntityType.ASSET))));

        assertThatCode(() -> validator.validateDataImpl(tenantId, bundle)).doesNotThrowAnyException();
    }

    @Test
    void givenSelfRegistrationWithoutPermissions_thenAccepted() {
        MobileAppBundle bundle = bundle();
        bundle.setSelfRegistrationParams(selfRegistrationParams(null));

        assertThatCode(() -> validator.validateDataImpl(tenantId, bundle)).doesNotThrowAnyException();
    }

    private MobileAppBundle bundle() {
        MobileAppBundle bundle = new MobileAppBundle();
        bundle.setTenantId(tenantId);
        bundle.setTitle("Bundle");
        return bundle;
    }

    private MobileAppId app(PlatformType platformType) {
        MobileApp app = new MobileApp(new MobileAppId(UUID.randomUUID()));
        app.setPlatformType(platformType);
        given(mobileAppDao.findById(tenantId, app.getUuidId())).willReturn(app);
        return app.getId();
    }

    private GroupPermission permission(EntityType groupType) {
        EntityGroup entityGroup = new EntityGroup(new EntityGroupId(UUID.randomUUID()));
        entityGroup.setType(groupType);
        given(entityGroupDao.findById(tenantId, entityGroup.getUuidId())).willReturn(entityGroup);
        GroupPermission permission = new GroupPermission();
        permission.setEntityGroupId(entityGroup.getId());
        return permission;
    }

    private MobileSelfRegistrationParams selfRegistrationParams(List<GroupPermission> permissions) {
        MobileSelfRegistrationParams params = new MobileSelfRegistrationParams();
        params.setPermissions(permissions);
        return params;
    }

}
