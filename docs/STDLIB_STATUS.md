# STDLIB_STATUS.md — jalo 組込み関数実装状況

## PR-A (feat/cmd-413-stdlib-pr-a)

| カテゴリ | 関数 | 状態 |
|---------|------|------|
| 文字列基盤 | str-count, str-get, subs, str-upper, str-lower, str-trim, str-starts-with?, str-ends-with?, str-contains?, str-split, str-join, str-replace, str-replace-first, str-index-of, str->number, number->str, str->keyword, keyword->str, char-at, str-empty?, str-blank?, str | ✅ PR-A |
| 配列基本 | count, conj, get, nth, first, rest, last, cons, concat, reverse, sort, sort-by, subvec, range, index-of, contains? | ✅ PR-B |
| マップ・コレクション | assoc, dissoc, keys, vals, entries, from-entries, merge, update, select-keys, empty?, get-in, assoc-in, update-in, dissoc-in | ✅ PR-B |
| 高階関数 | map, filter, remove, reduce, apply, identity, not, every?, some, not-any?, take, drop, trampoline | ✅ PR-C + 実装済 (trampoline: HofBuiltins.java / HofBuiltinsTest.java、Clojure 互換 fn バウンス) |
| 数値拡張 | quot, rem, mod, floor, ceil, round, trunc, abs, max, min, pow, sqrt, log, exp, nan?, infinite?, pos?, neg?, zero?, double, not= | ✅ PR-C |
| JSON・I/O | print, println, read-line, to-json | ✅ PR-C |

## Note

- 本 PR-A は Evaluator の builtins ディスパッチ基盤 + 文字列 22 関数の first cut を実装。
- `str-count` / `str-index-of` の戻り値型は `int` (cmd_416 で `JsonNumber` から `JaloInt` へ統一)。
- cmd_413 PR-B で Array / Map / Seq 基本関数群を追加済み。
- cmd_413 PR-C で HOF / 数値拡張 / I/O を追加し、`assoc-in` / `update-in` もスタブから本実装へ更新。
