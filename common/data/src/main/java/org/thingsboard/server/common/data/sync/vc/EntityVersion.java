// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.vc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntityVersion implements Serializable {

    private static final long serialVersionUID = -3705022663019175258L;
    
    private long timestamp;
    private String id;
    private String name;
    private String author;
}
