# jalo — JSON And Lisp Operations

English: [README.md](README.md)

jalo は AST に JSON モデルを採用した Lisp 方言です。
インタラクティブ REPL・1 行評価・ファイル実行の 3 モードを持つ
小規模で実用的なコマンドラインランタイムです。

## プロジェクト概要

- 名前: `jalo` (JSON And Lisp Operations)
- ランタイム: Java 21 + Gradle
- アーキテクチャ: Lexer → Parser → SyntaxChecker → Evaluator → REPL
- 目標: イミュータブルなデータ構造とエフェクト対応のランタイムで、予測可能な対話的開発を実現します

## クイックスタート

1. ビルド (初回のみ):

```bash
./gradlew build
```

2. REPL を起動:

```bash
./gradlew run
```

3. プロンプトで式を評価:

```text
jalo> (+ 1 2)
3
jalo> :quit
```

## 実行例

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

## CLI の使い方

REPL モード (引数なし):

```bash
./gradlew run
```

1 行評価モード:

```bash
./gradlew run --args="-e '(+ 1 2)'"
```

ファイル実行モード:

```bash
./gradlew run --args="examples/hello.jal"
```

## 伝統的 Lisp ユーザーへの注

jalo は Common Lisp / Scheme の慣習とは意図的に異なる設計をしています。
以下の主要な相違点を把握しておいてください。

| 項目 | 伝統的 Lisp | jalo | 理由 |
|------|------------|------|------|
| AST 表現 | S 式 / コンスセル | JSON モデル | JSON によるホモイコニック性 |
| ドット対 | `(a . b)` 使用可 | 使用不可 | マップ構造は `{}` で表現 |
| シンボル | 第一級データ型 | 存在しない | JSON にシンボル型がないため |
| 文字列の評価 | クォートしたリテラル | 変数参照 | JSON-as-AST のホモイコニック性のため |
| マップキー文字列 | 評価される場合がある | 評価されない | JSON の予測可能なセマンティクス |
| リスト区切り | `(...)` のみ (CL/Scheme) | `(...)` と `[...]` は等価 | JSON 配列との互換性のため |
| 真偽値リテラル | `t` / `nil` (CL) | `#true` / `#false` | null や空リストとは別個 |
| 数値型サフィックス | 暗黙的 | `42i` / `42L` / `3.14` | SPEC §2.1 の 3 つの数値型 |

### 補足

- **`(...)` と `[...]` の等価性**: jalo ではどちらも順序付きシーケンス (JSON 配列) として扱われます。
  すべての値が JSON を経由するため、文脈に応じて読みやすい方を使ってください。
- **数値型サフィックス** (`42i`、`42L`): 整数と長整数リテラルには `i` または `L` サフィックスが必要です。
  型システムの詳細は SPEC §2.1 を、リテラル構文は SPEC §3 をご覧ください。

参照: [SPEC §3 — 構文](SPEC.md) / [SPEC §4.1 — 評価規則](SPEC.md)

## ドキュメント

- 言語仕様: `SPEC.md`
- 実装設計: `docs/DESIGN.md`
- コントリビュート規則: `CONTRIBUTING.md`

## ビルド & テスト

```bash
./gradlew build
./gradlew test
./gradlew check
```
