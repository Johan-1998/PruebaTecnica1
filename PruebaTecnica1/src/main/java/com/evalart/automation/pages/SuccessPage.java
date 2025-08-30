package com.evalart.automation.pages;

import com.evalart.automation.core.BasePage;
import org.openqa.selenium.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SuccessPage extends BasePage {

    // Caja negra donde muestran el hash (tiene la clase "break-all" en tu screenshot)
    private final By hashBox = By.cssSelector("p.break-all, .break-all, [class*='break-all']");

    public SuccessPage(WebDriver driver) { super(driver); }

    /** Heurística para saber si estamos en la pantalla de éxito */
    public boolean isOnSuccess() {
        try {
            // Si aparece el contenedor del hash o el texto "Felicidades"
            if (!driver.findElements(hashBox).isEmpty()) return true;
            String body = driver.findElement(By.tagName("body")).getText();
            return body != null && body.toLowerCase().contains("felicidades");
        } catch (Exception e) {
            return false;
        }
    }

    /** Devuelve el hash en una sola línea, sin espacios/ saltos */
    public String getHash() {
        // 1) Intentar leer la caja del hash
        try {
            WebElement el = driver.findElement(hashBox);
            String text = el.getText();               // puede venir con saltos por el wrap
            String compact = text.replaceAll("\\s+", "");
            if (!compact.isBlank()) return compact;
        } catch (NoSuchElementException ignore) {}

        // 2) Fallback: buscar patrón en todo el body. Suele empezar con "<General>"
        String body = driver.findElement(By.tagName("body")).getText();
        String compactBody = body.replaceAll("\\s+", "");
        // Captura "<General>" seguido de una cadena larga (letras, números, '='...)
        Matcher m = Pattern.compile("(<General>[0-9A-Za-z=]{20,})").matcher(compactBody);
        if (m.find()) return m.group(1);

        // 3) Último recurso: cualquier token largo tipo hash
        m = Pattern.compile("\\b[0-9A-Fa-f=]{20,}\\b").matcher(compactBody);
        if (m.find()) return m.group();

        throw new NoSuchElementException("No se encontró el hash en la página de éxito.");
    }
}