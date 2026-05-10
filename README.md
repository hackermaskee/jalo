# jalo — JSON And Lisp Operations

jalo is a Lisp dialect whose AST is a JSON model.
It provides a small but practical command-line runtime with three execution modes:
interactive REPL, one-shot expression evaluation, and file evaluation.

## Project Description

- Name: `jalo` (JSON And Lisp Operations)
- Runtime: Java 21 + Gradle
- Architecture: Lexer -> Parser -> SyntaxChecker -> Evaluator -> REPL
- Goal: predictable interactive development with immutable data structures and effect-aware runtime behavior

## Quick Start

1. Build once:

```bash
./gradlew build
```

2. Start REPL:

```bash
./gradlew run
```

3. At the prompt, evaluate an expression:

```text
jalo> (+ 1 2)
3
jalo> :quit
```

## Examples

```text
(+ 1 2)                            => 3
(def x 10)                         => null
(+ x 5)                            => 15
(let [a 5 b 3] (- a b))            => 2
(quote [1 2 3])                    => [1,2,3]
(if #true 1 0)                     => 1
(fn [x] (* x x))                   => <function value>
(handle (raise (quote e) 42) [(quote e) v v]) => 42
:error                             => SYNTAX error ...
```

## CLI Usage

REPL mode (no args):

```bash
./gradlew run
```

Inline expression mode:

```bash
./gradlew run --args="-e '(+ 1 2)'"
```

File mode:

```bash
./gradlew run --args="examples/hello.jal"
```

## Documentation Links

- Language spec: `SPEC.md`
- Implementation design: `docs/DESIGN.md`
- Contribution rules: `CONTRIBUTING.md`

## Build & Test

```bash
./gradlew build
./gradlew test
./gradlew check
```
