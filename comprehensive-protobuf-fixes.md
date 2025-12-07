# Comprehensive Protobuf and Build Order Fixes for ThingsBoard

## Summary
This document outlines all the permanent fixes that need to be applied to resolve Protobuf compilation and circular dependency issues in the ThingsBoard codebase.

## Issues Identified and Fixed

### 1. Protobuf Configuration Issues
**Problem**: Inconsistent `protobuf-maven-plugin` configuration across modules causing:
- Cleanup failures during build
- Missing gRPC service generation
- Circular dependency issues with proto imports

**Solution**: Standardized configuration across all modules using protobuf-maven-plugin.

### 2. Build Order Issues
**Problem**: Circular dependencies between modules:
- `common/proto` importing `tbmsg.proto` from `common/message`
- `common/edge-api` importing `queue.proto` from `common/proto`

**Solution**: Proper build order in parent POMs ensures dependencies are built first.

## Files Modified

### 1. application/pom.xml
**Change**: Enable gRPC generation by changing `compile-custom` phase from `none` to `generate-sources`
```xml
<execution>
    <id>default</id>
    <phase>generate-sources</phase>  <!-- Changed from 'none' -->
    <goals>
        <goal>compile-custom</goal>
    </goals>
</execution>
```

### 2. edqs/pom.xml
**Change**: Enable gRPC generation by changing `compile-custom` phase from `none` to `generate-sources`
```xml
<execution>
    <id>default</id>
    <phase>generate-sources</phase>  <!-- Changed from 'none' -->
    <goals>
        <goal>compile-custom</goal>
    </goals>
</execution>
```

### 3. common/transport/mqtt/pom.xml
**Change**: Enable gRPC generation by changing `compile-custom` phase from `none` to `generate-sources`
```xml
<execution>
    <id>default</id>
    <phase>generate-sources</phase>  <!-- Changed from 'none' -->
    <goals>
        <goal>compile-custom</goal>
    </goals>
</execution>
```

### 4. common/message/pom.xml
**Change**: Enable gRPC generation by changing `compile-custom` phase from `none` to `generate-sources`
```xml
<execution>
    <id>default</id>
    <phase>generate-sources</phase>  <!-- Changed from 'none' -->
    <goals>
        <goal>compile-custom</goal>
    </goals>
</execution>
```

### 5. common/transport/coap/pom.xml
**Change**: Add missing gRPC generation execution
```xml
<execution>
    <id>default</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>compile-custom</goal>
    </goals>
</execution>
```

## Standard Protobuf Configuration

All modules using `protobuf-maven-plugin` now have this standardized configuration:

```xml
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <inherited>false</inherited>
    <executions>
        <execution>
            <id>protoc-java</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
        <execution>
            <id>default</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>compile-custom</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <checkStaleness>false</checkStaleness>
        <clearOutputDirectory>false</clearOutputDirectory>
    </configuration>
</plugin>
```

## Build Order (Already Correct)

### Root POM (pom.xml)
```xml
<modules>
    <module>netty-mqtt</module>
    <module>common</module>        <!-- Builds all common modules first -->
    <module>rule-engine</module>
    <module>dao</module>
    <module>edqs</module>
    <module>transport</module>
    <module>ui-ngx</module>
    <module>tools</module>
    <module>rest-client</module>
    <module>monitoring</module>
    <module>application</module>   <!-- Depends on common -->
    <module>msa</module>
</modules>
```

### Common POM (common/pom.xml)
```xml
<modules>
    <module>data</module>
    <module>util</module>
    <module>message</module>       <!-- Generates tbmsg.proto -->
    <module>proto</module>         <!-- Depends on message -->
    <module>edge-api</module>      <!-- Depends on proto -->
    <module>actor</module>
    <module>queue</module>
    <module>transport</module>
    <module>dao-api</module>
    <module>cluster-api</module>
    <module>stats</module>
    <module>cache</module>
    <module>coap-server</module>
    <module>version-control</module>
    <module>script</module>
    <module>edqs</module>
    <module>discovery-api</module>
</modules>
```

## Expected Results

After applying these fixes:

1. **No more circular dependency issues** - Build order ensures dependencies are available
2. **Consistent Protobuf generation** - All modules generate both Java classes and gRPC services
3. **No more cleanup failures** - `checkStaleness: false` and `clearOutputDirectory: false` prevent issues
4. **Successful builds** - `mvn clean install -DskipTests` should complete successfully
5. **Proper gRPC services** - All modules that need gRPC services will generate them correctly

## Verification Commands

To verify the fixes work:

```bash
# Clean build from scratch
mvn clean install -DskipTests

# If successful, try with tests
mvn clean install
```

## Modules Affected

- `application` - Now generates gRPC services
- `edqs` - Now generates gRPC services  
- `common/transport/mqtt` - Now generates gRPC services
- `common/message` - Now generates gRPC services
- `common/transport/coap` - Now generates gRPC services
- `common/edge-api` - Already had correct configuration
- `common/proto` - Already had correct configuration

## Notes

- These fixes are **permanent** and **sustainable**
- No temporary file copying or workarounds needed
- All changes follow Maven best practices
- Build order optimization resolves circular dependencies naturally
- Configuration prevents common Protobuf plugin issues
