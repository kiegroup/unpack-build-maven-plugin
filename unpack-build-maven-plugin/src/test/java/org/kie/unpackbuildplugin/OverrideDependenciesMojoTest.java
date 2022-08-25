package org.kie.unpackbuildplugin;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.kie.unpackbuildplugin.util.PomManipulationUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;

public class OverrideDependenciesMojoTest {
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    public OverrideDependenciesMojo getMojo(String pomResourceString) {
        try {
            return (OverrideDependenciesMojo) rule.lookupMojo("override-dependencies", new File(getClass().getResource(pomResourceString).toURI()));
        } catch (Exception e) {
            Assertions.fail("Failure initializing mojo in tests.", e);
        }
        return null;
    }

    @Test
    public void testGetListOfPatchedArtifacts() throws MojoExecutionException {
        OverrideDependenciesMojo mojo = getMojo("/override-dependencies-mojo/project1/pom.xml");
        Assertions.assertThat(mojo.getListOfPatchedArtifacts()).hasSize(2);
        Assertions.assertThat(mojo.getListOfPatchedArtifacts()).anyMatch(it -> {
            MavenProject mp = new MavenProject();
            mp.setModel(it);
            return mp.getGroupId().equals("org.acme")
                    && mp.getArtifactId().equals("module1")
                    && mp.getVersion().equals("1.0-patched");
        });
    }

    @Test
    public void testIsAmongPatchedArtifacts() {
        OverrideDependenciesMojo mojo = getMojo("/override-dependencies-mojo/project1/pom.xml");
        Model m = new Model();
        m.setGroupId("org.acme");
        m.setArtifactId("module1");
        Assertions.assertThat(mojo.isAmongPatchedArtifacts(m)).isEqualTo(true);
        Dependency d = new Dependency();
        d.setGroupId("org.acme");
        d.setArtifactId("module1");
        Assertions.assertThat(mojo.isAmongPatchedArtifacts(d)).isEqualTo(true);
        Model m2 = new Model();
        m2.setGroupId("org.acme");
        m2.setArtifactId("module2");
        Assertions.assertThat(mojo.isAmongPatchedArtifacts(m2)).isEqualTo(false);
        Dependency d2 = new Dependency();
        d2.setGroupId("org.acme");
        d2.setArtifactId("module2");
        Assertions.assertThat(mojo.isAmongPatchedArtifacts(d2)).isEqualTo(false);
    }

    @Test
    public void testAddDependencyManagementSection() throws MojoExecutionException {
        OverrideDependenciesMojo mojo = getMojo("/override-dependencies-mojo/project1/pom.xml");
        Model topLevelProject = PomManipulationUtils.loadPomModel(Paths.get(getClass().getResource("/override-dependencies-mojo/project1/pom.xml").getFile()));
        mojo.addDependencyManagementEntries(topLevelProject);
        DependencyManagement updatedDependencyManagement = topLevelProject.getDependencyManagement();
        Assertions.assertThat(updatedDependencyManagement).isNotNull();
        Assertions.assertThat(updatedDependencyManagement.getDependencies()).hasSize(3);
        Assertions.assertThat(updatedDependencyManagement.getDependencies()).anyMatch(it ->
                it.getGroupId().equals("org.acme")
                        && it.getArtifactId().equals("module1")
                        && it.getVersion().equals("1.0-patched"));
    }

    @Test
    public void testUpdateVersionsOfPatchedModules() throws MojoExecutionException {
        OverrideDependenciesMojo mojo = getMojo("/override-dependencies-mojo/project1/pom.xml");

        Model parentModel = PomManipulationUtils.loadPomModel(Paths.get(getClass().getResource("/override-dependencies-mojo/project1/pom.xml").getFile()));
        MavenProject parentProject = new MavenProject();
        parentProject.setModel(parentModel);
        parentProject.setOriginalModel(parentModel);

        Model module1Model = PomManipulationUtils.loadPomModel(Paths.get(getClass().getResource("/override-dependencies-mojo/project1/module1/pom.xml").getFile()));
        MavenProject module1Project = new MavenProject();
        module1Project.setModel(module1Model);
        module1Project.setOriginalModel(module1Model);

        Model module2model = PomManipulationUtils.loadPomModel(Paths.get(getClass().getResource("/override-dependencies-mojo/project1/module2/pom.xml").getFile()));
        MavenProject module2Project = new MavenProject();
        module2Project.setModel(module2model);
        module2Project.setOriginalModel(module2model);

        mojo.updateVersionsOfPatchedModules(Arrays.asList(new MavenProject[]{parentProject, module1Project, module2Project}));

        Assertions.assertThat(parentModel.getVersion()).isEqualTo("1.0");
        Assertions.assertThat(parentProject.getVersion()).isEqualTo("1.0");
        Assertions.assertThat(module1Model.getVersion()).isEqualTo("1.0-patched");
        Assertions.assertThat(module1Project.getVersion()).isEqualTo("1.0-patched");
        Assertions.assertThat(module2model.getVersion()).isNull();
        Assertions.assertThat(module2Project.getVersion()).isEqualTo("1.0");
    }
}
