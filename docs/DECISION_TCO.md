# ADR-001: jalo 末尾呼び出し最適化（TCO）戦略

## ステータス

Phase 1 確定 / Phase 2 再評価予定 — Phase 1 は Option F（処理系 TCO なし）+ stdlib trampoline 緩和策で確定。深い再帰は trampoline 経由でスタック消費なしに実現できる。TCO の処理系実装方式は Phase 2 着手時に改めて確定する（下記「推奨」節を参照）。

## 背景と TCO の本質

末尾位置の関数呼び出しを実行する**前**に、現在のコールスタックフレームを
巻き戻し、新フレームを積まずに制御を移すことで、再帰呼び出しがスタックを
消費しない最適化。

### ケース (i) 通常の自己末尾再帰での TCO 巻き戻し

以下のような自己末尾再帰関数を考える:

```jalo
(fn loop [n acc]
  (if (= n 0)
    acc
    (loop (- n 1) (+ acc 1))))     ; ← 末尾位置の自己呼び出し
```

TCO **なし**の実装 (現 jalo Phase 1) では Java スタックが下記のように積まれる:

```
┌─────────────────────────┐
│ eval (loop 3 0)         │  ← F1
├─────────────────────────┤
│ eval (loop 2 1)         │  ← F2
├─────────────────────────┤
│ eval (loop 1 2)         │  ← F3
├─────────────────────────┤
│ eval (loop 0 3)         │  ← F4 → 結果 3 を返却
└─────────────────────────┘
```

TCO **あり**の実装 (Option A の recur 採用時) では、末尾呼び出し直前にフレームを
巻き戻して同一フレームを再利用する:

```
┌─────────────────────────┐
│ eval (loop n acc)       │  ← 同じ F1 を再利用
│   n=3, acc=0  →         │
│   n=2, acc=1  →         │
│   n=1, acc=2  →         │
│   n=0, acc=3  → 結果 3  │
└─────────────────────────┘
```

本質は「終端関数呼出しの**前**にコールスタックを巻き戻す」こと。これにより無制限
深度の再帰が `StackOverflowError` を起こさずに完走する。

jalo は JVM（Java 21）上で動作する純粋関数型の Lisp 方言である。再帰的アルゴリズムは関数型プログラミングの慣用形だが、JVM はバイトコードレベルでの透過的な末尾呼び出し最適化をサポートしない。すべての呼び出しフレームはスタック領域を消費するため、無制限の再帰は最終的に `StackOverflowError` を引き起こす。

現在のプロジェクト状態は ISSUES.md I-08 に記録されている:

> **I-08**: 末尾呼び出し最適化 (TCO) — 状態: 資料 cmd_424 で評価中 (詳細 DECISION_TCO.md 参照)

同一の JVM 制約に直面した Clojure は、透過的 TCO ではなく明示的な `recur` 特殊形式でこれを解決した:

> "Since Clojure uses the Java calling conventions, it cannot, and does not, make the same tail call optimization guarantees."  
> — Clojure official documentation, "Functional Programming — Recursive Looping",  
> https://clojure.org/about/functional_programming (accessed 2026-05-12)

> "recur is the only non-stack-consuming looping construct in Clojure. There is no tail-call optimization and the use of self-calls for looping of unknown bounds is discouraged."  
> — Clojure official documentation, "Special Forms — recur",  
> https://clojure.org/reference/special_forms#recur (accessed 2026-05-12)

この決定記録は、jalo のアーキテクチャの文脈で TCO への 6 つのアプローチを評価し、プロジェクトオーナーのレビューに向けた推奨を提供する。

## 決定の要因

1. **代数的エフェクトハンドラとの相互作用**【最高優先度】— jalo の `raise`/`handle` 機構（SPEC §4.4）はコールスタック上に動的なフレーム境界を作る。いかなる TCO 戦略も、ハンドラのフレーム境界を保持するか、明示的に考慮しなければならない。
2. **JVM Phase 1 での実装可能性** — 現在の実装はツリーウォーキングインタープリタである。深いバイトコード変換を必要とするオプションは Phase 1 では実現不可能である。
3. **Phase 2 バイトコード互換性** — 選択した戦略は計画中の JVM バイトコードコンパイラ（DESIGN.md §1）をブロックしてはならない。Phase 1 では機能するが Phase 2 に向けて完全な再設計を要するような戦略は望ましくない。
4. **表現力** — 戦略は一般的な関数型イディオムをサポートすること。最低限、自己末尾再帰；理想的には相互再帰。
5. **学習コスト** — jalo の対象ユーザーには jq パワーユーザーと Clojure 開発者が含まれる。戦略は彼らの既存のメンタルモデルと一致すること。
6. **パフォーマンス** — Phase 1 は速度より正確さを優先するが、壊滅的なオーバーヘッドは不適格である。

## ハンドラ跨ぎ末尾呼び出し

jalo の evalHandle (Evaluator.java L375-394) は Java try/catch ベースで動作する。
JaloEffectSignal は RuntimeException を継承し、fillInStackTrace() を抑制して
例外生成コストを削減している。この実装は中断のみ (abort-only) の shallow handler
相当 (SPEC.md §4.4 参照)。

evalHandle が動作する際の Java スタックフレームのレイアウトは以下の通り:

```
evalHandle(form, env)                          ← Java frame F1
  try { eval(form.get(1), env) }               ← Java frame F2 (body eval)
    applyForm(...)                             ← Java frame F3
      eval(...)                                ← Java frame F4 (recursive)
        ...                                    ← F5, F6, ...
        throw new JaloEffectSignal(tag, val);  ← JVM stack unwind
  } catch (JaloEffectSignal sig) {             ← F1 まで JVM stack 巻き戻し
      evaluate handler[2] in handleEnv         ← 新たな eval frame F2'
  }
```

### 問題の定義

jalo のエフェクトハンドラはコールスタック上に動的なフレーム境界を作る。ハンドラ境界を越える末尾呼び出し——呼び出しサイトが `(handle ...)` フォームの内側にあり、継続がその外側にある場合——は、ハンドラフレームも同時にアンワインドしなければ最適化できない。これはスタックの安全性とハンドラセマンティクスの間に根本的な緊張を生む。

例:

```jalo
(handle
  (f x)   ; f の最後の動作は、このハンドラのスコープ外にある g を呼び出すこと
  [effect v (resume v)])
```

Option A では、`(recur ...)` を使って `handle` フレーム境界を越えることはできない。`recur` は最も近い `loop` または `fn` フレームに限定される。`g` への呼び出しは常にスタックフレームを消費する。

**handler 跨ぎ TCO の優先度については Atsushi Furuta (furuta@furuta.bsdclub.org) の判断待ちとする（Phase 2 設計時に確定予定）。本 ADR では Open Question として留保する。**

### ケース (ii) ハンドラ跨ぎ末尾呼出しで TCO と handle が衝突するケース

```jalo
(handle
  (loop 1000000 0)            ; ← (loop ...) は末尾位置だが handle の内側
  [effect v (resume v)])
```

現状の jalo evalHandle 実装 (Evaluator.java L375) では下記のように積まれる:

```
┌──────────────────────────────────────────┐
│ evalHandle(...)                          │  ← Java F1 (catch を待つ frame)
│   try { eval(body=(loop ...)) }          │
├──────────────────────────────────────────┤
│   eval (loop 1000000 0)                  │  ← Java F2
├──────────────────────────────────────────┤
│   eval (loop 999999 1)                   │  ← Java F3
├──────────────────────────────────────────┤
│   ...                                    │  ← F4, F5, ... 蓄積
└──────────────────────────────────────────┘
```

ここで TCO が F2 以降を素朴に巻き戻して F1 を再利用しようとすると、**F1 の try
ブロックも消滅**する。その後 (loop ...) 内部で `(raise "effect" v)` が起きた場合、
catch する handler が消えており、effect は外側 (REPL や CLI) に漏れる。これは
handle の意味論を破壊する。

したがって TCO 戦略は handle 境界をまたぐ呼び出しを除外するか (Option A / C)、
ハンドラフレーム自体を継続として扱うか (Option D / E) を選ばねばならない。

### jq try-catch との互換性

jalo の `handle`/`raise` は jq の `try … catch` のエフェクト対応版である。jq の `try`/`catch` パターンから `handle`/`raise` 形式に変換されたコードは、ハンドラが Option A・C では越えられない囲みフレームを作るため、末尾呼び出し最適化の機会を抑制する可能性がある。

オプション別の影響:

- **Option A・C** は jq 互換性を保つが、ハンドラ跨ぎ TCO を妨げる。
- **Option D・E** はハンドラ跨ぎ TCO を可能にするが、実装コストが高い。
- **Option F** は問題を完全に回避する。ユーザーは深いハンドラ跨ぎ再帰を避けること。

### 6 オプション：ハンドラ越えの比較

| オプション | ハンドラ跨ぎ末尾呼び出し | エフェクト透過性 | jq try-catch への影響 |
|---|---|---|---|
| A: `recur` | × 同フレームのみ | ○ ハンドラ非影響 | △ ハンドラがクロスフレーム `recur` をブロック |
| B: トランポリン | △ 可能だがハンドラはトランポリン対応が必要 | △ トランポリンはハンドラフレームを破棄してはならない | × トランポリン境界が try-catch セマンティクスを乱す可能性 |
| C: 自己末尾のみ | × 同フレームのみ | ○ | △ A と同じ |
| D: CPS | ○ CPS はハンドラを継続として自然に統合 | ◎ | ○ |
| E: 限定継続 | ◎ ネイティブ統合 | ◎ | ◎ |
| F: TCO なし | n/a | ◎ 干渉なし | ◎ 干渉なし |

### 推奨

ハンドラ跨ぎ末尾呼び出しが*必要かどうか*はプロジェクトオーナーの判断待ちである（Open Question として留保；下記「未解決の問い」Q4 参照）。必要でない場合は Option A または F で十分である。

## 検討オプション

- **Option A**: 明示的 `(recur ...)` — Clojure スタイル
- **Option B**: 暗黙的 TCO + トランポリン — Scheme スタイル
- **Option C**: 自己末尾のみ最適化 — 保守的サブセット
- **Option D**: CPS（継続渡しスタイル）変換
- **Option E**: 限定継続 — Koka スタイル
- **Option F**: TCO なし — 現状維持（I-08 Option F）

## 決定要因マトリクス

各セルは一行の根拠と記号を示す。◎ 最良 / ○ 良好 / △ 制約あり / × 不利

| オプション | エフェクトハンドラ | JVM Phase 1 | Phase 2 バイトコード | 表現力 | 学習コスト | パフォーマンス |
|---|---|---|---|---|---|---|
| A: `recur`（Clojure） | △ 同フレームのみ；ハンドラ境界を越えられない | ◎ バイトコード変換不要 | ○ Phase 2 設計と互換 | ○ 自己/ループ再帰をカバー | △ 非 Clojure ユーザーには新しい構文 | ◎ ヒープオーバーヘッドなし |
| B: 暗黙的 TCO + トランポリン | × トランポリンはエフェクトを意識する必要あり；ナイーブな実装はハンドラセマンティクスを壊す | ○ 中程度の労力で実現可能 | △ Phase 2 でのセマンティクス乖離リスク | ◎ Scheme スタイルの適切な末尾再帰 | ○ 新構文不要 | ○ バウンスごとにヒープ確保 |
| C: 自己末尾のみ | ○ 自己呼び出しはハンドラ境界を越えない | ◎ 最も実装が単純 | ○ 互換 | △ 相互再帰は対象外 | △ 部分的な最適化は驚きをもたらす | ◎ |
| D: CPS 変換 | ◎ ハンドラは継続として自然に統合 | × 非常に重い；インタープリタ全体を書き換える | △ Phase 2 設計に強い制約 | ◎ ハンドラ跨ぎ・相互再帰を含む完全な汎用性 | × 保守担当者の学習コスト非常に高い | × ヒープ確保コスト高 |
| E: 限定継続 | ◎ ネイティブ統合；エフェクトハンドラは継続 | × 非常に重い；Phase 1 では実現不可能 | × 根本的な再設計が必要 | ◎ 完全な汎用性 + 将来の継続サポート | × 非常に高い | × 非常に低速 |
| F: TCO なし（現状） | ◎ 相互作用なし | ◎ 実装コストゼロ | ◎ Phase 2 に制約なし | × 深い再帰は禁止；StackOverflow リスク | ◎ 最もシンプル；ユーザーは `reduce`/`map` を使用 | ◎ |

## オプション別 長所・短所

### Option A: 明示的 `recur`（Clojure スタイル）

`recur` は引数を評価し、最も近い囲みの `loop` または `fn` フレームを再バインドして、追加のスタックフレームを消費せずに実行を再開する特殊形式である。

**Clojure の設計根拠 — 公式ドキュメントより:**

> "While not as general as tail-call-optimization, it allows most of the same elegant constructs, and offers the advantage of checking that calls to recur can only happen in a tail position."  
> — Clojure official documentation, "Functional Programming — Recursive Looping",  
> https://clojure.org/about/functional_programming (accessed 2026-05-12)

JVM は透過的 TCO をサポートしないためにこの選択がなされた（背景節参照）。`recur` が使えない相互末尾再帰のために、Clojure はフォールバックとして `trampoline` ユーティリティを提供する。

Rich Hickey は Clojure の状態と制御に対する明示的なアプローチの背後にある広範な設計哲学を "Are We There Yet?" (JVM Languages Summit 2009, https://www.infoq.com/presentations/Are-We-There-Yet-Rich-Hickey/, accessed 2026-05-12) と "Simple Made Easy" (Strange Loop 2011, https://www.infoq.com/presentations/Simple-Made-Easy/, accessed 2026-05-12) で発表している。これらの講演は信頼性の前提条件としての明示性を主張し、末尾呼び出しの意図を不可視のコンパイラ変換に依存するのではなくプログラマに見えるようにすることを動機付けている。

**長所:**
- バイトコード変換なしに JVM スタック制約に直接対処する。
- 末尾呼び出しの意図を明示する。パフォーマンスが重要なパスがコードで見える。
- インタープリタは `recur` が末尾位置に現れることを静的に検証し、そうでなければエラーを報告できる。
- Clojure 開発者に馴染み深い。jalo の Clojure 影響を受けたイディオム（`let`・`fn`・`loop`）と整合する。

**短所:**
- 同じ囲みフレームに制限される。相互末尾再帰は `trampoline` が必要。
- `handle` 境界を越えられない。エフェクトを多用するコードでハンドラ跨ぎループに `recur` を使えない。
- jq ユーザーと初心者は jq にない新しい構文を学ぶ必要がある。

#### recur の相互再帰非対応と trampoline によるフォールバック

`recur` は最も近い囲みの `loop` または `fn` フレームに限定される (Clojure 公式:
Special Forms — recur)。したがって相互再帰末尾呼び出しでは使用できない。

Clojure はこの制約に対する標準回避策として `trampoline` ユーティリティを提供する:

> "If f returns a fn, calls that fn with no arguments, and continues to repeat,
> until the return value is not a fn"
> — Clojure 公式 ClojureDocs `clojure.core/trampoline`,
> https://clojuredocs.org/clojure.core/trampoline (accessed 2026-05-17)

```clojure
(defn is-even [n]
  (if (zero? n) true #(is-odd (dec n))))   ; 末尾で fn を返す

(defn is-odd [n]
  (if (zero? n) false #(is-even (dec n)))) ; 末尾で fn を返す

(trampoline is-even 1000000)               ; bouncing で stack 消費なし
```

各関数は末尾で fn を返し、`trampoline` がそれを引数なしで呼び出す。これを fn でない
値が返されるまで繰り返す。コードが一段冗長になる代わりに、stack 消費なしで相互再帰
を表現できる。

**jalo での含意**: Option A を採用する場合、相互末尾再帰には Clojure と同様の
trampoline パターンを stdlib に追加することが望ましい (Phase 2 着手時)。署名候補:
`(trampoline <fn-or-value> <args>...)`。本 ADR §6 マトリクスの「相互再帰」軸で
Clojure と同等の表現力を確保できる

### Option B: 暗黙的 TCO + トランポリン（Scheme スタイル）

すべての末尾呼び出しが自動的に最適化される。インタープリタは末尾位置を検出し、新しいスタックフレームを生成する代わりに現在のフレームを置き換えるか、JVM 上でこれをシミュレートするヒープ確保トランポリンを使用する。

**Scheme 標準 — R5RS §3.5 "Proper Tail Recursion":**

> "An implementation is properly tail-recursive if it supports an unbounded number of active tail calls... no space is needed for an active tail call because the continuation used in the tail call has the same semantics as the continuation passed to the procedure containing the call."  
> — Scheme R5RS §3.5, "Proper Tail Recursion",  
> https://conservatory.scheme.org/schemers/Documents/Standards/R5RS/HTML/r5rs-Z-H-6.html (accessed 2026-05-12)

R7RS も同様の要件を持つ。実装は「適切な末尾再帰」でなければならない。

**長所:**
- 透過的。すべての慣用的な再帰コードが自動的にスタックセーフになる。
- 新構文不要。既存コードがすぐに恩恵を受ける。
- 「適切な末尾再帰」の Scheme 標準保証に一致する。

**短所:**
- エフェクトハンドラはトランポリンが尊重しなければならないフレーム境界を作る。ナイーブな実装はハンドラセマンティクスを壊す（ハンドラ跨ぎ節参照）。
- トランポリン機構はバウンスごとにヒープ確保オーバーヘッドをもたらす。
- JVM Phase 1 の実装複雑さは中〜高程度。
- Phase 1 トランポリンと Phase 2 バイトコードコンパイラ間でセマンティクスが乖離するリスク。

### Option C: 自己末尾のみ

自己再帰末尾呼び出し（関数が末尾位置で直接自身を呼び出す場合）のみを最適化する。相互末尾再帰とハンドラ跨ぎ末尾呼び出しはカバーしない。

**長所:**
- 完全な TCO より実装が単純。検出するパターンは一つだけ。
- 自己呼び出しはハンドラ境界を越えないため、ハンドラとの相互作用は問題にならない。
- Phase 2 バイトコードと互換。

**短所:**
- 相互再帰をカバーしない。一部の関数型パターンで表現力が制限される。
- 完全な適切 TCO（Scheme）か明示的 `recur`（Clojure）かのいずれかを期待するユーザーを混乱させる可能性がある。
- 部分的な最適化は驚きを引き起こす可能性がある。「末尾再帰風」コードでも関数を跨ぐ再帰ならオーバーフローする。

### Option D: CPS（継続渡しスタイル）変換

インタープリタはすべての関数呼び出しを継続渡しスタイルに変換する。コールスタックはヒープ確保の継続クロージャに置き換えられる。エフェクトハンドラはそれ自体が継続であるため自然に統合される。

**長所:**
- 完全な汎用性。ハンドラ跨ぎ・相互再帰を含むすべての末尾呼び出しがスタックセーフ。
- 代数的エフェクトが CPS フレームワークに綺麗に統合される。
- 理論的に十分に研究されており正しい。

**短所:**
- Phase 1 では実装複雑さが非常に高い。インタープリタ全体を書き換える必要がある。
- すべての継続のヒープ確保によりパフォーマンスが大幅に低下する。
- Phase 2 バイトコード設計に大きな制約をもたらす。
- 保守担当者の学習コストが非常に高い。

### Option E: 限定継続（Koka スタイル）

エフェクトハンドラはファーストクラスの限定継続として実装される。ハンドラ内および跨ぎの末尾呼び出しはネイティブにスタックセーフ。これは「TRMC 最適化」（末尾再帰修正コンス）を特徴とする Koka（Daan Leijen、Microsoft Research）のアプローチであり、エフェクトハンドラを再開可能継続として扱う（Koka Language Book, https://koka-lang.github.io/koka/doc/book.html, accessed 2026-05-12）。

OCaml 5.0 は、既存のコンパイル言語にエフェクトハンドラを限定継続として後付けするという関連する方向を取った（OCaml 5 Manual — Effect Handlers, https://ocaml.org/manual/effects.html, accessed 2026-05-12）。

**長所:**
- 最も汎用的な解決策。ハンドラ跨ぎ末尾呼び出しをネイティブに処理する。
- 再開可能継続との前方互換性（I-06・SPEC §4.4）。
- 長期的なエフェクトシステムの方向性と整合する。

**短所:**
- すべてのオプション中、実装複雑さが最も高い。
- Phase 1 では実現不可能。根本的な再設計が必要。
- Phase 2 バイトコードでも全面的な再設計が必要。
- 学習コストが非常に高い。

### Option F: TCO なし — 現状維持

バージョン 1.0 では TCO を実装しない。制限を I-08 に明確に文書化し、深い再帰は `reduce`・`map`・`filter` といった高階反復関数で避けるようユーザーに案内する。

Erlang では BEAM 仮想マシンが適切な末尾呼び出しを自動的に処理する（"If the last expression of a function body is a function call, a tail-recursive call is done. This is to ensure that no system resources, for example, call stack, are consumed." — Erlang Reference Manual, Functions, https://www.erlang.org/doc/reference_manual/functions.html, accessed 2026-05-12）。JVM 上の jalo はこの保証を上記いずれかのオプションなしには提供できない。Option F はこの制限を隠さず明示する。

**長所:**
- 実装コストゼロ。
- 代数的エフェクトとの相互作用なし。
- Phase 2 計画との完全互換。バイトコードコンパイラが独立して TCO を追加できる。
- 再帰深度が典型的な API ペイロードの構造によって制限される大半の JSON 変換ユースケースに十分。

**短所:**
- 深い再帰はスタックセーフでない。大規模入力を再帰処理するプログラムは `StackOverflowError` を投げる。
- Scheme の適切な末尾再帰という根本的な保証からの乖離。
- Lisp 経験者のユーザーから言語の重大な制限として認識される可能性がある。

*Haskell に関する注記:* Haskell の遅延評価は保護された再帰——「再帰呼び出しがデータコンストラクタ内に発生する場合」——を通じて異なる形のスタックの安全性を提供する。これは従来の TCO とは根本的に異なる。「Haskell では関数呼び出しモデルが若干異なり、関数呼び出しが新しいスタックフレームを使わない可能性があるため、関数を末尾再帰にすることは通常それほど重要ではない。」（Haskell Wiki, "Tail recursion", https://wiki.haskell.org/Tail_recursion, accessed 2026-05-12）。jalo は正格評価（eager evaluation）を使用するため、Haskell の遅延サンク（lazy thunk）モデルは適用されず、直接採用できない。

## 推奨

**推奨: Phase 1: Option F (処理系 TCO なし) + stdlib `trampoline` 緩和策を採用する。深い自己/相互末尾再帰は `trampoline` 経由でスタック消費なしに実現できる。Phase 2 着手時に Option A/B/C/D/E の中から処理系 TCO 実装方式を改めて確定する。**

Phase 1 における jalo のミッション（VISION.md §1）——対話的探索と確実な自動化を可能にする合成可能な JSON ネイティブ計算——に対して、Option F（TCO なし）は以下の理由で適切である:

- JSON 変換パイプラインのほとんどのユースケースでは、再帰深度が典型的な API ペイロードの構造によって自然に制限される。
- 深い再帰が必要な場合は `reduce`・`map`・`filter` といった高階反復関数で代替できる。
- TCO 戦略の選択は代数的エフェクトとの相互作用を含む重大な設計判断であり、Phase 2 のバイトコードコンパイラ設計と合わせて確定するのが最も合理的である。

Phase 2 での TCO 実装方式（Option A〜E の選択）は、当時の設計制約・コミュニティフィードバック・プロジェクトオーナーの判断を踏まえて改めて決定する。

Option B〜E は Phase 2 以降に先送りする。代数的エフェクトとの相互作用・実装コスト、あるいはその両方が Phase 1 に相応しい水準を超えているためである。

#### cmd_427 (Phase / バージョニング独立) との整合

本 ADR は「Phase 2 で TCO を実装する」を推奨するが、これは **`1.0.0` への到達条件
を満たすことを意味しない**。SPEC.md §1.2 で定義する `1.0.0` 到達条件は以下の三条件
である:

- 言語仕様が安定し、後方互換性を維持できる状態であること。
- 年単位の deprecation 期間を運用できる体制が整っていること。
- 非互換変更に対する警告機構を実装側で提供できる能力を持つこと。

Phase 2 の TCO 実装 (Option A〜E のいずれか) は上記三条件の前提ではない。Phase 2
着手後も `0.x.y` 系列を継続することは正当であり、jalo は個人実験プロジェクトとして
`1.0.0` への到達は永遠に発生しない可能性も含めて運用する (DESIGN.md §2 実装フェーズ
節参照)。

## 未解決の問い

以下の問いは、この ADR を確定させる前にプロジェクトオーナーの判断が必要である。

- **Q4: ハンドラ跨ぎ末尾呼び出し** — ハンドラ跨ぎ末尾呼び出しは必須・推奨・不要のいずれか。これは Option A/C で十分か、最終的に D/E を検討する必要があるかを左右する主要な要因である。**優先度はプロジェクトオーナーのご判断待ち（Phase 2 設計時に確定予定）。本 ADR では Open Question として留保する。**
- **Q5: ADR のオープン問い** — この ADR はマージ前に最終的な決定を記録すべきか、「Phase 1 確定 / Phase 2 再評価予定」は個人実験プロジェクトとして長期状態として許容されるか。
- **Q6: マクロとの相互作用** — マクロ × TCO の相互作用は考慮外（2026-05-17 amendment_1 確定）。TCO はマクロ展開後の AST に対してのみ適用する。詳細は DECISION_MACRO.md §推奨節参照。
- **Q7: Phase 1 のスコープ** — Option F（Phase 1 では TCO なし）は Lisp 方言としての jalo のポジショニングを考慮して許容されるか。
- **Q10: Option F の緩和策** — Option F が選択された場合、どのような文書化と人間工学的な緩和策が必要か（推奨スタック深度制限・反復イディオムガイドなど）。
  **→ cmd_438 回答**: stdlib `(trampoline f & args)` (Clojure 互換シグネチャ) を
  Phase 1 で実装し、以下を提供する:
  (1) 末尾で `fn` を返す関数を書けば相互/自己末尾再帰がスタック消費なしに実現できる
  (2) 実装は Java 側 while ループによるバウンス (JVM スタック非消費)
  (3) SPEC.md stdlib 表に `trampoline` を追記し、推奨イディオム例を STDLIB_STATUS.md に記録
  これにより Q10 は解決済みとする。

## 参考文献

| 言語 | リソース | URL | アクセス日 |
|---|---|---|---|
| Clojure | Special Forms — recur | https://clojure.org/reference/special_forms#recur | 2026-05-12 |
| Clojure | Functional Programming — Recursive Looping | https://clojure.org/about/functional_programming | 2026-05-12 |
| Clojure | Are We There Yet? (Rich Hickey, JVM Languages Summit 2009) | https://www.infoq.com/presentations/Are-We-There-Yet-Rich-Hickey/ | 2026-05-12 |
| Clojure | Simple Made Easy (Rich Hickey, Strange Loop 2011) | https://www.infoq.com/presentations/Simple-Made-Easy/ | 2026-05-12 |
| Clojure | ClojureDocs — clojure.core/trampoline | https://clojuredocs.org/clojure.core/trampoline | 2026-05-17 |
| Scheme | R5RS §3.5 Proper Tail Recursion | https://conservatory.scheme.org/schemers/Documents/Standards/R5RS/HTML/r5rs-Z-H-6.html | 2026-05-12 |
| Koka | Koka Language Book | https://koka-lang.github.io/koka/doc/book.html | 2026-05-12 |
| Erlang | Reference Manual — Functions | https://www.erlang.org/doc/reference_manual/functions.html | 2026-05-12 |
| OCaml | Manual — Effect Handlers (OCaml 5) | https://ocaml.org/manual/effects.html | 2026-05-12 |
| Haskell | Haskell Wiki — Tail recursion | https://wiki.haskell.org/Tail_recursion | 2026-05-12 |

### amendment_2 (cmd_438, 2026-06-03)

- 推奨を「Option F + stdlib trampoline」に改訂 (処理系 TCO は Phase 2 以降)
- Q10 (Option F の緩和策) を `trampoline` 実装で解決済みに更新
- ステータス節に trampoline 緩和策の言及を追加
- 実装詳細は STDLIB_STATUS.md、テストは EvaluatorTest.java 参照
