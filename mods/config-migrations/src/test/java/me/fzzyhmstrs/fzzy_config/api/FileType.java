package me.fzzyhmstrs.fzzy_config.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.fzzyhmstrs.fzzy_config.util.ValidationResult;
import net.peanuuutz.tomlkt.TomlArray;
import net.peanuuutz.tomlkt.TomlElement;
import net.peanuuutz.tomlkt.TomlLiteral;
import net.peanuuutz.tomlkt.TomlTable;

public enum FileType {
    TOML(".toml"),
    JSON(".json");

    private static final Gson GSON = new Gson();

    private final String suffix;

    FileType(String suffix) {
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }

    public ValidationResult<TomlElement> decode(String contents) {
        try {
            return ValidationResult.success(fromJson(JsonParser.parseString(contents)));
        } catch (RuntimeException exception) {
            return ValidationResult.error(new TomlTable(Map.of(), Map.of()));
        }
    }

    public ValidationResult<String> encode(TomlElement element) {
        try {
            return ValidationResult.success(GSON.toJson(toJson(element)));
        } catch (RuntimeException exception) {
            return ValidationResult.error("");
        }
    }

    private static TomlElement fromJson(JsonElement element) {
        if (element instanceof JsonObject object) {
            Map<String, TomlElement> entries = new LinkedHashMap<>();
            object.entrySet().forEach(entry -> entries.put(entry.getKey(), fromJson(entry.getValue())));
            return new TomlTable(entries, Map.of());
        }
        if (element instanceof JsonArray array) {
            List<TomlElement> entries = new ArrayList<>();
            array.forEach(entry -> entries.add(fromJson(entry)));
            return new TomlArray(
                    entries,
                    entries.stream()
                            .map(ignored -> List.<java.lang.annotation.Annotation>of())
                            .toList());
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return new TomlLiteral(primitive.getAsString(), TomlLiteral.Type.Boolean);
        }
        if (primitive.isString()) {
            return new TomlLiteral(primitive.getAsString(), TomlLiteral.Type.String);
        }
        String number = primitive.getAsString();
        TomlLiteral.Type type = number.contains(".") || number.contains("e") || number.contains("E")
                ? TomlLiteral.Type.Float
                : TomlLiteral.Type.Integer;
        return new TomlLiteral(number, type);
    }

    private static JsonElement toJson(TomlElement element) {
        if (element instanceof TomlTable table) {
            JsonObject object = new JsonObject();
            table.getContent().forEach((key, value) -> object.add(key, toJson(value)));
            return object;
        }
        if (element instanceof TomlArray array) {
            JsonArray json = new JsonArray();
            array.getContent().forEach(value -> json.add(toJson(value)));
            return json;
        }
        TomlLiteral literal = (TomlLiteral) element;
        return switch (literal.getType()) {
            case Boolean -> new JsonPrimitive(Boolean.parseBoolean(literal.getContent()));
            case Integer -> new JsonPrimitive(Long.parseLong(literal.getContent()));
            case Float -> new JsonPrimitive(Double.parseDouble(literal.getContent()));
            case String -> new JsonPrimitive(literal.getContent());
        };
    }
}
