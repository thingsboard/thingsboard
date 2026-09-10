// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.tests.rulechainssmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Make rule chain root")
public class MakeRuleChainRootTest extends AbstractRuleChainTest {

    @AfterMethod
    public void makeRoot() {
        setRootRuleChain("Root Rule Chain");
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Make rule chain root by clicking on the 'Make rule chain root' icon in the right corner")
    public void makeRuleChainRootByRightCornerBtn() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.makeRootBtn(ruleChain).click();
        ruleChainsPage.warningPopUpYesBtn().click();

        assertIsDisplayed(ruleChainsPage.rootCheckBoxEnable(ruleChain));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Make rule chain root by clicking on the 'Make rule chain root' button in the entity view")
    public void makeRuleChainRootFromView() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.detailsBtn(ruleChain).click();
        ruleChainsPage.makeRootFromViewBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.closeEntityViewBtn().click();

        assertIsDisplayed(ruleChainsPage.rootCheckBoxEnable(ruleChain));
    }

    @Test(priority = 30, groups = "smoke")
    @Description("Make multiple root rule chains (only one rule chain can be root)")
    public void multiplyRoot() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.detailsBtn(ruleChain).click();
        jsClick(ruleChainsPage.makeRootFromViewBtn());
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.closeEntityViewBtn().click();

        assertThat(ruleChainsPage.rootCheckBoxesEnable()).as("Enable only 1 root checkbox").hasSize(1);
    }
}
