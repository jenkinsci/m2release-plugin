package org.jvnet.hudson.plugins.m2release;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class M2ReleaseBuildWrapperTest {

    private static final String PASSWORD = "mysecretpassword";
    @Rule
    public JenkinsRule jr = new JenkinsRule();


    @Issue("SECURITY-1435")
    @Test
    @LocalData
    public void testMigrationOfNexusPassword() throws Exception {
        M2ReleaseBuildWrapper.DescriptorImpl d =
                jr.jenkins.getDescriptorByType(M2ReleaseBuildWrapper.DescriptorImpl.class);
        if (d == null) {
            fail("could not find the descriptor");
        }
        assertThat("old password read ok", d.getNexusPassword(), notNullValue());
        assertThat("old password migrated", d.getNexusPassword().getPlainText(), is(PASSWORD));

        jr.configRoundtrip();

        assertThat("round tripped password", d.getNexusPassword(), notNullValue());
        assertThat("round tripped password", d.getNexusPassword().getPlainText(), is(PASSWORD));

        File f = new File(jr.jenkins.root, M2ReleaseBuildWrapper.class.getName() + ".xml");
        FileInputStream fis = new FileInputStream(f);
        try {
            String content = IOUtils.toString(fis, "UTF-8");
            assertThat("password should be encrypted", content, not(containsString(PASSWORD)));
        } finally {
            fis.close();
        }

    }
}
