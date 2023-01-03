import com.cleanroommc.cleanhocon.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class OrderedImpTest {
    static Path configPath = Path.of("./conf.hocon");
    static Map<String, String> stringStringMap = new LinkedHashMap<>();
    static Map<String, String> nestedStringStringMap = new LinkedHashMap<>();
    static {
        IntStream.rangeClosed(0, 10).
                forEach(i -> {
                    stringStringMap.put("key:" + i, "val:" + i);
                    nestedStringStringMap.put("nested-key:" + i, "nested-val:" + i);
                });
    }

    @BeforeEach
    void setup() throws IOException {
        ConfigSortingOptions.setOverwriteSorter(ConfigSortingOptions.originalSorter());
        if (Files.exists(configPath)) {
            Files.delete(configPath);
        }
    }

    @Test
    void basicTest() throws IOException {
        ConfigSortingOptions.setOverwriteSorter(ConfigSortingOptions.orderedSorter());

        var configObj = ConfigValueFactory.fromMap(stringStringMap);
        configObj = configObj.withValue("nested", ConfigValueFactory.fromMap(nestedStringStringMap));
        try (var w = Files.newBufferedWriter(configPath)) {
            w.write(configObj.render());
        }

        System.out.println("BASIC PRINT");
        System.out.println(configObj.render(ConfigRenderOptions.concise().setConfigSorter(ConfigSortingOptions.originalSorter())));
        System.out.println("------------------------------------");
        System.out.println("SORTED PARSED PRINT");
        var sortedParsedConfig = ConfigFactory.parseFile(configPath.toFile(), ConfigParseOptions.defaults().setConfigSorter(ConfigSortingOptions.orderedSorter())).root();
        System.out.println(sortedParsedConfig.render(ConfigRenderOptions.concise().setConfigSorter(ConfigSortingOptions.orderedSorter())));
        System.out.println("------------------------------------");
        System.out.println("ORIGINAL PARSED PRINT");
        var originalParsedConfig = ConfigFactory.parseFile(configPath.toFile(), ConfigParseOptions.defaults().setConfigSorter(ConfigSortingOptions.originalSorter())).root();
        System.out.println(originalParsedConfig.render(ConfigRenderOptions.concise().setConfigSorter(ConfigSortingOptions.originalSorter())));
    }

    @Test
    void basicTest2() throws IOException {
        ConfigSortingOptions.setOverwriteSorter(ConfigSortingOptions.orderedSorter());

        var configObj = ConfigValueFactory.fromMap(stringStringMap);
        configObj = configObj.withValue("nested", ConfigValueFactory.fromMap(nestedStringStringMap, ConfigSortingOptions.originalSorter()));
        try (var w = Files.newBufferedWriter(configPath)) {
            w.write(configObj.render());
        }

        System.out.println("BASIC PRINT");
        System.out.println(configObj.render(ConfigRenderOptions.concise().setConfigSorter(ConfigSortingOptions.originalSorter())));
        System.out.println("------------------------------------");
        System.out.println("SORTED PARSED PRINT");
        var sortedParsedConfig = ConfigFactory.parseFile(configPath.toFile(), ConfigParseOptions.defaults().setConfigSorter(ConfigSortingOptions.orderedSorter())).root();
        System.out.println(sortedParsedConfig.render(ConfigRenderOptions.concise().setConfigSorter(ConfigSortingOptions.orderedSorter())));
        System.out.println("------------------------------------");
        System.out.println("ORIGINAL PARSED PRINT");
        var originalParsedConfig = ConfigFactory.parseFile(configPath.toFile(), ConfigParseOptions.defaults().setConfigSorter(ConfigSortingOptions.originalSorter())).root();
        System.out.println(originalParsedConfig.render(ConfigRenderOptions.concise().setConfigSorter(ConfigSortingOptions.originalSorter())));
    }

    @Test
    void basicTest3() throws IOException {
        ConfigSortingOptions.setOverwriteSorter(ConfigSortingOptions.originalSorter());

        var configObj = ConfigValueFactory.fromMap(stringStringMap, ConfigSortingOptions.orderedSorter());
        configObj = configObj.withValue("nested", ConfigValueFactory.fromMap(nestedStringStringMap, ConfigSortingOptions.orderedSorter()));
        try (var w = Files.newBufferedWriter(configPath)) {
            w.write(configObj.render(ConfigRenderOptions.defaults().setConfigSorter(ConfigSortingOptions.orderedSorter())));
        }

        System.out.println("BASIC PRINT");
        System.out.println(configObj.render(ConfigRenderOptions.concise().setConfigSorter(ConfigSortingOptions.originalSorter())));
        System.out.println("------------------------------------");
        System.out.println("SORTED PARSED PRINT");
        var sortedParsedConfig = ConfigFactory.parseFile(configPath.toFile(), ConfigParseOptions.defaults().setConfigSorter(ConfigSortingOptions.orderedSorter())).root();
        System.out.println(sortedParsedConfig.render(ConfigRenderOptions.concise().setConfigSorter(ConfigSortingOptions.orderedSorter())));
        System.out.println("------------------------------------");
        System.out.println("ORIGINAL PARSED PRINT");
        var originalParsedConfig = ConfigFactory.parseFile(configPath.toFile(), ConfigParseOptions.defaults().setConfigSorter(ConfigSortingOptions.originalSorter())).root();
        System.out.println(originalParsedConfig.render(ConfigRenderOptions.concise().setConfigSorter(ConfigSortingOptions.originalSorter())));
    }
}
