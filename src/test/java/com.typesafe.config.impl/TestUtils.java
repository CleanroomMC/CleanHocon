package com.typesafe.config.impl;

import com.typesafe.config.ConfigIncluder;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;

import java.io.*;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtils {

    protected static <E extends Throwable> E intercept(Class<E> expectedClass, Supplier<Object> block) throws Exception {
        Throwable thrown = null;
        Object result = null;
        try {
            result = block.get();
        } catch (Throwable t) {
            thrown = t;
        }
        if (thrown != null) {
            if (expectedClass.isAssignableFrom(thrown.getClass())) {
                return expectedClass.cast(thrown);
            } else {
                throw new Exception(String.format("Expected exception %s was not thrown, got %s", expectedClass.getName(), thrown), thrown);
            }
        } else {
            throw new Exception(String.format("Expected exception %s was not thrown, no exception was thrown and got result %s", expectedClass.getName(), result));
        }
    }

    protected static <A> A describeFailure(String desc, Supplier<A> code) {
        try {
            return code.get();
        } catch (Throwable t) {
            System.out.println(String.format("Failure on: '%s'", desc));
            throw t;
        }
    }

    protected static class NotEqualToAnythingElse {
        @Override
        public boolean equals(Object other) {
            if (other instanceof NotEqualToAnythingElse) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 971;
        }
    }

    static NotEqualToAnythingElse notEqualToAnything = new NotEqualToAnythingElse();

    private static void checkNotEqualToRandomOtherThing(Object a) {
        assertNotEquals(a, notEqualToAnything);
        assertNotEquals(notEqualToAnything, a);
    }

    protected static void checkNotEqualObjects(Object a, Object b) {
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        // hashcode inequality isn't guaranteed, but
        // as long as it happens to work it might
        // detect a bug (if hashcodes are equal,
        // check if it's due to a bug or correct
        // before you remove this)
        assertFalse(a.hashCode() == b.hashCode());
        checkNotEqualToRandomOtherThing(a);
        checkNotEqualToRandomOtherThing(b);
    }

    protected static void checkEqualObjects(Object a, Object b) {
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());
        checkNotEqualToRandomOtherThing(a);
        checkNotEqualToRandomOtherThing(b);
    }

    private static final char[] hexDigits = new char[16];

    static {
        int i = 0;
        for (char c = '0'; c <= '9'; c++) {
            hexDigits[i] = c;
            i++;
        }
        for (char c = 'A'; c <= 'F'; c++) {
            hexDigits[i] = c;
            i++;
        }
    }

    private static String encodeLegibleBinary(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if ((b >= 'a' && b <= 'z') ||
                    (b >= 'A' && b <= 'Z') ||
                    (b >= '0' && b <= '9') ||
                    b == '-' || b == ':' || b == '.' || b == '/' || b == ' ') {
                sb.append('_');
                sb.appendCodePoint(b);
            } else {
                sb.append(hexDigits[(b & 0xF0) >> 4]);
                sb.append(hexDigits[b & 0x0F]);
            }
        }
        return sb.toString();
    }

    private static byte[] decodeLegibleBinary(String s) {
        byte[] a = new byte[s.length() / 2];
        int i = 0;
        int j = 0;
        while (i < s.length()) {
            String sub = s.substring(i, i + 2);
            i += 2;
            if (sub.charAt(0) == '_') {
                a[j] = (byte) sub.charAt(1);
            } else {
                a[j] = (byte) Integer.parseInt(sub, 16);
            }
            j++;
        }
        return a;
    }

    private static Object copyViaSerialize(Serializable o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(o);
        objectStream.close();
        ByteArrayInputStream inStream = new ByteArrayInputStream(byteStream.toByteArray());
        ObjectInputStream inObjectStream = new ObjectInputStream(inStream);
        Object copy = inObjectStream.readObject();
        inObjectStream.close();
        return copy;
    }

    // TODO: 25/12/2022 finish
    protected <T> void checkSerializationCompat(String expectedHex, T o, boolean changedOK) throws IOException, ClassNotFoundException {
        throw new RuntimeException();
    }

    protected void checkNotSerializable(Object o) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        NotSerializableException e = null;
        try {
            objectStream.writeObject(o);
        } catch (NotSerializableException exc) {
            e = exc;
        } finally {
            objectStream.close();
        }
        if (e == null) {
            throw new AssertionError("Expected NotSerializableException, but no exception was thrown");
        }
    }

    // TODO: 25/12/2022 finish
    protected <T> T checkSerializable(String expectedHex, T o) throws IOException, ClassNotFoundException {
        throw new RuntimeException();
    }

    // TODO: 25/12/2022 finish
    protected <T> T checkSerializableOldFormat(String expectedHex, T o) throws IOException, ClassNotFoundException {
        throw new RuntimeException();
    }

    protected <T> T checkSerializableNoMeaningfulEquals(T o) throws Exception {
        assertTrue(o instanceof Serializable, o.getClass().getSimpleName() + " not an instance of Serializable");

        Serializable a = (Serializable) o;

        Object b = null;
        try {
            b = copyViaSerialize(a);
        } catch (ClassNotFoundException nf) {
            throw new AssertionError("failed to make a copy via serialization, " +
                    "possibly caused by http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6446627",
                    nf);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new AssertionError("failed to make a copy via serialization", e);
        }

        assertTrue(o.getClass().isAssignableFrom(b.getClass()),
                "deserialized type " + b.getClass().getSimpleName() + " doesn't match serialized type " + a.getClass().getSimpleName());

        return (T) b;
    }

    protected void checkEqualOrigins(Object a, Object b) {
        if (a instanceof ConfigObject && b instanceof ConfigObject) {
            ConfigObject obj1 = (ConfigObject) a;
            ConfigObject obj2 = (ConfigObject) b;
            assertEquals(obj1.origin(), obj2.origin());
            for (Map.Entry<String, ConfigValue> e : obj1.entrySet()) {
                checkEqualOrigins(e.getValue(), obj2.get(e.getKey()));
            }
        } else if (a instanceof ConfigList && b instanceof ConfigList) {
            ConfigList list1 = (ConfigList) a;
            ConfigList list2 = (ConfigList) b;
            assertEquals(list1.origin(), list2.origin());
            for (int i = 0; i < list1.size(); i++) {
                checkEqualOrigins(list1.get(i), list2.get(i));
            }
        } else if (a instanceof ConfigValue && b instanceof ConfigValue) {
            ConfigValue value1 = (ConfigValue) a;
            ConfigValue value2 = (ConfigValue) b;
            assertEquals(value1.origin(), value2.origin());
        }
    }

    private SimpleConfigOrigin fakeOrigin() {
        return SimpleConfigOrigin.newSimple("fake origin");
    }

    private ConfigIncluder includer() {
        return ConfigImpl.defaultIncluder();
    }

}
