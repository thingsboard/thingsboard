package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

public class OpenRuleChainPageElementsAbstract extends AbstractBasePage {
    public OpenRuleChainPageElementsAbstract(WebDriver driver) {
        super(driver);
    }

    private static final String DONE_BTN = "//mat-icon[contains(text(),'done')]/../..";
    private static final String DONE_BTN_DISABLE = "//mat-icon[contains(text(),'done')]/../parent::button[@disabled='true']";
    private static final String INPUT_NODE = "//div[@class='tb-rule-node tb-input-type']";
    private static final String HEAD_RULE_CHAIN_NAME = "//div[@class='tb-breadcrumb']/span[2]";

    public WebElement inputNode() {
        return waitUntilVisibilityOfElementLocated(INPUT_NODE);
    }

    public WebElement headRuleChainName() {
        return waitUntilVisibilityOfElementLocated(HEAD_RULE_CHAIN_NAME);
    }

    public String getDoneBtnDisable() {
        return DONE_BTN_DISABLE;
    }

    public WebElement doneBtn() {
        return waitUntilElementToBeClickable(DONE_BTN);
    }

}
