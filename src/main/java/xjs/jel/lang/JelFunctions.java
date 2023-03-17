package xjs.jel.lang;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;
import xjs.core.JsonArray;
import xjs.core.JsonFormat;
import xjs.core.JsonObject;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.jel.JelContext;
import xjs.jel.Privilege;
import xjs.jel.exception.IllegalJelArgsException;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Callable;
import xjs.jel.expression.Expression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static xjs.jel.expression.LiteralExpression.of;
import static xjs.jel.expression.LiteralExpression.ofNull;

public final class JelFunctions {
    private static final Map<String, Function> FUNCTIONS = new ConcurrentHashMap<>();
    private static final Random RAND = new Random();

    static {
        register("dir", JelFunctions::dir);
        register("min", JelFunctions::min);
        register("max", JelFunctions::max);
        register("rand", JelFunctions::rand);
        register("size", JelFunctions::size);
        register("hash", JelFunctions::hash);
        register("startsWith", JelFunctions::startsWith);
        register("endsWith", JelFunctions::endsWith);
        register("endsWith", JelFunctions::endsWith);
        register("contains", JelFunctions::contains);
        register("has", JelFunctions::has);
        register("replace", JelFunctions::replace);
        register("matches", JelFunctions::matches);
        register("lowercase", JelFunctions::lowercase);
        register("uppercase", JelFunctions::uppercase);
        register("coalesce", Privilege.EXPERIMENTAL, JelFunctions::coalesce);
        register("orElse", JelFunctions::orElse);
        register("find", Privilege.EXPERIMENTAL, JelFunctions::find);
        register("range", Privilege.EXPERIMENTAL, JelFunctions::range);
        register("round", JelFunctions::round);
        register("file", Privilege.IO, JelFunctions::file);
        register("type", JelFunctions::type);
        register("pretty", JelFunctions::pretty);
        register("parse", JelFunctions::parse);
        register("isString", JelFunctions::isString);
        register("isNumber", JelFunctions::isNumber);
        register("isBool", JelFunctions::isBool);
        register("isContainer", JelFunctions::isContainer);
        register("isPrimitive", JelFunctions::isPrimitive);
        register("isArray", JelFunctions::isArray);
        register("isObject", JelFunctions::isObject);
        register("isNull", JelFunctions::isNull);
        register("string", JelFunctions::string);
        register("number", JelFunctions::number);
        register("bool", JelFunctions::bool);
        register("array", JelFunctions::array);
        register("object", JelFunctions::object);
    }

    public static @Nullable Callable lookup(final String name) {
        return lookup(Privilege.ALL, name);
    }

    public static @Nullable Callable lookup(
            final @MagicConstant(flagsFromClass = Privilege.class) int privilege,
            final String name) {
        final Function f = FUNCTIONS.get(name);
        if (f == null) {
            return null;
        } else if ((f.privilege & privilege) != f.privilege) {
            return (self, ctx, values) -> {
                throw new JelException("call is disallowed (unprivileged access): " + name);
            };
        }
        return f.function;
    }

    public static void register(final String name, final Callable function) {
        register(name, Privilege.NONE, function);
    }

    public static void register(
            final String name,
            final @MagicConstant(flagsFromClass = Privilege.class) int privilege,
            final Callable function) {
        FUNCTIONS.put(name, new Function(function, privilege));
    }

    private static class Function {
        final Callable function;
        final int privilege;

        private Function(final Callable function, final int privilege) {
            this.function = function;
            this.privilege = privilege;
        }
    }

    public static Expression dir(
            final JsonValue self, final JelContext ctx, final JsonValue... args) {
        final JsonArray a = new JsonArray();
        FUNCTIONS.keySet().forEach(a::add);
        return of(a);
    }

    public static Expression min(
            final JsonValue self, final JelContext ctx, final JsonValue... args) {
        if (args.length == 0) {
            if (!self.isContainer() || self.asContainer().size() == 0) {
                return ofNull();
            }
            return min(self.asContainer().values());
        } else if (args.length == 1 && args[0].isArray()) {
            return min(args[0].asArray());
        }
        return min(Arrays.asList(args));
    }

    private static Expression min(final Iterable<JsonValue> values) {
        double min = Double.MAX_VALUE;
        for (final JsonValue v : values) {
            min = Math.min(min, v.intoDouble());
        }
        return of(min);
    }

    public static Expression max(
            final JsonValue self, final JelContext ctx, final JsonValue... args) {
        if (args.length == 0) {
            if (!self.isContainer() || self.asContainer().size() == 0) {
                return ofNull();
            }
            return max(self.asContainer().values());
        } else if (args.length == 1 && args[0].isArray()) {
            return max(args[0].asArray());
        }
        return max(Arrays.asList(args));
    }

    private static Expression max(final Iterable<JsonValue> values) {
        double max = Double.MIN_VALUE;
        for (final JsonValue v : values) {
            max = Math.max(max, v.intoDouble());
        }
        return of(max);
    }

    public static Expression rand(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        requireArgs(0, 2, args);
        if (args.length == 0) {
            return of(RAND.nextDouble());
        } else if (args.length == 1) {
            if (args[0].isArray()) {
                return rand(args[0].asArray());
            }
            return of(RAND.nextInt(args[0].intoInt()));
        }
        int min = args[0].intoInt();
        int max = args[1].intoInt();
        if (min < 0 || max < 0) {
            throw new IllegalJelArgsException("less than 0: " + min + "..." + max);
        }
        final int num = min < max
            ? min + RAND.nextInt(max - min)
            : max + RAND.nextInt(min - max);
        return of(num);
    }

    private static Expression rand(final JsonArray array) {
        final int idx = RAND.nextInt(array.size());
        return of(array.get(idx));
    }

    public static Expression size(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, JelFunctions::size);
    }

    private static Expression size(final JsonValue value) {
        if (value.isContainer()) {
            return of(value.asContainer().size());
        } else if (value.isString()) {
            return of(value.asString().length());
        }
        return of(value.intoDouble());
    }

    public static Expression hash(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.hashCode()));
    }
    
    public static Expression startsWith(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        requireArgs(1, 1, args);
        if (self.isArray()) {
            return startsWith(self.asArray(), args[0].intoArray());
        } else if (self.isObject()) {
            throw new IllegalJelArgsException("unsupported type: object");
        }
        return of(self.intoString().startsWith(args[0].intoString()));
    }
    
    private static Expression startsWith(final JsonArray lhs, final JsonArray rhs) {
        if (rhs.size() > lhs.size()) {
            return of(false);
        }
        for (int i = 0; i < rhs.size(); i++) {
            if (!lhs.get(i).matches(rhs.get(i))) {
                return of(false);
            }
        }
        return of(true);
    }

    public static Expression endsWith(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        requireArgs(1, 1, args);
        if (self.isArray()) {
            return endsWith(self.asArray(), args[0].intoArray());
        } else if (self.isObject()) {
            throw new IllegalJelArgsException("unsupported type: object");
        }
        return of(self.intoString().endsWith(args[0].intoString()));
    }

    private static Expression endsWith(final JsonArray lhs, final JsonArray rhs) {
        if (rhs.size() > lhs.size()) {
            return of(false);
        }
        for (int i = lhs.size(); i >= 0; i--) {
            if (!lhs.get(i).matches(rhs.get(i))) {
                return of(false);
            }
        }
        return of(true);
    }

    public static Expression contains(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        requireArgs(1, 1, args);
        if (self.isContainer()) {
            return of(self.asContainer().contains(args[0]));
        }
        return of(self.intoString().contains(args[0].intoString()));
    }

    public static Expression has(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        requireArgs(1, 1, args);
        if (self.isObject()) {
            return of(self.asObject().has(args[0].intoString()));
        }
        return of(false);
    }

    public static Expression replace(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        requireArgs(2, 2, args);
        if (self.isContainer()) {
            throw new IllegalJelArgsException("unsupported type: container");
        }
        return of(self.intoString()
            .replaceAll(args[0].intoString(), args[1].intoString()));
    }

    // overridden by class expressions
    public static Expression matches(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        requireArgs(1, 1, args);
        return of(self.intoString().matches(args[0].intoString()));
    }

    public static Expression uppercase(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.intoString().toUpperCase()));
    }

    public static Expression lowercase(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.intoString().toLowerCase()));
    }

    public static Expression coalesce(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        requireArgs(1, 32, args);
        for (final JsonValue v : args) {
            if (!v.isNull()) {
                return of(v);
            }
        }
        return ofNull();
    }

    public static Expression orElse(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        requireArgs(1, 1, args);
        if (self.isNull()) {
            return of(args[0]);
        }
        return of(self);
    }

    public static Expression find(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        requireArgs(1, 1, args);
        if (args[0] instanceof CallableFacade) {
            final Callable c = ((CallableFacade) args[0]).getWrapped();
            for (final JsonReference r : self.intoArray().references()) {
                if (c.call(self, ctx, r.getOnly()).apply(ctx).intoBoolean()) {
                    return of(r.get());
                }
            }
            return ofNull();
        }
        if (!args[0].isObject()) {
            throw new JelException("find: argument must be an object or callable, called on an array");
        }
        final JsonObject matcher = args[0].asObject();
        for (final JsonReference r : self.intoArray().references()) {
            final JsonValue v = r.getOnly();
            if (v.isObject() && matches(v.asObject(), matcher)) {
                return of(r.get());
            }
        }
        return ofNull();
    }

    private static boolean matches(final JsonObject lhs, final JsonObject rhs) {
        for (final JsonObject.Member m : rhs) {
            final JsonValue actual = lhs.get(m.getKey());
            if (actual == null || !actual.matches(m.getOnly())) {
                return false;
            }
        }
        return true;
    }

    // experimental because we may add a true range (that does not expand into an array)
    public static Expression range(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        requireArgs(1, 2, args);
        final int min;
        final int max;
        if (args.length == 1) {
            min = 0;
            max = args[0].intoInt();
        } else {
            min = args[0].intoInt();
            max = args[1].intoInt();
        }
        final JsonArray array = new JsonArray(new ArrayList<>(max - min));
        for (int i = min; i < max; i++) {
            array.add(i);
        }
        return of(array);
    }

    public static Expression round(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        requireArgs(1, 2, args);
        final double num;
        final int places;
        if (args.length == 1) {
            num = self.intoDouble();
            places = args[0].intoInt();
        } else {
            num = args[0].intoDouble();
            places = args[1].intoInt();
        }
        final double pow = Math.pow(10, places);
        return of(Math.round(num * pow) / pow);
    }

    public static Expression file(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        throw new JelException("unimplemented").withDetails("This feature is still in design");
    }

    public static Expression type(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.getType().name().toLowerCase()));
    }

    public static Expression pretty(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.toString(JsonFormat.XJS)));
    }

    public static Expression parse(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v ->
            of(v.isString() ? ctx.eval(ctx.getSequencer().parse(v.asString())) : v));
    }

    public static Expression isString(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.isString()));
    }

    public static Expression isNumber(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.isNumber()));
    }

    public static Expression isBool(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.isBoolean()));
    }

    public static Expression isContainer(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.isContainer()));
    }

    public static Expression isPrimitive(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.isPrimitive()));
    }

    public static Expression isArray(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.isArray()));
    }

    public static Expression isObject(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.isObject()));
    }

    public static Expression isNull(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.isNull()));
    }

    public static Expression string(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.intoString()));
    }

    public static Expression number(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.intoDouble()));
    }

    public static Expression bool(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.intoBoolean()));
    }

    public static Expression array(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.intoArray()));
    }

    public static Expression object(
            final JsonValue self, final JelContext ctx, final JsonValue... args) throws JelException {
        return instanceMethodOrSingleArg(self, args, v -> of(v.intoObject()));
    }

    public static Expression instanceMethodOrSingleArg(
            final JsonValue self, JsonValue[] args, final ExpressionFunction f) throws JelException {
        requireArgs(0, 1, args);
        if (args.length == 0) {
            return f.apply(self);
        }
        return f.apply(args[0]);
    }

    public static void requireArgs(
            final int min, final int max, final JsonValue[] args) throws JelException {
        if (args.length < min || args.length > max) {
            if (min == max) {
                throw new IllegalJelArgsException("function expected " + min + " arguments(s)");
            }
            throw new IllegalJelArgsException("function expected " + min + ".." + max + " argument(s)");
        }
    }

    @FunctionalInterface
    private interface ExpressionFunction {
        Expression apply(final JsonValue value) throws JelException;
    }
}
