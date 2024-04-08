package ua.com.miltrex.plugin.generator;

import org.apache.maven.plugin.logging.Log;
import ua.com.miltrex.plugin.generator.definitions.MavlinkDefinitionDeserializer;
import ua.com.miltrex.plugin.generator.definitions.model.MavlinkDef;
import ua.com.miltrex.plugin.generator.definitions.model.MavlinkDeprecationDef;
import ua.com.miltrex.plugin.generator.definitions.model.MavlinkEntryDef;
import ua.com.miltrex.plugin.generator.definitions.model.MavlinkEnumDef;
import ua.com.miltrex.plugin.generator.definitions.model.MavlinkFieldDef;
import ua.com.miltrex.plugin.generator.definitions.model.MavlinkMessageDef;
import ua.com.miltrex.plugin.generator.definitions.model.MavlinkParamDef;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class MavlinkGeneratorFactory {

    private final String basePackage;
    private final List<File> xmlResources;

    private final Log log;

    public MavlinkGeneratorFactory(String basePackage, List<File> xmlResources, Log log) {
        this.basePackage = basePackage;
        this.xmlResources = xmlResources;
        this.log = log;
    }

    public MavlinkGenerator newGenerator() {
        MavlinkDefinitionDeserializer deserializer = new MavlinkDefinitionDeserializer();
        List<MavlinkDef> mavlinkDefs = xmlResources.stream()
                .map(f -> loadDefinition(f, deserializer))
                .collect(Collectors.toList());
        mavlinkDefs = sortDefinitions(mavlinkDefs);
        List<PackageGenerator> packageGenerators = new ArrayList<>(mavlinkDefs.size());
        mavlinkDefs.stream()
                .map(md -> visitPackage(basePackage, md, packageGenerators))
                .forEach(packageGenerators::add);
        return new MavlinkGenerator(packageGenerators);
    }

    private PackageGenerator visitPackage(String basePackage, MavlinkDef packageDef, List<PackageGenerator> packageGenerators) {
        String xmlName = packageDef.getName();
        String packageName = basePackage + "." + xmlName.substring(0, xmlName.lastIndexOf('.')).toLowerCase();
        List<PackageGenerator> dependencies = packageGenerators.stream()
                .filter(pg -> packageDef.getIncludes().contains(pg.getXmlName()))
                .collect(Collectors.toList());

        PackageGenerator packageGenerator = new PackageGenerator(
                xmlName,
                packageName,
                dependencies,
                new ArrayList<>(),
                new ArrayList<>());

        packageDef.getEnums()
                .forEach(ed -> visitEnum(basePackage, ed, packageGenerator));

        packageDef.getMessages()
                .stream()
                .map(md -> visitMessage(basePackage, md, packageGenerator))
                .forEach(packageGenerator::addMessage);

        return packageGenerator;
    }

    private MessageGenerator visitMessage(String basePackage, MavlinkMessageDef messageDef, PackageGenerator packageGenerator) {
        MessageGenerator messageGenerator = new MessageGenerator(
                basePackage,
                packageGenerator,
                messageDef.getId(),
                messageDef.getName(),
                packageGenerator.getTypeName(messageDef.getName()),
                messageDef.getDescription(),
                new ArrayList<>(messageDef.getFields().size()),
                visitDeprecation(messageDef.getDeprecation()),
                messageDef.isWorkInProgress());

        messageDef.getFields()
                .stream()
                .map(fd -> visitMessageField(basePackage, fd, packageGenerator))
                .forEach(messageGenerator::addField);

        return messageGenerator;
    }

    private FieldGenerator visitMessageField(String basePackage, MavlinkFieldDef fieldDef, PackageGenerator packageGenerator) {
        return new FieldGenerator(
                basePackage,
                packageGenerator,
                fieldDef.getName(),
                packageGenerator.getFieldName(fieldDef.getName()),
                fieldDef.getDescription(),
                fieldDef.getType().getConvertedType(),
                fieldDef.getEnumName(),
                fieldDef.getIndex(),
                fieldDef.getType().getTypeLength(),
                fieldDef.getType().isArray(),
                fieldDef.getType().getArrayLength(),
                fieldDef.isExtension());
    }

    private void visitEnum(String basePackage, MavlinkEnumDef enumDef, PackageGenerator packageGenerator) {
        EnumGenerator enumGenerator = packageGenerator.resolveEnum(enumDef.getName()).orElse(null);

        if (enumGenerator == null) {
            enumGenerator = new EnumGenerator(
                    basePackage,
                    packageGenerator,
                    enumDef.getName(),
                    packageGenerator.getTypeName(enumDef.getName()),
                    enumDef.getDescription(),
                    new ArrayList<>(enumDef.getEntries().size()),
                    visitDeprecation(enumDef.getDeprecation()));

            packageGenerator.addEnum(enumGenerator);
        }

        final EnumGenerator finalEnumGenerator = enumGenerator;
        enumDef.getEntries()
                .stream()
                .map(ed -> visitEnumEntry(basePackage, ed, finalEnumGenerator, packageGenerator))
                .forEach(enumGenerator::addConstant);

    }

    private EnumConstantGenerator visitEnumEntry(String basePackage, MavlinkEntryDef entryDef, EnumGenerator enumGenerator, PackageGenerator packageGenerator) {
        int value = entryDef.getValue() == null ?
                enumGenerator.maxValue() + 1 :
                entryDef.getValue();

        EnumConstantGenerator constantGenerator = new EnumConstantGenerator(
                basePackage,
                packageGenerator,
                entryDef.getName(),
                value,
                entryDef.getDescription(),
                new ArrayList<>(entryDef.getParams().size()),
                visitDeprecation(entryDef.getDeprecation()));

        entryDef.getParams()
                .stream()
                .map(pd -> visitEnumEntryParam(pd, packageGenerator))
                .forEach(constantGenerator::addParameter);

        return constantGenerator;
    }

    private EnumParameterGenerator visitEnumEntryParam(MavlinkParamDef paramDef, PackageGenerator packageGenerator) {
        return new EnumParameterGenerator(
                packageGenerator,
                paramDef.getIndex(),
                paramDef.getDescription());
    }

    private DeprecationGenerator visitDeprecation(MavlinkDeprecationDef deprecationDef) {
        if (deprecationDef == null) {
            return new DeprecationGenerator(false, "", "", "");
        }
        return new DeprecationGenerator(
                true,
                deprecationDef.getSince(),
                deprecationDef.getReplacedBy(),
                deprecationDef.getMessage());
    }


    private List<MavlinkDef> sortDefinitions(List<MavlinkDef> definitions) {
        List<MavlinkDef> sortedDefinitions = new ArrayList<>(definitions.size());
        definitions = new ArrayList<>(definitions);
        while (!definitions.isEmpty()) {
            MavlinkDef definition = nextDefinitionLeaf(new Stack<>(), definitions, sortedDefinitions, definitions.get(0));
            sortedDefinitions.add(definition);
            definitions.remove(definition);
        }
        return sortedDefinitions;
    }

    private MavlinkDef nextDefinitionLeaf(Stack<String> stack, List<MavlinkDef> work, List<MavlinkDef> sorted, MavlinkDef current) {
        if (stack.contains(current.getName())) {
            int lastCall = stack.lastIndexOf(current.getName());
            String cycle = stack.subList(lastCall, stack.size())
                    .stream()
                    .collect(Collectors.joining(" -> ", "", " -> " + current.getName()));
            throw new IllegalStateException(
                    "Cyclic dependencies for " + current.getName() + ", cycle is: \n" + cycle);
        }
        stack.add(current.getName());
        List<String> unmetDependencies = current.getIncludes()
                .stream()
                .filter(s -> sorted.stream()
                        .map(MavlinkDef::getName).noneMatch(s::equals))
                .toList();
        if (!unmetDependencies.isEmpty()) {
            String dependencyName = unmetDependencies.get(0);
            MavlinkDef dependency = work.stream()
                    .filter(md -> dependencyName.equals(md.getName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            current.getName() + " depends on " + dependencyName + " but such dialect does not exist."));
            return nextDefinitionLeaf(stack, work, sorted, dependency);
        }
        return current;
    }

    private MavlinkDef loadDefinition(File file, MavlinkDefinitionDeserializer deserializer) {
        try (InputStream is = Files.newInputStream(file.toPath())) {
            log.info(String.format("Generate by definition : %s", file.getName()));
            return deserializer.deserialize(is, new File(file.getPath()).getName());
        } catch (IOException e) {
            throw new IllegalStateException("unable to open stream for URL " + file, e);
        } catch (XMLStreamException e) {
            throw new IllegalStateException("malformed XML for URL " + file, e);
        }
    }
}
