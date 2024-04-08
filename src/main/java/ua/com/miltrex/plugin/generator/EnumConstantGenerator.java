package ua.com.miltrex.plugin.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnumConstantGenerator {
    private static ClassName MAVLINK_ENUM_ENTRY;
    private final PackageGenerator parentPackage;
    private final String name;
    private final int value;
    private final String description;
    private final List<EnumParameterGenerator> parameters;
    private final DeprecationGenerator deprecation;

    public EnumConstantGenerator(String basePackage,
                                 PackageGenerator parentPackage,
                                 String name,
                                 int value,
                                 String description,
                                 List<EnumParameterGenerator> parameters,
                                 DeprecationGenerator deprecation) {
        this.parentPackage = parentPackage;
        this.name = name;
        this.value = value;
        this.description = description;
        this.parameters = parameters;
        this.deprecation = deprecation;

        MAVLINK_ENUM_ENTRY = ClassName.get(
                basePackage + ".annotations",
                "MavlinkEntryInfo");
    }

    public String getName() {
        return name;
    }

    public int value() {
        return value;
    }

    public void addParameter(EnumParameterGenerator parameterGenerator) {
        parameters.add(parameterGenerator);
    }

    public AnnotationSpec annotation() {
        return AnnotationSpec.builder(MAVLINK_ENUM_ENTRY)
                .addMember("value", "$L", value)
                .build();
    }

    public List<AnnotationSpec> annotations() {
        List<AnnotationSpec> annotations = new ArrayList<>();
        annotations.add(annotation());
        if (deprecation.deprecated()) {
            annotations.add(deprecation.annotation());
        }
        return annotations;
    }

    public String javadoc() {
        StringBuilder javadoc = new StringBuilder();
        javadoc.append(parentPackage.processJavadoc(description));

        if (!parameters.isEmpty()) {
            String parameterJavadoc = parameters.stream()
                    .map(p -> String.format("  <dt>param%d</dt>\n  <dd>%s</dd>",
                            p.getIndex(),
                            p.javadoc().trim()))
                    .collect(Collectors.joining("\n\n", "<dl>\n", "\n</dl>\n"));
            javadoc.append(parameterJavadoc);
        }

        if (deprecation.deprecated()) {
            javadoc.append(parentPackage.processJavadoc(deprecation.javadoc()));
        }

        return javadoc.toString();
    }

    public TypeSpec generate() {
        return TypeSpec.anonymousClassBuilder("")
                .addJavadoc(javadoc())
                .addAnnotations(annotations())
                .build();
    }
}
