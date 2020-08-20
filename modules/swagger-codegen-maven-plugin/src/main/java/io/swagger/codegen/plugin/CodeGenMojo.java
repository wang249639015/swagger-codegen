package io.swagger.codegen.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import static io.swagger.codegen.config.CodegenConfiguratorUtils.applyAdditionalPropertiesKvp;
import static io.swagger.codegen.config.CodegenConfiguratorUtils.applyImportMappingsKvp;
import static io.swagger.codegen.config.CodegenConfiguratorUtils.applyInstantiationTypesKvp;
import static io.swagger.codegen.config.CodegenConfiguratorUtils.applyLanguageSpecificPrimitivesCsv;
import static io.swagger.codegen.config.CodegenConfiguratorUtils.applyTypeMappingsKvp;
import static io.swagger.codegen.config.CodegenConfiguratorUtils.applyReservedWordsMappingsKvp;
import static io.swagger.codegen.config.CodegenConfiguratorUtils.applyAdditionalPropertiesKvpList;
import static io.swagger.codegen.config.CodegenConfiguratorUtils.applyImportMappingsKvpList;
import static io.swagger.codegen.config.CodegenConfiguratorUtils.applyInstantiationTypesKvpList;
import static io.swagger.codegen.config.CodegenConfiguratorUtils.applyLanguageSpecificPrimitivesCsvList;
import static io.swagger.codegen.config.CodegenConfiguratorUtils.applyTypeMappingsKvpList;
import static io.swagger.codegen.config.CodegenConfiguratorUtils.applyReservedWordsMappingsKvpList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.ClientOptInput;
import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.DefaultGenerator;
import io.swagger.codegen.config.CodegenConfigurator;

/**
 * Goal which generates client/server code from a swagger json/yaml definition.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class CodeGenMojo extends AbstractMojo {

    @Parameter(name = "verbose", required = false, defaultValue = "false")
    private boolean verbose;

    /**
     * Client language to generate.
     */
    @Parameter(name = "language", required = true)
    private String language;

    /**
     * Location of the output directory.
     */
    @Parameter(name = "output", property = "swagger.codegen.maven.plugin.output",
            defaultValue = "${project.build.directory}/generated-sources/swagger")
    private File output;

    /**
     * Location of the swagger spec, as URL or file.
     */
    @Parameter(name = "inputSpec", required = true)
    private String inputSpec;

    /**
     * Git user ID, e.g. swagger-api.
     */
    @Parameter(name = "gitUserId", required = false)
    private String gitUserId;

    /**
     * Git repo ID, e.g. swagger-codegen.
     */
    @Parameter(name = "gitRepoId", required = false)
    private String gitRepoId;

    /**
     * Folder containing the template files.
     */
    @Parameter(name = "templateDirectory")
    private File templateDirectory;

    /**
     * Adds authorization headers when fetching the swagger definitions remotely. " Pass in a
     * URL-encoded string of name:header with a comma separating multiple values
     */
    @Parameter(name = "auth")
    private String auth;

    /**
     * Path to separate json configuration file.
     */
    @Parameter(name = "configurationFile", required = false)
    private String configurationFile;

    /**
     * Specifies if the existing files should be overwritten during the generation.
     */
    @Parameter(name = "skipOverwrite", required = false)
    private Boolean skipOverwrite;

    /**
     * Specifies if the existing files should be overwritten during the generation.
     */
    @Parameter(name = "removeOperationIdPrefix", required = false)
    private Boolean removeOperationIdPrefix;

    /**
     * The package to use for generated api objects/classes
     */
    @Parameter(name = "apiPackage")
    private String apiPackage;

    /**
     * The package to use for generated model objects/classes
     */
    @Parameter(name = "modelPackage")
    private String modelPackage;

    /**
     * The package to use for the generated invoker objects
     */
    @Parameter(name = "invokerPackage")
    private String invokerPackage;

    /**
     * groupId in generated pom.xml
     */
    @Parameter(name = "groupId")
    private String groupId;

    /**
     * artifactId in generated pom.xml
     */
    @Parameter(name = "artifactId")
    private String artifactId;

    /**
     * artifact version in generated pom.xml
     */
    @Parameter(name = "artifactVersion")
    private String artifactVersion;

    /**
     * Sets the library
     */
    @Parameter(name = "library", required = false)
    private String library;

    /**
     * Sets the prefix for model enums and classes
     */
    @Parameter(name = "modelNamePrefix", required = false)
    private String modelNamePrefix;

    /**
     * Sets the suffix for model enums and classes
     */
    @Parameter(name = "modelNameSuffix", required = false)
    private String modelNameSuffix;

    /**
     * Sets an optional ignoreFileOverride path
     */
    @Parameter(name = "ignoreFileOverride", required = false)
    private String ignoreFileOverride;

    /**
     * A map of language-specific parameters as passed with the -c option to the command line
     */
    @Parameter(name = "configOptions")
    private Map<?, ?> configOptions;

    /**
     * A map of types and the types they should be instantiated as
     */
    @Parameter(name = "instantiationTypes")
    private List<String> instantiationTypes;

    /**
     * A map of classes and the import that should be used for that class
     */
    @Parameter(name = "importMappings")
    private List<String> importMappings;

    /**
     * A map of swagger spec types and the generated code types to use for them
     */
    @Parameter(name = "typeMappings")
    private List<String> typeMappings;

    /**
     * A map of additional language specific primitive types
     */
    @Parameter(name = "languageSpecificPrimitives")
    private List<String> languageSpecificPrimitives;

    /**
     * A map of additional properties that can be referenced by the mustache templates
     * <additionalProperties>
     *     <additionalProperty>key=value</additionalProperty>
     * </additionalProperties>
     */
    @Parameter(name = "additionalProperties")
    private List<String> additionalProperties;

    /**
     * A map of reserved names and how they should be escaped
     */
    @Parameter(name = "reservedWordsMappings")
    private List<String> reservedWordsMappings;

    /**
     * Generate the apis
     */
    @Parameter(name = "generateApis", required = false)
    private Boolean generateApis = true;

    /**
     * Generate the models
     */
    @Parameter(name = "generateModels", required = false)
    private Boolean generateModels = true;

    /**
     * A comma separated list of models to generate. All models is the default.
     */
    @Parameter(name = "modelsToGenerate", required = false)
    private String modelsToGenerate = "";

    /**
     * Generate the supporting files
     */
    @Parameter(name = "generateSupportingFiles", required = false)
    private Boolean generateSupportingFiles = true;

    /**
     * A comma separated list of models to generate. All models is the default.
     */
    @Parameter(name = "supportingFilesToGenerate", required = false)
    private String supportingFilesToGenerate = "";

    /**
     * Generate the model tests
     */
    @Parameter(name = "generateModelTests", required = false)
    private Boolean generateModelTests = true;

    /**
     * Generate the model documentation
     */
    @Parameter(name = "generateModelDocumentation", required = false)
    private Boolean generateModelDocumentation = true;

    /**
     * Generate the api tests
     */
    @Parameter(name = "generateApiTests", required = false)
    private Boolean generateApiTests = true;

    /**
     * Generate the api documentation
     */
    @Parameter(name = "generateApiDocumentation", required = false)
    private Boolean generateApiDocumentation = true;

    /**
     * Generate the api documentation
     */
    @Parameter(name = "withXml", required = false)
    private Boolean withXml = false;

    /**
     * Skip the execution.
     */
    @Parameter(name = "skip", property = "codegen.skip", required = false, defaultValue = "false")
    private Boolean skip;

    /**
     * Add the output directory to the project as a source root, so that the generated java types
     * are compiled and included in the project artifact.
     */
    @Parameter(defaultValue = "true")
    private boolean addCompileSourceRoot = true;

    @Parameter
    protected Map<String, String> environmentVariables = new HashMap<String, String>();

    @Parameter
    protected Map<String, String> originalEnvironmentVariables = new HashMap<String, String>();

    @Parameter
    private boolean configHelp = false;

    /**
     * The project being built.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    private MavenProject project;



    @Override
    public void execute() throws MojoExecutionException {
        // Using the naive approach for achieving thread safety
        synchronized (CodeGenMojo.class) {
            execute_();
        }
    }

    protected void execute_() throws MojoExecutionException {

       if (!StringUtils.isNotEmpty(this.inputSpec)) {
            System.out.println("inspect can't be null");
        } else {
            String[] split = this.inputSpec.split("##");
            String path = split[0];
            System.out.println(path);
            String files = split[1];
            String[] filesArray = files.split(",");
            String[] var5 = filesArray;
            int var6 = filesArray.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                String file1 = var5[var7].trim();
                if (this.skip) {
                    this.getLog().info("Code generation is skipped.");
                    return;
                }

                CodegenConfigurator configurator = CodegenConfigurator.fromFile(this.configurationFile);
                if (configurator == null) {
                    configurator = new CodegenConfigurator();
                }

                configurator.setVerbose(this.verbose);
                if (this.skipOverwrite != null) {
                    configurator.setSkipOverwrite(this.skipOverwrite);
                }

                if (this.removeOperationIdPrefix != null) {
                    configurator.setRemoveOperationIdPrefix(this.removeOperationIdPrefix);
                }

                if (StringUtils.isNotEmpty(this.inputSpec)) {
                    configurator.setInputSpec(this.inputSpec);
                }

                if (StringUtils.isNotEmpty(this.gitUserId)) {
                    configurator.setGitUserId(this.gitUserId);
                }

                if (StringUtils.isNotEmpty(this.gitRepoId)) {
                    configurator.setGitRepoId(this.gitRepoId);
                }

                if (StringUtils.isNotEmpty(this.ignoreFileOverride)) {
                    configurator.setIgnoreFileOverride(this.ignoreFileOverride);
                }

                configurator.setLang(this.language);
                configurator.setOutputDir(this.output.getAbsolutePath());
                if (StringUtils.isNotEmpty(this.auth)) {
                    configurator.setAuth(this.auth);
                }

                if (StringUtils.isNotEmpty(this.apiPackage)) {
                    configurator.setApiPackage(this.apiPackage);
                }

                if (StringUtils.isNotEmpty(this.modelPackage)) {
                    configurator.setModelPackage(this.modelPackage);
                }

                if (StringUtils.isNotEmpty(this.invokerPackage)) {
                    configurator.setInvokerPackage(this.invokerPackage);
                }

                if (StringUtils.isNotEmpty(this.groupId)) {
                    configurator.setGroupId(this.groupId);
                }

                if (StringUtils.isNotEmpty(this.artifactId)) {
                    configurator.setArtifactId(this.artifactId);
                }

                if (StringUtils.isNotEmpty(this.artifactVersion)) {
                    configurator.setArtifactVersion(this.artifactVersion);
                }

                if (StringUtils.isNotEmpty(this.library)) {
                    configurator.setLibrary(this.library);
                }

                if (StringUtils.isNotEmpty(this.modelNamePrefix)) {
                    configurator.setModelNamePrefix(this.modelNamePrefix);
                }

                if (StringUtils.isNotEmpty(this.modelNameSuffix)) {
                    configurator.setModelNameSuffix(this.modelNameSuffix);
                }

                if (null != this.templateDirectory) {
                    configurator.setTemplateDir(this.templateDirectory.getAbsolutePath());
                }

                configurator.setInputSpec(path + file1);
                if (null != this.generateApis && this.generateApis) {
                    System.setProperty("apis", "");
                } else {
                    System.clearProperty("apis");
                }

                if (null != this.generateModels && this.generateModels) {
                    System.setProperty("models", this.modelsToGenerate);
                } else {
                    System.clearProperty("models");
                }

                if (null != this.generateSupportingFiles && this.generateSupportingFiles) {
                    System.setProperty("supportingFiles", this.supportingFilesToGenerate);
                } else {
                    System.clearProperty("supportingFiles");
                }

                System.setProperty("modelTests", this.generateModelTests.toString());
                System.setProperty("modelDocs", this.generateModelDocumentation.toString());
                System.setProperty("apiTests", this.generateApiTests.toString());
                System.setProperty("apiDocs", this.generateApiDocumentation.toString());
                System.setProperty("withXml", this.withXml.toString());
                if (this.configOptions != null) {
                    if (this.instantiationTypes == null && this.configOptions.containsKey("instantiation-types")) {
                        CodegenConfiguratorUtils.applyInstantiationTypesKvp(this.configOptions.get("instantiation-types").toString(), configurator);
                    }

                    if (this.importMappings == null && this.configOptions.containsKey("import-mappings")) {
                        CodegenConfiguratorUtils.applyImportMappingsKvp(this.configOptions.get("import-mappings").toString(), configurator);
                    }

                    if (this.typeMappings == null && this.configOptions.containsKey("type-mappings")) {
                        CodegenConfiguratorUtils.applyTypeMappingsKvp(this.configOptions.get("type-mappings").toString(), configurator);
                    }

                    if (this.languageSpecificPrimitives == null && this.configOptions.containsKey("language-specific-primitives")) {
                        CodegenConfiguratorUtils.applyLanguageSpecificPrimitivesCsv(this.configOptions.get("language-specific-primitives").toString(), configurator);
                    }

                    if (this.additionalProperties == null && this.configOptions.containsKey("additional-properties")) {
                        CodegenConfiguratorUtils.applyAdditionalPropertiesKvp(this.configOptions.get("additional-properties").toString(), configurator);
                    }

                    if (this.reservedWordsMappings == null && this.configOptions.containsKey("reserved-words-mappings")) {
                        CodegenConfiguratorUtils.applyReservedWordsMappingsKvp(this.configOptions.get("reserved-words-mappings").toString(), configurator);
                    }
                }

                if (this.instantiationTypes != null && (this.configOptions == null || !this.configOptions.containsKey("instantiation-types"))) {
                    CodegenConfiguratorUtils.applyInstantiationTypesKvpList(this.instantiationTypes, configurator);
                }

                if (this.importMappings != null && (this.configOptions == null || !this.configOptions.containsKey("import-mappings"))) {
                    CodegenConfiguratorUtils.applyImportMappingsKvpList(this.importMappings, configurator);
                }

                if (this.typeMappings != null && (this.configOptions == null || !this.configOptions.containsKey("type-mappings"))) {
                    CodegenConfiguratorUtils.applyTypeMappingsKvpList(this.typeMappings, configurator);
                }

                if (this.languageSpecificPrimitives != null && (this.configOptions == null || !this.configOptions.containsKey("language-specific-primitives"))) {
                    CodegenConfiguratorUtils.applyLanguageSpecificPrimitivesCsvList(this.languageSpecificPrimitives, configurator);
                }

                if (this.additionalProperties != null && (this.configOptions == null || !this.configOptions.containsKey("additional-properties"))) {
                    CodegenConfiguratorUtils.applyAdditionalPropertiesKvpList(this.additionalProperties, configurator);
                }

                if (this.reservedWordsMappings != null && (this.configOptions == null || !this.configOptions.containsKey("reserved-words-mappings"))) {
                    CodegenConfiguratorUtils.applyReservedWordsMappingsKvpList(this.reservedWordsMappings, configurator);
                }

                if (this.environmentVariables != null) {
                    Iterator var10 = this.environmentVariables.keySet().iterator();

                    while(var10.hasNext()) {
                        String key = (String)var10.next();
                        this.originalEnvironmentVariables.put(key, System.getProperty(key));
                        String value = (String)this.environmentVariables.get(key);
                        if (value == null) {
                            value = "";
                        }

                        System.setProperty(key, value);
                        configurator.addSystemProperty(key, value);
                    }
                }

                ClientOptInput input = configurator.toClientOptInput();
                CodegenConfig config = input.getConfig();
                CliOption langCliOption;
                Iterator var17;
                if (this.configOptions != null) {
                    var17 = config.cliOptions().iterator();

                    while(var17.hasNext()) {
                        langCliOption = (CliOption)var17.next();
                        if (this.configOptions.containsKey(langCliOption.getOpt())) {
                            input.getConfig().additionalProperties().put(langCliOption.getOpt(), this.configOptions.get(langCliOption.getOpt()));
                        }
                    }
                }

                if (this.configHelp) {
                    var17 = config.cliOptions().iterator();

                    while(var17.hasNext()) {
                        langCliOption = (CliOption)var17.next();
                        System.out.println("\t" + langCliOption.getOpt());
                        System.out.println("\t    " + langCliOption.getOptionHelp().replaceAll("\n", "\n\t    "));
                        System.out.println();
                    }

                    return;
                }

                try {
                    (new DefaultGenerator()).opts(input).generate();
                    System.out.println("generate success");
                } catch (Exception var14) {
                    this.getLog().error(var14);
                    throw new MojoExecutionException("Code generation failed. See above for the full exception.");
                }
            }

        }
    }

    private void addCompileSourceRootIfConfigured() {
        if (addCompileSourceRoot) {
            final Object sourceFolderObject =
                    configOptions == null ? null : configOptions
                            .get(CodegenConstants.SOURCE_FOLDER);
            final String sourceFolder =
                    sourceFolderObject == null ? "src/main/java" : sourceFolderObject.toString();

            String sourceJavaFolder = output.toString() + "/" + sourceFolder;
            project.addCompileSourceRoot(sourceJavaFolder);
        }

        // Reset all environment variables to their original value. This prevents unexpected
        // behaviour
        // when running the plugin multiple consecutive times with different configurations.
        for (Map.Entry<String, String> entry : originalEnvironmentVariables.entrySet()) {
            if (entry.getValue() == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }
}
