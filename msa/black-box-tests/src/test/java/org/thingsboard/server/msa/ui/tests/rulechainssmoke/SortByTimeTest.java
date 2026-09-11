// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.tests.rulechainssmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

@Feature("Sort rule chain by time")
public class SortByTimeTest extends AbstractRuleChainTest {

    @Test(priority = 10, groups = "smoke")
    @Description("Sort rule chain 'DOWN'")
    public void sortByTimeDown() {
        ruleChainName = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setSort();
        String firstListElement = ruleChainsPage.getSort().get(ruleChainsPage.getSort().size() - 1);
        String lastCreated = ruleChainsPage.createdTime().get(0).getText();

        assertThat(firstListElement).as("Last in list is last created").isEqualTo(lastCreated);
        assertIsDisplayed(ruleChainsPage.createdTimeEntity(ruleChainName, lastCreated));
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Sort rule chain 'UP'")
    public void sortByTimeUp() {
        ruleChainName = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByTimeBtn().click();
        ruleChainsPage.setSort();
        String firstListElement = ruleChainsPage.getSort().get(ruleChainsPage.getSort().size() - 1);
        String lastCreated = ruleChainsPage.createdTime().get(ruleChainsPage.createdTime().size() - 1).getText();

        assertThat(firstListElement).as("First in list is last created").isEqualTo(lastCreated);
        assertIsDisplayed(ruleChainsPage.createdTimeEntity(ruleChainName, lastCreated));
    }
}
