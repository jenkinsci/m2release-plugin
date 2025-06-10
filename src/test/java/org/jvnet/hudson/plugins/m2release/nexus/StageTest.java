package org.jvnet.hudson.plugins.m2release.nexus;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class StageTest {

    // TODO start an embedded server instead to server these files so we also
    // test http auth access?
    private static URL nexusUrl;

    @BeforeAll
    static void setUp() throws Exception {
        nexusUrl = StageTest.class.getResource("stageTest/");
        // NEXUS_URL = new URL("http://localhost:8081/nexus");
        // NEXUS_URL = new URL("http://192.168.1.65:8081/nexus");
    }

    @Test
    @Disabled("requires test setup")
    void testSpecificStage() throws Exception {
        assumeTrue(nexusUrl.getProtocol().equals("file"));

        StageClient client = new StageClient(nexusUrl, "admin", "admin123");

        // group and artifact don't exist
        Stage stage = client.getOpenStageID("invalid", "bogus", "1.2.3-4");
        assertNull(stage, "Stage returned but we should not have one");

        // group and artifact exist but at different version
        stage = client.getOpenStageID("com.test.testone", "test", "1.0.2");
        assertNull(stage, "Stage returned but we should not have one");

        // full gav match
        stage = client.getOpenStageID("com.test.testone", "test", "1.0.0");
        assertEquals("test-005",
                stage.getStageID(),
                "Incorrect stage returned");

        // match group and artifact for any version
        stage = client.getOpenStageID("com.test.testone", "test", null);
        assertEquals("test-005",
                stage.getStageID(),
                "Incorrect stage returned");
    }

    @Test
    @Disabled("requires test setup")
    void testCloseStage() throws Exception {
        assumeTrue(nexusUrl.getProtocol().equals("http"));
        StageClient client = new StageClient(nexusUrl, "admin", "admin123");
        Stage stage = client
                .getOpenStageID("com.test.testone", "test", "1.0.0");
        assertNotNull(stage, "Stage is null");
        client.closeStage(stage, "Test stage closing from StageClient");
    }

    @Test
    @Disabled("requires test setup")
    void testDropStage() throws Exception {
        assumeTrue(nexusUrl.getProtocol().equals("http"));
        StageClient client = new StageClient(nexusUrl, "admin", "admin123");
        Stage stage = client
                .getOpenStageID("com.test.testone", "test", "1.0.0");
        assertNotNull(stage, "Stage is null");
        client.dropStage(stage);
    }
}
