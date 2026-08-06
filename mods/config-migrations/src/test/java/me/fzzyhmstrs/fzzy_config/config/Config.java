package me.fzzyhmstrs.fzzy_config.config;

import me.fzzyhmstrs.fzzy_config.api.FileType;
import net.peanuuutz.tomlkt.TomlTable;

public class Config {
    private final FileType fileType;
    private TomlTable document;

    public Config() {
        this(FileType.JSON);
    }

    public Config(FileType fileType) {
        this.fileType = fileType;
    }

    public FileType fileType() {
        return fileType;
    }

    public TomlTable fixtureDocument() {
        return document;
    }

    public void fixtureDocument(TomlTable document) {
        this.document = document;
    }
}
