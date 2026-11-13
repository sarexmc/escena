package com.sarexstudio.cinematics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lista de escenas conocidas (scenes.json empaquetado dentro del mod). */
public final class Scenes {
    private static Map<String, String> cache;

    private Scenes() {}

    /** nombre -> descripcion */
    public static synchronized Map<String, String> all() {
        if (cache != null) return cache;
        Map<String, String> m = new LinkedHashMap<>();
        try (InputStream in = Scenes.class.getResourceAsStream("/scenes.json")) {
            if (in != null) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonArray arr = root.getAsJsonArray("scenes");
                for (JsonElement e : arr) {
                    JsonObject o = e.getAsJsonObject();
                    m.put(o.get("name").getAsString(), o.has("desc") ? o.get("desc").getAsString() : "");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        cache = m;
        return m;
    }

    public static List<String> names() {
        return new ArrayList<>(all().keySet());
    }
}
