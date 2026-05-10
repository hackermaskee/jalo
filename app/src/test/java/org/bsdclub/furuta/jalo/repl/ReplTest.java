package org.bsdclub.furuta.jalo.repl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ReplTest {
    @Test
    void r1_additionPrintsResult() throws IOException {
        SessionOutput output = runSession("(+ 1 2)\n:quit\n");
        assertThat(output.stdout).contains("3");
    }

    @Test
    void r2_lexErrorPrintsErrorAndContinues() throws IOException {
        SessionOutput output = runSession("#abc\n(+ 2 3)\n:quit\n");
        assertThat(output.stderr).contains("LEX error");
        assertThat(output.stdout).contains("5");
    }

    @Test
    void r3_environmentPersistsAcrossInputs() throws IOException {
        SessionOutput output = runSession("(def x 10)\n(+ x 5)\n:quit\n");
        assertThat(output.stdout).contains("15");
    }

    @Test
    void r4_resetClearsEnvironment() throws IOException {
        SessionOutput output = runSession("(def x 10)\n:reset\nx\n:quit\n");
        assertThat(output.stdout).contains("; environment reset");
        assertThat(output.stderr).contains("SYNTAX error");
    }

    @Test
    void r5_quitEndsLoop() throws IOException {
        SessionOutput output = runSession(":quit\n(+ 9 9)\n");
        assertThat(output.stdout).doesNotContain("18");
    }

    private SessionOutput runSession(String input) throws IOException {
        CapturingPrintStream out = new CapturingPrintStream();
        CapturingPrintStream err = new CapturingPrintStream();
        Repl repl = new Repl(new Pipeline(), new BufferedReader(new StringReader(input)), out, err);
        repl.run();
        return new SessionOutput(out.getContent(), err.getContent());
    }

    private record SessionOutput(String stdout, String stderr) {}

    private static final class CapturingPrintStream extends PrintStream {
        private final java.io.ByteArrayOutputStream sink;

        CapturingPrintStream() {
            this(new java.io.ByteArrayOutputStream());
        }

        private CapturingPrintStream(java.io.ByteArrayOutputStream sink) {
            super(sink, true, StandardCharsets.UTF_8);
            this.sink = sink;
        }

        String getContent() {
            flush();
            return sink.toString(StandardCharsets.UTF_8);
        }
    }
}
