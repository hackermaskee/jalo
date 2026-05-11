# Changelog
All notable changes to this project will be documented in this file.

## [Unreleased]

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
