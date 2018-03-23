package io.swagger.codegen.languages.scala;

import com.samskivert.mustache.Escapers;
import com.samskivert.mustache.Mustache;
import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.languages.DefaultCodegenConfig;
import io.swagger.v3.oas.models.media.*;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;

public abstract class AbstractScalaCodegen extends DefaultCodegenConfig {

    protected String invokerPackage = "io.swagger.client";
    protected String sourceFolder = "src/main/Scala";
    protected boolean stripPackageName = true;

    protected List<String> generatorReservedWords() {
        return Arrays.asList();
    }

    protected List<String> scalaReservedWords() {
        return  Arrays.asList(
            "abstract", "case", "catch", "class", "def", "do", "else",
            "extends", "false", "final", "finally", "for", "forSome",
            "if", "implicit", "import", "lazy", "match", "new", "null",
            "object", "override", "package", "private", "protected", "return",
            "sealed", "super", "this", "throw", "trait", "try", "true",
            "type", "val", "var", "while", "with", "yield"
        );
    }

    public AbstractScalaCodegen() {
        super();

        setReservedWordsLowerCase(scalaReservedWords());
        setReservedWordsLowerCase(generatorReservedWords());

        languageSpecificPrimitives.addAll(Arrays.asList(
            "String",
            "boolean",
            "Boolean",
            "Double",
            "Int",
            "Long",
            "Float",
            "Object",
            "Any",
            "List",
            "Seq",
            "Map",
            "Array"
        ));

        instantiationTypes.put("array", "Seq");
        instantiationTypes.put("map", "Map");
        typeMapping.put("date", "Date");
        typeMapping.put("file", "Path");


        cliOptions.add(new CliOption(CodegenConstants.MODEL_PACKAGE, CodegenConstants.MODEL_PACKAGE_DESC));
        cliOptions.add(new CliOption(CodegenConstants.API_PACKAGE, CodegenConstants.API_PACKAGE_DESC));
        cliOptions.add(new CliOption(CodegenConstants.SOURCE_FOLDER, CodegenConstants.SOURCE_FOLDER_DESC));
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(CodegenConstants.SOURCE_FOLDER)) {
            this.setSourceFolder(
                (String) additionalProperties.get(CodegenConstants.SOURCE_FOLDER)
            );
        }
    }

    public void setSourceFolder(String sourceFolder) {
        this.sourceFolder = sourceFolder;
    }

    public String getSourceFolder() {
        return sourceFolder;
    }

    @Override
    public String escapeReservedWord(String name) {
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        // Reserved words will be further escaped at the mustache compiler level.
        // Scala escaping done here (via `, without compiler escaping) would otherwise be HTML encoded.
        return "`" + name + "`";
    }

    public Mustache.Compiler processCompiler(Mustache.Compiler compiler) {
        Mustache.Escaper SCALA = new Mustache.Escaper() {
            @Override public String escape (String text) {
                // Fix included as suggested by akkie in #6393
                // The given text is a reserved word which is escaped by enclosing it with grave accents. If we would
                // escape that with the default Mustache `HTML` escaper, then the escaper would also escape our grave
                // accents. So we remove the grave accents before the escaping and add it back after the escaping.
                if (text.startsWith("`") && text.endsWith("`")) {
                    String unescaped =  text.substring(1, text.length() - 1);
                    return "`" + Escapers.HTML.escape(unescaped) + "`";
                }

                // All none reserved words will be escaped with the default Mustache `HTML` escaper
                return Escapers.HTML.escape(text);
            }
        };

        return compiler.withEscaper(SCALA);
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
    }

    @Override
    public String getTypeDeclaration(Schema p) {
        if (p instanceof ArraySchema) {
            ArraySchema ap = (ArraySchema) p;
            Schema inner = ap.getItems();
            return getSchemaType(p) + "[" + getTypeDeclaration(inner) + "]";
        } else if (p instanceof MapSchema) {
            MapSchema mp = (MapSchema) p;
            Schema inner = (Schema) mp.getAdditionalProperties();
            return getSchemaType(p) +
                "[String, " + getTypeDeclaration(inner) + "]";
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    public String getSchemaType(Schema p) {
        String swaggerType = super.getSchemaType(p);
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type)) {
                return toModelName(type);
            }
        } else {
            type = swaggerType;
        }
        return toModelName(type);
    }

    @Override
    public String toInstantiationType(Schema p) {
        if (p instanceof MapSchema) {
            MapSchema ap = (MapSchema) p;
            String inner = getSchemaType((Schema)ap.getAdditionalProperties());
            return instantiationTypes.get("map") + "[String, " + inner + "]";
        } else if (p instanceof ArraySchema) {
            ArraySchema ap = (ArraySchema) p;
            String inner = getSchemaType(ap.getItems());
            return instantiationTypes.get("array") + "[" + inner + "]";
        } else {
            return null;
        }
    }

    public String toDefaultValue(Schema p) {
        if (p instanceof StringSchema) {
            return "null";
        } else if (p instanceof BooleanSchema) {
            return "null";
        } else if (p instanceof DateSchema) {
            return "null";
        } else if (p instanceof DateTimeSchema) {
            return "null";
        } else if (p instanceof IntegerSchema) {
            return "0";
        } else if (p instanceof NumberSchema) {
            return "0.0";
        } else if (p instanceof MapSchema) {
            MapSchema ap = (MapSchema) p;
            String inner = getSchemaType((Schema)ap.getAdditionalProperties());
            return "new Map[String, " + inner + "]() ";
        } else if (p instanceof ArraySchema) {
            ArraySchema ap = (ArraySchema) p;
            String inner = getSchemaType(ap.getItems());
            return "new Vector[" + inner + "]() ";
        } else {
            return "null";
        }
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        // remove model imports to avoid warnings for importing class in the same package in Scala
        List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
        final String prefix = modelPackage() + ".";
        Iterator<Map<String, String>> iterator = imports.iterator();
        while (iterator.hasNext()) {
            String _import = iterator.next().get("import");
            if (_import.startsWith(prefix)) iterator.remove();
        }
        return objs;
    }

    @Override
    public String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name);
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    protected String formatIdentifier(String name, boolean capitalized) {
        String identifier = camelize(sanitizeName(name), true);
        if (capitalized) {
            identifier = StringUtils.capitalize(identifier);
        }
        if (identifier.matches("[a-zA-Z_$][\\w_$]+") && !isReservedWord(identifier)) {
            return identifier;
        }
        return escapeReservedWord(identifier);
    }

    protected String stripPackageName(String input) {
        if (!stripPackageName || StringUtils.isEmpty(input) || input.lastIndexOf(".") < 0)
            return input;

        int lastIndexOfDot = input.lastIndexOf(".");
        return input.substring(lastIndexOfDot + 1);
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }
}
