# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a design-stage project for **jalo** — a Lisp-like language that uses the JSON data model as its AST instead of S-expressions. The primary design document is `lang.md` (written in Japanese).

## Language Design Concepts

### Core Idea
jalo is a homoiconic, functional language where:
- The **JSON model** (not JSON text itself) is the AST
- JSON, YAML, and a custom "standard syntax" are all valid surface representations
- JSON text can be parsed into the JSON model and evaluated as jalo programs

### Standard Syntax
The standard syntax intentionally resembles S-expressions to leverage Lisp tooling:
- `null`, `true`, `false` → `#null`, `#true`, `#false`
- Arrays: `[...]` or `(...)` (round brackets also accepted)
- Element separator: whitespace or comma (comma treated as whitespace)
- Strings: double-quoted, or bare identifiers if no special characters
- Strings act as symbols — evaluating a string looks up its variable binding
- Object/map keys are **not** evaluated

### Special Forms
`quote`, `if`, `let`, `fn`, `def`

### Pattern System (Backquote equivalent)
Used for both construction (right-hand side) and destructuring (left-hand side):
- `$expr` — splice a single value (like Lisp's `,`)
- `@expr` — splice an array into a surrounding array (like Lisp's `,@`)
- `%expr` — merge a map into a surrounding map
- Map keys can also use `$` for dynamic keys
- Left-side patterns (in `match`) only allow variables after `$`/`@`/`%`, not arbitrary expressions

### Pattern Matching Syntax
```
(match <expr0> <pattern1> <expr1> [<pattern2> <expr2>]...)
```
Evaluates `expr0`, then tries each pattern in order; binds matched sub-expressions and evaluates the corresponding expression.

### jq Compatibility
jalo aims for jq compatibility: a jq program can be parsed into a JSON model AST and evaluated by jalo with equivalent behavior. This is an add-on feature, not the core goal.

### Implementation Plan
1. **Phase 1**: Tree-walking interpreter
2. **Phase 2**: JVM bytecode compiler (while keeping REPL support)
- Immutable data structures implemented via **Paguro** library
- Language is purely functional — no variable mutation

### Testing Strategy
- jq-based test framework
- Test cases validated against existing jq implementations (C and Jackson-based)
- Gradually expand passing test cases for jalo's jq-compatible subset

## Document Maintenance

### ISSUES.md
`ISSUES.md` tracks open and resolved design issues. When a design question is settled (e.g., a decision is recorded in `SPEC.md`), update the corresponding entry in `ISSUES.md`:
- Change `**状態**: 未定義` / `**状態**: 未設計` / `**状態**: 未定` / `**状態**: 要確認` → `**状態**: 解決済み`
- Replace the issue body with a one-line summary of the decision and a pointer to where it is documented (e.g., `SPEC.md §X.Y`).
