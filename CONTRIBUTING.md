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

squash merge 前の feature branch でこの prefix が付いていれば、
軍師が `gh pr view --json commits` で TDD 遵守を機械的に確認可能。
適用範囲: コアロジック PR (設定・ビルド PR は通常の commit 命名を使用)。

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
- [ ] Require approvals: 1 (軍師 approve 必須)
- [ ] Dismiss stale pull request approvals when new commits are pushed: ON
- [ ] Require linear history: ON (merge commit 混入防止)
- [ ] Do not allow bypassing the above settings: ON (殿のみ emergency override)
- [ ] Restrict force pushes: ON (main 履歴破壊防止)

GitHub Repo Settings 推奨:
- Allow merge commits: OFF
- Allow squash: ON
- Allow rebase: ON

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
