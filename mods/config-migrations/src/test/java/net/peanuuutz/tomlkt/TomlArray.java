package net.peanuuutz.tomlkt;

import java.lang.annotation.Annotation;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public final class TomlArray extends AbstractList<TomlElement> implements TomlElement {
    private final List<TomlElement> content;
    private final List<List<Annotation>> annotations;

    @SuppressWarnings("unchecked")
    public TomlArray(
            List<? extends TomlElement> content,
            List<? extends List<? extends Annotation>> annotations) {
        this.content = new ArrayList<>(content);
        this.annotations = (List<List<Annotation>>) (List<?>) annotations;
    }

    @Override
    public List<TomlElement> getContent() {
        return content;
    }

    public List<List<Annotation>> getAnnotations() {
        return annotations;
    }

    @Override
    public TomlElement get(int index) {
        return content.get(index);
    }

    @Override
    public int size() {
        return content.size();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TomlArray array && content.equals(array.content);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}
