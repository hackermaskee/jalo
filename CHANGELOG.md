# Changelog
All notable changes to this project will be documented in this file.

## [Unreleased]

## [0.3.0] - 2026-05-11

### BREAKING CHANGES

- Suffix-less integer literals now produce `JaloInt` instead of `JaloNumber` (double).
  `(type 42)` is now `"int"` (was `"double"`).

### Added

- `d` suffix for explicit double literals: `42d` -> `JaloNumber(42.0)`.
- Int overflow auto-promotes to `JaloLong` (for example, `2147483648`).
- Long overflow now raises parse error.

### Changed

- SPEC §3.1 numeric literal table revised for integer-default behavior and `d` suffix.
- SPEC §3.4 adds a note clarifying integer handling difference between standard syntax and `from-json`.

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
