# CONTRIBUTING

## 0. 概要
このドキュメントは `hackermaskee/jalo` リポジトリにおけるブランチ運用、PR 作成、レビュー、マージ、main 保護設定の標準手順を定める。対象読者は足軽（codex/qwen 等の実装エージェント）、家老（統括実行）、軍師（レビュー担当）、殿（最終権限者）である。

## 1. 前提・役割分担
本リポジトリは殿のご決裁により、push/PR/merge を家老 pane に集約する。理由は、家老 pane に SSH agent が存在し、認証経路を一本化できるためである。

| ロール | 責務 |
|---|---|
| 足軽 (codex/qwen等) | feature branch 作成 → commit → 家老に PR 依頼 (inbox_write) |
| 家老 (claude pane) | git push + gh pr create + 軍師アサイン + squash merge 実施 |
| 軍師 (gunshi pane) | PR レビュー (Bloom L5 Critiquing) → approve / changes_requested |
| 殿 | main 保護設定・緊急時 override・PAT/SSH 鍵管理 |

ssh-agent 注記: 家老 pane に SSH agent が存在するため push を集約する。

## 2. ブランチ命名規約
- GitHub Flow ベース（`main` 1本 + 短命 feature branch）
- 標準命名: `feature/cmd-NNN-summary`
- 例: `feature/cmd-394-pr-rules`
- 補助プレフィックス（任意）: `fix/cmd-NNN-...` / `chore/...` / `docs/...`
- Conventional Commits との分離:
  - branch 名は cmd ベース
  - commit message は `feat:` / `fix:` などを使用

## 3. PR フロー（通知フロー全体）
(a) 足軽 → 家老（PR 依頼）:
```bash
bash scripts/inbox_write.sh karo "PR 依頼: branch=feature/cmd-NNN-foo cmd=cmd_NNN" pr_request ashigaru<N>
```

(b) 家老 → 軍師（review 依頼、gh pr create 後）:
```bash
bash scripts/inbox_write.sh gunshi "PR review 依頼: <PR URL> cmd=cmd_NNN" review_request karo
```

(c) 軍師 → 家老（review 結果）:
```bash
bash scripts/inbox_write.sh karo "PR review 完了: APPROVE / CHANGES_REQUESTED, comments=<n>" review_done gunshi
```

(d) 家老 → 足軽（merge 完了 or 修正依頼）:
```bash
bash scripts/inbox_write.sh ashigaru<N> "PR merged: feature/cmd-NNN-foo → main" merge_done karo
```

## 3.5 commit 命名規約 (TDD サイクル対応)
TDD サイクルに対応した Conventional Commits 準拠の prefix を使用する:
- `test(red): <description>` - 失敗テスト追加 (Red commit)
- `feat(green): <description>` - 最小実装でテスト通過 (Green commit)
- `refactor: <description>` - リファクタリング (Refactor commit)

軍師確認コマンド (例):
```bash
gh pr view <PR番号> --json commits --jq '.commits[].messageHeadline'
```
出力に `test(red):` / `feat(green):` / `refactor:` が含まれることを確認する。

適用範囲:
- 以下パッケージへの変更を含む PR が適用対象:
  `org.bsdclub.furuta.jalo.lexer` / `parser` / `json` / `syntaxcheck` / `value` / `evaluator`
  (将来追加予定: `runtime` / `stdlib`)

適用範囲外:
- 以下のみへの変更 PR は適用範囲外:
  `docs/*` / `.github/workflows/*` / `build.gradle` / `settings.gradle` / `CONTRIBUTING.md` / `*.md` 等

例外規定:
- refactor PR: 機能変更を伴わないリネーム・型階層変更等は `refactor:` commit のみで可。
  ただし PR description で `TDD N/A: refactor-only` と明示すること。
- 緊急修正 PR: 殿の override 発動時は適用範囲外。ただし事後 dashboard 記録必須。
- docs PR: ドキュメントのみの変更 (`CONTRIBUTING.md` / `SPEC.md` / `DESIGN.md` 等) は適用範囲外。

違反時対処:
- 軍師は PR の commit 履歴を確認し、適用範囲内 PR で TDD prefix が付与されていない場合、
  機械的根拠 (commit log を引用) と共に `changes_requested` を発動する。
- PR 提出者は修正後に re-request review すること。

squash merge 前の feature branch でこの prefix が付いていれば、
軍師が `gh pr view --json commits` で TDD 遵守を機械的に確認可能。

上位規範との接続:
- 本規約は cmd_396 で確立した t-wada 流 TDD 方針 (Red→Green→Refactor 短サイクル) の実装規約化である。
- 機械化 (GitHub Actions による commit prefix lint) は将来 cmd の候補とする。

## 3.6 Javadoc 規約

### 適用条件
殿の memo 指摘に基づき、以下の場合は Javadoc を省略可能とする。
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
Parser / JSON Model / SyntaxChecker / Evaluator / Runtime / Stdlib

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

## 3.7 マージゲート規律 (内部規則)

### ① 二重ゲート原則
- jalo の PR は「軍師 approve + CI green」が揃った時点で merge 可能とする。
- merge 実行は家老が `gh pr merge --squash` で行う。
- §3.5 (commit 規約) は人間規律、§3.6 (Javadoc 規約) は機械強制 (checkstyle)、§3.7 (本節) は両者を統合する merge ゲート規律と位置づける。

### ② GitHub 側強制との関係
- §7 main 保護ルールの `required_approving_review_count` は `0` とする。
- 理由: 自動化 user が PR 作成する場合、GitHub の自己 PR 承認禁止と衝突するため。
- 機械強制は CI green (status check) に絞り、人間 review は本節の内部規則で運用する。

### ③ 殿の自己 PR ケース
- CI green は例外なく必須とする。
- 軍師 approve は推奨とし、以下カテゴリは skip 可 (PR description に明記必須)。
  1. docs only PR (実装変更ゼロ)
  2. 設定変更のみ PR (build.gradle / CI workflow 等)
  3. 緊急 hotfix (運用障害復旧)
  4. trivial 修正 (typo / コメント修正等)
- 実装変更を含む場合は skip 不可とする。
- skip 時 PR description 文例:
  `self-approved by lord (gunshi review skipped per §3.7: <カテゴリ>)`

### ④ 違反検出と事後対応
- 家老は merge 前に軍師 approve を確認する (dashboard または `queue/reports/gunshi_report.yaml`)。
- 軍師は cmd 完了 cross-check として、merge 済 PR に approve が揃っているか確認する。
- 違反検出時は `dashboard.md` の 🚨要対応 に記録し、再発防止アクションを付記する。

## 4. マージ方式
- Default: Squash merge（家老が `gh pr merge --squash` を実施）
- 理由: 足軽の WIP commit を整理し、`main` の履歴を clean に保つ
- 例外: commit が 1-3 個で各々が意味ある単位なら Rebase merge を選択可能
- Merge commit: 使用禁止

判定基準:
- 「commit が WIP/微修正混在 → Squash」
- 「commit が 1-3 個で意味ある単位 → Rebase」

## 5. git push 例外規定 (MEMORY.md との整合)
本リポジトリ (hackermaskee/jalo) は殿のご決裁により、家老 pane が
git push / gh pr create / merge を代行します。
これは MEMORY.md「git push 永久禁止」ルールの「殿の明示的承認による例外」に該当します。

他のリポジトリ (multi-agent-shogun, freebsd-env, docling 等) では
引き続き push 禁止が継続します。本例外は jalo 専用です。

## 6. 軍師レビュー観点 (Bloom L5 Critiquing 4観点)
1. 設計判断 (Critiquing)
- cmd の北極星と整合しているか
- 代替案を検討したか

2. コード品質 (Differentiating)
- 可読性・命名・SRP・冗長性に問題がないか

3. 受入基準充足 (Checking)
- 表面的網羅ではなく、実質達成できているか
- **TDD チェックリスト** (コアロジック PR): テストファースト / Green commit 独立 /
  commit 命名規約 (`test(red)`/`feat(green)`/`refactor:`) の遵守を確認
  (設定・ビルド PR は N/A 可)

4. アーキテクチャ一貫性 (Organizing)
- 既存資産との整合、命名規約の一貫性が保たれているか

- approve 基準: 受入基準充足 + 設計妥当 + 改善余地は minor のみ
- changes_requested 基準: 受入基準未充足 OR 設計上の重大な懸念

## 7. main 保護ルール (殿の手動設定チェックリスト)
実施者: 殿（GitHub Settings → Branches → Add rule for main）

- [ ] Require a pull request before merging: ON
- [ ] Require approvals: 0 (GitHub 強制なし、§3.7 内部規則で運用)
  - cmd_409 変更理由: 自動化 user の自己 PR 承認禁止衝突を避けるため、1 → 0 に変更。
- [ ] Dismiss stale pull request approvals when new commits are pushed: ON
- [ ] Require linear history: ON (merge commit 混入防止)
- [ ] Do not allow bypassing the above settings: ON (殿のみ emergency override)
- [ ] Restrict force pushes: ON (main 履歴破壊防止)

GitHub Repo Settings 推奨:
- Allow merge commits: OFF
- Allow squash: ON
- Allow rebase: ON

branch protection 変更手順:
1. `gh api` を使う例 (殿が手動実施):
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
3. 家老 pane から直接変更してはならない。main 保護設定の実施権限は殿のみ。

## 8. トラブルシュート
(a) 家老 pane で git push 失敗（ssh-agent 未ロード）:
```bash
ssh-add -l
ssh-add ~/.ssh/id_ed25519
```

(b) 家老 pane 障害時の代替:
- 殿 pane で直接 git push（緊急時のみ）

(c) 軍師 review timeout:
- 4 分後自動 /clear → inbox 再処理

## 9. 受入基準 (documentation-structured-tech-guide スキル)
- [ ] 4 層役割表が正確に記載されている
- [ ] PR フロー通知コマンド例 (a)〜(d) が全て含まれている
- [ ] git push 例外規定セクションが MEMORY.md 整合を明記
- [ ] Squash/Rebase の判定基準が明記されている
- [ ] 軍師レビュー 4 観点が approve/changes_requested 基準つきで記載
- [ ] main 保護ルール 6 項目チェックリストが含まれている
