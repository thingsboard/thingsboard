// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.relation;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.relation.EntityRelation;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode
@Getter
@RequiredArgsConstructor
@Builder
public class RelationCacheValue implements Serializable {

    private static final long serialVersionUID = 3911151843961657570L;

    private final EntityRelation relation;
    private final List<EntityRelation> relations;

}
