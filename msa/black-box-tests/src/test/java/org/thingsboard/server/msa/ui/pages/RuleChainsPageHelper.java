/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.msa.ui.pages;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.rule.RuleChain;

import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
public class RuleChainsPageHelper extends RuleChainsPageElements {
    public RuleChainsPageHelper(WebDriver driver) {
        super(driver);
    }

    public void openCreateRuleChainView() {
        plusBtn().click();
        createRuleChainBtn().click();
    }

    public void openImportRuleChainView() {
        plusBtn().click();
        importRuleChainBtn().click();
    }

    private int getRandomNumberFromRuleChainsCount() {
        Random random = new Random();
        return random.nextInt(notRootRuleChainsNames().size());
    }

    private String ruleChainName;

    public void setRuleChainNameWithoutRoot() {
        this.ruleChainName = notRootRuleChainsNames().get(getRandomNumberFromRuleChainsCount()).getText();
    }

    public void setRuleChainNameWithoutRoot(int number) {
        this.ruleChainName = notRootRuleChainsNames().get(number).getText();
    }

    public void setRuleChainName(int number) {
        this.ruleChainName = allNames().get(number).getText();
    }

    public String getNotRootRuleChainName() {
        return this.ruleChainName;
    }

    public String getRuleChainId(String entityName) {
        PageData<RuleChain> tenantRuleChains;
        tenantRuleChains = client.getRuleChains(pageLink);
        return String.valueOf(tenantRuleChains.getData().stream().filter(s -> s.getName().equals(entityName)).collect(Collectors.toList()).get(0).getId());
    }

    public void deleteRuleChain(String entityName) {
        try {
            PageData<RuleChain> tenantRuleChains;
            tenantRuleChains = client.getRuleChains(pageLink);
            try {
                client.deleteRuleChain(tenantRuleChains.getData().stream().filter(s -> s.getName().equals(entityName)).collect(Collectors.toList()).get(0).getId());
            } catch (Exception e) {
                client.deleteRuleChain(tenantRuleChains.getData().stream().filter(s -> s.getName().equals(entityName)).collect(Collectors.toList()).get(1).getId());
            }
        } catch (Exception e) {
            log.info("Can't delete!");
        }
    }

    public void deleteAllRuleChain(String entityName) {
        try {
            PageData<RuleChain> tenantRuleChains;
            tenantRuleChains = client.getRuleChains(pageLink);
            tenantRuleChains.getData().stream().filter(s -> s.getName().equals(entityName)).collect(Collectors.toList()).forEach(x -> client.deleteRuleChain(x.getId()));
        } catch (Exception e) {
            log.info("Can't delete!");
        }
    }

    public void createRuleChain(String entityName) {
        try {
            PageData<RuleChain> tenantRuleChains;
            tenantRuleChains = client.getRuleChains(pageLink);
            RuleChain ruleChain = new RuleChain();
            ruleChain.setName(entityName);
            client.saveRuleChain(ruleChain);
            tenantRuleChains.getData().add(ruleChain);
        } catch (Exception e) {
            log.info("Can't create!");
        }
    }

    public void makeRoot() {
        try {
            PageData<RuleChain> tenantRuleChains;
            tenantRuleChains = client.getRuleChains(pageLink);
            tenantRuleChains.getData().stream().filter(s -> s.getName().equals("Root Rule Chain")).collect(Collectors.toList()).forEach(x -> client.setRootRuleChain(x.getId()));
        } catch (Exception e) {
            log.info("Can't make root!");
        }
    }

    public void createRuleChains(String entityName, int count) {
        try {
            PageData<RuleChain> tenantRuleChains;
            tenantRuleChains = client.getRuleChains(pageLink);
            RuleChain ruleChain = new RuleChain();
            for (int i = 0; i < count; i++) {
                ruleChain.setName(entityName);
                client.saveRuleChain(ruleChain);
                tenantRuleChains.getData().add(ruleChain);
            }
        } catch (Exception e) {
            log.info("Can't create!");
        }
    }

    public String deleteRuleChainUI(String entityName) {
        String s = "";
        if (deleteBtn(entityName) != null) {
            deleteBtn(entityName).click();
            warningPopUpYesBtn().click();
            return entityName;
        } else {
            for (int i = 0; i < deleteBtns().size(); i++) {
                if (deleteBtns().get(i).isEnabled()) {
                    deleteBtns().get(i).click();
                    warningPopUpYesBtn().click();
                    if (elementIsNotPresent(getWarningMessage())) {
                        s = driver.findElements(By.xpath(getDeleteBtns()
                                + "/../../../mat-cell/following-sibling::mat-cell/following-sibling::mat-cell[contains(@class,'cdk-column-name')]/span")).get(i).getText();
                        break;
                    }
                }
            }
            return s;
        }
    }

    public String deleteSelected(String entityName) {
        String s = "";
        if (deleteBtn(entityName) != null) {
            checkBox(entityName).click();
            deleteSelectedBtn().click();
            warningPopUpYesBtn().click();
            return entityName;
        } else {
            for (int i = 0; i < checkBoxes().size(); i++) {
                if (checkBoxes().get(i).isDisplayed()) {
                    s = driver.findElements(By.xpath(getCheckboxes() + "/../../mat-cell/following-sibling::mat-cell/following-sibling::mat-cell[contains(@class,'cdk-column-name')]/span")).get(i).getText();
                    checkBox(s).click();
                    deleteSelectedBtn().click();
                    warningPopUpYesBtn().click();
                    if (elementIsNotPresent(getWarningMessage())) {
                        break;
                    }
                }
            }
            return s;
        }
    }

    public String deleteRuleChainFromView(String ruleChainName) {
        String s = "";
        if (deleteBtnFromView() != null) {
            deleteBtnFromView().click();
            warningPopUpYesBtn().click();
            if (elementIsNotPresent(getWarningMessage())) {
                return getEntity(ruleChainName);
            }
        } else {
            for (int i = 0; i < notRootRuleChainsNames().size(); i++) {
                notRootRuleChainsNames().get(i).click();
                if (deleteBtnFromView() != null) {
                    deleteBtnFromView().click();
                    warningPopUpYesBtn().click();
                    if (elementIsNotPresent(getWarningMessage())) {
                        s = notRootRuleChainsNames().get(i).getText();
                        break;
                    }
                }
            }
        }
        return s;
    }

    public void assertCheckBoxIsNotDisplayed(String entityName) {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//mat-checkbox)[2]")));
        Assert.assertFalse(driver.findElement(By.xpath(getCheckbox(entityName))).isDisplayed());
    }

    public void assertDeleteBtnInRootRuleChainIsNotDisplayed() {
        Assert.assertFalse(driver.findElement(By.xpath(getDeleteRuleChainFromViewBtn())).isDisplayed());
    }

    public boolean ruleChainIsNotPresent(String ruleChainName) {
        sleep(2);
        return elementIsNotPresent(getEntity(ruleChainName));
    }

    public void doubleClickOnRuleChain(String ruleChainName) {
        doubleClick(ruleChain(ruleChainName));
    }

    public void searchRuleChain(String namePath) {
        searchBtn().click();
        searchField().sendKeys(namePath);
        sleep(1);
    }
}
