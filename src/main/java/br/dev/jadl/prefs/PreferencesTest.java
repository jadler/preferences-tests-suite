package br.dev.jadl.prefs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class PreferencesTest {

    // Maximum length of string allowed as a key.
    protected static final int MAX_KEY_LENGTH = 80;

    // Maximum length of string allowed as a value
    protected static final int MAX_VALUE_LENGTH = 8192;

    // Maximum length of a node name.
    protected static final int MAX_NAME_LENGTH = 80;

    private Stream<Arguments> rootNodeProvider() {
        return Stream.of(
                Arguments.of((Supplier<?>) () -> Preferences.userRoot(), "user", true),
                Arguments.of((Supplier<?>) () -> Preferences.systemRoot(), "system", false));
    }
    
    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("Should return the root node with empty name and '/' as absolute path for this preference tree.")
    public void testRoot(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        Assertions.assertAll(
                () -> Assertions.assertEquals("", root.name()),
                () -> Assertions.assertEquals("/", root.absolutePath()));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("Should not throw exceptions when using valid keys and values in put() methods")
    public void testPutValid(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        Assertions.assertAll(
                () -> Assertions.assertDoesNotThrow(() -> root.put("string", "value")),
                () -> Assertions.assertDoesNotThrow(() -> root.putBoolean("boolean", true)),
                () -> Assertions.assertDoesNotThrow(() -> root.putByteArray("bytes", "value".getBytes())),
                () -> Assertions.assertDoesNotThrow(() -> root.putDouble("double", 1.0)),
                () -> Assertions.assertDoesNotThrow(() -> root.putFloat("float", 1.0F)),
                () -> Assertions.assertDoesNotThrow(() -> root.putInt("integer", 1)),
                () -> Assertions.assertDoesNotThrow(() -> root.putLong("long", 1L)));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("Should throw exceptions when using invalid keys or values in put()")
    public void testPutInvalid(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        final String a = "a".repeat(MAX_KEY_LENGTH);
        final String b = "b".repeat(MAX_KEY_LENGTH + 1);
        final String v = "v".repeat(MAX_VALUE_LENGTH + 1);

        Assertions.assertAll(
                () -> Assertions.assertThrows(IllegalArgumentException.class, () -> root.put(a, v)),
                () -> Assertions.assertThrows(IllegalArgumentException.class, () -> root.put(a, "\u0000")),
                () -> Assertions.assertThrows(IllegalArgumentException.class, () -> root.put(b, "value")),
                () -> Assertions.assertThrows(NullPointerException.class, () -> root.put(a, null)),
                () -> Assertions.assertThrows(NullPointerException.class, () -> root.put(null, "value")));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("Should retrieve correct values using appropriate get() methods")
    public void testGet(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        root.put("string", "value");
        root.put("boolean", "true");
        root.put("double", "1.0");
        root.put("float", "1.0");
        root.put("integer", "1");
        root.put("long", "1");

        Assertions.assertAll(
                () -> Assertions.assertEquals("value", root.get("string", null)),
                () -> Assertions.assertEquals(1, root.getInt("integer", 0)),
                () -> Assertions.assertEquals(1.0, root.getDouble("double", 0.0)),
                () -> Assertions.assertEquals(1.0, root.getFloat("float", 0.0F)),
                () -> Assertions.assertEquals(1L, root.getLong("long", 0L)),
                () -> Assertions.assertTrue(root.getBoolean("boolean", false)));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("It should remove the value associated with the specified key in this preference node.")
    public void testRemove(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        root.put("a", "value");
        root.put("b", "another value");

        Assertions.assertAll(
                () -> Assertions.assertDoesNotThrow(() -> root.remove("a")),
                () -> Assertions.assertDoesNotThrow(() -> root.remove("c")),
                () -> Assertions.assertEquals("another value", root.get("b", null)));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("It should throw an exception for invalid inputs in this preference node.")
    public void testRemoveExceptions(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        Assertions.assertAll(
                () -> Assertions.assertThrows(IllegalArgumentException.class, () -> root.remove("\u0000")),
                () -> Assertions.assertThrows(NullPointerException.class, () -> root.remove(null)));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("It should removes all of the preferences (key-value associations) in this preference node.")
    public void testClear(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        final Preferences a = root.node("a");
        a.put("a", "a");

        root.put("a", "a");
        root.put("b", "b");

        Assertions.assertAll(
            () -> Assertions.assertEquals("a", root.get("a", null)),
            () -> Assertions.assertEquals("b", root.get("b", null)));

        Assertions.assertDoesNotThrow(() -> root.clear());

        Assertions.assertAll(
            () -> Assertions.assertEquals("a", a.get("a", null)),
            () -> Assertions.assertEquals(null, root.get("a", null)),
            () -> Assertions.assertEquals(null, root.get("b", null)),
            () -> Assertions.assertTrue(Assertions.assertDoesNotThrow(() -> root.nodeExists("a"))));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    public void testPutByteArray(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        final byte[] array = new byte[MAX_VALUE_LENGTH * 3 / 4];
        Arrays.fill(array, (byte) 0xAB);

        Assertions.assertDoesNotThrow(() -> root.putByteArray("key", array));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("Should throw exceptions when using invalid keys or values in put()")
    public void testByteArrayMaxLengthException(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        final byte[] array = new byte[MAX_VALUE_LENGTH * 3 / 4 + 1];
        Arrays.fill(array, (byte) 0xAB);

        Assertions.assertThrows(IllegalArgumentException.class, () -> root.putByteArray("key", array));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("It should return all of the keys that have an associated value in this preference node.")
    public void testKeys(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        root.putInt("a", 1);
        root.putLong("b", 2L);
        root.putBoolean("c", true);

        Assertions.assertAll(
            () -> Assertions.assertEquals(3, root.keys().length),
            () -> Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, root.keys()));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("It should return an empty array if has no elements on this preference node.")
    public void testEmptyArray(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        Assertions.assertDoesNotThrow(() -> Assertions.assertEquals(0, root.keys().length));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    public void testChildrenNames(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        Preferences a = root.node("a");
        a.node("b");
        
        Assertions.assertAll(
                () -> Assertions.assertArrayEquals(new String[] {"a"}, root.childrenNames()),
                () -> Assertions.assertArrayEquals(new String[] {"b"}, a.childrenNames()));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("It should throws an exception for invalid path names.")
    public void testNodeExceptions(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        final Preferences a = root.node("a");
        final Preferences b = a.node("b");

        Assertions.assertDoesNotThrow(() -> Assertions.assertTrue(root.nodeExists("a/b")));
        Assertions.assertDoesNotThrow(() -> a.removeNode());

        Assertions.assertAll(
            () -> Assertions.assertThrows(IllegalArgumentException.class, () -> root.node("a//b")),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () -> root.node("/a//b")),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () -> root.node("/a/b/")),
            () -> Assertions.assertThrows(NullPointerException.class, () -> root.node(null)),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> b.node("c")));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("It should return the parent of this preference node, or null if this is the root.")
    public void testParent(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        final Preferences a = root.node("a");
        Assertions.assertAll(
            () -> Assertions.assertNull(root.parent()),
            () -> Assertions.assertNull(root.node("/").parent()),
            () -> Assertions.assertEquals(root, a.parent()));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("It should removes this preference node and all of its descendants")
    public void testRemoveNode(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        final Preferences c = root.node("a/b/c");
        final Preferences d = c.node("d");

        Assertions.assertDoesNotThrow(() -> Assertions.assertTrue(d.nodeExists("")));
        Assertions.assertDoesNotThrow(() -> c.removeNode());

        final OutputStream os = OutputStream.nullOutputStream();

        Assertions.assertAll(
            () -> Assertions.assertFalse(d.nodeExists("")),
            () -> Assertions.assertThrows(UnsupportedOperationException.class, () -> root.removeNode()),

            () -> Assertions.assertThrows(IllegalStateException.class, () -> c.clear()),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> c.exportSubtree(os)),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> c.exportNode(os)),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> c.get("key", "value")),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> c.keys()),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> c.node("d")),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> c.parent()),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> c.put("key", "value")),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> c.remove("any key")),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> c.sync()),

            () -> Assertions.assertDoesNotThrow(() -> c.name()),
            () -> Assertions.assertDoesNotThrow(() -> c.absolutePath()),
            () -> Assertions.assertDoesNotThrow(() -> c.isUserNode()),
            () -> Assertions.assertDoesNotThrow(() -> c.nodeExists("")),
            () -> Assertions.assertDoesNotThrow(() -> c.flush()),

            () -> Assertions.assertThrows(IllegalStateException.class, () -> d.node("e")),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> d.put("key", "value")),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> d.parent()));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("It should return the path of this node relative to its ancestor. The root node has a node name of the empty string (\"\").")
    public void testNodeName(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        final Preferences a = root.node("a");
        final Preferences b = root.node("a/b");
        final Preferences c = root.node("a").node("b").node("c");
        final Preferences d = root.node(" ").node(" ").node(" ").node("d");

        Assertions.assertAll(
            () -> Assertions.assertEquals("", root.name()),
            () -> Assertions.assertEquals("a", a.name()),
            () -> Assertions.assertEquals("b", b.name()),
            () -> Assertions.assertEquals("c", c.name()),
            () -> Assertions.assertEquals("d", d.name()));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    public void testNodeNameLength(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        Assertions.assertAll(
            () -> Assertions.assertDoesNotThrow(() -> root.node("n".repeat(MAX_NAME_LENGTH))),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () -> root.node("n".repeat(MAX_NAME_LENGTH + 1))));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("It should return the absolute path of this node, beginning with the root node. The root node’s absolute path is the string \"/\".")
    public void testAbsolutePath(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences root = supplier.get();

        final Preferences a = root.node("a");
        final Preferences b = root.node("a/b");
        final Preferences c = root.node("a").node("b").node("c");
        final Preferences d = root.node(" ").node(" ").node(" ").node("d");

        Assertions.assertAll(
            () -> Assertions.assertEquals("/", root.absolutePath()),
            () -> Assertions.assertEquals("/a", a.absolutePath()),
            () -> Assertions.assertEquals("/a/b", b.absolutePath()),
            () -> Assertions.assertEquals("/a/b/c", c.absolutePath()),
            () -> Assertions.assertEquals("/ / / /d", d.absolutePath()));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("It should return true if this preference node is part of user's tree.")
    public void testUserNode(final Supplier<Preferences> supplier, final String scope, final Boolean expected) {
        final Preferences root = supplier.get();

        Assertions.assertAll(
            () -> Assertions.assertEquals(expected, root.isUserNode()),
            () -> Assertions.assertEquals(expected, root.node("a").isUserNode()));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    public void testFlushAndSync(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final Preferences first = supplier.get();
        first.put("key", "value");
        first.node("a").put("key", "value");
        Assertions.assertDoesNotThrow(() -> first.flush());

        final Preferences second = supplier.get();
        Assertions.assertDoesNotThrow(() -> second.sync());

        Assertions.assertAll(
            () -> Assertions.assertTrue(second.nodeExists("a")),
            () -> Assertions.assertEquals("value", second.get("key", null)));
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    public void testExportNode(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final String tree = """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <!DOCTYPE preferences SYSTEM "http://java.sun.com/dtd/preferences.dtd">
            <preferences EXTERNAL_XML_VERSION="1.0">
              <root type="%s">
                <map>
                  <entry key="a" value="value"/>
                </map>
              </root>
            </preferences>
            """.formatted(scope);

        Preferences root = supplier.get();
        root.put("a", "value");
        root.node("a").put("a", "value");

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        Assertions.assertDoesNotThrow(() -> root.exportNode(baos));
        Assertions.assertArrayEquals(tree.getBytes(), baos.toByteArray());
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    public void testExportSubtree(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) {
        final String tree = """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <!DOCTYPE preferences SYSTEM "http://java.sun.com/dtd/preferences.dtd">
            <preferences EXTERNAL_XML_VERSION="1.0">
              <root type="%s">
                <map>
                  <entry key="a" value="value"/>
                </map>
                <node name="a">
                  <map>
                    <entry key="a" value="value"/>
                  </map>
                </node>
              </root>
            </preferences>
            """.formatted(scope);

        Preferences root = supplier.get();
        root.put("a", "value");
        root.node("a").put("a", "value");

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        Assertions.assertDoesNotThrow(() -> root.exportSubtree(baos));
        Assertions.assertArrayEquals(tree.getBytes(), baos.toByteArray());
    }

    @ParameterizedTest
    @MethodSource("rootNodeProvider")
    @DisplayName("Imports all of the preferences represented by the XML document on the specified input stream.")
    public void testImportPreferences(final Supplier<Preferences> supplier, final String scope, final Boolean userNode) throws IOException, BackingStoreException {
        final ByteArrayInputStream tree = new ByteArrayInputStream("""
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <!DOCTYPE preferences SYSTEM "http://java.sun.com/dtd/preferences.dtd">
            <preferences EXTERNAL_XML_VERSION="1.0">
              <root type="%s">
                <map>
                  <entry key="key" value="value"/>
                </map>
                <node name="a">
                  <map/>
                  <node name="b">
                    <map>
                      <entry key="key" value="value"/>
                    </map>
                    <node name="c">
                      <map/>
                    </node>
                  </node>
                </node>
              </root>
            </preferences>
            """.formatted(scope).getBytes());

        final Preferences a = supplier.get();
        Assertions.assertAll(
            () -> Assertions.assertEquals(null, a.get("key", null)),
            () -> Assertions.assertFalse(a.nodeExists("a/b/c")),
            () -> Assertions.assertEquals(null, a.node("a/b").get("key", null)));

        Assertions.assertDoesNotThrow(() -> Preferences.importPreferences(tree));

        Assertions.assertDoesNotThrow(() -> a.sync());

        final Preferences b = supplier.get();
        Assertions.assertAll(
            () -> Assertions.assertEquals("value", b.get("key", null), "value == b.get(key, null)"),
            () -> Assertions.assertTrue(b.nodeExists("a/b"), "b.nodeExists(a/b)"),
            () -> Assertions.assertTrue(b.nodeExists("a/b/c"), "b.nodeExists(a/b/c)"),
            () -> Assertions.assertEquals("value", b.node("a/b").get("key", null), "value == b.node(a/b).get(key, null)"));
    }

    @Test
    @DisplayName("It should throws an InvalidPreferencesFormatException")
    public void testInvalidPreferencesFormatException() {

        final ByteArrayInputStream incomplete = new ByteArrayInputStream("""
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <!DOCTYPE preferences SYSTEM "http://java.sun.com/dtd/preferences.dtd">
            <preferences EXTERNAL_XML_VERSION="1.0">
              <root type="user">
                <map/>
            """.getBytes());

        final ByteArrayInputStream invalid = new ByteArrayInputStream("""
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <!DOCTYPE preferences SYSTEM "http://java.sun.com/dtd/preferences.dtd">
            <preferences EXTERNAL_XML_VERSION="1.0">
              <root type="whatever">
                <map/>
              </root>
            </preferences>
            """.getBytes());

        Assertions.assertAll(
            () -> Assertions.assertThrows(InvalidPreferencesFormatException.class, () -> Preferences.importPreferences(InputStream.nullInputStream())),
            () -> Assertions.assertThrows(InvalidPreferencesFormatException.class, () -> Preferences.importPreferences(incomplete)),
            () -> Assertions.assertThrows(InvalidPreferencesFormatException.class, () -> Preferences.importPreferences(invalid)));
    }
}
