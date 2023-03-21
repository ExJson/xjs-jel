package xjs.jel.scope;

import xjs.jel.expression.Callable;

import java.util.List;

public interface CallableAccessor {
    Callable getCallable(final String key);
    List<String> callableKeys();
}
