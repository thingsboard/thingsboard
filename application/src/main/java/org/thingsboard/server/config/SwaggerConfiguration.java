/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.RouterOperationCustomizer;
import org.springdoc.core.discoverer.SpringDocParameterNameDiscoverer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.exception.ThingsboardCredentialsExpiredResponse;
import org.thingsboard.server.exception.ThingsboardErrorResponse;
import org.thingsboard.server.service.security.auth.rest.LoginRequest;
import org.thingsboard.server.service.security.auth.rest.LoginResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Configuration
@ConditionalOnExpression("('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core') && '${springdoc.api-docs.enabled:true}'=='true'")
@Profile("!test")
public class SwaggerConfiguration {

    public static final String LOGIN_ENDPOINT = "/api/auth/login";
    public static final String REFRESH_TOKEN_ENDPOINT = "/api/auth/token";

    private static final String LOGIN_PASSWORD_SCHEME = "HTTP login form";
    private static final String API_KEY_SCHEME = "API key form";

    private static final ApiResponses loginResponses = loginResponses();
    private static final ApiResponses defaultErrorResponses = defaultErrorResponses(false);
    private static final ApiResponses defaultPostErrorResponses = defaultErrorResponses(true);

    @Value("${swagger.api_path:/api/**}")
    private String apiPath;
    @Value("${swagger.security_path_regex}")
    private String securityPathRegex;
    @Value("${swagger.non_security_path_regex}")
    private String nonSecurityPathRegex;
    @Value("${swagger.title}")
    private String title;
    @Value("${swagger.description}")
    private String description;
    @Value("${swagger.contact.name}")
    private String contactName;
    @Value("${swagger.contact.url}")
    private String contactUrl;
    @Value("${swagger.contact.email}")
    private String contactEmail;
    @Value("${swagger.license.title}")
    private String licenseTitle;
    @Value("${swagger.license.url}")
    private String licenseUrl;
    @Value("${swagger.version}")
    private String version;
    @Value("${app.version:unknown}")
    private String appVersion;
    @Value("${swagger.group_name:thingsboard}")
    private String groupName;
    @Value("${swagger.doc_expansion:list}")
    private String docExpansion;

    @Bean
    public OpenAPI thingsboardApi() {
        Contact contact = new Contact()
                .name(contactName)
                .url(contactUrl)
                .email(contactEmail);

        License license = new License()
                .name(licenseTitle)
                .url(licenseUrl);

        String apiVersion = version;
        if (StringUtils.isEmpty(apiVersion)) {
            apiVersion = appVersion;
        }

        Info info = new Info()
                .title(title)
                .description(description)
                .contact(contact)
                .license(license)
                .version(apiVersion);

        SecurityScheme loginPasswordScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .description("Enter Username / Password")
                .scheme("loginPassword")
                .bearerFormat("/api/auth/login|X-Authorization");

        SecurityScheme apiKeyScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .name("X-Authorization")
                .in(SecurityScheme.In.HEADER)
                .description("""
                        Enter the API key value with 'ApiKey' prefix in format: **ApiKey <your_api_key_value>**
                        
                        Example: **ApiKey tb_5te51SkLRYpjGrujUGwqkjFvooWBlQpVe2An2Dr3w13wjfxDW**
                        
                        <br>**NOTE**: Use only ONE authentication method at a time. If both are authorized, JWT auth takes the priority.<br>
                        """);

        var openApi = new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(LOGIN_PASSWORD_SCHEME, loginPasswordScheme)
                        .addSecuritySchemes(API_KEY_SCHEME, apiKeyScheme))
                .info(info);
        addDefaultSchemas(openApi);
        addLoginOperation(openApi);
        addRefreshTokenOperation(openApi);
        return openApi;
    }

    @Bean
    @Primary
    public SpringDocConfigProperties springDocConfig(SpringDocConfigProperties springDocProperties) {
        springDocProperties.getApiDocs().setVersion(SpringDocConfigProperties.ApiDocs.OpenApiVersion.OPENAPI_3_1);
        springDocProperties.setRemoveBrokenReferenceDefinitions(false);
        return springDocProperties;
    }

    @Bean
    @Primary
    public SwaggerUiConfigProperties swaggerUiConfig(SwaggerUiConfigProperties uiProperties) {
        uiProperties.setDeepLinking(true);
        uiProperties.setDisplayOperationId(false);
        uiProperties.setDefaultModelsExpandDepth(1);
        uiProperties.setDefaultModelExpandDepth(1);
        uiProperties.setDefaultModelRendering("example");
        uiProperties.setDisplayRequestDuration(false);
        uiProperties.setDocExpansion(docExpansion);
        uiProperties.setFilter("false");
        uiProperties.setMaxDisplayedTags(null);
        uiProperties.setOperationsSorter("alpha");
        uiProperties.setTagsSorter("alpha");
        uiProperties.setShowExtensions(false);
        uiProperties.setShowCommonExtensions(false);
        uiProperties.setSupportedSubmitMethods(List.of("get", "put", "post", "delete", "options", "head", "patch", "trace"));
        uiProperties.setValidatorUrl(null);
        uiProperties.setPersistAuthorization(true);

        var syntaxHighLight = new SwaggerUiConfigProperties.SyntaxHighlight();
        syntaxHighLight.setActivated(true);
        syntaxHighLight.setTheme("agate");

        uiProperties.setSyntaxHighlight(syntaxHighLight);
        return uiProperties;
    }

    private void addLoginOperation(OpenAPI openAPI) {
        var operation = new Operation();
        operation.summary("Login method to get user JWT token data");
        operation.description("""
                Login method used to authenticate user and get JWT token data.
                
                Value of the response **token** field can be used as **X-Authorization** header value:
                
                `X-Authorization: Bearer $JWT_TOKEN_VALUE`.""");

        var requestBody = new RequestBody().description("Login request")
                .content(new Content().addMediaType(APPLICATION_JSON_VALUE,
                        new MediaType().schema(new Schema<LoginRequest>().$ref("#/components/schemas/LoginRequest"))));
        operation.requestBody(requestBody);

        operation.responses(loginResponses);

        operation.addTagsItem("login-endpoint");
        var pathItem = new PathItem().post(operation);
        openAPI.path(LOGIN_ENDPOINT, pathItem);
    }

    private void addRefreshTokenOperation(OpenAPI openAPI) {
        var operation = new Operation();
        operation.summary("Refresh user JWT token data");
        operation.description("""
                Method to refresh JWT token. Provide a valid refresh token to get a new JWT token.
                
                The response contains a new token that can be used for authorization.
                
                `X-Authorization: Bearer $JWT_TOKEN_VALUE`""");

        var requestBody = new RequestBody().description("Refresh token request")
                .content(new Content().addMediaType(APPLICATION_JSON_VALUE,
                        new MediaType().schema(new Schema<JsonNode>().addProperty("refreshToken", new Schema<>().type("string")))));

        operation.requestBody(requestBody);

        operation.responses(loginResponses);

        operation.addTagsItem("login-endpoint");
        var pathItem = new PathItem().post(operation);
        openAPI.path(REFRESH_TOKEN_ENDPOINT, pathItem);
    }

    @Bean
    public GroupedOpenApi groupedApi(SpringDocParameterNameDiscoverer localSpringDocParameterNameDiscoverer) {
        return GroupedOpenApi.builder()
                .group(groupName)
                .pathsToMatch(apiPath)
                .addRouterOperationCustomizer(routerOperationCustomizer(localSpringDocParameterNameDiscoverer))
                .addOperationCustomizer(operationCustomizer())
                .addOpenApiCustomizer(customOpenApiCustomizer())
                .build();
    }

    @Bean
    @Lazy(false)
    ModelConverter mapAwareConverter() {
        return (type, context, chain) -> {
            if (chain.hasNext()) {
                Schema schema = chain.next().resolve(type, context, chain);
                JavaType javaType = Json.mapper().constructType(type.getType());
                if (javaType != null) {
                    Class<?> cls = javaType.getRawClass();
                    if (Map.class.isAssignableFrom(cls)) {
                        if (schema != null && schema.getProperties() != null) {
                            schema.getProperties().remove("empty");
                            if (schema.getProperties().isEmpty()) {
                                schema.setProperties(null);
                            }
                        }
                    }
                }
                return schema;
            } else {
                return null;
            }
        };
    }

    private void addDefaultSchemas(OpenAPI openAPI) {
        var jsonNodeSchema = ModelConverters.getInstance().readAllAsResolvedSchema(new AnnotatedType().type(JsonNode.class)).schema;
        jsonNodeSchema.setType("any");
        //noinspection unchecked
        jsonNodeSchema.setExamples(List.of(JacksonUtil.newObjectNode()));
        jsonNodeSchema.setDescription("A value representing the any type (object or primitive)");
        openAPI.getComponents()
                .addSchemas("JsonNode", jsonNodeSchema)
                .addSchemas("LoginRequest", ModelConverters.getInstance().readAllAsResolvedSchema(new AnnotatedType().type(LoginRequest.class)).schema)
                .addSchemas("LoginResponse", ModelConverters.getInstance().readAllAsResolvedSchema(new AnnotatedType().type(LoginResponse.class)).schema)
                .addSchemas("ThingsboardErrorResponse", ModelConverters.getInstance().readAllAsResolvedSchema(new AnnotatedType().type(ThingsboardErrorResponse.class)).schema)
                .addSchemas("ThingsboardCredentialsExpiredResponse", ModelConverters.getInstance().readAllAsResolvedSchema(new AnnotatedType().type(ThingsboardCredentialsExpiredResponse.class)).schema);
    }

    private RouterOperationCustomizer routerOperationCustomizer(SpringDocParameterNameDiscoverer localSpringDocParameterNameDiscoverer) {
        return (routerOperation, handlerMethod) -> {
            String[] pNames = localSpringDocParameterNameDiscoverer.getParameterNames(handlerMethod.getMethod());
            String[] reflectionParametersNames = Arrays.stream(handlerMethod.getMethod().getParameters()).map(java.lang.reflect.Parameter::getName).toArray(String[]::new);
            if (pNames == null || Arrays.stream(pNames).anyMatch(Objects::isNull)) {
                pNames = reflectionParametersNames;
            }
            MethodParameter[] parameters = handlerMethod.getMethodParameters();
            List<String> requestParams = new ArrayList<>();
            for (var i = 0; i < parameters.length; i++) {
                var methodParameter = parameters[i];
                RequestParam requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
                if (requestParam != null) {
                    String pName = StringUtils.isNotBlank(requestParam.value()) ? requestParam.value() :
                            pNames[i];
                    if (StringUtils.isNotBlank(pName)) {
                        requestParams.add(pName);
                    }
                }
            }
            if (!requestParams.isEmpty()) {
                var path = routerOperation.getPath() + "{?" + String.join(",", requestParams) + "}";
                routerOperation.setPath(path);
            }
            return routerOperation;
        };
    }

    private OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {
            if (StringUtils.isBlank(operation.getSummary())) {
                operation.setSummary(operation.getOperationId());
            }
            return operation;
        };
    }

    private OpenApiCustomizer customOpenApiCustomizer() {
        var loginRequirement = createSecurityRequirement(LOGIN_PASSWORD_SCHEME);
        var apiKeyRequirement = createSecurityRequirement(API_KEY_SCHEME);

        return openAPI -> {
            var paths = openAPI.getPaths();
            paths.entrySet().stream()
                    .peek(entry -> {
                        securityCustomization(entry, loginRequirement, apiKeyRequirement);
                        if (!entry.getKey().equals(LOGIN_ENDPOINT)) {
                            defaultErrorResponsesCustomization(entry.getValue());
                        }
                    })
                    .map(this::extractTagFromPath).filter(Objects::nonNull).distinct().sorted(Comparator.comparing(Tag::getName)).forEach(openAPI::addTagsItem);

            var pathItemsByTags = new TreeMap<String, Map<String, PathItem>>();
            paths.forEach((k, v) -> {
                var tagItem = tagItemFromPathItem(v);
                if (tagItem != null) {
                    pathItemsByTags.computeIfAbsent(tagItem, k1 -> new TreeMap<>()).put(k, v);
                }
            });
            var sortedPaths = new Paths();
            pathItemsByTags.forEach((tagItem, pathItemMap) -> {
                pathItemMap.forEach(sortedPaths::addPathItem);
            });
            sortedPaths.setExtensions(paths.getExtensions());
            openAPI.setPaths(sortedPaths);
            var sortedSchemas = new TreeMap<>(openAPI.getComponents().getSchemas());
            openAPI.getComponents().setSchemas(new LinkedHashMap<>(sortedSchemas));
        };
    }

    private SecurityRequirement createSecurityRequirement(String schemeName) {
        return new SecurityRequirement().addList(schemeName, Arrays.asList(
                Authority.SYS_ADMIN.name(),
                Authority.TENANT_ADMIN.name(),
                Authority.CUSTOMER_USER.name()
        ));
    }

    private Tag extractTagFromPath(Map.Entry<String, PathItem> entry) {
        var tagName = tagItemFromPathItem(entry.getValue());
        return tagName != null ? tagFromTagItem(tagName) : null;
    }

    private String tagItemFromPathItem(PathItem item) {
        var operations = item.readOperationsMap().values();
        var operation = operations.stream().findAny();
        if (operation.isPresent()) {
            var tags = operation.get().getTags();
            if (tags != null && !tags.isEmpty()) {
                return tags.get(0);
            }
        }
        return null;
    }

    private Tag tagFromTagItem(String tagItem) {
        String[] words = tagItem.split("-");
        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(word.substring(0, 1).toUpperCase());
                sb.append(word.substring(1).toLowerCase());
                sb.append(" ");
            }
        }

        return new Tag().name(tagItem).description(sb.toString().trim());
    }

    private void defaultErrorResponsesCustomization(PathItem pathItem) {
        pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
            var errorResponses = httpMethod.equals(PathItem.HttpMethod.POST) ? defaultPostErrorResponses : defaultErrorResponses;

            var responses = operation.getResponses();
            if (responses == null) {
                responses = errorResponses;
            } else {
                ApiResponses updated = responses;
                errorResponses.forEach((key, apiResponse) -> {
                    if (!updated.containsKey(key)) {
                        updated.put(key, apiResponse);
                    }
                });
            }
            operation.setResponses(responses);
        });
    }

    private void securityCustomization(Map.Entry<String, PathItem> entry, SecurityRequirement jwtBearerRequirement, SecurityRequirement apiKeyRequirement) {
        var path = entry.getKey();
        if (path.matches(securityPathRegex) && !path.matches(nonSecurityPathRegex) && !path.equals(LOGIN_ENDPOINT) && !path.equals(REFRESH_TOKEN_ENDPOINT)) {
            entry.getValue()
                    .readOperationsMap()
                    .values()
                    .forEach(operation -> {
                        operation.addSecurityItem(jwtBearerRequirement);
                        operation.addSecurityItem(apiKeyRequirement);
                    });
        }
    }

    private static ApiResponses loginResponses() {
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", new ApiResponse().description("OK")
                .content(new Content().addMediaType(APPLICATION_JSON_VALUE,
                        new MediaType().schema(new Schema<LoginResponse>().$ref("#/components/schemas/LoginResponse")))));
        apiResponses.putAll(loginErrorResponses());
        return apiResponses;
    }

    private static ApiResponses defaultErrorResponses(boolean isPost) {
        ApiResponses apiResponses = new ApiResponses();

        apiResponses.addApiResponse("400", errorResponse("400", "Bad Request",
                ThingsboardErrorResponse.of(isPost ? "Invalid request body" : "Invalid UUID string: 123", ThingsboardErrorCode.BAD_REQUEST_PARAMS, HttpStatus.BAD_REQUEST)));

        apiResponses.addApiResponse("401", errorResponse("401", "Unauthorized",
                ThingsboardErrorResponse.of("Authentication failed", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED)));

        apiResponses.addApiResponse("403", errorResponse("403", "Forbidden",
                ThingsboardErrorResponse.of("You don't have permission to perform this operation!", ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN)));

        apiResponses.addApiResponse("404", errorResponse("404", "Not Found",
                ThingsboardErrorResponse.of("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND, HttpStatus.NOT_FOUND)));

        apiResponses.addApiResponse("429", errorResponse("429", "Too Many Requests",
                ThingsboardErrorResponse.of("Too many requests for current tenant!", ThingsboardErrorCode.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS)));

        return apiResponses;
    }

    private static ApiResponses loginErrorResponses() {
        ApiResponses apiResponses = new ApiResponses();

        apiResponses.addApiResponse("401", errorResponse("Unauthorized",
                Map.of(
                        "bad-credentials", errorExample("Bad credentials",
                                ThingsboardErrorResponse.of("Invalid username or password", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED)),
                        "token-expired", errorExample("JWT token expired",
                                ThingsboardErrorResponse.of("Token has expired", ThingsboardErrorCode.JWT_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED)),
                        "account-disabled", errorExample("Disabled account",
                                ThingsboardErrorResponse.of("User account is not active", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED)),
                        "account-locked", errorExample("Locked account",
                                ThingsboardErrorResponse.of("User account is locked due to security policy", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED)),
                        "authentication-failed", errorExample("General authentication error",
                                ThingsboardErrorResponse.of("Authentication failed", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED))
                )
        ));
        var credentialsExpiredSchema = new Schema<ThingsboardCredentialsExpiredResponse>().$ref("#/components/schemas/ThingsboardCredentialsExpiredResponse");
        apiResponses.addApiResponse("401 ", errorResponse("Unauthorized (**Expired credentials**)",
                Map.of(
                        "credentials-expired", errorExample("Expired credentials",
                                ThingsboardCredentialsExpiredResponse.of("User password expired!", StringUtils.randomAlphanumeric(30)))
                ),
                credentialsExpiredSchema
        ));
        return apiResponses;
    }

    private static ApiResponse errorResponse(String code, String description, ThingsboardErrorResponse example) {
        return errorResponse(description, Map.of("error-code-" + code, errorExample(description, example)));
    }

    private static ApiResponse errorResponse(String description, Map<String, Example> examples) {
        var schema = new Schema<ThingsboardErrorResponse>().$ref("#/components/schemas/ThingsboardErrorResponse");
        return errorResponse(description, examples, schema);
    }

    private static ApiResponse errorResponse(String description, Map<String, Example> examples, Schema<? extends ThingsboardErrorResponse> errorResponseSchema) {
        MediaType mediaType = new MediaType().schema(errorResponseSchema).examples(examples);
        Content content = new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, mediaType);
        return new ApiResponse().description(description).content(content);
    }

    private static Example errorExample(String summary, ThingsboardErrorResponse example) {
        return new Example()
                .summary(summary)
                .value(example);
    }

}
