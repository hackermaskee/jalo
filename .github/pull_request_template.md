## 概要 (Summary)
<!-- 1-3 文で変更内容を説明 -->

## 変更種別
<!-- 該当するものを残す -->
- [ ] 言語仕様変更 (SPEC / ADR / 意味論) → 敵対的検証 + バージョン bump 必須
- [ ] 実装 (仕様変更なし)
- [ ] docs / 設定のみ

## テスト結果
<!-- 実行コマンドと結果を記載 -->
```bash
<test command>
```
Result: PASS / FAIL

## TDD 遵守 (CONTRIBUTING §4 準拠)
<!-- 適用対象: lexer/parser/jq/syntaxcheck/value/evaluator パッケージを含む PR -->
<!-- 確認コマンド: gh pr view <N> --json commits --jq '.commits[].messageHeadline' -->
- [ ] test(red): commit が feat(green): commit より先に存在する
- [ ] feat(green): commit がリファクタと分離されている
- [ ] テストリスト記載 (下記「## テストリスト」セクション)
- [ ] 適用範囲外 / 例外 (N/A): 理由: refactor-only / docs / 設定 など

## テストリスト (TDD 適用時)
<!-- 実装着手前に作成、達成済みは [x] に変更 -->
- [ ] <test 1>
- [ ] <test 2>

## 敵対的検証 (CONTRIBUTING §6 準拠)
<!-- 言語仕様に関わる変更は必須。独立文脈 (別セッション/サブエージェント) で反証を試みる -->
- [ ] 実施済 — 結果を下記に記載
- [ ] N/A — 理由: <書式のみ / 仕様に関わらない実装詳細 など>

実施結果:
<!-- 例: 実施済（独立セッション）— 反例 2 件検出 → 1 件修正 / 1 件は ISSUES.md I-XX に登録 -->
<!-- 反例が見つからなかった場合は、試みた攻撃 (検証観点) を列挙 -->

## レビュー観点
- [ ] 設計判断: 変更の目的と整合しているか、代替案を検討したか
- [ ] コード品質: 命名・SRP・冗長性の問題なし
- [ ] 受入基準充足: 表面的でなく実質的に達成しているか
- [ ] アーキテクチャ一貫性: 既存資産・命名規約と整合

## バージョン bump (言語仕様変更時)
- 種別: x / y / N/A
- 根拠:

## 関連
- ISSUES.md: <I-XX>
- ADR / SPEC: <docs/DECISION_*.md §X / docs/SPEC.md §X>
