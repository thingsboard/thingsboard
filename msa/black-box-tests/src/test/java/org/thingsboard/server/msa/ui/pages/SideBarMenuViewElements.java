package pages;

import base.BasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SideBarMenuViewElements extends BasePage {
    public SideBarMenuViewElements(WebDriver driver) {
        super(driver);
    }

    private static final String RULE_CHAINS_BTN = "//mat-toolbar//a[@href='/ruleChains']";
    private static final String CUSTOMER_BTN = "//mat-toolbar//a[@href='/customers']";
    private static final String DASHBOARD_BTN = "//mat-toolbar//a[@href='/dashboards']";

    public WebElement ruleChainsBtn() {
        return waitUntilElementToBeClickable(RULE_CHAINS_BTN);
    }

    public WebElement customerBtn() {
        return waitUntilElementToBeClickable(CUSTOMER_BTN);
    }

    public WebElement dashboardBtn() {
        return waitUntilElementToBeClickable(DASHBOARD_BTN);
    }
}