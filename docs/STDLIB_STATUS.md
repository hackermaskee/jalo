# STDLIB_STATUS.md — jalo 組込み関数実装状況

## PR-A (feat/cmd-413-stdlib-pr-a)

| カテゴリ | 関数 | 状態 |
|---------|------|------|
| 文字列基盤 | str-count, str-get, subs, str-upper, str-lower, str-trim, str-starts-with?, str-ends-with?, str-contains?, str-split, str-join, str-replace, str-replace-first, str-index-of, str->number, number->str, str->keyword, keyword->str, char-at, str-empty?, str-blank?, str | ✅ PR-A |
| 配列基本 | count, conj, get, nth, take, drop など | ⏸ PR-B |
| マップ・コレクション | keys, values, has-key?, assoc, dissoc など | ⏸ PR-B |
| 高階関数 | map, filter, reduce, sort-by など | ⏸ PR-C |
| JSON・I/O | to-json, parse-json, read-file など | ⏸ PR-C |

## Note

- 本 PR-A は Evaluator の builtins ディスパッチ基盤 + 文字列 22 関数の first cut を実装。
- `str-count` / `str-index-of` の戻り値型は `int` (cmd_416 で `JsonNumber` から `JaloInt` へ統一)。
- 残関数は cmd_413 PR-B / PR-C で段階的に実装する。
