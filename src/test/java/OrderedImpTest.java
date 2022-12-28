import com.cleanroommc.cleanhocon.Config;
import com.cleanroommc.cleanhocon.ConfigFactory;
import com.cleanroommc.cleanhocon.ConfigSortingOptions;
import com.cleanroommc.cleanhocon.ConfigValueFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class OrderedImpTest {
    static Map<String, String> stringStringMap = new LinkedHashMap<>();
    static {
        IntStream.rangeClosed(0, 10).
                forEach(i -> {
                    stringStringMap.put("key:" + i, "val:" + i);
                });
    }

    @Test
    void basicTest() throws IOException {

        Path configPath = Path.of("./conf.hocon");
        if (Files.exists(configPath)) {
            Files.delete(configPath);
        }

        ConfigSortingOptions.setOverwriteSorter(ConfigSortingOptions.orderedSorter());
        var rootConfig = ConfigValueFactory.fromMap(stringStringMap).toConfig();
        rootConfig = rootConfig.root().withValue("nested", ConfigValueFactory.fromMap(stringStringMap)).toConfig();
        try (var w = Files.newBufferedWriter(configPath)) {
            w.write(rootConfig.root().render());
        }

        rootConfig.root().entrySet().forEach(System.out::println);
        System.out.println("------------------------------------");
        Config parsedConfig = ConfigFactory.parseFile(configPath.toFile());
        parsedConfig.root().entrySet().forEach(System.out::println);
    }

}
