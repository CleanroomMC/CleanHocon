package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigResolveOptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigSubstitutionTest {
    private SimpleConfig resolveWithoutFallbacks(AbstractConfigObject v) {
        var options = ConfigResolveOptions.noSystem();
        return ((AbstractConfigObject) ResolveContext.resolve(v, v, options)).toConfig();
    }

    private AbstractConfigValue resolveWithoutFallbacks(AbstractConfigValue s, AbstractConfigObject root) {
        var options = ConfigResolveOptions.noSystem();
        return ResolveContext.resolve(s, root, options);
    }

    private SimpleConfig resolve(AbstractConfigObject v) {
        var options = ConfigResolveOptions.defaults();
        return ((AbstractConfigObject) ResolveContext.resolve(v, v, options)).toConfig();
    }

    private AbstractConfigValue resolve(AbstractConfigValue s, AbstractConfigObject root) {
        var options = ConfigResolveOptions.defaults();
        return ResolveContext.resolve(s, root, options);
    }

    private void simpleObject() {
        return parseObject("""
                {
                   "foo" : 42,
                   "bar" : {
                       "int" : 43,
                       "bool" : true,
                       "null" : null,
                       "string" : "hello",
                       "double" : 3.14
                    }
                }
                """);
    }

    @Test
    void resolveTrivialKey() {
        var s = subst("foo");
        var v = resolveWithoutFallbacks(s, simpleObject());
        assertEquals(intValue(42), v);
    }

    @Test
    void resolveTrivialPath() {
        var s = subst("bar.int");
        var v = resolveWithoutFallbacks(s, simpleObject());
        assertEquals(intValue(43), v);
    }

    @Test
    void resolveInt() {
        var s = subst("bar.int");
        var v = resolveWithoutFallbacks(s, simpleObject());
        assertEquals(intValue(43), v);
    }

    @Test
    void resolveBool() {
        var s = subst("bar.bool");
        var v = resolveWithoutFallbacks(s, simpleObject());
        assertEquals(boolValue(true), v);
    }

    @Test
    void resolveNull() {
        var s = subst("bar.null");
        var v = resolveWithoutFallbacks(s, simpleObject());
        assertEquals(nullValue, v);
    }

    @Test
    void resolveString() {
        var s = subst("bar.string");
        var v = resolveWithoutFallbacks(s, simpleObject());
        assertEquals(stringValue("hello"), v);
    }

    @Test
    void resolveDouble() {
        var s = subst("bar.double");
        var v = resolveWithoutFallbacks(s, simpleObject());
        assertEquals(doubleValue(3.14), v);
    }

    @Test
    void resolveMissingThrows() {
        var e = intercept[ConfigException.UnresolvedSubstitution] {
            var s = subst("bar.missing");
            var v = resolveWithoutFallbacks(s, simpleObject());
        }
        assertTrue("wrong exception: " + e.getMessage()
        !e.getMessage().contains("cycle"));
    }

    @Test
    void resolveIntInString() {
        var s = substInString("bar.int");
        var v = resolveWithoutFallbacks(s, simpleObject());
        assertEquals(stringValue("start<43>end"), v);
    }

    @Test
    void resolveNullInString() {
        var s = substInString("bar.null");
        var v = resolveWithoutFallbacks(s, simpleObject());
        assertEquals(stringValue("start<null>end"), v);

        // when null is NOT a subst, it should also not become empty
        var o = parseConfig("""
                { "a" : null foo bar }""");
        assertEquals("null foo bar", o.getString("a"));
    }

    @Test
    void resolveMissingInString() {
        var s = substInString("bar.missing", optional = true);
        var v = resolveWithoutFallbacks(s, simpleObject);
        // absent object becomes empty string
        assertEquals(stringValue("start<>end"), v);

        intercept[ConfigException.UnresolvedSubstitution] {
            var s2 = substInString("bar.missing", optional = false)
            resolveWithoutFallbacks(s2, simpleObject());
        }
    }

    @Test
    void resolveBoolInString() {
        var s = substInString("bar.bool");
        var v = resolveWithoutFallbacks(s, simpleObject());
        assertEquals(stringValue("start<true>end"), v);
    }

    @Test
    void resolveStringInString() {
        var s = substInString("bar.string");
        var v = resolveWithoutFallbacks(s, simpleObject());
        assertEquals(stringValue("start<hello>end"), v);
    }

    @Test
    void resolveDoubleInString() {
        var s = substInString("bar.double");
        var v = resolveWithoutFallbacks(s, simpleObject());
        assertEquals(stringValue("start<3.14>end"), v);
    }

    @Test
    void missingInArray() {
        var obj = parseObject("""
                    a : [ ${?missing}, ${?also.missing} ]
                """);

        var resolved = resolve(obj);

        assertEquals(Seq(), resolved.getList("a").asScala);
    }

    @Test
    void missingInObject() {
        var obj = parseObject("""
                    a : ${?missing}, b : ${?also.missing}, c : ${?b}, d : ${?c}
                """);

        var resolved = resolve(obj);
        assertTrue(resolved.isEmpty());
    }

    private void substChainObject() {
        return parseObject("""
                {
                    "foo" : ${bar},
                    "bar" : ${a.b.c},
                    "a" : { "b" : { "c" : 57 } }
                }
                """);
    }

    @Test
    void chainSubstitutions() {
        var s = subst("foo");
        var v = resolveWithoutFallbacks(s, substChainObject);
        assertEquals(intValue(57), v);
    }

    @Test
    void substitutionsLookForward() {
        var obj = parseObject("""a=1,b=${a},a=2""");
        var resolved = resolve(obj);
        assertEquals(2, resolved.getInt("b"));
    }

    @Test
    void throwOnIncrediblyTrivialCycle() {
        var s = subst("a");
        var e = intercept[ConfigException.UnresolvedSubstitution] {
            var v = resolveWithoutFallbacks(s, parseObject("a: ${a}"));
        }
        assertTrue("Wrong exception: " + e.getMessage(), e.getMessage().contains("cycle"));
        assertTrue("Wrong exception: " + e.getMessage(), e.getMessage().contains("${a}"));
    }

    private void substCycleObject() {
        return parseObject("""
                {
                    "foo" : ${bar},
                    "bar" : ${a.b.c},
                    "a" : { "b" : { "c" : ${foo} } }
                }
                """);
    }

    @Test
    void throwOnCycles() {
        var s = subst("foo");
        var e = intercept[ConfigException.UnresolvedSubstitution] {
            var v = resolveWithoutFallbacks(s, substCycleObject);
        }
        assertTrue("Wrong exception: " + e.getMessage(), e.getMessage().contains("cycle"));
        assertTrue("Wrong exception: " + e.getMessage(), e.getMessage().contains("${foo}, ${bar}, ${a.b.c}, ${foo}"));
    }

    @Test
    void throwOnOptionalReferenceToNonOptionalCycle() {
        // we look up ${?foo}, but the cycle has hard
        // non-optional links in it so still has to throw.
        var s = subst("foo", optional = true);
        var e = intercept[ConfigException.UnresolvedSubstitution] {
            var v = resolveWithoutFallbacks(s, substCycleObject);
        }
        assertTrue("Wrong exception: " + e.getMessage(), e.getMessage().contains("cycle"));
    }

    // ALL the links have to be optional here for the cycle to be ignored
    private void substCycleObjectOptionalLink() {
        return parseObject("""
                {
                    "foo" : ${?bar},
                    "bar" : ${?a.b.c},
                    "a" : { "b" : { "c" : ${?foo} } }
                }
                """);
    }

    @Test
    void optionalLinkCyclesActLikeUndefined() {
        var s = subst("foo", optional = true);
        var v = resolveWithoutFallbacks(s, substCycleObjectOptionalLink);
        assertNull(v, "Cycle with optional links in it resolves to null if it's a cycle");
    }

    @Test
    void throwOnTwoKeyCycle() {
        var obj = parseObject("""a:${b},b:${a}""");
        var e = intercept[ConfigException.UnresolvedSubstitution] {
            resolve(obj);
        }
        assertTrue("Wrong exception: " + e.getMessage(), e.getMessage().contains("cycle"));
    }

    @Test
    void throwOnFourKeyCycle() {
        var obj = parseObject("""a:${b},b:${c},c:${d},d:${a}""")
        var e = intercept[ConfigException.UnresolvedSubstitution] {
            resolve(obj);
        }
        assertTrue("Wrong exception: " + e.getMessage(), e.getMessage().contains("cycle"));
    }

    @Test
    void resolveObject() {
        var resolved = resolveWithoutFallbacks(substChainObject);
        assertEquals(57, resolved.getInt("foo"));
        assertEquals(57, resolved.getInt("bar"));
        assertEquals(57, resolved.getInt("a.b.c"));
    }

    private void substSideEffectCycle() {
        return parseObject("""
                {
                    "foo" : ${a.b.c},
                    "a" : { "b" : { "c" : 42, "cycle" : ${foo} }, "cycle" : ${foo} }
                }
                """);
    }

    @Test
    void avoidSideEffectCycles() {
        // The point of this test is that in traversing objects
        // to resolve a path, we need to avoid resolving
        // substitutions that are in the traversed objects but
        // are not directly required to resolve the path.
        // i.e. there should not be a cycle in this test.

        var resolved = resolveWithoutFallbacks(substSideEffectCycle);

        assertEquals(42, resolved.getInt("foo"));
        assertEquals(42, resolved.getInt("a.b.cycle"));
        assertEquals(42, resolved.getInt("a.cycle"));
    }

    @Test
    void ignoreHiddenUndefinedSubst() {
        // if a substitution is overridden then it shouldn't matter that it's undefined
        var obj = parseObject("""
                a=${nonexistent},a=42""");
        var resolved = resolve(obj);
        assertEquals(42, resolved.getInt("a"));
    }

    @Test
    void objectDoesNotHideUndefinedSubst() {
        // if a substitution is overridden by an object we still need to
        // evaluate the substitution
        var obj = parseObject("""
                a=${nonexistent},a={ b : 42 }""");
        var e = intercept[ConfigException.UnresolvedSubstitution] {
            resolve(obj);
        }
        assertTrue("wrong exception: " + e.getMessage(), e.getMessage.contains("Could not resolve"));
    }

    @Test
    void ignoreHiddenCircularSubst() {
        // if a substitution is overridden then it shouldn't matter that it's circular
        var obj = parseObject("""
                a=${a},a=42""");
        var resolved = resolve(obj);
        assertEquals(42, resolved.getInt("a"));
    }

    private void delayedMergeObjectResolveProblem1() {
        return parseObject("""
                  defaults {
                    a = 1
                    b = 2
                  }
                  // make item1 into a ConfigDelayedMergeObject
                  item1 = ${defaults}
                  // note that we'll resolve to a non-object value
                  // so item1.b will ignoreFallbacks and not depend on
                  // ${defaults}
                  item1.b = 3
                  // be sure we can resolve a substitution to a value in
                  // a delayed-merge object.
                  item2.b = ${item1.b}
                """);
    }

    @Test
    void avoidDelayedMergeObjectResolveProblem1() {
        assertTrue(delayedMergeObjectResolveProblem1().attemptPeekWithPartialResolve("item1") instanceof ConfigDelayedMergeObject);

        var resolved = resolveWithoutFallbacks(delayedMergeObjectResolveProblem1());

        assertEquals(3, resolved.getInt("item1.b"));
        assertEquals(3, resolved.getInt("item2.b"));
    }

    private void delayedMergeObjectResolveProblem2() {
        return parseObject("""
                  defaults {
                    a = 1
                    b = 2
                  }
                  // make item1 into a ConfigDelayedMergeObject
                  item1 = ${defaults}
                  // note that we'll resolve to an object value
                  // so item1.b will depend on also looking up ${defaults}
                  item1.b = { c : 43 }
                  // be sure we can resolve a substitution to a value in
                  // a delayed-merge object.
                  item2.b = ${item1.b}
                """);
    }

    @Test
    void avoidDelayedMergeObjectResolveProblem2() {
        assertTrue(delayedMergeObjectResolveProblem2().attemptPeekWithPartialResolve("item1") instanceof ConfigDelayedMergeObject);

        var resolved = resolveWithoutFallbacks(delayedMergeObjectResolveProblem2());

        assertEquals(parseObject("{ c : 43 }"), resolved.getObject("item1.b"));
        assertEquals(43, resolved.getInt("item1.b.c"));
        assertEquals(43, resolved.getInt("item2.b.c"));
    }

    // in this case, item1 is self-referential because
    // it refers to ${defaults} which refers back to
    // ${item1}. When self-referencing, only the
    // value of ${item1} "looking back" should be
    // visible. This is really a test of the
    // self-referencing semantics.
    private void delayedMergeObjectResolveProblem3() {
        return parseObject("""
                  item1.b.c = 100
                  defaults {
                    // we depend on item1.b.c
                    a = ${item1.b.c}
                    b = 2
                  }
                  // make item1 into a ConfigDelayedMergeObject
                  item1 = ${defaults}
                  // the ${item1.b.c} above in ${defaults} should ignore
                  // this because it only looks back
                  item1.b = { c : 43 }
                  // be sure we can resolve a substitution to a value in
                  // a delayed-merge object.
                  item2.b = ${item1.b}
                """);
    }

    @Test
    void avoidDelayedMergeObjectResolveProblem3() {
        assertTrue(delayedMergeObjectResolveProblem3().attemptPeekWithPartialResolve("item1") instanceof ConfigDelayedMergeObject);

        var resolved = resolveWithoutFallbacks(delayedMergeObjectResolveProblem3());

        assertEquals(parseObject("{ c : 43 }"), resolved.getObject("item1.b"));
        assertEquals(43, resolved.getInt("item1.b.c"));
        assertEquals(43, resolved.getInt("item2.b.c"));
        assertEquals(100, resolved.getInt("defaults.a"));
    }

    private void delayedMergeObjectResolveProblem4() {
        return parseObject("""
                  defaults {
                    a = 1
                    b = 2
                  }

                  item1.b = 7
                  // make item1 into a ConfigDelayedMerge
                  item1 = ${defaults}
                  // be sure we can resolve a substitution to a value in
                  // a delayed-merge object.
                  item2.b = ${item1.b}
                """);
    }

    @Test
    void avoidDelayedMergeObjectResolveProblem4() {
        // in this case we have a ConfigDelayedMerge not a ConfigDelayedMergeObject
        assertTrue(delayedMergeObjectResolveProblem4.attemptPeekWithPartialResolve("item1") instanceof ConfigDelayedMerge);

        var resolved = resolveWithoutFallbacks(delayedMergeObjectResolveProblem4());

        assertEquals(2, resolved.getInt("item1.b"));
        assertEquals(2, resolved.getInt("item2.b"));
    }

    private void delayedMergeObjectResolveProblem5() {
        return parseObject("""
                  defaults {
                    a = ${item1.b} // tricky cycle - we won't see ${defaults}
                                   // as we resolve this
                    b = 2
                  }

                  item1.b = 7
                  // make item1 into a ConfigDelayedMerge
                  item1 = ${defaults}
                  // be sure we can resolve a substitution to a value in
                  // a delayed-merge object.
                  item2.b = ${item1.b}
                """);
    }

    @Test
    void avoidDelayedMergeObjectResolveProblem5() {
        // in this case we have a ConfigDelayedMerge not a ConfigDelayedMergeObject
        assertTrue(delayedMergeObjectResolveProblem5().attemptPeekWithPartialResolve("item1") instanceof ConfigDelayedMerge);

        var resolved = resolveWithoutFallbacks(delayedMergeObjectResolveProblem5());

        assertEquals(2, resolved.getInt("item1.b"), "item1.b");
        assertEquals(2, resolved.getInt("item2.b"), "item2.b");
        assertEquals(7, resolved.getInt("defaults.a"), "defaults.a");
    }

    private void delayedMergeObjectResolveProblem6() {
        return parseObject("""
                  z = 15
                  defaults-defaults-defaults {
                    m = ${z}
                    n.o.p = ${z}
                  }
                  defaults-defaults {
                    x = 10
                    y = 11
                    asdf = ${z}
                  }
                  defaults {
                    a = 1
                    b = 2
                  }
                  defaults-alias = ${defaults}
                  // make item1 into a ConfigDelayedMergeObject several layers deep
                  // that will NOT become resolved just because we resolve one path
                  // through it.
                  item1 = 345
                  item1 = ${?NONEXISTENT}
                  item1 = ${defaults-defaults-defaults}
                  item1 = {}
                  item1 = ${defaults-defaults}
                  item1 = ${defaults-alias}
                  item1 = ${defaults}
                  item1.b = { c : 43 }
                  item1.xyz = 101
                  // be sure we can resolve a substitution to a value in
                  // a delayed-merge object.
                  item2.b = ${item1.b}
                """);
    }

    @Test
    void avoidDelayedMergeObjectResolveProblem6() {
        assertTrue(delayedMergeObjectResolveProblem6().attemptPeekWithPartialResolve("item1") instanceof ConfigDelayedMergeObject);

        // should be able to attemptPeekWithPartialResolve() a known non-object without resolving
        assertEquals(101, delayedMergeObjectResolveProblem6().toConfig().getObject("item1").attemptPeekWithPartialResolve("xyz").unwrapped());

        var resolved = resolveWithoutFallbacks(delayedMergeObjectResolveProblem6());

        assertEquals(parseObject("{ c : 43 }"), resolved.getObject("item1.b"));
        assertEquals(43, resolved.getInt("item1.b.c"));
        assertEquals(43, resolved.getInt("item2.b.c"));
        assertEquals(15, resolved.getInt("item1.n.o.p"));
    }

    private void delayedMergeObjectWithKnownValue() {
        return parseObject("""
                  defaults {
                    a = 1
                    b = 2
                  }
                  // make item1 into a ConfigDelayedMergeObject
                  item1 = ${defaults}
                  // note that we'll resolve to a non-object value
                  // so item1.b will ignoreFallbacks and not depend on
                  // ${defaults}
                  item1.b = 3
                """);
    }

    @Test
    void fetchKnownValueFromDelayedMergeObject() {
        assertTrue(delayedMergeObjectWithKnownValue().attemptPeekWithPartialResolve("item1") instanceof ConfigDelayedMergeObject);

        assertEquals(3, delayedMergeObjectWithKnownValue().toConfig().getConfig("item1").getInt("b"));
    }

    private void delayedMergeObjectNeedsFullResolve() {
        return parseObject("""
                  defaults {
                    a = 1
                    b = { c : 31 }
                  }
                  item1 = ${defaults}
                  // because b is an object, fetching it requires resolving ${defaults} above
                  // to see if there are more keys to merge with b.
                  item1.b = { c : 41 }
                """);
    }

    @Test
    void failToFetchFromDelayedMergeObjectNeedsFullResolve() {
        assertTrue(delayedMergeObjectWithKnownValue().attemptPeekWithPartialResolve("item1") instanceof ConfigDelayedMergeObject);

        var e = intercept[ConfigException.NotResolved] {
            delayedMergeObjectNeedsFullResolve.toConfig().getObject("item1.b")
        }

        assertTrue("wrong exception: " + e.getMessage(), e.getMessage().contains("item1.b"));
    }

    // objects that mutually refer to each other
    private void delayedMergeObjectEmbrace() {
        return parseObject("""
                  defaults {
                    a = 1
                    b = 2
                  }

                  item1 = ${defaults}
                  // item1.c refers to a field in item2 that refers to item1
                  item1.c = ${item2.d}
                  // item1.x refers to a field in item2 that doesn't go back to item1
                  item1.x = ${item2.y}

                  item2 = ${defaults}
                  // item2.d refers to a field in item1
                  item2.d = ${item1.a}
                  item2.y = 15
                """);
    }

    @Test
    void resolveDelayedMergeObjectEmbrace() {
        assertTrue(delayedMergeObjectEmbrace().attemptPeekWithPartialResolve("item1") instanceof ConfigDelayedMergeObject);
        assertTrue(delayedMergeObjectEmbrace().attemptPeekWithPartialResolve("item2") instanceof ConfigDelayedMergeObject);

        var resolved = delayedMergeObjectEmbrace().toConfig().resolve();
        assertEquals(1, resolved.getInt("item1.c"));
        assertEquals(1, resolved.getInt("item2.d"));
        assertEquals(15, resolved.getInt("item1.x"));
    }

    // objects that mutually refer to each other
    private void plainObjectEmbrace() {
        return parseObject("""
                  item1.a = 10
                  item1.b = ${item2.d}
                  item2.c = 12
                  item2.d = 14
                  item2.e = ${item1.a}
                  item2.f = ${item1.b}   // item1.b goes back to item2
                  item2.g = ${item2.f}   // goes back to ourselves
                """);
    }

    @Test
    void resolvePlainObjectEmbrace() {
        assertTrue(plainObjectEmbrace().attemptPeekWithPartialResolve("item1") instanceof SimpleConfigObject);
        assertTrue(plainObjectEmbrace().attemptPeekWithPartialResolve("item2") instanceof SimpleConfigObject);

        var resolved = plainObjectEmbrace().toConfig.resolve();
        assertEquals(14, resolved.getInt("item1.b"));
        assertEquals(10, resolved.getInt("item2.e"));
        assertEquals(14, resolved.getInt("item2.f"));
        assertEquals(14, resolved.getInt("item2.g"));
    }

    @Test
    void useRelativeToSameFileWhenRelativized() {
        var child = parseObject("""
                foo=in child,bar=${foo}""");

        var values = new java.util.HashMap<String, AbstractConfigValue>();

        values.put("a", child.relativized(new Path("a")));
        // this "foo" should NOT be used.
        values.put("foo", stringValue("in parent"));

        var resolved = resolve(new SimpleConfigObject(fakeOrigin(), values));

        assertEquals("in child", resolved.getString("a.bar"));
    }

    @Test
    void useRelativeToRootWhenRelativized() {
        // here, "foo" is not defined in the child
        var child = parseObject("""
                bar=${foo}""");

        var values = new java.util.HashMap<String, AbstractConfigValue>();

        values.put("a", child.relativized(new Path("a")));
        // so this "foo" SHOULD be used
        values.put("foo", stringValue("in parent"));

        var resolved = resolve(new SimpleConfigObject(fakeOrigin(), values));

        assertEquals("in parent", resolved.getString("a.bar"));
    }

    private void substComplexObject() {
        return parseObject("""
                {
                    "foo" : ${bar},
                    "bar" : ${a.b.c},
                    "a" : { "b" : { "c" : 57, "d" : ${foo}, "e" : { "f" : ${foo} } } },
                    "objA" : ${a},
                    "objB" : ${a.b},
                    "objE" : ${a.b.e},
                    "foo.bar" : 37,
                    "arr" : [ ${foo}, ${a.b.c}, ${"foo.bar"}, ${objB.d}, ${objA.b.e.f}, ${objE.f} ],
                    "ptrToArr" : ${arr},
                    "x" : { "y" : { "ptrToPtrToArr" : ${ptrToArr} } }
                }
                """);
    }

    @Test
    void complexResolve() {
        var resolved = resolveWithoutFallbacks(substComplexObject);

        assertEquals(57, resolved.getInt("foo"));
        assertEquals(57, resolved.getInt("bar"));
        assertEquals(57, resolved.getInt("a.b.c"));
        assertEquals(57, resolved.getInt("a.b.d"));
        assertEquals(57, resolved.getInt("objB.d"));
        assertEquals(Seq(57, 57, 37, 57, 57, 57), resolved.getIntList("arr").asScala);
        assertEquals(Seq(57, 57, 37, 57, 57, 57), resolved.getIntList("ptrToArr").asScala);
        assertEquals(Seq(57, 57, 37, 57, 57, 57), resolved.getIntList("x.y.ptrToPtrToArr").asScala);
    }

    private void substSystemPropsObject() {
        return parseObject("""
                {
                    "a" : ${configtest.a},
                    "b" : ${configtest.b}
                }
                """);
    }

    @Test
    void doNotSerializeUnresolvedObject() {
        checkNotSerializable(substComplexObject);
    }

    @Test
    void resolveListFromSystemProps() {
        var props = parseObject(
                """
                        |"a": ${testList}
                        """.stripMargin);

        System.setProperty("testList.0", "0");
        System.setProperty("testList.1", "1");
        ConfigImpl.reloadSystemPropertiesConfig();

        var resolved = resolve((AbstractConfigObject) ConfigFactory.systemProperties().withFallback(props).root());

        assertEquals(List.of("0", "1"), resolved.getList("a").unwrapped());
    }

    @Test
    void resolveListFromEnvVars() {
        var props = parseObject(
                """
                        |"a": ${testList}
                        """.stripMargin);

        //"testList.0" and "testList.1" are defined as envVars in build.sbt
        var resolved = resolve(props);

        assertEquals(List.of("0", "1"), resolved.getList("a").unwrapped());
    }

    // this is a weird test, it used to test fallback to system props which made more sense.
    // Now it just tests that if you override with system props, you can use system props
    // in substitutions.
    @Test
    void overrideWithSystemProps() {
        System.setProperty("configtest.a", "1234");
        System.setProperty("configtest.b", "5678");
        ConfigImpl.reloadSystemPropertiesConfig();

        var resolved = resolve((AbstractConfigObject) ConfigFactory.systemProperties().withFallback(substSystemPropsObject()).root());

        assertEquals("1234", resolved.getString("a"));
        assertEquals("5678", resolved.getString("b"));
    }

    private void substEnvVarObject() {
        // prefix the names of keys with "key_" to allow us to embed a case sensitive env var name
        // in the key that wont therefore risk a naming collision with env vars themselves
        return parseObject("""
                {
                    "key_HOME" : ${?HOME},
                    "key_PWD" : ${?PWD},
                    "key_SHELL" : ${?SHELL},
                    "key_LANG" : ${?LANG},
                    "key_PATH" : ${?PATH},
                    "key_Path" : ${?Path}, // many windows machines use Path rather than PATH
                    "key_NOT_HERE" : ${?NOT_HERE}
                }
                """);
    }

    @Test
    void fallbackToEnv() throws Exception {
        var resolved = resolve(substEnvVarObject());

        var existed = 0;
        for (var k : resolved.root().keySet()) {
            var envVarName = k.replace("key_", "");
            var e = System.getenv(envVarName);
            if (e != null) {
                existed += 1;
                assertEquals(e, resolved.getString(k));
            } else {
                assertNull(resolved.root().get(k));
            }
        }
        if (existed == 0) {
            throw new Exception("None of the env vars we tried to use for testing were set");
        }
    }

    @Test
    void noFallbackToEnvIfValuesAreNull() {
        // create a fallback object with all the env var names
        // set to null. we want to be sure this blocks
        // lookup in the environment. i.e. if there is a
        // { HOME : null } then ${HOME} should be null.
        var nullsMap = new java.util.HashMap<String, Object>();
        for (var k : substEnvVarObject().keySet()) {
            var envVarName = k.replace("key_", "");
            nullsMap.put(envVarName, null);
        }
        var nulls = ConfigFactory.parseMap(nullsMap, "nulls map");

        var resolved = resolve(substEnvVarObject().withFallback(nulls));

        for (var k : resolved.root().keySet()) {
            assertNotNull(resolved.root().get(k));
            assertEquals(nullValue(), resolved.root().get(k));
        }
    }

    @Test
    void fallbackToEnvWhenRelativized() throws Exception {
        var values = new java.util.HashMap<String, AbstractConfigValue>();

        values.put("a", substEnvVarObject().relativized(new Path("a")));

        var resolved = resolve(new SimpleConfigObject(fakeOrigin(), values));

        var existed = 0;
        for (var k : resolved.getObject("a").keySet()) {
            var envVarName = k.replace("key_", "");
            var e = System.getenv(envVarName);
            if (e != null) {
                existed += 1;
                assertEquals(e, resolved.getConfig("a").getString(k));
            } else {
                assertNull(resolved.getObject("a").get(k));
            }
        }
        if (existed == 0) {
            throw new Exception("None of the env vars we tried to use for testing were set");
        }
    }

    @Test
    void throwWhenEnvNotFound() {
        var obj = parseObject("""
                { a : ${NOT_HERE} }""");
        intercept[ConfigException.UnresolvedSubstitution] {
            resolve(obj);
        }
    }

    @Test
    void optionalOverrideNotProvided() {
        var obj = parseObject("""
                { a: 42, a : ${?NOT_HERE} }""");
        var resolved = resolve(obj);
        assertEquals(42, resolved.getInt("a"));
    }

    @Test
    void optionalOverrideProvided() {
        var obj = parseObject("""
                { HERE : 43, a: 42, a : ${?HERE} }""");
        var resolved = resolve(obj);
        assertEquals(43, resolved.getInt("a"));
    }

    @Test
    void optionalOverrideOfObjectNotProvided() {
        var obj = parseObject("""
                { a: { b : 42 }, a : ${?NOT_HERE} }""");
        var resolved = resolve(obj);
        assertEquals(42, resolved.getInt("a.b"));
    }

    @Test
    void optionalOverrideOfObjectProvided() {
        var obj = parseObject("""
                { HERE : 43, a: { b : 42 }, a : ${?HERE} }""");
        var resolved = resolve(obj);
        assertEquals(43, resolved.getInt("a"));
        assertFalse(resolved.hasPath("a.b"));
    }

    @Test
    void optionalVanishesFromArray() {
        var obj = parseObject("""
                { a : [ 1, 2, 3, ${?NOT_HERE} ] }""");
        var resolved = resolve(obj);
        assertEquals(Seq(1, 2, 3), resolved.getIntList("a").asScala);
    }

    @Test
    void optionalUsedInArray() {
        var obj = parseObject("""
                { HERE: 4, a : [ 1, 2, 3, ${?HERE} ] }""");
        var resolved = resolve(obj);
        assertEquals(Seq(1, 2, 3, 4), resolved.getIntList("a").asScala);
    }

    @Test
    void substSelfReference() {
        var obj = parseObject("""a=1, a=${a}""");
        var resolved = resolve(obj);
        assertEquals(1, resolved.getInt("a"));
    }

    @Test
    void substSelfReferenceUndefined() {
        var obj = parseObject("""a=${a}""");
        var e = intercept[ConfigException.UnresolvedSubstitution] {
            resolve(obj)
        }
        assertTrue("wrong exception: " + e.getMessage(), e.getMessage().contains("cycle"));
    }

    @Test
    void substSelfReferenceOptional() {
        var obj = parseObject("""
                a=${?a}""");
        var resolved = resolve(obj);
        assertEquals(0, resolved.root().size(), "optional self reference disappears");
    }

    @Test
    void substSelfReferenceAlongPath() {
        var obj = parseObject("""
                a.b=1, a.b=${a.b}""");
        var resolved = resolve(obj);
        assertEquals(1, resolved.getInt("a.b"));
    }

    @Test
    void substSelfReferenceAlongLongerPath() {
        var obj = parseObject("""
                a.b.c=1, a.b.c=${a.b.c}""");
        var resolved = resolve(obj);
        assertEquals(1, resolved.getInt("a.b.c"));
    }

    @Test
    void substSelfReferenceAlongPathMoreComplex() {
        // this is an example from the spec
        var obj = parseObject("""
                foo : { a : { c : 1 } }
                foo : ${foo.a}
                foo : { a : 2 }
                            """);
        var resolved = resolve(obj);
        assertEquals(1, resolved.getInt("foo.c"));
        assertEquals(2, resolved.getInt("foo.a"));
    }

    @Test
    void substSelfReferenceIndirect() {
        // this has two possible outcomes depending on whether
        // we resolve and memoize a first or b first. currently
        // java 8's hash table makes it resolve OK, but
        // it's also allowed to throw an exception.
        var obj = parseObject("""
                a=1, b=${a}, a=${b}""");
        var resolved = resolve(obj);
        assertEquals(1, resolved.getInt("a"));
    }

    @Test
    void substSelfReferenceDoubleIndirect() {
        // this has two possible outcomes depending on whether we
        // resolve and memoize a, b, or c first. currently java
        // 8's hash table makes it resolve OK, but it's also
        // allowed to throw an exception.
        var obj = parseObject("""
                a=1, b=${c}, c=${a}, a=${b}""");
        var resolved = resolve(obj);
        assertEquals(1, resolved.getInt("a"));
    }

    @Test
    void substSelfReferenceIndirectStackCycle() {
        // this situation is undefined, depends on
        // whether we resolve a or b first.
        var obj = parseObject("""
                a=1, b={c=5}, b=${a}, a=${b}""");
        var resolved = resolve(obj);
        var option1 = parseObject("""
                b={c=5}, a={c=5} """).toConfig();
        var option2 = parseObject("""
                b=1, a=1 """).toConfig();
        assertTrue(resolved == option1 || resolved == option2,
                "not an expected possibility: " + resolved +
                        " expected 1: " + option1 + " or 2: " + option2);
    }

    @Test
    void substSelfReferenceObject() {
        var obj = parseObject("""
                a={b=5}, a=${a}""");
        var resolved = resolve(obj);
        assertEquals(5, resolved.getInt("a.b"));
    }

    @Test
    void substSelfReferenceObjectAlongPath() {
        var obj = parseObject("""
                a.b={c=5}, a.b=${a.b}""");
        var resolved = resolve(obj);
        assertEquals(5, resolved.getInt("a.b.c"));
    }

    @Test
    void substSelfReferenceInConcat() {
        var obj = parseObject("""
                a=1, a=${a}foo""");
        var resolved = resolve(obj);
        assertEquals("1foo", resolved.getString("a"));
    }

    @Test
    void substSelfReferenceIndirectInConcat() {
        // this situation is undefined, depends on
        // whether we resolve a or b first. If b first
        // then there's an error because ${a} is undefined.
        // if a first then b=1foo and a=1foo.
        var obj = parseObject("""
                a=1, b=${a}foo, a=${b}""");
        var either = try {
            Left(resolve(obj));
        } catch {
            case e:
                ConfigException.UnresolvedSubstitution =>
                Right(e)
        }
        var option1 = Left(parseObject("""a:1foo,b:1foo""").toConfig)
        assertTrue("not an expected possibility: " + either +
                        " expected value " + option1 + " or an exception",
                either == option1 || either.isRight)
    }

    @Test
    void substOptionalSelfReferenceInConcat() {
        var obj = parseObject("""
                a=${?a}foo""");
        var resolved = resolve(obj);
        assertEquals("foo", resolved.getString("a"));
    }

    @Test
    void substOptionalIndirectSelfReferenceInConcat() {
        var obj = parseObject("""
                a=${?b}foo,b=${?a}""");
        var resolved = resolve(obj);
        assertEquals("foo", resolved.getString("a"));
    }

    @Test
    void substTwoOptionalSelfReferencesInConcat() {
        var obj = parseObject("""
                a=${?a}foo${?a}""");
        var resolved = resolve(obj);
        assertEquals("foo", resolved.getString("a"));
    }

    @Test
    void substTwoOptionalSelfReferencesInConcatWithPriorValue() {
        var obj = parseObject("""
                a=1,a=${?a}foo${?a}""");
        var resolved = resolve(obj);
        assertEquals("1foo1", resolved.getString("a"));
    }

    @Test
    void substSelfReferenceMiddleOfStack() {
        var obj = parseObject("""
                a=1, a=${a}, a=2""")
        var resolved = resolve(obj);
        // the substitution would be 1, but then 2 overrides
        assertEquals(2, resolved.getInt("a"));
    }

    @Test
    void substSelfReferenceObjectMiddleOfStack() {
        var obj = parseObject("""
                a={b=5}, a=${a}, a={c=6}""");
        var resolved = resolve(obj);
        assertEquals(5, resolved.getInt("a.b"));
        assertEquals(6, resolved.getInt("a.c"));
    }

    @Test
    void substOptionalSelfReferenceMiddleOfStack() {
        var obj = parseObject("""
                a=1, a=${?a}, a=2""");
        var resolved = resolve(obj);
        // the substitution would be 1, but then 2 overrides
        assertEquals(2, resolved.getInt("a"));
    }

    @Test
    void substSelfReferenceBottomOfStack() {
        // self-reference should just be ignored since it's
        // overridden
        var obj = parseObject("""
                a=${a}, a=1, a=2""");
        var resolved = resolve(obj);
        assertEquals(2, resolved.getInt("a"));
    }

    @Test
    void substOptionalSelfReferenceBottomOfStack() {
        var obj = parseObject("""
                a=${?a}, a=1, a=2""");
        var resolved = resolve(obj);
        assertEquals(2, resolved.getInt("a"));
    }

    @Test
    void substSelfReferenceTopOfStack() {
        var obj = parseObject("""
                a=1, a=2, a=${a}""");
        var resolved = resolve(obj);
        assertEquals(2, resolved.getInt("a"));
    }

    @Test
    void substOptionalSelfReferenceTopOfStack() {
        var obj = parseObject("""
                a=1, a=2, a=${?a}""");
        var resolved = resolve(obj);
        assertEquals(2, resolved.getInt("a"));
    }

    @Test
    void substSelfReferenceAlongAPath() {
        // ${a} in the middle of the stack means "${a} in the stack
        // below us" and so ${a.b} means b inside the "${a} below us"
        // not b inside the final "${a}"
        var obj = parseObject("""
                a={b={c=5}}, a=${a.b}, a={b=2}""");
        var resolved = resolve(obj);
        assertEquals(5, resolved.getInt("a.c"));
    }

    @Test
    void substSelfReferenceAlongAPathInsideObject() {
        // if the ${a.b} is _inside_ a field value instead of
        // _being_ the field value, it does not look backward.
        var obj = parseObject("""
                a={b={c=5}}, a={ x : ${a.b} }, a={b=2}""");
        var resolved = resolve(obj);
        assertEquals(2, resolved.getInt("a.x"));
    }

    @Test
    void substInChildFieldNotASelfReference1() {
        // here, ${bar.foo} is not a self reference because
        // it's the value of a child field of bar, not bar
        // itself; so we use bar's current value, rather than
        // looking back in the merge stack
        var obj = parseObject("""
                bar : { foo : 42,
                        baz : ${bar.foo}
                }
                   """);
        var resolved = resolve(obj);
        assertEquals(42, resolved.getInt("bar.baz"));
        assertEquals(42, resolved.getInt("bar.foo"));
    }

    @Test
    void substInChildFieldNotASelfReference2() {
        // checking that having bar.foo later in the stack
        // doesn't break the behavior
        var obj = parseObject("""
                bar : { foo : 42,
                        baz : ${bar.foo}
                }
                bar : { foo : 43 }
                   """);
        var resolved = resolve(obj);
        assertEquals(43, resolved.getInt("bar.baz"));
        assertEquals(43, resolved.getInt("bar.foo"));
    }

    @Test
    void substInChildFieldNotASelfReference3() {
        // checking that having bar.foo earlier in the merge
        // stack doesn't break the behavior.
        var obj = parseObject("""
                bar : { foo : 43 }
                bar : { foo : 42,
                        baz : ${bar.foo}
                }
                   """);
        var resolved = resolve(obj);
        assertEquals(42, resolved.getInt("bar.baz"));
        assertEquals(42, resolved.getInt("bar.foo"));
    }

    @Test
    void substInChildFieldNotASelfReference4() {
        // checking that having bar set to non-object earlier
        // doesn't break the behavior.
        var obj = parseObject("""
                bar : 101
                bar : { foo : 42,
                        baz : ${bar.foo}
                }
                   """);
        var resolved = resolve(obj);
        assertEquals(42, resolved.getInt("bar.baz"));
        assertEquals(42, resolved.getInt("bar.foo"));
    }

    @Test
    void substInChildFieldNotASelfReference5() {
        // checking that having bar set to unresolved array earlier
        // doesn't break the behavior.
        var obj = parseObject("""
                x : 0
                bar : [ ${x}, 1, 2, 3 ]
                bar : { foo : 42,
                        baz : ${bar.foo}
                }
                   """);
        var resolved = resolve(obj);
        assertEquals(42, resolved.getInt("bar.baz"));
        assertEquals(42, resolved.getInt("bar.foo"));
    }

    @Test
    void mutuallyReferringNotASelfReference() {
        var obj = parseObject("""
                // bar.a should end up as 4
                bar : { a : ${foo.d}, b : 1 }
                bar.b = 3
                // foo.c should end up as 3
                foo : { c : ${bar.b}, d : 2 }
                foo.d = 4
                            """);
        var resolved = resolve(obj);
        assertEquals(4, resolved.getInt("bar.a"));
        assertEquals(3, resolved.getInt("foo.c"));
    }

    @Test
    void substSelfReferenceMultipleTimes() {
        var obj = parseObject("""a=1,a=${a},a=${a},a=${a}""");
        var resolved = resolve(obj);
        assertEquals(1, resolved.getInt("a"));
    }

    @Test
    void substSelfReferenceInConcatMultipleTimes() {
        var obj = parseObject("""
                a=1,a=${a}x,a=${a}y,a=${a}z""");
        var resolved = resolve(obj);
        assertEquals("1xyz", resolved.getString("a"));
    }

    @Test
    void substSelfReferenceInArray() {
        // never "look back" from "inside" an array
        var obj = parseObject("""
                a=1,a=[${a}, 2]""");
        var e = intercept[ConfigException.UnresolvedSubstitution] {
            resolve(obj)
        }
        assertTrue("wrong exception: " + e.getMessage(),
                e.getMessage().contains("cycle") && e.getMessage().contains("${a}"));
    }

    @Test
    void substSelfReferenceInObject() {
        // never "look back" from "inside" an object
        var obj = parseObject("""
                a=1,a={ x : ${a} }""");
        var e = intercept[ConfigException.UnresolvedSubstitution] {
            resolve(obj)
        }
        assertTrue("wrong exception: " + e.getMessage(),
                e.getMessage().contains("cycle") && e.getMessage().contains("${a}"));
    }

    @Test
    void selfReferentialObjectNotAffectedByOverriding() {
        // this is testing that we can still refer to another
        // field in the same object, even though we are overriding
        // an earlier object.
        var obj = parseObject("""
                a={ x : 42, y : ${a.x} }""");
        var resolved = resolve(obj);
        assertEquals(parseObject("{ x : 42, y : 42 }"), resolved.getConfig("a").root());

        // this is expected because if adding "a=1" here affects the outcome,
        // it would be flat-out bizarre.
        var obj2 = parseObject(
                """a=1, a={ x : 42, y : ${a.x} }""");
        var resolved2 = resolve(obj2);
        assertEquals(parseObject("{ x : 42, y : 42 }"), resolved2.getConfig("a").root());
    }

}
