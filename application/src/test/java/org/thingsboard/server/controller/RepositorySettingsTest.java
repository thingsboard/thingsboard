// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
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
