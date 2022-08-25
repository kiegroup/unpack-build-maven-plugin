package org.kie.unpackbuildplugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.kie.unpackbuildplugin.util.PomManipulationUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Allows to override version of dependencies before running unpack-build mojo.
 * Its input is a directory where patched artifacts are located. It loops over *.pom files, reads the GAV and that particular
 * version uses as an override.<br />
 * Then it
 * <ul>
 *   <li>Adds explicit dependencyManagement section with overriden artifacts into top-level project in reactor.</li>
 *   <li>Updates version in a module matching to the patched GAV (matched by groupId and artifactId) - so that the unpack-build goal downloads patched binary.</li>
 * </ul>
 */
@Mojo(name = "override-dependencies", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class OverrideDependenciesMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}")
    private MavenSession mavenSession;

    @Parameter(property = "unpackbuild.overriding.poms.directory", required = true)
    private File overridingPomsDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!mavenSession.getCurrentProject().isExecutionRoot()) {
            return;
        }
        addDependencyManagementEntries(mavenSession.getTopLevelProject().getOriginalModel());
        updateVersionsOfPatchedModules(mavenSession.getAllProjects());
    }

    void addDependencyManagementEntries(Model topLevelProject) throws MojoExecutionException {
        getLog().info(String.format("About to write dependencyManagement info into %s", topLevelProject.getPomFile()));

        PomManipulationUtils.manipulatePom(topLevelProject, mavenProject -> {
            DependencyManagement dependencyManagementToAdd = new DependencyManagement();
            DependencyManagement dependencyManagementOriginal = mavenProject.getModel().getDependencyManagement();
            // reuse existing dependencyManagement entry, possibly override version
            if (dependencyManagementOriginal != null) {
                for (Dependency d : dependencyManagementOriginal.getDependencies()) {
                    if (isAmongPatchedArtifacts(d)) {
                        d.setVersion(getMatchingPatchedArtifact(d).get().getVersion());
                    }
                    dependencyManagementToAdd.addDependency(d);
                }
            }
            // filter patched artifacts not initially present in dependencyManagement and add explicitly
            for (Model m : getListOfPatchedArtifacts()) {
                if (dependencyManagementToAdd.getDependencies().stream().noneMatch(it -> isSameArtifact(it, m))) {
                    Dependency d = new Dependency();
                    d.setArtifactId(m.getArtifactId());
                    d.setGroupId(StringUtils.isEmpty(m.getGroupId()) ? m.getParent().getGroupId() : m.getGroupId());
                    d.setVersion(m.getVersion());
                    dependencyManagementToAdd.addDependency(d);
                }
            }
            mavenProject.getModel().setDependencyManagement(dependencyManagementToAdd);
        });
    }

    void updateVersionsOfPatchedModules(List<MavenProject> projects) throws MojoExecutionException {
        getLog().info("About to update versions of patched modules.");
        for (MavenProject mavenProject : projects) {
            Model model = mavenProject.getOriginalModel();
            if (isAmongPatchedArtifacts(model)) {
                getLog().info(String.format("Upgrading version of module %s", mavenProject.getFile()));
                PomManipulationUtils.manipulatePom(model, it -> {
                    String patchedVersion = getMatchingPatchedArtifact(it.getModel()).get().getVersion();
                    it.setVersion(patchedVersion);
                    getLog().info(String.format("Overriding artifact %s:%s version to %s", it.getGroupId(), it.getArtifactId(), it.getVersion()));
                });
            }
        }
    }

    List<Model> getListOfPatchedArtifacts() {
        List<Model> artifacts = new ArrayList<>();
        if (overridingPomsDirectory == null || !overridingPomsDirectory.isDirectory()) {
            throw new RuntimeException("The provided unzipped patch directory does not exist.");
        }
        for (File pomFile : overridingPomsDirectory.listFiles(it -> it.getName().endsWith(".pom"))) {
            Model model = null;
            try {
                model = PomManipulationUtils.loadPomModel(pomFile.toPath());
            } catch (MojoExecutionException e) {
                throw new RuntimeException("Cannot load pom modules from the unzipped patch directory.", e);
            }
            artifacts.add(model);
        }
        return artifacts;
    }

    boolean isAmongPatchedArtifacts(Model m1) {
        return
                getMatchingPatchedArtifact(m1).isPresent();
    }

    boolean isAmongPatchedArtifacts(Dependency d1) {
        return
                getMatchingPatchedArtifact(d1).isPresent();
    }

    Optional<Model> getMatchingPatchedArtifact(Model m1) {
        return getListOfPatchedArtifacts().stream().filter(it -> isSameArtifact(it, m1)).findAny();
    }

    Optional<Model> getMatchingPatchedArtifact(Dependency d1) {
        return getListOfPatchedArtifacts().stream().filter(it -> isSameArtifact(d1, it)).findAny();
    }

    private boolean isSameArtifact(Model m1, Model m2) {
        if (m1 == null || m2 == null) {
            return false;
        }
        // rely on MavenProject for parent handling (groupId, version)
        MavenProject mp1 = new MavenProject();
        mp1.setModel(m1);
        MavenProject mp2 = new MavenProject();
        mp2.setModel(m2);
        return Objects.equals(mp1.getGroupId(), mp2.getGroupId()) && Objects.equals(mp1.getArtifactId(), mp2.getArtifactId());
    }

    private boolean isSameArtifact(Dependency d1, Model m1) {
        if (d1 == null || m1 == null) {
            return false;
        }
        // rely on MavenProject for parent handling (groupId, version)
        MavenProject mp1 = new MavenProject();
        mp1.setModel(m1);
        return Objects.equals(d1.getGroupId(), mp1.getGroupId()) && Objects.equals(d1.getArtifactId(), mp1.getArtifactId());
    }
}
