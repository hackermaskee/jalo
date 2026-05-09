## cmd_id
cmd_NNN

## 概要 (Summary)
<!-- 1-3 文で変更内容を説明 -->

## 受入基準充足
<!-- task YAML の受入基準を転記し、各々チェック -->
- [ ] <criterion 1>
- [ ] <criterion 2>

## テスト結果
<!-- 実行コマンドと結果を記載 -->
```bash
<test command>
```
Result: PASS / FAIL

## TDD 遵守 (§3.5 準拠)
<!-- 適用対象: lexer/parser/json/syntaxcheck/value/evaluator パッケージを含む PR -->
<!-- 確認コマンド: gh pr view <N> --json commits --jq '.commits[].messageHeadline' -->
- [ ] test(red): commit が feat(green): commit より先に存在する
- [ ] feat(green): commit がリファクタと分離されている
- [ ] テストリスト記載 (下記「## テストリスト」セクション)
- [ ] 適用範囲外 / 例外 (N/A): 理由: refactor-only / docs / 設定 / 緊急修正 など

## テストリスト (TDD 適用時)
<!-- 実装着手前に作成、達成済みは [x] に変更 -->
- [ ] <test 1>
- [ ] <test 2>

## 軍師レビュー観点 (Bloom L5 Critiquing)
- [ ] 設計判断: cmd の北極星と整合しているか
- [ ] コード品質: 命名・SRP・冗長性の問題なし
- [ ] 受入基準充足: 表面的でなく実質的に達成しているか
- [ ] アーキテクチャ一貫性: 既存資産・命名規約と整合

## 関連
- cmd YAML: queue/tasks/...
- report: queue/reports/...
- gunshi_report: queue/reports/gunshi_report.yaml

## 変更スコープ
- 足軽担当: <feature branch での実装範囲>
- 家老担当: push / PR 作成 / merge
- 軍師担当: L5 Critiquing レビュー
