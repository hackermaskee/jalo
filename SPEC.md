# jalo 言語仕様 (ドラフト)

## 1. 概要

jalo は S 式の代わりに JSON データモデルを AST として使う Lisp 方言である。
ホモイコニックな純粋関数型言語。

### 1.1 名前について

言語名は **jalo** (ジャロ)。以下の由来を持つ。

- **J**SON **A**nd **L**isp **O**perations の頭字語
- 4 文字 2 音節で発音しやすく、CLI コマンド名として扱いやすい (`jalo run foo.jal`)
- ファイル拡張子は `.jal`
- npm / PyPI / Maven Central / GitHub のいずれにも同名の競合プロジェクトが存在しない

旧仮称 `sjl` は廃止。

---

## 2. JSON モデル

jalo の AST は **JSON モデル** である。JSON テキストそのものではなく、
JSON テキストが表現するデータ構造（の抽象）を指す。

JSON モデルは再帰的に定義される:

- **基本データ**: null, boolean (true/false), 数値, 文字列
- **配列**: `[d0, d1, ..., dn]` — 各要素が JSON モデル
- **オブジェクト (Map)**: `{s0:d0, s1:d1, ..., sn:dn}` — キーは互いに異なる文字列、値は JSON モデル

JSON モデルの表現としては JSON テキスト・YAML・後述の標準構文などが使える。

### 2.1 数値型仕様

jalo の **プログラム (AST)** は JSON モデルであり、JSON モデルの数値は IEEE 754 binary64 (倍精度浮動小数点) である。一方 jalo の **実行時データ** は JSON モデルの厳密なスーパーセット (関数値など JSON では表現できない値を含む) であるため、追加の数値型を導入できる。

第 1 版でサポートする数値型は以下の 3 種類とする。`byte`・`short`・多倍長整数 (`bigint`, `bigdecimal`) は将来版で検討する。

| 型 | 内部表現 | 範囲 |
|----|----------|------|
| `double` | IEEE 754 binary64 倍精度浮動小数点 | ±1.7976931348623157 × 10³⁰⁸ |
| `int` | 32 ビット符号付き整数 (Java `int` 相当) | −2³¹ 〜 2³¹−1 |
| `long` | 64 ビット符号付き整数 (Java `long` 相当) | −2⁶³ 〜 2⁶³−1 |

#### `double` 型

jq および JavaScript の Number 型と同一の挙動とする。

##### リテラル構文

JSON (RFC 8259 §6) のサブセットとして定義される。JavaScript 固有の構文 (`0x…`, `0o…`, `0b…`) は非対応。

```
number    = [ "-" ] integer [ fraction ] [ exponent ]
integer   = "0" | nonzero-digit *DIGIT
fraction  = "." 1*DIGIT
exponent  = ("e" / "E") [ "+" / "-" ] 1*DIGIT
```

- 先頭ゼロ禁止: `0` は合法、`01` は非合法 (JSON 準拠)
- `NaN`・`Infinity` はリテラルとして書けない (JSON 仕様上、数値テキストとして表現不可能)

##### 精度と範囲

| 項目 | 値 |
|------|----|
| 表現 | 64 ビット IEEE 754 倍精度浮動小数点 |
| 整数/浮動小数点の区別 | なし (`1` と `1.0` は同じ値) |
| 有効桁数 | 十進数で約 15〜17 桁 |
| 安全整数範囲 | −(2⁵³−1) 〜 2⁵³−1 (±9,007,199,254,740,991) |
| 最大有限値 | ±1.7976931348623157 × 10³⁰⁸ |

安全整数範囲内の整数はすべて正確に表現できる。範囲外の整数は精度が失われる場合がある。

##### 特殊値

以下の特殊値が存在する。いずれも JSON テキストでは表現できないが、演算結果として発生しうる。

| 値 | 意味 | 発生例 |
|----|------|--------|
| `NaN` | 非数 (Not-a-Number) | `0/0`、数値への変換失敗 |
| `+Infinity` | 正の無限大 | `1/0`、オーバーフロー |
| `-Infinity` | 負の無限大 | `-1/0` |
| `-0` | 負のゼロ | `-1 / +Infinity` |

##### 演算の挙動

- 四則演算は IEEE 754 の **最近接偶数丸め (round-half-to-even)** に従う
- 有限値のオーバーフロー → `+Infinity` または `-Infinity`
- 有限値のアンダーフロー → `0` または `-0`
- `NaN` を含む算術演算の結果は `NaN`

##### 比較規則

- `NaN == NaN` は **偽** (NaN は自分自身とも等しくない)
- `-0 == +0` は **真** (比較演算では符号を区別しない)
- 等値判定で `-0` と `+0` を区別する演算は別途定義する (`same-value?` 等)

#### `int` / `long` 型

##### 演算の挙動

- 四則演算は Java の `int`/`long` 演算と同様、2 の補数によるラップアラウンドオーバーフロー
- 整数ゼロ除算は `"error"` エフェクトを発生させる

##### 型昇格規則

異なる数値型が混在する算術演算では以下の順位で自動昇格する。

```
int  <  long  <  double
```

例: `(+ 1i 2l)` → `long`、`(+ 1i 2.0)` → `double`。実装上、各算術演算子は `int` 版・`long` 版・`double` 版を個別に持ち、オペランドの型に応じて選択する。

##### 数値変換プリミティブ

```
(int <number>)
```

任意の数値を 32 ビット符号付き整数に変換する。変換は Java の `(int)` キャストと同様。非数値が渡された場合は `"error"` エフェクトを発生させる。

```
(long <number>)
```

任意の数値を 64 ビット符号付き整数に変換する。変換は Java の `(long)` キャストと同様。

型変換まとめ:

| 変換元 \ 変換先 | `(int x)` | `(long x)` |
|----------------|-----------|------------|
| `double` | ゼロ方向切り捨て + Java `(int)` キャスト | ゼロ方向切り捨て + Java `(long)` キャスト |
| `int` | 恒等 | 符号拡張 |
| `long` | Java `(int)` キャスト (下位 32 ビット) | 恒等 |

---

## 3. 標準構文

標準構文は人がプログラムを記述しやすいよう、意図的に S 式に寄せた表記である。
Lisp ツールチェーン（エディタ等）の流用を可能にする。

### 3.1 リテラル

| JSON モデル | 標準構文 |
|-------------|----------|
| null        | `#null`  |
| true        | `#true`  |
| false       | `#false` |
| 数値 (double) | JSON と同様 (§2.1 参照) |
| 数値 (int)  | `<数値>i` — `(int <数値>)` に展開 (例: `42i`, `-5i`) |
| 数値 (long) | `<数値>l` — `(long <数値>)` に展開 (例: `42l`, `9999999999l`) |
| 文字列      | `"..."` (ダブルクオート) または裸の識別子 |

裸の識別子は Clojure のシンボル規則に準じた文字が使用できる。

| 種別 | 使用可能文字 |
|------|-------------|
| 先頭文字 | 英字 `[a-zA-Z]`、`_`、演算子文字 `+ - * / = < > ! ?` |
| 2 文字目以降 | 先頭文字に加えて数字 `[0-9]`、`.`、`'` |

ただし以下の曖昧性解消規則が先行する:
- `-` または `+` の直後が数字の場合は数値リテラルとして解釈する
- `#` で始まる字句 (`#null`, `#true`, `#false`) はリテラルとして予約
- `` ` `` `$` `@` `%` は標準構文の特殊文字として予約

整数サフィックス `i`/`l` はパーサーレベルの糖衣構文であり、それぞれ `(int <数値>)` および `(long <数値>)` への展開として定義される。`<数値>` 部は `double` リテラルとして解釈される。

### 3.2 配列

`[...]` または `(...)` を使える (どちらも同じ JSON モデル配列)。
要素の区切りは空白列またはカンマ (カンマは空白の一種として扱う)。

```
[1 2 3]
(1 2 3)
[1, 2, 3]   ; カンマも区切りとして使える
```

### 3.3 オブジェクト (Map)

`{key: value ...}` の形式。キーと値の区切りはコロン、エントリの区切りは空白/カンマ。

```
{a: 1, b: 2}
{a: 1 b: 2}
```

### 3.4 正しい JSON との関係

null/true/false を含まない正しい JSON は、そのまま正しい標準構文である。

---

## 4. セマンティクス

jalo は **Lisp1** である。関数と変数は同一のスコープに存在し、同じ評価規則で解決される。第 1 版では名前空間機構を持たず、グローバルな単一名前空間のみとする。

### 4.1 評価規則

| 式の種類 | 評価結果 |
|----------|----------|
| null / boolean / 数値 | 自分自身 |
| 文字列 | 現在のスコープでその名前に束縛された値 |
| オブジェクトのキー部分の文字列 | **評価されない** (文字列リテラル) |
| 配列 `[f a0 a1 ...]` | `f` を関数として `a0, a1, ...` に適用 |

### 4.2 スペシャルフォーム

#### `quote`
```
(quote <expr>)
```
`<expr>` を評価せず JSON モデルとして返す。

#### `if`
```
(if <cond> <then> <else>)
```
`<cond>` が truthy なら `<then>` を、そうでなければ `<else>` を評価して返す。

#### `let`
```
(let [<var1> <expr1>  <var2> <expr2> ...] <body>)
```
すべての `<exprN>` を `let` 導入前のスコープで評価し、その結果を並列に `<varN>` へ束縛してから `<body>` を評価する（Common Lisp の `let` と同じ並列束縛）。後続の束縛式から前の束縛を参照することはできない。

#### `let*`

スペシャルフォームではなく、マクロとして提供する。Common Lisp の `let*` と同等のシーケンシャル束縛（後の束縛式から前の束縛を参照可能）。

#### `letrec`
```
(letrec [<var1> <expr1>  <var2> <expr2> ...] <body>)
```
相互再帰的な定義のためのスペシャルフォーム。すべての `<varN>` が束縛された環境で各 `<exprN>` を評価する（Common Lisp の `letrec` 相当）。主に相互再帰関数の定義に使う。

#### `fn`
```
(fn [<param1> <param2> ...] <body>)
```
無名関数を生成する。

#### `def`
```
(def <name> <expr>)
```
トップレベルでの名前定義。

### 4.3 変数束縛と不変性

- 一度束縛された変数への再代入はできない
- 生成されたデータはすべて immutable
- immutable データ実装には **Paguro** ライブラリを使用

### 4.4 エフェクト機構

jalo のエラー・非局所脱出機構は**代数的エフェクト (軽量版)** として設計する。第 1 版は abort-only (継続なし) とし、将来版で継続付きへの拡張を検討する。

#### 基本プリミティブ

```
(raise <tag> <value>)
```
タグ `<tag>` と値 `<value>` を持つエフェクトを発生させる。最も近い対応ハンドラへ非局所脱出する。ハンドラが見つからない場合はプログラム全体のエラーとなる。

```
(handle <body>
  [<tag1> <var1> <expr1>]
  [<tag2> <var2> <expr2>]
  ...)
```
`<body>` を評価し、その中で発生したエフェクトをタグで振り分けてハンドルする。`<varN>` にエフェクトの値が束縛された状態で対応する `<exprN>` を評価し、その結果が `handle` 式全体の値となる。いずれのタグにも一致しないエフェクトは外側のハンドラへ伝播する。

`<tag>` は任意の値だが、慣習としてキーワード文字列 (例: `"error"`, `"break/out"`) を使う。

#### 組み込みエフェクト

| タグ | 意味 |
|---|---|
| `"error"` | 実行時エラー (型エラー・未束縛変数・ゼロ除算等) |

#### 便利関数

```
(error <value>)   ; = (raise "error" <value>)
```

#### jq 互換との対応

```
; jq: error("msg")
(error "msg")

; jq: try E catch H
(handle E
  ["error" msg H])

; jq: try E  (エラーを無視)
(handle E
  ["error" _ #null])

; jq: label $out | E | ... | break $out
(handle
  (let [out "break/out"] E)
  ["break/out" _ #null])
```

#### 第 1 版の制約

- abort-only: `raise` した時点でスタックは破棄され、ハンドラから中断地点に戻ることはできない。
- `handle` はスペシャルフォーム (ハンドラ節は遅延評価)。
- JVM 実装: スタックトレースを生成しない `Throwable` サブクラス `JaloSignal(tag, value)` を使ったタグ付き例外として実装する。

### 4.5 組み込み関数

組み込み関数はスペシャルフォームではなく通常の関数として評価される。

以下の表で **区分** は次を表す:
- **P** — JVM ホスト言語実装が必要なプリミティブ
- **L** — jalo で記述可能なライブラリ関数

#### 型述語

| 名前 | シグネチャ | 説明 | 区分 |
|------|-----------|------|------|
| `null?` | `(null? x)` | null か | P |
| `boolean?` | `(boolean? x)` | boolean か | P |
| `number?` | `(number? x)` | 任意の数値型か | P |
| `double?` | `(double? x)` | double か | P |
| `int?` | `(int? x)` | int か | P |
| `long?` | `(long? x)` | long か | P |
| `string?` | `(string? x)` | 文字列か | P |
| `array?` | `(array? x)` | 配列か | P |
| `map?` | `(map? x)` | マップか | P |
| `fn?` | `(fn? x)` | 関数か | P |
| `type` | `(type x)` | 型名を文字列で返す (`"null"` / `"boolean"` / `"double"` / `"int"` / `"long"` / `"string"` / `"array"` / `"map"` / `"fn"`) | P |

#### 算術

混合型演算の昇格順: `int` < `long` < `double` (§2.1 参照)。

| 名前 | シグネチャ | 説明 | 区分 |
|------|-----------|------|------|
| `+` | `(+ x y)` | 加算 | P |
| `-` | `(- x y)` / `(- x)` | 減算 / 符号反転 | P |
| `*` | `(* x y)` | 乗算 | P |
| `/` | `(/ x y)` | 除算 | P |
| `quot` | `(quot x y)` | 整数商 (ゼロ方向切り捨て) | P |
| `rem` | `(rem x y)` | 余り (符号は被除数に従う) | P |
| `mod` | `(mod x y)` | 剰余 (符号は除数に従う) | L |
| `double` | `(double x)` | 任意数値を double に変換 | P |
| `floor` | `(floor x)` | 床関数 | P |
| `ceil` | `(ceil x)` | 天井関数 | P |
| `round` | `(round x)` | 丸め (最近接偶数) | P |
| `trunc` | `(trunc x)` | ゼロ方向切り捨て | P |
| `abs` | `(abs x)` | 絶対値 | L |
| `max` | `(max x y)` | 最大値 | L |
| `min` | `(min x y)` | 最小値 | L |
| `pow` | `(pow x y)` | x の y 乗 | P |
| `sqrt` | `(sqrt x)` | 平方根 | P |
| `log` | `(log x)` | 自然対数 | P |
| `exp` | `(exp x)` | 指数関数 | P |
| `nan?` | `(nan? x)` | NaN か | P |
| `infinite?` | `(infinite? x)` | 無限大か | P |
| `pos?` | `(pos? x)` | 正か | L |
| `neg?` | `(neg? x)` | 負か | L |
| `zero?` | `(zero? x)` | 零か | L |

#### 比較

| 名前 | シグネチャ | 説明 | 区分 |
|------|-----------|------|------|
| `=` | `(= x y)` | 等値 (型を区別、NaN ≠ NaN) | P |
| `not=` | `(not= x y)` | 非等値 | L |
| `<` | `(< x y)` | 小なり | P |
| `<=` | `(<= x y)` | 以下 | L |
| `>` | `(> x y)` | 大なり | L |
| `>=` | `(>= x y)` | 以上 | L |
| `compare` | `(compare x y)` | 比較 (負/0/正を返す) | P |
| `same-value?` | `(same-value? x y)` | `-0` と `+0` を区別する等値 (§2.1 参照) | P |

#### 論理

| 名前 | シグネチャ | 説明 | 区分 |
|------|-----------|------|------|
| `not` | `(not x)` | 論理否定 | L |
| `and` | `(and x y ...)` | 短絡論理積 (マクロ) | L |
| `or` | `(or x y ...)` | 短絡論理和 (マクロ) | L |

#### 文字列

文字型はなく、長さ 1 の文字列を文字として代用する。

| 名前 | シグネチャ | 説明 | 区分 |
|------|-----------|------|------|
| `str` | `(str x ...)` | 値の文字列化・連結 | P |
| `str-count` | `(str-count s)` | 文字列長 (UTF-16 コード単位) | P |
| `str-get` | `(str-get s i)` | i 番目の文字 (長さ 1 文字列) | P |
| `subs` | `(subs s start)` / `(subs s start end)` | 部分文字列 | P |
| `str-index-of` | `(str-index-of s sub)` | 最初の出現位置 (非存在は -1) | P |
| `str-replace` | `(str-replace s from to)` | 最初のマッチを置換 | P |
| `str-replace-all` | `(str-replace-all s from to)` | 全マッチを置換 | P |
| `str-split` | `(str-split s delim)` | デリミタで分割 → 文字列配列 | P |
| `str-join` | `(str-join sep coll)` | 配列を sep で結合 | P |
| `str-trim` | `(str-trim s)` | 両端の空白を除去 | P |
| `str-upper` | `(str-upper s)` | 大文字化 | P |
| `str-lower` | `(str-lower s)` | 小文字化 | P |
| `str->number` | `(str->number s)` | 文字列を数値にパース (失敗は null) | P |
| `char-code` | `(char-code c)` | 長さ 1 文字列の Unicode コードポイント → long | P |
| `from-char-code` | `(from-char-code n)` | コードポイントから長さ 1 文字列を生成 | P |
| `chars` | `(chars s)` | 文字列を長さ 1 文字列の配列に分解 | L |
| `from-chars` | `(from-chars coll)` | 文字配列を文字列に結合 | L |
| `str-starts-with?` | `(str-starts-with? s prefix)` | 前方一致 | L |
| `str-ends-with?` | `(str-ends-with? s suffix)` | 後方一致 | L |
| `str-contains?` | `(str-contains? s sub)` | 部分文字列を含むか | L |

#### 配列 (Clojure ベクタに対応)

| 名前 | シグネチャ | 説明 | 区分 |
|------|-----------|------|------|
| `conj` | `(conj coll x)` | 末尾に追加した新配列 | P |
| `cons` | `(cons x coll)` | 先頭に追加した新配列 | P |
| `concat` | `(concat coll ...)` | 複数配列を連結 | P |
| `subvec` | `(subvec coll start)` / `(subvec coll start end)` | 部分配列 | P |
| `reverse` | `(reverse coll)` | 逆順 | P |
| `sort` | `(sort coll)` | 自然順ソート | P |
| `sort-by` | `(sort-by f coll)` | `f` の結果でソート | L |
| `map` | `(map f coll)` | 各要素に `f` を適用 | L |
| `filter` | `(filter pred coll)` | `pred` が真の要素を抽出 | L |
| `remove` | `(remove pred coll)` | `pred` が偽の要素を抽出 | L |
| `reduce` | `(reduce f init coll)` | 左畳み込み | L |
| `reduce-right` | `(reduce-right f init coll)` | 右畳み込み | L |
| `mapcat` | `(mapcat f coll)` | map + concat (flatMap) | L |
| `keep` | `(keep f coll)` | map し null を除去 | L |
| `take` | `(take n coll)` | 先頭 n 件 | L |
| `drop` | `(drop n coll)` | 先頭 n 件を除いた残り | L |
| `take-while` | `(take-while pred coll)` | `pred` が真の間、先頭から取得 | L |
| `drop-while` | `(drop-while pred coll)` | `pred` が真の間、先頭を除外 | L |
| `partition` | `(partition n coll)` | n 個ずつの部分配列に分割 | L |
| `partition-by` | `(partition-by f coll)` | `f` の値が変わる境界で分割 | L |
| `group-by` | `(group-by f coll)` | `f` の値でグループ化 → マップ | L |
| `frequencies` | `(frequencies coll)` | 各要素の出現回数 → マップ | L |
| `distinct` | `(distinct coll)` | 重複除去 (順序保持) | L |
| `flatten` | `(flatten coll)` / `(flatten depth coll)` | ネスト配列を平坦化 | L |
| `zip` | `(zip coll ...)` | 複数配列を `[x y ...]` ペア配列に | L |
| `range` | `(range end)` / `(range start end)` / `(range start end step)` | 数値範囲配列 | L |
| `some` | `(some pred coll)` | いずれかが真か | L |
| `every?` | `(every? pred coll)` | すべてが真か | L |
| `not-any?` | `(not-any? pred coll)` | どれも真でないか | L |
| `min-by` | `(min-by f coll)` | `f` が最小の要素 | L |
| `max-by` | `(max-by f coll)` | `f` が最大の要素 | L |
| `index-of` | `(index-of coll x)` | `x` の最初の位置 (非存在は -1) | L |

#### マップ (Clojure マップに対応)

| 名前 | シグネチャ | 説明 | 区分 |
|------|-----------|------|------|
| `keys` | `(keys m)` | キーの配列 | P |
| `vals` | `(vals m)` | 値の配列 | P |
| `entries` | `(entries m)` | `[k v]` ペア配列 (jq の `to_entries` 相当) | P |
| `from-entries` | `(from-entries coll)` | `[k v]` ペア配列からマップを構築 | L |
| `merge` | `(merge m ...)` | マップをマージ (後勝ち) | L |
| `merge-with` | `(merge-with f m ...)` | 衝突を `f` で解決するマージ | L |
| `select-keys` | `(select-keys m keys)` | 指定キーのみのサブマップ | L |
| `rename-keys` | `(rename-keys m rename-map)` | キーの付け替え | L |
| `map-keys` | `(map-keys f m)` | 全キーに `f` を適用 | L |
| `map-vals` | `(map-vals f m)` | 全値に `f` を適用 | L |

#### コレクション共通 (配列・マップ両対応)

| 名前 | シグネチャ | 説明 | 区分 |
|------|-----------|------|------|
| `count` | `(count coll)` | 要素数 / エントリ数 | P |
| `empty?` | `(empty? coll)` | 空か | L |
| `not-empty` | `(not-empty coll)` | 空なら null、そうでなければ `coll` | L |
| `nth` | `(nth coll i)` / `(nth coll i default)` | i 番目の要素 | P |
| `get` | `(get coll key)` / `(get coll key default)` | 配列: インデックス / マップ: キー で取得 | P |
| `assoc` | `(assoc coll key val ...)` | 配列: インデックス更新 / マップ: キー設定 | P |
| `dissoc` | `(dissoc m k ...)` | マップからキーを除去 | P |
| `contains?` | `(contains? coll key)` | 配列: 有効インデックスか / マップ: キーが存在するか | P |
| `get-in` | `(get-in coll path)` / `(get-in coll path default)` | キーパス (文字列/整数の配列) でネスト取得 | L |
| `assoc-in` | `(assoc-in coll path val)` | キーパスでネスト更新 | L |
| `update` | `(update coll key f ...)` | key の値に `f` を適用した新コレクション | L |
| `update-in` | `(update-in coll path f ...)` | キーパスでネスト更新 | L |
| `into` | `(into target src)` | `src` の要素を `target` に追加 | L |
| `first` | `(first coll)` | 最初の要素 | L |
| `second` | `(second coll)` | 2 番目の要素 | L |
| `last` | `(last coll)` | 最後の要素 | L |
| `rest` | `(rest coll)` | 先頭以外の配列 | L |

#### 高階関数

| 名前 | シグネチャ | 説明 | 区分 |
|------|-----------|------|------|
| `apply` | `(apply f args)` | 配列を引数として `f` を適用 | P |
| `comp` | `(comp f g ...)` | 関数合成 (右から左) | L |
| `partial` | `(partial f x ...)` | 部分適用 | L |
| `identity` | `(identity x)` | 恒等関数 | L |
| `constantly` | `(constantly x)` | 常に `x` を返す関数 | L |
| `complement` | `(complement f)` | `f` の論理否定を返す関数 | L |
| `juxt` | `(juxt f g ...)` | 各関数を同一引数に適用した結果配列を返す関数 | L |
| `memoize` | `(memoize f)` | `f` のメモ化版を返す (組み込みのみ; 下記注) | P |

`memoize` は JVM 内部の HashMap を用いた組み込み実装のみ提供する。jalo は純粋関数型言語であり可変状態をユーザーが直接作成する手段を持たないため、ユーザー定義のメモ化機構は提供しない。

#### JSON・I/O

| 名前 | シグネチャ | 説明 | 区分 |
|------|-----------|------|------|
| `to-json` | `(to-json x)` | jalo 値を JSON テキストに変換 | P |
| `from-json` | `(from-json s)` | JSON テキストを jalo 値にパース | P |
| `print` | `(print x)` | 標準出力に出力 | P |
| `println` | `(println x)` | 標準出力に出力 + 改行 | L |
| `read-line` | `(read-line)` | 標準入力から 1 行読み込み → 文字列 | P |

#### jq 互換ライブラリとの対応

以上のプリミティブを使えば jq の代表的な機能を jalo で実装できる:

| jq 機能 | jalo 実装の基盤 |
|---------|----------------|
| `to_entries` / `from_entries` / `with_entries` | `entries`, `from-entries`, `map` |
| `recurse` / `walk` | `map`, `map-vals`, `reduce` |
| `group_by` / `unique_by` / `min_by` / `max_by` | `group-by`, `distinct`, `min-by`, `max-by` |
| `explode` / `implode` | `char-code`, `from-char-code` + 配列 HOF |
| `@base64`, `@html`, `@csv` などのフォーマット | 文字列プリミティブ上でライブラリ実装 |

---

## 5. パターン (バッククオート相当)

パターンは JSON モデルの構築テンプレートであり、かつ JSON モデルのパターンマッチによる分解にも使える。  
値の構築・`match` の左辺どちらも **`(backquote <pattern>)`** を使う。標準構文では `` `<pattern> `` と書く。

### 5.1 パターンの文法と JSON モデル表現

パターンの定義を帰納的に示す。各パターン `p` に対して、その JSON モデル表現 `⟦p⟧` を併記する。

**リテラル**  
`#null`, `#true`, `#false`, 数値, 文字列はパターンであり、値そのものが表現。

| パターン | 表現 `⟦p⟧` |
|---|---|
| `#null` | `null` |
| `#true` | `true` |
| `42` | `42` |
| `"hello"` | `"hello"` |

**埋め込み式 `$e`**  
`e` が標準構文の式ならば `$e` はパターンであり、`⟦$e⟧ = (dollar ⟦e⟧)`。

**配列パターン `[e0 ... en]`**  
各 `ei` がパターン、または `@e` の形 (`e` は標準構文の式) のとき、`[e0 ... en]` はパターン。

```
⟦[e0 ... en]⟧ = (array ⟦e0⟧ ... ⟦en⟧)
⟦@e⟧          = (at ⟦e⟧)
```

`(at ⟦e⟧)` は必ず `(array ...)` の直接の要素として出現する。

**Map パターン `{e0 ... en}`**  
各 `ei` が `k:v` の形 (キー `k` は文字列リテラルまたは `$e`、値 `v` はパターン) か `%e` の形のとき、`{e0 ... en}` はパターン。

```
⟦{e0 ... en}⟧          = (map ⟦e0⟧ ... ⟦en⟧)
⟦k:v⟧  (k は文字列)    = ("k" ⟦v⟧)
⟦($e):v⟧               = ((dollar ⟦e⟧) ⟦v⟧)
⟦%e⟧                   = (percent ⟦e⟧)
```

`(percent ⟦e⟧)` は必ず `(map ...)` の直接の要素として出現する。

### 5.2 `backquote` の意味

`(backquote <pattern>)` はマクロとして定義される。

- **右辺 (値の構築)**: 展開後に評価すると、`(dollar ...)` の式を評価して埋め込んだ JSON モデルの値が得られる。
- **左辺 (`match` のパターン)**: `match` が `(backquote ...)` を認識し、内部の `(array ...)` / `(map ...)` / `(dollar ...)` 等の構造を静的に解釈して変数束縛を行う。

右辺の例:

```
(let [x 42  arr [1 2 3]]
  `[$x @arr "end"])
; => [42, 1, 2, 3, "end"]
```

```
(let [k "name"  extra {age: 30}]
  `{$k: "Alice"  %extra})
; => {"name": "Alice", "age": 30}
```

JSON モデル上での表現:

```json
["backquote", ["array", ["dollar","x"], ["at","arr"], "end"]]

["backquote", ["map", [["dollar","k"], "Alice"], ["percent","extra"]]]
```

### 5.3 左辺パターンの制約

`match` のパターン位置で `(backquote ...)` を使う場合、以下の追加制約が課される。

- `(dollar ...)` の後には **変数のみ** 記述できる (任意式は不可)。
- `(at <var>)` は同一 `(array ...)` 内に高々 1 つ。
- `(percent <var>)` は同一 `(map ...)` 内に高々 1 つ。
- `(map ...)` のキー部分 (`(キー 値)` ペアの第 1 要素) に `(dollar ...)` は使用不可 (静的キーのみ)。

**ワイルドカード `_`**  
変数名 `_` はワイルドカードとして予約される (Clojure スタイル)。`$_` はどんな値にもマッチするが、値を束縛しない。同一パターン内に複数の `$_` を書ける。`@_` / `%_` も同様にワイルドカードとして使用できる。

### 5.4 `match` 式

```
(match <expr0> <pattern1> <expr1> [<pattern2> <expr2>]...)
```

1. `<expr0>` を評価する
2. `<pattern1>` とマッチを試みる。成功すれば束縛変数を環境に追加して `<expr1>` を評価し返す
3. 失敗すれば後続のパターンを順に試みる
4. すべてのパターンに失敗した場合は `#null` を返す (意図的な設計)

パターン位置には `(backquote ...)` フォーム、またはリテラル値 (文字列・数値・`#true`・`#false`・`#null`) を直接書ける。後者は値の等値比較で照合する。

```
(match val
  `[$x @rest]                        (list x rest)
  `{name: $n  %_}                    n
  42                                 "forty-two")
```

非網羅を明示的にエラーにしたい場合は、末尾に全一致パターン `$var` を置いてエラーを発生させる慣用句を使う:

```
(match val
  `[$x @rest]  (list x rest)
  `$_          (error "unexpected value"))
```

### 5.5 パーサーの動作

標準構文パーサーは `` ` `` (バッククオート) を読んだ後、**パターン構文モード**に切り替わる。

- `[...]` → `(array ...)` (通常構文の配列リテラルとは別ノード)
- `{k: v ...}` → `(map ("k" ...) ...)` (通常構文の Map リテラルとは別ノード)
- `$e` → `(dollar ⟦e⟧)`
- `@e` → `(at ⟦e⟧)`
- `%e` → `(percent ⟦e⟧)`

パターン構文モード内の `e`/`k` 等、パターンではなく **式** を期待する位置 (`$e` の `e`、`@e` の `e` 等) では通常構文モードに戻る。

`(backquote ...)` / `` ` `` の外側に `$`/`@`/`%` が出現した場合はパースエラー。

---

## 6. jq 互換性

jalo は jq との互換性を付加価値として持つ。

- jq プログラムをパースして JSON モデル AST を生成し、jalo で評価すると同等の動作をする
- すべての jq 機能を網羅する必要はなく、代表的なフィルタ機能を優先する
- jq プリミティブは jalo ビルトインでなく、jalo で定義されたライブラリとして実装してよい
- jq 構文上の多義的記号 (例: `[]`) は、AST 上では別ノードとして区別する

---

## 7. 実装

### 7.1 フェーズ

1. **フェーズ 1**: ツリー歩行インタプリタ
2. **フェーズ 2**: JVM バイトコードコンパイラ (REPL 対応を維持)

### 7.2 データ構造

immutable データ構造の実装に **Paguro** ライブラリを使用する。

---

## 8. テスト戦略

- jq ベースのテストフレームワークを用意する
- テストケースは既存の jq 実装 (C 実装 および Jackson ベース実装) で妥当性を検証する
- jalo の jq 互換機能をそのテストケースで検証し、通過するケースを段階的に増やす
