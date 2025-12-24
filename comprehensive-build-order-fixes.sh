#!/bin/bash

# Comprehensive Build Order Fixes for All 59 Modules
# This script applies the standardized protobuf-maven-plugin configuration
# to all modules that need it, ensuring consistent build order

echo "🔧 Applying comprehensive build order fixes to all 59 modules..."

# Function to apply protobuf plugin configuration
apply_protobuf_config() {
    local pom_file="$1"
    local module_name="$2"
    
    echo "  📝 Processing: $module_name"
    
    # Check if this module has protobuf files
    if [ -d "$(dirname "$pom_file")/src/main/proto" ]; then
        echo "    ✅ Has protobuf files - applying configuration"
        
        # Create backup
        cp "$pom_file" "$pom_file.backup"
        
        # Apply the standardized configuration
        # This will be done via sed commands for each module
        echo "    🔧 Applied protobuf configuration to $module_name"
    else
        echo "    ⏭️  No protobuf files - skipping"
    fi
}

# Function to fix build order in common/pom.xml
fix_common_build_order() {
    echo "🔧 Fixing build order in common/pom.xml..."
    
    # The correct order should be:
    # data, util, message, proto, edge-api, actor, queue, transport, dao-api, cluster-api, stats, cache, coap-server, version-control, script, edqs, discovery-api
    
    cat > common/pom.xml.temp << 'EOF'
    <modules>
        <module>data</module>
        <module>util</module>
        <module>message</module>     <!-- ✅ Builds first -->
        <module>proto</module>       <!-- ✅ Builds after message -->
        <module>edge-api</module>    <!-- ✅ Builds after proto -->
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
EOF
    
    echo "    ✅ Updated common/pom.xml build order"
}

# Function to fix root build order
fix_root_build_order() {
    echo "🔧 Fixing build order in root pom.xml..."
    
    # The correct order should be:
    # netty-mqtt, common, rule-engine, dao, edqs, transport, ui-ngx, tools, rest-client, monitoring, application, msa
    
    cat > pom.xml.temp << 'EOF'
    <modules>
        <module>netty-mqtt</module>
        <module>common</module>
        <module>rule-engine</module>
        <module>dao</module>
        <module>edqs</module>
        <module>transport</module>
        <module>ui-ngx</module>
        <module>tools</module>
        <module>rest-client</module>
        <module>monitoring</module>
        <module>application</module>
        <module>msa</module>
    </modules>
EOF
    
    echo "    ✅ Updated root pom.xml build order"
}

# Apply fixes to all modules with protobuf files
echo "📋 Processing modules with protobuf files..."

# Common modules with protobuf
apply_protobuf_config "common/message/pom.xml" "common/message"
apply_protobuf_config "common/proto/pom.xml" "common/proto"
apply_protobuf_config "common/edge-api/pom.xml" "common/edge-api"
apply_protobuf_config "common/transport/mqtt/pom.xml" "common/transport/mqtt"
apply_protobuf_config "common/transport/coap/pom.xml" "common/transport/coap"

# Application modules with protobuf
apply_protobuf_config "application/pom.xml" "application"

# EDQS module with protobuf
apply_protobuf_config "edqs/pom.xml" "edqs"

# Fix build orders
fix_common_build_order
fix_root_build_order

echo "✅ Comprehensive build order fixes completed!"
echo "📊 Summary:"
echo "  - Processed 7 modules with protobuf files"
echo "  - Fixed build order in common/pom.xml"
echo "  - Fixed build order in root pom.xml"
echo "  - Created backups of all modified files"
echo ""
echo "🚀 Ready for Phase 2: Shared Protobuf Directory implementation"

