# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a design-stage project for **jalo** — a Lisp-like language that uses the JSON data model as its AST instead of S-expressions.

## ドキュメント参照ガイド

- `SPEC.md` — 言語仕様 (構文・型・意味論・組み込み関数・パターン)
- `docs/DESIGN.md` — 実装設計 (アーキテクチャ・モジュール・設計判断・テスト戦略)
- `docs/ISSUES.md` — 設計上の未解決事項・解決済み事項トラッカー
- `docs/SECURITY_SETUP.md` — SSH 認証設定記録
- `CONTRIBUTING.md` — PR フロー・branch 命名・役割分担

## 基本開発コマンド

```bash
./gradlew build
./gradlew test
./gradlew run
```

## Document Maintenance

### docs/ISSUES.md

`docs/ISSUES.md` tracks open and resolved design issues. When a design question is settled (e.g., a decision is recorded in `SPEC.md`), update the corresponding entry in `docs/ISSUES.md`:

- Change `**状態**: 未定義` / `**状態**: 未設計` / `**状態**: 未定` / `**状態**: 要確認` → `**状態**: 解決済み`
- Replace the issue body with a one-line summary of the decision and a pointer to where it is documented (e.g., `SPEC.md §X.Y`).
