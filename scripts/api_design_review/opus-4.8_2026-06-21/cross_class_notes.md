# Cross-class consistency notes (running log)

Jot observations the moment they appear; consolidate into SUMMARY at the end.

## Known structural facts
- BooleanMatrix uses boxed `Stream<Boolean>` for its element stream where every other primitive
  matrix uses a primitive stream (ByteStream, IntStream, ...). Watch for ripple inconsistencies
  (e.g. stream-returning methods that must box, or methods omitted in BooleanMatrix).
- The 8 primitive classes + Matrix<T> mirror each other; AbstractMatrix holds shared skeleton.

## Observations — VERIFIED against source (Wave 1)

1. **matmul is the consistent matrix-multiply name** across all 7 numeric primitives
   (ByteMatrix:2668, CharMatrix:2636, ShortMatrix:2623, IntMatrix:2848, LongMatrix:2739,
   FloatMatrix:2632, DoubleMatrix:2817). There is NO `multiply` method. BooleanMatrix has none
   (correct). => HEALTHY, not a finding. (NB: the Matrices review-agent's claim of `IntMatrix.multiply`
   was a factual error — discard it.)

2. **No whole-matrix reductions** (`sum`/`min`/`max`/`average`) exist on ANY matrix class — only via
   `streamH()/streamV()`. Verified by grep (only stream-internal `count()` inner classes). Potential
   missing-method FAMILY. Note overflow trap: `IntStream.sum()` returns `int`. Medium.

3. **Position-based `updateAll` boxes** in ALL 9 classes: `updateAll(IntBiFunction<? extends Wrapper>)`
   (Bool:1150, Byte:1256, Char:1226, Short:1213, Int:1373, Long:1266, Float:1178, Double:1293,
   Matrix:1134) vs the value-based `updateAll(<prim>UnaryOperator)` which is primitive. Uniform =>
   consistent, but a boxing+null-unbox hazard for primitive matrices. NB: a primitive `(int,int)->prim`
   FI mostly doesn't exist except `IntBinaryOperator` (Int only). Low-Med, family-wide.

4. **`from(...)` widening-factory coverage is asymmetric** (verified by grep):
   - IntMatrix.from: char(155), byte(210), short(264)  => COMPLETE for int's narrower sources
   - LongMatrix.from: int(155) only  => MISSING byte/short/char
   - FloatMatrix.from: int(169) only  => MISSING byte/short/char/long
   - DoubleMatrix.from: int(167), long(226), float(283)  => MISSING byte/short/char
   - ShortMatrix/ByteMatrix/CharMatrix/BooleanMatrix: NO `from` (only `of`). Short could add from(byte);
     byte is narrowest (none); char/boolean defensibly none.
   Real gap. Medium-High. Lead class (IntMatrix) sets the pattern; Long/Float/Double under-cover.

5. **`mapToObj(mapper, Class)` vs `map(mapper, Class)`**: primitives use `mapToObj` (IntMatrix:1584);
   Matrix<T> overloads the type-changing transform onto `map` (Matrix:1304). Vocabulary asymmetry. Med.

6. **Matrices.zip coverage gap** (verified by full grep of zip signatures): the primary `zip` family
   (binary, ternary, Collection-binary, Collection-NFunction x2) exists ONLY for Byte/Int/Long/Double
   + generic Matrix. MISSING for Short, Char, Float, Boolean. FloatMatrix is the most glaring
   (Double has full family, Float none). All primitive classes have instance `zipWith`, so the static
   mirrors are trivially addable. High.

7. **Matrices.zipToXxx widening-converter lattice asymmetric**: zipToInt(Byte:1574), zipToLong(Int:2138),
   zipToDouble(Int:2382), zipToDouble(Long:2909). Missing Short/Char source converters, Byte->Long/Double,
   Float->Double. Curated subset, but undocumented as intentional + asymmetric (Byte gets only ->Int,
   Int gets ->Long and ->Double). Medium.

8. **Diagonal accessors are `get`-prefixed** (`mainDiagonalCopy`/`antiDiagonalCopy`, AbstractMatrix:1638/1688)
   while row/column accessors are NOT (`rowCopy`/`columnCopy`, 403/433). Only `get`-prefixed array
   accessors in the family. Setters: setMainDiagonal/setRow. Naming inconsistency. Medium.

9. **isSameShape(M m)** (AbstractMatrix:900) accepts only the same self-type M (can't compare IntMatrix vs
   DoubleMatrix though shape is type-independent) AND throws IAE on null instead of returning false
   (surprising for a boolean isXxx predicate). Medium.

10. **Exception-type split** (verified IntMatrix): `get`/`set` use raw array access => AIOOBE (651);
    `value*`/ranged use `checkRowColumnIndex`/`checkFromToIndex` => IOOBE (valueAbove:755). This MATCHES
    the project's established validation-exception policy (get/set keep AIOOBE; index/range -> IOOBE).
    By-design; recommend documenting the deliberate split. Low.

11. **Matrix Dataset converters** (`toDataset`/`toTransposedDataset`, @Beta, Matrix:3690/3744) exist only on
    Matrix<T>. `toTransposedDataset`'s param is named `columnNames` but validated against `rowCount` (3747) —
    each matrix ROW becomes a Dataset COLUMN. Documented but mildly confusing. Low.

12. `forEachIndices` has a `(i,j,matrix)` variant (AbstractMatrix:1360/1415) that passes `(M)this` — marginal
    value vs closure capture. Low (keep).

## Observations — VERIFIED against source (Wave 2)

13. **`toXxxMatrix()` conversion convention is CLEAN & CONSISTENT** (verified by grep of all `to*Matrix()`):
    every numeric matrix exposes a converter to each of {int, long, float, double} EXCEPT its own type —
    Byte/Char/Short → int,long,float,double (4 each); Int → long,float,double; Long → int,float,double;
    Float → int,long,double; Double → int,long,float. No `toByteMatrix`/`toShortMatrix`/`toCharMatrix`
    exists anywhere. Some are narrowing (Long.toInt, Double.toInt/toLong/toFloat, Float.toInt/toLong) and
    are documented as such. => HEALTHY.
    **CORRECTION: REJECT the ByteMatrix agent's "missing toShortMatrix" (it claimed High).** byte->short is a
    widening edge but `short` is not a conversion TARGET anywhere; ByteMatrix is consistent with the family
    convention. Mark in ByteMatrix report as "considered & rejected — consistent."

14. **`mapToXxx` primitive-mapper family is INCONSISTENT** (verified by grep):
    - Matrix<T>: mapToInt, mapToLong, mapToFloat, mapToDouble (ALL 4) — the only complete one.
    - IntMatrix: mapToLong, mapToDouble (2)   |  LongMatrix: mapToInt, mapToDouble (2)
    - DoubleMatrix: mapToInt, mapToLong (2)   |  FloatMatrix: NONE (0)  <= OUTLIER among main numeric types
    - ByteMatrix / ShortMatrix / CharMatrix: NONE (0) — consistent among the narrow tier.
    Two real findings:
      (a) **FloatMatrix has zero primitive mapTo** while its peers Int/Long/Double each have two — clear
          outlier; should gain mapToInt/mapToLong/mapToDouble. High.
      (b) **mapToFloat is missing on EVERY primitive matrix** (Int/Long/Double/Float/Byte/Short/Char) even
          though `toFloatMatrix()` exists on all of them and Matrix<T> has mapToFloat. Breaks the
          "convert-to-X implies map-to-X" pairing specifically for float. Medium.
      (c) Byte/Short/Char have no mapToXxx at all (consistent tier) but DO have toIntMatrix/etc.; adding
          mapToInt/mapToLong/mapToDouble would pair with their converters. Low-Med, design decision.

15. **`from(...)` widening-factory family is INCONSISTENT** (verified):
    - IntMatrix.from: char, byte, short (all narrower integral sources) — most complete.
    - LongMatrix.from: int ONLY (missing byte/short/char).
    - FloatMatrix.from: int ONLY (missing long — Double has it — plus byte/short/char).
    - DoubleMatrix.from: int, long, float (missing byte/short/char).
    - ShortMatrix.from: NONE (byte->short is widening; missing from(byte)).
    - ByteMatrix/CharMatrix/BooleanMatrix.from: NONE (defensible — byte narrowest; char/boolean no clean source).
    Needs a policy decision: which source types may `from` accept? Then apply uniformly. Concrete highest-value
    gaps: LongMatrix.from(byte/short/char); FloatMatrix.from(long) (+byte/short/char); DoubleMatrix.from(byte/short/char);
    ShortMatrix.from(byte). Medium-High.

16. **BooleanMatrix missing `not()`** (verified: has and@2437/or@2472/xor@2507, NO not/negate). The unary
    complement is the only one of the 4 core boolean ops users must hand-roll via map(b->!b). High.
    Minor symmetry extras (noneTrue/allFalse/anyFalse) Low.

17. **Matrices static `zip` gap** ties to FloatMatrix/ShortMatrix/CharMatrix: those classes have instance
    `zipWith` but no static `Matrices.zip`. FloatMatrix is the clearest (Double has full family). BooleanMatrix
    static-zip absence is defensible (and/or/xor + zipWith cover it).

18. **CharMatrix** is essentially clean (no actionable findings beyond the shared family-level gaps above:
    from(), mapToXxx, static zip). Arithmetic on char is unusual but documented & consistent across the integral
    tier. Report as a short stub + cross-refs.

19. **char/byte/short arithmetic (add/subtract/matmul) present across integral tier** with documented wrap
    semantics — consistent. Not a finding (though "arithmetic on char" is conceptually odd, it's uniform & documented).
