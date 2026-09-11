// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.util.ContactBasedEntityDetails;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class TbAbstractGetEntityDetailsNodeConfiguration extends TbAbstractFetchToNodeConfiguration {

    private List<ContactBasedEntityDetails> detailsList;

}
