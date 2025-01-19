package jvs.utils;

public class Utils {

    /**
     * Round the input double to the closest even number
     * @param input The input double.
     * @return The integer that represent the closest even number to the input double.
     */
    public static int roundEven(final double input) {
        return (int)(Math.round(input / 2) * 2);
    }

    /**
     * Trims all trailing occurrences of the input suffix.
     * @param str The input string.
     * @param suffix The suffix to remove.
     * @return The sanitized string.
     */
    public static String trimTrailingSuffix(String str, final String suffix) {
        if (str.isEmpty()) {
            return str;
        }

        while (str.endsWith(suffix) && !str.isEmpty()) {
            int indexOfLast = str.lastIndexOf(suffix);
            if (indexOfLast >= 0 && indexOfLast < str.length()) {
                str = str.substring(0, indexOfLast);
            }
        }

        return str;
    }
}
