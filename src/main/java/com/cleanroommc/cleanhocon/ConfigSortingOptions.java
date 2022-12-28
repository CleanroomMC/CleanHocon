package com.cleanroommc.cleanhocon;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ConfigSortingOptions implements ConfigSorter {
    private static ConfigSorter overwrite;
    private static final ConfigSorter originalSorter = new ConfigSortingOptions(HashMap::new, RenderComparator::new);
    private static final ConfigSorter orderedSorter = new ConfigSortingOptions(LinkedHashMap::new, () -> null);
    private final Supplier<Map<?, ?>> backingMap;
    private final Supplier<Comparator<String>> rendererSorter;

    private ConfigSortingOptions(Supplier<Map<?, ?>> backingMap, Supplier<Comparator<String>> rendererSorter) {
        this.backingMap = backingMap;
        this.rendererSorter = rendererSorter;
    }

    public static ConfigSorter defaultSorter() {
        if (overwrite != null) {
            return overwrite;
        }
        return originalSorter;
    }

    public static ConfigSorter originalSorter() {
        return originalSorter;
    }

    public static ConfigSorter orderedSorter() {
        return orderedSorter;
    }

    public static void setOverwriteSorter(ConfigSorter sorter) {
        overwrite = sorter;
    }

    @Override
    public Map getMapBacking() {
        return backingMap.get();
    }

    @Override
    public Comparator<String> getRendererSorter() {
        return rendererSorter.get();
    }

    static final private class RenderComparator implements Comparator<String> {
        private static final long serialVersionUID = 1L;

        private static boolean isAllDigits(String s) {
            int length = s.length();

            // empty string doesn't count as a number
            // string longer than "max number of digits in a long" cannot be parsed as a long
            if (length == 0)
                return false;

            for (int i = 0; i < length; ++i) {
                char c = s.charAt(i);

                if (!Character.isDigit(c))
                    return false;
            }
            return true;
        }

        // This is supposed to sort numbers before strings,
        // and sort the numbers numerically. The point is
        // to make objects which are really list-like
        // (numeric indices) appear in order.
        @Override
        public int compare(String a, String b) {
            boolean aDigits = isAllDigits(a);
            boolean bDigits = isAllDigits(b);
            if (aDigits && bDigits) {
                return new BigInteger(a).compareTo(new BigInteger(b));
            } else if (aDigits) {
                return -1;
            } else if (bDigits) {
                return 1;
            } else {
                return a.compareTo(b);
            }
        }
    }

}
