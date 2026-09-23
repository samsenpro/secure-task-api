package com.example.secureapi.common;

import java.util.Locale;

public final class Emails {

    private Emails() {
    }

    /**
     * Normaliza el email para que la unicidad no dependa de mayúsculas o espacios.
     */
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
