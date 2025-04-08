package org.jvnet.hudson.plugins.m2release;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WithJenkins
class M2ReleaseBuildWrapperTest {

    private static final String PASSWORD = "mysecretpassword";

    private JenkinsRule rule;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        this.rule = rule;
    }

    @Issue("SECURITY-1435")
    @Test
    @LocalData
    void testMigrationOfNexusPassword() throws Exception {
        M2ReleaseBuildWrapper.DescriptorImpl d =
                rule.jenkins.getDescriptorByType(M2ReleaseBuildWrapper.DescriptorImpl.class);
        assertNotNull(d, "could not find the descriptor");

        assertThat("old password read ok", d.getNexusPassword(), notNullValue());
        assertThat("old password migrated", d.getNexusPassword().getPlainText(), is(PASSWORD));

        rule.configRoundtrip();

        assertThat("round tripped password", d.getNexusPassword(), notNullValue());
        assertThat("round tripped password", d.getNexusPassword().getPlainText(), is(PASSWORD));

        File f = new File(rule.jenkins.root, M2ReleaseBuildWrapper.class.getName() + ".xml");
        try (FileInputStream fis = new FileInputStream(f)) {
            String content = IOUtils.toString(fis, StandardCharsets.UTF_8);
            assertThat("password should be encrypted", content, not(containsString(PASSWORD)));
        }
    }
}
