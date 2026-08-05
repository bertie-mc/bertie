package io.github.bertie_mc.testing.client.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bertie_mc.testing.client.context.ClientTestContext;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;

final class ClientTestResultsTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void writesParseableJUnitXmlForPassesAndFailures() throws Exception {
        Path report = temporaryDirectory.resolve("TEST-clienttest.xml");
        ClientTestResults.write(report, List.of(
                TestResult.passed("example.Passing.test", Duration.ofMillis(25)),
                TestResult.failed(
                        "example.Failing.test",
                        Duration.ofMillis(50),
                        new AssertionError("expected <left> & \"right\""))));

        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report.toFile());
        Element suite = document.getDocumentElement();
        assertEquals("2", suite.getAttribute("tests"));
        assertEquals("1", suite.getAttribute("failures"));
        assertEquals(2, document.getElementsByTagName("testcase").getLength());
        assertNotNull(document.getElementsByTagName("failure").item(0));
    }

    @Test
    void matchesTheDescriptorStoredByNeoForgeAnnotationScanning() throws Exception {
        Method method = ClientTestResultsTest.class.getDeclaredMethod(
                "exampleClientTest", ClientTestContext.class);

        assertTrue(ClientTestDriver.matchesScannedMethod(
                method,
                "exampleClientTest(Lio/github/bertie_mc/testing/client/context/ClientTestContext;)V"));
        assertFalse(ClientTestDriver.matchesScannedMethod(method, "exampleClientTest"));
    }

    private static void exampleClientTest(ClientTestContext context) {
    }
}
