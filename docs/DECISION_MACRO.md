# ADR-002: jalo マクロ機構設計

## ステータス

Draft / 殿の中間レビュー待ち — Q1-Q6 参照。Phase 2 以降での実装を推奨。当面は `quasiquote` + reader macro `#jq(...)` で代用する（下記「推奨」節を参照）。

## 背景と問題の定義

jalo は JVM（Java 21）上で動作する純粋関数型の Lisp 方言であり、SPEC §5.2 において「`(quasiquote <pattern>)` は第 1 版ではスペシャルフォームとして実装される（第 2 版以降でマクロ機構へ移行する予定）」と明示している。本 ADR はこの「第 2 版マクロ機構」の具体的設計を確定するための決定記録である。

現在のプロジェクト状態は ISSUES.md I-14 に記録されている:

> **I-14**: マクロ機構未設計 — 状態: 📋 設計未確定（殿レビュー待ち）。詳細は `docs/DECISION_MACRO.md` 参照。

マクロ機構がない現状、以下の制約がある:

- DSL 構築（ドメイン固有言語）が不可能。新たな構文的抽象化を追加するには jalo コアを変更する必要がある。
- コード生成パターンが表現力に欠ける。
- VISION.md §6「マクロシステム（1.0 以降）」で明示されたロードマップ項目が未着手。
- DECISION_TCO.md Q6 でも「将来のマクロシステムは TCO 戦略に制約を課すか」が Open Question として残されている。

Clojure の設計哲学として、公式ドキュメントは次のように述べている:

> "Clojure has a programmatic macro system which allows the compiler to be extended by user code."  
> — Clojure official documentation, "Macros",  
> https://clojure.org/reference/macros (accessed 2026-05-12)

この記録は、jalo のアーキテクチャの文脈でマクロ機構への 5 つのアプローチを評価し、プロジェクトオーナーのレビューに向けた推奨を提供する。

## 決定の要因

1. **hygiene 保証（衛生性の強度）**【最高優先度】— マクロが展開後の識別子を不意に捕捉しないか。衛生マクロは識別子の暗黙的リネームで変数捕捉を防ぐ。
2. **JVM 実装難度（jalo 実装コスト）** — 現在のツリーウォーキングインタープリタへの追加実装コスト。深いバイトコード変換を必要とするオプションは Phase 1 では過剰である。
3. **Phase 2 バイトコード親和性** — 選択した戦略は計画中の JVM バイトコードコンパイラ（DESIGN.md §1）をブロックしてはならない。
4. **代数的エフェクト直交性** — jalo の `raise`/`handle` 機構（SPEC §4.4）との相互作用。マクロが effect handler frame を跨ぐケースを考慮する必要がある。
5. **TCO 設計との相互作用（DECISION_TCO.md 参照）** — macro が生成するコードの末尾位置が保持されるか。特に CPS 変換型マクロ（Option D）は TCO 設計に制約を課す可能性がある。
6. **jq 互換層（`#jq`）との親和性** — 既存の reader macro `#jq(...)` との統合。既存機構の継続性と新機構の導入コストのバランス。

## 検討オプション

- **Option A**: Scheme syntax-rules スタイル（パターンベース衛生マクロ）
- **Option B**: Scheme syntax-case スタイル（手続き的衛生マクロ）
- **Option C**: Clojure defmacro（auto-gensym + syntax-quote）
- **Option D**: Racket スタイル（phase-level 分離 + syntax-parse）
- **Option E**: Reader macro 拡張中心（`#xxx(...)` 一般化、defmacro は最小）

## 決定要因マトリクス

各セルは一行の根拠と記号を示す。◎ 最良 / ○ 良好 / △ 制約あり / × 不利

| オプション | (i) hygiene 保証 | (ii) JVM 実装難度 | (iii) Phase 2 親和 | (iv) effect 直交 | (v) TCO 相互作用 | (vi) jq 親和 |
|---|---|---|---|---|---|---|
| A: syntax-rules | ◎ 完全衛生（識別子自動 rename） | ○ 新規モジュール 1-2 個、中程度 | ◎ 展開後 AST を通常コンパイル | ◎ 構文変換のみ、相互作用なし | ◎ 展開後コードに対して TCO 適用可 | △ 既存 #jq と別枠組み |
| B: syntax-case | ◎ 完全衛生（syntax object 操作） | △ syntax object 設計が高コスト | ○ 軽微な制約あり | ◎ 構文変換のみ | ◎ 同上 | △ 既存 #jq と別枠組み |
| C: Clojure defmacro | △ auto-gensym + ns 限定、部分衛生 | ○ Clojure 前例あり、中程度 | ○ 軽微な制約あり | ○ ほぼ直交（defmacro 本体 eval 注意） | △ defmacro 本体評価が末尾位置を変えうる | △ 別実装、既存 #jq 維持可 |
| D: Racket phase + parse | ◎ phase-level 分離で完全衛生 | × full redesign 必要、Phase 1 不可 | △ phase 設計が bytecode 設計に影響 | ◎ 完全直交（phase 分離） | ○ ほぼ独立 | △ 別実装 |
| E: reader macro 拡張 | ◎ 衛生不要（read time 変換のみ） | ◎ 既存 #jq の自然拡張 | ◎ AST は read 時点で確定、bytecode 無影響 | ◎ read time のみ | ◎ TCO とは別 phase | ◎ 既存 #jq と完全統合 |

## 既存 quasiquote / reader macro との関係

### quasiquote（SPEC §5.1-§5.2）の現在の役割

jalo の `quasiquote` は JSON モデル値の構築と `match` 左辺パターンに使うスペシャルフォームである（SPEC §5.1-§5.2）。`(var ...)` / `(rest-seq ...)` / `(rest-map ...)` を内蔵し、バッククオート構文 `` ` `` が展開される。

**重要な設計予告（SPEC §5.2 より）:**

> 「`(quasiquote <pattern>)` は第 1 版ではスペシャルフォームとして実装される（第 2 版以降でマクロ機構へ移行する予定）。」  
> — jalo SPEC §5.2 (accessed 2026-05-12)

本 ADR はこの「第 2 版マクロ機構」の具体的設計を確定する文書である。各 Option が quasiquote をどう継承・拡張するか:

- **Option A-D**: compile-time macro は quasiquote を「マクロ本体での値構築機構」として引き続き利用する。新 `defmacro` の展開テンプレート内で `` ` `` と `~`（unquote）が使われる（Clojure 等と同様）。
- **Option E**: read-time のみの変換であり、quasiquote はスペシャルフォームとして独立して維持される。Option E を採択した場合は「第 2 版マクロ機構への移行予告」（SPEC §5.2）の再評価が必要となる。

### reader macro `#jq(...)` との関係（SPEC §5.6）

SPEC §5.6 で定義された `#jq(...)` は jq フィルタ文字列を read 時に jalo AST へ変換する reader macro である。Lexer 段で `HashJqText` トークンとして切り出し、Parser 段で `JqParser.transpile()` を呼び出す。

Clojure の tagged literals（`#inst`, `#uuid` 等）と同様の reader-time 変換機構であり、その一般化が Option E の基本方針となる:

> "Tagged literals: ... The reader will check for a data reader function ..."  
> — Clojure official documentation, "Reader — Tagged Literals",  
> https://clojure.org/reference/reader#tagged_literals (accessed 2026-05-12)

**注記**: jalo の `#jq(...)` は AST 変換まで含む独自拡張（Clojure tagged literals は値構築のみ）。

**各 Option と `#jq` の共存方針:**
- **Option A-D**: compile-time macro と read-time `#jq` は並立する（Clojure 同様）。`defmacro` は compile-time 変換、`#jq` は read-time 変換として役割分担。
- **Option E**: `#jq` を `#<name>(...)` 一般化規則で再定義し、`defreader` で任意の reader macro を登録可能にする。compile-time macro は導入しない。

### DECISION_TCO.md との cross-reference

TCO 設計（DECISION_TCO.md）との相互作用は決定要因マトリクスの軸 (v) で評価した。Phase 2 で macro 機構と TCO 機構を同時に再評価することが合理的である（VISION.md §6 連動）。具体的な論点は下記「DECISION_TCO.md との交差論点」節を参照。

## オプション別 長所・短所

### Option A: Scheme syntax-rules スタイル（衛生マクロ）

パターンベースの完全衛生マクロ。R7RS small で標準化、宣言的なテンプレートと literals を記述する。

```jalo
; jalo への移植を想定したコード例
(define-syntax swap!
  (syntax-rules ()
    ((_ a b)
     (let ((tmp a))
       (set! a b)
       (set! b tmp)))))
```

**Scheme 標準 — R7RS §4.3:**

> "A transformer spec determines how each use of a macro is expanded into a new expression."  
> — Scheme R7RS §4.3, "Macros" (PDF),  
> https://small.r7rs.org/attachment/r7rs.pdf §4.3 (accessed 2026-05-12)

**長所:**
- 完全な衛生保証。識別子の暗黙的リネームにより変数捕捉なし。
- 宣言的なパターン記述で可読性が高い。
- Phase 2 bytecode と完全互換（展開後 AST を通常コンパイル）。
- 代数的エフェクトとの相互作用なし（構文変換のみ）。

**短所:**
- 任意の Lisp コードをマクロ本体に書けない（純粋なパターン変換のみ）。表現力は限定的。
- 既存 `#jq` と異なる枠組みのため、2 種類の構文変換機構が並立する。
- Scheme 経験者以外には構文が馴染みにくい。

### Option B: Scheme syntax-case スタイル（手続き的衛生マクロ）

syntax object をプログラムとして操作可能な、R6RS 準拠の手続き的衛生マクロ。Racket の syntax-parse の基盤となる方式。

```jalo
; jalo への移植を想定したコード例
(define-syntax swap!
  (lambda (stx)
    (syntax-case stx ()
      ((_ a b)
       (syntax (let ((tmp a))
                 (set! a b)
                 (set! b tmp)))))))
```

**R6RS §12 および SRFI-72:**

> "This SRFI describes a procedural macro proposal for Scheme with the following features: ..."  
> — SRFI-72, "Hygienic macros",  
> https://srfi.schemers.org/srfi-72/srfi-72.html (accessed 2026-05-12)

**長所:**
- 完全な衛生保証（syntax-rules 同等）。
- syntax object をマクロ本体で自由に操作できる（procedural）。
- Option D（Racket）への段階的移行が容易。

**短所:**
- syntax object の設計と実装が高コスト（JVM 上での新規モジュール設計が必要）。
- R6RS は Scheme 標準の中でも実装コストが高い部類。
- Phase 1 での実装として過剰な複雑さ。

### Option C: Clojure defmacro（auto-gensym + syntax-quote）

非衛生だが auto-gensym（`x#`）と namespace qualified symbol（syntax-quote `` ` ``）で実用上の衛生を担保する。Rich Hickey による JVM 上の Lisp としての実用主義的設計。

```jalo
; Clojure defmacro 方式の jalo への移植想定例
(defmacro swap! [a b]
  `(let [tmp# ~a]
     (set! ~a ~b)
     (set! ~b tmp#)))
```

**Clojure 公式ドキュメントより:**

> "Clojure has a programmatic macro system which allows the compiler to be extended by user code."  
> — Clojure official documentation, "Macros",  
> https://clojure.org/reference/macros (accessed 2026-05-12)

**auto-gensym の仕組み（Rich Hickey 設計、Clojure 公式ドキュメントより）:**

> "If a symbol is non-namespace-qualified and ends with '#', it is resolved to a generated symbol with the same name to which '_' and a unique id have been appended. e.g. x# will resolve to x_123."  
> — Clojure official documentation, "Reader — Syntax-quote",  
> https://clojure.org/reference/reader#syntax-quote (accessed 2026-05-12)

**注記**: Rich Hickey の defmacro 設計に関する talk 直接引用（transcript / blog 等）は確定できず。上記 Clojure 公式ドキュメントを Rich Hickey 設計の公式記述として引用する（捏造禁止 / 断定回避）。

**長所:**
- Clojure 開発者に最も馴染みがある。jalo の Clojure 影響（`let`, `fn`, `loop`）と整合する。
- auto-gensym で実用上の衛生を確保しながら、マクロ本体で任意の Lisp コードが書ける。
- JVM 実装コストは中程度（Clojure の前例がある）。
- quasiquote `` ` `` と unquote `~` は既存の quasiquote 機構を継承可能。

**短所:**
- 完全衛生ではない（namespace 跨ぎでは namespace-qualified symbol が必要）。Scheme 経験者の期待を下回る。
- `defmacro` 本体の評価タイミングが末尾位置を変える可能性がある（TCO 相互作用）。
- jq ユーザーには新しい概念（マクロ展開フェーズ）の学習が必要。

### Option D: Racket スタイル（phase-level 分離 + syntax-parse）

Matthew Flatt et al. による研究実装。phase-level で compile-time / run-time を厳格分離し、syntax-parse で複雑なパターンと高品質エラーメッセージを提供する。

```racket
; Racket での実例（jalo 移植時は括弧構文に変換）
(define-syntax (swap! stx)
  (syntax-parse stx
    [(_ a:id b:id)
     #'(let ([tmp a]) (set! a b) (set! b tmp))]))
```

**Racket Reference より:**

> "The first form creates a transformer binding of id with the value of expr, which is an expression at phase level 1 relative to the surrounding context."  
> — Racket Reference, "define-syntax",  
> https://docs.racket-lang.org/reference/define.html (accessed 2026-05-12)

> "The syntax/parse library provides a framework for writing macros and processing syntax."  
> — Racket Documentation, "syntax-parse",  
> https://docs.racket-lang.org/syntax/stxparse.html (accessed 2026-05-12)

**理論的基盤:**

> Composable and Compilable Macros: You Want it When?  
> — Matthew Flatt, ICFP 2002 (PDF),  
> https://www-old.cs.utah.edu/plt/publications/macromod.pdf (accessed 2026-05-12)

**長所:**
- 最も厳格な衛生保証（phase-level 分離により compile-time と run-time が明確に分離）。
- syntax-parse による高品質エラーメッセージと複雑なパターン記述。
- 長期的な教育的価値が高い。

**短所:**
- jalo の Phase 1 実装として過剰（Evaluator + Parser の full redesign が必要）。Phase 1 では実現不可能。
- phase-level 設計が Phase 2 bytecode 設計に影響を与える（DECISION_TCO.md との相互作用）。
- 保守担当者の学習コストが非常に高い。
- jq パワーユーザー・Clojure 経験者ともに学習障壁が高い。

### Option E: Reader macro 拡張中心（`#jq` 一般化、defmacro は最小）

既存 `#jq(...)` を `#<name>(...)` として一般化し、`defreader` で任意の reader macro を登録可能にする。compile-time macro 機構は導入せず、read-time のみで賄う jalo 独自路線。

```jalo
; 既存 #jq の一般化として、ユーザー定義 reader macro を想定
(defreader yaml [text] (parse-yaml text))
; 使用例:
#yaml(foo: 1, bar: 2)
; => {"foo": 1, "bar": 2}

; 既存 #jq は引き続き動作する
#jq(.name)
```

**Clojure の tagged literals との比較（二次資料として参考）:**

> "Tagged literals: ... The reader will check for a data reader function ..."  
> — Clojure official documentation, "Reader — Tagged Literals",  
> https://clojure.org/reference/reader#tagged_literals (accessed 2026-05-12)

**注記**: jalo の `#jq(...)` は AST 変換まで含む独自拡張（Clojure tagged literals は値構築のみ）。

**長所:**
- 既存 `#jq(...)` の自然な拡張で、実装コストが最小（既存 Lexer/Parser の拡張のみ）。
- jalo ユーザーの認知負荷が最小（新しい評価フェーズの概念不要）。
- Phase 2 bytecode と完全互換（AST は read 時点で確定）。
- 代数的エフェクトとの相互作用なし（read time のみ）。

**短所:**
- 表現力が限定的。compile-time での構文的抽象化（展開後 AST 変換）が不可能。
- SPEC §5.2「第 2 版マクロ機構移行予告」と合致しない。Option E を採択した場合は SPEC §5.2 の再評価が必要。
- Lisp の核心的な機能（ホモイコニシティを活かしたコード変換）を放棄することになる。
- Clojure 開発者・Scheme 経験者の期待と乖離する。

## 推奨

**推奨: Phase 2 で実装する。Phase 1 は当面マクロ機構なしで継続し、`quasiquote` + reader macro `#jq(...)` で代用する。Phase 2 着手時に 5 Options から確定する。**

Phase 1 における jalo のミッション（VISION.md §1）——JSON ネイティブなデータに対して予測可能・合成可能・エフェクトを意識した計算を行う——に対して、マクロ機構なし（当面の状態）は以下の理由で許容される:

- VISION.md §2「目標外」に「バージョン 1.0 でのマクロシステム」が明示されている（スコープ外として確定）。
- Phase 1 の代表的なユースケース（JSON ログ集計・API レスポンス変換）では、`quasiquote`・`fn`・高階関数で表現力は十分。
- マクロ機構の選択は TCO 設計（DECISION_TCO.md）との相互作用を含む重大な設計判断であり、Phase 2 の bytecode コンパイラ設計と合わせて確定するのが最も合理的。

**Phase 1 での現状代用手段（quasiquote + `#jq` reader macro）:**

```jalo
; 代用パターン 1: quasiquote で配列・マップを構築
(let [name "Alice"  age 30]
  `{name: $name  age: $age})
; => {"name": "Alice", "age": 30}

; 代用パターン 2: fn で高階抽象化（マクロ不要）
(def make-pair (fn [k v] `{$k: $v}))
(make-pair "x" 42)
; => {"x": 42}

; 代用パターン 3: #jq reader macro で jq パターンを埋め込み
(def get-name (fn [obj] #jq(.name)))
(get-name {"name": "Alice"})
; => "Alice"
```

Phase 2 での実装方式（Option A〜E の選択）は、当時の設計制約・プロジェクトオーナーの判断（Q1-Q6）を踏まえて改めて決定する。

Option D（Racket phase + parse）は Phase 2 以降に先送りする。JVM 実装難度が全 Options 中最も高く、Phase 1 では実現不可能であるためである。Option B〜D は Phase 2 以降の評価対象とする。Option A・C・E は Phase 2 での有力候補として Q1（衛生方式）の判断後に絞り込む。

## DECISION_TCO.md との交差論点

### macro と TCO の相互作用

DECISION_TCO.md Q6 は「将来のマクロシステムは TCO 戦略に制約を課すか」を Open Question として留保している。本節はその具体化である。

**展開後コードの末尾位置保持:**

compile-time macro（Option A-C）は AST を変換し展開後コードを生成する。展開後のコードが末尾位置にあれば TCO が適用可能であり、Option A/B/C は TCO 設計に追加制約を与えない。

```jalo
; swap! マクロを末尾位置で使う場合
(fn [a b]
  (swap! a b))   ; 展開後コードが末尾位置にあれば TCO 適用可能
```

**macro が effect handler frame を跨ぐ場合:**

マクロ展開後のコードが `handle` フォームを生成し、その内部から外部への末尾呼び出しを行う場合、DECISION_TCO.md「ハンドラ跨ぎの末尾呼び出し」と同様の問題が生じる可能性がある。

**Option 別の影響:**
- **Option A/B/C**: 展開後 AST は通常 jalo コードと同等、TCO 設計に追加制約なし（◎）。
- **Option D**: phase-level 分離が Phase 2 bytecode 設計に干渉する可能性（△）。
- **Option E**: read-time 変換のみ、TCO とは完全に別 phase（◎）。

**結論**: Option A/B/C/E は TCO 設計と直交する。Phase 2 での実装確定時に macro × TCO 統合設計を行う（VISION.md §6 連動）。

## 未解決の問い

以下の問いは、この ADR を確定させる前にプロジェクトオーナーの判断が必要である。

- **Q1: 衛生マクロのみか、非衛生も許すか**【優先度: 高】— Scheme 系（Option A/B/D）は完全衛生、Clojure（C）は auto-gensym 限定衛生、Option E は衛生概念外。「Scheme 流（完全衛生強制）/ Clojure 流（実用主義）/ E 路線（衛生概念導入回避）」のどれを採るか。Recommendation を導く核論点。
- **Q2: マクロと effect handler の相互作用** — `defmacro` 本体で `raise`/`handle` を使えるか？マクロ展開中の effect は通常評価と同じ扱いか？compile-time effect handler を別途設けるか？
- **Q3: マクロ展開タイミング** — read-time / compile-time / runtime のどれを正とするか。Option E は read-time のみ、Option A-D は compile-time。Phase 1（tree walker）では compile-time が tree-walk-time となる。
- **Q4: hygiene vs 学習コスト** — jq ユーザーはマクロ未経験、Clojure 経験者は `defmacro` に馴染みあり、Scheme 経験者は `syntax-rules` を期待。学習コスト最小化のためどのスタイルを採るか（VISION.md §4 Target Users との整合）。
- **Q5: macro と TCO の相互作用（DECISION_TCO.md Q6 の具体化）**【優先度: 中】— CPS 変換型マクロ（Option D の極端例）は TCO 設計を侵食する可能性。DECISION_TCO.md Phase 2 での TCO 実装方式確定時に macro 機構選択がどう影響するか。
- **Q6: 「第 2 版マクロ機構移行（SPEC §5.2）」の具体的タイミング** — 本 ADR で確定したマクロ機構を実際に実装するのは Phase 2 着手時か、Phase 1 末期の別 cmd か。VISION.md §6「マクロシステム（1.0+）」との整合。

## 参考文献

| 言語/規格 | リソース | URL | アクセス日 |
|---|---|---|---|
| Clojure | Macros（defmacro 公式仕様） | https://clojure.org/reference/macros | 2026-05-12 |
| Clojure | Reader — Syntax-quote（auto-gensym） | https://clojure.org/reference/reader#syntax-quote | 2026-05-12 |
| Clojure | Reader — Tagged Literals | https://clojure.org/reference/reader#tagged_literals | 2026-05-12 |
| Clojure | Special Forms | https://clojure.org/reference/special_forms | 2026-05-12 |
| Scheme | R7RS small §4.3 Macros（PDF） | https://small.r7rs.org/attachment/r7rs.pdf | 2026-05-12 |
| Scheme | R6RS §12 Syntax-case | https://www.r6rs.org/final/html/r6rs/r6rs-Z-H-16.html | 2026-05-12 |
| Scheme | SRFI-72: Hygienic macros（Sperber + Dybvig） | https://srfi.schemers.org/srfi-72/srfi-72.html | 2026-05-12 |
| Scheme | The Scheme Programming Language §8 Syntactic Extension（二次資料） | https://www.scheme.com/tspl4/syntax.html | 2026-05-12 |
| Racket | Reference — define-syntax | https://docs.racket-lang.org/reference/define.html | 2026-05-12 |
| Racket | Documentation — syntax-parse | https://docs.racket-lang.org/syntax/stxparse.html | 2026-05-12 |
| Racket | Composable and Compilable Macros: You Want it When?（Flatt, ICFP 2002, PDF） | https://www-old.cs.utah.edu/plt/publications/macromod.pdf | 2026-05-12 |
| jalo | SPEC §5.2 quasiquote 意味、§5.6 reader macro #jq | （本リポジトリ docs/SPEC.md） | 2026-05-12 |
| jalo | VISION.md §6 長期ビジョン | （本リポジトリ docs/VISION.md） | 2026-05-12 |
| jalo | DECISION_TCO.md（ADR-001） | （本リポジトリ docs/DECISION_TCO.md） | 2026-05-12 |
