package io.swagger.codegen.languages;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenParameter;
import io.swagger.codegen.CodegenProperty;
import io.swagger.models.properties.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.regex.Matcher;

import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPhpCodegen extends DefaultCodegenConfig {

    static Logger LOGGER = LoggerFactory.getLogger(AbstractPhpCodegen.class);

    public static final String VARIABLE_NAMING_CONVENTION = "variableNamingConvention";
    public static final String PACKAGE_PATH = "packagePath";
    public static final String SRC_BASE_PATH = "srcBasePath";
    // composerVendorName/composerProjectName has be replaced by gitUserId/gitRepoId. prepare to remove these.
    // public static final String COMPOSER_VENDOR_NAME = "composerVendorName";
    // public static final String COMPOSER_PROJECT_NAME = "composerProjectName";
    // protected String composerVendorName = null;
    // protected String composerProjectName = null;
    protected String invokerPackage = "php";
    protected String packagePath = "php-base";
    protected String artifactVersion = null;
    protected String srcBasePath = "lib";
    protected String testBasePath = "test";
    protected String docsBasePath = "docs";
    protected String apiDirName = "Api";
    protected String modelDirName = "Model";
    protected String variableNamingConvention= "snake_case";
    protected String apiDocPath = docsBasePath + File.separator + apiDirName;
    protected String modelDocPath = docsBasePath + File.separator + modelDirName;

    public AbstractPhpCodegen() {
        super();

        modelTemplateFiles.put("model.mustache", ".php");
        apiTemplateFiles.put("api.mustache", ".php");
        apiTestTemplateFiles.put("api_test.mustache", ".php");
        modelDocTemplateFiles.put("model_doc.mustache", ".md");
        apiDocTemplateFiles.put("api_doc.mustache", ".md");

        apiPackage = invokerPackage + "\\" + apiDirName;
        modelPackage = invokerPackage + "\\" + modelDirName;

        setReservedWordsLowerCase(
                Arrays.asList(
                    // local variables used in api methods (endpoints)
                    "resourcePath", "httpBody", "queryParams", "headerParams",
                    "formParams", "_header_accept", "_tempBody",

                    // PHP reserved words
                    "__halt_compiler", "abstract", "and", "array", "as", "break", "callable", "case", "catch", "class", "clone", "const", "continue", "declare", "default", "die", "do", "echo", "else", "elseif", "empty", "enddeclare", "endfor", "endforeach", "endif", "endswitch", "endwhile", "eval", "exit", "extends", "final", "for", "foreach", "function", "global", "goto", "if", "implements", "include", "include_once", "instanceof", "insteadof", "interface", "isset", "list", "namespace", "new", "or", "print", "private", "protected", "public", "require", "require_once", "return", "static", "switch", "throw", "trait", "try", "unset", "use", "var", "while", "xor")
        );

        // ref: http://php.net/manual/en/language.types.intro.php
        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList(
                        "bool",
                        "boolean",
                        "int",
                        "integer",
                        "double",
                        "float",
                        "string",
                        "object",
                        "DateTime",
                        "mixed",
                        "number",
                        "void",
                        "byte")
        );

        instantiationTypes.put("array", "array");
        instantiationTypes.put("map", "map");


        // provide primitives to mustache template
        String primitives = "'" + StringUtils.join(languageSpecificPrimitives, "', '") + "'";
        additionalProperties.put("primitives", primitives);

        // ref: https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#data-types
        typeMapping = new HashMap<String, String>();
        typeMapping.put("integer", "int");
        typeMapping.put("long", "int");
        typeMapping.put("number", "float");
        typeMapping.put("float", "float");
        typeMapping.put("double", "double");
        typeMapping.put("string", "string");
        typeMapping.put("byte", "int");
        typeMapping.put("boolean", "bool");
        typeMapping.put("Date", "\\DateTime");
        typeMapping.put("DateTime", "\\DateTime");
        typeMapping.put("file", "\\SplFileObject");
        typeMapping.put("map", "map");
        typeMapping.put("array", "array");
        typeMapping.put("list", "array");
        typeMapping.put("object", "object");
        typeMapping.put("binary", "string");
        typeMapping.put("ByteArray", "string");
        typeMapping.put("UUID", "string");

        cliOptions.add(new CliOption(CodegenConstants.MODEL_PACKAGE, CodegenConstants.MODEL_PACKAGE_DESC));
        cliOptions.add(new CliOption(CodegenConstants.API_PACKAGE, CodegenConstants.API_PACKAGE_DESC));
        cliOptions.add(new CliOption(VARIABLE_NAMING_CONVENTION, "naming convention of variable name, e.g. camelCase.")
                .defaultValue("snake_case"));
        cliOptions.add(new CliOption(CodegenConstants.INVOKER_PACKAGE, "The main namespace to use for all classes. e.g. Yay\\Pets"));
        cliOptions.add(new CliOption(PACKAGE_PATH, "The main package name for classes. e.g. GeneratedPetstore"));
        cliOptions.add(new CliOption(SRC_BASE_PATH, "The directory under packagePath to serve as source root."));
        // cliOptions.add(new CliOption(COMPOSER_VENDOR_NAME, "The vendor name used in the composer package name. The template uses {{composerVendorName}}/{{composerProjectName}} for the composer package name. e.g. yaypets. IMPORTANT NOTE (2016/03): composerVendorName will be deprecated and replaced by gitUserId in the next swagger-codegen release"));
        cliOptions.add(new CliOption(CodegenConstants.GIT_USER_ID, CodegenConstants.GIT_USER_ID_DESC));
        // cliOptions.add(new CliOption(COMPOSER_PROJECT_NAME, "The project name used in the composer package name. The template uses {{composerVendorName}}/{{composerProjectName}} for the composer package name. e.g. petstore-client. IMPORTANT NOTE (2016/03): composerProjectName will be deprecated and replaced by gitRepoId in the next swagger-codegen release"));
        cliOptions.add(new CliOption(CodegenConstants.GIT_REPO_ID, CodegenConstants.GIT_REPO_ID_DESC));
        cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_VERSION, "The version to use in the composer package version field. e.g. 1.2.3"));
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(PACKAGE_PATH)) {
            this.setPackagePath((String) additionalProperties.get(PACKAGE_PATH));
        } else {
            additionalProperties.put(PACKAGE_PATH, packagePath);
        }

        if (additionalProperties.containsKey(SRC_BASE_PATH)) {
            this.setSrcBasePath((String) additionalProperties.get(SRC_BASE_PATH));
        } else {
            additionalProperties.put(SRC_BASE_PATH, srcBasePath);
        }

        if (additionalProperties.containsKey(CodegenConstants.INVOKER_PACKAGE)) {
            this.setInvokerPackage((String) additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
        } else {
            additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);
        }

        if (!additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            additionalProperties.put(CodegenConstants.MODEL_PACKAGE, modelPackage);
        }

        if (!additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            additionalProperties.put(CodegenConstants.API_PACKAGE, apiPackage);
        }

        // if (additionalProperties.containsKey(COMPOSER_PROJECT_NAME)) {
        //     this.setComposerProjectName((String) additionalProperties.get(COMPOSER_PROJECT_NAME));
        // } else {
        //     additionalProperties.put(COMPOSER_PROJECT_NAME, composerProjectName);
        // }

        if (additionalProperties.containsKey(CodegenConstants.GIT_USER_ID)) {
            this.setGitUserId((String) additionalProperties.get(CodegenConstants.GIT_USER_ID));
        } else {
            additionalProperties.put(CodegenConstants.GIT_USER_ID, gitUserId);
        }

        // if (additionalProperties.containsKey(COMPOSER_VENDOR_NAME)) {
        //     this.setComposerVendorName((String) additionalProperties.get(COMPOSER_VENDOR_NAME));
        // } else {
        //     additionalProperties.put(COMPOSER_VENDOR_NAME, composerVendorName);
        // }

        if (additionalProperties.containsKey(CodegenConstants.GIT_REPO_ID)) {
            this.setGitRepoId((String) additionalProperties.get(CodegenConstants.GIT_REPO_ID));
        } else {
            additionalProperties.put(CodegenConstants.GIT_REPO_ID, gitRepoId);
        }

        if (additionalProperties.containsKey(CodegenConstants.ARTIFACT_VERSION)) {
            this.setArtifactVersion((String) additionalProperties.get(CodegenConstants.ARTIFACT_VERSION));
        } else {
            additionalProperties.put(CodegenConstants.ARTIFACT_VERSION, artifactVersion);
        }

        if (additionalProperties.containsKey(VARIABLE_NAMING_CONVENTION)) {
            this.setParameterNamingConvention((String) additionalProperties.get(VARIABLE_NAMING_CONVENTION));
        }

        additionalProperties.put("escapedInvokerPackage", invokerPackage.replace("\\", "\\\\"));

        // make api and model src path available in mustache template
        additionalProperties.put("apiSrcPath", "." + File.separator + toSrcPath(apiPackage, srcBasePath));
        additionalProperties.put("modelSrcPath", "." + File.separator + toSrcPath(modelPackage, srcBasePath));
        additionalProperties.put("apiTestPath", "." + File.separator + testBasePath + File.separator + apiDirName);
        additionalProperties.put("modelTestPath", "." + File.separator + testBasePath + File.separator + modelDirName);

        // make api and model doc path available in mustache template
        additionalProperties.put("apiDocPath", apiDocPath);
        additionalProperties.put("modelDocPath", modelDocPath);

        // make test path available in mustache template
        additionalProperties.put("testBasePath", testBasePath);

        // // apache v2 license
        // supportingFiles.add(new SupportingFile("LICENSE", getPackagePath(), "LICENSE"));
    }

    public String getPackagePath() {
        return packagePath;
    }

    public String toPackagePath(String packageName, String basePath) {
        return (getPackagePath() + File.separatorChar + toSrcPath(packageName, basePath));
    }

    public String toSrcPath(String packageName, String basePath) {
        packageName = packageName.replace(invokerPackage, ""); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
        if (basePath != null && basePath.length() > 0) {
            basePath = basePath.replaceAll("[\\\\/]?$", "") + File.separatorChar; // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
        }

        String regFirstPathSeparator;
        if ("/".equals(File.separator)) { // for mac, linux
            regFirstPathSeparator = "^/";
        } else { // for windows
            regFirstPathSeparator = "^\\\\";
        }

        String regLastPathSeparator;
        if ("/".equals(File.separator)) { // for mac, linux
            regLastPathSeparator = "/$";
        } else { // for windows
            regLastPathSeparator = "\\\\$";
        }

        return (basePath
                // Replace period, backslash, forward slash with file separator in package name
                + packageName.replaceAll("[\\.\\\\/]", Matcher.quoteReplacement(File.separator))
                // Trim prefix file separators from package path
                .replaceAll(regFirstPathSeparator, ""))
                // Trim trailing file separators from the overall path
                .replaceAll(regLastPathSeparator+ "$", "");
    }

   @Override
    public String escapeReservedWord(String name) {           
        if(this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name;
    }

    @Override
    public String apiFileFolder() {
        return (outputFolder + File.separator + toPackagePath(apiPackage, srcBasePath));
    }

    @Override
    public String modelFileFolder() {
        return (outputFolder + File.separator + toPackagePath(modelPackage, srcBasePath));
    }

    @Override
    public String apiTestFileFolder() {
        return (outputFolder + File.separator + getPackagePath() + File.separator + testBasePath + File.separator + apiDirName);
    }

    @Override
    public String modelTestFileFolder() {
        return (outputFolder + File.separator + getPackagePath() + File.separator + testBasePath + File.separator + modelDirName);
    }

    @Override
    public String apiDocFileFolder() {
        return (outputFolder + File.separator + getPackagePath() + File.separator + apiDocPath);
    }

    @Override
    public String modelDocFileFolder() {
        return (outputFolder + File.separator + getPackagePath() + File.separator + modelDocPath);
    }

    @Override
    public String toModelDocFilename(String name) {
        return toModelName(name);
    }

    @Override
    public String toApiDocFilename(String name) {
        return toApiName(name);
    }

    @Override
    public String getTypeDeclaration(Schema propertySchema) {
        if (propertySchema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) propertySchema;
            Schema inner = arraySchema.getItems();
            if (inner == null) {
                LOGGER.warn(arraySchema.getName() + "(array property) does not have a proper inner type defined");
                return "";
            }
            return getTypeDeclaration(inner) + "[]";
        } else if (propertySchema instanceof MapSchema) {
            MapSchema mapSchema = (MapSchema) propertySchema;
            Schema inner = (Schema) mapSchema.getAdditionalProperties();
            if (inner == null) {
                LOGGER.warn(propertySchema.getName() + "(map property) does not have a proper inner type defined");
                return "";
            }
            return getSchemaType(propertySchema) + "[string," + getTypeDeclaration(inner) + "]";
        }

        return super.getTypeDeclaration(propertySchema);
    }

    @Override
    public String getTypeDeclaration(String name) {
        if (!languageSpecificPrimitives.contains(name)) {
            return "\\" + modelPackage + "\\" + name;
        }
        return super.getTypeDeclaration(name);
    }

    @Override
    public String getSchemaType(Schema property) {
        String schemaType = super.getSchemaType(property);
        String type = null;
        if (typeMapping.containsKey(schemaType)) {
            type = typeMapping.get(schemaType);
            if (languageSpecificPrimitives.contains(type)) {
                return type;
            } else if (instantiationTypes.containsKey(type)) {
                return type;
            }
        } else {
            type = schemaType;
        }
        if (type == null) {
            return null;
        }
        return toModelName(type);
    }

    public void setInvokerPackage(String invokerPackage) {
        this.invokerPackage = invokerPackage;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
    }

    public void setSrcBasePath(String srcBasePath) {
        this.srcBasePath = srcBasePath;
    }

    public void setParameterNamingConvention(String variableNamingConvention) {
        this.variableNamingConvention = variableNamingConvention;
    }

    // public void setComposerVendorName(String composerVendorName) {
    //     this.composerVendorName = composerVendorName;
    // }

    // public void setComposerProjectName(String composerProjectName) {
    //     this.composerProjectName = composerProjectName;
    // }

    @Override
    public String toVarName(String name) {
        // sanitize name
        name = sanitizeName(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        if ("camelCase".equals(variableNamingConvention)) {
          // return the name in camelCase style
          // phone_number => phoneNumber
          name =  camelize(name, true);
        } else { // default to snake case
          // return the name in underscore style
          // PhoneNumber => phone_number
          name =  underscore(name);
        }

        // parameter name starting with number won't compile
        // need to escape it by appending _ at the beginning
        if (name.matches("^\\d.*")) {
            name = "_" + name;
        }

        return name;
    }

    @Override
    public String toParamName(String name) {
        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String toModelName(String name) {
        // remove [
        name = name.replaceAll("\\]", "");

        // Note: backslash ("\\") is allowed for e.g. "\\DateTime"
        name = name.replaceAll("[^\\w\\\\]+", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // remove dollar sign
        name = name.replaceAll("$", "");

        // model name cannot use reserved keyword
        if (isReservedWord(name)) {
            LOGGER.warn(name + " (reserved word) cannot be used as model name. Renamed to " + camelize("model_" + name));
            name = "model_" + name; // e.g. return => ModelReturn (after camelize)
        }

        // model name starts with number
        if (name.matches("^\\d.*")) {
            LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + camelize("model_" + name));
            name = "model_" + name; // e.g. 200Response => Model200Response (after camelize)
        }

        // add prefix and/or suffic only if name does not start wth \ (e.g. \DateTime)
        if (!name.matches("^\\\\.*")) {
            name = modelNamePrefix + name + modelNameSuffix;
        }

        // camelize the model name
        // phone_number => PhoneNumber
        return camelize(name);
    }

    @Override
    public String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name);
    }

    @Override
    public String toModelTestFilename(String name) {
        // should be the same as the model name
        return toModelName(name) + "Test";
    }

    @Override
    public String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed");
        }

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + camelize(sanitizeName("call_" + operationId), true));
            operationId = "call_" + operationId;
        }

        return camelize(sanitizeName(operationId), true);
    }

    /**
     * Return the default value of the property
     *
     * @param p Swagger property object
     * @return string presentation of the default value of the property
     */
    @Override
    public String toDefaultValue(Schema schema) {
        if (schema instanceof StringSchema) {
            StringSchema stringSchema = (StringSchema) schema;
            if (stringSchema.getDefault() != null) {
                return "'" + stringSchema.getDefault() + "'";
            }
        } else if (schema instanceof BooleanSchema) {
            BooleanSchema booleanSchema = (BooleanSchema) schema;
            if (booleanSchema.getDefault() != null) {
                return booleanSchema.getDefault().toString();
            }
        } else if (schema instanceof DateSchema) {
            // TODO
        } else if (schema instanceof DateTimeSchema) {
            // TODO
        } else if (schema instanceof NumberSchema) {
            NumberSchema numberSchema = (NumberSchema) schema;
            if (
                    numberSchema.getDefault() != null
                            && (SchemaTypeUtil.FLOAT_FORMAT.equals(schema.getFormat()) || SchemaTypeUtil.DOUBLE_FORMAT.equals(schema.getFormat()))
                    ) {
                return numberSchema.getDefault().toString();
            }
        } else if (schema instanceof IntegerSchema) {
            IntegerSchema integerSchema = (IntegerSchema) schema;
            if (integerSchema.getDefault() != null) {
                return integerSchema.getDefault().toString();
            }
        }

        return null;
    }

    @Override
    public void setParameterExampleValue(CodegenParameter p) {
        String example;

        if (p.defaultValue == null) {
            example = p.example;
        } else {
            example = p.defaultValue;
        }

        String type = p.baseType;
        if (type == null) {
            type = p.dataType;
        }

        if ("String".equalsIgnoreCase(type)) {
            if (example == null) {
                example = p.paramName + "_example";
            }
            example = "\"" + escapeText(example) + "\"";
        } else if ("Integer".equals(type) || "int".equals(type)) {
            if (example == null) {
                example = "56";
            }
        } else if ("Float".equalsIgnoreCase(type) || "Double".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "3.4";
            }
        } else if ("BOOLEAN".equalsIgnoreCase(type) || "bool".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "True";
            }
        } else if ("\\SplFileObject".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "/path/to/file";
            }
            example = "\"" + escapeText(example) + "\"";
        } else if ("Date".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "2013-10-20";
            }
            example = "new \\DateTime(\"" + escapeText(example) + "\")";
        } else if ("DateTime".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "2013-10-20T19:20:30+01:00";
            }
            example = "new \\DateTime(\"" + escapeText(example) + "\")";
        } else if (!languageSpecificPrimitives.contains(type)) {
            // type is a model class, e.g. User
            example = "new " + getTypeDeclaration(type) + "()";
        } else {
            LOGGER.warn("Type " + type + " not handled properly in setParameterExampleValue");
        }

        if (example == null) {
            example = "NULL";
        } else if (Boolean.TRUE.equals(p.isListContainer)) {
            example = "array(" + example + ")";
        } else if (Boolean.TRUE.equals(p.isMapContainer)) {
            example = "array('key' => " + example + ")";
        }

        p.example = example;
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if ("int".equals(datatype) || "double".equals(datatype) || "float".equals(datatype)) {
            return value;
        } else {
            return "\'" + escapeText(value) + "\'";
        }
    }

    @Override
    public String toEnumDefaultValue(String value, String datatype) {
        return datatype + "_" + value;
    }

    @Override
    public String toEnumVarName(String name, String datatype) {
        if (name.length() == 0) {
            return "EMPTY";
        }

        // for symbol, e.g. $, #
        if (getSymbolName(name) != null) {
            return (getSymbolName(name)).toUpperCase();
        }

        // number
        if ("int".equals(datatype) || "double".equals(datatype) || "float".equals(datatype)) {
            String varName = name;
            varName = varName.replaceAll("-", "MINUS_");
            varName = varName.replaceAll("\\+", "PLUS_");
            varName = varName.replaceAll("\\.", "_DOT_");
            return varName;
        }

        // string
        String enumName = sanitizeName(underscore(name).toUpperCase());
        enumName = enumName.replaceFirst("^_", "");
        enumName = enumName.replaceFirst("_$", "");

        if (enumName.matches("\\d.*")) { // starts with number
            return "_" + enumName;
        } else {
            return enumName;
        }
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        String enumName = underscore(toModelName(property.name)).toUpperCase();

        if (enumName.matches("\\d.*")) { // starts with number
            return "_" + enumName;
        } else {
            return enumName;
        }
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        // process enum in models
        return postProcessModelsEnum(objs);
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        List<CodegenOperation> operationList = (List<CodegenOperation>) operations.get("operation");
        for (CodegenOperation op : operationList) {
            op.vendorExtensions.put("x-testOperationId", camelize(op.operationId));
        }
        return objs;
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove ' to avoid code injection
        return input.replace("'", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "");
    }

    protected String extractSimpleName(String phpClassName) {
        if (phpClassName == null) {
            return null;
        }

        final int lastBackslashIndex = phpClassName.lastIndexOf('\\');
        return phpClassName.substring(lastBackslashIndex + 1);
    }
}