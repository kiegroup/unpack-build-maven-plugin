package org.kie.unpackbuildplugin;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.kie.unpackbuildplugin.util.ArtifactPackaging;
import org.kie.unpackbuildplugin.util.Messages;

@Mojo(name = "unpack-build", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class UnpackBuildMojo extends AbstractMojo {

    // Injected things from environment.

    private static final String TARGET_FOLDER = "target";
    private static final String CLASSES_FOLDER = "classes";


    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    private List<ArtifactRepository> pomRemoteRepositories;

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private ArchiverManager archiverManager;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    // Real parameters from here

    @Parameter(property = "unpackbuild.rootdirectory", required = true)
    private File rootDirectory;

    @Parameter(defaultValue = "${project.version}", property = "unpackbuild.version")
    private String version;

    @Parameter
    private List<String> excludeDirectories;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (excludeDirectories == null) {
            excludeDirectories = new ArrayList<>();
        }
        downloadAndUnpackArtifact(rootDirectory);
    }

    private void downloadAndUnpackArtifact(final File moduleDirectory) throws MojoExecutionException, MojoFailureException {
        excludeDirectories.add(Pattern.quote(new File(moduleDirectory, "src" + File.separator + "test").getAbsolutePath()));
        excludeDirectories.add(Pattern.quote(new File(moduleDirectory, "src" + File.separator + "main").getAbsolutePath()));
        if (!isIgnored(moduleDirectory)) {
            final Model model = readModel(moduleDirectory);
            if (model != null) {
                getLog().info("Processing directory " + moduleDirectory.getAbsolutePath());
                if (!ArtifactPackaging.isIgnored(model.getPackaging())) {
                    final File resolvedFile = resolveArtifact(model);
                    try {
                        extractArtifact(moduleDirectory, resolvedFile);
                    } catch (final NoSuchArchiverException e) {
                        throw new MojoFailureException(String.format(Messages.CANNOT_FIND_UNARCHIVER_FOR, resolvedFile), e);
                    } catch (final IOException e) {
                        throw new MojoExecutionException(String.format(Messages.CANNOT_CREATE_DIRECTORIES, TARGET_FOLDER + File.separator + CLASSES_FOLDER), e);
                    }
                } else {
                    getLog().info(String.format(Messages.IGNORE_DIRECTORY, moduleDirectory.getAbsolutePath())
                                  + " " + String.format(Messages.IGNORE_TYPE,
                                                        ArtifactPackaging.convertToFileType(model.getPackaging())));
                }
            }

            final File[] submodules = moduleDirectory.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
            if (submodules != null) {
                for (final File submodule : submodules) {
                    downloadAndUnpackArtifact(submodule);
                }
            }
        } else {
            // If a directory is ignored, also its subdirectories are ignored.
            getLog().info(String.format(Messages.IGNORE_DIRECTORY, moduleDirectory.getAbsolutePath())
                                  + " " + Messages.DEFINED_AS_EXCLUDED);
        }
    }

    private File resolveArtifact(final Model model) throws MojoExecutionException {
        try {
            final ProjectBuildingRequest buildingRequest =
                    new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

            buildingRequest.setRemoteRepositories(pomRemoteRepositories);

            // TODO - fix classifier
            final ArtifactCoordinate artifactCoordinate = getArtifactCoordinate(getGroupId(model),
                                                                                model.getArtifactId(),
                                                                                version,
                                                                                "",
                                                                                ArtifactPackaging.convertToFileType(model.getPackaging()));

            getLog().info("Resolving " + model.toString());
            final ArtifactResult result = artifactResolver.resolveArtifact(buildingRequest, artifactCoordinate);
            final File resolvedFile = result.getArtifact().getFile();
            getLog().info("Resolved artifact file: " + resolvedFile.getAbsolutePath());
            return resolvedFile;
        } catch (final ArtifactResolverException e) {
            throw new MojoExecutionException("Couldn't download artifact: " + e.getMessage(), e);
        }
    }

    private void extractArtifact(final File baseDirectory, final File artifactFile) throws NoSuchArchiverException, IOException {
        final Path outputPath = Paths.get(baseDirectory.getAbsolutePath(), TARGET_FOLDER, CLASSES_FOLDER);
        Files.createDirectories(outputPath);
        getLog().info("Writing to folder " + outputPath.toString());

        final UnArchiver unArchiver = archiverManager.getUnArchiver(artifactFile);
        unArchiver.setSourceFile(artifactFile);
        unArchiver.setDestDirectory(outputPath.toFile());
        unArchiver.extract();

        Files.copy(artifactFile.toPath(), Paths.get(baseDirectory.getAbsolutePath(), TARGET_FOLDER, artifactFile.getName()));

        excludeDirectories.add(Pattern.quote(outputPath.toAbsolutePath().toString()));
    }

    private ArtifactCoordinate getArtifactCoordinate(final String groupId, final String artifactId, final String version,
                                                     final String classifier, final String type) {
        final DefaultArtifactCoordinate artifactCoordinate = new DefaultArtifactCoordinate();
        artifactCoordinate.setGroupId(groupId);
        artifactCoordinate.setArtifactId(artifactId);
        artifactCoordinate.setVersion(version);
        artifactCoordinate.setClassifier(classifier);
        artifactCoordinate.setExtension(artifactHandlerManager.getArtifactHandler(type).getExtension());
        return artifactCoordinate;
    }

    private Model readModel(final File moduleDirectory) throws MojoExecutionException {
        final File pomFile = new File(moduleDirectory, "pom.xml");
        if (pomFile.exists()) {
            try (final FileReader pomReader = new FileReader(pomFile)) {
                final MavenXpp3Reader reader = new MavenXpp3Reader();
                return reader.read(pomReader);
            } catch (final IOException | XmlPullParserException e) {
                throw new MojoExecutionException(String.format(Messages.CANNOT_READ_POM, moduleDirectory.getAbsolutePath()));
            }
        } else {
            return null;
        }
    }

    private boolean isIgnored(final File directory) {
        final String directoryPath = directory.getAbsolutePath();
        for (final String excludeDirectory : excludeDirectories) {
            if (directoryPath.matches(excludeDirectory)) {
                return true;
            }
        }
        return false;
    }

    private String getGroupId(final Model model) {
        String groupId = model.getGroupId();
        if (groupId == null || "".equals(groupId)) {
            groupId = model.getParent().getGroupId();
        }
        return groupId;
    }
}
