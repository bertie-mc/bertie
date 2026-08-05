package io.github.bertie_mc.testing.client.driver.world;

import java.nio.file.Path;

/** Identifies world data prepared for the in-process dedicated-server entrypoint. */
public record PreparedDedicatedWorld(Path universe, String levelName) {}
