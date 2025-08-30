package com.evalart.automation.pages;

import com.evalart.automation.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private final By userInput = By.cssSelector("input[name='username'], input#username, input[name='user']");
    private final By passInput = By.cssSelector("input[type='password'], input[name='password'], input#password");
    private final By loginBtn  = By.cssSelector("button[type='submit'], input[type='submit'], button.login");

    public LoginPage(WebDriver driver) { super(driver); }

    public void goTo(String url) { driver.get(url); }

    public void login(String user, String pass) {
        type(userInput, user);
        type(passInput, pass);
        click(loginBtn);
    }
}