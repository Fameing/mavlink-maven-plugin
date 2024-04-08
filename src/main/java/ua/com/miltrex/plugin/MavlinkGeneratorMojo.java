package ua.com.miltrex.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import ua.com.miltrex.plugin.generator.MavlinkGenerator;
import ua.com.miltrex.plugin.generator.MavlinkGeneratorFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static java.lang.String.format;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class MavlinkGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "definitions", required = true, readonly = false)
    File definitions;

    @Parameter(property = "basePackage", defaultValue = "ua.com.miltrex.mavlink", readonly = false)
    String basePackage;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources", required = true, readonly = true)
    File generatedSources;

    public File getDefinitions() {
        return definitions;
    }

    public void setDefinitions(File definitions) {
        this.definitions = definitions;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void execute() throws MojoExecutionException {
        // no need to proceed if definitions is null
        if (definitions == null) return;

        if (!definitions.isDirectory()) throw new IllegalArgumentException(
                "'definitions' should be a directory, but got a file instead.");

        if (generatedSources == null) {
            throw new IllegalStateException("'generatedSources' is not specified.");
        }

        if (generatedSources.exists() && !deleteAll(generatedSources)) {
            throw new IllegalStateException("unable to clean generated sources.");
        }

        if (!generatedSources.mkdirs()) {
            throw new IllegalStateException("unable to create 'generatedSources' directory at " + generatedSources.getAbsolutePath());
        }

        //noinspection ConstantConditions
        MavlinkGeneratorFactory generatorFactory = new MavlinkGeneratorFactory(basePackage, Arrays.asList(definitions.listFiles()), getLog());
        MavlinkGenerator generator = generatorFactory.newGenerator();
        generator.generate().forEach(javaFile -> {
            try {
                javaFile.writeTo(generatedSources);
            } catch (IOException e) {
                throw new IllegalStateException("unable to save generated source", e);
            }
        });

        getLog().error(format("Resource count : %s", project.getResources().size()));
    }

    private boolean deleteAll(File f) {
        if (f.isDirectory()) {
            //noinspection ConstantConditions
            Arrays.stream(f.listFiles()).forEach(this::deleteAll);
        }
        return f.delete();
    }
}
