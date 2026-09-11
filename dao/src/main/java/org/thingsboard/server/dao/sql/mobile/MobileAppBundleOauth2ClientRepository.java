// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.mobile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.dao.model.sql.MobileAppOauth2ClientCompositeKey;
import org.thingsboard.server.dao.model.sql.MobileAppBundleOauth2ClientEntity;

import java.util.List;
import java.util.UUID;

public interface MobileAppBundleOauth2ClientRepository extends JpaRepository<MobileAppBundleOauth2ClientEntity, MobileAppOauth2ClientCompositeKey> {

    List<MobileAppBundleOauth2ClientEntity> findAllByMobileAppBundleId(UUID mobileAppId);

}
