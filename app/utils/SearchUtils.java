package utils;

import org.apache.commons.lang3.StringUtils;

public class SearchUtils {

    public static double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
        return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) /
                               (double) longerLength;
    }
}
