// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.rule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.dao.model.sql.RuleChainDetailsEntity;

import java.util.UUID;

public interface RuleChainDetailsRepository extends JpaRepository<RuleChainDetailsEntity, UUID> {

}
