package com.evalart.automation.pages;

import com.evalart.automation.core.BasePage;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormPage extends BasePage {

    private final By formContainer = By.cssSelector("form");
    private final By submitBtn = By.xpath(".//button[normalize-space()='Enviar' or @type='submit'] | .//input[@type='submit']");
    private final By cycleText = By.xpath("//*[contains(.,'Se encuentra en el ciclo') and contains(.,'de 10')]");

    public FormPage(WebDriver driver) { super(driver); }

    public boolean isLoaded() {
        try { waitVisible(formContainer); return true; }
        catch (TimeoutException e) { return false; }
    }

    /** Resuelve las 4 consignas y envía el formulario */
    public void solveAndSubmit() {
        WebElement form = waitVisible(formContainer);
        String pageText = driver.findElement(By.tagName("body")).getText();

        // 1) Conteo de emoji objetivo en la secuencia
        Integer emojiCount = parseEmojiCount(pageText);
        if (emojiCount != null) fillFirstVisible(form, "input[type='number']", String.valueOf(emojiCount));

        // 2) Fecha: suma o resta N días según el enunciado
        LocalDate targetDate = parseRelativeDateSmart(pageText);
        if (targetDate != null) {
            WebElement dateEl = firstVisible(form, By.cssSelector("input[type='date']"));
            if (dateEl == null) dateEl = firstVisible(form, By.cssSelector("input[name='date']"));
            if (dateEl == null) dateEl = firstVisible(form, By.cssSelector("input")); // último recurso
            if (dateEl != null) setDateInput(dateEl, targetDate);
        }

        // 3) Escriba N veces la letra "X"
        String repeated = parseRepeatChars(pageText);
        if (repeated != null) fillFirstVisible(form, "textarea, input[type='text']", repeated);

        // 4) Operación matemática
        Long math = parseMathResult(pageText);
        if (math != null) selectAnswer(form, String.valueOf(math));

        // Enviar
        try { click(submitBtn); }
        catch (Exception e) { jsClick(form.findElement(submitBtn)); }
    }

    /** Espera cambio de ciclo o éxito */
    public void waitNextCycleOrSuccess(int expectedCycle) {
        long end = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < end) {
            if (driver.findElements(formContainer).isEmpty()) return;
            try {
                WebElement el = driver.findElement(cycleText);
                String t = el.getText();
                Matcher m = Pattern.compile("Se encuentra en el ciclo\\s+(\\d+)\\s+de\\s+(\\d+)").matcher(t);
                if (m.find() && Integer.parseInt(m.group(1)) == expectedCycle) return;
            } catch (Exception ignore) {}
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
    }

    // ================== Fecha robusta ==================

    private void setDateInput(WebElement dateEl, LocalDate date) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String iso = date.format(DateTimeFormatter.ISO_LOCAL_DATE);            // yyyy-MM-dd
        String mmddyyyy = date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        String digits = date.format(DateTimeFormatter.ofPattern("MMddyyyy"));

        try { js.executeScript("arguments[0].scrollIntoView({block:'center'});", dateEl); } catch (Exception ignore) {}
        try { dateEl.click(); } catch (Exception ignore) {}

        // 1) JS: value=ISO + eventos
        try {
            js.executeScript(
                    "arguments[0].value = arguments[1];" +
                            "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));" +
                            "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                    dateEl, iso
            );
        } catch (Exception ignore) {}
        if (isValidDateInput(dateEl)) return;

        // 2) sendKeys ISO + TAB
        try { clearWithKeys(dateEl); dateEl.sendKeys(iso); dateEl.sendKeys(Keys.TAB); } catch (Exception ignore) {}
        if (isValidDateInput(dateEl)) return;

        // 3) sendKeys MM/dd/yyyy + TAB (placeholder mm/dd/aaaa)
        try { clearWithKeys(dateEl); dateEl.sendKeys(mmddyyyy); dateEl.sendKeys(Keys.TAB); } catch (Exception ignore) {}
        if (isValidDateInput(dateEl)) return;

        // 4) sendKeys sólo dígitos MMddyyyy + TAB
        try { clearWithKeys(dateEl); dateEl.sendKeys(digits); dateEl.sendKeys(Keys.TAB); } catch (Exception ignore) {}
        if (isValidDateInput(dateEl)) return;

        // 5) valueAsDate (UTC) + eventos
        try {
            js.executeScript(
                    "arguments[0].valueAsDate = new Date(Date.UTC(arguments[1], arguments[2]-1, arguments[3]));" +
                            "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));" +
                            "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                    dateEl, date.getYear(), date.getMonthValue(), date.getDayOfMonth()
            );
        } catch (Exception ignore) {}
        if (isValidDateInput(dateEl)) return;

        // info para debug si algo raro pasa
        String current = "";
        try { current = (String) js.executeScript("return arguments[0].value;", dateEl); } catch (Exception ignore) {}
        throw new RuntimeException("No pude completar la fecha. Valor actual del input='" + current + "'");
    }

    private boolean isValidDateInput(WebElement el) {
        try {
            Object ok = ((JavascriptExecutor) driver)
                    .executeScript("return !!arguments[0].value && (!arguments[0].checkValidity || arguments[0].checkValidity());", el);
            return ok instanceof Boolean && (Boolean) ok;
        } catch (Exception e) {
            return !el.getAttribute("value").isBlank();
        }
    }

    private void clearWithKeys(WebElement el) {
        try {
            el.click();
            el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            el.sendKeys(Keys.DELETE);
        } catch (Exception ignore) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].value='';", el);
        }
    }

    // ================== Helpers comunes ==================

    private void clearAndType(WebElement el, String value) {
        try {
            el.click();
            el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            el.sendKeys(Keys.DELETE);
            el.sendKeys(value);
        } catch (Exception ex) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1];" +
                            "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));" +
                            "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                    el, value
            );
        }
    }

    private void fillFirstVisible(WebElement form, String css, String value) {
        List<WebElement> els = form.findElements(By.cssSelector(css));
        for (WebElement el : els) if (el.isDisplayed() && el.isEnabled()) {
            clearAndType(el, value);
            break;
        }
    }

    private void selectAnswer(WebElement form, String target) {
        // SELECT
        List<WebElement> selects = form.findElements(By.tagName("select"));
        for (WebElement s : selects) if (s.isDisplayed() && s.isEnabled()) {
            try {
                Select sel = new Select(s);
                for (WebElement opt : sel.getOptions()) {
                    if (opt.getText().trim().equals(target)) {
                        sel.selectByVisibleText(target);
                        return;
                    }
                }
            } catch (Exception ignore) {}
        }
        // RADIO por label exacto
        List<WebElement> labels = form.findElements(By.xpath(".//label[normalize-space()='"+target+"']"));
        if (!labels.isEmpty()) { clickOrJs(labels.get(0)); return; }
        // RADIO por input + hermano
        List<WebElement> radios = form.findElements(By.cssSelector("input[type='radio']"));
        for (WebElement r : radios) if (r.isDisplayed() && r.isEnabled()) {
            try {
                WebElement parent = r.findElement(By.xpath("./ancestor::label[1]"));
                if (parent.getText().trim().equals(target)) { clickOrJs(r); return; }
            } catch (Exception ignore) {}
            try {
                WebElement sib = r.findElement(By.xpath("./following-sibling::*[1]"));
                if (sib.getText().trim().equals(target)) { clickOrJs(r); return; }
            } catch (Exception ignore) {}
        }
        // Input simple en el bloque (por si la operación viene como input)
        WebElement in = firstVisible(form, By.cssSelector("input[type='text'], input[type='number']"));
        if (in != null) clearAndType(in, target);
    }

    private void clickOrJs(WebElement el) { try { el.click(); } catch (Exception e) { jsClick(el); } }

    private WebElement firstVisible(WebElement scope, By locator) {
        List<WebElement> els = scope.findElements(locator);
        for (WebElement el : els) if (el.isDisplayed() && el.isEnabled()) return el;
        return null;
    }

    private String lower(String s) { return s == null ? "" : s.toLowerCase(); }

    // ================== Parsers ==================

    /** Emoji objetivo + conteo en la línea de la secuencia */
    private Integer parseEmojiCount(String txt) {
        // capturamos el emoji objetivo y la línea de secuencia
        Pattern p = Pattern.compile(
                "Indique\\s+cu[aá]ntos\\s+(.+?)\\s+hay.*?secuencia:\\s*([^\\r\\n]+)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL
        );
        Matcher m = p.matcher(txt);
        if (m.find()) {
            String target = m.group(1).trim();
            String sequence = m.group(2);
            // a veces el grupo 1 incluye texto adicional, nos quedamos con el primer "token" visual
            // (muchos emojis son varios codepoints; usamos split por la cadena completa encontrada)
            int count = sequence.split(Pattern.quote(target), -1).length - 1;
            return count;
        }
        return null;
    }

    /** Suma o resta días según el texto: "antes de" => resta ; "desde el" => suma */
    private LocalDate parseRelativeDateSmart(String txt) {
        Pattern p = Pattern.compile(
                "(\\d{1,3})\\s*d[ií]as.*?(antes de|desde el)\\s*:?\\s*(\\d{2}/\\d{2}/\\d{4})",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL
        );
        Matcher m = p.matcher(txt);
        if (m.find()) {
            int days = Integer.parseInt(m.group(1));
            String op = lower(m.group(2));
            LocalDate base = LocalDate.parse(m.group(3), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return op.contains("antes") ? base.minusDays(days) : base.plusDays(days);
        }
        return null;
    }

    /** "Escriba N veces la letra "X"" (comillas simples o dobles) */
    private String parseRepeatChars(String txt) {
        Pattern p = Pattern.compile(
                "Escriba\\s+(\\d{1,4})\\s+veces\\s+la\\s+letra\\s+[\"“”']?([^\"“”'\\s])[\"“”']?",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
        Matcher m = p.matcher(txt);
        if (m.find()) {
            int times = Integer.parseInt(m.group(1));
            String ch = m.group(2);
            return ch.repeat(times);
        }
        return null;
    }

    /** Expresión aritmética (long) con precedencia * / sobre + - */
    private Long parseMathResult(String txt) {
        Matcher m = Pattern.compile(
                "operaci[oó]n\\s+matem[aá]tica\\s*:\\s*([\\d\\s+\\-*/]+)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ).matcher(txt);
        if (m.find()) {
            String expr = m.group(1).replaceAll("\\s+","");
            return evalSimpleLong(expr);
        }
        return null;
    }

    private Long evalSimpleLong(String expr) {
        // tokenización sencilla con signo unario
        List<String> tokens = new ArrayList<>();
        StringBuilder num = new StringBuilder();
        for (int i=0;i<expr.length();i++) {
            char c = expr.charAt(i);
            if (c=='+'||c=='-'||c=='*'||c=='/') {
                if (num.length()==0 && (c=='+'||c=='-')) {
                    num.append(c); // signo unario
                } else {
                    tokens.add(num.toString()); num.setLength(0);
                    tokens.add(String.valueOf(c));
                }
            } else {
                num.append(c);
            }
        }
        if (num.length()>0) tokens.add(num.toString());

        // 1ª pasada: * /
        List<String> pass1 = new ArrayList<>();
        for (int i=0; i<tokens.size();) {
            if (i+1<tokens.size() && (tokens.get(i+1).equals("*") || tokens.get(i+1).equals("/"))) {
                long a = Long.parseLong(tokens.get(i));
                String op = tokens.get(i+1);
                long b = Long.parseLong(tokens.get(i+2));
                long r = op.equals("*") ? a*b : a/b;
                tokens.set(i+2, String.valueOf(r));
                i += 2;
            } else {
                pass1.add(tokens.get(i));
                i++;
            }
        }

        // 2ª pasada: + -
        long result = Long.parseLong(pass1.get(0));
        for (int j=1; j<pass1.size(); j+=2) {
            String op = pass1.get(j);
            long b = Long.parseLong(pass1.get(j+1));
            if (op.equals("+")) result += b; else result -= b;
        }
        return result;
    }
}