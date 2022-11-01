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

import java.util.List;

import static org.thingsboard.server.msa.ui.utils.TestUtil.sleep;

public class RuleChainsPageElements extends OtherPageElementsHelper {
    public RuleChainsPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String ENTITY = "//mat-row//span[contains(text(),'%s')]";
    private static final String DELETE_BTN = ENTITY + "/../..//mat-icon[contains(text(),' delete')]/../..";
    private static final String MAKE_ROOT_BTN = ENTITY + "/../..//mat-icon[contains(text(),' flag')]/../..";
    private static final String ROOT = ENTITY + "/../..//mat-icon[text() = 'check_box']";
    private static final String ROOT_DISABLE = ENTITY + "/../..//mat-icon[text() = 'check_box_outline_blank']";
    private static final String PLUS_BTN = "//mat-icon[contains(text(),'add')]/../parent::button";
    private static final String CREATE_RULE_CHAIN_BTN = "//span[contains(text(),'Create new rule chain')]";
    private static final String CREATE_RULE_CHAIN_NAME_FIELD = "//form[@class='ng-untouched ng-pristine ng-invalid']//input[@formcontrolname='name']";
    private static final String CREATE_RULE_CHAIN_ADD_BTN = "//span[contains(text(),'Add')]/..";
    private static final String RULE_CHAIN = "//mat-row//span[contains(text(),'%s')]";
    private static final String RULE_CHAINS_NAMES_WITHOUT_ROOT = "//mat-icon[contains(text(),'check_box_outline_blank')]/../../../mat-cell[contains(@class,'name')]/span";
    private static final String WARNING_MESSAGE = "//tb-snack-bar-component/div/div";
    private static final String DELETE_RULE_CHAIN_FROM_VIEW_BTN = "//span[contains(text(),' Delete rule chain ')]";
    private static final String IMPORT_RULE_CHAIN_BTN = "//span[contains(text(),'Import rule chain')]";
    private static final String BROWSE_FILE = "//input[@class='file-input']";
    private static final String IMPORT_BROWSE_FILE = "//mat-dialog-container//span[contains(text(),'Import')]/..";
    private static final String IMPORTING_FILE = "//div[contains(text(),'%s')]";
    private static final String CLEAR_IMPORT_FILE_BTN = "//div[@class='tb-file-clear-container']//button";
    private static final String OPEN_RULE_CHAIN = ENTITY + "/../..//mat-icon[contains(text(),' settings_ethernet')]";
    private static final String OPEN_RULE_CHAIN_FROM_VIEW = "//span[contains(text(),'Open rule chain')]";
    private static final String MAKE_ROOT_FROM_VIEW = "(//span[contains(text(),' Make rule chain root ')]/..)[1]";
    private static final String ROOT_ACTIVE_CHECKBOXES = "//mat-icon[text() = 'check_box']";
    private static final String SEARCH_RULE_CHAIN_FIELD = "//input[@data-placeholder='Search rule chains']";
    private static final String ALL_NAMES = "//mat-icon[contains(text(),'check')]/../../../mat-cell[contains(@class,'name')]/span";

    public String getWarningMessage() {
        return WARNING_MESSAGE;
    }

    public String getDeleteRuleChainFromViewBtn() {
        return DELETE_RULE_CHAIN_FROM_VIEW_BTN;
    }

    public String getEntity(String entityName) {
        return String.format(ENTITY, entityName);
    }

    public WebElement deleteBtn(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(DELETE_BTN, entityName));
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

    public WebElement plusBtn() {
        return waitUntilElementToBeClickable(PLUS_BTN);
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

    public WebElement addBtnC() {
        return waitUntilElementToBeClickable(CREATE_RULE_CHAIN_ADD_BTN);
    }

    public WebElement addBtnV() {
        return waitUntilVisibilityOfElementLocated(CREATE_RULE_CHAIN_ADD_BTN);
    }

    public WebElement ruleChain(String entityName) {
        return waitUntilElementToBeClickable(String.format(RULE_CHAIN, entityName));
    }

    public WebElement warningMessage() {
        return waitUntilVisibilityOfElementLocated(WARNING_MESSAGE);
    }

    public List<WebElement> notRootRuleChainsNames() {
        return waitUntilVisibilityOfElementsLocated(RULE_CHAINS_NAMES_WITHOUT_ROOT);
    }

    public WebElement deleteBtnFromView() {
        sleep(1);
        return waitUntilElementToBeClickable(DELETE_RULE_CHAIN_FROM_VIEW_BTN);
    }

    public WebElement browseFile() {
        sleep(2);
        return driver.findElement(By.xpath(BROWSE_FILE));
    }

    public WebElement importBrowseFileBtn() {
        return waitUntilElementToBeClickable(IMPORT_BROWSE_FILE);
    }

    public WebElement importingFile(String fileName) {
        return waitUntilVisibilityOfElementLocated(String.format(IMPORTING_FILE, fileName));
    }

    public WebElement clearImportFileBtn() {
        return waitUntilElementToBeClickable(CLEAR_IMPORT_FILE_BTN);
    }

    public WebElement openRuleChainFromViewBtn() {
        return waitUntilElementToBeClickable(OPEN_RULE_CHAIN_FROM_VIEW);
    }

    public WebElement openRuleChainBtn(String name) {
        return waitUntilElementToBeClickable(String.format(OPEN_RULE_CHAIN, name));
    }

    public List<WebElement> entities(String name) {
        return waitUntilVisibilityOfElementsLocated(String.format(ENTITY, name));
    }

    public WebElement makeRootFromViewBtn() {
        return waitUntilElementToBeClickable(MAKE_ROOT_FROM_VIEW);
    }

    public WebElement searchField() {
        return waitUntilElementToBeClickable(SEARCH_RULE_CHAIN_FIELD);
    }

    public List<WebElement> allNames() {
        return waitUntilVisibilityOfElementsLocated(ALL_NAMES);
    }
}
