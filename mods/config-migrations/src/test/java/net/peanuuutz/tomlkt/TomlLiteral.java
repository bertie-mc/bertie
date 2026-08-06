package net.peanuuutz.tomlkt;

import java.util.Objects;

public final class TomlLiteral implements TomlElement {
    private final String content;
    private final Type type;

    public TomlLiteral(String content, Type type) {
        this.content = content;
        this.type = type;
    }

    @Override
    public String getContent() {
        return content;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TomlLiteral literal && type == literal.type && content.equals(literal.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, type);
    }

    public enum Type {
        Boolean,
        Integer,
        Float,
        String
    }
}
