package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AlarmHelper extends AlarmElements {
    public AlarmHelper(WebDriver driver) {
        super(driver);
    }

    public void assignTo(String user) {
        jsClick(assignBtn());
        userFromAssignDropDown(user).click();
    }

    private List<String> users;

    public void setUsers() {
        users = assignUsers()
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public List<String> getUsers() {
        return users;
    }
}
