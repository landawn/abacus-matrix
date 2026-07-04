# Line-by-line review — `Matrices.java`

File: `src/main/java/com/landawn/abacus/matrix/Matrices.java` (3988 lines)
Reviewer pass date: 2026-07-04
Scope: full read, lines 1–3988. Cross-checked against `AbstractMatrix` (fields `rowCount`/`columnCount`/`elementCount`/`elementType`/`a`, `checkRepresentableShape`, `MSG_NEGATIVE_DIMENSION`).

## Executive summary

This file is in excellent shape, fully consistent with the 12 prior clean review passes.
I found **no bugs** — no off-by-one, null-handling, boundary/empty/negative, concurrency
(the matmul parallelization correctly never parallelizes over `k`), overflow (`saturatedMultiply`
is correct incl. the `Long.MIN_VALUE * -1` special case), row/column-major, or copy/paste
type/dimension errors. All ~40 worked doc examples were arithmetic-verified and are correct.

The only findings are **minor Naming/Consistency/Design observations**, all Low severity, and
several are plausibly intentional. They are listed for completeness, not because the file is
defective.

---

## 1. BUGS

**None.**

Notes on things specifically checked and found correct:
- `saturatedMultiply` (255–278): overflow via divide-back is guarded by the explicit
  `Long.MIN_VALUE`/`-1` special case; the sole call site (970) only ever passes non-negative
  operands, so the sign-based saturation branch is defensive-only. Correct.
- `forEachCartesianIndices` parallel branch (1030–1074): parallelizes only over `i`
  (output rows) or `j` (output columns), never over `k`, so concurrent non-atomic `+=` into the
  same output cell cannot happen. The sequential branch (1076–1131) exhaustively covers the three
  "smallest dimension" cases with correct smallest-outermost loop ordering. Correct and matches
  the class doc (983–992).
- Collection-fold zips (e.g. 1387–1396, 1952–1961, 3679–3688): each `(i,j)` writes only
  `result[i][j]`, so the `forEachIndices`-driven parallelization is race-free. Correct.
- All widening variants use the right result-array primitive type and the right backing-field
  types (`byte[][]`→`int[][]`, `int[][]`→`long[][]`/`double[][]`, `long[][]`→`double[][]`).
  Spot-checked every overload; no wrong-type/wrong-dimension copy/paste.
- `newMatrixArray` (448–461) doc matches `AbstractMatrix.checkRepresentableShape` exactly
  (`(0,0)` allowed, `(0,3)` rejected, negatives rejected → IAE).
- Null-return `@throws NullPointerException` is documented on exactly the primitive-widening
  zips (which auto-unbox the boxed return) and omitted from `zipToObj` (which stores the value,
  incl. `null`, into an object array). Consistent and correct.

---

## 2. JAVADOC / COMMENTS / LOGS

### 2.1 (Low) Validation error messages hard-code "matrices" but the public zip params are named `coll`
- Lines: helper `checkMatricesNotEmptyAndNoNullElements` (3977–3987, esp. `N.checkArgNotEmpty(matrices, "matrices")` at 3978 and `"matrices[" + idx + "] is null"` at 3983) and `checkShapeForZip(Collection)` (3961–3975, message `"...matrices[0] is {}x{} but matrices[{}] is {}x{}"` at 3971).
- Why: every public `zip*(Collection ...)` overload names its parameter `coll` and documents it as `@param coll` (e.g. 1360, 1571, 1934, 2126, 3651). When such a call fails on an empty collection or a `null` element, the runtime message refers to `matrices`/`matrices[idx]`, which does not correspond to any parameter the caller named. (By contrast `stackVertically`/`stackHorizontally` name their parameter `matrices` (1166, 1215), so for those the message matches.)
- Impact: purely cosmetic; "matrices" is still a reasonable description of the contents. No behavioral effect.
- Suggested fix: either rename the collection parameter of the zip overloads from `coll` to `matrices` (matches the stack methods and the helper messages, and BC is not a constraint), or thread the caller's label into the helper messages.

### 2.2 (Low) `@link` to `resolveCommonElementType(Matrix[])` from a public method targets a `protected` member
- Line: 3623 — `{@link #resolveCommonElementType(Matrix[])}` inside the public `zip(Collection, BinaryOperator)` Javadoc; target method is `protected` (3901).
- Why: harmless in same-class Javadoc, but if public-only Javadoc is ever generated the link may dangle. Tied to finding 3.3 (the `protected` modifier itself). No action needed if all-members Javadoc is generated.

---

## 3. NAMING / CONSISTENCY / DESIGN

### 3.1 (Low) N-ary collection→object zip is named `zipToObj` for primitives but `zip` for the generic `Matrix`
- Lines: primitive N-ary "array function → `Matrix<R>`" methods are named `zipToObj`
  (Byte 1706/1763, Int 2491/2550, Long 3020/3079, Double 3311/3367); the analogous generic
  methods are named `zip` (3738 `zip(Collection<Matrix<T>>, Function<T[],R>, Class)` and
  3800 the `boolean`-sharing overload).
- Why: a user who learned `Matrices.zipToObj(Collection<IntMatrix>, ...)` will not find a
  `zipToObj(Collection<Matrix<T>>, ...)` — the same operation is spelled `zip` for the generic
  family.
- Counter-argument (why it may be intentional): the `toObj`/`toInt`/`toLong`/`toDouble` suffix
  signals a change of *matrix family* (primitive → object, or one primitive → another). A generic
  `Matrix` zip never changes family (result is always an object `Matrix`), so no suffix is the
  principled choice, and all generic zips uniformly use `zip` + an optional `Class` target.
- Suggested action: leave as-is if the "suffix = family change" convention is deliberate;
  otherwise rename the two generic overloads to `zipToObj` for cross-family symmetry.

### 3.2 (Low) `forEachCartesianIndices` restricts the action to `RuntimeException`, while `forEachIndices`/`mapIndices` accept a generic checked `E`
- Lines: `forEachCartesianIndices` uses `Throwables.IntTriConsumer<RuntimeException>` (961, 1016),
  whereas `forEachIndices` uses `Throwables.IntBiConsumer<E>` and declares `throws E` (556, 609).
- Why: an asymmetry in the traversal-helper family — callers of `forEachCartesianIndices` cannot
  let their accumulator throw a checked exception, but callers of `forEachIndices` can. Since
  `forEachIndices` also runs in parallel and successfully propagates `E`, the restriction does not
  appear to be forced by the parallel-stream machinery.
- Counter-argument: matmul accumulation (`C[i][j] += A[i][k]*B[k][j]`) is pure arithmetic and has
  no need to throw checked exceptions, so the tighter bound is arguably a deliberate simplification.
- Suggested action: for family symmetry, consider widening to `<E extends Exception>` +
  `throws E`; otherwise leave as intentional.

### 3.3 (Low) `protected` modifier on helpers of a `final` class
- Lines: `resolveCommonAssignableType` (3842) and `resolveCommonElementType` (3901) are
  `protected static` inside `public final class Matrices` (56).
- Why: `protected` in a `final` (non-subclassable) class grants nothing beyond package-private
  access, so the modifier is misleading. Both are used only inside this class/package (3676, 3813,
  3905).
- Suggested fix: change to package-private (drop `protected`), or `private` if package access is
  not actually required.

### 3.4 (Low) Widening-zip coverage is asymmetric across primitive families
- `ByteMatrix` offers only `zipToInt` (1439/1503/1571/1630) — no `zipToLong`/`zipToDouble` —
  whereas `IntMatrix` offers both `zipToLong` (2003…) and `zipToDouble` (2247…).
- Why: `byte` can widen to `long`/`double` just as `int` can, so a user summing many byte matrices
  into a `long` accumulator has no direct `zipToLong` helper (must go through `zipToInt` or
  `zipToObj`).
- Counter-argument: `byte→int` already covers the common overflow case; deeper widening is niche.
  Likely intentional. Listed only as an observation.

### 3.5 (Low) No zip helpers exist for `FloatMatrix`, `ShortMatrix`, `CharMatrix`, `BooleanMatrix`
- Confirmed by search: `Matrices` provides static zip helpers for `Byte`/`Int`/`Long`/`Double`
  and the generic `Matrix` only. The class Javadoc (49–50) accurately lists exactly these types,
  so this is **not** a doc discrepancy.
- Why noted: it is a genuine surface asymmetry versus the eight primitive matrix classes that all
  ship their own `zipWith`. For `Boolean`/`Char` there is no natural element-wise arithmetic, and
  `Float`/`Short` are less common, so the omission is very likely deliberate. Observation only.

### 3.6 (Low) Cosmetic `throws`-clause style inconsistency among sibling zips
- The delegating binary/ternary zips declare only `throws E` (e.g. `zip(ByteMatrix,ByteMatrix,…)`
  1269; `zip(IntMatrix,IntMatrix,…)` 1831; `zip(Matrix,Matrix,BiFunction)` 3439), while the
  self-implementing widening zips additionally declare `throws IllegalArgumentException`
  (e.g. `zipToInt(ByteMatrix,ByteMatrix,…)` 1439). All of them throw `IllegalArgumentException`
  at runtime (null + shape checks), and `IAE` is unchecked, so the difference is purely
  documentary. Low priority; harmonize the `throws` clauses if a consistent signature style is
  desired.

---

## Category / severity tally

| Category | High | Medium | Low |
|---|---|---|---|
| Bug | 0 | 0 | 0 |
| Javadoc/Comment/Log | 0 | 0 | 2 |
| Naming/Consistency/Design | 0 | 0 | 6 |

No High or Medium findings. No code change is required for correctness.
