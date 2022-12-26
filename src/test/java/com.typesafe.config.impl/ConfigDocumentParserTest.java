package com.typesafe.config.impl;

import com.typesafe.config.ConfigParseOptions;
import org.junit.jupiter.api.Test;


import static com.typesafe.config.ConfigSyntax.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigDocumentParserTest {

    private void parseTest(String origText) {
        var node = ConfigDocumentParser.parse(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults());
        assertEquals(origText, node.render());
    }

    private void parseJSONFailuresTest(String origText, String containsMessage) {
        var exceptionThrown = false;
        var e = intercept[ConfigException] {
            ConfigDocumentParser.parse(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults().setSyntax(JSON));
        }
        assertTrue(e.getMessage().contains(containsMessage));
    }

    private void parseSimpleValueTest(String origText, String finalText) {
        var expectedRenderedText = finalText == null ? origText : finalText;
        var node = ConfigDocumentParser.parseValue(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults());
        assertEquals(expectedRenderedText, node.render());
        assertTrue(node instanceof ConfigNodeSimpleValue);

        var nodeJSON = ConfigDocumentParser.parseValue(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults().setSyntax(JSON));
        assertEquals(expectedRenderedText, nodeJSON.render());
        assertTrue(nodeJSON instanceof ConfigNodeSimpleValue);
    }

    private void parseComplexValueTest(String origText) {
        var node = ConfigDocumentParser.parseValue(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults());
        assertEquals(origText, node.render());
        assertTrue(node instanceof ConfigNodeComplexValue);

        var nodeJSON = ConfigDocumentParser.parseValue(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults().setSyntax(JSON));
        assertEquals(origText, nodeJSON.render());
        assertTrue(nodeJSON instanceof ConfigNodeComplexValue);
    }

    private void parseSingleValueInvalidJSONTest(String origText, String containsMessage) {
        var node = ConfigDocumentParser.parseValue(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults());
        assertEquals(origText, node.render());

        var e = intercept[ConfigException] {
            ConfigDocumentParser.parseValue(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults().setSyntax(JSON));
        }
        assertTrue(e.getMessage().contains(containsMessage));
    }

    private void parseLeadingTrailingFailure(String toReplace) {
        var e = intercept[ConfigException] {
            ConfigDocumentParser.parseValue(tokenize(toReplace), fakeOrigin(), ConfigParseOptions.defaults());
        }
        assertTrue("expected message parsing leading trailing", e.getMessage().contains("The value from withValueText cannot have leading or trailing newlines, whitespace, or comments"));
    }

    @Test
    void parseSuccess() {
        parseTest("foo:bar");
        parseTest(" foo : bar ");
        parseTest("""include "foo.conf" """);
        parseTest("   \nfoo:bar\n    ");

        // Can parse a map with all simple types
        parseTest(
                """
                        aUnquoted : bar
                        aString = "qux"
                        aNum:123
                        aDouble=123.456
                        aTrue=true
                        aFalse=false
                        aNull=null
                        aSub =  ${a.b}
                        include "foo.conf"
                        """);
        parseTest("{}");
        parseTest("{foo:bar}");
        parseTest("{  foo  :  bar  }");
        parseTest("{foo:bar}     ");
        parseTest("""{include "foo.conf"}""");
        parseTest("   \n{foo:bar}\n    ");

        //Can parse a map with all simple types
        parseTest(
                """{
              aUnquoted : bar
              aString = "qux"
              aNum:123
              aDouble=123.456
              aTrue=true
              aFalse=false
              aNull=null
              aSub =  ${a.b}
              include "foo.conf"
              }""");

        // Test that maps can be nested within other maps
        parseTest(
                """
                        foo.bar.baz : {
                          qux : "abcdefg"
                          "abc".def."ghi" : 123
                          abc = { foo:bar }
                        }
                        qux = 123.456
                        """);

        // Test that comments can be parsed in maps
        parseTest(
                """{
              foo: bar
              // This is a comment
              baz:qux // This is another comment
             }""");

        // Basic array tests
        parseTest("[]");
        parseTest("[foo]");

        // Test trailing comment and whitespace
        parseTest("[foo,]");
        parseTest("[foo,]     ");

        // Test leading and trailing whitespace
        parseTest("   \n[]\n   ");

        // Can parse arrays with all simple types
        parseTest("""[foo, bar,"qux", 123,123.456, true,false, null, ${a.b}]""");
        parseTest("""[foo,   bar,"qux"    , 123 ,  123.456, true,false, null,   ${a.b}   ]""");

        // Basic concatenation tests
        parseTest("[foo bar baz qux]");
        parseTest("{foo: foo bar baz qux}");
        parseTest("[abc 123 123.456 null true false [1, 2, 3] {a:b}, 2]");

        // Complex node with all types test
        parseTest(
                """{
              foo: bar baz    qux    ernie
              // The above was a concatenation
    
              baz   =   [ abc 123, {a:12
                                    b: {
                                      c: 13
                                      d: {
                                        a: 22
                                        b: "abcdefg" # this is a comment
                                        c: [1, 2, 3]
                                      }
                                    }
                                    }, # this was an object in an array
                                    //The above value is a map containing a map containing a map, all in an array
                                    22,
                                    // The below value is an array contained in another array
                                    [1,2,3]]
              // This is a map with some nested maps and arrays within it, as well as some concatenations
              qux {
                baz: abc 123
                bar: {
                  baz: abcdefg
                  bar: {
                    a: null
                    b: true
                    c: [true false 123, null, [1, 2, 3]]
                  }
                }
              }
            // Did I cover everything?
            }""");

        // Can correctly parse a JSON string
        var origText =
                """{
                  "foo": "bar",
                  "baz": 123,
                  "qux": true,
                  "array": [
                    {"a": true,
                     "c": false},
                    12
                  ]
               }
          """;
        var node = ConfigDocumentParser.parse(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults().setSyntax(JSON));
        assertEquals(origText, node.render());
    }

    @Test
    void parseJSONFailures() {
        // JSON does not support concatenations
        parseJSONFailuresTest("""{ "foo": 123 456 789 } """, "Expecting close brace } or a comma");

        // JSON must begin with { or [
        parseJSONFailuresTest(""""a": 123, "b": 456""", "Document must have an object or array at root");

        // JSON does not support unquoted text
        parseJSONFailuresTest("""{"foo": unquotedtext}""", "Token not allowed in valid JSON");

        // JSON does not support substitutions
        parseJSONFailuresTest("""{"foo": ${"a.b"}}""", "Substitutions (${} syntax) not allowed in JSON");

        // JSON does not support multi-element paths
        parseJSONFailuresTest("""{"foo"."bar": 123}""", "Token not allowed in valid JSON");

        // JSON does not support =
        parseJSONFailuresTest("""{"foo"=123}""", """Key '"foo"' may not be followed by token: '='""");

        // JSON does not support +=
        parseJSONFailuresTest("""{"foo" += "bar"}""", """Key '"foo"' may not be followed by token: '+='""");

        // JSON does not support duplicate keys
        parseJSONFailuresTest("""{"foo" : 123, "foo": 456}""", "JSON does not allow duplicate fields");

        // JSON does not support trailing commas
        parseJSONFailuresTest("""{"foo" : 123,}""", "expecting a field name after a comma, got a close brace } instead");

        // JSON does not support empty documents
        parseJSONFailuresTest("", "Empty document");

    }

    @Test
    void parseSingleValues() {
        // Parse simple values
        parseSimpleValueTest("123", null);
        parseSimpleValueTest("123.456", null);
        parseSimpleValueTest(""""a string""""", null);
        parseSimpleValueTest("true", null);
        parseSimpleValueTest("false", null);
        parseSimpleValueTest("null", null);

        // Can parse complex values
        parseComplexValueTest("""{"a": "b"}""");
        parseComplexValueTest("""["a","b","c"]""");

        // Check that concatenations are handled by CONF parsing
        var origText = "123 456 \"abc\"";
        var node = ConfigDocumentParser.parseValue(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults());
        assertEquals(origText, node.render());

        // Check that keys with no separators and object values are handled by CONF parsing
        origText = """{"foo" { "bar" : 12 } }""";
        node = ConfigDocumentParser.parseValue(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults());
        assertEquals(origText, node.render());
    }

    @Test
    void parseSingleValuesFailures() {
        // Parse Simple Value throws on leading and trailing whitespace, comments, or newlines
        parseLeadingTrailingFailure("   123");
        parseLeadingTrailingFailure("123   ");
        parseLeadingTrailingFailure(" 123 ");
        parseLeadingTrailingFailure("\n123");
        parseLeadingTrailingFailure("123\n");
        parseLeadingTrailingFailure("\n123\n");
        parseLeadingTrailingFailure("#thisisacomment\n123#comment");

        // Parse Simple Value correctly throws on whitespace after a concatenation
        parseLeadingTrailingFailure("123 456 789   ");

        parseSingleValueInvalidJSONTest("unquotedtext", "Token not allowed in valid JSON");
        parseSingleValueInvalidJSONTest("${a.b}", "Substitutions (${} syntax) not allowed in JSON");

        // Check that concatenations in JSON will throw an error
        var origText = "123 456 \"abc\"";
        var e = intercept[ConfigException] {
            ConfigDocumentParser.parseValue(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults().setSyntax(JSON));
        }
        assertTrue("expected message for parsing concat as json", e.getMessage().contains("Parsing JSON and the value set in withValueText was either a concatenation or had trailing whitespace, newlines, or comments"));

        // Check that keys with no separators and object values in JSON will throw an error
        origText = """{"foo" { "bar" : 12 } }""";
        e = intercept[ConfigException] {
            ConfigDocumentParser.parseValue(tokenize(origText), fakeOrigin(), ConfigParseOptions.defaults().setSyntax(JSON));
        }
        assertTrue("expected failure for key foo followed by token", e.getMessage.contains("""Key '"foo"' may not be followed by token: '{'"""));
    }

    @Test
    void parseEmptyDocument() {
        var node = ConfigDocumentParser.parse(tokenize(""), fakeOrigin(), ConfigParseOptions.defaults());
        assertTrue(node.value() instanceof ConfigNodeObject);
        assertTrue(node.value().children().isEmpty());

        var node2 = ConfigDocumentParser.parse(tokenize("#comment\n#comment\n\n"), fakeOrigin(), ConfigParseOptions.defaults());
        assertTrue(node2.value() instanceof ConfigNodeObject);
    }

}
