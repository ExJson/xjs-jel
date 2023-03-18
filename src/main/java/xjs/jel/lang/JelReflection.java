package xjs.jel.lang;

import org.jetbrains.annotations.Nullable;
import xjs.core.JsonContainer;
import xjs.core.JsonObject;
import xjs.core.JsonReference;
import xjs.jel.JelMember;
import xjs.jel.expression.Callable;
import xjs.jel.scope.Scope;

import java.util.ArrayList;
import java.util.List;

public final class JelReflection {

    private JelReflection() {}

    public static int getSize(final JsonContainer container) {
        if (container instanceof JelContainer) {
            return ((JelContainer) container).declaredSize();
        }
        return container.size();
    }

    public static int getSize(final JelContainer container) {
        return container.declaredSize();
    }

    public static JsonReference getReference(final JsonContainer container, final int i) {
        if (container instanceof JelContainer) {
            return ((JelContainer) container).getDeclaredReference(i);
        }
        return container.getReference(i);
    }

    public static JsonReference getReference(final JelContainer container, final int i) {
        return container.getDeclaredReference(i);
    }

    public static @Nullable JsonReference getReference(final JsonObject object, final String key) {
        if (object instanceof JelObject) {
            return ((JelObject) object).getDeclaredReference(key);
        }
        return object.getReference(key);
    }

    public static @Nullable JsonReference getReference(final JelObject object, final String key) {
        return object.getDeclaredReference(key);
    }

    public static @Nullable Callable getCallable(final JsonObject object, final String key) {
        if (object instanceof JelObject) {
            return ((JelObject) object).getCallable(key);
        }
        return null;
    }

    public static @Nullable Callable getCallable(final JelObject object, final String key) {
        return object.getCallable(key);
    }

    public static List<JelMember> jelMembers(final JsonObject object) {
        if (object instanceof JelObject) {
            return ((JelObject) object).jelMembers();
        }
        final List<JelMember> members = new ArrayList<>();
        for (final JsonObject.Member member : object) {
            // todo: leaking access
            members.add(JelMember.of(member.getKey(), member.getOnly()));
        }
        return members;
    }

    public static List<JelMember> jelMembers(final JelObject object) {
        return object.jelMembers();
    }

    public static void copyInto(final JsonObject object, final Scope scope) {
        if (object instanceof JelObject) {
            ((JelObject) object).copyInto(scope);
        } else {
            for (final JsonObject.Member member : object) {
                scope.add(member.getKey(), member.getReference());
            }
        }
    }

    public static void copyInto(final JelObject object, final Scope scope) {
        object.copyInto(scope);
    }

    public static List<String> declaredKeys(final JsonObject object) {
        if (object instanceof JelObject) {
            return ((JelObject) object).declaredKeys();
        }
        return object.keys();
    }

    public static List<String> declaredKeys(final JelObject object) {
        return object.declaredKeys();
    }
}
