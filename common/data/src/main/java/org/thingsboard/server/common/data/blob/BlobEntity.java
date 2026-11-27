// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.blob;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.BlobEntityId;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Optional;

@Data
@EqualsAndHashCode(callSuper = true)
public class BlobEntity extends BlobEntityInfo {

    private static final long serialVersionUID = 2807343057809549363L;

    private transient ByteBuffer data;

    public BlobEntity() {
        super();
    }

    public BlobEntity(BlobEntityId id) {
        super(id);
    }

    public BlobEntity(BlobEntity blobEntity) {
        super(blobEntity);
        this.data = blobEntity.getData();
    }

    @Schema(description = "Blob entity data", example = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPExXTTJNPgo8T2JqZWN0IE9iamVjdFR5cGU9Ik1PRGVmaW5pdGlvbiI+CjxOYW1lPkJpbmFyeUFwcERhdGFDb250YWluZXI8L05hbWU+CjxEZXNjcmlwdGlvbjE+QXBwbGljYXRpb24gZGF0YSBhcyByYXcgYnl0ZXMuPC9EZXNjcmlwdGlvbjE+CjxPYmplY3RJRD4xOTwvT2JqZWN0SUQ+CjxPYmplY3RVUk4+dXJuOm9tYTpsd20ybTpvbWE6MTk8L09iamVjdFVSTj4KPE11bHRpcGxlSW5zdGFuY2VzPk11bHRpcGxlPC9NdWx0aXBsZUluc3RhbmNlcz4KPE1hbmRhdG9yeT5PcHRpb25hbDwvTWFuZGF0b3J5Pgo8UmVzb3VyY2VzPgo8SXRlbSBJRD0iMCI+CjxOYW1lPkRhdGE8L05hbWU+CjxPcGVyYXRpb25zPlJXPC9PcGVyYXRpb25zPgo8TXVsdGlwbGVJbnN0YW5jZXM+TXVsdGlwbGU8L011bHRpcGxlSW5zdGFuY2VzPgo8TWFuZGF0b3J5Pk1hbmRhdG9yeTwvTWFuZGF0b3J5Pgo8VHlwZT5PcGFxdWU8L1R5cGU+CjxSYW5nZUVudW1lcmF0aW9uPjwvUmFuZ2VFbnVtZXJhdGlvbj4KPFVuaXRzPjwvVW5pdHM+CjxEZXNjcmlwdGlvbj5UaGUgYXBwbGljYXRpb24gZGF0YS48L0Rlc2NyaXB0aW9uPgo8L0l0ZW0+CjwvUmVzb3VyY2VzPgo8RGVzY3JpcHRpb24yPjwvRGVzY3JpcHRpb24yPgo8L09iamVjdD4KPC9MV00yTT4K", accessMode = Schema.AccessMode.READ_WRITE)
    @JsonGetter("data")
    public String getEncodedData() {
        return Optional.ofNullable(data)
                .map(ByteBuffer::array)
                .map(Base64.getEncoder()::encodeToString)
                .orElse(null);
    }

    @JsonSetter("data")
    public void setEncodedData(String data) {
        this.data = Optional.ofNullable(data)
                .map(Base64.getDecoder()::decode)
                .map(ByteBuffer::wrap)
                .orElse(null);
    }

}
