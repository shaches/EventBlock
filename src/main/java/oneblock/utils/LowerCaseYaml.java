// Copyright 2025 MrMarL. The MIT License (MIT).
package oneblock.utils;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public class LowerCaseYaml extends YamlConfiguration {

  public static YamlConfiguration loadAndFixConfig(File file) {
    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
    convertKeysToLowerCase(config);
    return config;
  }

  private static void convertKeysToLowerCase(YamlConfiguration config) {
    for (String key : config.getKeys(false)) {
      String lowerKey = key.toLowerCase(Locale.ROOT);
      if (lowerKey.equals(key)) continue;
      Object value = config.get(key);
      config.set(key, null);
      config.set(lowerKey, processValue(value));
    }
  }

  private static Object processValue(Object value) {
    if (value instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) value;
      Map<String, Object> newMap = new LinkedHashMap<>();
      map.forEach((k, v) -> newMap.put(k.toString().toLowerCase(Locale.ROOT), processValue(v)));
      return newMap;
    } else if (value instanceof List) {
      List<?> list = (List<?>) value;
      return list.stream().map(LowerCaseYaml::processValue).toList();
    }
    return value;
  }
}
