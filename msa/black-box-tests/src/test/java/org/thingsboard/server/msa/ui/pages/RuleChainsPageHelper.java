/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

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
    private String description;

    public void setRuleChainNameWithoutRoot() {
        this.ruleChainName = notRootRuleChainsNames().get(getRandomNumberFromRuleChainsCount()).getText();
    }

    public void setRuleChainNameWithoutRoot(int number) {
        this.ruleChainName = notRootRuleChainsNames().get(number).getText();
    }

    public void setDescription() {
        scrollToElement(descriptionEntityView());
        this.description = descriptionEntityView().getAttribute("value");
    }

    public void setRuleChainName(int number) {
        this.ruleChainName = allNames().get(number).getText();
    }

    public String getRuleChainName() {
        return this.ruleChainName;
    }

    public String getDescription() {
        return description;
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

    public boolean deleteBtnInRootRuleChainIsNotDisplayed() {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(getDeleteRuleChainFromViewBtn())));
    }

    public boolean assertRuleChainsIsNotPresent(String ruleChainName) {
        return elementsIsNotPresent(getEntity(ruleChainName));
    }

    public void sortByNameDown() {
        doubleClick(sortByNameBtn());
    }

    ArrayList<String> sort;

    public void setSort() {
        ArrayList<String> createdTime = new ArrayList<>();
        createdTime().forEach(x -> createdTime.add(x.getText()));
        Collections.sort(createdTime);
        sort = createdTime;
    }

    public ArrayList<String> getSort() {
        return sort;
    }
}
