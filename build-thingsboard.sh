#!/bin/bash

# ThingsBoard Build Script with Protobuf Cleanup Handling
# Long-term solution for file lock issues

set -e

echo "🚀 ThingsBoard Build Script - Enhanced Protobuf Handling"
echo "=================================================="

# Function to clean up protobuf temp directories
cleanup_protobuf_temp() {
    echo "🧹 Cleaning up protobuf temporary directories..."
    find . -name "protoc-dependencies" -type d -exec rm -rf {} + 2>/dev/null || true
    find . -name "protoc-temp" -type d -exec rm -rf {} + 2>/dev/null || true
    echo "✅ Protobuf cleanup completed"
}

# Function to aggressively clean target directories (for file lock issues)
cleanup_target_dirs() {
    echo "🧹 Cleaning up target directories..."
    # Clean MQTT-related target directories first
    for dir in common/transport/mqtt/target netty-mqtt/target transport/mqtt/target; do
        if [ -d "$dir" ]; then
            echo "   Removing $dir..."
            rm -rf "$dir" 2>/dev/null || true
        fi
    done
    # Clean protobuf-related target directories
    for dir in common/proto/target common/message/target common/edge-api/target; do
        if [ -d "$dir" ]; then
            echo "   Removing $dir..."
            rm -rf "$dir" 2>/dev/null || true
        fi
    done
    echo "✅ Target directory cleanup completed"
}

# Function to handle build with retry logic
build_with_retry() {
    local max_attempts=3
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        echo "🔄 Build attempt $attempt of $max_attempts"
        
        if mvn clean install -Dlicense.skip=true -DskipTests -Duse.shared-proto-deps=true -Dbuild.sequential=true; then
            echo "✅ Build successful on attempt $attempt"
            return 0
        else
            echo "❌ Build failed on attempt $attempt"
            # Enhanced cleanup on failure
            cleanup_protobuf_temp
            cleanup_target_dirs
            attempt=$((attempt + 1))
            if [ $attempt -le $max_attempts ]; then
                echo "⏳ Waiting 5 seconds before retry..."
                sleep 5
            fi
        fi
    done
    
    echo "💥 Build failed after $max_attempts attempts"
    return 1
}

# Main execution
echo "📋 Build Configuration:"
echo "   - License checks: SKIPPED"
echo "   - Tests: SKIPPED" 
echo "   - Shared protobuf: ENABLED"
echo "   - Sequential build: ENABLED"
echo "   - Max retries: 3"
echo ""

# Initial cleanup
cleanup_protobuf_temp
cleanup_target_dirs

# Execute build with retry logic
if build_with_retry; then
    echo ""
    echo "🎉 BUILD SUCCESSFUL!"
    echo "📊 Build artifacts generated:"
    find . -name "*.jar" -path "*/target/*" | grep -v "sources" | wc -l | xargs echo "   JAR files:"
    echo ""
    echo "🚀 Ready for deployment!"
else
    echo ""
    echo "💥 BUILD FAILED!"
    echo "🔍 Check the logs above for specific error details"
    echo "💡 Try running: ./build-thingsboard.sh"
    exit 1
fi
