unpack-build-maven-plugin
---
Sometimes there is a need to use classes from a specific build to run e.g. tests with them or similar. This plugin enables this. 

How it works: 
- When the plugin is configured in a Maven module's pom.xml file, it downloads the module's jar file from a Maven repository, in a specified version, into the `target` directory of the module. 
- After the jar is downloaded, the plugin unzips the jar into `target/classes` directory.
- The goal is to have the module in a state similar as after it being compiled. Just in a different version.  

Usages:
---
- Reproduce a bug/problem on older versions using the current tests. E.g. you want to make sure you fixed a problem - when the plugin is configured, it is enough to just run (depends on how you configure it) `mvn clean verify -Dunpackbuild.version=someOtherVersion` to test the old behaviour.
- Test a specific build of a module. Sometimes after a module is built, the tests are executed in a separate phase, even in some separate build environment. In such case, to make sure the tests are executed with the exact specific binary, the source code cannot be recompiled again, because it will produce a different build. Using this plugin enables to test the exact binary build/version needed.

Configuration:
---
You can configure the plugin by adding the configuration to a Maven module's pom.xml file, e.g. like this: 

```
<properties>
  <unpackbuild.version>${project.version}</unpackbuild.version>
</properties>
<plugin>
  <groupId>org.kie</groupId>
  <artifactId>unpack-build-maven-plugin</artifactId>
  <version>1.0-SNAPSHOT</version>
  <executions>
    <execution>
      <id>download-and-unpack-jars</id>
      <phase>compile</phase>
      <goals>
        <goal>unpack-build</goal>
      </goals>
      <configuration>
        <rootDirectory>${project.basedir}</rootDirectory>
        <version>${unpackbuild.version}</version>
      </configuration>
    </execution>
  </executions>
</plugin>
```

This way, you can override the version property from the command line. When combined in a profile with a maven-compiler-plugin configuration that disables the compilation of sources, enabling the profile will basically simulate a build with a different version but with current tests.

Such profile can be configured e.g. like this: 
```
<profile>
  <id>test-with-custom-binaries</id>
  <activation>
    <property>
      <name>unpackbuild.version</name>
    </property>
  </activation>
  <properties>
    <maven.main.skip>true</maven.main.skip>
  </properties>
  <build>
    <plugins>
      <plugin>
        <groupId>org.kie</groupId>
        <artifactId>unpack-build-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <executions>
          <execution>
            <id>download-and-unpack-jars</id>
            <phase>compile</phase>
            <goals>
              <goal>unpack-build</goal>
            </goals>
            <configuration>
              <rootDirectory>${project.basedir}</rootDirectory>
              <version>${unpackbuild.version}</version>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</profile>
```