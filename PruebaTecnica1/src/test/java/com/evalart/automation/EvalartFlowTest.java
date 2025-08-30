package com.evalart.automation;

import com.evalart.automation.core.DriverFactory;
import com.evalart.automation.pages.FormPage;
import com.evalart.automation.pages.LoginPage;
import com.evalart.automation.pages.SuccessPage;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.*;

public class EvalartFlowTest {

    private WebDriver driver;

    private static final String URL  = "https://tasks.evalartapp.com/automatization/";
    private static final String USER = "1124386";
    private static final String PASS = "10df2f32286b7120Mi00LTY4MzQyMTE=30e0c83e6c29f1c3";

    @BeforeClass
    public void setup() { driver = DriverFactory.create(); }

    @AfterClass(alwaysRun = true)
    public void tearDown() { if (driver != null) driver.quit(); }

    @Test
    public void fullFlowTenCycles_withRestartOnError() {
        String finalHash = runWithRestartOnError(10);
        System.out.println("\n==============================");
        System.out.println("HASH DE ÉXITO: " + finalHash);
        System.out.println("==============================\n");
        Assert.assertNotNull(finalHash);
        Assert.assertFalse(finalHash.isBlank(), "El hash no debería venir vacío");
    }

    private String runWithRestartOnError(int totalCycles) {
        int cycle = 1;

        while (true) {
            try {
                LoginPage login = new LoginPage(driver);
                login.goTo(URL);
                login.login(USER, PASS);

                FormPage formPage = new FormPage(driver);
                if (!formPage.isLoaded()) throw new TimeoutException("No cargó el formulario tras login.");

                for (; cycle <= totalCycles; cycle++) {
                    formPage.solveAndSubmit();
                    formPage.waitNextCycleOrSuccess(cycle + 1);
                    // Si ya estamos en éxito, rompemos el bucle
                    if (!formPage.isLoaded()) break;
                }

                SuccessPage success = new SuccessPage(driver);

                long t0 = System.currentTimeMillis();
                while (!success.isOnSuccess() && System.currentTimeMillis() - t0 < 20000) {
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                }

                if (!success.isOnSuccess()) throw new TimeoutException("No se detectó la página de éxito tras 10 ciclos.");
                return success.getHash();

            } catch (Exception e) {
                System.err.println("Fallo detectado en ciclo " + cycle + " -> reiniciando desde 1. Causa: " + e.getMessage());
                cycle = 1; // reiniciar
            }
        }
    }
}