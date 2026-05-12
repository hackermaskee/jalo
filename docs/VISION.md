# jalo VISION (Design Philosophy & Long-term Direction)

## 1. Mission

jalo exists to close the gap between JSON's universality and Lisp's composability. JSON has become the lingua franca of structured data, yet the tools available for transforming it—most notably jq—reach the limits of their expressiveness when programs grow beyond simple filters. Conventional Lisp dialects offer rich composition through higher-order functions, closures, and a homoiconic data model, but treat JSON as a foreign encoding rather than a native substrate.

jalo unites these two worlds: it is a pure functional language whose AST *is* the JSON model, making data and code structurally identical. The goal is predictable, composable, effect-aware computation over JSON-native data, in an environment that rewards interactive exploration and reliable automation. jalo targets the space between jq's powerful-but-limited filter model and Clojure's expressive-but-heavy JVM Lisp.

See SPEC.md §1 for language specification and DESIGN.md for implementation decisions.

## 2. Non-Goals

The following are explicitly out of scope. Recognizing what jalo is *not* is as important as knowing what it is.

- **General-purpose systems programming language.** jalo is optimized for data transformation pipelines and interactive JSON manipulation, not for operating systems, device drivers, or high-throughput network services.
- **100% jq compatibility.** jalo supports a representative subset of jq filter syntax (see SPEC §6), not a complete superset. Programs that rely on jq-specific edge cases or advanced path expressions may not run unchanged.
- **JIT compiler or native binary distribution.** Phase 1 is a tree-walking interpreter on the JVM. Phase 2 targets JVM bytecode but remains source-interpreted at the REPL level. AOT compilation to native binaries is not planned.
- **Macro system in version 1.0.** A hygienic macro facility is on the long-term roadmap (§6), but it is explicitly excluded from the first stable release.
- **Namespace mechanism in version 1.0.** Module-level namespaces (I-02) are deferred; all definitions share a single global scope in the current version.
- **Tail-call optimization in version 1.0.** TCO strategy is under active evaluation (I-08; see DECISION_TCO.md). The first release does not guarantee stack-safe tail recursion; deep recursion should be replaced by higher-order iteration functions such as `reduce` and `map`.
- **Static type system.** jalo is dynamically typed. A gradual or static type layer is a distant future consideration and is not part of the current design.

## 3. Design Philosophy

### 3.1 JSON as Homoiconic Data Model

Traditional Lisp dialects store programs as cons-cell trees (S-expressions) and data as separate runtime objects. jalo collapses this distinction: *the AST is a JSON value*. Every jalo program is a legal JSON document, and every JSON document is a potential jalo program.

This choice has three practical consequences. First, any tool that reads JSON—editors, diff utilities, databases, REST APIs—can inspect or generate jalo source without a dedicated parser. Second, the boundary between "code" and "data" dissolves naturally: `(quote expr)` returns a JSON value structurally identical to the original form. Third, jalo interoperates immediately with the YAML and jq ecosystems because the underlying data model is shared.

The idea echoes Lisp's original insight about homoiconicity, but grounds it in the contemporary data landscape rather than in 1960s list notation.

### 3.2 Why Lisp Family

Given that the problem is composable JSON transformation, why choose a Lisp dialect rather than extending jq itself?

jq is powerful but intentionally minimal. Its filter model—a chain of transformations on a single input—breaks down when programs need named abstractions, closures, or recursive algorithms. Adding those features to jq's formalism requires departing from its core model so thoroughly that the result would no longer be recognizable as jq.

Lisp provides exactly the primitives that jq lacks: first-class functions with lexical closure, a `let`/`fn`/`def` binding system, and a REPL-centric interactive development model. Clojure's idiomatic vocabulary (`let`, `fn`, higher-order sequence operations) proved especially influential on jalo's design, because Clojure demonstrated that a Lisp can feel both functional and practical on a host platform without a native runtime (see DESIGN.md §2.1).

The homoiconicity of Lisp also opens the door to a macro system in future versions (§6). Once programs are data, code generation and domain-specific languages become first-class concerns rather than bolt-on metaprogramming.

### 3.3 Immutable First

All jalo values are immutable. Lists, maps, and strings created by one expression cannot be modified in place by another. This is enforced at the implementation level via Paguro persistent collections (see DESIGN.md §2.4), not merely by convention.

Immutability provides referential transparency: if `f(x) = v`, then every occurrence of `f(x)` can be replaced by `v`. This property makes programs easier to reason about, easier to test, and easier to parallelize. It also means that jalo programs are inherently safe to share across contexts in a concurrent pipeline without defensive copying.

### 3.4 Algebraic Effects over Exceptions

jalo uses algebraic effects—`raise` and `handle`—as its primary mechanism for non-local control flow, rather than the exception hierarchies found in Java or Common Lisp (see DESIGN.md §2.1, SPEC §4.4).

The motivation is twofold. First, jq programs use `error`, `try`, and `catch` to manage failure and alternative paths. A unified effect model can represent all of these patterns in a single, composable abstraction. Second, algebraic effects are *first-class and resumable* in principle: a handler can examine an effect, decide what to do, and resume the suspended computation with a value. This is strictly more general than stack-unwinding exceptions, which discard the continuation unconditionally.

The first version implements *abort-only* effects (SPEC §4.4): a raised effect unwinds the stack to the nearest matching handler, which cannot resume the computation. This is sufficient to model jq's error/try semantics. Full resumable continuations are on the roadmap (§6, I-06), and the current design is intentionally forward-compatible with them.

## 4. Target Users & Use Cases

**Primary user profiles:**

| User | What they need from jalo |
|---|---|
| jq power users | More expressive transformations: named functions, recursion, closures, reusable filters |
| Clojure/Lisp developers | A lightweight JVM Lisp with a JSON-native data model and familiar `fn`/`let` vocabulary |
| Java developers | A scripting language that runs on the JVM without a separate runtime installation |
| Data engineers | Composable JSON processing pipelines that can be version-controlled and unit-tested |

**Representative use cases:**

1. *JSON log aggregation* — transform, filter, and summarize structured log output with named functions and recursive descent.
2. *CI/CD pipeline scripting* — manipulate complex JSON payloads from GitHub Actions or Kubernetes manifests with readable, testable scripts.
3. *API response transformation* — reshape nested JSON structures from external APIs before storing or forwarding them.
4. *Lisp education* — a small, approachable Lisp whose data model eliminates the barrier for learners already familiar with JSON.
5. *Interactive data exploration* — REPL-driven prototyping of data transformation logic with immediate feedback.

## 5. Differentiation

The table below positions jalo against the most commonly compared tools. The TCO row reflects the status as of this document; see DECISION_TCO.md for the decision under active review.

| Dimension | jq | Clojure | Babashka | Scheme (R7RS) | jalo |
|---|---|---|---|---|---|
| Data model | JSON | EDN / Java objects | EDN / Java objects | S-expressions | JSON model (native) |
| Error handling | `try`/`catch`/`error` | Java exceptions | Java exceptions | `condition`/`handler` | Algebraic effects (`raise`/`handle`) |
| TCO | n/a (no recursion) | Explicit `recur` | Explicit `recur` | Proper tail recursion (implicit) | See DECISION_TCO.md |
| Startup cost | Lightweight binary | JVM heavy (~0.5 s+) | GraalVM fast (~50 ms) | Varies | JVM heavy (Phase 1) |
| JSON nativeness | Native | Requires parsing | Requires parsing | Requires parsing | Native |
| Macro system | None | Full (hygienic) | Full (via Clojure) | `define-syntax` | Planned (1.0+) |
| Namespaces | None | Full | Full | `define-library` | Planned (1.0+) |

jalo's primary differentiator is the combination of *JSON-native homoiconicity* and *algebraic effects*: no other widely used tool offers both. jq provides JSON nativeness without composability; Clojure provides Lisp composability without JSON nativeness; Scheme provides proper tail recursion and hygienic macros but lacks a JSON-native representation.

## 6. Long-term Vision

- **Algebraic effects with resumable continuations** — lift the Phase 1 abort-only restriction and enable full coroutine and generator patterns (I-06, SPEC §4.4).
- **TCO strategy decision** — adopt one of the options evaluated in DECISION_TCO.md after receiving project owner sign-off; implement in a subsequent release.
- **Macro system** — introduce a hygienic macro facility (tentatively `defmacro`) in version 1.0 or later, enabling DSLs and code-generation patterns.
- **Namespace mechanism** — resolve I-02 to allow multi-file programs with explicit import/export boundaries.
- **JVM bytecode compiler (Phase 2)** — compile jalo programs to JVM class files, reducing startup cost and enabling optimization passes (see DESIGN.md §1).
- **`1.0.0` release** — criteria to be decided by consensus (see SPEC §1.2). Prerequisites include a stable language spec, a passing test suite, and resolution of critical open issues.
