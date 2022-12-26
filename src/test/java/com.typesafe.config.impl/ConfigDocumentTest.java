package com.typesafe.config.impl;

import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;
import com.typesafe.config.ConfigValueFactory;
import com.typesafe.config.parser.ConfigDocumentFactory;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigDocumentTest {
    private void configDocumentReplaceJsonTest(String origText, String finalText, String newValue, String replacePath) {
        var configDocument = ConfigDocumentFactory.parseString(origText, ConfigParseOptions.defaults().setSyntax(ConfigSyntax.JSON));
        assertEquals(origText, configDocument.render());
        var newDocument = configDocument.withValueText(replacePath, newValue);
        assertTrue(newDocument instanceof SimpleConfigDocument);
        assertEquals(finalText, newDocument.render());
    }

    private void configDocumentReplaceConfTest(String origText, String finalText, String newValue, String replacePath) {
        var configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals(origText, configDocument.render());
        var newDocument = configDocument.withValueText(replacePath, newValue);
        assertTrue(newDocument instanceof SimpleConfigDocument);
        assertEquals(finalText, newDocument.render());
    }

    @Test
    void configDocumentReplace() {
        // Can handle parsing/replacement with a very simple map
        configDocumentReplaceConfTest("""
                {"a":1}""", """
                {"a":2}""", "2", "a");
        configDocumentReplaceJsonTest("""
                {"a":1}""", """
                {"a":2}""", "2", "a");

        // Can handle parsing/replacement with a map without surrounding braces
        configDocumentReplaceConfTest("a: b\nc = d", "a: b\nc = 12", "12", "c");

        // Can handle parsing/replacement with a complicated map
        var origText =
                """
                         {
                         "a":123,
                         "b": 123.456,
                         "c": true,
                         "d": false,
                         "e": null,
                         "f": "a string",
                         "g": [1,2,3,4,5],
                         "h": {
                           "a": 123,
                           "b": {
                             "a": 12
                           },
                           "c": [1, 2, 3, {"a": "b"}, [1,2,3]]
                         }
                        }""";
        var finalText =
                """
                         {
                         "a":123,
                         "b": 123.456,
                         "c": true,
                         "d": false,
                         "e": null,
                         "f": "a string",
                         "g": [1,2,3,4,5],
                         "h": {
                           "a": 123,
                           "b": {
                             "a": "i am now a string"
                           },
                           "c": [1, 2, 3, {"a": "b"}, [1,2,3]]
                         }
                        }""";
        configDocumentReplaceConfTest(origText, finalText, "i am now a string", " h.b.a");
        configDocumentReplaceJsonTest(origText, finalText, "i am now a string", " h.b.a");

        // Can handle replacing values with maps
        finalText =
                """
                         {
                         "a":123,
                         "b": 123.456,
                         "c": true,
                         "d": false,
                         "e": null,
                         "f": "a string",
                         "g": [1,2,3,4,5],
                         "h": {
                           "a": 123,
                           "b": {
                             "a": {"a":"b", "c":"d"}
                           },
                           "c": [1, 2, 3, {"a": "b"}, [1,2,3]]
                         }
                        }"""
        configDocumentReplaceConfTest(origText, finalText, """
                {"a":"b", "c":"d"}""", "h.b.a");
        configDocumentReplaceJsonTest(origText, finalText, """
                {"a":"b", "c":"d"}""", "h.b.a");

        // Can handle replacing values with arrays
        finalText =
                """
                         {
                         "a":123,
                         "b": 123.456,
                         "c": true,
                         "d": false,
                         "e": null,
                         "f": "a string",
                         "g": [1,2,3,4,5],
                         "h": {
                           "a": 123,
                           "b": {
                             "a": [1,2,3,4,5]
                           },
                           "c": [1, 2, 3, {"a": "b"}, [1,2,3]]
                         }
                        }""";
        configDocumentReplaceConfTest(origText, finalText, "[1,2,3,4,5]", "h.b.a");
        configDocumentReplaceJsonTest(origText, finalText, "[1,2,3,4,5]", "h.b.a");

        finalText =
                """
                         {
                         "a":123,
                         "b": 123.456,
                         "c": true,
                         "d": false,
                         "e": null,
                         "f": "a string",
                         "g": [1,2,3,4,5],
                         "h": {
                           "a": 123,
                           "b": {
                             "a": this is a concatenation 123 456 {a:b} [1,2,3] {a: this is another 123 concatenation null true}
                           },
                           "c": [1, 2, 3, {"a": "b"}, [1,2,3]]
                         }
                        }""";
        configDocumentReplaceConfTest(origText, finalText,
                "this is a concatenation 123 456 {a:b} [1,2,3] {a: this is another 123 concatenation null true}", "h.b.a");
    }

    @Test
    void configDocumentMultiElementDuplicatesRemoved() {
        var origText = "{a: b, a.b.c: d, a: e}";
        var configDoc = ConfigDocumentFactory.parseString(origText);
        assertEquals("{a: 2}", configDoc.withValueText("a", "2").render());

        origText = "{a: b, a: e, a.b.c: d}";
        configDoc = ConfigDocumentFactory.parseString(origText);
        assertEquals("{a: 2, }", configDoc.withValueText("a", "2").render());

        origText = "{a.b.c: d}";
        configDoc = ConfigDocumentFactory.parseString(origText);
        assertEquals("{ a : 2}", configDoc.withValueText("a", "2").render());
    }

    @Test
    void configDocumentSetNewValueBraceRoot() {
        var origText = "{\n\t\"a\":\"b\",\n\t\"c\":\"d\"\n}";
        var finalTextConf = "{\n\t\"a\":\"b\",\n\t\"c\":\"d\"\n\t\"e\" : \"f\"\n}";
        var finalTextJson = "{\n\t\"a\":\"b\",\n\t\"c\":\"d\",\n\t\"e\" : \"f\"\n}";
        configDocumentReplaceConfTest(origText, finalTextConf, "\"f\"", "\"e\"");
        configDocumentReplaceJsonTest(origText, finalTextJson, "\"f\"", "\"e\"");
    }

    @Test
    void configDocumentSetNewValueNoBraces() {
        var origText = "\"a\":\"b\",\n\"c\":\"d\"\n";
        var finalText = "\"a\":\"b\",\n\"c\":\"d\"\n\"e\" : \"f\"\n";
        configDocumentReplaceConfTest(origText, finalText, "\"f\"", "\"e\"");
    }

    @Test
    void configDocumentSetNewValueMultiLevelConf() {
        var origText = "a:b\nc:d";
        var finalText = "a:b\nc:d\ne : {\n  f : {\n    g : 12\n  }\n}";
        configDocumentReplaceConfTest(origText, finalText, "12", "e.f.g");
    }

    @Test
    void configDocumentSetNewValueMultiLevelJson() {
        var origText = "{\"a\":\"b\",\n\"c\":\"d\"}";
        var finalText = "{\"a\":\"b\",\n\"c\":\"d\",\n  \"e\" : {\n    \"f\" : {\n      \"g\" : 12\n    }\n  }}";
        configDocumentReplaceJsonTest(origText, finalText, "12", "e.f.g");
    }

    @Test
    void configDocumentSetNewConfigValue() {
        var origText = "{\"a\": \"b\"}";
        var finalText = "{\"a\": 12}";
        var configDocHOCON = ConfigDocumentFactory.parseString(origText);
        var configDocJSON = ConfigDocumentFactory.parseString(origText, ConfigParseOptions.defaults.setSyntax(ConfigSyntax.JSON));
        var newValue = ConfigValueFactory.fromAnyRef(12);
        assertEquals(origText, configDocHOCON.render());
        assertEquals(origText, configDocJSON.render());
        assertEquals(finalText, configDocHOCON.withValue("a", newValue).render());
        assertEquals(finalText, configDocJSON.withValue("a", newValue).render());
    }

    @Test
    void configDocumentHasValue() {
        var origText = "{a: b, a.b.c.d: e, c: {a: {b: c}}}";
        var configDoc = ConfigDocumentFactory.parseString(origText);

        assertTrue(configDoc.hasPath("a"));
        assertTrue(configDoc.hasPath("a.b.c"));
        assertTrue(configDoc.hasPath("c.a.b"));
        assertFalse(configDoc.hasPath("c.a.b.c"));
        assertFalse(configDoc.hasPath("a.b.c.d.e"));
        assertFalse(configDoc.hasPath("this.does.not.exist"));
    }

    @Test
    void configDocumentRemoveValue() {
        var origText = "{a: b, a.b.c.d: e, c: {a: {b: c}}}";
        var configDoc = ConfigDocumentFactory.parseString(origText);

        assertEquals("{c: {a: {b: c}}}", configDoc.withoutPath("a").render());
        assertEquals("{a: b, a.b.c.d: e, }", configDoc.withoutPath("c").render());
        assertEquals(configDoc, configDoc.withoutPath("this.does.not.exist"));
    }

    @Test
    void configDocumentRemoveValueJSON() {
        var origText = """
                {"a": "b", "c": "d"}""";
        var configDoc = ConfigDocumentFactory.parseString(origText, ConfigParseOptions.defaults().setSyntax(ConfigSyntax.JSON));

        // Ensure that removing a value in JSON does not leave us with a trailing comma
        assertEquals("""
                {"a": "b" }""", configDoc.withoutPath("c").render());
    }

    @Test
    void configDocumentRemoveMultiple() {
        var origText = "a { b: 42 }, a.b = 43, a { b: { c: 44 } }";
        var configDoc = ConfigDocumentFactory.parseString(origText);
        var removed = configDoc.withoutPath("a.b");
        assertEquals("a { }, a { }", removed.render());
    }

    @Test
    void configDocumentRemoveOverridden() {
        var origText = "a { b: 42 }, a.b = 43, a { b: { c: 44 } }, a : 57 ";
        var configDoc = ConfigDocumentFactory.parseString(origText);
        var removed = configDoc.withoutPath("a.b");
        assertEquals("a { }, a { }, a : 57 ", removed.render());
    }

    @Test
    void configDocumentRemoveNested() {
        var origText = "a { b: 42 }, a.b = 43, a { b: { c: 44 } }";
        var configDoc = ConfigDocumentFactory.parseString(origText);
        var removed = configDoc.withoutPath("a.b.c");
        assertEquals("a { b: 42 }, a.b = 43, a { b: { } }", removed.render());
    }

    @Test
    void configDocumentArrayFailures() {
        // Attempting certain methods on a ConfigDocument parsed from an array throws an error
        var origText = "[1, 2, 3, 4, 5]";
        var document = ConfigDocumentFactory.parseString(origText);

        var e1 = intercept[ConfigException] {
            document.withValueText("a", "1")
        }
        assertTrue(e1.getMessage().contains("ConfigDocument had an array at the root level"));

        var e2 = intercept[ConfigException] {
            document.hasPath("a")
        }
        assertTrue(e2.getMessage().contains("ConfigDocument had an array at the root level"));

        var e3 = intercept[ConfigException] {
            document.withoutPath("a")
        }
        assertTrue(e3.getMessage().contains("ConfigDocument had an array at the root level"));
    }

    @Test
    void configDocumentJSONReplaceFailure() {
        // Attempting a replace on a ConfigDocument parsed from JSON with a value using HOCON syntax
        // will fail
        var origText = "{\"foo\": \"bar\", \"baz\": \"qux\"}";
        var document = ConfigDocumentFactory.parseString(origText, ConfigParseOptions.defaults().setSyntax(ConfigSyntax.JSON));

        var e = intercept[ConfigException] {
            document.withValueText("foo", "unquoted")
        }
        assertTrue(e.getMessage().contains("Token not allowed in valid JSON"));
    }

    @Test
    void configDocumentJSONReplaceWithConcatenationFailure() {
        // Attempting a replace on a ConfigDocument parsed from JSON with a concatenation will
        // fail
        var origText = "{\"foo\": \"bar\", \"baz\": \"qux\"}";
        var document = ConfigDocumentFactory.parseString(origText, ConfigParseOptions.defaults().setSyntax(ConfigSyntax.JSON));

        var e = intercept[ConfigException] {
            document.withValueText("foo", "1 2 3 concatenation")
        }
        assertTrue("got correct exception for concat value", e.getMessage().contains("Parsing JSON and the value set in withValueText was either a concatenation or had trailing whitespace, newlines, or comments"));
    }

    @Test
    void configDocumentFileParse() throws IOException {
        var configDocument = ConfigDocumentFactory.parseFile(new File("/test03.conf"));
        var fileReader = new BufferedReader(new FileReader("src/test/resources/test03.conf"));
        var line = fileReader.readLine();
        var sb = new StringBuilder();
        while (line != null) {
            sb.append(line);
            sb.append("\n");
            line = fileReader.readLine();
        }
        fileReader.close();
        var fileText = sb.toString();
        assertEquals(fileText, defaultLineEndingsToUnix(configDocument.render()));
    }

    private String defaultLineEndingsToUnix(String s) {
        return s.replaceAll(System.lineSeparator(), "\n");
    }

    @Test
    void configDocumentReaderParse() throws FileNotFoundException {
        var configDocument = ConfigDocumentFactory.parseReader(new FileReader("/test03.conf"));
        var configDocumentFile = ConfigDocumentFactory.parseFile(new File("/test03.conf"));
        assertEquals(configDocumentFile.render(), configDocument.render());
    }

    @Test
    void configDocumentIndentationSingleLineObject() {
        // Proper insertion for single-line objects
        var origText = "a { b: c }";
        var configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals("a { b: c, d : e }", configDocument.withValueText("a.d", "e").render());

        origText = "a { b: c }, d: e";
        configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals("a { b: c }, d: e, f : g", configDocument.withValueText("f", "g").render());

        origText = "a { b: c }, d: e,";
        configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals("a { b: c }, d: e, f : g", configDocument.withValueText("f", "g").render());

        assertEquals("a { b: c }, d: e, f : { g : { h : i } }", configDocument.withValueText("f.g.h", "i").render());

        origText = "{a { b: c }, d: e}";
        configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals("{a { b: c }, d: e, f : g}", configDocument.withValueText("f", "g").render());

        assertEquals("{a { b: c }, d: e, f : { g : { h : i } }}", configDocument.withValueText("f.g.h", "i").render());
    }

    @Test
    void configDocumentIndentationMultiLineObject() {
        var origText = "a {\n  b: c\n}";
        var configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals("a {\n  b: c\n  e : f\n}", configDocument.withValueText("a.e", "f").render());

        assertEquals("a {\n  b: c\n  d : {\n    e : {\n      f : g\n    }\n  }\n}", configDocument.withValueText("a.d.e.f", "g").render());

        origText = "a {\n b: c\n}\n";
        configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals("a {\n b: c\n}\nd : e\n", configDocument.withValueText("d", "e").render());

        assertEquals("a {\n b: c\n}\nd : {\n  e : {\n    f : g\n  }\n}\n", configDocument.withValueText("d.e.f", "g").render());
    }

    @Test
    void configDocumentIndentationNested() {
        var origText = "a { b { c { d: e } } }";
        var configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals("a { b { c { d: e, f : g } } }", configDocument.withValueText("a.b.c.f", "g").render());

        origText = "a {\n  b {\n    c {\n      d: e\n    }\n  }\n}";
        configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals("a {\n  b {\n    c {\n      d: e\n      f : g\n    }\n  }\n}", configDocument.withValueText("a.b.c.f", "g").render());
    }

    @Test
    void configDocumentIndentationEmptyObject() {
        var origText = "a { }";
        var configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals("a { b : c }", configDocument.withValueText("a.b", "c").render());

        origText = "a {\n  b {\n  }\n}";
        configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals("a {\n  b {\n    c : d\n  }\n}", configDocument.withValueText("a.b.c", "d").render());
    }

    @Test
    void configDocumentIndentationMultiLineValue() {
        var origText = "a {\n  b {\n    c {\n      d: e\n    }\n  }\n}";
        var configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals("a {\n  b {\n    c {\n      d: e\n      f : {\n        g: h\n        i: j\n        k: {\n          l: m\n        }\n      }\n    }\n  }\n}",
                configDocument.withValueText("a.b.c.f", "{\n  g: h\n  i: j\n  k: {\n    l: m\n  }\n}").render());

        assertEquals("a {\n  b {\n    c {\n      d: e\n      f : 12 13 [1,\n      2,\n      3,\n      {\n        a:b\n      }]\n    }\n  }\n}",
                configDocument.withValueText("a.b.c.f", "12 13 [1,\n2,\n3,\n{\n  a:b\n}]").render());
    }

    @Test
    void configDocumentIndentationMultiLineValueSingleLineObject() {
        // Weird indentation occurs when adding a multi-line value to a single-line object
        var origText = "a { b { } }";
        var configDocument = ConfigDocumentFactory.parseString(origText);
        assertEquals("a { b { c : {\n   c:d\n } } }", configDocument.withValueText("a.b.c", "{\n  c:d\n}").render());
    }

    @Test
    void configDocumentIndentationSingleLineObjectContainingMultiLineValue() {
        var origText = "a { b {\n  c: d\n} }";
        var configDocument = ConfigDocumentFactory.parseString(origText);

        assertEquals("a { b {\n  c: d\n}, e : f }", configDocument.withValueText("a.e", "f").render());
    }

    @Test
    void configDocumentIndentationReplacingWithMultiLineValue() {
        var origText = "a {\n  b {\n    c : 22\n  }\n}";
        var configDocument = ConfigDocumentFactory.parseString(origText);

        assertEquals("a {\n  b {\n    c : {\n      d:e\n    }\n  }\n}", configDocument.withValueText("a.b.c", "{\n  d:e\n}").render());

        origText = "a {\n  b {\n                f : 10\n    c : 22\n  }\n}";
        configDocument = ConfigDocumentFactory.parseString(origText);

        assertEquals("a {\n  b {\n                f : 10\n    c : {\n      d:e\n    }\n  }\n}", configDocument.withValueText("a.b.c", "{\n  d:e\n}").render());
    }

    @Test
    void configDocumentIndentationValueWithInclude() {
        var origText = "a {\n  b {\n    c : 22\n  }\n}";
        var configDocument = ConfigDocumentFactory.parseString(origText);

        assertEquals("a {\n  b {\n    c : 22\n    d : {\n      include \"foo\"\n      e:f\n    }\n  }\n}",
                configDocument.withValueText("a.b.d", "{\n  include \"foo\"\n  e:f\n}").render());
    }

    @Test
    void configDocumentIndentationBasedOnIncludeNode() {
        var origText = "a : b\n      include \"foo\"\n";
        var configDocument = ConfigDocumentFactory.parseString(origText);

        assertEquals("a : b\n      include \"foo\"\n      c : d\n", configDocument.withValueText("c", "d").render());
    }

    @Test
    void configDocumentEmptyTest() {
        var origText = "";
        var configDocument = ConfigDocumentFactory.parseString(origText);

        assertEquals("a : 1", configDocument.withValueText("a", "1").render());

        var mapVar = ConfigValueFactory.fromAnyRef(Map.of("a", 1, "b", 2));
        assertEquals("a : {\n    \"a\" : 1,\n    \"b\" : 2\n}",
                configDocument.withValue("a", mapVar).render());

        var arrayVar = ConfigValueFactory.fromAnyRef(List.of(1, 2));
        assertEquals("a : [\n    1,\n    2\n]", configDocument.withValue("a", arrayVar).render());
    }

    @Test
    void configDocumentConfigObjectInsertion() {
        var origText = "{ a : b }";
        var configDocument = ConfigDocumentFactory.parseString(origText);

        var configVar = ConfigValueFactory.fromAnyRef(Map.of("a", 1, "b", 2));

        assertEquals("{ a : {\n     \"a\" : 1,\n     \"b\" : 2\n } }",
                configDocument.withValue("a", configVar).render());
    }
}
