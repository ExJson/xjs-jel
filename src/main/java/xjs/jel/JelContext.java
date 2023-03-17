package xjs.jel;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;
import xjs.core.JsonContainer;
import xjs.core.JsonLiteral;
import xjs.core.JsonReference;
import xjs.core.JsonValue;
import xjs.exception.SyntaxException;
import xjs.jel.exception.JelException;
import xjs.jel.expression.Expression;
import xjs.jel.scope.Scope;
import xjs.jel.sequence.Sequence;
import xjs.jel.serialization.sequence.Sequencer;
import xjs.serialization.JsonContext;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.TokenStream;
import xjs.serialization.token.TokenType;
import xjs.serialization.token.Tokenizer;
import xjs.serialization.util.PositionTrackingReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class JelContext {
    public static final JelContext GLOBAL_CONTEXT =
        new JelContext(new File("/"));

    private final Map<String, Output> outputMap;
    private final Stack<Scope> scopeStack;
    private final Stack<JsonContainer> parentStack;
    private final Set<String> inProgress;
    private final Stack<File> filesInProgress;
    private final File root;
    private Sequencer sequencer;
    private @Nullable Logger log;
    private @Nullable JsonContainer parent;
    private boolean outputPrefix;
    private boolean strictPathing;
    private final Scope globalScope;
    private Scope scope;
    private int privilege;
    private int folderDepth;

    public JelContext(final @Nullable File root) {
        this(root, null);
    }

    public JelContext(
            final @Nullable File root, final @Nullable Logger log) {
        this.outputMap = new HashMap<>();
        this.scopeStack = new Stack<>();
        this.parentStack = new Stack<>();
        this.inProgress = new HashSet<>();
        this.filesInProgress = new Stack<>();
        this.root = root != null ? root : new File(System.getProperty("user.dir"));
        this.sequencer = Sequencer.JEL;
        this.log = log;
        this.outputPrefix = true;
        this.globalScope = new Scope();
        this.scope = this.globalScope;
        this.privilege = Privilege.BASIC;
        this.folderDepth = this == GLOBAL_CONTEXT || isGlobal(root) ? 1 : 8;
    }

    private static boolean isGlobal(final @Nullable File root) {
        if (root == null) return false;
        return root.getAbsolutePath().equals(new File("/").getAbsolutePath());
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

    public Scope getGlobalScope() {
        return this.globalScope;
    }

    public void defineGlobal(final String key, final JsonValue value) {
        this.defineGlobal(key, value, false);
    }

    public void defineGlobal(final String key, final JsonValue value, final boolean mutable) {
        if (mutable) {
            this.globalScope.add(new JsonReference(value));
            return;
        }
        if (value instanceof JsonContainer) {
            ((JsonContainer) value).freeze(true);
        }
        this.globalScope.add(key, new JsonReference(value).freeze());
    }

    public Sequencer getSequencer() {
        return this.sequencer;
    }

    public void setSequencer(final Sequencer sequencer) {
        this.sequencer = sequencer;
    }

    public void setFolderDepth(final int folderDepth) {
        this.folderDepth = folderDepth;
    }

    public void loadAll() {
        this.loadRecursive(this.folderDepth, this.root);
    }

    private void loadRecursive(final int depth, final File dir) {
        if (depth > this.folderDepth) {
            return;
        }
        final File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (final File f : files) {
            if (f.isDirectory()) {
                this.loadRecursive(depth + 1, f);
            } else {
                this.getOrLoadFile(f, Privilege.ALL);
            }
        }
    }

    public void pushScope(final Scope capture) {
        this.scopeStack.add(this.scope);
        this.scope = capture;
    }

    public void dropScope() {
        this.scope = this.scopeStack.pop();
    }

    public Scope getScope() {
        return this.scope;
    }

    public void pushParent(final JsonContainer parent) {
        if (this.parent != null) {
            this.parentStack.add(this.parent);
        }
        this.parent = parent;
    }

    public void dropParent() {
        if (this.parentStack.isEmpty()) {
            this.parent = null;
        } else {
            this.parent = this.parentStack.pop();
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
        } else if (this.parentStack.isEmpty()) {
            return this.parent;
        }
        return this.parentStack.firstElement();
    }

    public Sequence<?> parse(
            final String path, final ContainerToken tokens) throws JelException {
        try {
            return this.sequencer.parse(tokens);
        } catch (final JelException e) {
            throw e.remapSpans(path);
        }
    }

    public JsonValue eval(final Sequence<?> sequence) throws JelException {
        return this.eval(null, sequence);
    }

    public JsonValue eval(
            final @Nullable String path, final Sequence<?> sequence) throws JelException {
        if (sequence instanceof Expression) {
            this.pushScope(this.globalScope.captureWithPath(path));
            try {
                return ((Expression) sequence).apply(this);
            } finally {
                this.dropScope();
            }
        }
        throw new JelException("not an expression").withSpan(sequence);
    }

    public boolean isLoading(final String path) {
        final File file = new File(this.root, path);
        return this.inProgress.contains(file.getAbsolutePath());
    }

    public JsonValue getImport(final String path) throws JelException {
        final File file = this.resolveFile(path);
        if (file == null) {
            throw new JelException("File not found: " + path);
        }
        final Output out = this.getOrLoadFile(file, this.privilege);
        if (out == null) {
            throw new JelException("Unprivileged access")
                .withDetails(this.getFilename() + " does not have privilege to import or read files.");
        } else if (out.isError()) {
            throw new JelException("Dependency not loaded: " + path, out.getThrown());
        }
        return out.getValue();
    }

    private @Nullable File resolveFile(final String path) {
        if (!this.filesInProgress.isEmpty()) {
            final File loading = this.filesInProgress.peek();
            File relative = new File(loading.getParent(), path);
            if (relative.exists() && relative.isFile()) {
                return relative;
            }
            relative = this.fileInRoot(path);
            if (relative != null) {
                return relative;
            }
            final String rootPath = this.root.getAbsolutePath();
            File parent = loading.getParentFile();
            while (rootPath.length() < parent.getAbsolutePath().length()) {
                relative = new File(loading.getParent(), path);
                if (relative.exists() && relative.isFile()) {
                    return relative;
                }
                parent = parent.getParentFile();
            }
        }
        return this.fileInRoot(path);
    }

    private @Nullable File fileInRoot(final String path) {
        final File relative = new File(this.root, path);
        if (this.outputMap.containsKey(relative.getAbsolutePath())) {
            return relative; // added manually
        }
        if (relative.exists() && relative.isFile()) {
            return relative;
        }
        return null;
    }

    public @Nullable JsonValue getOutput(final File file) {
        final Output out = this.getOrLoadFile(file, Privilege.ALL);
        if (out != null) {
            return out.getValue();
        }
        return null;
    }

    private @Nullable Output getOrLoadFile(final File file, final int privilege) {
        final String path = file.getAbsolutePath();
        Output output = this.outputMap.get(path);
        if (output != null) {
            return output;
        }
        if ((Privilege.IMPORTS & privilege) != Privilege.IMPORTS) {
            return null;
        }
        output = this.loadFile(file);
        this.outputMap.put(path, output);
        return output;
    }

    public Output loadFile(final File file) {
        final String path = file.getAbsolutePath();
        // for now, jel is only parsed in xjs files.
        if (!file.getName().endsWith(".xjs")) {
            return this.parseNonXjs(file);
        }
        if (!this.inProgress.add(path)) {
            throw new IllegalStateException("Unhandled cyclical reference: " + file);
        }
        this.filesInProgress.push(file);
        PositionTrackingReader reader = null;
        JsonValue value = null;
        JelException thrown = null;
        try (final FileInputStream fis = new FileInputStream(file)) {
            reader = PositionTrackingReader.fromIs(fis, true);
            final TokenStream stream =
                new TokenStream(new Tokenizer(reader), TokenType.OPEN);
            final ContainerToken tokens =
                Tokenizer.containerize(stream);
            value = this.eval(path, this.parse(path, tokens));
        } catch (final IOException e) {
            thrown = new JelException("Cannot read file", e);
        } catch (final SyntaxException e) {
            if (reader == null) {
                throw new IllegalStateException("unreachable");
            }
            thrown = JelException.fromSyntaxException(reader.getFullText().toString(), e);
        } catch (final JelException e) {
            thrown = e;
        }
        this.inProgress.remove(path);
        final String fullText = reader != null ? reader.getFullText().toString() : null;
        return new Output(value, thrown, fullText);
    }

    private Output parseNonXjs(final File f) {
        JsonValue value = null;
        JelException thrown = null;
        try {
            value = JsonContext.autoParse(f);
        } catch (final IOException e) {
            thrown = new JelException("Cannot read file", e);
        } catch (final SyntaxException e) {
            thrown = new JelException("File contains syntax errors", e);
        }
        return new Output(value, thrown, null);
    }

    public void addOutput(final File file, final JsonValue value) {
        this.outputMap.put(file.getAbsolutePath(), new Output(value, null, null));
    }

    public String getRelativePath(final String path) {
        final String rootPath = this.root.getAbsolutePath();
        if (path.startsWith(rootPath)) {
            return path.substring(rootPath.length() + 1).replace("\\", "/");
        }
        return new File(path).getName();
    }

    public @Nullable String getFullText(final String absolutePath) {
        final Output output = this.outputMap.get(absolutePath);
        return output != null ? output.getFullText() : null;
    }

    public @Nullable JelException getError(final String path) {
        final File f = this.resolveFile(path);
        if (f == null) {
            throw new IllegalArgumentException("file not found: " + path);
        }
        return this.getError(f);
    }

    public @Nullable JelException getError(final File file) {
        final Output output = this.outputMap.get(file.getAbsolutePath());
        return output != null ? output.getThrown() : null;
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
        final String path = this.scope.getFilePath();
        if (path == null) {
            return "evaluator";
        }
        return new File(path).getName();
    }

    public void dispose() {
        this.outputMap.clear();
        this.scopeStack.clear();
        this.scope.dispose();
    }

    public static class Output {
        private final JsonValue value;
        private final JelException thrown;
        private final String fullText;

        private Output(
                final JsonValue value, final JelException thrown, final String fullText) {
            this.value = value;
            this.thrown = thrown;
            this.fullText = fullText;
        }

        public boolean isError() {
            return this.value == null;
        }

        // null if error thrown
        public @Nullable JsonValue getValue() {
            return this.value;
        }

        // null if no error thrown
        public @Nullable JelException getThrown() {
            return this.thrown;
        }

        // will eventually be null if no error can be thrown
        public @Nullable String getFullText() {
            return this.fullText;
        }
    }
}
