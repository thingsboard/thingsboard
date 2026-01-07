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
package org.thingsboard.server.msa.ui.utils;

import org.openqa.selenium.Keys;
import org.testng.annotations.DataProvider;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomSymbol;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.DEVICE_ACTIVE_STATE;
import static org.thingsboard.server.msa.ui.utils.Const.DEVICE_INACTIVE_STATE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class DataProviderCredential {

    private static final String NAME = ENTITY_NAME;
    private static final String SYMBOL = String.valueOf(getRandomSymbol());
    private static final String NUMBER = "1";
    private static final String LONG_PHONE_NUMBER = "20155501231";
    private static final String SHORT_PHONE_NUMBER = "201555011";
    private static final String RULE_CHAIN_SECOND_WORD_NAME_PATH = "Rule";
    private static final String CUSTOMER_SECOND_WORD_NAME_PATH = "Customer";
    private static final String RULE_CHAIN_FIRST_WORD_NAME_PATH = "Root";
    private static final String CUSTOMER_FIRST_WORD_NAME_PATH = "A";
    private static final String DEFAULT_DEVICE_PROFILE_NAME = "Device Profile";
    private static final String DEFAULT_ASSET_PROFILE_NAME = "Asset Profile";

    @DataProvider
    public static Object[][] ruleChainNameForSearchByFirstAndSecondWord() {
        return new Object[][]{
                {RULE_CHAIN_SECOND_WORD_NAME_PATH},
                {RULE_CHAIN_FIRST_WORD_NAME_PATH}};
    }

    @DataProvider
    public static Object[][] nameForSearchBySymbolAndNumber() {
        String name = ENTITY_NAME + random();
        return new Object[][]{
                {name, name.split("`")[1]},
                {NAME + getRandomNumber(), "~"},
                {NAME + getRandomNumber(), "`"},
                {NAME + getRandomNumber(), "!"},
                {NAME + getRandomNumber(), "@"},
                {NAME + getRandomNumber(), "#"},
                {NAME + getRandomNumber(), "$"},
                {NAME + getRandomNumber(), "^"},
                {NAME + getRandomNumber(), "&"},
                {NAME + getRandomNumber(), "*"},
                {NAME + getRandomNumber(), "("},
                {NAME + getRandomNumber(), ")"},
                {NAME + getRandomNumber(), "+"},
                {NAME + getRandomNumber(), "="},
                {NAME + getRandomNumber(), "-"}};
    }

    @DataProvider
    public static Object[][] nameForSort() {
        return new Object[][]{
                {NAME + getRandomNumber()},
                {SYMBOL},
                {NUMBER}};
    }

    @DataProvider
    public static Object[][] nameForAllSort() {
        return new Object[][]{
                {NAME + getRandomNumber(), SYMBOL, NUMBER}};
    }

    @DataProvider
    public static Object[][] incorrectPhoneNumber() {
        return new Object[][]{
                {LONG_PHONE_NUMBER},
                {SHORT_PHONE_NUMBER},
                {ENTITY_NAME + getRandomNumber()}};
    }

    @DataProvider
    public static Object[][] customerNameForSearchByFirstAndSecondWord() {
        return new Object[][]{
                {CUSTOMER_FIRST_WORD_NAME_PATH},
                {CUSTOMER_SECOND_WORD_NAME_PATH}};
    }

    @DataProvider
    public static Object[][] deviceProfileSearch() {
        String name = NAME + getRandomNumber();
        return new Object[][]{
                {DEFAULT_DEVICE_PROFILE_NAME, DEFAULT_DEVICE_PROFILE_NAME.split(" ")[0]},
                {DEFAULT_DEVICE_PROFILE_NAME, DEFAULT_DEVICE_PROFILE_NAME.split(" ")[1]},
                {name, name.split("`")[1]},
                {NAME + getRandomNumber(), "~"},
                {NAME + getRandomNumber(), "`"},
                {NAME + getRandomNumber(), "!"},
                {NAME + getRandomNumber(), "@"},
                {NAME + getRandomNumber(), "#"},
                {NAME + getRandomNumber(), "$"},
                {NAME + getRandomNumber(), "^"},
                {NAME + getRandomNumber(), "&"},
                {NAME + getRandomNumber(), "*"},
                {NAME + getRandomNumber(), "("},
                {NAME + getRandomNumber(), ")"},
                {NAME + getRandomNumber(), "+"},
                {NAME + getRandomNumber(), "="},
                {NAME + getRandomNumber(), "-"}};
    }

    @DataProvider
    public static Object[][] assetProfileSearch() {
        String name = NAME + getRandomNumber();
        return new Object[][]{
                {DEFAULT_ASSET_PROFILE_NAME, DEFAULT_ASSET_PROFILE_NAME.split(" ")[0]},
                {DEFAULT_ASSET_PROFILE_NAME, DEFAULT_ASSET_PROFILE_NAME.split(" ")[1]},
                {name, name.split("`")[1]},
                {NAME + getRandomNumber(), "~"},
                {NAME + getRandomNumber(), "`"},
                {NAME + getRandomNumber(), "!"},
                {NAME + getRandomNumber(), "@"},
                {NAME + getRandomNumber(), "#"},
                {NAME + getRandomNumber(), "$"},
                {NAME + getRandomNumber(), "^"},
                {NAME + getRandomNumber(), "&"},
                {NAME + getRandomNumber(), "*"},
                {NAME + getRandomNumber(), "("},
                {NAME + getRandomNumber(), ")"},
                {NAME + getRandomNumber(), "+"},
                {NAME + getRandomNumber(), "="},
                {NAME + getRandomNumber(), "-"}};
    }

    @DataProvider
    public static Object[][] editMenuDescription() {
        String newDescription = "Description" + getRandomNumber();
        String description = "Description";
        return new Object[][]{
                {"", newDescription, newDescription},
                {description, newDescription, description + newDescription},
                {description, Keys.CONTROL + "A" + Keys.BACK_SPACE, ""}};
    }

    @DataProvider
    public static Object[][] enable() {
        return new Object[][]{
                {false},
                {true}};
    }

    @DataProvider
    public static Object[][] editDeviceLabel() {
        String newLabel = "Label" + getRandomNumber();
        String label = "Label";
        return new Object[][]{
                {"", newLabel, newLabel},
                {label, newLabel, label + newLabel},
                {label, Keys.CONTROL + "A" + Keys.BACK_SPACE, ""}};
    }

    @DataProvider(name = "filterData")
    public static Object[][] getFilterData() {
        return new Object[][]{
                {DEVICE_ACTIVE_STATE},
                {DEVICE_INACTIVE_STATE}
        };
    }
}
