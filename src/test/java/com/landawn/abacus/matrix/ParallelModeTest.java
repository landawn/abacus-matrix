/*
 * Copyright (C) 2024 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ParallelModeTest extends MatrixTestSupport {

    private static Stream<Arguments> modePairs() {
        return Arrays.stream(ParallelMode.values()).flatMap(left -> Arrays.stream(ParallelMode.values()).map(right -> Arguments.of(left, right)));
    }

    @Nested
    class ValuesTests {

        @Test
        public void testValues_DeclarationOrderAndIndependentArrays() {
            final ParallelMode[] expected = { ParallelMode.FORCE_ON, ParallelMode.FORCE_OFF, ParallelMode.AUTO };
            final ParallelMode[] first = ParallelMode.values();
            final ParallelMode[] second = ParallelMode.values();
            assertArrayEquals(expected, first);
            assertArrayEquals(expected, second);
            assertNotSame(first, second);
            first[0] = null;
            assertArrayEquals(expected, ParallelMode.values());
        }

        @Test
        public void testCollections_KeepAllThreeDistinctPolicies() {
            final var modes = Arrays.asList(ParallelMode.values());
            assertEquals(3, modes.size());
            assertEquals(EnumSet.allOf(ParallelMode.class), new HashSet<>(modes));
            assertEquals(3, EnumSet.allOf(ParallelMode.class).size());
            for (Map<ParallelMode, Integer> map : Arrays.<Map<ParallelMode, Integer>> asList(new HashMap<>(), new EnumMap<>(ParallelMode.class))) {
                map.put(ParallelMode.FORCE_ON, 1);
                map.put(ParallelMode.FORCE_OFF, 0);
                map.put(ParallelMode.AUTO, -1);
                assertEquals(3, map.size());
                assertEquals(1, map.get(ParallelMode.FORCE_ON));
                assertEquals(0, map.get(ParallelMode.FORCE_OFF));
                assertEquals(-1, map.get(ParallelMode.AUTO));
            }
        }
    }

    @Nested
    class ValueOfTests {

        @ParameterizedTest(name = "{0} has ordinal {1}")
        @CsvSource({ "FORCE_ON, 0", "FORCE_OFF, 1", "AUTO, 2" })
        public void testValueOf_NameOrdinalAndIdentity(final String name, final int ordinal) {
            final ParallelMode mode = ParallelMode.valueOf(name);
            assertEquals(name, mode.name());
            assertEquals(name, mode.toString());
            assertEquals(ordinal, mode.ordinal());
            assertSame(ParallelMode.values()[ordinal], mode);
            assertSame(ParallelMode.class, mode.getDeclaringClass());
            assertEquals(mode.hashCode(), ParallelMode.valueOf(name).hashCode());
        }

        @Test
        public void testValueOf_NullName() {
            assertThrows(NullPointerException.class, () -> ParallelMode.valueOf(null));
        }

        @ParameterizedTest(name = "rejects [{0}]")
        @EmptySource
        @ValueSource(strings = { "yes", "no", "default", "Yes", "No", "Default", "INVALID", " FORCE_ON ", "force_on", "force_off", "auto" })
        public void testValueOf_InvalidNames(final String name) {
            assertThrows(IllegalArgumentException.class, () -> ParallelMode.valueOf(name));
        }
    }

    @Nested
    class EqualsTests {

        @ParameterizedTest(name = "{0} compared with {1}")
        @MethodSource("com.landawn.abacus.matrix.ParallelModeTest#modePairs")
        public void testEqualsAndCompareTo_AllPairs(final ParallelMode left, final ParallelMode right) {
            assertEquals(left.ordinal() - right.ordinal(), left.compareTo(right));
            assertEquals(-right.compareTo(left), left.compareTo(right));
            assertEquals(left == right, left.equals(right));
            assertEquals(right.equals(left), left.equals(right));
            assertFalse(left.equals(null));
            assertFalse(left.equals(left.name()));
        }
    }

    @Nested
    class ContractsTests {

        @ParameterizedTest
        @EnumSource(ParallelMode.class)
        public void testSerialization_RestoresCanonicalInstance(final ParallelMode mode) throws Exception {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
                output.writeObject(mode);
            }
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                assertSame(mode, input.readObject());
            }
        }
    }
}
