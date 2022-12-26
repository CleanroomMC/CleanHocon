package com.typesafe.config.impl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigNodeTest {
    private void singleTokenNodeTest(Token token) {
        var node = configNodeSingleToken(token);
        assertEquals(node.render(), token.tokenText());
    }

    private void keyNodeTest(String path) {
        var node = configNodeKey(path);
        assertEquals(path, node.render());
    }

    private void simpleValueNodeTest(Token token) {
        var node = configNodeSimpleValue(token);
        assertEquals(node.render(), token.tokenText());
    }

    private void fieldNodeTest(ConfigNodePath key, AbstractConfigNodeValue value, AbstractConfigNodeValue newValue) {
        var keyValNode = nodeKeyValuePair(key, value);
        assertEquals(key.render() + " : " + value.render(), keyValNode.render());
        assertEquals(key.render, keyValNode.path().render());
        assertEquals(value.render, keyValNode.value().render());

        var newKeyValNode = keyValNode.replaceValue(newValue);
        assertEquals(key.render() + " : " + newValue.render(), newKeyValNode.render());
        assertEquals(newValue.render(), newKeyValNode.value().render());
    }

    private void topLevelValueReplaceTest(AbstractConfigNodeValue value, AbstractConfigNodeValue newValue, String key) {
        if (key == null) {
            key = "foo";
        }
        var complexNodeChildren = List.of(nodeOpenBrace,
                nodeKeyValuePair(configNodeKey(key), value),
                nodeCloseBrace);
        var complexNode = configNodeObject(complexNodeChildren);
        var newNode = complexNode.setValueOnPath(key, newValue);
        var origText = "{" + key + " : " + value.render() + "}";
        var finalText = "{" + key + " : " + newValue.render() + "}";

        assertEquals(origText, complexNode.render());
        assertEquals(finalText, newNode.render());
    }

    private void replaceDuplicatesTest(AbstractConfigNodeValue value1, AbstractConfigNodeValue value2, AbstractConfigNodeValue value3) {
        var key = configNodeKey("foo");
        var keyValPair1 = nodeKeyValuePair(key, value1);
        var keyValPair2 = nodeKeyValuePair(key, value2);
        var keyValPair3 = nodeKeyValuePair(key, value3);
        var complexNode = configNodeObject(List(keyValPair1, keyValPair2, keyValPair3));
        var origText = keyValPair1.render() + keyValPair2.render() + keyValPair3.render();
        var finalText = key.render() + " : 15";

        assertEquals(origText, complexNode.render());
        assertEquals(finalText, complexNode.setValueOnPath("foo", nodeInt(15)).render());
    }

    private void nonExistentPathTest(AbstractConfigNodeValue value) {
        var node = configNodeObject(List(nodeKeyValuePair(configNodeKey("bar"), nodeInt(15))));
        assertEquals("bar : 15", node.render());
        var newNode = node.setValueOnPath("foo", value);
        var finalText = "bar : 15, foo : " + value.render();
        assertEquals(finalText, newNode.render());
    }

    @Test
    void createBasicConfigNode() {
        //Ensure a ConfigNodeSingleToken can handle all its required token types
        singleTokenNodeTest(Tokens.START);
        singleTokenNodeTest(Tokens.END);
        singleTokenNodeTest(Tokens.OPEN_CURLY);
        singleTokenNodeTest(Tokens.CLOSE_CURLY);
        singleTokenNodeTest(Tokens.OPEN_SQUARE);
        singleTokenNodeTest(Tokens.CLOSE_SQUARE);
        singleTokenNodeTest(Tokens.COMMA);
        singleTokenNodeTest(Tokens.EQUALS);
        singleTokenNodeTest(Tokens.COLON);
        singleTokenNodeTest(Tokens.PLUS_EQUALS);
        singleTokenNodeTest(tokenUnquoted("             "));
        singleTokenNodeTest(tokenWhitespace("             "));
        singleTokenNodeTest(tokenLine(1))
        singleTokenNodeTest(tokenCommentDoubleSlash("  this is a double slash comment   "));
        singleTokenNodeTest(tokenCommentHash("   this is a hash comment   "));
    }

    @Test
    void createConfigNodeSetting() {
        //Ensure a ConfigNodeSetting can handle the normal key types
        keyNodeTest("foo");
        keyNodeTest("\"Hello I am a key how are you today\"");
    }

    @Test
    void pathNodeSubpath() {
        var origPath = "a.b.c.\"@$%#@!@#$\".\"\".1234.5678";
        var pathNode = configNodeKey(origPath);
        assertEquals(origPath, pathNode.render());
        assertEquals("c.\"@$%#@!@#$\".\"\".1234.5678", pathNode.subPath(2).render());
        assertEquals("5678", pathNode.subPath(6).render());
    }

    @Test
    void createConfigNodeSimpleValue() {
        //Ensure a ConfigNodeSimpleValue can handle the normal value types
        simpleValueNodeTest(tokenInt(10));
        simpleValueNodeTest(tokenLong(10000));
        simpleValueNodeTest(tokenDouble(3.14159));
        simpleValueNodeTest(tokenFalse);
        simpleValueNodeTest(tokenTrue);
        simpleValueNodeTest(tokenNull);
        simpleValueNodeTest(tokenString("Hello my name is string"));
        simpleValueNodeTest(tokenUnquoted("mynameisunquotedstring"));
        simpleValueNodeTest(tokenKeySubstitution("c.d"));
        simpleValueNodeTest(tokenOptionalSubstitution(tokenUnquoted("x.y")));
        simpleValueNodeTest(tokenSubstitution(tokenUnquoted("a.b")));
    }

    @Test
    void createConfigNodeField() {
        // Supports Quoted and Unquoted keys
        fieldNodeTest(configNodeKey("\"abc\""), nodeInt(123), nodeInt(245));
        fieldNodeTest(configNodeKey("abc"), nodeInt(123), nodeInt(245));

        // Can replace value with values of different types
        fieldNodeTest(configNodeKey("\"abc\""), nodeInt(123), nodeString("I am a string"));
        fieldNodeTest(configNodeKey("\"abc\""), nodeInt(123), configNodeObject(List(nodeOpenBrace, nodeCloseBrace)));
    }

    @Test
    void replaceNodes() {
        //Ensure simple values can be replaced by other simple values
        topLevelValueReplaceTest(nodeInt(10), nodeInt(15));
        topLevelValueReplaceTest(nodeLong(10000), nodeInt(20));
        topLevelValueReplaceTest(nodeDouble(3.14159), nodeLong(10000));
        topLevelValueReplaceTest(nodeFalse, nodeTrue);
        topLevelValueReplaceTest(nodeTrue, nodeNull);
        topLevelValueReplaceTest(nodeNull, nodeString("Hello my name is string"));
        topLevelValueReplaceTest(nodeString("Hello my name is string"), nodeUnquotedText("mynameisunquotedstring"));
        topLevelValueReplaceTest(nodeUnquotedText("mynameisunquotedstring"), nodeKeySubstitution("c.d"));
        topLevelValueReplaceTest(nodeInt(10), nodeOptionalSubstitution(tokenUnquoted("x.y")));
        topLevelValueReplaceTest(nodeInt(10), nodeSubstitution(tokenUnquoted("a.b")));
        topLevelValueReplaceTest(nodeSubstitution(tokenUnquoted("a.b")), nodeInt(10));

        // Ensure arrays can be replaced
        var array = configNodeArray(List.of(nodeOpenBracket, nodeInt(10), nodeSpace, nodeComma, nodeSpace, nodeInt(15), nodeCloseBracket));
        topLevelValueReplaceTest(nodeInt(10), array);
        topLevelValueReplaceTest(array, nodeInt(10));
        topLevelValueReplaceTest(array, configNodeObject(List.of(nodeOpenBrace, nodeCloseBrace)));

        // Ensure objects can be replaced
        var nestedMap = configNodeObject(List(nodeOpenBrace,
                nodeKeyValuePair(configNodeKey("abc"), configNodeSimpleValue(tokenString("a string"))),
                nodeCloseBrace));
        topLevelValueReplaceTest(nestedMap, nodeInt(10));
        topLevelValueReplaceTest(nodeInt(10), nestedMap);
        topLevelValueReplaceTest(array, nestedMap);
        topLevelValueReplaceTest(nestedMap, array);
        topLevelValueReplaceTest(nestedMap, configNodeObject(List.of(nodeOpenBrace, nodeCloseBrace)));

        // Ensure concatenations can be replaced
        var concatenation = configNodeConcatenation(List.of(nodeInt(10), nodeSpace, nodeString("Hello")));
        topLevelValueReplaceTest(concatenation, nodeInt(12));
        topLevelValueReplaceTest(nodeInt(12), concatenation);
        topLevelValueReplaceTest(nestedMap, concatenation);
        topLevelValueReplaceTest(concatenation, nestedMap);
        topLevelValueReplaceTest(array, concatenation);
        topLevelValueReplaceTest(concatenation, array);

        //Ensure a key with format "a.b" will be properly replaced
        topLevelValueReplaceTest(nodeInt(10), nestedMap, "foo.bar");
    }

    @Test
    void removeDuplicates() {
        var emptyMapNode = configNodeObject(List(nodeOpenBrace, nodeCloseBrace));
        var emptyArrayNode = configNodeArray(List(nodeOpenBracket, nodeCloseBracket));
        //Ensure duplicates of a key are removed from a map
        replaceDuplicatesTest(nodeInt(10), nodeTrue, nodeNull);
        replaceDuplicatesTest(emptyMapNode, emptyMapNode, emptyMapNode);
        replaceDuplicatesTest(emptyArrayNode, emptyArrayNode, emptyArrayNode);
        replaceDuplicatesTest(nodeInt(10), emptyMapNode, emptyArrayNode);
    }

    @Test
    void addNonExistentPaths() {
        nonExistentPathTest(nodeInt(10));
        nonExistentPathTest(configNodeArray(List(nodeOpenBracket, nodeInt(15), nodeCloseBracket)));
        nonExistentPathTest(configNodeObject(List(nodeOpenBrace, nodeKeyValuePair(configNodeKey("foo"), nodeDouble(3.14)), nodeCloseBrace)));
    }

    @Test
    void replaceNestedNodes() {
        // Test that all features of node replacement in a map work in a complex map containing nested maps
        var origText = "foo : bar\nbaz : {\n\t\"abc.def\" : 123\n\t//This is a comment about the below setting\n\n\tabc : {\n\t\t" +
                "void : \"this is a string\"\n\t\tghi : ${\"a.b\"}\n\t}\n}\nbaz.abc.ghi : 52\nbaz.abc.ghi : 53\n}";
        var lowestLevelMap = configNodeObject(List.of(nodeOpenBrace, nodeLine(6), nodeWhitespace("\t\t"),
                nodeKeyValuePair(configNodeKey("def"), configNodeSimpleValue(tokenString("this is a string"))), nodeLine(7),
                nodeWhitespace("\t\t"), nodeKeyValuePair(configNodeKey("ghi"), configNodeSimpleValue(tokenKeySubstitution("a.b"))), nodeLine(8),
                nodeWhitespace("\t"), nodeCloseBrace));
        var higherLevelMap = configNodeObject(List(nodeOpenBrace, nodeLine(2),
                nodeWhitespace("\t"), nodeKeyValuePair(configNodeKey("\"abc.def\""), configNodeSimpleValue(tokenInt(123))), nodeLine(3),
                nodeWhitespace("\t"), nodeCommentDoubleSlash(("This is a comment about the below setting")),
                nodeLine(4), nodeLine(5),
                nodeWhitespace("\t"), nodeKeyValuePair(configNodeKey("abc"), lowestLevelMap), nodeLine(9),
                nodeCloseBrace));
        var origNode = configNodeObject(List(nodeKeyValuePair(configNodeKey("foo"), configNodeSimpleValue(tokenUnquoted("bar"))), nodeLine(1),
                nodeKeyValuePair(configNodeKey("baz"), higherLevelMap), nodeLine(10),
                nodeKeyValuePair(configNodeKey("baz.abc.ghi"), configNodeSimpleValue(tokenInt(52))), nodeLine(11),
                nodeKeyValuePair(configNodeKey("baz.abc.ghi"), configNodeSimpleValue(tokenInt(53))), nodeLine(12),
                nodeCloseBrace));
        assertEquals(origText, origNode.render());
        var finalText = "foo : bar\nbaz : {\n\t\"abc.def\" : true\n\t//This is a comment about the below setting\n\n\tabc : {\n\t\t" +
                "void : false\n\t\t\n\t\t\"this.does.not.exist@@@+$#\" : {\n\t\t  end : doesnotexist\n\t\t}\n\t}\n}\n\nbaz.abc.ghi : randomunquotedString\n}";

        //Can replace settings in nested maps
        // Paths with quotes in the name are treated as a single Path, rather than multiple sub-paths
        var newNode = origNode.setValueOnPath("baz.\"abc.def\"", configNodeSimpleValue(tokenTrue));
        newNode = newNode.setValueOnPath("baz.abc.def", configNodeSimpleValue(tokenFalse));

        // Repeats are removed from nested maps
        newNode = newNode.setValueOnPath("baz.abc.ghi", configNodeSimpleValue(tokenUnquoted("randomunquotedString")));

        // Missing paths are added to the top level if they don't appear anywhere, including in nested maps
        newNode = newNode.setValueOnPath("baz.abc.\"this.does.not.exist@@@+$#\".end", configNodeSimpleValue(tokenUnquoted("doesnotexist")));

        // The above operations cause the resultant map to be rendered properly
        assertEquals(finalText, newNode.render());
    }
}
