package com.typesafe.config.impl;

import beanconfig.*;
import com.typesafe.config.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigBeanFactoryTest {

    @Test
    void testToCamelCase() {
        assertEquals("configProp", ConfigImplUtil.toCamelCase("config-prop"));
        assertEquals("configProp", ConfigImplUtil.toCamelCase("configProp"));
        assertEquals("fooBar", ConfigImplUtil.toCamelCase("foo-----bar"));
        assertEquals("fooBar", ConfigImplUtil.toCamelCase("fooBar"));
        assertEquals("foo", ConfigImplUtil.toCamelCase("-foo"));
        assertEquals("bar", ConfigImplUtil.toCamelCase("bar-"));
    }

    @Test
    void testCreate() {
        var configIs = this.getClass().getClassLoader().getResourceAsStream("beanconfig/beanconfig01.conf");
        var config = ConfigFactory.parseReader(new InputStreamReader(configIs),
                ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF)).resolve();
        var beanConfig = ConfigBeanFactory.create(config, TestBeanConfig.class);
        assertNotNull(beanConfig);
        // recursive bean inside the first bean
        assertEquals(3, beanConfig.getNumbers().getIntVal());
    }

    @Test
    void testValidation() {
        var configIs = this.getClass().getClassLoader().getResourceAsStream("beanconfig/beanconfig01.conf");
        var config = ConfigFactory.parseReader(new InputStreamReader(configIs),
                ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF)).resolve().getConfig("validation");
        var e = intercept[ConfigException.ValidationFailed] {
            ConfigBeanFactory.create(config, ValidationBeanConfig.class);
        }

        var expecteds = Seq(Missing("propNotListedInConfig", 77, "string"),
                WrongType("shouldBeInt", 78, "number", "boolean"),
                WrongType("should-be-boolean", 79, "boolean", "number"),
                WrongType("should-be-list", 80, "list", "string"))

        checkValidationException(e, expecteds)
    }

    @Test
    void testCreateBool() {
        var beanConfig = ConfigBeanFactory.create(loadConfig().getConfig("booleans"), BooleansConfig.class);
        assertNotNull(beanConfig);
        assertEquals(true, beanConfig.getTrueVal());
        assertEquals(false, beanConfig.getFalseVal());
    }

    @Test
    void testCreateString() {
        var beanConfig = ConfigBeanFactory.create(loadConfig().getConfig("strings"), StringsConfig.class);
        assertNotNull(beanConfig);
        assertEquals("abcd", beanConfig.getAbcd());
        assertEquals("yes", beanConfig.getYes());
    }

    @Test
    void testCreateEnum() {
        var beanConfig = ConfigBeanFactory.create(loadConfig().getConfig("enums"), EnumsConfig.class);
        assertNotNull(beanConfig);
        assertEquals(EnumsConfig.Problem.P1, beanConfig.getProblem());
        assertEquals(List.of(EnumsConfig.Solution.S1, EnumsConfig.Solution.S3), beanConfig.getSolutions());
    }

    @Test
    void testCreateNumber() {
        var beanConfig = ConfigBeanFactory.create(loadConfig().getConfig("numbers"), NumbersConfig.class);
        assertNotNull(beanConfig);

        assertEquals(3, beanConfig.getIntVal());
        assertEquals(3, beanConfig.getIntObj());

        assertEquals(4L, beanConfig.getLongVal());
        assertEquals(4L, beanConfig.getLongObj());

        assertEquals(1.0, beanConfig.getDoubleVal(), 1e-6);
        assertEquals(1.0, beanConfig.getDoubleObj(), 1e-6);
    }

    @Test
    void testCreateList() {
        var beanConfig = ConfigBeanFactory.create(loadConfig().getConfig("arrays"), ArraysConfig.class);
        assertNotNull(beanConfig);
        assertEquals(Collections.emptyList(), beanConfig.getEmpty());
        assertEquals(List.of(1, 2, 3), beanConfig.getOfInt());
        assertEquals(List.of(32L, 42L, 52L), beanConfig.getOfLong());
        assertEquals(List.of("a", "b", "c"), beanConfig.getOfString());
        //assertEquals(List(List("a", "b", "c").asJava,
        //    List("a", "b", "c").asJava,
        //    List("a", "b", "c").asJava).asJava,
        //    beanConfig.getOfArray)
        assertEquals(3, beanConfig.getOfObject().size());
        assertEquals(3, beanConfig.getOfDouble().size());
        assertEquals(3, beanConfig.getOfConfig().size());
        assertNotNull(beanConfig.getOfConfig().get(0));
        assertEquals(3, beanConfig.getOfConfigObject().size());
        assertNotNull(beanConfig.getOfConfigObject().get(0));
        assertEquals(List.of(1, 2,"a"), beanConfig.getOfConfigValue());
        assertEquals(List.of(Duration.ofMillis(1), Duration.ofHours(2), Duration.ofDays(3)), beanConfig.getOfDuration());
        assertEquals(List.of(ConfigMemorySize.ofBytes(1024),
                        ConfigMemorySize.ofBytes(1048576),
                        ConfigMemorySize.ofBytes(1073741824)),
                beanConfig.getOfMemorySize());

        var stringsConfigOne = new StringsConfig();
        stringsConfigOne.setAbcd("testAbcdOne");
        stringsConfigOne.setYes("testYesOne");
        var stringsConfigTwo = new StringsConfig();
        stringsConfigTwo.setAbcd("testAbcdTwo");
        stringsConfigTwo.setYes("testYesTwo");

        assertEquals(List.of(stringsConfigOne, stringsConfigTwo), beanConfig.getOfStringBean());
    }

    @Test
    void testCreateSet() {
        var beanConfig = ConfigBeanFactory.create(loadConfig().getConfig("sets"), SetsConfig.class);
        assertNotNull(beanConfig);
        assertEquals(Collections.emptySet(), beanConfig.getEmpty());
        assertEquals(Set.of(1, 2, 3), beanConfig.getOfInt());
        assertEquals(Set.of(32L, 42L, 52L), beanConfig.getOfLong());
        assertEquals(Set.of("a", "b", "c"), beanConfig.getOfString());
        assertEquals(3, beanConfig.getOfObject().size());
        assertEquals(3, beanConfig.getOfObject().size());
        assertEquals(3, beanConfig.getOfObject().size());
        assertNotNull(beanConfig.getOfConfig().iterator().next());
        assertEquals(3, beanConfig.getOfConfigObject().size());
        assertNotNull(beanConfig.getOfConfigObject().iterator().next());
        assertEquals(Set.of(1, 2,"a"), beanConfig.getOfConfigValue());
        assertEquals(Set.of(Duration.ofMillis(1), Duration.ofHours(2), Duration.ofDays(3)),
                beanConfig.getOfDuration());
        assertEquals(Set.of(ConfigMemorySize.ofBytes(1024),
                        ConfigMemorySize.ofBytes(1048576),
                        ConfigMemorySize.ofBytes(1073741824)),
                beanConfig.getOfMemorySize());

        var stringsConfigOne = new StringsConfig();
        stringsConfigOne.setAbcd("testAbcdOne");
        stringsConfigOne.setYes("testYesOne");
        var stringsConfigTwo = new StringsConfig();
        stringsConfigTwo.setAbcd("testAbcdTwo");
        stringsConfigTwo.setYes("testYesTwo");

        assertEquals(Set.of(stringsConfigOne, stringsConfigTwo), beanConfig.getOfStringBean());
    }

    @Test
    void testCreateDuration() {
        var beanConfig = ConfigBeanFactory.create(loadConfig().getConfig("durations"), DurationsConfig.class);
        assertNotNull(beanConfig);
        assertEquals(Duration.ofMillis(500), beanConfig.getHalfSecond());
        assertEquals(Duration.ofMillis(1000), beanConfig.getSecond());
        assertEquals(Duration.ofMillis(1000), beanConfig.getSecondAsNumber());
    }

    @Test
    void testCreateBytes() {
        var beanConfig = ConfigBeanFactory.create(loadConfig().getConfig("bytes"), BytesConfig.class);
        assertNotNull(beanConfig);
        assertEquals(ConfigMemorySize.ofBytes(1024), beanConfig.getKibibyte());
        assertEquals(ConfigMemorySize.ofBytes(1000), beanConfig.getKilobyte());
        assertEquals(ConfigMemorySize.ofBytes(1000), beanConfig.getThousandBytes());
    }

    @Test
    void testPreferCamelNames() {
        var beanConfig = ConfigBeanFactory.create(loadConfig().getConfig("preferCamelNames"), PreferCamelNamesConfig.class);
        assertNotNull(beanConfig);

        assertEquals("yes", beanConfig.getFooBar());
        assertEquals("yes", beanConfig.getBazBar());
    }

    @Test
    void testValues() {
        var beanConfig = ConfigBeanFactory.create(loadConfig().getConfig("values"), ValuesConfig.class);
        assertNotNull(beanConfig);
        assertEquals(42, beanConfig.getObj());
        assertEquals("abcd", beanConfig.getConfig().getString("abcd"));
        assertEquals(3, beanConfig.getConfigObj().toConfig().getInt("intVal"));
        assertEquals("hello world", beanConfig.getConfigValue());
        //assertEquals(List(1, 2, 3).map(intValue), beanConfig.getList());
        assertEquals(List.of(1, 2, 3), beanConfig.getList());
        assertEquals(true, beanConfig.getUnwrappedMap().get("shouldBeInt"));
        assertEquals(42, beanConfig.getUnwrappedMap().get("should-be-boolean"));
    }

    @Test
    void testOptionalProperties() {
        var beanConfig = ConfigBeanFactory.create(loadConfig().getConfig("objects"), ObjectsConfig.class);
        assertNotNull(beanConfig);
        assertNotNull(beanConfig.getValueObject());
        assertNull(beanConfig.getValueObject().getOptionalValue());
        assertNull(beanConfig.getValueObject().getDefault());
        assertEquals("notNull", beanConfig.getValueObject().getMandatoryValue());
    }

    @Test
    void testNotAnOptionalProperty() {
        var e = intercept[ConfigException.ValidationFailed] {
            ConfigBeanFactory.create(parseConfig("{valueObject: {}}"), ObjectsConfig.class);
        }
        assertTrue("missing value error", e.getMessage().contains("No setting"));
        assertTrue("error about the right property", e.getMessage().contains("mandatoryValue"));
    }

    @Test
    void testNotABeanField() {
        var e = intercept[ConfigException.BadBean] {
            ConfigBeanFactory.create(parseConfig("notBean=42"), NotABeanFieldConfig.class);
        }
        assertTrue("unsupported type error", e.getMessage().contains("unsupported type"));
        assertTrue("error about the right property", e.getMessage().contains("notBean"));
    }

    @Test
    void testNotAnEnumField() {
        var e = intercept[ConfigException.BadValue] {
            ConfigBeanFactory.create(parseConfig("{problem=P1,solutions=[S4]}"), EnumsConfig.class);
        }
        assertTrue("invalid value error", e.getMessage().contains("Invalid value"));
        assertTrue("error about the right property", e.getMessage().contains("solutions"));
        assertTrue("error enumerates the enum constants", e.getMessage().contains("should be one of [S1, S2, S3]"));
    }

    @Test
    void testUnsupportedListElement() {
        var e = intercept[ConfigException.BadBean] {
            ConfigBeanFactory.create(parseConfig("uri=[42]"), UnsupportedListElementConfig.class);
        }
        assertTrue("unsupported element type error", e.getMessage().contains("unsupported list element type"));
        assertTrue("error about the right property", e.getMessage().contains("uri"));
    }

    @Test
    void testUnsupportedMapKey() {
        var e = intercept[ConfigException.BadBean] {
            ConfigBeanFactory.create(parseConfig("map={}"), UnsupportedMapKeyConfig.class);
        }
        assertTrue("unsupported map type error", e.getMessage().contains("unsupported Map"));
        assertTrue("error about the right property", e.getMessage().contains("'map'"));
    }

    @Test
    void testUnsupportedMapValue() {
        var e = intercept[ConfigException.BadBean] {
            ConfigBeanFactory.create(parseConfig("map={}"), UnsupportedMapValueConfig.class);
        }
        assertTrue("unsupported map type error", e.getMessage().contains("unsupported Map"));
        assertTrue("error about the right property", e.getMessage().contains("'map'"));
    }

    @Test
    void testDifferentFieldNameFromAccessors() {
        var e = intercept[ConfigException.ValidationFailed] {
            ConfigBeanFactory.create(ConfigFactory.empty(), DifferentFieldNameFromAccessorsConfig.class);
        }
        assertTrue("only one missing value error", e.getMessage().contains("No setting"));
    }

    private Config loadConfig() {
        try (var configIs = this.getClass().getClassLoader().getResourceAsStream("beanconfig/beanconfig01.conf")){
            return ConfigFactory.parseReader(new InputStreamReader(configIs),
                    ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF)).resolve();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
