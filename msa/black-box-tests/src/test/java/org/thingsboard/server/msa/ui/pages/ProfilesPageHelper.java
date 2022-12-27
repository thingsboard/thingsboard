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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ProfilesPageHelper extends ProfilesPageElements {
    public ProfilesPageHelper(WebDriver driver) {
        super(driver);
    }

    private String name;
    private String ruleChain;
    private String mobileDashboard;
    private String queue;
    private String description;

    public void setName() {
        this.name = profileViewNameField().getAttribute("value");
    }

    public void setRuleChain() {
        this.ruleChain = profileViewRuleChainField().getAttribute("value");
    }

    public void setMobileDashboard() {
        this.mobileDashboard = profileViewMobileDashboardField().getAttribute("value");
    }

    public void setQueue() {
        this.queue = profileViewQueueField().getAttribute("value");
    }

    public void setDescription() {
        this.description = profileViewDescriptionField().getAttribute("value");
    }

    public String getName() {
        return this.name;
    }

    public String getRuleChain() {
        return this.ruleChain;
    }

    public String getMobileDashboard() {
        return this.mobileDashboard;
    }

    public String getQueue() {
        return this.queue;
    }

    public String getDescription() {
        return this.description;
    }

    public void enterName(String name) {
        addDeviceProfileNameField().click();
        addDeviceProfileNameField().sendKeys(name);
    }

    public void chooseRuleChain(String ruleChain) {
        addDeviceProfileRuleChainField().click();
        entityFromList(ruleChain).click();
    }

    public void chooseMobileDashboard(String mobileDashboard) {
        addDeviceProfileMobileDashboardField().click();
        entityFromList(mobileDashboard).click();
    }

    public void chooseQueue(String queue) {
        addDeviceProfileQueueField().click();
        entityFromList(queue).click();
    }

    public void enterDescription(String description) {
        addDeviceDescriptionField().sendKeys(description);
    }

    public void openCreateDeviceProfileView() {
        plusBtn().click();
        createNewDeviceProfileBtn().click();
    }

    public void openImportDeviceProfileView() {
        plusBtn().click();
        importDeviceProfileBtn().click();
    }

    public boolean deleteDeviceProfileFromViewBtnIsNotDisplayed() {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(getDeviseProfileViewDeleteBtn())));
    }
 }

