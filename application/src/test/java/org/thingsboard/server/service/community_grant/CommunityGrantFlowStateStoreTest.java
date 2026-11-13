// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.community_grant.CommunityGrantFlowState;
import org.thingsboard.server.common.data.community_grant.CommunityGrantMode;
import org.thingsboard.server.common.data.community_grant.CommunityGrantParkReason;
import org.thingsboard.server.common.data.community_grant.CommunityGrantState;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityGrantFlowStateStoreTest {

    @Mock
    private AdminSettingsService adminSettingsService;

    @InjectMocks
    private CommunityGrantFlowStateStore store;

    @Test
    void testGetReturnsTheInitialStateWhenNothingIsStored() {
        givenStored(null);

        CommunityGrantFlowState state = store.get();

        assertThat(state.getState()).isEqualTo(CommunityGrantState.NOT_STARTED);
        assertThat(state.getMode()).isEqualTo(CommunityGrantMode.ONLINE);
        assertThat(state.getSignUpUrl()).isNull();
        assertThat(state.getLastPolledAt()).isNull();
    }

    @Test
    void testGetReadsTheStoredState() {
        CommunityGrantFlowState stored = CommunityGrantFlowState.initial();
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setSignUpUrl("https://portal.example/signup?clusterId=x&claimToken=y&offline=true");
        stored.setLastPolledAt(42L);
        givenStored(settingsOf(stored));

        CommunityGrantFlowState state = store.get();

        assertThat(state.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(state.getMode()).isEqualTo(CommunityGrantMode.OFFLINE);
        assertThat(state.getSignUpUrl())
                .isEqualTo("https://portal.example/signup?clusterId=x&claimToken=y&offline=true");
        assertThat(state.getLastPolledAt()).isEqualTo(42L);
    }

    @Test
    void testUpdateRoundTripsTheFieldsTheFlowCarriesBetweenPolls() {
        givenStored(null);

        store.update(state -> {
            state.setState(CommunityGrantState.COLLECTING);
            state.setStartedAt(1234L);
            state.setLastOfflineCheckAt(5678L);
            state.setParkReason(CommunityGrantParkReason.CHECK_FAILED);
            return state;
        });

        ArgumentCaptor<AdminSettings> captor = ArgumentCaptor.forClass(AdminSettings.class);
        verify(adminSettingsService).saveAdminSettings(eq(TenantId.SYS_TENANT_ID), captor.capture());
        when(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, CommunityGrantFlowStateStore.SETTINGS_KEY))
                .thenReturn(captor.getValue());

        CommunityGrantFlowState reread = store.get();

        assertThat(reread.getStartedAt()).isEqualTo(1234L);
        assertThat(reread.getLastOfflineCheckAt()).isEqualTo(5678L);
        assertThat(reread.getParkReason()).isEqualTo(CommunityGrantParkReason.CHECK_FAILED);
    }

    @Test
    void testUpdateCreatesTheSettingsRowOnFirstWrite() {
        givenStored(null);

        CommunityGrantFlowState updated = store.update(state -> {
            state.setState(CommunityGrantState.AWAITING_SIGNUP);
            state.setSignUpUrl("https://portal.example/signup");
            return state;
        });

        assertThat(updated.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        ArgumentCaptor<AdminSettings> captor = ArgumentCaptor.forClass(AdminSettings.class);
        verify(adminSettingsService).saveAdminSettings(eq(TenantId.SYS_TENANT_ID), captor.capture());
        AdminSettings saved = captor.getValue();
        assertThat(saved.getKey()).isEqualTo(CommunityGrantFlowStateStore.SETTINGS_KEY);
        assertThat(saved.getTenantId()).isEqualTo(TenantId.SYS_TENANT_ID);
        JsonNode json = saved.getJsonValue();
        assertThat(json.get("state").asText()).isEqualTo("AWAITING_SIGNUP");
        assertThat(json.get("signUpUrl").asText()).isEqualTo("https://portal.example/signup");
    }

    @Test
    void testUpdatePreservesFieldsTheUpdaterDoesNotTouch() {
        CommunityGrantFlowState stored = CommunityGrantFlowState.initial();
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);
        stored.setSignUpUrl("https://portal.example/signup");
        givenStored(settingsOf(stored));

        store.update(state -> {
            state.setLastPolledAt(100L);
            return state;
        });

        ArgumentCaptor<AdminSettings> captor = ArgumentCaptor.forClass(AdminSettings.class);
        verify(adminSettingsService).saveAdminSettings(eq(TenantId.SYS_TENANT_ID), captor.capture());
        JsonNode json = captor.getValue().getJsonValue();
        assertThat(json.get("signUpUrl").asText()).isEqualTo("https://portal.example/signup");
        assertThat(json.get("lastPolledAt").asLong()).isEqualTo(100L);
    }

    @Test
    void testGetFallsBackToTheInitialStateOnUnreadableJson() {
        givenStored(settingsOf("{\"state\":\"NOT_A_STATE\"}"));

        assertThat(store.get().getState()).isEqualTo(CommunityGrantState.NOT_STARTED);
    }

    @Test
    void testUpdateThrowsOnUnreadableJsonAndLeavesTheStoredDocumentUntouched() {
        givenStored(settingsOf("{\"state\":\"NOT_A_STATE\"}"));

        assertThatThrownBy(() -> store.update(state -> state))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be parsed");

        verify(adminSettingsService, never()).saveAdminSettings(any(), any());
    }

    @Test
    void testUpdateRetriesAsAnUpdateWhenAConcurrentFirstWriteWinsTheCreateRace() {
        CommunityGrantFlowState winnerState = CommunityGrantFlowState.initial();
        winnerState.setState(CommunityGrantState.COLLECTING);
        winnerState.setSignUpUrl("https://portal.example/signup?winner=true");
        AdminSettings winnerRow = new AdminSettings();
        winnerRow.setId(new AdminSettingsId(UUID.randomUUID()));
        winnerRow.setTenantId(TenantId.SYS_TENANT_ID);
        winnerRow.setKey(CommunityGrantFlowStateStore.SETTINGS_KEY);
        winnerRow.setJsonValue(JacksonUtil.valueToTree(winnerState));
        // No row on the first read; the re-read after the lost create finds the other node's row.
        when(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, CommunityGrantFlowStateStore.SETTINGS_KEY))
                .thenReturn(null, winnerRow);
        when(adminSettingsService.saveAdminSettings(eq(TenantId.SYS_TENANT_ID), argThat(s -> s.getId() == null)))
                .thenThrow(new DataValidationException("Admin settings with such name already exists!"));
        when(adminSettingsService.saveAdminSettings(eq(TenantId.SYS_TENANT_ID), argThat(s -> s != null && s.getId() != null)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        CommunityGrantFlowState result = store.update(state -> {
            state.setLastPolledAt(999L);
            return state;
        });

        assertThat(result.getState()).isEqualTo(CommunityGrantState.COLLECTING);
        assertThat(result.getSignUpUrl()).isEqualTo("https://portal.example/signup?winner=true");
        assertThat(result.getLastPolledAt()).isEqualTo(999L);
        verify(adminSettingsService, times(2)).saveAdminSettings(eq(TenantId.SYS_TENANT_ID), any(AdminSettings.class));
    }

    @Test
    void testAnUnreadableWinnerDocumentThrowsAndKeepsTheCreateRaceAttached() {
        AdminSettings winnerRow = new AdminSettings();
        winnerRow.setId(new AdminSettingsId(UUID.randomUUID()));
        winnerRow.setTenantId(TenantId.SYS_TENANT_ID);
        winnerRow.setKey(CommunityGrantFlowStateStore.SETTINGS_KEY);
        winnerRow.setJsonValue(JacksonUtil.toJsonNode("{\"state\":\"NOT_A_STATE\"}"));
        when(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, CommunityGrantFlowStateStore.SETTINGS_KEY))
                .thenReturn(null, winnerRow);
        when(adminSettingsService.saveAdminSettings(eq(TenantId.SYS_TENANT_ID), argThat(s -> s.getId() == null)))
                .thenThrow(new DataValidationException("Admin settings with such name already exists!"));

        assertThatThrownBy(() -> store.update(state -> {
            state.setState(CommunityGrantState.AWAITING_SIGNUP);
            return state;
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be parsed")
                .satisfies(e -> assertThat(e.getSuppressed())
                        .as("the create race that led to the re-read")
                        .anySatisfy(suppressed -> assertThat(suppressed)
                                .isInstanceOf(DataValidationException.class)));

        verify(adminSettingsService, times(1)).saveAdminSettings(eq(TenantId.SYS_TENANT_ID), any(AdminSettings.class));
    }

    @Test
    void testUpdateDoesNotRetryWhenTheRejectedWriteWasAnUpdateRatherThanACreate() {
        AdminSettings stored = settingsOf(CommunityGrantFlowState.initial());
        stored.setId(new AdminSettingsId(UUID.randomUUID()));
        givenStored(stored);
        when(adminSettingsService.saveAdminSettings(eq(TenantId.SYS_TENANT_ID), any(AdminSettings.class)))
                .thenThrow(new DataValidationException("Admin settings with such name already exists!"));

        assertThatThrownBy(() -> store.update(state -> {
            state.setState(CommunityGrantState.AWAITING_SIGNUP);
            return state;
        })).isInstanceOf(DataValidationException.class);

        verify(adminSettingsService, times(1)).saveAdminSettings(eq(TenantId.SYS_TENANT_ID), any(AdminSettings.class));
    }

    @Test
    void testUpdateDoesNotRetryWhenTheRejectedCreateFindsNoWinnerRow() {
        givenStored(null);
        when(adminSettingsService.saveAdminSettings(eq(TenantId.SYS_TENANT_ID), any(AdminSettings.class)))
                .thenThrow(new DataValidationException("Admin settings with such name already exists!"));

        assertThatThrownBy(() -> store.update(state -> {
            state.setState(CommunityGrantState.AWAITING_SIGNUP);
            return state;
        })).isInstanceOf(DataValidationException.class);

        verify(adminSettingsService, times(1)).saveAdminSettings(eq(TenantId.SYS_TENANT_ID), any(AdminSettings.class));
    }

    @Test
    void testUpdateSucceedsWhenTheStoredDocumentParsesButCarriesNoState() {
        givenStored(settingsOf("{}"));

        CommunityGrantFlowState updated = store.update(state -> {
            state.setState(CommunityGrantState.AWAITING_SIGNUP);
            return state;
        });

        assertThat(updated.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        verify(adminSettingsService).saveAdminSettings(eq(TenantId.SYS_TENANT_ID), any(AdminSettings.class));
    }

    @Test
    void testUpdateSucceedsWhenTheStoredJsonValueIsAJsonNull() {
        givenStored(settingsOf("null"));

        CommunityGrantFlowState updated = store.update(state -> {
            state.setState(CommunityGrantState.AWAITING_SIGNUP);
            return state;
        });

        assertThat(updated.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        verify(adminSettingsService).saveAdminSettings(eq(TenantId.SYS_TENANT_ID), any(AdminSettings.class));
    }

    @Test
    void testGetDefaultsTheModeWhenTheStoredDocumentHasNone() {
        givenStored(settingsOf("{\"state\":\"COLLECTING\"}"));

        CommunityGrantFlowState state = store.get();

        assertThat(state.getState()).isEqualTo(CommunityGrantState.COLLECTING);
        assertThat(state.getMode()).isEqualTo(CommunityGrantMode.ONLINE);
    }

    @Test
    void testGetIgnoresUnknownJsonFieldsInsteadOfFailing() {
        givenStored(settingsOf("{\"state\":\"AWAITING_SIGNUP\",\"futureField\":1}"));

        assertThat(store.get().getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
    }

    private AdminSettings settingsOf(CommunityGrantFlowState state) {
        return settingsOf(JacksonUtil.valueToTree(state));
    }

    private AdminSettings settingsOf(String json) {
        return settingsOf(JacksonUtil.toJsonNode(json));
    }

    private AdminSettings settingsOf(JsonNode jsonValue) {
        AdminSettings settings = new AdminSettings();
        settings.setTenantId(TenantId.SYS_TENANT_ID);
        settings.setKey(CommunityGrantFlowStateStore.SETTINGS_KEY);
        settings.setJsonValue(jsonValue);
        return settings;
    }

    private void givenStored(AdminSettings settings) {
        when(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, CommunityGrantFlowStateStore.SETTINGS_KEY))
                .thenReturn(settings);
    }

}
