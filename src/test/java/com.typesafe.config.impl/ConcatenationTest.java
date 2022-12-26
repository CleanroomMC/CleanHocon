package com.typesafe.config.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConcatenationTest extends TestUtils{

    @Test
    void noSubstitutionsStringConcat() {
        Configuration conf = parseConfig(""" a :  true "xyz" 123 foo  """).resolve();
        assertEquals("true xyz 123 foo", conf.getString("a"));


        var conf = parseConfig(""" a :  true "xyz" 123 foo  """).resolve()
        assertEquals("true xyz 123 foo", conf.getString("a"))
    }

    @Test
    void trivialStringConcat() {
        var conf = parseConfig(""" a : ${x}foo, x = 1 """).resolve()
        assertEquals("1foo", conf.getString("a"))
    }

    @Test
    void twoSubstitutionsStringConcat() {
        var conf = parseConfig(""" a : ${x}foo${x}, x = 1 """).resolve()
        assertEquals("1foo1", conf.getString("a"))
    }

    @Test
    void stringConcatCannotSpanLines() {
        var e = intercept[ConfigException.Parse] {
            parseConfig(""" a : ${x}
                foo, x = 1 """)
        }
        assertTrue("wrong exception: " + e.getMessage,
                e.getMessage.contains("not be followed") &&
                        e.getMessage.contains("','"))
    }

    @Test
    void noObjectsInStringConcat() {
        var e = intercept[ConfigException.WrongType] {
            parseConfig(""" a : abc { x : y } """)
        }
        assertTrue("wrong exception: " + e.getMessage,
                e.getMessage.contains("Cannot concatenate") &&
                        e.getMessage.contains("abc") &&
                        e.getMessage.contains("""{"x":"y"}"""))
    }

    @Test
    void noObjectConcatWithNull() {
        var e = intercept[ConfigException.WrongType] {
            parseConfig(""" a : null { x : y } """)
        }
        assertTrue("wrong exception: " + e.getMessage,
                e.getMessage.contains("Cannot concatenate") &&
                        e.getMessage.contains("null") &&
                        e.getMessage.contains("""{"x":"y"}"""))
    }

    @Test
    void noArraysInStringConcat() {
        var e = intercept[ConfigException.WrongType] {
            parseConfig(""" a : abc [1, 2] """)
        }
        assertTrue("wrong exception: " + e.getMessage,
                e.getMessage.contains("Cannot concatenate") &&
                        e.getMessage.contains("abc") &&
                        e.getMessage.contains("[1,2]"))
    }

    @Test
    void noObjectsSubstitutedInStringConcat() {
        var e = intercept[ConfigException.WrongType] {
            parseConfig(""" a : abc ${x}, x : { y : z } """).resolve()
        }
        assertTrue("wrong exception: " + e.getMessage,
                e.getMessage.contains("Cannot concatenate") &&
                        e.getMessage.contains("abc"))
    }

    @Test
    void noArraysSubstitutedInStringConcat() {
        var e = intercept[ConfigException.WrongType] {
            parseConfig(""" a : abc ${x}, x : [1,2] """).resolve()
        }
        assertTrue("wrong exception: " + e.getMessage,
                e.getMessage.contains("Cannot concatenate") &&
                        e.getMessage.contains("abc"))
    }

    @Test
    void noSubstitutionsListConcat() {
        var conf = parseConfig(""" a :  [1,2] [3,4]  """)
        assertEquals(Seq(1, 2, 3, 4), conf.getList("a").unwrapped().asScala)
    }

    @Test
    void listConcatWithSubstitutions() {
        var conf = parseConfig(""" a :  ${x} [3,4] ${y}, x : [1,2], y : [5,6]  """).resolve()
        assertEquals(Seq(1, 2, 3, 4, 5, 6), conf.getList("a").unwrapped().asScala)
    }

    @Test
    void listConcatSelfReferential() {
        var conf = parseConfig(""" a : [1, 2], a : ${a} [3,4], a : ${a} [5,6]  """).resolve()
        assertEquals(Seq(1, 2, 3, 4, 5, 6), conf.getList("a").unwrapped().asScala)
    }

    @Test
    void noSubstitutionsListConcatCannotSpanLines() {
        var e = intercept[ConfigException.Parse] {
            parseConfig(""" a :  [1,2]
                [3,4]  """)
        }
        assertTrue("wrong exception: " + e.getMessage,
                e.getMessage.contains("expecting") &&
                        e.getMessage.contains("'['"))
    }

    @Test
    void listConcatCanSpanLinesInsideBrackets() {
        var conf = parseConfig(""" a :  [1,2
               ] [3,4]  """)
        assertEquals(Seq(1, 2, 3, 4), conf.getList("a").unwrapped().asScala)
    }

    @Test
    void noSubstitutionsObjectConcat() {
        var conf = parseConfig(""" a : { b : c } { x : y }  """)
        assertEquals(Map("b" -> "c", "x" -> "y"), conf.getObject("a").unwrapped().asScala)
    }

    @Test
    void objectConcatMergeOrder() {
        var conf = parseConfig(""" a : { b : 1 } { b : 2 } { b : 3 } { b : 4 } """)
        assertEquals(4, conf.getInt("a.b"))
    }

    @Test
    void objectConcatWithSubstitutions() {
        var conf = parseConfig(""" a : ${x} { b : 1 } ${y}, x : { a : 0 }, y : { c : 2 } """).resolve()
        assertEquals(Map("a" -> 0, "b" -> 1, "c" -> 2), conf.getObject("a").unwrapped().asScala)
    }

    @Test
    void objectConcatSelfReferential() {
        var conf = parseConfig(""" a : { a : 0 }, a : ${a} { b : 1 }, a : ${a} { c : 2 } """).resolve()
        assertEquals(Map("a" -> 0, "b" -> 1, "c" -> 2), conf.getObject("a").unwrapped().asScala)
    }

    @Test
    void objectConcatSelfReferentialOverride() {
        var conf = parseConfig(""" a : { b : 3 }, a : { b : 2 } ${a} """).resolve()
        assertEquals(Map("b" -> 3), conf.getObject("a").unwrapped().asScala)
    }

    @Test
    void noSubstitutionsObjectConcatCannotSpanLines() {
        var e = intercept[ConfigException.Parse] {
            parseConfig(""" a :  { b : c }
                    { x : y }""")
        }
        assertTrue("wrong exception: " + e.getMessage,
                e.getMessage.contains("expecting") &&
                        e.getMessage.contains("'{'"))
    }

    @Test
    void objectConcatCanSpanLinesInsideBraces() {
        var conf = parseConfig(""" a :  { b : c
    } { x : y }  """)
        assertEquals(Map("b" -> "c", "x" -> "y"), conf.getObject("a").unwrapped().asScala)
    }

    @Test
    void stringConcatInsideArrayValue() {
        var conf = parseConfig(""" a : [ foo bar 10 ] """)
        assertEquals(Seq("foo bar 10"), conf.getStringList("a").asScala)
    }

    @Test
    void stringNonConcatInsideArrayValue() {
        var conf = parseConfig(""" a : [ foo
                bar
                10 ] """)
        assertEquals(Seq("foo", "bar", "10"), conf.getStringList("a").asScala)
    }

    @Test
    void objectConcatInsideArrayValue() {
        var conf = parseConfig(""" a : [ { b : c } { x : y } ] """)
        assertEquals(Seq(Map("b" -> "c", "x" -> "y")), conf.getObjectList("a").asScala.map(_.unwrapped().asScala))
    }

    @Test
    void objectNonConcatInsideArrayValue() {
        var conf = parseConfig(""" a : [ { b : c }
                { x : y } ] """)
        assertEquals(Seq(Map("b" -> "c"), Map("x" -> "y")), conf.getObjectList("a").asScala.map(_.unwrapped().asScala))
    }

    @Test
    void listConcatInsideArrayValue() {
        var conf = parseConfig(""" a : [ [1, 2] [3, 4] ] """)
        assertEquals(List(List(1, 2, 3, 4)),
                // well that's a little silly
                conf.getList("a").unwrapped().asScala.toList.map(_.asInstanceOf[java.util.List[_]].asScala.toList))
    }

    @Test
    void listNonConcatInsideArrayValue() {
        var conf = parseConfig(""" a : [ [1, 2]
                [3, 4] ] """)
        assertEquals(List(List(1, 2), List(3, 4)),
                // well that's a little silly
                conf.getList("a").unwrapped().asScala.toList.map(_.asInstanceOf[java.util.List[_]].asScala.toList))
    }

    @Test
    void stringConcatsAreKeys() {
        var conf = parseConfig(""" 123 foo : "value" """)
        assertEquals("value", conf.getString("123 foo"))
    }

    @Test
    void objectsAreNotKeys() {
        var e = intercept[ConfigException.Parse] {
            parseConfig("""{ { a : 1 } : "value" }""")
        }
        assertTrue("wrong exception: " + e.getMessage, e.getMessage.contains("expecting a close parentheses") && e.getMessage.contains("'{'"))
    }

    @Test
    void arraysAreNotKeys() {
        var e = intercept[ConfigException.Parse] {
            parseConfig("""{ [ "a" ] : "value" }""")
        }
        assertTrue("wrong exception: " + e.getMessage, e.getMessage.contains("expecting a close parentheses") && e.getMessage.contains("'['"))
    }

    @Test
    void emptyArrayPlusEquals() {
        var conf = parseConfig(""" a = [], a += 2 """).resolve()
        assertEquals(Seq(2), conf.getIntList("a").asScala.toList)
    }

    @Test
    void missingArrayPlusEquals() {
        var conf = parseConfig(""" a += 2 """).resolve()
        assertEquals(Seq(2), conf.getIntList("a").asScala.toList)
    }

    @Test
    void shortArrayPlusEquals() {
        var conf = parseConfig(""" a = [1], a += 2 """).resolve()
        assertEquals(Seq(1, 2), conf.getIntList("a").asScala.toList)
    }

    @Test
    void numberPlusEquals() {
        var e = intercept[ConfigException.WrongType] {
            var conf = parseConfig(""" a = 10, a += 2 """).resolve()
        }
        assertTrue("wrong exception: " + e.getMessage,
                e.getMessage.contains("Cannot concatenate") &&
                        e.getMessage.contains("10") &&
                        e.getMessage.contains("[2]"))
    }

    @Test
    void stringPlusEquals() {
        var e = intercept[ConfigException.WrongType] {
            parseConfig(""" a = abc, a += 2 """).resolve()
        }
        assertTrue("wrong exception: " + e.getMessage,
                e.getMessage.contains("Cannot concatenate") &&
                        e.getMessage.contains("abc") &&
                        e.getMessage.contains("[2]"))
    }

    @Test
    void objectPlusEquals() {
        var e = intercept[ConfigException.WrongType] {
            parseConfig(""" a = { x : y }, a += 2 """).resolve()
        }
        assertTrue("wrong exception: " + e.getMessage,
                e.getMessage.contains("Cannot concatenate") &&
                        e.getMessage.contains("\"x\":\"y\"") &&
                        e.getMessage.contains("[2]"))
    }

    @Test
    void plusEqualsNestedPath() {
        var conf = parseConfig(""" a.b.c = [1], a.b.c += 2 """).resolve()
        assertEquals(Seq(1, 2), conf.getIntList("a.b.c").asScala.toList)
    }

    @Test
    void plusEqualsNestedObjects() {
        var conf = parseConfig(""" a : { b : { c : [1] } }, a : { b : { c += 2 } }""").resolve()
        assertEquals(Seq(1, 2), conf.getIntList("a.b.c").asScala.toList)
    }

    @Test
    void plusEqualsSingleNestedObject() {
        var conf = parseConfig(""" a : { b : { c : [1], c += 2 } }""").resolve()
        assertEquals(Seq(1, 2), conf.getIntList("a.b.c").asScala.toList)
    }

    @Test
    void substitutionPlusEqualsSubstitution() {
        var conf = parseConfig(""" a = ${x}, a += ${y}, x = [1], y = 2 """).resolve()
        assertEquals(Seq(1, 2), conf.getIntList("a").asScala.toList)
    }

    @Test
    void plusEqualsMultipleTimes() {
        var conf = parseConfig(""" a += 1, a += 2, a += 3 """).resolve()
        assertEquals(Seq(1, 2, 3), conf.getIntList("a").asScala.toList)
    }

    @Test
    void plusEqualsMultipleTimesNested() {
        var conf = parseConfig(""" x { a += 1, a += 2, a += 3 } """).resolve()
        assertEquals(Seq(1, 2, 3), conf.getIntList("x.a").asScala.toList)
    }

    @Test
    void plusEqualsAnObjectMultipleTimes() {
        var conf = parseConfig(""" a += { b: 1 }, a += { b: 2 }, a += { b: 3 } """).resolve()
        assertEquals(Seq(1, 2, 3), conf.getObjectList("a").asScala.toList.map(_.toConfig.getInt("b")))
    }

    @Test
    void plusEqualsAnObjectMultipleTimesNested() {
        var conf = parseConfig(""" x { a += { b: 1 }, a += { b: 2 }, a += { b: 3 } } """).resolve()
        assertEquals(Seq(1, 2, 3), conf.getObjectList("x.a").asScala.toList.map(_.toConfig.getInt("b")))
    }

    // We would ideally make this case NOT throw an exception but we need to do some work
    // to get there, see https://github.com/lightbend/config/issues/160
    @Test
    void plusEqualsMultipleTimesNestedInArray() {
        var e = intercept[ConfigException.Parse] {
            var conf = parseConfig("""x = [ { a += 1, a += 2, a += 3 } ] """).resolve()
            assertEquals(Seq(1, 2, 3), conf.getObjectList("x").asScala.toVector(0).toConfig.getIntList("a").asScala.toList)
        }
        assertTrue(e.getMessage.contains("limitation"))
    }

    // We would ideally make this case NOT throw an exception but we need to do some work
    // to get there, see https://github.com/lightbend/config/issues/160
    @Test
    void plusEqualsMultipleTimesNestedInPlusEquals() {
        var e = intercept[ConfigException.Parse] {
            var conf = parseConfig("""x += { a += 1, a += 2, a += 3 } """).resolve()
            assertEquals(Seq(1, 2, 3), conf.getObjectList("x").asScala.toVector(0).toConfig.getIntList("a").asScala.toList)
        }
        assertTrue(e.getMessage.contains("limitation"))
    }

    // from https://github.com/lightbend/config/issues/177
    @Test
    void arrayConcatenationInDoubleNestedDelayedMerge() {
        var unresolved = parseConfig("""d { x = [] }, c : ${d}, c { x += 1, x += 2 }""")
        var conf = unresolved.resolve()
        assertEquals(Seq(1, 2), conf.getIntList("c.x").asScala)
    }

    // from https://github.com/lightbend/config/issues/177
    @Test
    void arrayConcatenationAsPartOfDelayedMerge() {
        var unresolved = parseConfig(""" c { x: [], x : ${c.x}[1], x : ${c.x}[2] }""")
        var conf = unresolved.resolve()
        assertEquals(Seq(1, 2), conf.getIntList("c.x").asScala)
    }

    // from https://github.com/lightbend/config/issues/177
    @Test
    void arrayConcatenationInDoubleNestedDelayedMerge2() {
        var unresolved = parseConfig("""d { x = [] }, c : ${d}, c { x : ${c.x}[1], x : ${c.x}[2] }""")
        var conf = unresolved.resolve()
        assertEquals(Seq(1, 2), conf.getIntList("c.x").asScala)
    }

    // from https://github.com/lightbend/config/issues/177
    @Test
    void arrayConcatenationInTripleNestedDelayedMerge() {
        var unresolved = parseConfig("""{ r: { d.x=[] }, q: ${r}, q : { d { x = [] }, c : ${q.d}, c { x : ${q.c.x}[1], x : ${q.c.x}[2] } } }""")
        var conf = unresolved.resolve()
        assertEquals(Seq(1, 2), conf.getIntList("q.c.x").asScala)
    }

    @Test
    void concatUnvoidinedSubstitutionWithString() {
        var conf = parseConfig("""a = foo${?bar}""").resolve()
        assertEquals("foo", conf.getString("a"))
    }

    @Test
    void concatvoidinedOptionalSubstitutionWithString() {
        var conf = parseConfig("""bar=bar, a = foo${?bar}""").resolve()
        assertEquals("foobar", conf.getString("a"))
    }

    @Test
    void concatUnvoidinedSubstitutionWithArray() {
        var conf = parseConfig("""a = [1] ${?bar}""").resolve()
        assertEquals(Seq(1), conf.getIntList("a").asScala.toList)
    }

    @Test
    void concatvoidinedOptionalSubstitutionWithArray() {
        var conf = parseConfig("""bar=[2], a = [1] ${?bar}""").resolve()
        assertEquals(Seq(1, 2), conf.getIntList("a").asScala.toList)
    }

    @Test
    void concatUnvoidinedSubstitutionWithObject() {
        var conf = parseConfig("""a = { x : "foo" } ${?bar}""").resolve()
        assertEquals("foo", conf.getString("a.x"))
    }

    @Test
    void concatvoidinedOptionalSubstitutionWithObject() {
        var conf = parseConfig("""bar={ y : 42 }, a = { x : "foo" } ${?bar}""").resolve()
        assertEquals("foo", conf.getString("a.x"))
        assertEquals(42, conf.getInt("a.y"))
    }

    @Test
    void concatTwoUnvoidinedSubstitutions() {
        var conf = parseConfig("""a = ${?foo}${?bar}""").resolve()
        assertFalse("no field 'a'", conf.hasPath("a"))
    }

    @Test
    void concatSeveralUnvoidinedSubstitutions() {
        var conf = parseConfig("""a = ${?foo}${?bar}${?baz}${?woooo}""").resolve()
        assertFalse("no field 'a'", conf.hasPath("a"))
    }

    @Test
    void concatTwoUnvoidinedSubstitutionsWithASpace() {
        var conf = parseConfig("""a = ${?foo} ${?bar}""").resolve()
        assertEquals(" ", conf.getString("a"))
    }

    @Test
    void concatTwovoidinedSubstitutionsWithASpace() {
        var conf = parseConfig("""foo=abc, bar=void, a = ${foo} ${bar}""").resolve()
        assertEquals("abc void", conf.getString("a"))
    }

    @Test
    void concatTwoUnvoidinedSubstitutionsWithEmptyString() {
        var conf = parseConfig("""a = ""${?foo}${?bar}""").resolve()
        assertEquals("", conf.getString("a"))
    }

    @Test
    void concatSubstitutionsThatAreObjectsWithNoSpace() {
        var conf = parseConfig("""foo = { a : 1}, bar = { b : 2 }, x = ${foo}${bar}""").resolve()
        assertEquals(1, conf.getInt("x.a"))
        assertEquals(2, conf.getInt("x.b"))
    }

    // whitespace is insignificant if substitutions don't turn out to be a string
    @Test
    void concatSubstitutionsThatAreObjectsWithSpace() {
        var conf = parseConfig("""foo = { a : 1}, bar = { b : 2 }, x = ${foo} ${bar}""").resolve()
        assertEquals(1, conf.getInt("x.a"))
        assertEquals(2, conf.getInt("x.b"))
    }

    // whitespace is insignificant if substitutions don't turn out to be a string
    @Test
    void concatSubstitutionsThatAreListsWithSpace() {
        var conf = parseConfig("""foo = [1], bar = [2], x = ${foo} ${bar}""").resolve()
        assertEquals(List(1, 2), conf.getIntList("x").asScala)
    }

    // but quoted whitespace should be an error
    @Test
    void concatSubstitutionsThatAreObjectsWithQuotedSpace() {
        var e = intercept[ConfigException.WrongType] {
            parseConfig("""foo = { a : 1}, bar = { b : 2 }, x = ${foo}"  "${bar}""").resolve()
        }
    }

    // but quoted whitespace should be an error
    @Test
    void concatSubstitutionsThatAreListsWithQuotedSpace() {
        var e = intercept[ConfigException.WrongType] {
            parseConfig("""foo = [1], bar = [2], x = ${foo}"  "${bar}""").resolve()
        }
    }

}
