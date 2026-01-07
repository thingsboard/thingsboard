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
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

public class OpenRuleChainPageElements extends AbstractBasePage {
    public OpenRuleChainPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String DONE_BTN = "//mat-icon[contains(text(),'done')]/parent::button";
    private static final String INPUT_NODE = "//div[@class='tb-rule-node tb-input-type']";
    private static final String HEAD_RULE_CHAIN_NAME = "//div[@class='tb-breadcrumb']/span[2]";

    public WebElement inputNode() {
        return waitUntilVisibilityOfElementLocated(INPUT_NODE);
    }

    public WebElement headRuleChainName() {
        return waitUntilVisibilityOfElementLocated(HEAD_RULE_CHAIN_NAME);
    }

    public WebElement doneBtn() {
        return waitUntilElementToBeClickable(DONE_BTN);
    }

}
