package org.thingsboard.server.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.extern.log4j.Log4j2;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.*;

@Log4j2
public class TbSwaggerModelConverter implements ModelConverter {
    private static final String refPrefix = "#/components/schemas/";

    @Override
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (!chain.hasNext()) {
            return null;
        }

        ModelConverter nextConverter = chain.next();
        Schema schema = nextConverter.resolve(type, context, chain);

        JavaType javaType = Json.mapper().constructType(type.getType());
        if (javaType != null) {
            Class<?> cls = javaType.getRawClass();
            // Remove "empty" property from Map types
            removeEmptyProperty(schema, cls);
            // Add discriminator mappings for polymorphic types
           fixPolymorphicSchemas(cls, schema, context, nextConverter);
        }

        return schema;
    }


    /// Remove empty props, by Igor Kulikov
    private void removeEmptyProperty(Schema schema, Class<?> cls) {
        if (!Map.class.isAssignableFrom(cls)) {
            return;
        }
        if (schema != null && schema.getProperties() != null) {
            schema.getProperties().remove("empty");
            if (schema.getProperties().isEmpty()) {
                schema.setProperties(null);
            }
        }
    }

    /// Helper to get a type of discriminator property from a class
    public static Class<?> getJsonTypeInfoPropertyType(Class<?> cls) throws Exception {
        // Find @JsonTypeInfo annotation (on the class or its members)
        JsonTypeInfo typeInfo = cls.getAnnotation(JsonTypeInfo.class);
        if (typeInfo == null) {
            // Maybe inside a nested interface
            for (Class<?> nested : cls.getDeclaredClasses()) {
                typeInfo = nested.getAnnotation(JsonTypeInfo.class);
                if (typeInfo != null) {
                    cls = nested; // analyze nested type instead
                    break;
                }
            }
        }

        if (typeInfo == null) {
            throw new IllegalStateException("@JsonTypeInfo not found");
        }

        String propName = typeInfo.property();

        // Use Java Beans introspection
        BeanInfo beanInfo = Introspector.getBeanInfo(cls);


        PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();

        PropertyDescriptor pd = Arrays.stream(props)
                .filter(p -> p.getName().equals(propName))
                .findFirst()
                .orElse(null);

        if (pd == null) {
            return null;
        }

        return pd.getPropertyType();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void fixPolymorphicSchemas(Class<?> cls, Schema<?> schema, ModelConverterContext context, ModelConverter mc) {
        if (cls == null || schema == null) {
            return;
        }
        if (!cls.isAnnotationPresent(JsonSubTypes.class) || !cls.isAnnotationPresent(JsonTypeInfo.class)) {
            return;
        }

/// Find a declaration of an interface in swagger schema
        final Schema<?> baseSchema = context.resolve(new AnnotatedType().type(cls));
        Schema baseType = context.getDefinedModels().get(baseSchema.getName());
        if (baseType == null) {
            return;
        }

        /// We don't want to work with declarations here
        if(schema.get$ref() ==null) {
            return;
        }
        Discriminator discriminator = baseType.getDiscriminator();
        if (discriminator == null) {
            return;
        }
        /// Add mapping object to a declaration since it's not generated from Jackson annotations
        fixMapping(cls, context, discriminator, baseType);
        /// Fix discriminator property, in some cases it gets declared without reference
        fixDiscriminatorProperty(cls, context, baseType);

    }

    /// Updates schema of a base type declaration, adds a refence to a declaration
    private static void fixDiscriminatorProperty(Class<?> cls, ModelConverterContext context, Schema baseType) {
        JsonTypeInfo annotation = cls.getAnnotation(JsonTypeInfo.class);
        final String propName = annotation.property();
        try {

            final Class<?> propertyType = getJsonTypeInfoPropertyType(cls);
            /// If we can't find a type of discriminator prop we generate a new type based on mapping
            Schema<String> propSchema = new Schema();
            propSchema.setType("string");
            propSchema.setName(baseType.getName() + "DiscriminatorEnum");
            propSchema.setEnum(new ArrayList<>(baseType.getDiscriminator().getMapping().keySet()));
            /// If type is found use it
            if (propertyType != null) {
                propSchema = context.resolve(new AnnotatedType().type(propertyType));
            }
            /// Something strange happens, but we should abort in this case
            if (propSchema.get$ref() == null && propSchema.getName() == null) {
                return;
            }

            String propertySchemaName = propSchema.getName();
            /// This means that we've got a reference to already declared schema, get a name from a ref
            if (propertySchemaName == null) {
                propertySchemaName = getSchemaNameFromRef(propSchema.get$ref());
            }

            Map<String, Schema> baseTypeProps = baseType.getProperties();
            if (baseTypeProps != null && !baseTypeProps.isEmpty()) {
                /// find this property in a declaration Schema of base type
                Schema discriminatorProp = baseTypeProps.get(propName);
                if (discriminatorProp == null) {
                    return;
                }
                /// If there is no reference, add it
                if (discriminatorProp.get$ref() == null) {
                    discriminatorProp.set$ref(refPrefix + propertySchemaName);
                }
/// If Schema of property not declared, (Probably because of missing @Schema on it, we declare it manually)
///but we should ensure that schema we have is a declaration, not a reference
                if (context.getDefinedModels().get(propertySchemaName) == null && propSchema.get$ref() == null) {
                    context.defineModel(propSchema.getName(), propSchema);
                }

            }
        } catch (Exception error) {
            log.error(error.getMessage(), error);
        }
    }

    /// Every polymorphic type should have a discriminator with mapping in a schema (Basically it's what we define with @JsonSubtypes)
    private static void fixMapping(Class<?> cls, ModelConverterContext context, Discriminator discriminator, Schema baseType) {
        JsonSubTypes annotation = cls.getAnnotation(JsonSubTypes.class);
        JsonSubTypes.Type[] types = annotation.value();
        HashMap<String, String> mapping = new HashMap<>();

        for (JsonSubTypes.Type type : types) {
            String name = type.name();
            Class<?> subClass = type.value();

            final Schema<?> subSchema = context.resolve(new AnnotatedType().type(subClass));
            String subSchemaName = subSchema.getName();
            /// Get a name from a ref instead, in swagger schema we can't have both name and ref
            if (subSchema.get$ref() != null) {
                subSchemaName = getSchemaNameFromRef(subSchema.get$ref());
            }
            mapping.put(name, refPrefix + subSchemaName);
        }
        discriminator.setMapping(mapping);

        baseType.setDiscriminator(discriminator);
        context.defineModel(baseType.getName(), baseType);
    }
    /// Helper to get Schema name from $ref
    private static String getSchemaNameFromRef(String ref) {
        String[] parts = ref.split("/");
        return parts[parts.length - 1];
    }
    /// Applies fixes to all declared schemas to generate proper polymorphic schemas
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void fixPolymorphicSchemas(OpenAPI openAPI) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return;
        }

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();


        // First pass: remove all allOf declarations
        for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
            String schemaName = entry.getKey();
            Schema schema = entry.getValue();
            clearAllOff(schema, schemas, schemaName);
        }
        // Replace oneOf in properties with references to a base type
        for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
            Schema schema = entry.getValue();
            replaceInlinedOneOfInSchema(schema, schemas);
        }
      //  openAPI.getComponents().setSchemas(schemas);

    }
/// Remove allOf from subtypes and add oneOf to base type
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void clearAllOff(Schema schema, Map<String, Schema> schemas, String schemaName) {
        String refString = refPrefix + schemaName;
        if (schema.getAllOf() == null || schema.getAllOf().isEmpty()) {
        return;
        }
       List<Schema> allOffs= schema.getAllOf();
        /// Find a base type in list of allOffs and replace it with ref
            for (Schema off : allOffs) {
                if(off.get$ref() == null){
                    continue;
                }
                String baseTypeRef = off.get$ref();
                String baseTypeName = getSchemaNameFromRef(baseTypeRef);
                Schema baseSchema = schemas.get(baseTypeName);
                if (baseSchema == null) {
                    return;
                }

                List<Schema> oneOffs = baseSchema.getOneOf();

                if (oneOffs != null) {
                    oneOffs.stream().filter(s -> s.get$ref().equals(refString)).findFirst().ifPresent(oneOffs::remove);
                } else {
                    oneOffs = new ArrayList<>();
                }
                Schema ref = new Schema<>();
                ref.set$ref(refString);
                oneOffs.add(ref);
                baseSchema.setOneOf(oneOffs);
            }
            /// Find declaration of subtype in allOffs
      Schema declaration =   allOffs.stream().filter(s -> s.get$ref() == null).findFirst().orElse(null);
        if(declaration != null) {
            /// copy required and replace subtype schema
            declaration.setRequired(schema.getRequired());
          schemas.put(schemaName, declaration);
        }

    }

    /**
     * Replace inlined oneOf in a single schema (handles nested properties)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static  void replaceInlinedOneOfInSchema(Schema schema, Map<String, Schema> schemas) {
        if (schema == null) {
            return;
        }

        Map<String, Schema> properties = schema.getProperties();
        if( properties == null  || properties.isEmpty() ) {
            return;
        }
        Map<String, Schema> updatedProps = new HashMap<>();
        for (Map.Entry<String, Schema> prop : properties.entrySet()) {
            String propName = prop.getKey();
            Schema propValue = prop.getValue();
            /// Handle nested properties (this should not happen, just in case)
            if(propValue.getProperties() != null && !propValue.getProperties().isEmpty()) {
replaceInlinedOneOfInSchema(propValue, schemas);
            }
            /// Handle Maps
          Object additionalProps = propValue.getAdditionalProperties();
          if(additionalProps instanceof Schema && ((Schema<?>) additionalProps).getOneOf() != null) {
              replaceOneOffPropertyWithReference(schemas, ((Schema<?>) additionalProps));
          }
          /// Handle Lists
            Schema propItems = propValue.getItems();
            List<Schema> propOneOf = propValue.getOneOf();
          if(propItems != null && propItems.getOneOf() != null) {
              replaceOneOffPropertyWithReference(schemas, propItems );
          }
            /// Handle Objects
            if (propOneOf != null && !propOneOf.isEmpty()) {
                replaceOneOffPropertyWithReference(schemas, propValue);
            }
            updatedProps.put(propName, propValue);
        }

        schema.setProperties(updatedProps);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void replaceOneOffPropertyWithReference(Map<String, Schema> schemas, Schema propValue) {
      /// Handle edge case, this should not happen
        if(propValue.get$ref() != null) {
            propValue.setOneOf(null);
            return;
        }
        if(propValue.getOneOf() == null || propValue.getOneOf().isEmpty()) {
            return;
        }
        for (Schema oneOfProp : (List<Schema>)propValue.getOneOf()) {
            /// Find a Schema declaration which has this ref in oneOf
            Map.Entry<String, Schema> baseTypeEntry = schemas.entrySet().stream().filter((s) -> {
                        List<Schema> itemOneOf = s.getValue().getOneOf();
                        if( itemOneOf == null || itemOneOf.isEmpty()) {
                            return false;
                        }
                        Schema sameRef =  itemOneOf.stream().filter((r -> r.get$ref().equals(oneOfProp.get$ref()))).findFirst().orElse(null);
                        return sameRef != null;
                    }
            ).findFirst().orElse(null);
            Schema baseType = baseTypeEntry != null ? baseTypeEntry.getValue() : null;
            if (baseType != null) {
                propValue.setOneOf(null);
                propValue.set$ref(refPrefix + baseTypeEntry.getKey());
            }
        }
    }
/// Remove inlined oneOf in requests themselves
    public static void fixRequestSchemas(OpenAPI openAPI) {
        if (openAPI.getPaths() == null) {
            return;
        }
        HashMap<String, PathItem> fixedPaths = new HashMap<>();
        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            PathItem pathItem = entry.getValue();
           /// process responses
    fixResponseSchema(pathItem.getGet(),  openAPI);
    fixResponseSchema(pathItem.getPost(), openAPI);

fixedPaths.put(entry.getKey(), pathItem);
        }
        openAPI.getPaths().putAll(fixedPaths);
    }
    /// Replace oneOf in the given operation to a ref, handles request body and response body
    private static void fixResponseSchema(Operation operation, OpenAPI openAPI) {
        if(operation == null) {
            return;
        }
if(operation.getRequestBody() != null) {
    fixContentSchema(operation.getRequestBody().getContent(), openAPI);
}
        ApiResponses responses =  operation.getResponses();
        if(responses == null) {
            return;
        }
        if(responses.get("200") == null) {
            return;
        }
        fixContentSchema(responses.get("200").getContent(), openAPI);
    }
/// Helper that handles content object and replaces oneOf
    private static void fixContentSchema(Content content, OpenAPI openAPI) {
        if(content == null) {
            return;
        }


        if(content.get("application/json") == null) {
            return;
        }

          Schema  schema =content.get("application/json").getSchema();
        if(schema == null) {
            return;
        }
          if(schema.getOneOf() != null) {
              replaceOneOffPropertyWithReference(openAPI.getComponents().getSchemas(), schema);
              /// In case some strange scenario happens and complex object with props will not be moved to schema
              replaceInlinedOneOfInSchema(schema, openAPI.getComponents().getSchemas());
          }

    }
}
