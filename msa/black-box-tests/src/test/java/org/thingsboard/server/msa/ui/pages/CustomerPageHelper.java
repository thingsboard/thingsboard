package org.thingsboard.server.msa.ui.pages;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.page.PageData;

import java.util.stream.Collectors;

@Slf4j
public class CustomerPageHelperAbstract extends CustomerPageElementsAbstract {
    public CustomerPageHelperAbstract(WebDriver driver) {
        super(driver);
    }

    private String customerName;
    private String country;
    private String dashboard;
    private String dashboardFromView;

    private String customerEmail;
    private String customerCountry;
    private String customerCity;

    public void setCustomerName() {
        this.customerName = entityTitles().get(0).getText();
    }

    public void setCustomerName(int number) {
        this.customerName = entityTitles().get(number).getText();
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCountry() {
        this.country = countries().get(0).getText();
    }

    public String getCountry() {
        return country;
    }

    public void setDashboard() {
        this.dashboard = listOfEntity().get(0).getText();
    }

    public void setDashboardFromView() {
        this.dashboardFromView = editMenuDashboardField().getAttribute("value");
    }

    public String getDashboard() {
        return dashboard;
    }

    public String getDashboardFromView() {
        return dashboardFromView;
    }

    public void setCustomerEmail(String title) {
        this.customerEmail = email(title).getText();
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerCountry(String title) {
        this.customerCountry = country(title).getText();
    }

    public String getCustomerCountry() {
        return customerCountry;
    }

    public void setCustomerCity(String title) {
        this.customerCity = city(title).getText();
    }

    public String getCustomerCity() {
        return customerCity;
    }

    public void createCustomer(String entityName) {
        try {
            PageData<Customer> tenantCustomer;
            tenantCustomer = client.getCustomers(pageLink);
            Customer customer = new Customer();
            customer.setTitle(entityName);
            client.saveCustomer(customer);
            tenantCustomer.getData().add(customer);
        } catch (Exception e) {
            log.info("Can't create!");
        }
    }

    public void deleteCustomer(String entityName) {
        try {
            PageData<Customer> tenantRuleChains;
            tenantRuleChains = client.getCustomers(pageLink);
            try {
                client.deleteCustomer(tenantRuleChains.getData().stream().filter(s -> s.getName().equals(entityName)).collect(Collectors.toList()).get(0).getId());
            } catch (Exception e) {
                client.deleteCustomer(tenantRuleChains.getData().stream().filter(s -> s.getName().equals(entityName)).collect(Collectors.toList()).get(1).getId());
            }
        } catch (Exception e) {
            log.info("Can't delete!");
        }
    }

    public void changeTitleEditMenu(String newTitle) {
        titleFieldEntityView().clear();
        wait.until(ExpectedConditions.textToBe(By.xpath(String.format(INPUT_FIELD, INPUT_FIELD_NAME_TITLE)), ""));
        titleFieldEntityView().sendKeys(newTitle);
    }

    public void chooseDashboard() {
        editMenuDashboardField().click();
        sleep(0.5);
        editMenuDashboard().click();
        sleep(0.5);
    }

    public void createCustomersUser() {
        plusBtn().click();
        addUserEmailField().sendKeys(getRandomNumber() + "@gmail.com");
        addBtnC().click();
        activateWindowOkBtn().click();
    }

    public void selectCountryEntityView() {
        countrySelectMenuEntityView().click();
        setCountry();
        countries().get(0).click();
    }

    public void selectCountryAddEntityView() {
        countrySelectMenuAddEntityView().click();
        setCountry();
        countries().get(0).click();
    }

    public void assignedDashboard() {
        plusBtn().click();
        assignedField().click();
        setDashboard();
        listOfEntity().get(0).click();
        submitAssignedBtn().click();
    }

    public boolean customerIsNotPresent(String title) {
        return elementsIsNotPresent(getEntity(title));
    }

    public void sortByNameDown() {
        doubleClick(sortByTitleBtn());
    }
}
