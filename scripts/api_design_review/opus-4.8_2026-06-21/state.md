# API Design Review — State

- **MODEL**: opus-4.8
- **DATE**: 2026-06-21
- **EXECUTION**: PARALLEL (one sub-agent per class/batch; orchestrator verifies every finding against source before it enters a report)
- **Scope**: PUBLIC API of all classes in `com.landawn.abacus.matrix` under `./src/main/java`

## In-scope classes (12)

| Class | Lines | Kind | Status |
|-------|-------|------|--------|
| ParallelMode | 70 | enum | DONE (reviewed inline — clean, see note) |
| AbstractMatrix | 2429 | abstract sealed base | todo |
| Matrix\<T\> | 3905 | generic object matrix | todo |
| BooleanMatrix | 3863 | primitive matrix | todo |
| ByteMatrix | 3899 | primitive matrix | todo |
| ShortMatrix | 3836 | primitive matrix | todo |
| CharMatrix | 3838 | primitive matrix | todo |
| IntMatrix | 3989 | primitive matrix | todo |
| LongMatrix | 3928 | primitive matrix | todo |
| FloatMatrix | 3829 | primitive matrix | todo |
| DoubleMatrix | 4038 | primitive matrix | todo |
| Matrices | 3989 | static utility/factory | todo |

All primitive/Matrix files are > 2000 lines → MUST be read in line-range SEGMENTS by reviewing agents.

## Relationship map

- `AbstractMatrix<A, PL, ES, RS, M extends AbstractMatrix<...>>` — abstract **sealed** base.
  Type params: A=backing array type, PL=primitive/element List type, ES=element Stream type,
  RS=row Stream type, M=self type (CRTP).
- 8 primitive subclasses (each `final`, extends AbstractMatrix with concrete type args):
  - BooleanMatrix → `boolean[], BooleanList, Stream<Boolean>, Stream<Stream<Boolean>>`  ⚠ uses boxed `Stream<Boolean>` (no BooleanStream exists)
  - ByteMatrix   → `byte[], ByteList, ByteStream, Stream<ByteStream>`
  - ShortMatrix  → `short[], ShortList, ShortStream, Stream<ShortStream>`
  - CharMatrix   → `char[], CharList, CharStream, Stream<CharStream>`
  - IntMatrix    → `int[], IntList, IntStream, Stream<IntStream>`
  - LongMatrix   → `long[], LongList, LongStream, Stream<LongStream>`
  - FloatMatrix  → `float[], FloatList, FloatStream, Stream<FloatStream>`
  - DoubleMatrix → `double[], DoubleList, DoubleStream, Stream<DoubleStream>`
- `Matrix<T>` → `T[], List<T>, Stream<T>, Stream<Stream<T>>` — generic object matrix (also a subclass).
- `Matrices` — `final` class, **static** factory + cross-matrix ops (zip, multiply, etc.) + ThreadLocal `ParallelMode` control. Delegates per-type work to the matrix classes.
- `ParallelMode` — enum {FORCE_ON, FORCE_OFF, AUTO}; consumed by Matrices.

**Mirroring rule for GROUND RULE 2:** the 8 primitive classes + Matrix\<T\> are intentional API mirrors
of each other and all inherit the AbstractMatrix template-method skeleton. A method appearing in
several of them via mirroring/inheritance is NOT a duplicate. Divergence between them IS a finding.

## ParallelMode note (DONE)
Enum of 3 values, fully documented, consistent. No actionable API findings.

## Execution plan
- Wave 1 (lead/pattern-setting): AbstractMatrix, Matrix, Matrices, IntMatrix.
- Wave 2 (remaining primitives, instructed to check divergence vs IntMatrix + own unique methods):
  BooleanMatrix, ByteMatrix, ShortMatrix, CharMatrix, LongMatrix, FloatMatrix, DoubleMatrix.
- Each finding verified against source by orchestrator before entering a report.

## Wave status
- Wave 1 (AbstractMatrix, Matrix, Matrices, IntMatrix): DONE — agents returned, findings VERIFIED against source.
- Wave 2 (Boolean, Byte, Short, Char, Long, Float, Double): DONE — agents returned, findings VERIFIED.
  Notable verification outcomes: corrected agent claim of `IntMatrix.multiply` (it's `matmul`); rejected
  ByteMatrix "missing toShortMatrix" (conversion convention is to-{int,long,float,double}-except-self);
  confirmed mapToXxx inconsistency (Float outlier; mapToFloat missing family-wide) and from() inconsistency.
- Per-class reports: writing now.
- SUMMARY: todo

## Per-class report status — ALL DONE
- ParallelMode: DONE (stub — clean)
- AbstractMatrix: DONE
- Matrix: DONE
- Matrices: DONE
- IntMatrix: DONE
- LongMatrix: DONE
- FloatMatrix: DONE
- DoubleMatrix: DONE
- ByteMatrix: DONE
- ShortMatrix: DONE
- CharMatrix: DONE
- BooleanMatrix: DONE
- SUMMARY: DONE

REVIEW COMPLETE. All findings verified against source by the orchestrator before entering reports.
