# Changelog
All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- docs: DECISION_MACRO.md V1 (ADR-002 V1) — jalo マクロ機構設計。Phase 2 候補 Option A (syntax-rules) / Option C (Clojure defmacro + auto-gensym) に絞込、衛生方針 (Q1 解決)、de-special-form 確定候補 4 件 (let*/quasiquote/and/or)、D1 (マクロ先行実装) / D2 (Option E 除外) 設計指針を確定 (cmd_425 + amendment_1)

  > **Note**: 本 ADR (V1) は cmd_434 で起案される ADR-002 (macro × namespace 統合設計) によって近日 supersede される予定。本 V1 は「Phase 2 候補 Option A/C 絞込」までの議論記録として保存される。

### Changed
- docs: Updated DECISION_MACRO.md §Recommendation — Q1 (hygienic + auto-gensym),
  Q5 (TCO independence), D1 (macro before bytecode compiler, de-special-form motivation),
  D2 (Option E excluded from macro mechanism, Phase 2 candidates = A/C only)

## [0.6.2] - 2026-05-17

### Changed
- docs: Rewrote DECISION_TCO.md for readability — added callstack diagrams (case i/ii),
  evalHandle Java frame layout, JVM language TCO survey (Kotlin/Scala/Kawa/Frege/ABCL),
  Option G (stdlib trampoline), cmd_427 phase/versioning alignment, §11 references (16 URLs)

## [0.6.1] - 2026-05-15

### Changed
- Decouple Phase 1/2 implementation scope from `1.0.0` versioning across all docs
  (SPEC.md §1.2, DESIGN.md §2, VISION.md, DECISION_TCO.md, DECISION_MACRO.md, README)

### Clarified
- SPEC.md §1.2: `1.0.0` 到達条件を三条件 (仕様安定 / 年単位 deprecation / 非互換警告機構) として明文化。
  Phase 2 機能実装との独立性を明記。jalo は個人実験プロジェクトであり `1.0.0` への到達は
  永遠に発生しない可能性が高い旨を明記。
- DESIGN.md: §2 「実装フェーズ」独立節を新設。Phase 1 (現行) / Phase 2 (計画中) を明文化し、
  バージョニングとの独立性を示す。
- VISION.md: 「バージョン 1.0」→「Phase 1/Phase 2」表記に統一し、同一視を除去。

## [0.6.0] - 2026-05-12

### Added

- Reader macro `#jq(...)` in standard syntax: lexer tokenization (`HashJqText`) with balanced-parenthesis scanning.
- Parser integration for `#jq(...)` via `JqParser.transpile(...)`.
- `JqParseException` and REPL/CLI error kind `JQ_PARSE`.
- CLI error format for jq transpile failures: `JQ_PARSE error rest-seq line N:M: ...`.
- Tests for lexer/parser/pipeline/app around `#jq(...)` success and failure paths.

### Changed

- SPEC §5.6 added (`#jq(...)` rules, bracket counting, AST examples, error taxonomy).
- SPEC §7.3 KIND list now includes `JQ_PARSE`.

## [0.5.1] - 2026-05-12

### Documentation

- SPEC.md §6: Add jq→jalo AST conversion rules for 14 supported constructs
- SPEC.md §6.9: Add unimplemented constructs list (T-019..T-023) with jalo alternatives
- JQ_COMPAT_STATUS.md: Correct T-019..T-023 status from ✅ to 📋 (transpiler path missing)
- Add bidirectional links between SPEC.md §6 and JQ_COMPAT_STATUS.md

## [0.5.0] - 2026-05-12

### Added

- jq compatibility layer completed across PR-A/PR-B/PR-C as first-class jalo built-ins.
- CLI jq mode: `-j <filter> [<json-file>]`, `-c` compact output, `-n/--null-input`.
- Auto jq mode for `.jq` file extension.
- `docs/JQ_COMPAT_STATUS.md` updated to current compatibility matrix and PR-C coverage.

### Changed

- `docs/SPEC.md` §6/§7 updated with jq mode CLI definitions and transpile-strategy note.
- Project version bumped from `0.4.1` to `0.5.0`.

## [0.4.1] - 2026-05-11

### Changed

- Move `SPEC.md` to `docs/SPEC.md` for consistent documentation layout
- Update all references to `SPEC.md` in README, README_ja, CONTRIBUTING, CLAUDE.md

## [0.4.0] - 2026-05-11

### BREAKING CHANGES

- `match` pattern lhs syntax changed from quasiquote to pattern sugar:
  - `` `[ ... ] `` -> `#[ ... ]`
  - `` `{ ... } `` -> `#{ ... }`
- Internal form rename:
  - `(backquote ...)` -> `(quasiquote ...)`
  - `(dollar x)` -> `(var x)`
  - `(at x)` -> `(rest-seq x)`
  - `(percent x)` -> `(rest-map x)`
- `match` pattern position now rejects quasiquote/backtick with parser error and guidance to `#[...]`.

### Added

- Pattern sugar reader tokens `#[` / `#{`.

### Changed

- SPEC §5 revised for quasiquote/pattern split and new pattern forms.

## [0.2.0] - 2026-05-11

### BREAKING CHANGES

- **`JsonValue` 削除**: `json.*` パッケージ (`JsonNull`, `JsonBool`, `JsonNumber`, `JsonString`, `JsonArray`, `JsonObject`, `JsonValue`) が全て削除された。全ての値型は `JaloValue` の直接実装となった。
- **配列・マップ要素型変更**: `JaloArray.elements()` の型が `PersistentVector<JaloValue>` に、`JaloMap.entries()` の型が `PersistentHashMap<String, JaloValue>` に変更され、`JaloInt`/`JaloLong` を型損失なく格納可能になった。
- **`range`/`conj`/`cons`/`assoc` 型挙動変更**: これらの関数で整数値を扱う際、従来は `JaloNumber (double)` に変換されていたが、`JaloInt` のまま維持されるようになった。

### Added

- `JaloValue` を sealed interface として確立し、9 つの型 (`JaloNull`, `JaloBool`, `JaloNumber`, `JaloString`, `JaloArray`, `JaloMap`, `JaloInt`, `JaloLong`, `JaloFunction`) を直接 permits。コンパイラによる網羅性検証が可能になった。
- `(pure-json? x)` 組込み関数: 値が完全に JSON 値モデルに適合するかを動的かつ再帰的に判定する。

### Changed

- SPEC §1/§2 改訂: sealed JaloValue 型階層と全 9 型の一覧を明示。
- SPEC §4.5 更新: `pure-json?` エントリと使用例を追加 (PR-B)。

## [0.1.0] - 2026-05-11
### Added
- Evaluator: quote/if/def/let/letrec/fn special forms (cmd_403)
- Value package: JaloValue/JaloInt/JaloLong type hierarchy (cmd_404)
- Javadoc policy and retroactive application (cmd_405)
- Checkstyle static analysis integration (cmd_406)
- REPL pipeline and interactive mode (cmd_408)
- Internal review gate (cmd_409)
- README internationalization (cmd_410)
- Backquote/match/pattern engine (cmd_412)
- Stdlib foundation + StringBuiltins 22 functions (cmd_413 PR-A)
- Quote shorthand 'expr -> (quote expr) (cmd_415)
- count/index return int type (cmd_416)
- Versioning policy and incubation declaration (cmd_417)
