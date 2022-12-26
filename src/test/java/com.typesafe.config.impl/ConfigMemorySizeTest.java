package com.typesafe.config.impl;

import com.typesafe.config.ConfigMemorySize;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigMemorySizeTest {

    @Test
    void testEquals() {
        assertEquals(ConfigMemorySize.ofBytes(10), ConfigMemorySize.ofBytes(10), "Equal ConfigMemorySize are equal");
        assertNotEquals(ConfigMemorySize.ofBytes(10), ConfigMemorySize.ofBytes(11), "Different ConfigMemorySize are not equal");
    }

    @Test
    void testToUnits() {
        var kilobyte = ConfigMemorySize.ofBytes(1024);
        assertEquals(1024, kilobyte.toBytes());
    }

    @Test
    void testGetBytes() {
        var yottabyte = ConfigMemorySize.ofBytes(new BigInteger("1000000000000000000000000"));
        assertEquals(new BigInteger("1000000000000000000000000"), yottabyte.toBytesBigInteger());
    }
}
