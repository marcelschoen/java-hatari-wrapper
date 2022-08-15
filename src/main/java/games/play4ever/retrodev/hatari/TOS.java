package games.play4ever.retrodev.hatari;

import java.util.Locale;

/**
 * TOS versions included in this Java wrapper.
 */
public enum TOS {
    etos512cz,
    etos512de,
    etos512es,
    etos512fi,
    etos512fr,
    etos512gr,
    etos512hu,
    etos512it,
    etos512nl,
    etos512no,
    etos512pl,
    etos512ru,
    etos512se,
    etos512sg,
    etos512tr,
    etos512uk,
    etos512us,
    tos100,
    tos102,
    tos104,
    tos106,
    tos205,
    tos206,
    tos306,
    tos402,
    tos404;

    /**
     * Tries to return the EmuTOS version which corresponds to the current locale language
     * or country code value of the running JVM.
     *
     * @return The EmuTOS matching the current language / country, or the US version (fallback).
     */
    public static TOS getEmuTOSByLocale() {
        // 1st try by language code, e.g. "de", "it"
        TOS value = getLocalizedEmuTOS(Locale.getDefault().getLanguage().toLowerCase());
        if(value == null) {
            // If that fails, try country code (e.g. "uk", "us")
            value = getLocalizedEmuTOS(Locale.getDefault().getCountry().toLowerCase());
        }
        return value == null ? etos512us : value;
    }

    /**
     * Tries to return the EmuTOS version which corresponds to the given language
     * or country code value.
     *
     * @param code The 2-character country or language code
     * @return The EmuTOS matching the current language, or the US version (fallback).
     */
    public static TOS getEmuTOSByCountryOrLanguage(String code) {
        return getLocalizedEmuTOS(code);
    }

    private static TOS getLocalizedEmuTOS(String code) {
        if(code.trim().length() != 2) {
            throw new IllegalArgumentException("Parameter code must be a 2-character value: " +  code);
        }
        String tosName = "etos512" + code;
        try {
            return valueOf(tosName);
        } catch(Exception e) {
            // ignore
        }
        return null;
    }
}
