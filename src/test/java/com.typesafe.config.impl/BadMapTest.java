package com.typesafe.config.impl;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BadMapTest {

    @Test
    void copyingPut() {
        var map = new BadMap<String, String>();
        var copy = map.copyingPut("key", "value");

        assertNull(map.get("key"));
        assertEquals("value", copy.get("key"));
    }

    @Test
    void retrieveOldElement() {
        var map = new BadMap<String, String>()
                .copyingPut("key1", "value1")
                .copyingPut("key2", "value2")
                .copyingPut("key3", "value3");

        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
        assertEquals("value3", map.get("key3"));
    }

    @Test
    void putOverride() {
        var map = new BadMap<String, String>()
                .copyingPut("key", "value1")
                .copyingPut("key", "value2")
                .copyingPut("key", "value3");

        assertEquals("value3", map.get("key"));
    }

    @Test
    void notFound() {
        var map = new BadMap<String, String>();
        assertNull(map.get("invalid key"));
    }

    @Test
    void putMany() {
        Map<String, String> entries = IntStream.rangeClosed(0, 1000).collect(HashMap::new, (m, i) -> m.put("key" + i, "value" + i), HashMap::putAll);
        var map = new BadMap<>();

        for (var entry : entries.entrySet()) {
            map = map.copyingPut(entry.getKey(), entry.getValue());
        }

        for (var entry : entries.entrySet()) {
            assertEquals(entry.getValue(), map.get(entry.getKey()));
        }
    }

    @Test
    void putSameHash() {
        var hash = 2;
        Map<UniqueKeyWithHash, String> entries = IntStream.rangeClosed(0, 10).collect(HashMap::new, (m, i) -> m.put(new UniqueKeyWithHash(hash), "value" + i), HashMap::putAll);
        var map = new BadMap<UniqueKeyWithHash, String>();

        for (var entry : entries.entrySet()) {
            map = map.copyingPut(entry.getKey(), entry.getValue());
        }

        for (var entry : entries.entrySet()) {
            assertEquals(entry.getValue(), map.get(entry.getKey()));
        }
    }

    @Test
    void putSameHashModLength() {
        // given that the table will eventually be the following size, we insert entries who should
        // eventually all share the same index and then later be redistributed once rehashed
        var size = 11;
        Map<UniqueKeyWithHash, String> entries = IntStream.rangeClosed(0, 10).collect(HashMap::new, (m, i) -> m.put(new UniqueKeyWithHash(size * i), "value" + i), HashMap::putAll);
        var map = new BadMap<UniqueKeyWithHash, String>();

        for (var entry : entries.entrySet()) {
            map = map.copyingPut(entry.getKey(), entry.getValue());
        }

        for (var entry : entries.entrySet()) {
            assertEquals(entry.getValue(), map.get(entry.getKey()));
        }
    }

    public static class UniqueKeyWithHash {
        int hash;

        public UniqueKeyWithHash(int hash) {
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
