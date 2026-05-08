# jalo 実装設計

## 1. アーキテクチャ概要

jalo は言語仕様 (`SPEC.md`) と実装設計 (`docs/DESIGN.md`) を分離し、実装方式の変更が仕様へ混入しない構成を採る。

責務分離の概念図:

| レイヤ | 主責務 | 出力 |
|---|---|---|
| Parser | JSON/YAML/標準構文の構文解析 | JSON モデル |
| JSON モデル | 言語中間表現 (JsonValue 階層) の保持 | 正規化 JSON モデル |
| TypeChecker | 名前解決・特殊フォーム構造・パターン構文検証 (型推論は将来) | 検証済み JSON モデル |
| Evaluator | 実行時評価 (Phase 1) | 値 / エフェクト |
| Runtime | 環境・束縛・例外/エフェクト制御 | 実行コンテキスト |
| Stdlib | 組み込み・jq互換関数群 | ユーザ可視 API |

実装フェーズ:

- Phase 1: ツリー歩行インタプリタ
- Phase 2: JVM バイトコードコンパイラ（REPL 維持）

想定実装スタック:

- Kotlin/Java on JVM
- immutable データ構造: Paguro
- jq 互換ライブラリ群

## 2. 重要な設計判断

### 2.1 代数的エフェクト採用

- 典拠: commit `ff49816`「エラー/例外処理として代数的エフェクトを採用」
- 判断: 例外を言語機能として規定し、`raise`/`handle` を中核機構にする。
- 理由: jq 互換で必要な error/catch 系フローを統一的に扱える。

### 2.2 REPL 課題

- 典拠: commit `9563a8a`「REPL における各種課題に対する定義を追加」
- 判断: `def` 再定義、`declare`、未定義参照時挙動を設計時点で明文化する。
- 理由: 対話環境の挙動が未定義だと実装差異が大きくなるため。

### 2.3 パターン処理方式 (backquote / match)

- 出所: 旧 `SPEC.md` の `backquote` 実装注釈、`§5` 系のパターン章
- 判断: `backquote` をパターン構築/分解の統一記法として扱い、`match` 側で静的制約を付与する。

### 2.4 Paguro ライブラリ採用 (immutable データ構造)

- 出所: 旧 `SPEC.md §7.2`
- 判断: Phase 1 から immutable collection を前提にランタイムを構築する。
- 理由: 言語の純粋関数型方針と整合。
- 採用バージョン: 3.10.3 (Maven Central、cmd_400 で確定)
- 主要用途:
  - JsonArray の内部 collection: `PersistentVector<JsonValue>`
  - JsonObject の内部 collection: `PersistentHashMap<String, JsonValue>`
- ライセンス: Eclipse Public License v1.0 + Apache License 2.0

### 2.5 JVM 例外機構設計 (JaloSignal)

- 出所: 旧 `SPEC.md §4.4 第1版の制約③`
- 判断: スタックトレースを抑制した `Throwable` サブクラス `JaloSignal(tag, value)` で制御フローを扱う。

### 2.6 Phase 1 引数評価戦略 (左→右、仕様上は不定)

- 出所: 旧 `SPEC.md §4.1` 注釈
- 判断: 仕様は評価順不定を維持しつつ、Phase 1 実装では左→右評価を採用。

### 2.7 算術演算子のディスパッチ実装 (型別個別関数)

- 出所: 旧 `SPEC.md §2.1` 注釈
- 判断: `int` / `long` / `double` の実装関数を分離し、昇格後に適切な演算子実体へ振り分ける。

### 2.8 P/L 実装プリミティブ区分

- 出所: 旧 `SPEC.md §4.5` の区分列
- 判断: 実装管理上は以下の区分を維持する（仕様本文からは分離）。

| 区分 | 意味 |
|---|---|
| P | JVM ホスト言語実装が必要なプリミティブ |
| L | jalo で記述可能なライブラリ関数 |

## 3. 初版コーディング時の優先順序

Phase 1 完成に向けた推奨順序:

1. 字句解析（標準構文トークナイザ）
2. パーサ（JSON/YAML/標準構文の AST 変換）
3. AST 正規化層
4. 評価器（特殊形式 + 関数適用）
5. ランタイム（環境・束縛・エフェクト）
6. 組み込み関数最小セット
7. jq 互換ライブラリ拡張

依存関係の要点:

- 評価器は AST 正規化前提
- 例外/エフェクト機構は評価器と同時に必要
- jq 互換はコア評価器安定後に拡張

旧 `SPEC.md §7` の実装フェーズ計画を再編し、実装順序に読み替えた。

## 4. 主要モジュール構成 (想定案)

本章は実装開始前の想定案である。初回実装 PR 後に実モジュール構造をもとに更新する。

想定ディレクトリ (`app/`)：

- `parser/` — 構文解析 (JSON/YAML/標準構文)
- `lexer/` — 字句解析
- `json/` — JSON モデル定義 (JsonValue 階層)
- `evaluator/` — ツリー歩行評価器
- `runtime/` — 環境・束縛・エフェクト機構
- `stdlib/` — 組み込み関数・jq 互換ライブラリ
- `typecheck/` — 型検査器

## 5. テスト戦略

### 5.1 TDD 方針 (t-wada スタイル)

コアロジック実装は Red→Green→Refactor サイクルを基本とし、次の 7 ステップを反復する。

1. 次の目標を考える
2. 目標を示すテストを書く
3. テストを実行して失敗させる (Red)
4. 目的のコードを書く
5. テストを成功させる (Green)
6. テストが通るままリファクタリングを行う (Refactor)
7. 1〜6 を繰り返す

実装着手前にテストリストを作成し、満たすべき動作を先行列挙する。テストリストは進捗可視化 (済/未着手/仕掛中) に使い、先に行う設計判断はインターフェース設計に限定し、実装の設計判断は Red/Green の往復で確定する。

実装ステップは不安の度合いで使い分ける。最初に試みるのは明白な実装。自信がなければ仮実装または三角測量に降りる。典拠: Kent Beck『テスト駆動開発』(和田卓人訳、オーム社 2017)。

- 明白な実装 (Obvious Implementation): 自明な場合、テストを安全網として直接書く
- 仮実装 (Fake Implementation): テストの正しさが不安なとき、べた書きでテストを通す
- 三角測量 (Triangulation): 一般化を引き出したいとき、2 件目以降のテストで本実装を強制する

リファクタリングは Green 状態でのみ行う。過度な最適化を避け、次の Red に進める見通しが立った時点でサイクルを戻す。

### 5.2 ツール

- JUnit 5 (Jupiter)
- AssertJ (流暢なアサーション)
  - 代数的エフェクト (`raise` / `handle`) のテストには `assertThatThrownBy` を使用
- Gradle `test` task

### 5.3 適用範囲

- コアロジック (`parser` / `ast` / `evaluator` / `runtime` / `stdlib`): TDD 必須
- 設定・ビルド・スクリプト: TDD 任意

### 5.4 粒度

- 1 テスト 1 アサーション、1 失敗 1 原因 (失敗時に即原因が判明する粒度)
- 単体テスト: コアロジック単位 (例: 1 関数 1 入力)
- 統合テスト: 複数モジュール連携 (例: Lexer + Parser パイプライン)
- 性質テスト (Property-Based): 任意・将来検討

### 5.5 jalo 特有のパターン例

例1: Lexer のトークン認識
単純なリテラル (例: 0〜9) は明白な実装で十分。一般化 (任意桁の int) を引き出したい場合は三角測量を選ぶ。不正リテラル (例: 先頭ゼロ 01) は別テストケースで境界条件を確認する。典型順序: int `0` → int `42` (三角測量) → long `42l` → 不正 `01`。

例2: Parser の段階的拡張
テストリストを先行作成し、単純構文から複雑構文へ段階拡張する。例: パターン構文 ``42` (リテラル) → `[$x]` (配列+束縛) → `[$x @rest]` (rest) → `{k: $v}` (Map) と段階拡張する。各段階で不安駆動で実装ステップを選択する。

例3: 評価器の代数的エフェクト
制御フロー (`raise` / `handle`) のテストでは AssertJ `assertThatThrownBy` 等で捕捉する。典型順序: 通常評価 → handle 一致 → handle なしエラー (`assertThatThrownBy`) → タグ不一致伝播 → ネストハンドラ。各ケースをテストリストで先行列挙する。

```java
assertThatThrownBy(() -> evaluator.eval(expr_without_handler))
    .isInstanceOf(JaloSignal.class);
```

### 5.6 jq 互換性検証戦略 (TDD と並立)

本節は §5.1〜§5.5 の TDD サイクルと並立する。jalo の jq 互換性回帰検証用。

- jq ベースのテストフレームワークを整備
- 既存 jq 実装 (C 実装 / Jackson ベース) との比較検証を行う
- jalo 側の jq 互換サブセットを段階的に拡大し、回帰テストへ追加
- 正常系だけでなくエフェクト処理系 (`raise` / `handle`) のケースを必須化
- REPL の再定義・未定義参照挙動を独立スイートで検証

## 6. SPEC からの移送マップ

| 移送元 (SPEC) | 移送先 (DESIGN) | 内容 |
|---|---|---|
| §7 実装 (旧行 708-737) | §1 アーキテクチャ概要 + §3 優先順序 | Phase 1/2 計画・Paguro 等 |
| §8 テスト戦略 (旧行 739-743) | §5 テスト戦略 | jq ベース検証 |
| §4.1 評価順注釈 (旧行 217) | §2.6 Phase 1 引数評価戦略 | 左→右実装詳細 |
| §2.1 算術注釈 (旧行 115) | §2.7 算術ディスパッチ | 型別個別関数実装 |
| §4.4 第1版制約③ (旧行 347) | §2.5 JVM 例外機構 | JaloSignal 設計 |
| §4.5 P/L マーク | §2.8 P/L 区分 | 実装プリミティブ一覧 |
| §4.4 第1版制約注釈 (旧行 275) | §2 重要な設計判断 | 静的解析 Phase 1 制約 |
| SPEC.md §7 Paguro (旧行 288) | §2.4 Paguro 採用 | immutable DS |
| §5.2 backquote 注釈 (旧行 614) | §2.3 パターン処理 | 第1版スペシャルフォーム実装 |

## §7 パッケージ規約 (Package Namespace Convention)

### 7.1 Top-level namespace

jalo の全 Java ソースは `org.bsdclub.furuta.jalo` を起点とする。

```
org.bsdclub.furuta.jalo          ← CLI/REPL エントリポイント (App.java 等)
org.bsdclub.furuta.jalo.lexer    ← 字句解析器 (Lexer/Token/LexerException)
org.bsdclub.furuta.jalo.parser   ← 構文解析器 (Parser/ParserException)
org.bsdclub.furuta.jalo.json     ← JSON モデル定義 (JsonValue sealed hierarchy)
org.bsdclub.furuta.jalo.typecheck ← 静的検査器 (名前解決・特殊フォーム・パターン)
org.bsdclub.furuta.jalo.evaluator ← 評価器 (代数的エフェクト含む)
org.bsdclub.furuta.jalo.runtime  ← ランタイム環境
org.bsdclub.furuta.jalo.stdlib   ← 標準ライブラリ
```

**根拠**: `org.bsdclub.furuta` は殿の所有ドメイン (furuta@furuta.bsdclub.org) に基づく
Java/Maven 標準命名規約 (ドメイン逆順)。`jalo` は本プロジェクトの artifact ID。

### 7.2 サブパッケージ命名指針

各サブパッケージは §1 アーキテクチャ のレイヤ定義と一対一に対応する:

| サブパッケージ | 対応レイヤ (§1) | 主要クラス例 |
|---------|---------|---------|
| lexer | 字句解析 (Lexer) | Lexer, Token, LexerException |
| parser | 構文解析 (Token → JSON モデル変換) | Parser, ParserException |
| json | JSON モデル AST | JsonValue (sealed), JsonNull, JsonBool, JsonNumber, JsonString, JsonArray, JsonObject |
| typecheck | 名前解決・特殊フォーム検証・パターン検証 | TypeChecker, TypeCheckException, Scope |
| evaluator | 評価器 | Evaluator, JaloSignal |
| runtime | ランタイム | Environment, CallStack |
| stdlib | 標準ライブラリ | StdLib, BuiltinFn |

### 7.3 test source set

テストは main と同一パッケージに配置する:

```
app/src/main/java/org/bsdclub/furuta/jalo/lexer/Lexer.java
app/src/test/java/org/bsdclub/furuta/jalo/lexer/LexerTest.java
```

テストフレームワーク: JUnit 5 (Jupiter) + AssertJ (§5.2 参照)。

### 7.4 新規モジュール追加時のチェックリスト

新しいサブパッケージ (`parser` 等) を追加する際は以下を確認:

- [ ] サブパッケージ名が §7.1 の命名指針に従っているか
- [ ] `app/src/main/java/org/bsdclub/furuta/jalo/<name>/` を作成したか
- [ ] `app/src/test/java/org/bsdclub/furuta/jalo/<name>/` を作成したか
- [ ] §1 アーキテクチャ のレイヤ表に対応関係を記載したか

### 7.5 既存実装との対応

**cmd_397 Lexer** が本規約の初適用例:
- `org.bsdclub.furuta.jalo.lexer.Lexer` — `List<Token> tokenize(String input)`
- `org.bsdclub.furuta.jalo.lexer.Token` — sealed interface + 21 record subtypes
- `org.bsdclub.furuta.jalo.lexer.LexerException` — RuntimeException + line/col
