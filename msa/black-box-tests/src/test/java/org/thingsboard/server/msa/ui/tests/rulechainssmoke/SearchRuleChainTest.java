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
package org.thingsboard.server.msa.ui.tests.rulechainssmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

@Feature("Search rule chain")
public class SearchRuleChainTest extends AbstractRuleChainTest {

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "ruleChainNameForSearchByFirstAndSecondWord")
    @Description("Search rule chain by first word in the name/Search rule chain by second word in the name")
    public void searchFirstWord(String namePath) {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.searchEntity(namePath);

        ruleChainsPage.allNames().forEach(rc -> assertThat(rc.getText().contains(namePath))
                .as("All entity contains search input").isTrue());
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSearchBySymbolAndNumber")
    @Description("Search rule chain by symbol in the name/Search rule chain by number in the name")
    public void searchNumber(String name, String namePath) {
        ruleChainName = name;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.searchEntity(namePath);

        ruleChainsPage.allNames().forEach(rc -> assertThat(rc.getText().contains(namePath))
                .as("All entity contains search input").isTrue());
    }
}
