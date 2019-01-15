package org.kie.unpackbuildplugin.util;

public final class Messages {

    public static final String IGNORE_DIRECTORY = "Directory %s is ignored.";
    public static final String DEFINED_AS_EXCLUDED = "It is defined as excluded.";
    public static final String IGNORE_TYPE = "It contains %s type artifact.";
    public static final String CANNOT_FIND_UNARCHIVER_FOR = "Cannot find unarchiver for %s!";
    public static final String CANNOT_CREATE_DIRECTORIES = "Cannot create directories on path %s!";
    public static final String CANNOT_READ_POM = "Cannot read pom.xml file for module %s!";
    public static final String UNKNOWN_ARTIFACT_PACKAGING = "Unknown artifact packaging %s!";

    private Messages() {
        // It is forbidden to create instances of util classes.
    }
}
