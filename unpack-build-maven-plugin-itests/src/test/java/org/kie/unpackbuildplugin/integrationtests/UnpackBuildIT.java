package org.kie.unpackbuildplugin.integrationtests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;

public class UnpackBuildIT {

    private static final String IT_PROJECT_FOLDER = "/integrationtests-project";

    @Test
    public void testBuildProject() throws IOException, VerificationException {
        final File testDir = ResourceExtractor.simpleExtractResources(getClass(), IT_PROJECT_FOLDER);
        final Verifier verifier = new Verifier(testDir.getAbsolutePath());
        try {
            verifier.executeGoal("clean");

            verifier.executeGoal("verify", getProperties());
            verifier.verifyErrorFreeLog();
        } finally {
            verifier.resetStreams();
        }
    }

    private Map<String, String> getProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(Constants.TEST_GIT_REPOSITORY_URL_KEY,
                       System.getProperty(Constants.TEST_GIT_REPOSITORY_SYSTEM_PROPERTY_KEY));
        return properties;
    }

}
