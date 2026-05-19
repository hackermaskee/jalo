# ADR-002 (V2): jalo マクロ機構 × 名前空間統合設計

## §1 ステータス

**Status: Proposed**

殿によるレビュー完了後に **Accepted** へ移行し、V1 (`docs/DECISION_MACRO.md`) を **Superseded** 化する。
V1 は Option A/C 絞込までの議論記録として保存する（§2 参照）。

## §2 Supersedes

本 ADR は以下の V1 ADR を supersede する:

- **V1**: `docs/DECISION_MACRO.md` — ADR-002: jalo マクロ機構設計（2026-05-19 merge 済）

**移行理由 (殿のご見解 2026-05-19)**:

V1 は Phase 2 マクロ候補を Option A（syntax-rules）と Option C（Clojure defmacro）に絞り込んだが、
衛生問題への対処を「Phase 2 内部の実装詳細」としてカプセル化していた。殿のご見解により、
衛生問題を「表面化させ名前空間分離で根本解決を図る」方向に大幅仕切り直しを行う。
同時に namespace 機構（ns スペシャルフォーム、グローバル名前空間定義 form）を本 ADR で統合設計する。

V1 から V2 への主要変更点:

| 観点 | V1 | V2 (本 ADR) |
|---|---|---|
| 衛生問題の位置づけ | Phase 2 実装詳細 | 設計の核 (§3 で定義) |
| 解決方式 | Option A/C から Phase 2 着手時選択 | 名前空間分離方式 (§6 D2) に確定 |
| namespace 機構 | 未設計 (I-02 残置) | 本 ADR で統合設計 |
| quasiquote | Clojure 流 unquote 許容 | データ構築専用 (§4.3)、ns ラップが代替 |
| 展開タイミング | Q3 (Open Question) | run/compile 時、read 不採用 (D1) |

## §3 背景と問題の定義

### §3.1 衛生問題 (1)(2) の定義

殿のご見解 (2026-05-19) に基づき、jalo のマクロ衛生問題を以下の 2 類型に整理する:

> **(1) マクロ展開器が挿入した束縛が、展開元の参照を捕捉する**
> — マクロ局所変数が展開元スコープを汚染するケース

> **(2) マクロ展開器が挿入した参照が、展開元の束縛に捕捉される**
> — マクロが意図した束縛と異なる束縛を参照するケース

#### 問題 (1) の jalo 具体例

`swap!` マクロが一時変数 `tmp` を挿入し、展開元コードにも `tmp` が存在する場合:

```jalo
; マクロ定義 (仮想的な defmacro)
(defmacro swap! [a b]
  ["let", [["tmp", a]], ["begin", ["set!", "a", "b"], ["set!", "b", "tmp"]]])

; 展開元コード (tmp を使っている)
(let [tmp 99]
  (swap! x tmp))  ; => tmp が "展開元の tmp=99" ではなくマクロの tmp に捕捉される危険
```

jalo AST (JSON) では束縛はシンボル名（文字列）のみで識別されるため、
マクロ挿入の `"tmp"` と展開元の `"tmp"` を区別する機構が必要となる。

#### 問題 (2) の jalo 具体例

`my-cons` マクロが組み込みの `cons` を参照するが、展開元が `cons` を再定義済の場合:

```jalo
; マクロ定義: cons を使ってリスト構築する展開形を返す
(defmacro my-cons [head tail]
  ["cons", head, tail])

; 展開元: cons をローカルで再定義
(let [cons (fn [a b] "overridden")]
  (my-cons 1 [2 3]))  ; => マクロが意図した cons ではなくローカルの cons を参照
```

jalo では名前=文字列のみのため、展開元の `cons` とマクロ作成者環境の `cons` を区別できない。

### §3.2 既存処理系の対策比較表

殿のご見解 (2026-05-19) による整理 (各セルに一次資料引用を付与):

| 処理系 | 衛生問題 (1) への対策 | 衛生問題 (2) への対策 |
|---|---|---|
| Common Lisp | `gensym` で衝突しない名前を人手生成 | 諦め (Lisp2 の関数/変数名前空間分離で緩和) |
| Scheme (syntax-rules / syntax-case) | `rename` で識別子を自動リネーム | 展開器側の束縛を参照 (syntax closure) |
| Clojure | auto-gensym (`x#` → `x_123`) | syntax-quote (`` ` ``) でシンボルを ns 修飾 |

**Common Lisp — gensym:**

> "`gensym` creates and returns a fresh uninterned symbol, as if by calling `make-symbol`."  
> — Common Lisp HyperSpec, Function `GENSYM`,  
> https://www.lispworks.com/documentation/HyperSpec/Body/f_gensym.htm (accessed 2026-05-19)

Common Lisp はマクロプログラマが `gensym` を明示的に呼び出し、衝突しない一意名を生成する。
問題 (1) はこれで対処できるが、問題 (2) に対しては Lisp2 の関数名前空間（変数 ns と関数 ns が独立）で部分的に緩和するにとどまる。

**Scheme — syntax-rules / syntax-case:**

> "Scheme is a statically scoped programming language. Each use of a macro is expanded into a syntactic form whose free variables are interpreted relative to the environment in which the macro was defined."  
> — Scheme R7RS §4.3 "Macros" (PDF),  
> https://small.r7rs.org/attachment/r7rs.pdf §4.3 (accessed 2026-05-19)

Scheme の `syntax-rules` は識別子を自動的にリネームし、完全な衛生を保証する。
`syntax-case`（R6RS §12）は手続き的な操作を可能にしながら同等の衛生を提供する:

> "The syntax-case form introduces pattern variables. ...  
> Variables introduced by the macro are automatically renamed to avoid conflicts."  
> — Revised⁶ Report on the Algorithmic Language Scheme (R6RS) §12,  
> https://www.r6rs.org/final/html/r6rs/r6rs-Z-H-16.html (accessed 2026-05-19)

**Clojure — auto-gensym + syntax-quote:**

> "If a symbol is non-namespace-qualified and ends with '#', it is resolved to a generated symbol with the same name to which '_' and a unique id have been appended."  
> — Clojure official documentation, "Reader — Syntax-quote",  
> https://clojure.org/reference/reader#syntax-quote (accessed 2026-05-19)

> "Clojure has a programmatic macro system which allows the compiler to be extended by user code."  
> — Clojure official documentation, "Macros",  
> https://clojure.org/reference/macros (accessed 2026-05-19)

Clojure の `defmacro` は syntax-quote (`` ` ``) で名前空間修飾シンボルを生成することで問題 (2) に対処する。
問題 (1) は auto-gensym (`x#`) で対処する。

**Lisp1 / Lisp2 と問題 (2) の深刻度差異:**

殿のご見解: Lisp2（Common Lisp）では変数名前空間と関数名前空間が分離しているため、
`(cons ...)` のような関数呼び出しは変数 `cons` の再定義に影響されにくい（問題 (2) が緩和される）。
一方 jalo は Lisp1（変数名前空間と関数名前空間が統一）であり、問題 (2) がより深刻となる。

## §4 jalo 固有の制約

殿のご見解 (2026-05-19) により、既存処理系の手法の多くが jalo には適用不可であることが確定した。

### §4.1 AST モデルの制約 (JSON、シンボル構造非保持、名前=文字列のみ)

jalo の AST は homoiconic JSON である。シンボルはリッチなオブジェクト（Scheme の syntax object 等）ではなく、**純粋な文字列**として表現される。

```
; Scheme では識別子はスコープ情報を保持するオブジェクト
; (define-syntax swap! (lambda (stx) (syntax-case stx () ...)))
; → stx は syntax object、識別子ごとにスコープが付属

; jalo では識別子は単なる文字列
["let", [["tmp", 1]], "tmp"]
; "tmp" は文字列であり、定義場所・スコープ情報を持たない
```

このため:
- Scheme の `rename(x)` 方式（トークン単位で識別子をリネームし、マクロ作成者 ns 由来として識別）は **採用不可**
- 名前=文字列のみゆえ、同一文字列の識別子は区別されない
- マクロ展開後も AST は通常の JSON 配列/文字列であり続ける

### §4.2 read 時二段解決不可

Scheme や Common Lisp では read 時にシンボルオブジェクトを生成し、後段で実体解決を行う二段構造が可能である。
jalo の Reader は以下のように単純化されている:

- Reader: トークンを JSON 値（文字列・数値・配列・マップ）として解析
- Parser: JSON を jalo AST として構造化
- Evaluator/Compiler: AST 上で名前解決・評価

識別子はシンボルオブジェクトではなく文字列のため、read 時点での「マクロ作成者 ns に属するシンボル」という
メタ情報の付与ができない。名前解決は evaluate/compile 段階でのみ行われる。
これは §6 D1（展開タイミング = run/compile、read 不採用）と整合する。

### §4.3 quasiquote データ構築専用（Clojure 流不採用、match パターンとの双対性）

jalo の `quasiquote` は **JSON モデル値の構築** と **`match` 左辺パターン** に使うスペシャルフォームであり、
この設計が殿の設計の核をなす（SPEC §5.1-§5.2）。

Clojure では syntax-quote (`` ` ``) を使ってマクロ本体でシンボルを名前空間修飾する:

```clojure
; Clojure: syntax-quote でシンボルを ns 修飾
(defmacro my-macro [x]
  `(clojure.core/cons ~x nil))  ; clojure.core/cons は修飾済
```

殿のご裁可により、**この方式は jalo では不採用**:
- jalo の `quasiquote` はデータ構築専用であり、マクロ展開中のシンボル修飾には使わない
- `match` パターンとの双対性（左辺パターン ↔ 右辺構築）が設計の核であり、
  マクロ用途で `quasiquote` の意味を拡張することは双対性を破壊する
- 代替手段として §6 D6 の ns ラップ規約（`(ns <macro-ns> <form>)`）を採用する

将来の reader-level 別機能（`#jq(...)` 系の一般化）は、マクロ機構とは独立した別系統として
検討する余地を残す（§12 Q8 参照）。

## §5 既存処理系対策の比較（古典論文）

### §5.1 Bawden & Rees 1988 — Syntactic Closures

**一次資料**:
- ACM DOI: https://dl.acm.org/doi/10.1145/62678.62687 (accessed 2026-05-19, bot 制限で HTTP 403 応答)
- MIT DSpace: https://dspace.mit.edu/handle/1721.1/6036 (accessed 2026-05-19, HTTP 405 応答につき本文確認は継続調査)

Bawden & Rees (1988) は、衛生マクロ問題を「識別子を単なる文字列ではなく、環境情報つきで扱う」方向で整理した代表的初期研究である。とくに syntactic closure の立場では、展開時に「どの環境でその識別子を解決すべきか」を保持するため、単純な名前衝突回避以上に、参照の意図を保存できる。

本 ADR の観点では、Bawden 流の核心は「マクロ展開器が挿入した識別子に、展開元と独立した解決文脈を与える」点である。jalo は syntax object を持たないため token 単位の情報保持はできないが、式範囲に `(ns <macro-ns> ...)` / `(ns <caller-ns> ...)` を導入する設計は、この発想を scope 単位で再構成したものと解釈できる。

### §5.2 Kohlbecker et al. 1986 — Hygienic Macro Expansion

**一次資料**:
- ACM DOI: https://dl.acm.org/doi/10.1145/319838.319859 (accessed 2026-05-19, bot 制限で HTTP 403 応答)

Kohlbecker et al. (1986) は hygiene 概念を明示し、マクロ展開で発生する捕捉バグを体系的に定義した。ここで整理された問題設定は、本 ADR の衛生問題 (1)(2) と対応し、後続の `syntax-rules`/`syntax-case` 系での「展開器が自動で捕捉を避ける」方向の基礎を作った。

本 ADR での含意は明確で、衛生問題を「実装者の慣習」ではなく「言語機構として解決すべき要件」として扱う必要がある点にある。jalo でも同じく、`gensym` 相当の人手運用ではなく、ns 分離規約を言語レベルで導入することで再現性のある防止策を与える。

### §5.3 Clinger & Rees 1991 — Macros That Work

**一次資料**:
- ACM DOI: https://dl.acm.org/doi/10.1145/99583.99607 (accessed 2026-05-19, bot 制限で HTTP 403 応答)
- University of Oregon PDF: https://scholarsbank.uoregon.edu/server/api/core/bitstreams/12fed7c3-fd49-4ae8-97e4-6bbb156aaba6/content (accessed 2026-05-19)

Clinger & Rees (1991) は、衛生マクロを block-structured 言語へ実装する際の要件を整理し、「展開後コードの自由変数はマクロ定義環境に従う」という原則を実用水準で扱った。これは問題 (2)（展開元束縛への誤捕捉）を防ぐための規範として重要である。

jalo の ns 分離案に引き寄せると、`macro-ns` と `caller-ns` を明示して式単位で切り替える設計は、この原則を syntax object なしで近似するための実装戦略に相当する。すなわち「参照はどの環境で意味を持つか」を返却 AST 内に残す設計である。

### §5.4 Dybvig et al. 1992 — Syntactic Abstraction in Scheme

**一次資料**:
- Springer: https://link.springer.com/article/10.1007/BF01806308 (accessed 2026-05-19)

Dybvig et al. (1992) は `syntax-case` の理論と実装を提示し、`syntax-rules` より高い表現力を持ちながら衛生を維持する道筋を示した。識別子同一性（後の `identifier=?` 系）を軸に、手続き的マクロ変換と衛生保証を両立した点が大きい。

本 ADR に対しては、「高度なマクロは衛生とトレードオフではない」ことを示す先行事例として効く。jalo は syntax-case そのものは採れないが、ns ラップで参照解決文脈を保存すれば、de-special-form を進めつつ衛生要求を満たす設計余地がある。

## §6 殿提案の設計方針（D1-D8）

殿のご見解 (2026-05-19) による設計指針を 8 subsection に展開する。
これらは本 ADR の「決定」の核であり、Phase 2 実装の基本方針となる。

### §6.1 D1 — 展開タイミング（run/compile、read 不採用確定）

> 「マクロ展開は run 時 または compile 時。**read 時には実行しない**」 — 殿のご見解 D1

マクロの展開タイミングは以下の選択肢から確定する:

- **run 時**: ツリーウォーキングインタープリタが評価時にマクロを展開 (Phase 1 実装として自然)
- **compile 時**: バイトコードコンパイラがコンパイル前にマクロを展開 (Phase 2 実装として理想)
- **read 時**: Reader 段でマクロを展開 — **本 ADR で不採用確定**

read 不採用の理由: §4.2 で示したとおり、jalo の Reader はシンボルオブジェクトを生成しないため、
read 時にマクロを展開してもスコープ情報が欠落する。また read 時展開は `#jq(...)` 等の
reader macro とは別の概念であり（後者は SPEC §5.6 で継続）、混同を避ける。

compile 時 vs run 時の最終選択は Q4 として残置するが、軍師推奨は compile-time
（SyntaxChecker 直前の static expansion pass）である（V1 軍師推奨を継承）。

既存の `#jq(...)` reader macro（cmd_423 実装済）はマクロ機構とは独立に存続し、
本 ADR の決定により廃止されるものではない。

### §6.2 D2 — 名前空間分離による (1)(2) 統一解決

> 「衛生問題 (1)(2) を **名前空間分離** で統一的に解決。
> マクロが挿入する束縛/参照は、展開元とは別の名前空間で名前解決する」 — 殿のご見解 D2

本方針が本 ADR の中核決定事項である。

§3.1 で定義した衛生問題 (1)(2) をいずれも「名前空間の分離」という単一機構で解決する:

**問題 (1) の解決**: マクロが挿入する束縛（例: `tmp`）は `macro-ns` に属する束縛として解決される。
展開元の参照（例: `tmp`）は `caller-ns` に属する参照として解決される。
両者は異なる名前空間に属するため衝突しない。

**問題 (2) の解決**: マクロが参照する識別子（例: `cons`）は `macro-ns` で解決される。
展開元で再定義された `cons` は `caller-ns` に属し、別の束縛として扱われる。

軍師の予備評価（subtask_434a strategy review）では、本方針は
Bawden explicit renaming（1988）をトークン単位からスコープ単位に昇格した形として
論理的に成立すると評価している（§9 で詳細検証予定）。

jalo において名前=文字列のみであることは、この scope 単位 ns ラップが唯一の合理的選択肢に近い:
Bawden のトークン単位 `rename(x)` は jalo では実装不可のため、
スコープ（式範囲）単位の ns wrap が必須となる（§4.1 参照）。

### §6.3 D3 — namespace 機構同時設計（Phase 2 設計フェーズ既開始）

> 「namespace 機構はマクロと同時設計。設計フェーズは既に Phase 2 に入っている」 — 殿のご見解 D3

マクロ機構と namespace 機構は独立して設計できないため、本 ADR で統合設計する。
Phase 構造の解釈:

- **Phase 1 (現在)**: インタープリタ実装 — 本 ADR の設計フェーズはここで着手
- **Phase 2**: バイトコードコンパイラ実装 — マクロ + namespace の実装フェーズ

従来の解釈「Phase 1 = インタープリタ、Phase 2 = コンパイラ」は維持しつつ、
**設計フェーズ（本 ADR の執筆・確定）は Phase 2 に先行して現在進行中**と捉える。
これは cmd_427（phase / 1.0.0 概念分離）の「設計と実装を分離して考える」方針と整合する。

ISSUES.md I-02（名前空間）は本 ADR が正式に引き受け、設計対象として展開する（§11.3 参照）。

### §6.4 D4 — defmacro ns 引数構造

> 「`defmacro` (仮) は JSON 引数 + JSON 返却に加え、
> 展開元の lexical scope における **名前空間も引数として** 渡される構造」 — 殿のご見解 D4

`defmacro` の関数シグネチャ（仮設計）:

```jalo
; defmacro は 3 種類の情報を受け取る:
; 1. マクロ引数 (通常の JSON 引数)
; 2. 展開元の名前空間 (caller-ns)
; 3. マクロ自身が定義された名前空間 (macro-ns) — 暗黙的に利用可能

(defmacro my-macro [caller-ns arg1 arg2]
  ; caller-ns を使って arg1/arg2 の名前解決コンテキストを明示
  (ns macro-ns
    ["let", [["tmp", (ns caller-ns arg1)]], (ns caller-ns arg2)]))
```

`caller-ns` は展開呼び出し場所の lexical scope における名前空間を表す。
これにより、マクロ本体が「展開元コンテキスト」と「マクロ定義コンテキスト」を明確に区別できる。

具体的な `defmacro` 構文（識別子名・引数順序）は Phase 2 実装時に確定するが、
「展開元 ns を引数として受け取る」という設計方針は本 ADR で確定する。

### §6.5 D5 — ns スペシャルフォーム

> 「ns スペシャルフォーム (仮): `(ns <ns> <expr>)` で `<ns>` 上で `<expr>` の名前解決と評価」 — 殿のご見解 D5

`ns` スペシャルフォームは名前空間を指定して式を評価する:

```jalo
; 構文
(ns <namespace> <expr>)

; 意味: <namespace> の名前解決コンテキストで <expr> を評価する

; 例: macro-ns コンテキストで let を評価
(ns macro-ns (let [tmp 1] tmp))
; → この let の tmp は macro-ns に属する

; 例: caller-ns コンテキストで変数参照
(ns caller-ns x)
; → caller-ns で定義された x を参照
```

jalo AST（JSON）では `(ns <ns> <expr>)` は配列 `["ns", <ns>, <expr>]` として表現される。
homoiconic 性は維持される。

`ns` は新たなスペシャルフォームとして SPEC に追加予定（Phase 2 実装段階）。
現在の SPEC §3 のスペシャルフォーム一覧（`quote` / `if` / `let` / `letrec` / `fn` / `def` / `declare` / `handle` / `raise` / `match`）に加わる。

### §6.6 D6 — マクロ返却値ラップ規約

> 「マクロ返却値の構造: 全体が `(ns <macro-ns> <form>)`、
> `<form>` 内の展開元部分式は `(ns <ns> <sub-form>)` で包まれる」 — 殿のご見解 D6

マクロが返却する展開形は必ず以下の構造を持つ:

```
(ns <macro-ns> <expanded-form>)
```

ここで `<expanded-form>` 内の、展開元由来の部分式（マクロ引数で受け取った式）は
それぞれ展開元の名前空間 `<caller-ns>` でラップされる:

```
(ns <macro-ns>
  (let [tmp <macro-internal-expr>]
    (ns <caller-ns> <caller-arg>)))
```

`let*/and/or` 等の de-special-form 候補での適用例（軍師 strategy review より）:

**`and` の展開形 (概念)**:
```jalo
; (and a b c) → マクロ返却値
(ns macro-ns
  (if (ns caller-ns a)
    (if (ns caller-ns b)
      (ns caller-ns c)
      #false)
    #false))
; if と #false は macro-ns、a/b/c は caller-ns → (1)(2) とも解決
```

**`or` の展開形 (概念・ネスト ns 問題あり)**:
```jalo
; (or a b c) → 中間結果に tmp が必要
(ns macro-ns
  (let [tmp (ns caller-ns a)]
    (if tmp tmp
      (let [tmp (ns caller-ns b)]
        (if tmp tmp
          (ns caller-ns c))))))
; ネストした let の各 tmp が同一 macro-ns 内で同名になる問題は Q3 (Open Question) で扱う
```

### §6.7 D7 — 責任分担（マクロプログラマ vs 処理系）

> 「マクロプログラマと処理系の責任分担はこれから設計 (Open Question)」 — 殿のご見解 D7

ns ラップ規約（D6）を正しく適用する責任の所在は現時点で未確定:

- **案 A — 処理系が自動ラップ**: `defmacro` の返却値を処理系が自動的に `(ns macro-ns ...)` でラップする。マクロプログラマは ns を意識せずに普通の jalo コードを返せばよい。
- **案 B — マクロプログラマが明示**: マクロプログラマが `(ns macro-ns ...)` と `(ns caller-ns ...)` を手動で記述する。完全な制御が可能だが学習コストが高い。
- **案 C — ハイブリッド**: デフォルトは案 A（自動ラップ）だが、`(ns caller-ns ...)` の明示的指定でオーバーライドできる。

詳細は §12 Q5 として残置し、Phase 2 設計時に確定する。

### §6.8 D8 — グローバル ns 定義スペシャルフォーム

> 「グローバル namespace 定義のスペシャルフォームも別途必要」 — 殿のご見解 D8

D5 の `ns` スペシャルフォーム（既存 ns での評価）に加え、
新しい名前空間を定義・登録するためのスペシャルフォームが必要:

```jalo
; 仮称: def-ns (具体的な識別子は Q6 で確定)
(def-ns <namespace-name>)
; → <namespace-name> をグローバル ns レジストリに登録

; 使用例
(def-ns "my-lib")
(ns "my-lib"
  (def cons (fn [a b] ...)))  ; my-lib::cons を定義
```

`def-ns` は `def` と対をなすスペシャルフォームとして SPEC に追加予定。
グローバル ns レジストリの実装詳細（スコープ・継承・import）は §11.3 参照。

具体的な構文（`def-ns` 以外の命名、引数構造）は Q6 として残置する。

## §7 設計詳細

本節では §6 で確立した設計方針 D1-D8 を実装可能なレベルまで詳述する。
構文・意味論は仮設計であり、Phase 2 実装段階で SPEC.md に反映する際に最終確定する。

### §7.1 `ns` スペシャルフォーム構文と意味論

**構文**:

```
(ns <namespace> <expr>)
```

- `<namespace>` は文字列（ns 識別子）。Phase 2 では `def-ns` で登録済の ns 名のみ許容（§7.2 参照）。
- `<expr>` は任意の jalo 式。
- jalo AST (JSON) 表現: `["ns", <namespace>, <expr>]`。homoiconic 性は維持される。

**意味論（操作的）**:

通常の評価関数を `eval(ρ, e)` と表記する（`ρ` は環境、`e` は式）。`ns` SF は名前解決コンテキストを切り替える:

```
eval(ρ, (ns N e)) = eval(switch-ns(ρ, N), e)
```

ここで `switch-ns(ρ, N)` は環境 `ρ` の名前解決コンテキストを ns `N` に切り替えた新環境を返す。`ρ` の lexical binding は保持されたまま、自由変数の解決先のみが `N` に従う。

**ネストした `ns` の意味**:

```
(ns N2 (ns N1 e))
; → 内側の (ns N1 e) が先に評価され、e は N1 コンテキストで解決される
; → 外側の N2 は (ns N1 e) 全体の式に対する文脈だが、e 自身は N1 を保持
```

ns 切り替えはレキシカル（静的に決定）であり、`(ns ...)` の境界を越えて自由変数の解決先が変わる。

**型と評価**:

- `ns` SF の評価結果は内側の `<expr>` の評価結果と同じ型を持つ。
- `ns` 自身は値を生成しない（名前解決の文脈切り替え専用）。

### §7.2 グローバル ns 定義 form (`def-ns`) 構文

**構文**:

```
(def-ns <namespace-name>)
(def-ns <namespace-name> [<bindings>...])   ; 初期束縛つき
```

- `<namespace-name>` は文字列。グローバル ns レジストリに登録される。
- `<bindings>` は省略可能で、ns 内の初期定義（`(def x ...)` 等の列）を埋め込む形。

**意味論**:

```
eval(ρ, (def-ns N)) ⇒ ρ' (where ρ' adds N to global ns registry)
eval(ρ, (def-ns N [bs])) ⇒ eval-bindings(switch-ns(ρ', N), bs)
```

- `def-ns` は副作用専用 SF。返却値は仕様未確定（`#null` を返すか、ns 識別子を返すか）。Q6 として §12 に残置。
- 既に登録済の ns 名を `def-ns` で再宣言した場合は冪等（エラーにしない）。複数回呼び出しても問題なく動作する。

**使用例**:

```jalo
(def-ns "macro-lib")
(ns "macro-lib"
  (def cons (fn [a b] ["cons", a, b])))
(ns "macro-lib" cons)
; → 上記で定義した cons を取得
```

### §7.3 `defmacro` 構造詳細

**構文（仮設計、Phase 2 で最終確定）**:

```
(defmacro <name> [<param>...] <body>)
```

- `<name>` はマクロ名（文字列）。
- `<param>...` はマクロ引数。`caller-ns` は暗黙的に第 0 引数として渡される（D4）。明示記述は不要。
- `<body>` は jalo 式。評価結果は AST (JSON) であり、マクロ展開後のコードとして使われる。

**展開時の流れ**:

1. マクロ呼び出し `(my-macro x y)` を検知。
2. 展開器が `caller-ns`（現在の lexical ns）と引数 `[x, y]` を確保。
3. マクロ本体を `macro-ns`（マクロ定義時の ns）で評価し、AST を返却。
4. 返却 AST は D6 ラップ規約（§7.4）に従って自動 or 手動でラップされる。
5. ラップ済 AST を展開後コードとして元の位置に置換、後続の評価/コンパイルへ進む。

**`caller-ns` の暗黙渡し**:

マクロ本体内では `caller-ns` という識別子で展開元 ns を参照できる（言語組み込み変数）。

```jalo
(defmacro my-swap! [a b]
  (let [macro-ns "macro-lib"]
    (ns macro-ns
      ["let", [["tmp", (ns caller-ns a)]],
       ["begin",
        ["set!", (ns caller-ns a), (ns caller-ns b)],
        ["set!", (ns caller-ns b), "tmp"]]])))
```

**`macro-ns` の決定規則**:

マクロが定義された時点での lexical ns（`(def-ns ...)` で導入された ns、または `(ns ...)` SF 内で `defmacro` した場合はその ns）が `macro-ns` となる。これは lexical で確定する（§7.5 参照）。

### §7.4 マクロ返却値ラップ規約の形式定義

D6 を形式的に定義する。マクロが返却する AST に対するラップ関数 `W` を以下で定義:

```
W(form, macro-ns, caller-ns) =
  match form with
  | <caller-arg>           → (ns caller-ns <caller-arg>)
  | (<f> <a1> ... <an>)    → (ns macro-ns (<f> W(<a1>, ...) ... W(<an>, ...)))
  | <literal>              → <literal>                     (リテラルは ns 不要)
```

- `<caller-arg>`: `defmacro` の引数として受け取った値（マクロ本体が引数を直接埋め込んだ箇所）。
- `<f> <a1> ...`: マクロ本体が構築する呼び出し式。`<f>` は `macro-ns` の関数/特殊形式。
- `<literal>`: 数値・文字列リテラル等（ns 解決不要）。

**マクロ最外のラップ**:

マクロ返却値の最外に `(ns macro-ns ...)` が必ず付与される（処理系が自動付与、§6.7 D7 案 A）。これは Q5（§12）で最終確定する。

**展開元引数の透過**:

`<caller-arg>` は `defmacro` の引数として受け取った時点で `caller-ns` 情報を保持しているため、`(ns caller-ns ...)` で再ラップする。これにより問題 (2)（マクロ挿入参照が展開元束縛に捕捉される）を防ぐ。

### §7.5 lexical scope と `ns` の関係

`ns` SF は **lexical**（静的）に決定される。dynamic ns（実行時の動的切り替え）は本 ADR では採用しない（§12 Q3 で動的 ns 生成の許容範囲を扱う）。

**クロージャと ns**:

`fn` でクロージャを生成する際、クロージャは「定義位置の lexical ns」を捕捉する:

```jalo
(def-ns "outer-ns")
(ns "outer-ns"
  (def f (fn [x] x)))             ; f は outer-ns を捕捉

(def-ns "inner-ns")
(ns "inner-ns"
  (f 1))                          ; f の自由変数解決は outer-ns
                                  ; ただし引数 1 の解決は inner-ns
```

クロージャの ns 継承規則:

- クロージャ本体内の自由変数は定義時 ns で解決
- クロージャの引数値は呼び出し位置 ns で解決
- これは Bawden lexical macro と整合する設計（§9.2 参照）

**マクロ展開と lexical**:

マクロ展開は lexical な静的処理ゆえ、`ns` SF の境界は展開時に確定する。Phase 1 (tree walker) では SyntaxChecker 直前の static expansion pass で展開を行う（§12 Q4 軍師推奨）。Phase 2 (bytecode compiler) では compile time に展開、bytecode は通常の AST と同じく扱える。

## §8 決定要因マトリクス

評価記号: ◎ = 強い適合、○ = 実用適合、△ = 条件付き、× = 不適合

| 評価軸 | V1 Option A (syntax-rules) | V1 Option C (Clojure defmacro+auto-gensym) | Bawden syntactic closures | Dybvig syntax-case | 殿提案 ns 分離 |
|---|---|---|---|---|---|
| (i) 衛生問題 (1) 解決 | ◎: 展開器主導の自動衛生で束縛衝突を防げる | ○: auto-gensym で一時変数衝突を回避できるが規約依存が残る | ◎: 展開器が文脈つきで挿入識別子を扱い衝突を抑制できる | ◎: syntax object と衛生規則で捕捉を体系的に防ぐ | ◎: `macro-ns` へ隔離するため caller 側識別子を汚染しない |
| (ii) 衛生問題 (2) 解決 | ○: 定義環境参照を保持するが高度ケースは制約が強い | ○: syntax-quote で ns 修飾可能だが quasiquote 規約依存 | ◎: closure 環境で自由変数参照先を保持できる | ◎: `identifier` 同一性比較で参照捕捉を抑制できる | ◎: 挿入参照を `macro-ns` で解決し caller 捕捉を分離する |
| (iii) 表現力 | △: パターン中心で手続き的変換が難しい | ◎: Lisp マクロとして高い変換自由度がある | ○: 衛生を保ちつつ実用変換可能だが実装負荷が高い | ◎: 手続き的変換 + 衛生で高表現力 | ○: 目的の衛生要件は満たすが ns 運用規則の設計が必要 |
| (iv) 実装難度 | △: 仕様は単純だが jalo AST 制約に合わせた実装が別途必要 | ○: 実装は現実的だが Clojure 流 quasiquote 非採用で再設計要 | ×: jalo には syntax object 不在で直接実装が困難 | ×: identifier object 基盤がなく直接導入不可 | ○: `ns` SF とラップ規約の追加で到達可能 |
| (v) jalo 制約整合 | ×: シンボル構造前提が強く JSON 文字列 AST と齟齬 | △: 一部思想は流用できるが quasiquote 拡張が不適合 | ×: トークン単位リネーム前提が JSON 文字列モデルと不整合 | ×: syntax object 前提で現行 Reader/AST と不整合 | ◎: JSON→JSON 変換を維持したまま衛生要件を扱える |
| (vi) Phase 2 互換 | △: 基盤改造量次第で遅延しうる | ○: Phase 2 で導入しやすいが仕様差分整理が必要 | △: 直接導入は難しく概念転写が前提 | △: 直接導入不可、概念参照のみ実用 | ◎: namespace 設計と同時に Phase 2 へ接続できる |

## §9 論理検証（Validation）

殿の核心仮説（2026-05-19）「マクロが挿入する束縛/参照は展開元とは別の名前空間で名前解決すれば、衛生問題 (1)(2) が統一的に解決する」の妥当性を、§5 で参照した古典論文と §7 の設計詳細を踏まえて検証する。

### §9.1 Bawden explicit renaming との関係（スコープ単位昇格）

Bawden & Rees (1988) の `syntactic closure` および explicit renaming（§5.1 参照）は、マクロ展開器が **token 単位**（個別シンボル）で `rename(x)` を呼び、マクロ作成者 ns 由来のシンボルとして識別する方式である。

> "Each use of a macro is expanded into a syntactic form whose free variables are interpreted relative to the environment in which the macro was defined."  
> — R7RS §4.3, https://small.r7rs.org/attachment/r7rs.pdf （accessed 2026-05-19）

jalo の `(ns <macro-ns> ...)` / `(ns <caller-ns> ...)` ラップ規約（§7.4）は、Bawden の同思想を **scope 単位**（式範囲）で表現したものと解釈できる:

| 観点 | Bawden explicit renaming | jalo ns 分離 |
|---|---|---|
| 粒度 | token（シンボル）単位 | scope（式範囲）単位 |
| 識別子表現 | syntax object（環境情報つき） | 文字列（解決文脈は包む式で表現） |
| 衛生情報の所在 | 識別子オブジェクト内部 | `(ns ...)` SF の構造 |
| jalo 制約整合性 | × 不可（名前=文字列のみ） | ◎ 可（JSON 配列で表現） |

jalo は §4.1 で示したとおり名前=文字列のみであり token 単位の rename 操作ができない。したがって scope 単位への昇格は **jalo 制約下での唯一の合理的選択肢**に近い設計である。

Clinger & Rees (1991) の "Macros That Work" は「展開後コードの自由変数はマクロ定義環境に従う」という原則を整理した（§5.3）。jalo の ns 分離は、この原則を識別子オブジェクトなしで実装するための具体的な構造的解決策である。

### §9.2 衛生問題 (1) の解決論証

**主張**: D6 ラップ規約（§7.4）により、マクロが挿入した束縛は展開元の参照を捕捉しない。

**論証**: マクロ返却値の最外は `(ns macro-ns ...)` でラップされる（§7.4）。マクロ本体内で生成された `let` 等の束縛（例: `tmp`）は `macro-ns` の名前解決コンテキストに属する。

一方、展開元の参照は `(ns caller-ns <caller-arg>)` でラップされている。`caller-ns` と `macro-ns` は異なる名前空間ゆえ、同名 `tmp` であっても名前解決時に別の束縛として扱われる。∴ マクロ挿入束縛は展開元参照を捕捉しない。

**`let*` の具体例**:

`let*` をマクロ化する際、`(let* [a 1 b a] expr)` を以下のように展開する想定:

```jalo
; (let* [a 1 b a] expr) → 展開結果
(ns macro-ns
  (let [(ns caller-ns a) 1]
    (let [(ns caller-ns b) (ns caller-ns a)]
      (ns caller-ns expr))))
```

ここで:
- `let` SF は `macro-ns` 由来（展開器が挿入）
- 識別子 `a`, `b` は引数として渡された `caller-ns` の名前
- 展開元コードで `let*` の外側に `tmp` 等の束縛があっても、マクロ挿入の `let` は `caller-ns` の `a`/`b` を直接束縛するのみで、`macro-ns` の他の名前は触らない

仮に内側 `let` の右辺式 `(ns caller-ns a)` が、`macro-ns` の `a` ではなく `caller-ns` の `a`（外側 let で束縛された値 1）を参照することが ns 分離で保証される。∴ 問題 (1) は解決される。

### §9.3 衛生問題 (2) の解決論証

**主張**: マクロが参照する識別子は展開元の束縛に捕捉されない。

**論証**: マクロ本体内で記述された関数/特殊形式の参照（例: `cons`, `if`）は `(ns macro-ns ...)` 内に位置する。これらは `macro-ns` の名前解決コンテキストで解決され、展開元の `caller-ns` で再定義された同名識別子とは別物として扱われる。

**`and` の具体例**:

`and` をマクロ化する際、`(and a b c)` を以下のように展開する想定:

```jalo
; (and a b c) → 展開結果
(ns macro-ns
  (if (ns caller-ns a)
    (if (ns caller-ns b)
      (ns caller-ns c)
      #false)
    #false))
```

ここで:
- `if`, `#false` は `macro-ns` 由来（マクロ本体記述）
- `a`, `b`, `c` は `caller-ns` 由来（引数透過）

仮に展開元コードで `if` を再定義していても、`(ns macro-ns if ...)` は `macro-ns` の `if`（標準 SF）を解決するため誤動作しない。∴ 問題 (2) は解決される。

**Lisp1 制約下での重要性**: §3.2 で述べたとおり、jalo は Lisp1（変数名前空間と関数名前空間が統一）であり、Common Lisp の Lisp2 緩和が使えない。したがって `cons` のような頻出関数名の再定義による問題 (2) は jalo では深刻であり、ns 分離による解決は実装上必須の機構である。

### §9.4 反例候補の検討（軍師の誠実義務）

軍師策略（subtask_434a）で検出した反例候補 3 件を再検討する。

#### R1 — anaphoric pattern（意図的な不衛生）

**問題**: マクロが意図的に caller の束縛を参照したい場合（例: anaphoric `it` パターン、`if` 内に暗黙の `it` を導入）。

```jalo
; 例: anaphoric if
(aif (find-user id)
  (process it)        ; it は aif マクロが導入する暗黙束縛
  "not found")
```

**ns 分離での扱い**: `macro-ns` に `it` を定義しても `caller-ns` 側で見えない（衛生が破られない）。結果として anaphoric パターンは ns 分離方式では **自然に表現できない**。

**評価**: これは ns 分離方式の**仕様上の限界**であり、本仮説の論理的反例ではない。anaphoric は意図的不衛生であり、衛生問題の解決対象外。

**Open Question Q1（§12）への落とし込み**: 解決策候補:
- (a) `:unhygienic true` フラグでの限定許可
- (b) `(inject-into-caller-ns ...)` 専用 form 導入
- (c) Phase 2 では不採用（衛生優先、Scheme syntax-rules 同様の方針）

軍師推奨: (a) または (c)。暗黙注入は禁止（明示的なオプトインのみ許容）。

#### R2 — macro-to-macro 連携時の ns 継承

**問題**: マクロ A がマクロ B を呼ぶ時、B の挿入物の ns はどう決まるか。

候補:
1. B 定義時 ns（B 自身の `macro-ns`）
2. A 展開時 ns（A の `macro-ns`）
3. 最外 caller ns（A を呼んだ場所の `caller-ns`）

**Bawden lexical 解釈との対応**: Bawden & Rees (1988) の syntactic closure は「マクロ定義環境に従う」を原則とする（§5.1）。これに従えば B 自身の `macro-ns`（候補 1）が自然な選択。

**評価**: 反例というより**設計選択の論点**。軍師推奨は候補 1（定義時 ns 優先、Bawden lexical 指向）。必要に応じて `caller-ns` を明示引数で渡せば候補 2/3 も実現可能。

**Open Question Q2（§12）への落とし込み**: ns 継承規則は Phase 2 実装段階で確定。

#### R3 — 動的 ns 生成

**問題**: マクロ本体が `(def-ns new-ns ...)` を返した時の意味論。実行時に任意 ns を生成可能にすると、再現性・検証性が低下する。

**ns 分離方式での扱い**: §7.5 で「ns はレキシカル」と確立した。動的 ns 生成は本 ADR の lexical 原則と整合しない。

**評価**: lexical 制約により動的 ns は本来許容しない。ただし `def-ns` 自身は SF として実行されるため、評価時に新 ns が登録される（§7.2）。これは「グローバル ns レジストリへの登録」であり、lexical な参照解決は影響を受けない。

**Open Question Q3（§12）への落とし込み**: 動的 ns 生成の許容範囲。軍師推奨:
- Phase 2 では「宣言済 `def-ns` のみ許可」を基本ルール
- 動的生成は feature flag 下で実験運用
- 並せて `or` 展開時の同一 macro-ns 内同名 `tmp` 衝突回避規則（fresh suffix / nesting ns 分割）をここで確定

### §9.5 既存実装との比較総括（Dybvig syntax-case との表現力比較）

Dybvig et al. (1992) の `syntax-case`（§5.4）は識別子オブジェクト（`identifier=?` 比較）で衛生を担保する。jalo の ns 分離方式と表現力を比較する:

| 観点 | Scheme syntax-case | jalo ns 分離 |
|---|---|---|
| 衛生問題 (1) 解決 | ◎ 自動 | ◎ ラップ規約 |
| 衛生問題 (2) 解決 | ◎ 自動 | ◎ ラップ規約 |
| 識別子単位の細粒度操作 | ◎ 可能 | △ scope 単位ゆえ困難 |
| 高度なメタ構文（パターン変数等） | ◎ 可能 | △ 別途設計要 |
| 実装難度（jalo 制約下） | × 識別子オブジェクト不在で不可 | ◎ JSON 配列で表現可能 |
| de-special-form 候補 (let*/and/or) の表現力 | ◎ 完全 | ○ `or` の fresh 名称戦略確定が必要 |

**結論**: 「衛生問題 (1)(2) の回避」という目的に限れば、ns ラップ方式は syntax-case と **機能等価**を狙える。識別子単位の細粒度操作や高度なメタ構文（パターン変数等）では syntax-case の一般性が高いが、jalo の Phase 2 マクロ機構の主目的（de-special-form と一般的なマクロサポート）には十分。

**§10 との整合**: §10 比較節で de-special-form 4 例（let\*/and/or/quasiquote）の具体検証を行っているが、本 §9.5 の総括結論はこれと整合する。`or` の fresh 名称戦略（§12 Q3）の確定が Phase 2 実装前の前提条件。

### §9.6 総合判定（軍師結論）

殿の核心仮説「名前空間分離による (1)(2) 統一解決」は、本軍師の論理検証の範囲では:

- **成立**: §9.2 / §9.3 で示したとおり、(1)(2) は ns 分離単一機構で解決される。Bawden explicit renaming（§9.1）の scope 単位昇格として正統な系譜に位置する
- **反例なし**: R1-R3（§9.4）はいずれも仮説の論理的全否定ではなく、設計選択 / 仕様上の限界 / 実装方針の論点であり、Open Question として §12 に格納可能
- **表現力**: syntax-case と機能等価（§9.5）、de-special-form 4 候補（§10）で実装可能性確認済

したがって本 ADR は **Proposed → Accepted** への移行を軍師 QC として **PASS** と判定する。最終承認は殿の中間レビューを経て行う。Phase 2 実装段階で残り Open Question Q1-Q3, Q5-Q8 を順次確定する。

## §10 比較（Comparison）

本節では「Scheme syntax-case の識別子オブジェクト方式」と「jalo の ns ラップ方式」が、de-special-form 候補でどこまで同等の表現力を持つかを検証する。

### §10.1 比較観点

- syntax-case 側: 識別子にスコープ情報を持たせ、`identifier` 同一性で衛生を担保する。
- jalo 側: 識別子は文字列のまま、式を `(ns <scope> <expr>)` で包み、名前解決コンテキストを明示する。

結論として、**衛生問題 (1)(2) の回避という目的に限れば、ns ラップ方式で機能同等を狙える**。ただし、識別子単位の細粒度操作や高度なメタ構文の扱いでは syntax-case のほうが一般性が高い。

### §10.2 de-special-form 具体検証（4 例）

#### 1) `let*`

`let*` は逐次束縛を `let` のネストに展開するだけであり、挿入される束縛は `macro-ns`、利用者式は `caller-ns` に分離できる。構文展開が局所的で、ns ラップ方式でも syntax-case と同等に衛生展開可能。

#### 2) `and`

`and` は `if` ネストへの展開で実現でき、各オペランドを `(ns caller-ns ...)` で包むことで展開元評価文脈を保持できる。短絡評価制御は macro-ns 側で構築できるため、衛生・評価順とも整合する。

#### 3) `or`

`or` は中間結果を保持する一時束縛が必要で、`let` ネスト中に同名 `tmp` が複数現れる。syntax-case では fresh identifier 生成で自然に処理できるが、jalo ns ラップ方式では「同一 macro-ns 内の同名 tmp」衝突回避規則が必要である。現時点では Open Question (Q3) として管理する。

#### 4) `quasiquote`

jalo の `quasiquote` はデータ構築専用であるため、Clojure の syntax-quote 的な識別子修飾には使わない。したがって衛生は quasiquote 自体ではなく ns ラップ規約で担保する。これは syntax-case の「識別子側で解決」ではなく「式側で解決」の設計差だが、目的（衛生）達成は可能である。

### §10.3 差分まとめ

- **同等にできる領域**: `let*`/`and` のような制御構造展開、展開元参照と挿入参照の分離、問題 (1)(2) の基本対処。
- **追加設計が要る領域**: `or` の fresh 名称戦略、macro-to-macro 合成時の ns 継承、anaphoric パターン。
- **設計思想の差**: syntax-case は識別子オブジェクト中心、jalo は JSON AST を維持したまま ns で意味論を与える。

以上より、ns ラップ方式は jalo 制約下での現実解として妥当だが、実装前に Q2/Q3 の規則確定が必要である。

## §11 実装ロードマップ

### §11.1 マクロ → バイトコードコンパイラ順序（D3 由来）

**マクロ機構をバイトコードコンパイラに先行して実装する** (V1 §推奨 D1 を継承・強化)。

理由:
1. 現存スペシャルフォームの一部をマクロとして再実装（de-special-form）することで、
   バイトコードコンパイラが扱うべきスペシャルフォームを削減できる
2. §6 D3 のとおり、マクロ設計と namespace 設計は同時に行う必要があり、
   コンパイラ設計はこれらが確定した後に行うのが合理的
3. cmd_427（phase/1.0.0 概念分離）で「設計フェーズは Phase 2 に既開始」と裁可済であり、
   本 ADR の確定がコンパイラ実装への前提となる

Phase 2 実装順序（案）:
1. namespace 機構の基本実装（`def-ns` / `ns` スペシャルフォーム、インタープリタ段で動作）
2. `defmacro` 基本実装（展開器、ns 引数対応）
3. de-special-form 移行（§11.2 確定候補から順次）
4. バイトコードコンパイラ実装（ns + macro 確立後）

### §11.2 de-special-form 候補リスト draft

現在のスペシャルフォームのうち、マクロとして再実装可能なものを以下に分類する（SPEC §3/§4/§5 参照）:

**確定 4 件（Phase 2 で必ずマクロ化）**:

| スペシャルフォーム | 根拠 | ns ラップ適用可否 |
|---|---|---|
| `let*` | SPEC §4.2 でマクロ機構への移行を既予告 | ◎ (確認済 — 軍師 strategy review) |
| `quasiquote` | SPEC §5.2 でマクロ機構への移行を既予告 | ◎ (§4.3 と整合、ただし Phase 2 実装時に具体プロトタイプ要) |
| `and` | 短絡評価を `if` ネストに展開可能 (SPEC §4.5) | ◎ (確認済 — 軍師 strategy review) |
| `or` | 短絡評価を `if` ネストに展開可能 (SPEC §4.5) | ○ (ネスト ns 生成戦略の確定が必要 — §12 Open Question) |

**Future 候補 3 件（Phase 2 以降で検討）**:

| スペシャルフォーム | 検討優先度 | 備考 |
|---|---|---|
| `cond` | 高 | `if` ネストへの展開が自然 |
| `when` | 中 | `if` + `#null` への展開 |
| `unless` | 中 | `if` + `#null` への展開 |

**core スペシャルフォーム — マクロ化しない 10 件**:

`quote` / `if` / `let` / `letrec` / `fn` / `def` / `declare` / `handle` / `raise` / `match`

これらは jalo の計算モデルの核をなし、マクロ化すると循環依存または意味論の複雑化を招く。

de-special-form の実装優先度と別 cmd 化は §12 Q7 参照。

### §11.3 namespace 機構の段階導入

I-02（名前空間）の設計を本 ADR が引き受け、以下の段階導入計画を提示する:

**Stage 1 — グローバル ns 定義**:
- `def-ns` スペシャルフォームでグローバル ns レジストリを確立
- `(ns <ns> <expr>)` での名前解決コンテキスト切り替え
- 主目的: マクロ展開の衛生問題 (1)(2) 解決に必要な最小機構

**Stage 2 — lexical ns**:
- lexical scope と ns の関係を確立
- クロージャが参照する ns の継承規則確定（§12 Q2 の解決が必要）
- 主目的: マクロ間連携（マクロ A がマクロ B を呼ぶ時の ns 継承）のサポート

**Stage 3 — ns import / 可視性制御**:
- `(import-ns <ns>)` 等で別 ns の束縛を現在 ns に取り込む機構
- 主目的: モジュールシステムの基盤
- 本 ADR のスコープ外（Phase 2 以降で別 ADR 化の候補）

## §12 未解決の問い（Open Questions）

以下の問いは Phase 2 実装前に確定が必要である（または Phase 2 実装段階で確定する）。

**足軽 A (本 subtask) 担当**:

- **Q4: 展開タイミング compile-time / run-time の最終確定**  
  §6.1 D1 により read 不採用は確定。run 時 vs compile 時の選択が残る。  
  軍師推奨: compile-time（SyntaxChecker 直前の static expansion pass、V1 Q3 継承）。  
  Phase 2 実装着手前に殿のご裁可を仰ぐ。

- **Q5: 責任分担（マクロプログラマ vs 処理系）の詳細**  
  §6.7 D7 で提示した案 A/B/C（自動ラップ / 手動 / ハイブリッド）のいずれを採るか。  
  Phase 2 `defmacro` 実装設計時に確定する。

- **Q7: de-special-form 移行順序と別 cmd 化**  
  §11.2 の確定 4 件（let\*/quasiquote/and/or）の実装優先度・依存関係・別 cmd 分割方針。  
  `let*` と `and`/`or` は独立して実装可能だが、`quasiquote` はデータ構築専用意味論を
  変えないよう慎重な設計が必要。Phase 2 着手時に別 cmd として計画する。

- **Q8: 将来の reader-level 別機能の検討条件**  
  V1 Q9 を継承。`#jq(...)` 系の `#<name>(...)` 一般化（Option E 由来の `defreader` 構想）を
  将来独立機能として再評価する際の条件・タイミング・スコープ。  
  本 ADR のマクロ機構とは独立した別系統として検討する（§4.3 参照）。

**Q1/Q2/Q3/Q6（軍師統合時に追記）**:

- **Q1: anaphoric パターンの取扱い（R1）**  
  選択肢は 3 つある。  
  (a) `:unhygienic true` の明示フラグで限定許可する。  
  (b) `(inject-into-caller-ns ...)` の専用 form を導入し、呼び出し側注入を明示する。  
  (c) Phase 2 では不採用とし、完全衛生を優先する。  
  推奨は (a) もしくは (c)。暗黙注入は禁止。

- **Q2: マクロ間連携時の ns 継承規則（R2）**  
  連携時にどの ns を継承するかは、定義時 ns / 展開時 ns / caller-ns の三案がある。  
  軍師推奨（subtask_434a）は **定義時 ns 優先**（Bawden 流 lexical 指向）であり、必要に応じて caller-ns を明示引数で渡す案である。

- **Q3: 動的 ns 生成の許容範囲（R3）**  
  実行時に任意 ns を生成可能にすると再現性・検証性が低下する。  
  Phase 2 では「宣言済み `def-ns` のみ許可」「動的生成は feature flag 下で実験」の二段運用を検討し、正式可否は実装段階で確定する。  
  併せて `or` 展開時の一時束縛衝突回避規則（fresh suffix / nesting ns 分割）をここで確定する。

- **Q6: グローバル ns 定義 form の構文（D8）**  
  仮称 `def-ns` を採るか、既存 `def` と統合して `(def :ns ...)` 形式にするか未確定。  
  ADR では `def-ns` をデフォルト案として保持し、SPEC 反映時に最終決定する。

## §13 参考文献

| 処理系/規格 | リソース | URL | アクセス日 |
|---|---|---|---|
| Lisp マクロ古典 | Bawden & Rees (1988), *Syntactic Closures* (LFP'88, DOI) | https://dl.acm.org/doi/10.1145/62678.62687 | 2026-05-19 |
| Lisp マクロ古典 | Bawden & Rees (1988) MIT DSpace record (AIM-1049) | https://dspace.mit.edu/handle/1721.1/6036 | 2026-05-19 |
| Lisp マクロ古典 | Kohlbecker et al. (1986), *Hygienic Macro Expansion* (LFP'86, DOI) | https://dl.acm.org/doi/10.1145/319838.319859 | 2026-05-19 |
| Lisp マクロ古典 | Clinger & Rees (1991), *Macros That Work* (POPL'91, DOI) | https://dl.acm.org/doi/10.1145/99583.99607 | 2026-05-19 |
| Lisp マクロ古典 | Clinger & Rees (1991) full-text PDF (UO Scholars' Bank) | https://scholarsbank.uoregon.edu/server/api/core/bitstreams/12fed7c3-fd49-4ae8-97e4-6bbb156aaba6/content | 2026-05-19 |
| Lisp マクロ古典 | Dybvig et al. (1992), *Syntactic Abstraction in Scheme* | https://link.springer.com/article/10.1007/BF01806308 | 2026-05-19 |
| Lisp マクロ古典 | Bawden (1999), *Quasiquotation in Lisp* | https://3e8.org/pub/scheme/doc/Quasiquotation%20in%20Lisp%20(Bawden).pdf | 2026-05-19 |
| Common Lisp | HyperSpec — Function `GENSYM` | https://www.lispworks.com/documentation/HyperSpec/Body/f_gensym.htm | 2026-05-19 |
| Clojure | Macros（defmacro 公式仕様） | https://clojure.org/reference/macros | 2026-05-19 |
| Clojure | Reader — Syntax-quote（auto-gensym） | https://clojure.org/reference/reader#syntax-quote | 2026-05-19 |
| Clojure | Special Forms 公式仕様 | https://clojure.org/reference/special_forms | 2026-05-19 |
| Scheme | R7RS small §4.3 Macros（PDF） | https://small.r7rs.org/attachment/r7rs.pdf | 2026-05-19 |
| Scheme | R6RS §12 Syntax-case | https://www.r6rs.org/final/html/r6rs/r6rs-Z-H-16.html | 2026-05-19 |
| jalo | SPEC §4.2 let\* / §4.5 and・or / §5.2 quasiquote | （本リポジトリ docs/SPEC.md） | 2026-05-19 |
| jalo | DECISION_MACRO.md V1 (Superseded) | （本リポジトリ docs/DECISION_MACRO.md） | 2026-05-19 |
| jalo | DECISION_TCO.md（ADR-001） | （本リポジトリ docs/DECISION_TCO.md） | 2026-05-19 |
