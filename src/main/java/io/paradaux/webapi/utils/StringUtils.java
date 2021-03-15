package io.paradaux.webapi.utils;

import org.springframework.lang.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public class StringUtils {

    private static final Pattern VALID_EMAIL = Pattern.compile("(?:[a-z0-9!#$%&'*+\\/=?^_`{|}~-]+(?:\\"
            + ".[a-z0-9!#$%&'*+\\/=?^_`{|}~-]+)*|\""
            + "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01"
            + "-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)"
            + "+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:"
            + "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
            + "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:"
            + "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09"
            + "\\x0b\\x0c\\x0e-\\x7f])+)\\])");

    private static final char STRIKE_CONTROL_CHARACTER = '\u0336';

    private static final String SHORTHAND_CARDINAL_DIRECTIONS[] = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};

    /**
     * Converts the specified string to Title Case
     * <br>
     * Title Case is a text format whereby the first letter of every word is capitalised.
     * @param text The text you wish to convert to Title Case
     * @return Text In Title Case
     * */
    public static String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // In case input is in some form of snake case.
        text = text.replace("_", " ");

        StringBuilder converted = new StringBuilder();

        boolean convertNext = true;
        for (char ch : text.toCharArray()) {
            if (Character.isSpaceChar(ch)) {
                convertNext = true;
            } else if (convertNext) {
                ch = Character.toTitleCase(ch);
                convertNext = false;
            } else {
                ch = Character.toLowerCase(ch);
            }
            converted.append(ch);
        }

        return converted.toString();
    }

    /**
     * Checks against a regex pattern whether or not the email provided is valid.
     * @param email The Email you wish to verify is valid
     * @return Whether or not the email is valid
     * */
    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }

        return VALID_EMAIL.matcher(email.toLowerCase()).matches();
    }

    /**
     * Gets the domain (the section after the @ sign) from an email address.
     * @param email The Email you wish to get the domain of
     * @return The Domain of an email. Returns null if the email is invalid
     * */
    @Nullable
    public static String getEmailDomain(String email) {
        if (!isValidEmail(email)) {
            return null;
        }
        return email.substring(email.indexOf("@") + 1);
    }

    /**
     * Create a basic authentication header
     * */
    public static String basicAuth(String user, String pass) {
        return Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
    }

    /**
     * Create URL-encoded parameters.
     * */
    public static String urlEncode(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

}
