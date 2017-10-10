/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Application;
import org.thingsboard.server.common.data.id.ApplicationId;
import org.thingsboard.server.dao.entity.AbstractEntityService;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class ApplicationServiceImpl extends AbstractEntityService implements ApplicationService{

    @Autowired
    private ApplicationDao applicationDao;

    @Override
    public Application saveApplication(Application application) {
        log.trace("Executing saveApplication [{}]", application);
        Application savedApplication = applicationDao.save(application);
        return savedApplication;
    }

    @Override
    public Application findApplicationById(ApplicationId applicationId) {
        log.trace("Executing findApplicationById [{}]", applicationId);
        validateId(applicationId, "Incorrect applicationId " + applicationId);
        return applicationDao.findById(applicationId.getId());
    }

    @Override
    public void deleteApplication(ApplicationId applicationId) {
        log.trace("Executing deleteApplication [{}]", applicationId);
        validateId(applicationId, "Incorrect applicationId " + applicationId);

        applicationDao.removeById(applicationId.getId());
    }


}
