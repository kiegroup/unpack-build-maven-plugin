package org.kie.unpackbuildplugin.util;

import java.util.Arrays;

public enum ArtifactPackaging {
    POM,
    JAR,
    WAR,
    BUNDLE,
    MAVEN_PLUGIN,
    MAVEN_ARCHETYPE,
    KJAR,
    GWT_LIB;

    private static final String FILETYPE_POM = "pom";
    private static final String FILETYPE_JAR = "jar";
    private static final String FILETYPE_WAR = "war";

    public static boolean isIgnored(final String type) {
        return Arrays.stream(ArtifactPackaging.values())
                .filter(artifactPackaging -> artifactPackaging == POM)
                .anyMatch((ignoredType) -> ignoredType.name().equalsIgnoreCase(type));
    }

    public static String convertToFileType(final String artifactPackaging) {
        switch (getArtifactPackagingFromString(artifactPackaging)) {
            case POM:
                return FILETYPE_POM;
            case JAR:
                return FILETYPE_JAR;
            case WAR:
                return FILETYPE_WAR;
            case BUNDLE:
                return FILETYPE_JAR;
            case MAVEN_PLUGIN:
                return FILETYPE_JAR;
            case MAVEN_ARCHETYPE:
                return FILETYPE_JAR;
            case KJAR:
                return FILETYPE_JAR;
            case GWT_LIB:
                return FILETYPE_JAR;
            default:
                throw new IllegalArgumentException(String.format(Messages.UNKNOWN_ARTIFACT_PACKAGING, artifactPackaging));
        }
    }

    private static ArtifactPackaging getArtifactPackagingFromString(final String stringValue) {
        if (MAVEN_PLUGIN.toString().equals(stringValue.toUpperCase())) {
            return MAVEN_PLUGIN;
        } else if (MAVEN_ARCHETYPE.toString().equals(stringValue.toUpperCase())) {
            return MAVEN_ARCHETYPE;
        } else if (GWT_LIB.toString().equals(stringValue.toUpperCase())) {
            return GWT_LIB;
        } else {
            return ArtifactPackaging.valueOf(stringValue.toUpperCase());
        }
    }

    @Override
    public String toString() {
        if (this == MAVEN_PLUGIN) {
            return "MAVEN-PLUGIN";
        } else if (this == MAVEN_ARCHETYPE) {
            return "MAVEN-ARCHETYPE";
        } else if (this == GWT_LIB) {
            return "GWT-LIB";
        } else {
            return super.toString();
        }
    }
}
