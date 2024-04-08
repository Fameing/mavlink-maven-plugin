package ua.com.miltrex.plugin.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PackageGenerator {
    private static ClassName MAVLINK_DIALECT;
    private static ClassName ABSTRACT_MAVLINK_DIALECT;
    private static ClassName UNMODIFIABLE_MAP_BUILDER;

    private final String packageName;
    private final String xmlName;
    private final List<PackageGenerator> dependencies;
    private final List<EnumGenerator> enums;
    private final List<MessageGenerator> messages;

    public PackageGenerator(String xmlName,
                            String packageName,
                            List<PackageGenerator> dependencies,
                            List<EnumGenerator> enums,
                            List<MessageGenerator> messages) {
        this.xmlName = xmlName;
        this.packageName = packageName;
        this.dependencies = dependencies;
        this.enums = enums;
        this.messages = messages;


        MAVLINK_DIALECT = ClassName.get(
                packageName,
                "MavlinkDialect");

        ABSTRACT_MAVLINK_DIALECT = ClassName.get(
                packageName,
                "AbstractMavlinkDialect");

        UNMODIFIABLE_MAP_BUILDER = ClassName.get(
                packageName + ".util",
                "UnmodifiableMapBuilder");
    }

    public String getPackageName() {
        return packageName;
    }

    public String dialectName() {
        return xmlName.substring(0, xmlName.lastIndexOf('.')).toLowerCase();
    }

    public ClassName dialectClassName() {
        return ClassName.get(packageName, toUpperCamelCase(dialectName()) + "Dialect");
    }

    public List<EnumGenerator> getEnumsIncludingDependencies() {
        return Stream.concat(
                        enums.stream(),
                        dependencies.stream()
                                .map(PackageGenerator::getEnumsIncludingDependencies)
                                .flatMap(List::stream))
                .collect(Collectors.toList());
    }

    public Optional<EnumGenerator> resolveEnum(String name) {
        return getEnumsIncludingDependencies().stream()
                .filter(e -> name.equals(e.getName()))
                .findFirst();
    }

    public List<MessageGenerator> getMessagesIncludingDependencies() {
        return Stream.concat(
                        messages.stream(),
                        dependencies.stream()
                                .map(pg -> pg.messages)
                                .flatMap(List::stream))
                .collect(Collectors.toList());
    }

    public Optional<MessageGenerator> resolveMessage(String name) {
        return getMessagesIncludingDependencies().stream()
                .filter(m -> name.equals(m.getName()))
                .findFirst();
    }

    public ClassName resolve(String name) {
        return resolveEnum(name)
                .map(EnumGenerator::getClassName)
                .orElseGet(() -> resolveMessage(name)
                        .map(MessageGenerator::getClassName)
                        .orElse(null));
    }

    public ClassName getTypeName(String name) {
        return ClassName.get(packageName, toUpperCamelCase(name));
    }

    public String getFieldName(String name) {
        return toCamelCase(name);
    }

    public void addEnum(EnumGenerator enumGenerator) {
        enums.add(enumGenerator);
    }

    public void addMessage(MessageGenerator messageGenerator) {
        messages.add(messageGenerator);
    }

    public String processJavadoc(String description) {
        if (description == null) {
            description = "";
        }
        Matcher matcher = Pattern.compile("([A-Z_]+)").matcher(
                wordWrap(description, 80));
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            ClassName className = resolve(name);

            if (className != null) {
                matcher.appendReplacement(sb, String.format(
                        "{@link %s %s}",
                        className,
                        name));
            }
        }
        matcher.appendTail(sb);
        sb.append("\n");
        return sb.toString()
                .replace("&", "&amp;")
                .replace(">", "&gt;")
                .replace("<", "&lt;");
    }

    public String getXmlName() {
        return xmlName;
    }

    public TypeSpec generateDialect() {
        CodeBlock.Builder dependenciesInitializer = CodeBlock.builder();
        if (dependencies.isEmpty()) {
            dependenciesInitializer.add("$T.emptyList()", Collections.class);
        } else {
            dependenciesInitializer.add("$T.asList$>$>", Arrays.class);
            dependenciesInitializer.add(
                    dependencies.stream()
                            .map(dep -> CodeBlock.builder()
                                    .add("new $T()", dep.dialectClassName())
                                    .build())
                            .collect(CodeBlock.joining(",\n", "(\n", ")")));
            dependenciesInitializer.add("$<$<");
        }

        CodeBlock.Builder messageTypesInitializer = CodeBlock.builder();
        if (messages.isEmpty()) {
            messageTypesInitializer.add("$T.emptyMap()", Collections.class);
        } else {
            messageTypesInitializer.add("new $T()\n$>$>", ParameterizedTypeName.get(
                    UNMODIFIABLE_MAP_BUILDER,
                    TypeName.get(Integer.class),
                    TypeName.get(Class.class)));
            messageTypesInitializer.add(
                    messages.stream()
                            .map(m -> CodeBlock.builder()
                                    .add(".put($L, $T.class)", m.getId(), m.getClassName())
                                    .build())
                            .collect(CodeBlock.joining("\n", "", "\n.build()")));
            messageTypesInitializer.add("$<$<");
        }

        return TypeSpec.classBuilder(dialectClassName())
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .superclass(ABSTRACT_MAVLINK_DIALECT)
                .addField(FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(List.class), MAVLINK_DIALECT),
                                "dependencies",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .addJavadoc("A list of all of the dependencies of this dialect.\n")
                        .initializer(dependenciesInitializer.build())
                        .build())
                .addField(FieldSpec.builder(
                                ParameterizedTypeName.get(Map.class, Integer.class, Class.class),
                                "messages",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .addJavadoc("A list of all message types supported by this dialect.\n")
                        .initializer(messageTypesInitializer.build())
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("super($S, $N, $N)",
                                dialectName(),
                                "dependencies",
                                "messages")
                        .build())
                .build();
    }

    public List<JavaFile> generate() {
        return Stream.concat(
                        Stream.concat(
                                messages.stream().map(MessageGenerator::generate),
                                enums.stream().map(EnumGenerator::generate)),
                        Stream.of(generateDialect()))
                .map(ts -> JavaFile.builder(packageName, ts)
                        .indent("    ")
                        .build())
                .collect(Collectors.toList());
    }

    private String wordWrap(String text, int max) {
        AtomicInteger line = new AtomicInteger(0);
        StringBuilder sb = new StringBuilder();
        Arrays.stream(text.split("\\s+"))
                .forEach(word -> {
                    int next = line.get() + word.length();
                    if (next > max) {
                        sb.append("\n");
                        line.set(word.length());
                    } else {
                        line.set(next);
                    }
                    sb.append(word).append(" ");
                });
        return sb.toString();
    }

    private String toUpperCamelCase(String upperUnderscore) {
        return Arrays.stream(upperUnderscore.split("_"))
                .map(String::toLowerCase)
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(""));
    }

    private String toCamelCase(String underscored) {
        String upperCamelCase = toUpperCamelCase(underscored);
        return Character.toLowerCase(upperCamelCase.charAt(0)) + upperCamelCase.substring(1);
    }
}
