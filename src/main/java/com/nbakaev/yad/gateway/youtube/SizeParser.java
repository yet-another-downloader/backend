package com.nbakaev.yad.gateway.youtube;

public class SizeParser {

    private final static long KB_FACTOR = 1000;
    private final static long KIB_FACTOR = 1024;
    private final static long MB_FACTOR = 1000 * KB_FACTOR;
    private final static long MIB_FACTOR = 1024 * KIB_FACTOR;
    private final static long GB_FACTOR = 1000 * MB_FACTOR;
    private final static long GIB_FACTOR = 1024 * MIB_FACTOR;

    private static final char DOT = ".".charAt(0);

    public static long parse(String arg0) {
        var fileSizeFactorIndex = 0;

        for (; fileSizeFactorIndex < arg0.length(); fileSizeFactorIndex++) {
            var c = arg0.charAt(fileSizeFactorIndex);
            if (DOT == c || Character.isDigit(c)) {
                //
            } else {
                break;
            }
        }

        double ret = Double.parseDouble(arg0.substring(0, fileSizeFactorIndex));
        switch (arg0.substring(fileSizeFactorIndex)) {
            case "GB":
                return (long) (ret * GB_FACTOR);
            case "GiB":
                return (long) (ret * GIB_FACTOR);
            case "MB":
                return (long) (ret * MB_FACTOR);
            case "MiB":
                return (long) (ret * MIB_FACTOR);
            case "KB":
                return (long) (ret * KB_FACTOR);
            case "KiB":
                return (long) (ret * KIB_FACTOR);
        }
        return -1;
    }

}
