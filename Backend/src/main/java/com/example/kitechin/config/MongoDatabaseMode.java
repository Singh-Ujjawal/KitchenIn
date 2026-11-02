package com.example.kitechin.config;

import org.springframework.stereotype.Component;

@Component
public class MongoDatabaseMode {

    public static final String ATLAS = "atlas";
    public static final String MEMORY = "memory";

    private volatile String mode = MEMORY;

    public String mode() {
        return mode;
    }

    public boolean memory() {
        return MEMORY.equals(mode);
    }

    void useAtlas() {
        this.mode = ATLAS;
    }

    void useMemory() {
        this.mode = MEMORY;
    }
}
