package com.evalart.automation.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class RandomData {

    public static String randomText(int len) {
        return RandomStringUtils.randomAlphabetic(len);
    }

    public static String randomNumeric(int min, int max) {
        int n = ThreadLocalRandom.current().nextInt(min, max + 1);
        return String.valueOf(n);
    }

    public static String todayISO() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE); // yyyy-MM-dd
    }

    public static String randomOptionIndex(int optionsCount) {
        // Elige la primera opción distinta de "Seleccione..." si existe
        // Por defecto selecciona índice 1 si hay al menos 2 opciones
        if (optionsCount > 1) return "1";
        return "0";
    }
}
