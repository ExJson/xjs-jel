package xjs.jel;

/**
 * A handful of modifiers providing alternate behaviors and QOL parsing
 * changes for JEL expressions.
 */
public final class JelFlags {

    private JelFlags() {}

    /**
     * This value is to be used only by the config itself and should
     * not be exposed to the parsing application.
     */
    public static final int VAR = 1;

    /**
     * This value has private scope and cannot be reexported to other
     * files.
     */
    public static final int PRIVATE = 1 << 1;

    /**
     * This value should only get added to an array.
     */
    public static final int ADD = 1 << 2;

    /**
     * This value should only get merged into an object and must be an
     * object.
     */
    public static final int MERGE = 1 << 3;

    /**
     * This value has been imported by some expression.
     */
    public static final int IMPORT = 1 << 4;

    /**
     * This value is a template expression.
     */
    public static final int TEMPLATE = 1 << 5;

    /**
     * This is an expanded class prototype expression.
     */
    public static final int CLASS = 1 << 6;

    /**
     * This field defines a list of possible values.
     */
    public static final int ENUM = 1 << 7;

    /**
     * This value may contain expression-like data, but should be
     * evaluated.
     */
    public static final int NOINLINE = 1 << 6;

    /**
     * In addition to containing a regular RHS expression, this field
     * contains a config-like "meta" descriptor of the expression.
     */
    public static final int META = 1 << 7;

    /**
     * Indicates that a field is only used as part of a destructuring
     * expression to create other values.
     */
    public static final int FROM = 1 << 8;

    /**
     * Indicates that a value has just been created and should not yet
     * get copied by reference expressions. This reduces the number of
     * clone operations that occur in the evaluator.
     */
    public static final int CREATED = 1 << 29;

    /**
     * Indicates that a field was generated as the result of some
     * non-literal expression.
     */
    public static final int GENERATED = 1 << 30;

    /**
     * This field does not have flags and is a valid candidate to be
     * overwritten.
     */
    public static final int NULL = 1 << 31;

}
