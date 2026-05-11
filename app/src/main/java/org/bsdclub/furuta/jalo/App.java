package org.bsdclub.furuta.jalo;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.bsdclub.furuta.jalo.jq.JqRuntime;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.lexer.Token;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.repl.EvalResult;
import org.bsdclub.furuta.jalo.repl.Pipeline;
import org.bsdclub.furuta.jalo.repl.Repl;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloFunction;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Dispatches jalo command-line execution modes.
 *
 * <p>Layer: REPL (per DESIGN.md §1 architecture table).
 */
public final class App {
    private App() {
    }

    /**
     * Starts jalo from the command line.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = run(args, System.in, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Executes jalo in REPL, inline-eval, or file mode.
     *
     * @param args command-line arguments
     * @param in input stream for REPL mode
     * @param out standard output stream
     * @param err standard error stream
     * @return the process exit code ({@code 0}, {@code 1}, or {@code 2})
     */
    static int run(String[] args, InputStream in, PrintStream out, PrintStream err) {
        try {
            if (args.length == 0) {
                new Repl(new Pipeline(), in, out, err).run();
                return 0;
            }
            if (args.length == 2 && "-e".equals(args[0])) {
                return evalAndPrint(args[1], out, err);
            }
            if (isJqMode(args)) {
                return runJqMode(args, in, out, err);
            }
            if (args.length == 1) {
                String source = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
                if (args[0].endsWith(".jq")) {
                    return runJqFilter(source, false, false, null, in, out, err);
                }
                return evalAndPrint(source, out, err);
            }

            err.println("Usage: jalo [-e <expr>] [<file>] | jalo -j [-c] [-n] <filter> [<json-file>]");
            return 2;
        } catch (IOException e) {
            err.println("I/O error: " + e.getMessage());
            return 1;
        }
    }

    private static boolean isJqMode(String[] args) {
        for (String arg : args) {
            if ("-j".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static int runJqMode(String[] args, InputStream in, PrintStream out, PrintStream err) throws IOException {
        boolean compact = false;
        boolean nullInput = false;
        boolean jq = false;
        String filter = null;
        String jsonFile = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-j" -> jq = true;
                case "-c" -> compact = true;
                case "-n", "--null-input" -> nullInput = true;
                default -> {
                    if (arg.startsWith("-")) {
                        err.println("Usage: jalo -j [-c] [-n] <filter> [<json-file>]");
                        return 2;
                    }
                    if (filter == null) {
                        filter = arg;
                    } else if (jsonFile == null) {
                        jsonFile = arg;
                    } else {
                        err.println("Usage: jalo -j [-c] [-n] <filter> [<json-file>]");
                        return 2;
                    }
                }
            }
        }

        if (!jq || filter == null) {
            err.println("Usage: jalo -j [-c] [-n] <filter> [<json-file>]");
            return 2;
        }
        return runJqFilter(filter, compact, nullInput, jsonFile, in, out, err);
    }

    private static int runJqFilter(
        String filter, boolean compact, boolean nullInput, String jsonFile, InputStream in, PrintStream out, PrintStream err)
        throws IOException {
        try {
            JaloValue input = nullInput ? JaloNull.INSTANCE : readJsonInput(jsonFile, in);
            JaloValue output = new JqRuntime().eval(filter, input);
            if (compact) {
                out.print(toCompactJson(output));
            } else {
                out.println(output);
            }
            return 0;
        } catch (RuntimeException e) {
            err.println("INTERNAL error: " + e.getMessage());
            return 1;
        }
    }

    private static JaloValue readJsonInput(String jsonFile, InputStream in) throws IOException {
        String jsonText;
        if (jsonFile != null) {
            jsonText = Files.readString(Path.of(jsonFile), StandardCharsets.UTF_8);
        } else {
            jsonText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<Token> tokens = new Lexer().tokenize(jsonText);
        return new Parser().parseJson(tokens);
    }

    private static String toCompactJson(JaloValue value) {
        if (value instanceof JaloNull) return "null";
        if (value instanceof JaloBool b) return b.value() ? "true" : "false";
        if (value instanceof JaloNumber n) return Double.toString(n.value());
        if (value instanceof JaloInt i) return Integer.toString(i.value());
        if (value instanceof JaloLong l) return Long.toString(l.value());
        if (value instanceof JaloString s) return quote(s.value());
        if (value instanceof JaloArray arr) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < arr.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(toCompactJson(arr.get(i)));
            }
            sb.append(']');
            return sb.toString();
        }
        if (value instanceof JaloMap map) {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (java.util.Map.Entry<String, JaloValue> entry : map.entries().entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(quote(entry.getKey())).append(':').append(toCompactJson(entry.getValue()));
            }
            sb.append('}');
            return sb.toString();
        }
        if (value instanceof JaloFunction) {
            throw new IllegalArgumentException("compact output requires pure JSON value");
        }
        throw new IllegalArgumentException("unsupported jq output type: " + value.getClass().getSimpleName());
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static int evalAndPrint(String source, PrintStream out, PrintStream err) {
        EvalResult result = new Pipeline().run(source);
        if (result instanceof EvalResult.Success success) {
            out.println(success.value());
            return 0;
        }
        if (result instanceof EvalResult.Failure failure) {
            err.println(formatFailure(failure));
            return 1;
        }
        return 1;
    }

    private static String formatFailure(EvalResult.Failure failure) {
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
}
