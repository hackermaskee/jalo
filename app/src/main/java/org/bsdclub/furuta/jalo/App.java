package org.bsdclub.furuta.jalo;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bsdclub.furuta.jalo.repl.EvalResult;
import org.bsdclub.furuta.jalo.repl.Pipeline;
import org.bsdclub.furuta.jalo.repl.Repl;

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
            if (args.length == 1) {
                String source = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
                return evalAndPrint(source, out, err);
            }

            err.println("Usage: jalo [-e <expr>] [<file>]");
            return 2;
        } catch (IOException e) {
            err.println("I/O error: " + e.getMessage());
            return 1;
        }
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
