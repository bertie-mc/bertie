package io.github.bertie_mc.testing.gametest.driver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.ParserConfigurationException;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.neoforged.fml.common.Mod;

/** Adds a Gradle-consumable XML report without replacing the native GameTest runner. */
@Mod(GameTestDriver.MOD_ID)
public final class GameTestDriver {
    public static final String MOD_ID = "bertie_gametest_driver";
    private static final String REPORT_PROPERTY = "bertie.gametest.report";

    public GameTestDriver() {
        String configured = System.getProperty(REPORT_PROPERTY);
        if (configured == null || configured.isBlank()) {
            return;
        }
        Path report = Path.of(configured).toAbsolutePath().normalize();
        try {
            Path parent = report.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            GlobalTestReporter.replaceWith(new CompositeReporter(
                    new LogTestReporter(), new JUnitLikeTestReporter(report.toFile())));
        } catch (IOException | ParserConfigurationException exception) {
            throw new IllegalStateException("Cannot prepare GameTest report " + report, exception);
        }
    }

    private record CompositeReporter(TestReporter first, TestReporter second)
            implements TestReporter {
        @Override
        public void onTestFailed(GameTestInfo testInfo) {
            first.onTestFailed(testInfo);
            second.onTestFailed(testInfo);
        }

        @Override
        public void onTestSuccess(GameTestInfo testInfo) {
            first.onTestSuccess(testInfo);
            second.onTestSuccess(testInfo);
        }

        @Override
        public void finish() {
            first.finish();
            second.finish();
        }
    }
}
