package org.kie.unpackbuildplugin.util;

import java.util.Arrays;

public enum ArtifactPackaging {
    POM, JAR, WAR, BUNDLE;

    private static final String FILETYPE_POM = "pom";
    private static final String FILETYPE_JAR = "jar";
    private static final String FILETYPE_WAR = "war";

    public static boolean isIgnored(final String type) {
        return Arrays.stream(ArtifactPackaging.values())
                .filter(artifactPackaging -> artifactPackaging == POM)
                .anyMatch((ignoredType) -> ignoredType.name().equalsIgnoreCase(type));
    }

    public static String convertToFileType(final String artifactPackaging) {
        switch (ArtifactPackaging.valueOf(artifactPackaging.toUpperCase())) {
            case POM: return FILETYPE_POM;
            case JAR: return FILETYPE_JAR;
            case WAR: return FILETYPE_WAR;
            case BUNDLE: return FILETYPE_JAR;
            default:
                throw new IllegalArgumentException(String.format(Messages.UNKNOWN_ARTIFACT_PACKAGING, artifactPackaging));
        }
    }
}
