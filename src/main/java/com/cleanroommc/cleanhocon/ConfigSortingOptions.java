package com.cleanroommc.cleanhocon;

import java.math.BigInteger;
import java.util.*;

public final class ConfigSortingOptions {
    private ConfigSortingOptions() {
    }

    private static ConfigSorter overwrite;
    private static final ConfigSorter originalSorter;
    private static final ConfigSorter orderedSorter;

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

    static {
        originalSorter = new ConfigSorter() {
            @Override
            public Map getMapBacking() {
                return new HashMap<>();
            }

            @Override
            public Set getSetBacking() {
                return new HashSet<>();
            }

            @Override
            public Comparator<String> getRendererSorter() {
                return new Comparator<>() {
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
                };
            }

            @Override
            public String toString() {
                return "ConfigSorter: originalSorter,";
            }
        };

        orderedSorter = new ConfigSorter() {
            @Override
            public Map getMapBacking() {
                return new LinkedHashMap<>();
            }

            @Override
            public Set getSetBacking() {
                return new LinkedHashSet<>();
            }

            @Override
            public Comparator<String> getRendererSorter() {
                return null;
            }

            @Override
            public String toString() {
                return "ConfigSorter: orderedSorter,";
            }
        };
    }

}
