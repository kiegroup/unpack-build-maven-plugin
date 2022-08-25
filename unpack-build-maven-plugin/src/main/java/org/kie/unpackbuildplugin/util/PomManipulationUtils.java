package org.kie.unpackbuildplugin.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class PomManipulationUtils {

    private PomManipulationUtils() {
        // private constructor to prevent instantiation
    }

    /**
     * Get Maven Model from given pomFile.
     *
     * @param pomFile path to pom.xml
     * @return
     * @throws MojoExecutionException
     */
    public static Model loadPomModel(Path pomFile) throws MojoExecutionException {
        Model model = null;
        try (
                FileInputStream fileReader = new FileInputStream(pomFile.toFile());) {
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            model = mavenReader.read(fileReader);
            model.setPomFile(pomFile.toFile());
        } catch (IOException | XmlPullParserException e) {
            throw new MojoExecutionException("Error while opening generated pom: " + pomFile, e);
        }
        return model;
    }

    /**
     * Method that accepts maven Model for given pom file and operation to be applied on the MavenProject
     * instance denoted by the Model instance.
     *
     * @param model       Loaded maven pom model to manipulate and save to after changes.
     * @param manipulator consumer that receives {@linkplain MavenProject} instance.
     * @throws MojoExecutionException when error during manipulation occurs.
     */
    public static void manipulatePom(Model model, Consumer<MavenProject> manipulator) throws MojoExecutionException {
        try (
                FileOutputStream fileWriter = new FileOutputStream(model.getPomFile())) {
            MavenProject project = new MavenProject(model);
            manipulator.accept(project);
            MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
            mavenWriter.write(fileWriter, model);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while saving manipulated pom: " + model.getPomFile(), e);
        }
    }
}
