# JQ_COMPAT_STATUS.md — jq Compatibility Status (cmd_414)

> For detailed conversion rules, see [docs/SPEC.md §6](SPEC.md#6-jq-互換性).

## Compatibility Matrix (Q7: 25 samples) — 10/25 fully compatible, 4 partial

| ID | Category | jq feature | PR | Status | Notes |
|---|---|---|---|---|---|
| T-001 | basic_path | `.` | PR-A | ✅ | |
| T-002 | basic_path | `.foo` | PR-A | ✅ | |
| T-003 | basic_path | `.foo.bar` | PR-A | 🟡 | single-key lookup; path traversal (.foo.bar) not implemented as nested path |
| T-004 | basic_path | `.foo?` | PR-A | 🟡 | ? suffix treated as literal key name, not optional-operator |
| T-005 | array_access | `.[0]` | PR-A | ✅ | |
| T-006 | array_access | `.[2:4]` | PR-A | 📋 | |
| T-007 | array_access | `.[]` | PR-A | 🟡 | .[] returns identity; array/object iteration not implemented |
| T-008 | array_access | `.foo[]` | PR-A | 🟡 | .foo[] treated as literal key 'foo[]', not iterate-over-field |
| T-009 | pipe | `.foo \| .bar` | PR-A | ✅ | |
| T-010 | construction | `[.foo, .bar]` | PR-A | 📋 | |
| T-011 | construction | `{name: .foo, age: .bar}` | PR-A | 📋 | |
| T-012 | construction | `[.[]]` | PR-A | ✅ | |
| T-013 | hof | `map(.foo)` | PR-A | ✅ | |
| T-014 | hof | `select(.age > 30)` | PR-A | ✅ | |
| T-015 | hof | `map(. * 2)` | PR-A | 📋 | |
| T-016 | hof | `length` | PR-A | ✅ | |
| T-017 | aggregate | `keys` | PR-B | 📋 | |
| T-018 | aggregate | `values` | PR-B | 📋 | |
| T-019 | aggregate | `to_entries` | PR-B | 📋 | kebab-case builtin (to-entries 等) は jalo 標準構文で呼出可 |
| T-020 | aggregate | `from_entries` | PR-B | 📋 | kebab-case builtin (from-entries 等) は jalo 標準構文で呼出可 |
| T-021 | aggregate | `group_by(.type)` | PR-B | 📋 | kebab-case builtin (group-by 等) は jalo 標準構文で呼出可 |
| T-022 | set_operations | `unique` | PR-B | 📋 | kebab-case builtin (unique 等) は jalo 標準構文で呼出可 |
| T-023 | set_operations | `sort_by(.foo)` | PR-B | 📋 | kebab-case builtin (sort-by 等) は jalo 標準構文で呼出可 |
| T-024 | formats | `@csv` | PR-C | ✅ | |
| T-025 | formats | `@base64` | PR-C | ✅ | |

## PR-C updates

- Added CLI jq mode:
  - `jalo -j '<filter>' [<json-file>]`
  - `-c` compact JSON output
  - `-n` / `--null-input`
  - `.jq` extension auto-detect mode
- Updated status for PR-C scope items (`@csv`, `@base64`)

## Explicitly unsupported / pending

- `try ... catch` (❌)
- jq generators (❌)
- `def f(x): ...` (❌)
- `$ENV` (❌ in this PR-B)
- `$__loc__` (❌ in this PR-B)
