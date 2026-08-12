# CONTRIBUTING

## 0. 概要

このドキュメントは `hackermaskee/jalo` リポジトリにおけるブランチ運用、実装規律、検証、PR 作成、マージ、main 保護設定の標準手順を定める。

開発体制は **Atsushi Furuta (furuta@furuta.bsdclub.org、最終権限者) + AI コーディングエージェント** の 2 者構成である。かつての multi-agent-shogun 体制（実装・統括・レビューを複数エージェントの役割として分担）は 2026-08 に廃止した。現行のエージェントは単一セッション内で実装と一定水準の検証を遂行できるため、多段の役割分担フローは廃し、独立した文脈が本質的に必要な場面（§6 敵対的検証）に限って別セッション/サブエージェントを用いる。

## 1. 役割分担

| ロール | 責務 |
|---|---|
| Atsushi Furuta | 方針決定・言語仕様の裁可・PR 最終確認と merge 承認・main 保護設定・SSH/PAT 鍵管理 |
| エージェント | 実装・テスト・ドキュメント整備・セルフレビュー・敵対的検証の実施・push / PR 作成（指示または承認に基づく） |

- 言語仕様（`docs/SPEC.md` および意味論に関わる決定）の最終決定権は常に Atsushi Furuta にある。エージェントは提案と検証を行い、裁可を仰ぐ。
- `git push` / `gh pr create` はエージェントが代行してよい。merge は §7 のゲートを満たしたうえで Atsushi Furuta の承認を要する。

## 2. ブランチ命名規約

- GitHub Flow ベース（`main` 1 本 + 短命 feature branch）
- 標準命名: `<type>/<topic-summary>`
  - `<type>`: `feature` / `fix` / `docs` / `chore` / `refactor`
  - 例: `feature/macro-expansion-pass`, `docs/adr-002-revision`
- `docs/ISSUES.md` の項目に対応する場合は ID を含めてよい: `feature/i14-macro-expansion`
- 旧体制の `feature/cmd-NNN-...` 形式は歴史的経緯によるもの。既存 branch・commit message 中の `cmd_NNN` 参照はそのまま有効だが、新規 branch では使用しない。
- Conventional Commits との分離: branch 名は topic ベース、commit message は `feat:` / `fix:` 等を使用する。

## 3. 変更フロー

1. **課題整理** — 設計上の論点は `docs/ISSUES.md` に記録する。言語仕様の大きな決定は ADR（`docs/DECISION_*.md`）として起草する。
2. **branch 作成** — §2 に従う。
3. **実装** — TDD（§4）。
4. **セルフ検証** — `./gradlew build`（test + checkstyle 込み）を green にする。
5. **敵対的検証** — 言語仕様に関わる変更は §6 を必須で実施する。
6. **push + PR 作成** — PR テンプレート（`.github/pull_request_template.md`）に従って記載する。
7. **マージゲート確認 → squash merge** — §7。
8. **ドキュメント連動更新** — `SPEC.md` / `DESIGN.md` / `ISSUES.md` の整合を保つ（`CLAUDE.md` の Document Maintenance 参照）。解決した論点の `ISSUES.md` への反映を忘れない。

## 4. commit 命名規約（TDD サイクル対応）

TDD サイクルに対応した Conventional Commits 準拠の prefix を使用する:
- `test(red): <description>` - 失敗テスト追加 (Red commit)
- `feat(green): <description>` - 最小実装でテスト通過 (Green commit)
- `refactor: <description>` - リファクタリング (Refactor commit)

確認コマンド (例):
```bash
gh pr view <PR番号> --json commits --jq '.commits[].messageHeadline'
```
出力に `test(red):` / `feat(green):` / `refactor:` が含まれることを確認する。

適用範囲:
- 以下パッケージへの変更を含む PR が適用対象:
  `org.bsdclub.furuta.jalo.lexer` / `parser` / `jq` / `syntaxcheck` / `value` / `evaluator`
  (将来追加予定: `runtime` / `stdlib`)

適用範囲外:
- 以下のみへの変更 PR は適用範囲外:
  `docs/*` / `.github/workflows/*` / `build.gradle` / `settings.gradle` / `CONTRIBUTING.md` / `*.md` / `repl` パッケージ（対話 UI）等

例外規定:
- refactor PR: 機能変更を伴わないリネーム・型階層変更等は `refactor:` commit のみで可。
  ただし PR description で `TDD N/A: refactor-only` と明示すること。
- docs PR: ドキュメントのみの変更は適用範囲外。

上位規範との接続:
- 本規約は t-wada 流 TDD 方針 (Red→Green→Refactor 短サイクル) の実装規約化である。
- 機械化 (GitHub Actions による commit prefix lint) は将来課題とする。

## 5. Javadoc 規約

### 適用条件
以下の場合は Javadoc を省略可能とする。
1. テストコード (`src/test/` 配下)
2. `private` メソッド
3. `@Override` メソッドで、override 元の Javadoc で十分な場合
4. setter/getter

`@Override` の省略は、以下 3 条件をすべて満たす場合のみ許可する。
1. super 実装と同等の挙動
2. 独自意味論なし (独自フォーマット、NaN 扱い変更などがない)
3. 副作用・パフォーマンス特性が super と同一

いずれかを満たさない場合は追記必須とする。
例: `fillInStackTrace` override のような性能特性変更は追記必須。

### 言語規約
- Javadoc は **英語のみ** とする。
- 既存の日本語 Javadoc は英訳する。

### 文体規約 (DbC 意識)
Design by Contract (DbC, Bertrand Meyer) を意識して記述する。

文頭 (一文目サマリ):
- 3 人称現在動詞で開始する: Returns / Computes / Validates / Parses / Binds / Evaluates / Checks
- 良い例: `Returns the tokenized list of the input string.`
- 悪い例: `Tokenize the input string.` / `Tokenized list.`

`@param` (precondition):
- 呼び出し側責任を明示する。
- 良い例: `@param name variable name to look up; must not be null`
- 悪い例: `@param name the name`

`@return` (postcondition):
- 名詞句 (`the ...`) で記述する。
- 良い例: `@return the bound {@link JaloValue}, never {@code null}`
- 悪い例: `@return Returns the value.`

`@throws` (invariant/exception):
- `if ...` 形式で発動条件を明示する。可能なら SPEC 章を参照する。
- 良い例: `@throws LexerException if the input contains an unrecognized character (SPEC §3.1)`
- 悪い例: `@throws LexerException Lexer error.`

### クラス Javadoc 構成
以下の構成を標準とする。

```java
/**
 * <One-sentence summary in 3rd person present tense.>
 *
 * <p>Layer: <Layer name> (per DESIGN.md §1 architecture table).
 * <Detailed responsibility paragraph.>
 *
 * @see <RelatedClass>
 * @see <a href="../../../docs/SPEC.md#X">SPEC §X reference</a>
 */
```

Layer 名は `DESIGN.md` §1 のアーキテクチャ表に合わせる:
Parser / JSON Model / SyntaxChecker / Evaluator / REPL / Runtime / Stdlib

### `@implSpec` / `@implNote` タグ
Java 9+ タグを採用する。
- `@implSpec`: overrider が守るべき実装仕様 (サブタイプへ継承される契約)
- `@implNote`: 実装詳細メモ (API 利用者向けではない設計意図)

使用例:
- `@implSpec`: `Evaluator.eval` (sealed switch ディスパッチ規約), `JaloFunction.apply` (評価順序)
- `@implNote`: `Environment.bind` (flat-flatten 設計), `JaloEffectSignal.fillInStackTrace` (stack trace 抑制)

### 推奨タグ整理
| タグ | 用途 |
|---|---|
| `@param` | 事前条件 (precondition) |
| `@return` | 事後条件 (postcondition) |
| `@throws` | 例外条件 (invariant 違反) |
| `@implSpec` | 実装仕様 (overrider への契約) |
| `@implNote` | 実装メモ (設計選択理由) |
| `{@link}` | 関連クラス・メソッドへのリンク |
| `{@code}` | コード断片のインライン表示 |

## 6. 敵対的検証 (Adversarial Verification)

### 目的
実装・起草した本人（同一文脈のエージェント）による確認は、実装時の前提や思い込みをそのまま検証にも引き継ぐ。言語仕様は jalo の根幹であり、一度 merge した意味論の変更は撤回コストが極めて高い。そこで、仕様に関わる変更には「**変更を不成立にする反例を積極的に探す**」独立検証を必須ゲートとして課す。

### 適用対象（いずれかに該当する PR は必須）
- `docs/SPEC.md` の構文・型・意味論・組み込み関数・パターンの変更（typo・書式修正を除く）
- ADR（`docs/DECISION_*.md`）の新規 Accepted 化、または決定内容の変更
- `evaluator` / `syntaxcheck` / `parser` / `lexer` / `jq` / `value` パッケージにおける観測可能な挙動の変更

### 実施要領
1. **独立文脈で実施する**: 実装・起草した文脈とは別の文脈で行う。具体的には、実装時の意図・要約を引き継がない新しいエージェントセッション（またはサブエージェント）に、変更 diff と関連ドキュメントのみを与える。
2. **反証タスクとして指示する**: 「この変更は正しいか確認せよ」ではなく「**この変更を不成立にする反例・矛盾・未定義動作を探せ。見つからないと結論する場合は、試みた攻撃を列挙せよ**」と指示する。
3. **最低限の検証観点**:
   - 既存 SPEC 条項との矛盾・非整合
   - 反例プログラムの構成（空リスト / ネスト / シャドーイング / 自由変数 / 深い再帰 / エラー系入力）
   - 未定義動作の発生（仕様に書かれていない入力に対する挙動）
   - 後方互換性（既存テスト・既存サンプルコードが壊れないか）
   - 他 Lisp 処理系（Scheme / Clojure / Common Lisp）との相違が意図的なものか
4. **反例のテスト化**: 検出した反例は、修正の有無にかかわらず実行可能なテストに落とす。仕様文書側の問題であれば SPEC / ISSUES.md への追記で確定させる。
5. **PR への記録**: 結果を PR description の「敵対的検証」欄に記載する。
   書式例: `敵対的検証: 実施済（独立セッション）— 反例 2 件検出 → 1 件修正 / 1 件は仕様判断として ISSUES.md I-XX に登録`
6. **エスカレーション**: 反例が変更の前提そのものを崩す場合は merge せず、Atsushi Furuta の裁可を仰ぐ。

### 適用除外
typo・書式・リンク修正のみの変更は N/A とし、PR に `敵対的検証: N/A（書式のみ）` と明記する。

## 7. マージゲートとマージ方式

### 7.1 ゲート条件（すべて必須）
1. **CI green** — `.github/workflows/ci.yml` の Test (JDK 21) job。例外なし。
2. **敵対的検証** — §6 適用対象 PR は実施と PR 記載が必須。
3. **Atsushi Furuta の承認** — GitHub 上の approve、またはセッション内での明示承認。docs typo 等の trivial 修正は、事前委任がある場合に限り省略可（PR に明記）。

GitHub 側の `required_approving_review_count` は `0` とする（自動化アカウントの自己 PR 承認禁止との衝突回避）。人間承認は本節の内部規則として運用する。

### 7.2 マージ方式
- Default: Squash merge（`gh pr merge --squash`）
- 理由: WIP commit を整理し、`main` の履歴を clean に保つ
- 例外: commit が 1-3 個で各々が意味ある単位なら Rebase merge を選択可能
- Merge commit: 使用禁止

判定基準:
- 「commit が WIP/微修正混在 → Squash」
- 「commit が 1-3 個で意味ある単位 → Rebase」

## 8. main 保護ルール（Atsushi Furuta の手動設定チェックリスト）
実施者: Atsushi Furuta（GitHub Settings → Branches → Add rule for main）

- [ ] Require a pull request before merging: ON
- [ ] Require approvals: 0 (GitHub 強制なし、§7.1 内部規則で運用)
- [ ] Dismiss stale pull request approvals when new commits are pushed: ON
- [ ] Require linear history: ON (merge commit 混入防止)
- [ ] Do not allow bypassing the above settings: ON (Atsushi Furuta のみ emergency override)
- [ ] Restrict force pushes: ON (main 履歴破壊防止)

GitHub Repo Settings 推奨:
- Allow merge commits: OFF
- Allow squash: ON
- Allow rebase: ON

branch protection 変更手順:
1. `gh api` を使う例 (Atsushi Furuta が手動実施):
```bash
gh api \
  --method PUT \
  -H "Accept: application/vnd.github+json" \
  /repos/hackermaskee/jalo/branches/main/protection \
  -F required_status_checks.strict=true \
  -F required_status_checks.contexts[]='build-and-test' \
  -F enforce_admins=true \
  -F required_pull_request_reviews.required_approving_review_count=0 \
  -F required_pull_request_reviews.dismiss_stale_reviews=true \
  -F restrictions=
```
2. GitHub Web UI を使う例:
   `Settings → Branches → main rule Edit` で `Require approvals` を `0` に変更。
3. main 保護設定の実施権限は Atsushi Furuta のみ。エージェントは変更してはならない。

## 9. バージョン bump 手順

- 言語仕様変更を含む PR では、バージョン bump を必須とする。
- PR description に、bump 種別を明記すること。
  - 軽微な変更: `y` bump
  - 重大な変更: `x` bump
- bump の妥当性 (変更内容と bump 種別の整合) は PR レビュー時に確認する。判断に迷う場合は Atsushi Furuta の裁可を仰ぐ。
