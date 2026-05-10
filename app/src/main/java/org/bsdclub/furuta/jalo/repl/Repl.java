package org.bsdclub.furuta.jalo.repl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Runs the interactive REPL loop for jalo.
 *
 * <p>Layer: REPL (per DESIGN.md §1 architecture table).
 */
public final class Repl {
    private static final String PROMPT = "jalo> ";
    private static final String CONTINUATION_PROMPT = "  ... ";

    private final Pipeline pipeline;
    private final BufferedReader in;
    private final PrintStream out;
    private final PrintStream err;

    /** Creates a REPL using standard input and output streams. */
    public Repl() {
        this(new Pipeline(), System.in, System.out, System.err);
    }

    /**
     * Creates a REPL with explicit dependencies for tests.
     *
     * @param pipeline evaluation pipeline used by this REPL
     * @param input source for user input
     * @param out sink for normal output
     * @param err sink for error output
     */
    public Repl(Pipeline pipeline, InputStream input, PrintStream out, PrintStream err) {
        this(pipeline, new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)), out, err);
    }

    /**
     * Creates a REPL with injected reader and streams.
     *
     * @param pipeline evaluation pipeline used by this REPL
     * @param in source for user input
     * @param out sink for normal output
     * @param err sink for error output
     */
    public Repl(Pipeline pipeline, BufferedReader in, PrintStream out, PrintStream err) {
        this.pipeline = pipeline;
        this.in = in;
        this.out = out;
        this.err = err;
    }

    /**
     * Starts the REPL loop and returns when {@code :quit} is received.
     *
     * @throws IOException if reading input fails
     */
    public void run() throws IOException {
        out.println("jalo REPL (type :quit to exit, :reset to clear bindings)");
        while (true) {
            String input = readExpression();
            if (input == null) {
                break;
            }
            String trimmed = input.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (":quit".equals(trimmed) || ":q".equals(trimmed)) {
                break;
            }
            if (":reset".equals(trimmed)) {
                pipeline.reset();
                out.println("; environment reset");
                continue;
            }

            EvalResult result = pipeline.run(trimmed);
            if (result instanceof EvalResult.Success success) {
                out.println(success.value());
            } else if (result instanceof EvalResult.Failure failure) {
                err.println(formatFailure(failure));
            }
        }
    }

    private String readExpression() throws IOException {
        StringBuilder builder = new StringBuilder();
        String firstLine = readPromptedLine(PROMPT);
        if (firstLine == null) {
            return null;
        }
        builder.append(firstLine);

        while (!isBalanced(builder.toString())) {
            String nextLine = readPromptedLine(CONTINUATION_PROMPT);
            if (nextLine == null) {
                break;
            }
            builder.append('\n').append(nextLine);
        }

        return builder.toString();
    }

    private String readPromptedLine(String prompt) throws IOException {
        out.print(prompt);
        out.flush();
        return in.readLine();
    }

    private String formatFailure(EvalResult.Failure failure) {
        StringBuilder sb = new StringBuilder();
        sb.append(failure.kind()).append(" error");
        if (failure.line().isPresent()) {
            sb.append(" at line ").append(failure.line().getAsInt());
            if (failure.col().isPresent()) {
                sb.append(':').append(failure.col().getAsInt());
            }
        }
        sb.append(": ").append(failure.message());
        return sb.toString();
    }

    private boolean isBalanced(String source) {
        int depth = 0;
        boolean inString = false;

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            }
        }

        return depth <= 0;
    }
}
