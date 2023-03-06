package xjs.jel;

public final class Privilege {

    private Privilege() {}

    public static final int NONE = 0;

    public static final int IMPORTS = 1;

    public static final int EXPORTS = 1 << 1;

    public static final int IO = IMPORTS | EXPORTS;

    public static final int LOGGING = 1 << 2;

    public static final int UTILS = 1 << 3;

    public static final int BASIC = IO | LOGGING | UTILS;

    public static final int EXPERIMENTAL = 1 << 4;

    public static final int ALL = Integer.MAX_VALUE;
}
