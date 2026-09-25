package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.TestBase;

class PrimitiveExceptionReviewTest extends TestBase {

    @Test
    void copyFactoriesPreserveValidationFailureOrderAndMessages() {
        final boolean[][] booleans = { { true }, { false, true }, null };
        final byte[][] bytes = { { 1 }, { 2, 3 }, null };
        final char[][] chars = { { 'a' }, { 'b', 'c' }, null };
        final short[][] shorts = { { 1 }, { 2, 3 }, null };

        assertSameArgumentFailure(() -> BooleanMatrix.wrap(booleans), () -> BooleanMatrix.copyOf(booleans));
        assertSameArgumentFailure(() -> ByteMatrix.wrap(bytes), () -> ByteMatrix.copyOf(bytes));
        assertSameArgumentFailure(() -> CharMatrix.wrap(chars), () -> CharMatrix.copyOf(chars));
        assertSameArgumentFailure(() -> ShortMatrix.wrap(shorts), () -> ShortMatrix.copyOf(shorts));

        assertSameArgumentFailure(() -> BooleanMatrix.wrap((boolean[]) null), () -> BooleanMatrix.copyOf((boolean[]) null));
        assertSameArgumentFailure(() -> ByteMatrix.wrap((byte[]) null), () -> ByteMatrix.copyOf((byte[]) null));
        assertSameArgumentFailure(() -> CharMatrix.wrap((char[]) null), () -> CharMatrix.copyOf((char[]) null));
        assertSameArgumentFailure(() -> ShortMatrix.wrap((short[]) null), () -> ShortMatrix.copyOf((short[]) null));
    }

    @Test
    void copyFactoriesStillAcceptRepeatedRowsAndOwnIndependentCopies() {
        final boolean[] booleans = { true };
        final byte[] bytes = { 1 };
        final char[] chars = { 'a' };
        final short[] shorts = { 1 };
        final BooleanMatrix booleanCopy = BooleanMatrix.copyOf(booleans, booleans);
        final ByteMatrix byteCopy = ByteMatrix.copyOf(bytes, bytes);
        final CharMatrix charCopy = CharMatrix.copyOf(chars, chars);
        final ShortMatrix shortCopy = ShortMatrix.copyOf(shorts, shorts);

        assertNotSame(booleans, booleanCopy.rowView(0));
        assertNotSame(booleanCopy.rowView(0), booleanCopy.rowView(1));
        assertNotSame(bytes, byteCopy.rowView(0));
        assertNotSame(byteCopy.rowView(0), byteCopy.rowView(1));
        assertNotSame(chars, charCopy.rowView(0));
        assertNotSame(charCopy.rowView(0), charCopy.rowView(1));
        assertNotSame(shorts, shortCopy.rowView(0));
        assertNotSame(shortCopy.rowView(0), shortCopy.rowView(1));
    }

    @Test
    void suppliedRandomGeneratorFailuresPropagateOnlyWhenGeneratingElements() {
        final IllegalStateException failure = new IllegalStateException("random source unavailable");
        final RandomGenerator generator = () -> {
            throw failure;
        };

        assertEquals(3, BooleanMatrix.random(0, 3, generator).columnCount());
        assertEquals(3, ByteMatrix.random(0, 3, generator).columnCount());
        assertEquals(3, CharMatrix.random(0, 3, generator).columnCount());
        assertEquals(3, ShortMatrix.random(0, 3, generator).columnCount());
        assertSame(failure, assertThrows(IllegalStateException.class, () -> BooleanMatrix.randomRow(1, generator)));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> ByteMatrix.randomRow(1, generator)));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> CharMatrix.randomRow(1, generator)));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> ShortMatrix.randomRow(1, generator)));

        assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.random(0, 3, null));
        assertThrows(IllegalArgumentException.class, () -> ByteMatrix.random(0, 3, null));
        assertThrows(IllegalArgumentException.class, () -> CharMatrix.random(0, 3, null));
        assertThrows(IllegalArgumentException.class, () -> ShortMatrix.random(0, 3, null));
    }

    @Test
    void objectMappingRejectsVoidEvenWhenThereAreNoCells() {
        assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.empty().mapToObj(value -> null, void.class));
        assertThrows(IllegalArgumentException.class, () -> ByteMatrix.empty().mapToObj(value -> null, void.class));
        assertThrows(IllegalArgumentException.class, () -> CharMatrix.empty().mapToObj(value -> null, void.class));
        assertThrows(IllegalArgumentException.class, () -> ShortMatrix.empty().mapToObj(value -> null, void.class));
    }

    @Test
    void characterRenderingValidatesInputsBeforeConversionAndOutput() {
        final CharMatrix matrix = CharMatrix.empty();
        assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(null, "not a character"));
        assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(new StringBuilder(), null));
        assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(null, 'a'));
        assertThrows(ClassCastException.class, () -> matrix.appendElementForOutput(new StringBuilder(), "not a character"));

        final IOException failure = new IOException("destination unavailable");
        final Appendable output = new Appendable() {
            @Override
            public Appendable append(final CharSequence value) throws IOException {
                throw failure;
            }

            @Override
            public Appendable append(final CharSequence value, final int start, final int end) throws IOException {
                throw failure;
            }

            @Override
            public Appendable append(final char value) throws IOException {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(IOException.class, () -> matrix.appendElementForOutput(output, 'a')));
    }

    private static void assertSameArgumentFailure(final Executable constructor, final Executable copy) {
        final IllegalArgumentException expected = assertThrows(IllegalArgumentException.class, constructor);
        final IllegalArgumentException actual = assertThrows(IllegalArgumentException.class, copy);
        assertEquals(expected.getMessage(), actual.getMessage());
    }
}
