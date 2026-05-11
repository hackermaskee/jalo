package org.bsdclub.furuta.jalo.repl;

import java.util.List;
import java.util.OptionalInt;
import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.lexer.LexerException;
import org.bsdclub.furuta.jalo.lexer.Token;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.parser.ParserException;
import org.bsdclub.furuta.jalo.syntaxcheck.SyntaxCheckException;
import org.bsdclub.furuta.jalo.syntaxcheck.SyntaxChecker;

/**
 * Connects lexer, parser, syntax checker, and evaluator into one stateful pipeline.
 *
 * <p>Layer: REPL (per DESIGN.md §1 architecture table).
 */
public final class Pipeline {
    private Evaluator evaluator;
    private SyntaxChecker syntaxChecker;

    /** Creates a new pipeline with a fresh evaluator context. */
    public Pipeline() {
        this.evaluator = new Evaluator();
        this.syntaxChecker = new SyntaxChecker();
    }

    /**
     * Runs the full Lex -> Parse -> SyntaxCheck -> Eval pipeline.
     *
     * @param src jalo source input
     * @return pipeline success or failure result
     */
    public EvalResult run(String src) {
        try {
            List<Token> tokens = new Lexer().tokenize(src);
            JaloValue ast = new Parser().parseStandard(tokens);
            syntaxChecker.check(ast);
            JaloValue value = evaluator.eval(ast);
            return new EvalResult.Success(value);
        } catch (LexerException e) {
            return new EvalResult.Failure(
                EvalResult.ErrorKind.LEX,
                e.getMessage(),
                OptionalInt.of(e.line()),
                OptionalInt.of(e.col()));
        } catch (ParserException e) {
            return new EvalResult.Failure(
                EvalResult.ErrorKind.PARSE,
                e.getMessage(),
                OptionalInt.of(e.line()),
                OptionalInt.of(e.col()));
        } catch (SyntaxCheckException e) {
            return new EvalResult.Failure(
                EvalResult.ErrorKind.SYNTAX,
                e.getMessage(),
                OptionalInt.empty(),
                OptionalInt.empty());
        } catch (JaloEffectSignal e) {
            return new EvalResult.Failure(
                EvalResult.ErrorKind.EFFECT,
                e.getMessage(),
                OptionalInt.empty(),
                OptionalInt.empty());
        } catch (RuntimeException e) {
            return new EvalResult.Failure(
                EvalResult.ErrorKind.INTERNAL,
                e.getClass().getSimpleName() + ": " + e.getMessage(),
                OptionalInt.empty(),
                OptionalInt.empty());
        }
    }

    /** Resets accumulated evaluator state such as {@code def} bindings. */
    public void reset() {
        this.evaluator = new Evaluator();
        this.syntaxChecker = new SyntaxChecker();
    }
}
