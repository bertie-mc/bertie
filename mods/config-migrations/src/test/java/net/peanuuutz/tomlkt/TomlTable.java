package net.peanuuutz.tomlkt;

import java.lang.annotation.Annotation;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TomlTable extends AbstractMap<String, TomlElement> implements TomlElement {
    private final Map<String, TomlElement> content;
    private final Map<String, List<Annotation>> annotations;

    @SuppressWarnings("unchecked")
    public TomlTable(
            Map<String, ? extends TomlElement> content, Map<String, ? extends List<? extends Annotation>> annotations) {
        this.content = new LinkedHashMap<>(content);
        this.annotations = (Map<String, List<Annotation>>) (Map<?, ?>) annotations;
    }

    @Override
    public Map<String, TomlElement> getContent() {
        return content;
    }

    public Map<String, List<Annotation>> getAnnotations() {
        return annotations;
    }

    public TomlElement get(String key) {
        return content.get(key);
    }

    @Override
    public Set<Entry<String, TomlElement>> entrySet() {
        return content.entrySet();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TomlTable table && content.equals(table.content);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}
