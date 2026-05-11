package org.bsdclub.furuta.jalo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JqCliTest {
    @Test
    void c1_jFlagReadsJsonFromStdin() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exit = App.run(
            new String[] {"-j", ".foo"},
            new ByteArrayInputStream("{\"foo\":42}".getBytes(StandardCharsets.UTF_8)),
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));

        assertThat(exit).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8)).contains("42");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void c2_jFlagCompactOutput() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exit = App.run(
            new String[] {"-j", "-c", "."},
            new ByteArrayInputStream("{\"foo\":42}".getBytes(StandardCharsets.UTF_8)),
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));

        assertThat(exit).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo("{\"foo\":42.0}");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void c3_jFlagNullInput() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exit = App.run(
            new String[] {"-j", "-n", "."},
            new ByteArrayInputStream(new byte[0]),
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));

        assertThat(exit).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8)).contains("null");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void c4_jqFileRunsInJqMode() throws IOException {
        Path file = Files.createTempFile("jalo-jq-cli-", ".jq");
        Files.writeString(file, ".foo", StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exit = App.run(
            new String[] {file.toString()},
            new ByteArrayInputStream("{\"foo\":42}".getBytes(StandardCharsets.UTF_8)),
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));

        assertThat(exit).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8)).contains("42");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }
}
