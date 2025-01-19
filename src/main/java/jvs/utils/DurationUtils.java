package jvs.utils;

import jvs.Constants;
import jvs.config.ConfigManager;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Duration utilities
 */
public class DurationUtils {

    private DurationUtils(){}

    /**
    * Formats an input duration into ISO8601 duration.
     * @param duration The duration
    */
    public static String formatToISO8601(final Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(
                "PT%dH%dM%dS",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    /**
     * Parses duration from input string.
     * @param input The input string formatted as HH:mm:ss.SSS
     * @return The parsed duration
     */
    public static Duration parseDuration(final String input) {
        Pattern pattern = ConfigManager.getConfig().getPattern(Constants.PATTERNS.DURATION);
        Matcher matcher = pattern.matcher(input);

        if (!matcher.matches()) {
            Logger.warn("Unable to parse duration from input string: " + input);
            return Duration.ofSeconds(0);
        }

        String group0 = matcher.group(1);
        String group1 = matcher.group(2);
        String group2 = matcher.group(3);
        String group3 = matcher.group(4);

        Integer hours = Integer.parseInt(group0);
        Integer minutes = Integer.parseInt(group1);
        Integer seconds = Integer.parseInt(group2);
        Integer millis = Integer.parseInt(group3);

        return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds).plusMillis(millis);
    }
}
