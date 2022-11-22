package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class OpenRuleChainPageHelperAbstract extends OpenRuleChainPageElementsAbstract {
    public OpenRuleChainPageHelperAbstract(WebDriver driver) {
        super(driver);
    }

    private String headName;

    public void setHeadName() {
        this.headName = headRuleChainName().getText().split(" ")[1];
    }

    public String getHeadName() {
        return headName;
    }

    public void waitUntilDoneBtnDisable() {
        waitUntilVisibilityOfElementLocated(getDoneBtnDisable());
    }
}
