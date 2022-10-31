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

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;

public class OtherPageElementsHelper extends OtherPageElements {
    public OtherPageElementsHelper(WebDriver driver) {
        super(driver);
    }

    private String headerName;

    public void setHeaderName() {
        this.headerName = headerNameView().getText();
    }

    public String getHeaderName() {
        return headerName;
    }

    public void goToNextTab() {
        helpBtn().click();
        ArrayList<String> tabs2 = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(tabs2.get(1));
    }

    public void clickOnCheckBoxes(int count) {
        for (int i = 0; i < count; i++) {
            checkBoxes().get(i).click();
        }
    }

    public void changeNameEditMenu(String newName) {
        nameFieldEditMenu().sendKeys(Keys.CONTROL + "a" + Keys.BACK_SPACE);
        nameFieldEditMenu().sendKeys(newName);
    }

    public void changeDescription(String newDescription) {
        description().sendKeys(Keys.CONTROL + "a" + Keys.BACK_SPACE);
        description().sendKeys(newDescription);
    }
}

