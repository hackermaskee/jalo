# jalo — JSON And Lisp Operations

> 日本語版: [README_ja.md](README_ja.md)

jalo is a Lisp dialect whose AST is a JSON model.
It provides a small but practical command-line runtime with three execution modes:
interactive REPL, one-shot expression evaluation, and file evaluation.

> ⚠️ **Incubation (v0.3.0)** — jalo is under active development.  
> Language specifications may change in non-backward-compatible ways.  
> Programs written against the current version are not guaranteed to work in future versions.  
> See [SPEC.md §1.2](SPEC.md#12-versioning-policy) for details.

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
(quote foo)                        => "foo"
'foo                               => "foo"
'(1 2 3)                           => [1,2,3]
(str-count "hello")                => 5   ; int
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

## Notes for traditional Lisp users

jalo intentionally diverges from Common Lisp / Scheme conventions.
Below are the key differences to keep in mind:

| Aspect | Traditional Lisp | jalo | Why |
|--------|------------------|------|-----|
| AST representation | S-expression / cons cell | JSON model | Homoiconic over JSON |
| Dotted pair | `(a . b)` available | not available | Map-based structures use `{}` |
| Symbols | First-class type | not present | JSON has no symbol type |
| String evaluation | Quoted literal | Variable reference | Strings act as identifiers |
| Map key strings | Sometimes evaluated | Never evaluated | Predictable JSON semantics |
| List delimiters | `(...)` only (CL/Scheme) | `(...)` and `[...]` equivalent | JSON array compatibility |
| Boolean literals | `t` / `nil` (CL) | `#true` / `#false` | Distinct from null and empty list |
| Numeric literals | integer default + suffixes | `42`/`42i`/`42l`/`42d`/`3.14` | integer literals default to int; `d` forces double |
| Quote shorthand | `(quote expr)` | `'<expr>` also available | Traditional Lisp notation support |
| Integer counts and indices | implementation-dependent numeric type | `count`/`index-of`/`str-count`/`str-index-of` return `int` | matches exact integer semantics |

### Notes

- **`(...)` and `[...]` equivalence**: jalo treats both as ordered sequences (JSON arrays),
  because all values must round-trip through JSON. Use whichever reads more naturally in context.
- **Numeric type tags** (`42i`, `42L`): suffix-less integers default to `int`; use `i`/`l`/`d` for explicit type control.
  See SPEC §2.1 for the full type system and SPEC §3 for the literal syntax.
- **Quote shorthand**: jalo supports traditional `'<expr>` as a synonym for `(quote expr)`.
  Identifier-internal `'` (e.g., `foo'bar`) remains an identifier character (Haskell/SML-style prime identifiers).

See also: [SPEC §3 — Syntax](SPEC.md) and [SPEC §4.1 — Evaluation Rules](SPEC.md).

## Documentation Links

- Language spec: `SPEC.md`
- Implementation design: `docs/DESIGN.md`
- Contribution rules: `CONTRIBUTING.md`

## Troubleshooting

### REPL exits immediately after `./gradlew run`

This happens if your Gradle version does not forward stdin to the JVM process.
The fix is already included in `app/build.gradle` (`standardInput = System.in`).
If you still see the issue, pass `--console=plain` to suppress Gradle's rich console:

```bash
./gradlew run --console=plain
```

Alternatively, build a standalone distribution and run it directly:

```bash
./gradlew installDist
./app/build/install/app/bin/app
```

## Build & Test

```bash
./gradlew build
./gradlew test
./gradlew check
```
