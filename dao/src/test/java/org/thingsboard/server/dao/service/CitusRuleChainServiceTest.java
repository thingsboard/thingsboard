// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

/**
 * Reruns the rule-chain service tests against a real Citus cluster. Beyond the general coverage of the
 * covering-lock write paths ({@code saveRuleChainMetaData} / rule-chain deletion), this notably exercises the
 * {@code fk_default_rule_chain_device_profile} / {@code fk_default_rule_chain_asset_profile} friendly-message
 * translation with the constraint name arriving shard-suffixed from a worker, which
 * {@code DaoUtil.constraintNameMatches} must still recognize (see
 * {@code testDeleteRuleChainReferencedAsDeviceProfileDefault} / {@code ...AsAssetProfileDefault}).
 */
@CitusDaoSqlTest
public class CitusRuleChainServiceTest extends RuleChainServiceTest {
}
