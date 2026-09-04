// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.ai;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.ai.AiModel;

public interface TbAiModelService {

    AiModel save(AiModel model, User user);

    boolean delete(AiModel model, User user);

}
