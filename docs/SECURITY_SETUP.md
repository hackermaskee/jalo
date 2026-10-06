# jalo 認証設定記録 (PAT → SSH 移行)

> **実施日**: 2026-05-08  
> **実施者**: Atsushi Furuta (furuta@furuta.bsdclub.org) (手動)  
> **結果**: SSH 鍵認証に切替完了。PAT 痕跡なし。多エージェント運用解禁済。

---

## 実施した手順

### 1. 認証方式の選択
SSH 鍵認証 (既存鍵流用) を採用。

### 2. GitHub 公開鍵登録

```bash
gh ssh-key list  # 登録済み鍵の確認
```

GitHub の `Settings → SSH and GPG keys` に公開鍵が登録済みであることを確認。

### 3. SSH 接続確認

```bash
ssh -T git@github.com
# => Hi hackermaskee! You've successfully authenticated...
```

### 4. remote URL を SSH 形式に変更

```bash
cd ~/work/jalo
git remote set-url origin git@github.com:hackermaskee/jalo.git
```

### 5. 疎通確認

```bash
git fetch origin
# => 正常応答確認
```

### 6. PAT 痕跡確認

```bash
grep -r "github_pat\|://.*@" .git/config
# => 出力なし (PAT 痕跡なし)
```

---

## 完了後の状態

| 項目 | 状態 |
|------|------|
| `origin` URL | `git@github.com:hackermaskee/jalo.git` (SSH形式) |
| `.git/config` PAT痕跡 | なし |
| `ssh -T git@github.com` | hackermaskee 認証通過 |
| `git fetch` | 正常 |
| 旧 PAT revoke | 実施済 |

---

## 多エージェント運用上の注意

- **ssh-agent socket の共有**: （歴史的記録 — multi-agent-shogun 体制時の運用。2026-08 廃止）当時は統括エージェント pane のみに `SSH_AUTH_SOCK` が設定されていたため、push / PR 作成 / merge を同 pane に集約していた。
- 現行体制では、エージェントが Atsushi Furuta の指示または承認に基づき `git push` + `gh pr create` を実施する（CONTRIBUTING.md §1 参照）。
- ssh-agent の pane 間共有設定は別途検討 (ssh_agent 共有 cmd 参照)。

---

## 同種作業での再実施手順 (ワンライナー要約)

```bash
# 1. remote URL を SSH 形式に変更
git remote set-url origin git@github.com:<USER>/<REPO>.git
# 2. SSH 確認
ssh -T git@github.com
# 3. fetch 疎通確認
git fetch origin
# 4. GitHub Settings → tokens で旧 PAT を revoke
```
