package org.kie.unpackbuildplugin.integrationtests;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UnpackBuildIT {

    private static final String IT_PROJECT_FOLDER = "/integrationtests-project";
    private static final String IT_PROJECT_FOLDER_INVOKER = "/integrationtests-project-invoker";

    @Test
    public void testBuildProjectWithPluginConfig() throws IOException, VerificationException {
        testBuildProject(IT_PROJECT_FOLDER);
    }

    @Test
    public void testBuildProjectUsingInvoker() throws IOException, VerificationException {

        testBuildProject(IT_PROJECT_FOLDER_INVOKER);
    }

    private void testBuildProject(String projectFolder) throws IOException, VerificationException {
        final File testDir = ResourceExtractor.simpleExtractResources(getClass(), projectFolder);
        System.out.println("Test dir is " + testDir);
        final Verifier verifier = new Verifier(testDir.getAbsolutePath());
        try {
            verifier.executeGoal("clean");
            verifier.executeGoal("verify", getProperties());
            verifier.verifyErrorFreeLog();
        } finally {
            verifier.resetStreams();
        }

        final File testRootDirectory = new File(testDir.getParentFile().getParentFile().getParentFile().getParentFile(), "unpack-build-maven-plugin");
        assertThat(testRootDirectory).exists().isDirectory();
        assertMavenPluginProjectDirectory(testRootDirectory);
    }

    private void assertMavenPluginProjectDirectory(final File mavenPluginProjectDirectory) {
        assertThat(new File(mavenPluginProjectDirectory, "target")).exists().isDirectory();
        assertThat(Paths.get(mavenPluginProjectDirectory.getAbsolutePath(), "target", "classes").toFile())
                .exists().isDirectory();
        assertThat(Paths.get(mavenPluginProjectDirectory.getAbsolutePath(), "target", "classes", "META-INF").toFile())
                .exists().isDirectory();
        assertThat(Paths.get(mavenPluginProjectDirectory.getAbsolutePath(), "target", "classes", "META-INF", "MANIFEST.MF").toFile())
                .exists().isFile();
        assertThat(Paths.get(mavenPluginProjectDirectory.getAbsolutePath(), "target", "classes", "org", "kie", "unpackbuildplugin", "UnpackBuildMojo.class").toFile())
                .exists().isFile();
    }

    private Map<String, String> getProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(Constants.TEST_GIT_REPOSITORY_URL_KEY,
                       System.getProperty(Constants.TEST_GIT_REPOSITORY_SYSTEM_PROPERTY_KEY));
        return properties;
    }

}