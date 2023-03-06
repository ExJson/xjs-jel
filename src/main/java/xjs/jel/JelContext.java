package xjs.jel;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;
import xjs.core.JsonContainer;
import xjs.core.JsonLiteral;
import xjs.core.JsonValue;
import xjs.exception.SyntaxException;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.scope.Scope;
import xjs.jel.sequence.Sequence;
import xjs.jel.serialization.sequence.Sequencer;
import xjs.serialization.token.TokenStream;
import xjs.serialization.token.TokenType;
import xjs.serialization.token.Tokenizer;
import xjs.serialization.util.PositionTrackingReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class JelContext {
    public static final JelContext GLOBAL_CONTEXT =
        new JelContext(new File("/"));
    private static final JsonValue ERRED_VALUE =
        JsonLiteral.jsonNull();

    public final Map<File, Scope> globalScope;
    private final Map<String, JsonValue> fileMap;
    private final Map<String, JelException> errorMap;
    private final Deque<Scope> scopeQueue;
    private final Deque<JsonContainer> parentQueue;
    private final Set<String> inProgress;
    private final File root;
    private Sequencer sequencer;
    private @Nullable Logger log;
    private @Nullable JsonContainer parent;
    private @Nullable File current;
    private boolean outputPrefix;
    private boolean strictPathing;
    private Scope scope;
    private int privilege;

    public JelContext(final File root) {
        this(root, null);
    }

    public JelContext(final File root, final @Nullable Logger log) {
        this.globalScope = new HashMap<>();
        this.fileMap = new HashMap<>();
        this.errorMap = new HashMap<>();
        this.scopeQueue = new ArrayDeque<>();
        this.parentQueue = new ArrayDeque<>();
        this.inProgress = new HashSet<>();
        this.root = root;
        this.sequencer = Sequencer.JEL;
        this.log = log;
        this.outputPrefix = true;
        this.scope = new Scope();
        this.privilege = Privilege.BASIC;
    }

    public void setLog(final @Nullable Logger log) {
        this.log = log;
    }

    @MagicConstant(flagsFromClass = Privilege.class)
    public int getPrivilege() {
        return this.privilege;
    }

    public void setPrivilege(
            final @MagicConstant(flagsFromClass = Privilege.class) int privilege) {
        this.privilege = privilege;
    }

    public void setOutputPrefix(final boolean outputPrefix) {
        this.outputPrefix = outputPrefix;
    }

    public boolean isStrictPathing() {
        return this.strictPathing;
    }

    public void setStrictPathing(final boolean strictPathing) {
        this.strictPathing = strictPathing;
    }

    public Sequencer getSequencer() {
        return this.sequencer;
    }

    public void setSequencer(final Sequencer sequencer) {
        this.sequencer = sequencer;
    }

    public void loadAll() {
        throw new UnsupportedOperationException("todo");
    }

    public void pushCapture(final Scope capture) {
        this.scopeQueue.add(this.scope);
        this.scope = capture;
    }

    public void dropCapture() {
        this.scope = this.scopeQueue.remove();
    }

    public Scope getScope() {
        return this.scope;
    }

    public void pushParent(final JsonContainer parent) {
        if (this.parent != null) {
            this.parentQueue.add(this.parent);
        }
        this.parent = parent;
    }

    public void dropParent() {
        if (this.parentQueue.isEmpty()) {
            this.parent = null;
        } else {
            this.parent = this.parentQueue.remove();
        }
    }

    public JsonContainer getParent() {
        if (this.parent == null) {
            throw new IllegalStateException("no parents in context");
        }
        return this.parent;
    }

    public JsonValue peekParent() {
        if (this.parent == null) {
            return JsonLiteral.jsonNull();
        }
        return this.parent;
    }

    public JsonContainer getRoot() {
        if (this.parent == null) {
            throw new IllegalStateException("no parents in context");
        } else if (this.parentQueue.isEmpty()) {
            return this.parent;
        }
        return this.parentQueue.getLast();
    }

    public JsonValue eval(final Sequence<?> sequence) throws JelException {
        if (sequence instanceof Expression) {
            return ((Expression) sequence).apply(this);
        }
        throw new JelException("not an expression").withSpan(sequence);
    }

    public boolean isLoading(final String path) {
        final File file = new File(this.root, path);
        return this.inProgress.contains(file.getAbsolutePath());
    }

    public JsonValue getImport(final String path) throws JelException {
        final File file = new File(this.root, path);
        final JsonValue out = this.getOrLoadFile(file);
        if (out == null) {
            throw new JelException("File not found: " + path);
        } else if (out == ERRED_VALUE) {
            final JelException e = this.errorMap.get(file.getAbsolutePath());
            throw new JelException("Dependency not loaded: " + path, e);
        }
        return out;
    }

    public @Nullable JsonValue getOutput(final File file) {
        final JsonValue out = this.getOrLoadFile(file);
        if (out == ERRED_VALUE) {
            return null;
        }
        return out;
    }

    private JsonValue getOrLoadFile(final File file) {
        final String path = file.getAbsolutePath();
        JsonValue value = this.fileMap.get(path);
        if (value != null) {
            return value;
        }
        value = this.loadFile(file);
        this.fileMap.put(path, value);
        return value;
    }

    public JsonValue loadFile(final File file) {
        final String path = file.getAbsolutePath();
        if (!this.inProgress.add(path)) {
            throw new IllegalStateException("Unhandled cyclical reference: " + file);
        }
        PositionTrackingReader reader = null;
        try (final FileInputStream fis = new FileInputStream(file)) {
            reader = PositionTrackingReader.fromIs(fis, true);
            final TokenStream stream =
                new TokenStream(new Tokenizer(reader), TokenType.OPEN);
            final JsonValue out = this.eval(
                this.sequencer.parse(Tokenizer.containerize(stream)));
            this.inProgress.remove(path);
            return out;
        } catch (final IOException e) {
            this.errorMap.put(path, new JelException("Cannot read file", e));
        } catch (final SyntaxException e) {
            if (reader == null) {
                throw new IllegalStateException("unreachable");
            }
            this.errorMap.put(path,
                JelException.fromSyntaxException(reader.getFullText().toString(), e));
        } catch (final JelException e) {
            this.errorMap.put(path, e);
        }
        this.inProgress.remove(path);
        return ERRED_VALUE;
    }

    public void addOutput(final File file, final JsonValue value) {
        this.fileMap.put(file.getAbsolutePath(), value);
    }

    public void log(final String s) {
        if (this.log != null) {
            final LogRecord lr = new LogRecord(Level.INFO, s);
            if (this.outputPrefix) {
                lr.setSourceClassName(this.getFilename());
            }
            this.log.log(lr);
        } else {
            System.out.println(this.getPrefix(false) + s);
        }
    }

    public void error(final String s) {
        if (this.log != null) {
            final LogRecord lr = new LogRecord(Level.SEVERE, s);
            if (this.outputPrefix) {
                lr.setSourceClassName(this.getFilename());
            }
            this.log.log(lr);
        } else {
            System.err.println(this.getPrefix(true) + s);
        }
    }

    private String getPrefix(final boolean error) {
        if (!this.outputPrefix) {
            return "";
        }
        final String level = error ? "ERROR" : "INFO";
        return String.format("[%s] [%s]: ", level, this.getFilename());
    }

    protected String getFilename() {
        return this.current != null ? this.current.getName() : "evaluator";
    }

    public void dispose() {
        this.globalScope.clear();
        this.fileMap.clear();
        this.errorMap.clear();
        this.scopeQueue.clear();
        this.scope.dispose();
    }
}
