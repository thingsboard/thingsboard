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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class RuleChainsPageElements extends OtherPageElementsHelper {
    public RuleChainsPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String MAKE_ROOT_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'flag')]/ancestor::button";
    private static final String ROOT = ENTITY + "/../..//mat-icon[text() = 'check_box']";
    private static final String ROOT_DISABLE = ENTITY + "/../..//mat-icon[text() = 'check_box_outline_blank']";
    private static final String CREATED_TIME = ENTITY + "/../..//mat-cell/span[contains(text(),'%s')]";
    private static final String CREATE_RULE_CHAIN_BTN = "//span[contains(text(),'Create new rule chain')]";
    private static final String CREATE_RULE_CHAIN_NAME_FIELD = "//form[@class='ng-untouched ng-pristine ng-invalid']//input[@formcontrolname='name']";
    private static final String RULE_CHAINS_NAMES_WITHOUT_ROOT = "//mat-icon[contains(text(),'check_box_outline_blank')]/../../../mat-cell[contains(@class,'name')]/span";
    private static final String DELETE_RULE_CHAIN_FROM_VIEW_BTN = "//span[contains(text(),' Delete')]";
    private static final String IMPORT_RULE_CHAIN_BTN = "//span[contains(text(),'Import rule chain')]";
    private static final String OPEN_RULE_CHAIN_FROM_VIEW = "//span[contains(text(),'Open rule chain')]";
    private static final String MAKE_ROOT_FROM_VIEW = "//span[contains(text(),' Make rule chain root ')]/parent::button";
    private static final String ROOT_ACTIVE_CHECKBOXES = "//mat-icon[text() = 'check_box']";
    private static final String ALL_NAMES = "//mat-icon[contains(text(),'check')]/../../../mat-cell[contains(@class,'name')]/span";

    public String getDeleteRuleChainFromViewBtn() {
        return DELETE_RULE_CHAIN_FROM_VIEW_BTN;
    }

    public WebElement makeRootBtn(String entityName) {
        return waitUntilElementToBeClickable(String.format(MAKE_ROOT_BTN, entityName));
    }

    public List<WebElement> rootCheckBoxesEnable() {
        return waitUntilVisibilityOfElementsLocated(ROOT_ACTIVE_CHECKBOXES);
    }

    public WebElement rootCheckBoxEnable(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(ROOT, entityName));
    }

    public WebElement rootCheckBoxDisable(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(ROOT_DISABLE, entityName));
    }

    public WebElement createRuleChainBtn() {
        return waitUntilElementToBeClickable(CREATE_RULE_CHAIN_BTN);
    }

    public WebElement importRuleChainBtn() {
        return waitUntilElementToBeClickable(IMPORT_RULE_CHAIN_BTN);
    }

    public WebElement nameField() {
        return waitUntilElementToBeClickable(CREATE_RULE_CHAIN_NAME_FIELD);
    }

    public List<WebElement> notRootRuleChainsNames() {
        return waitUntilVisibilityOfElementsLocated(RULE_CHAINS_NAMES_WITHOUT_ROOT);
    }

    public WebElement deleteBtnFromView() {
        return waitUntilElementToBeClickable(DELETE_RULE_CHAIN_FROM_VIEW_BTN);
    }

    public WebElement openRuleChainFromViewBtn() {
        return waitUntilElementToBeClickable(OPEN_RULE_CHAIN_FROM_VIEW);
    }

    public List<WebElement> entities(String name) {
        return waitUntilVisibilityOfElementsLocated(String.format(ENTITY, name));
    }

    public WebElement makeRootFromViewBtn() {
        return waitUntilElementToBeClickable(MAKE_ROOT_FROM_VIEW);
    }

    public List<WebElement> allNames() {
        return waitUntilVisibilityOfElementsLocated(ALL_NAMES);
    }

    public WebElement createdTimeEntity(String name, String time) {
        return waitUntilElementToBeClickable(String.format(CREATED_TIME, name, time));
    }
}
