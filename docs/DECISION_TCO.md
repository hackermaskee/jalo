# ADR-001: jalo Tail-Call Optimization (TCO) Strategy

## Status

Draft / Under Review — awaiting project owner decision (see Open Questions)

## Context and Problem Statement

jalo is a pure functional Lisp dialect running on the JVM (Java 21). Recursive algorithms are idiomatic in functional programming, but the JVM does not support transparent tail-call optimization at the bytecode level. Every call frame consumes stack space; unbounded recursion will eventually throw `StackOverflowError`.

The current project state is captured in ISSUES.md I-08:

> **I-08**: 末尾呼び出し最適化 (TCO) — 状態: 資料 cmd_424 で評価中 (詳細 DECISION_TCO.md 参照)

Clojure, which faces the identical JVM constraint, resolved it with an explicit `recur` special form rather than transparent TCO:

> "Since Clojure uses the Java calling conventions, it cannot, and does not, make the same tail call optimization guarantees."  
> — Clojure official documentation, "Functional Programming — Recursive Looping",  
> https://clojure.org/about/functional_programming (accessed 2026-05-12)

> "recur is the only non-stack-consuming looping construct in Clojure. There is no tail-call optimization and the use of self-calls for looping of unknown bounds is discouraged."  
> — Clojure official documentation, "Special Forms — recur",  
> https://clojure.org/reference/special_forms#recur (accessed 2026-05-12)

This decision record evaluates six approaches to TCO in the context of jalo's architecture and provides a recommendation for project owner review.

## Decision Drivers

1. **Algebraic effect handler interaction** [highest priority] — jalo's `raise`/`handle` mechanism (SPEC §4.4) creates dynamic frame boundaries on the call stack. Any TCO strategy must either preserve or explicitly account for handler frame boundaries.
2. **JVM Phase 1 implementability** — the current implementation is a tree-walking interpreter. Options requiring deep bytecode transformation are not feasible in Phase 1.
3. **Phase 2 bytecode compatibility** — the chosen strategy must not block the planned JVM bytecode compiler (DESIGN.md §1). A strategy that works in Phase 1 but requires a full redesign for Phase 2 is undesirable.
4. **Expressiveness** — the strategy should support common functional idioms: at minimum, self-tail recursion; ideally, mutual recursion.
5. **Learning cost** — jalo's target users include jq power users and Clojure developers; the strategy should align with their existing mental models.
6. **Performance** — Phase 1 prioritizes correctness over speed, but catastrophic overhead is disqualifying.

## Considered Options

- **Option A**: Explicit `(recur ...)` — Clojure style
- **Option B**: Implicit TCO + trampoline — Scheme style
- **Option C**: Self-tail-only optimization — conservative subset
- **Option D**: CPS (Continuation-Passing Style) transformation
- **Option E**: Delimited continuations — Koka style
- **Option F**: No TCO — preserve current state (I-08 Option F)

## Decision Drivers Matrix

Each cell gives a one-line rationale followed by a symbol: ◎ best / ○ good / △ constrained / × disadvantaged.

| Option | Effect-Handler | JVM Phase 1 | Phase 2 Bytecode | Expressiveness | Learning Cost | Performance |
|---|---|---|---|---|---|---|
| A: `recur` (Clojure) | △ same-frame only; handler boundaries cannot be crossed | ◎ no bytecode transform needed | ○ compatible with Phase 2 design | ○ covers self/loop recursion | △ new construct for non-Clojure users | ◎ no heap overhead |
| B: Implicit TCO + trampoline | × trampoline must be effect-aware; naive impl breaks handler semantics | ○ feasible with moderate effort | △ semantic divergence risk at Phase 2 | ◎ Scheme-style proper tail recursion | ○ no new syntax | ○ heap allocation per bounce |
| C: Self-tail only | ○ self-calls never cross handler boundaries | ◎ simplest to implement | ○ compatible | △ mutual recursion excluded | △ partial optimization is surprising | ◎ |
| D: CPS transformation | ◎ handlers integrate naturally as continuations | × very heavy; rewrites entire evaluator | △ constrains Phase 2 design heavily | ◎ full generality including cross-handler | × very steep maintainer curve | × high heap allocation |
| E: Delimited continuations | ◎ native integration; effect handlers are continuations | × extremely heavy; not feasible Phase 1 | × requires fundamental redesign | ◎ full generality + future continuation support | × very steep | × very slow |
| F: No TCO (current state) | ◎ no interaction whatsoever | ◎ zero implementation cost | ◎ no constraint on Phase 2 | × deep recursion prohibited; StackOverflow risk | ◎ simplest; users use `reduce`/`map` | ◎ |

## Cross-Handler Tail Calls

### Problem Statement

Effect handlers in jalo create dynamic frame boundaries on the call stack. A tail call that crosses a handler boundary—where the call site is inside a `(handle ...)` form and the continuation is outside—cannot be optimized away without also unwinding the handler frame. This creates a fundamental tension between stack-safety and handler semantics.

Consider:

```jalo
(handle
  (f x)   ; f's last action is to call g, which is outside this handler's extent
  [effect v (resume v)])
```

Under Option A, `(recur ...)` cannot be used to cross the `handle` frame boundary: `recur` is limited to the nearest enclosing `loop` or `fn`. The call to `g` will always consume a stack frame.

### jq try-catch Compatibility

jalo's `handle`/`raise` is the effect-aware equivalent of jq's `try … catch`. Code translated from jq `try`/`catch` patterns into `handle`/`raise` forms may suppress tail-call optimization opportunities, because the handler creates an enclosing frame that Options A and C cannot cross.

The impact per option:

- **Options A and C** preserve jq compatibility but prevent cross-handler TCO.
- **Options D and E** enable cross-handler TCO but at high implementation cost.
- **Option F** sidesteps the problem entirely; users must avoid deep cross-handler recursion.

### 6 Options: Handler Crossing Comparison

| Option | Cross-handler tail calls | Effect transparency | jq try-catch influence |
|---|---|---|---|
| A: `recur` | × same-frame only | ○ handlers unaffected | △ handlers block cross-frame `recur` |
| B: Trampoline | △ possible but handler must be trampoline-aware | △ trampoline must not discard handler frames | × trampoline boundary may disrupt try-catch semantics |
| C: Self-tail only | × same-frame only | ○ | △ same as A |
| D: CPS | ○ CPS naturally integrates handler as continuation | ◎ | ○ |
| E: Delimited cont. | ◎ native integration | ◎ | ◎ |
| F: No TCO | n/a | ◎ no interference | ◎ no interference |

### Recommendation

Whether cross-handler tail calls are *required* is an open question for the project owner (Q4 below). If they are not required, Options A or F are sufficient.

## Pros and Cons per Option

### Option A: Explicit `recur` (Clojure Style)

`recur` is a special form that evaluates its arguments and rebinds the nearest enclosing `loop` or `fn` frame, restarting execution without consuming an additional stack frame.

**Clojure's design rationale — from official documentation:**

> "While not as general as tail-call-optimization, it allows most of the same elegant constructs, and offers the advantage of checking that calls to recur can only happen in a tail position."  
> — Clojure official documentation, "Functional Programming — Recursive Looping",  
> https://clojure.org/about/functional_programming (accessed 2026-05-12)

This choice was made because the JVM does not support transparent TCO; see Context section. For mutual tail recursion where `recur` cannot be used, Clojure provides a `trampoline` utility as a fallback.

Rich Hickey presented the broader design philosophy behind Clojure's explicit approach to state and control in "Are We There Yet?" (JVM Languages Summit 2009, https://www.infoq.com/presentations/Are-We-There-Yet-Rich-Hickey/ , accessed 2026-05-12) and "Simple Made Easy" (Strange Loop 2011, https://www.infoq.com/presentations/Simple-Made-Easy/ , accessed 2026-05-12). The talks advocate for explicitness as a prerequisite for reliability — a principle that motivates making tail-call intent visible to the programmer rather than relying on invisible compiler transformation.

**Pros:**
- Directly addresses the JVM stack constraint without bytecode transformation.
- Makes tail-call intent explicit; performance-sensitive paths are visible in the code.
- The evaluator can statically verify that `recur` appears in tail position and report an error otherwise.
- Familiar to Clojure developers; aligns with jalo's Clojure-influenced idioms (`let`, `fn`, `loop`).

**Cons:**
- Restricted to the same enclosing frame; mutual tail recursion requires `trampoline`.
- Cannot cross `handle` boundaries; effect-heavy code cannot use `recur` for cross-handler loops.
- jq users and newcomers must learn a new construct not present in jq.

### Option B: Implicit TCO + Trampoline (Scheme Style)

All tail calls are automatically optimized. The interpreter detects tail position and, rather than creating a new stack frame, replaces the current one — or uses a heap-allocated trampoline to simulate this on the JVM.

**Scheme standard — R5RS §3.5 "Proper Tail Recursion":**

> "An implementation is properly tail-recursive if it supports an unbounded number of active tail calls... no space is needed for an active tail call because the continuation used in the tail call has the same semantics as the continuation passed to the procedure containing the call."  
> — Scheme R5RS §3.5, "Proper Tail Recursion",  
> https://conservatory.scheme.org/schemers/Documents/Standards/R5RS/HTML/r5rs-Z-H-6.html (accessed 2026-05-12)

R7RS carries the same requirement: implementations must be "properly tail-recursive."

**Pros:**
- Transparent: all idiomatic recursive code is automatically stack-safe.
- No new syntax; existing code benefits immediately.
- Matches the Scheme standard guarantee of "proper tail recursion."

**Cons:**
- Effect handlers create frame boundaries that the trampoline must respect; a naive implementation breaks handler semantics (see Cross-Handler section).
- The trampoline mechanism introduces heap allocation overhead per bounce.
- JVM Phase 1 implementation complexity is moderate to high.
- Risk of semantic divergence between Phase 1 trampoline and Phase 2 bytecode compiler.

### Option C: Self-Tail Only

Only self-recursive tail calls (a function calling itself directly in tail position) are optimized. Mutual tail recursion and cross-handler tail calls are not covered.

**Pros:**
- Simpler to implement than full TCO: only one pattern to detect.
- Self-calls never cross handler boundaries, so handler interaction is not a concern.
- Compatible with Phase 2 bytecode.

**Cons:**
- Does not cover mutual recursion, limiting expressiveness for some functional patterns.
- May confuse users who expect either full proper TCO (Scheme) or explicit `recur` (Clojure).
- Partial optimization can create surprising behavior: "tail-recursive looking" code may still overflow if the recursion is cross-function.

### Option D: CPS (Continuation-Passing Style) Transformation

The interpreter transforms all function calls into continuation-passing style. The call stack is replaced by heap-allocated continuation closures. Effect handlers integrate naturally because they are themselves continuations.

**Pros:**
- Full generality: all tail calls, including cross-handler and mutual recursion, are stack-safe.
- Algebraic effects integrate cleanly into the CPS framework.
- Theoretically well-studied and correct.

**Cons:**
- Implementation complexity is very high for Phase 1; the entire evaluator must be rewritten.
- Performance is severely degraded by heap allocation for every continuation.
- Constrains Phase 2 bytecode design significantly.
- Very steep learning curve for maintainers.

### Option E: Delimited Continuations (Koka Style)

Effect handlers are implemented as first-class delimited continuations. Tail calls within and across handlers are natively stack-safe. This is the approach taken by Koka (Daan Leijen, Microsoft Research), which features "TRMC optimizations" (Tail Recursion Modulo Cons) and treats effect handlers as resumable continuations (Koka Language Book, https://koka-lang.github.io/koka/doc/book.html, accessed 2026-05-12).

OCaml 5.0 took a related direction by retrofitting effect handlers as delimited continuations onto an existing compiled language (OCaml 5 Manual — Effect Handlers, https://ocaml.org/manual/effects.html, accessed 2026-05-12).

**Pros:**
- Most general solution: handles cross-handler tail calls natively.
- Forward-compatible with resumable continuations (I-06, SPEC §4.4).
- Aligns with the long-term effect system direction.

**Cons:**
- Implementation complexity is the highest of all options.
- Not feasible for Phase 1; requires fundamental redesign.
- Requires full redesign for Phase 2 bytecode.
- Very steep learning curve.

### Option F: No TCO — Preserve Current State

Do not implement TCO in version 1.0. Document the limitation clearly in I-08, and direct users to avoid deep recursion by using higher-order iteration functions (`reduce`, `map`, `filter`).

In Erlang, the BEAM virtual machine handles proper tail calls automatically ("If the last expression of a function body is a function call, a tail-recursive call is done. This is to ensure that no system resources, for example, call stack, are consumed." — Erlang Reference Manual, Functions, https://www.erlang.org/doc/reference_manual/functions.html, accessed 2026-05-12). jalo on the JVM cannot offer this guarantee without one of the options above. Option F makes this limitation explicit rather than hiding it.

**Pros:**
- Zero implementation cost.
- No interaction with algebraic effects.
- Maintains full compatibility with Phase 2 plans; the bytecode compiler can add TCO independently.
- Adequate for the majority of JSON transformation use cases, where recursion depth is bounded by the structure of typical API payloads.

**Cons:**
- Deep recursion is not stack-safe; programs recursing over large inputs will throw `StackOverflowError`.
- Creates a divergence from Scheme's fundamental guarantee of proper tail recursion.
- May be perceived as a significant language limitation by Lisp-experienced users.

*Note on Haskell:* Haskell's lazy evaluation provides a different form of stack-safety through guarded recursion — "where any recursive calls occur within a data constructor." This is fundamentally different from traditional TCO: "In Haskell, the function call model is a little different, function calls might not use a new stack frame, so making a function tail-recursive typically isn't as big a deal." (Haskell Wiki, "Tail recursion", https://wiki.haskell.org/Tail_recursion, accessed 2026-05-12). Because jalo uses eager (strict) evaluation, Haskell's lazy thunk model does not apply and cannot be adopted directly.

## Recommendation

The two most viable options for jalo's Phase 1 are **Option A (explicit `recur`)** and **Option F (no TCO)**.

**Option F** is the accurate description of the current state, requires zero implementation effort, and is adequate if the primary use cases are JSON transformation pipelines where recursion depth is bounded by input structure. If jalo's target users routinely process deeply nested or arbitrarily large recursive data, the lack of TCO becomes a real limitation.

**Option A** resolves the most common case—self and loop recursion—at moderate implementation cost, and aligns jalo with Clojure's established solution to the same JVM constraint. It does not solve cross-handler tail recursion, but that problem is left to future versions (Options D or E).

This recommendation is derived from jalo's mission (VISION.md §1): composable JSON-native computation that rewards interactive exploration. Option A enables clean recursive idioms for typical JSON traversal depths; Option F remains valid if the project owner judges that iteration (`reduce`/`map`) is the idiomatic alternative for all cases.

**This recommendation is a draft pending project owner review.** See Open Questions Q3 and Q4.

Options B–E are deferred to Phase 2 or later: the interaction with algebraic effects, implementation cost, or both exceed what is appropriate for Phase 1.

## Open Questions

The following questions require project owner decision before this ADR can be finalized.

- **Q1: Document language** — should VISION.md and DECISION_TCO.md be English-only, Japanese-only, or bilingual (matching the README.md / README_ja.md pattern from cmd_410)?
- **Q3: TCO phase** — is TCO required for version 1.0, deferred to Phase 2, or permanently deferred (Option F as the final answer)?
- **Q4: Cross-handler tail calls** — are handler-crossing tail calls required, recommended, or not needed? This is the key factor distinguishing whether Options A/C are sufficient or whether D/E must eventually be considered.
- **Q5: Open questions in ADR** — should this ADR record a final decision before merging, or is "Draft / Under Review" an acceptable long-term state for a project in incubation?
- **Q6: Macro interaction** — does a future macro system (VISION.md §6) impose any constraint on the TCO strategy? Option D (CPS) in particular may interact with macro expansion.
- **Q7: Version 1.0 scope** — is Option F (no TCO through 1.0) acceptable given jalo's positioning as a Lisp dialect?
- **Q10: Option F mitigations** — if Option F is selected permanently, what documentation and ergonomic mitigations are needed (e.g., recommended stack depth limits, iteration idiom guide)?

## References

| Language | Resource | URL | Accessed |
|---|---|---|---|
| Clojure | Special Forms — recur | https://clojure.org/reference/special_forms#recur | 2026-05-12 |
| Clojure | Functional Programming — Recursive Looping | https://clojure.org/about/functional_programming | 2026-05-12 |
| Clojure | Are We There Yet? (Rich Hickey, JVM Languages Summit 2009) | https://www.infoq.com/presentations/Are-We-There-Yet-Rich-Hickey/ | 2026-05-12 |
| Clojure | Simple Made Easy (Rich Hickey, Strange Loop 2011) | https://www.infoq.com/presentations/Simple-Made-Easy/ | 2026-05-12 |
| Scheme | R5RS §3.5 Proper Tail Recursion | https://conservatory.scheme.org/schemers/Documents/Standards/R5RS/HTML/r5rs-Z-H-6.html | 2026-05-12 |
| Koka | Koka Language Book | https://koka-lang.github.io/koka/doc/book.html | 2026-05-12 |
| Erlang | Reference Manual — Functions | https://www.erlang.org/doc/reference_manual/functions.html | 2026-05-12 |
| OCaml | Manual — Effect Handlers (OCaml 5) | https://ocaml.org/manual/effects.html | 2026-05-12 |
| Haskell | Haskell Wiki — Tail recursion | https://wiki.haskell.org/Tail_recursion | 2026-05-12 |
