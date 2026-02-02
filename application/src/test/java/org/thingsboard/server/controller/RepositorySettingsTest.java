/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.thingsboard.server.common.data.sync.vc.RepositoryAuthMethod;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.sync.vc.GitVersionControlQueueService;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class RepositorySettingsTest extends AbstractControllerTest {

    @MockBean
    private GitVersionControlQueueService gitVersionControlQueueService;

    @Test
    public void testFindRepositorySettings() throws Exception {
        loginTenantAdmin();
        doGet("/api/admin/repositorySettings")
                .andExpect(status().isNotFound());

        String testRepositoryUri = "https://github.com/test/version-control-test-repository.git";

        SettableFuture<Void> successFuture = SettableFuture.create();
        successFuture.set(null);
        when(gitVersionControlQueueService.initRepository(any(), any()))
                .thenReturn(successFuture);

        RepositorySettings repositorySettings = new RepositorySettings();
        repositorySettings.setPassword("test");
        repositorySettings.setAuthMethod(RepositoryAuthMethod.USERNAME_PASSWORD);
        repositorySettings.setRepositoryUri(testRepositoryUri);
        repositorySettings.setDefaultBranch("main");
        doPost("/api/admin/repositorySettings", repositorySettings)
                .andExpect(status().isOk());

        // check repository settings
        doGet("/api/admin/repositorySettings")
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.repositoryUri", is(testRepositoryUri)));

        // delete settings
        when(gitVersionControlQueueService.clearRepository(any()))
                .thenReturn(successFuture);
        doDelete("/api/admin/repositorySettings")
                .andExpect(status().isOk());

        // check repository settings
        doGet("/api/admin/repositorySettings")
                .andExpect(status().isNotFound());
    }

}
