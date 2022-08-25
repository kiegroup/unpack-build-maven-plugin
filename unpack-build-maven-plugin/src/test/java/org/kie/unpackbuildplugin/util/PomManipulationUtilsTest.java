package org.kie.unpackbuildplugin.util;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PomManipulationUtilsTest {

    @Rule
    public ExpectedException exceptionGrabber = ExpectedException.none();

    @Test
    public void loadSimplePomModel() throws MojoExecutionException {
        Model pomModel = PomManipulationUtils.loadPomModel(Paths.get(getClass().getResource("/pom-manipulation-utils/simple-pom.xml").getFile()).toAbsolutePath());
        Assertions.assertThat(pomModel.getGroupId()).isEqualTo("org.acme");
        Assertions.assertThat(pomModel.getArtifactId()).isEqualTo("simple-pom");
    }

    @Test
    public void loadInvalidPomModel() throws MojoExecutionException {
        exceptionGrabber.expect(MojoExecutionException.class);
        exceptionGrabber.expectMessage("Error while opening generated pom: ");
        PomManipulationUtils.loadPomModel(Paths.get(getClass().getResource("/pom-manipulation-utils/invalid-pom.xml").getFile()).toAbsolutePath());
    }

    @Test
    public void manipulateSimplePomModel() throws MojoExecutionException, IOException {
        Path pomFile = Paths.get(getClass().getResource("/pom-manipulation-utils/simple-pom.xml").getFile()).toAbsolutePath();
        Model pomModel = PomManipulationUtils.loadPomModel(pomFile);
        Assertions.assertThat(pomModel.getGroupId()).isEqualTo("org.acme");
        Assertions.assertThat(pomModel.getArtifactId()).isEqualTo("simple-pom");
        Assertions.assertThat(pomModel.getProperties()).hasSize(0);
        File tmpCpy = File.createTempFile("pom-manipulation-test", null);
        FileUtils.copyFile(pomFile.toFile(), tmpCpy);
        PomManipulationUtils.manipulatePom(PomManipulationUtils.loadPomModel(tmpCpy.toPath()), it -> it.getProperties().put("test.prop", "value"));
        Model pomModelAfterSave = PomManipulationUtils.loadPomModel(tmpCpy.toPath());
        Assertions.assertThat(pomModelAfterSave.getProperties()).hasSize(1);
        Assertions.assertThat(pomModelAfterSave.getProperties()).hasEntrySatisfying("test.prop", it -> Assertions.assertThat(it).isEqualTo("value"));
    }

}
