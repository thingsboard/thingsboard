# Cascading Implementation Plan: From Immediate to Long-term Solutions

## Executive Summary

This plan implements the three solutions in a cascading manner, where each phase builds upon the previous one, ensuring stability and minimizing risk while progressively solving the circular dependency issues.

## Phase 1: Immediate Solution - Plugin Upgrade (Week 1)

### **Objective**
Upgrade protobuf-maven-plugin from 0.6.1 to 0.6.2 to resolve cleanup failures and improve build stability.

### **Changes Required**

#### **1.1 Update Root POM**
```xml
<!-- File: pom.xml -->
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>0.6.2</version>  <!-- Changed from 0.6.1 -->
    <configuration>
        <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
        <pluginId>grpc-java</pluginId>
        <pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}</pluginArtifact>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
                <goal>compile-custom</goal>
                <goal>test-compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### **1.2 Verify All Module Configurations**
Ensure all 7 protobuf modules maintain their current configurations:
- `common/message/pom.xml`
- `common/proto/pom.xml`
- `common/edge-api/pom.xml`
- `common/transport/mqtt/pom.xml`
- `common/transport/coap/pom.xml`
- `application/pom.xml`
- `edqs/pom.xml`

### **Implementation Steps**

```bash
# Step 1: Backup current configuration
cp pom.xml pom.xml.backup

# Step 2: Update plugin version
sed -i 's/<version>0.6.1<\/version>/<version>0.6.2<\/version>/' pom.xml

# Step 3: Test build
mvn clean install -DskipTests

# Step 4: Verify results
echo "Build completed with exit code: $?"
```

### **Expected Results**
- ✅ Reduced cleanup failures
- ✅ Better error messages
- ✅ Improved build stability
- ✅ All existing functionality preserved

### **Validation Criteria**
- [ ] `mvn clean install -DskipTests` completes successfully
- [ ] No "Unable to clean up temporary proto file directory" errors
- [ ] All protobuf classes generated correctly
- [ ] Build time improved or maintained

---

## Phase 2: Short-term Solution - Shared Protobuf Directory (Week 2-3)

### **Objective**
Implement shared protobuf directory to eliminate file conflicts and enable parallel builds.

### **Changes Required**

#### **2.1 Create Shared Directory Structure**
```bash
# Create shared protobuf directory
mkdir -p shared-proto-deps/

# Copy shared proto files
cp common/message/src/main/proto/tbmsg.proto shared-proto-deps/
cp common/proto/src/main/proto/queue.proto shared-proto-deps/
cp common/proto/src/main/proto/transport.proto shared-proto-deps/
cp common/edge-api/src/main/proto/edge.proto shared-proto-deps/
```

#### **2.2 Update Root POM for Shared Directory**
```xml
<!-- File: pom.xml -->
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>0.6.2</version>  <!-- From Phase 1 -->
    <configuration>
        <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
        <pluginId>grpc-java</pluginId>
        <pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}</pluginArtifact>
        <!-- New: Shared directory configuration -->
        <protoSourceRoot>${project.basedir}/shared-proto-deps</protoSourceRoot>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
                <goal>compile-custom</goal>
                <goal>test-compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### **2.3 Update All Protobuf Module Configurations**
```xml
<!-- File: common/message/pom.xml -->
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
            <id>protoc-test</id>
            <phase>generate-test-sources</phase>
            <goals>
                <goal>test-compile</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <checkStaleness>false</checkStaleness>
        <clearOutputDirectory>false</clearOutputDirectory>
        <!-- New: Use shared directory -->
        <protoSourceRoot>${project.basedir}/../shared-proto-deps</protoSourceRoot>
    </configuration>
</plugin>
```

**Apply similar configuration to:**
- `common/proto/pom.xml`
- `common/edge-api/pom.xml`
- `common/transport/mqtt/pom.xml`
- `common/transport/coap/pom.xml`
- `application/pom.xml`
- `edqs/pom.xml`

#### **2.4 Create Maven Profile for Backward Compatibility**
```xml
<!-- File: pom.xml -->
<profiles>
    <profile>
        <id>shared-proto</id>
        <activation>
            <property>
                <name>use.shared.proto</name>
            </property>
        </activation>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.xolstice.maven.plugins</groupId>
                    <artifactId>protobuf-maven-plugin</artifactId>
                    <configuration>
                        <protoSourceRoot>${project.basedir}/shared-proto-deps</protoSourceRoot>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

### **Implementation Steps**

```bash
# Step 1: Create shared directory
mkdir -p shared-proto-deps/

# Step 2: Copy shared proto files
cp common/message/src/main/proto/tbmsg.proto shared-proto-deps/
cp common/proto/src/main/proto/queue.proto shared-proto-deps/
cp common/proto/src/main/proto/transport.proto shared-proto-deps/
cp common/edge-api/src/main/proto/edge.proto shared-proto-deps/

# Step 3: Update all POM files
# (Manual editing required for each module)

# Step 4: Test with shared directory
mvn clean install -DskipTests -Duse.shared.proto=true

# Step 5: Test without shared directory (backward compatibility)
mvn clean install -DskipTests

# Step 6: Test parallel build
mvn clean install -DskipTests -T 4
```

### **Expected Results**
- ✅ Eliminated file conflicts between modules
- ✅ Parallel build support
- ✅ Reduced build time
- ✅ Backward compatibility maintained

### **Validation Criteria**
- [ ] `mvn clean install -DskipTests -Duse.shared.proto=true` completes successfully
- [ ] `mvn clean install -DskipTests` still works (backward compatibility)
- [ ] `mvn clean install -DskipTests -T 4` works (parallel build)
- [ ] No file conflicts between modules
- [ ] All protobuf classes generated correctly

---

## Phase 3: Long-term Solution - Module Consolidation (Month 2-3)

### **Objective**
Consolidate all protobuf generation into a single module to eliminate circular dependencies and simplify architecture.

### **Changes Required**

#### **3.1 Create Consolidated Protobuf Module**
```bash
# Create new module structure
mkdir -p protobuf-generator/src/main/proto/
mkdir -p protobuf-generator/src/main/java/
mkdir -p protobuf-generator/src/test/java/
```

#### **3.2 Create Consolidated Module POM**
```xml
<!-- File: protobuf-generator/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.thingsboard</groupId>
        <artifactId>thingsboard</artifactId>
        <version>4.3.0-SNAPSHOT</version>
    </parent>

    <groupId>org.thingsboard.common</groupId>
    <artifactId>protobuf-generator</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.thingsboard.common</groupId>
            <artifactId>data</artifactId>
        </dependency>
        <dependency>
            <groupId>org.thingsboard.common</groupId>
            <artifactId>util</artifactId>
        </dependency>
        <dependency>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-protobuf</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <version>0.6.2</version>  <!-- From Phase 1 -->
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
                    <protoSourceRoot>${project.basedir}/src/main/proto</protoSourceRoot>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### **3.3 Move All Proto Files to Consolidated Module**
```bash
# Move proto files from individual modules
mv common/message/src/main/proto/tbmsg.proto protobuf-generator/src/main/proto/
mv common/proto/src/main/proto/queue.proto protobuf-generator/src/main/proto/
mv common/proto/src/main/proto/transport.proto protobuf-generator/src/main/proto/
mv common/edge-api/src/main/proto/edge.proto protobuf-generator/src/main/proto/
mv common/transport/mqtt/src/main/proto/sparkplug.proto protobuf-generator/src/main/proto/
mv common/transport/coap/src/main/proto/efento/*.proto protobuf-generator/src/main/proto/efento/
```

#### **3.4 Update Build Order**
```xml
<!-- File: common/pom.xml -->
<modules>
    <module>data</module>
    <module>util</module>
    <module>protobuf-generator</module>  <!-- New: Consolidated module -->
    <module>message</module>             <!-- Keep for backward compatibility -->
    <module>proto</module>               <!-- Keep for backward compatibility -->
    <module>edge-api</module>            <!-- Keep for backward compatibility -->
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

#### **3.5 Update Dependencies in All Modules**
```xml
<!-- File: common/queue/pom.xml -->
<dependencies>
    <!-- New: Consolidated protobuf dependency -->
    <dependency>
        <groupId>org.thingsboard.common</groupId>
        <artifactId>protobuf-generator</artifactId>
    </dependency>
    
    <!-- Keep old dependencies for backward compatibility -->
    <dependency>
        <groupId>org.thingsboard.common</groupId>
        <artifactId>proto</artifactId>
        <scope>provided</scope>  <!-- Mark as provided to avoid conflicts -->
    </dependency>
    
    <!-- ... other dependencies ... -->
</dependencies>
```

**Apply similar dependency updates to:**
- `common/transport/pom.xml`
- `common/cluster-api/pom.xml`
- `common/cache/pom.xml`
- `common/edqs/pom.xml`
- `common/discovery-api/pom.xml`
- `application/pom.xml`
- `edqs/pom.xml`

#### **3.6 Create Migration Profile**
```xml
<!-- File: pom.xml -->
<profiles>
    <profile>
        <id>consolidated-protobuf</id>
        <activation>
            <property>
                <name>use.consolidated.protobuf</name>
            </property>
        </activation>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.xolstice.maven.plugins</groupId>
                    <artifactId>protobuf-maven-plugin</artifactId>
                    <configuration>
                        <skip>true</skip>  <!-- Skip individual module protobuf generation -->
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

### **Implementation Steps**

```bash
# Step 1: Create consolidated module structure
mkdir -p protobuf-generator/src/main/proto/
mkdir -p protobuf-generator/src/main/java/
mkdir -p protobuf-generator/src/test/java/

# Step 2: Create consolidated module POM
# (Manual creation of protobuf-generator/pom.xml)

# Step 3: Move all proto files
mv common/message/src/main/proto/tbmsg.proto protobuf-generator/src/main/proto/
mv common/proto/src/main/proto/queue.proto protobuf-generator/src/main/proto/
mv common/proto/src/main/proto/transport.proto protobuf-generator/src/main/proto/
mv common/edge-api/src/main/proto/edge.proto protobuf-generator/src/main/proto/
mv common/transport/mqtt/src/main/proto/sparkplug.proto protobuf-generator/src/main/proto/
mkdir -p protobuf-generator/src/main/proto/efento/
mv common/transport/coap/src/main/proto/efento/*.proto protobuf-generator/src/main/proto/efento/

# Step 4: Update build order
# (Manual editing of common/pom.xml)

# Step 5: Update all module dependencies
# (Manual editing of all affected POM files)

# Step 6: Test consolidated build
mvn clean install -DskipTests -Duse.consolidated.protobuf=true

# Step 7: Test backward compatibility
mvn clean install -DskipTests

# Step 8: Test parallel build
mvn clean install -DskipTests -T 4
```

### **Expected Results**
- ✅ Simplified architecture
- ✅ Single source of truth for protobuf generation
- ✅ Eliminated circular dependencies
- ✅ Improved build performance
- ✅ Backward compatibility maintained

### **Validation Criteria**
- [ ] `mvn clean install -DskipTests -Duse.consolidated.protobuf=true` completes successfully
- [ ] `mvn clean install -DskipTests` still works (backward compatibility)
- [ ] `mvn clean install -DskipTests -T 4` works (parallel build)
- [ ] All protobuf classes generated in single module
- [ ] No circular dependencies
- [ ] Build time improved

---

## Implementation Timeline

### **Week 1: Phase 1 - Plugin Upgrade**
- **Day 1-2**: Update plugin version and test
- **Day 3-4**: Validate all modules work with new version
- **Day 5**: Document results and prepare for Phase 2

### **Week 2-3: Phase 2 - Shared Directory**
- **Week 2**: Create shared directory and update configurations
- **Week 3**: Test and validate shared directory approach
- **End of Week 3**: Document results and prepare for Phase 3

### **Month 2-3: Phase 3 - Module Consolidation**
- **Month 2**: Create consolidated module and move proto files
- **Month 3**: Update dependencies and test consolidated approach
- **End of Month 3**: Complete migration and document results

## Risk Mitigation

### **Phase 1 Risks**
- **Risk**: Plugin upgrade breaks existing functionality
- **Mitigation**: Test thoroughly before proceeding to Phase 2

### **Phase 2 Risks**
- **Risk**: Shared directory approach causes new conflicts
- **Mitigation**: Maintain backward compatibility profiles

### **Phase 3 Risks**
- **Risk**: Module consolidation breaks existing dependencies
- **Mitigation**: Gradual migration with fallback mechanisms

## Success Metrics

### **Phase 1 Success**
- [ ] Build failures reduced by 80%
- [ ] Cleanup errors eliminated
- [ ] Build time maintained or improved

### **Phase 2 Success**
- [ ] File conflicts eliminated
- [ ] Parallel build support enabled
- [ ] Build time improved by 20%

### **Phase 3 Success**
- [ ] Circular dependencies eliminated
- [ ] Architecture simplified
- [ ] Build time improved by 30%
- [ ] Maintenance overhead reduced

## Conclusion

This cascading implementation plan provides a structured approach to solving the circular dependency issues while minimizing risk and ensuring stability. Each phase builds upon the previous one, creating a comprehensive solution that addresses the root causes of the build problems.

**Key Benefits:**
- ✅ **Sequential implementation** minimizes risk
- ✅ **Backward compatibility** maintained throughout
- ✅ **Progressive improvement** with measurable results
- ✅ **Comprehensive solution** addressing all aspects of the problem
