// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rest.client;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rest.client.utils.RestJsonConverter;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.FeaturesInfo;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbSecretDeleteResult;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.UsageInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserEmailInfo;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentUpgradeRequest;
import org.thingsboard.server.common.data.agent.AgentAppInstallResponse;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldInfo;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.IntervalType;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestPreview;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.UserNotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.permission.AllowedPermissionsInfo;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.ShareGroupRequest;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.AvailableEntityKeys;
import org.thingsboard.server.common.data.query.AvailableEntityKeysV2;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.DefaultRuleChainCreateRequest;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SignUpSelfRegistrationParams;
import org.thingsboard.server.common.data.signup.SignUpRequest;
import org.thingsboard.server.common.data.signup.SignUpResult;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.thingsboard.server.common.data.sync.vc.AutoCommitSettings;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityDataDiff;
import org.thingsboard.server.common.data.sync.vc.EntityDataInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static org.thingsboard.server.common.data.StringUtils.isEmpty;

public class RestClient implements Closeable {

    private static final String TOKEN_HEADER_PARAM = "X-Authorization";
    private static final long AVG_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    protected static final String ACTIVATE_TOKEN_REGEX = "/api/noauth/activate?activateToken=";
    private final LazyInitializer<ExecutorService> executor = LazyInitializer.<ExecutorService>builder()
            .setInitializer(() -> ThingsBoardExecutors.newWorkStealingPool(10, getClass()))
            .get();
    @Getter
    protected final RestTemplate restTemplate;
    protected final RestTemplate loginRestTemplate;
    protected final String baseURL;

    private String username;
    private String password;
    private String mainToken;
    @Getter
    private String refreshToken;
    private long mainTokenExpTs;
    private long refreshTokenExpTs;
    private long clientServerTimeDiff;

    public enum AuthType {JWT, API_KEY}

    public RestClient(String baseURL) {
        this(new RestTemplate(), baseURL);
    }

    public RestClient(RestTemplate restTemplate, String baseURL) {
        this(restTemplate, baseURL, AuthType.JWT, null);
    }

    public RestClient(RestTemplate restTemplate, String baseURL, String accessToken) {
        this(restTemplate, baseURL, AuthType.JWT, accessToken);
    }

    public RestClient(RestTemplate restTemplate, String baseURL, AuthType authType, String token) {
        this.restTemplate = restTemplate;
        this.loginRestTemplate = new RestTemplate(restTemplate.getRequestFactory());
        this.baseURL = baseURL;
        this.restTemplate.getInterceptors().add((request, bytes, execution) -> {
            HttpRequest wrapper = new HttpRequestWrapper(request);
            switch (authType) {
                case JWT -> {
                    if (token == null) {
                        long calculatedTs = System.currentTimeMillis() + clientServerTimeDiff + AVG_REQUEST_TIMEOUT;
                        if (calculatedTs > mainTokenExpTs) {
                            synchronized (RestClient.this) {
                                if (calculatedTs > mainTokenExpTs) {
                                    if (calculatedTs < refreshTokenExpTs) {
                                        refreshToken();
                                    } else {
                                        doLogin();
                                    }
                                }
                            }
                        }
                    } else {
                        mainToken = token;
                    }
                    wrapper.getHeaders().set(TOKEN_HEADER_PARAM, "Bearer " + mainToken);
                }
                case API_KEY -> {
                    wrapper.getHeaders().set(TOKEN_HEADER_PARAM, "ApiKey " + token);
                }
            }
            return execution.execute(wrapper, bytes);
        });
    }

    public static RestClient withApiKey(String baseURL, String token) {
        return withApiKey(new RestTemplate(), baseURL, token);
    }

    public static RestClient withApiKey(RestTemplate rt, String baseURL, String token) {
        return new RestClient(rt, baseURL, AuthType.API_KEY, token);
    }

    public String getToken() {
        return mainToken;
    }

    public void refreshToken() {
        Map<String, String> refreshTokenRequest = new HashMap<>();
        refreshTokenRequest.put("refreshToken", refreshToken);
        long ts = System.currentTimeMillis();
        ResponseEntity<JsonNode> tokenInfo = loginRestTemplate.postForEntity(baseURL + "/api/auth/token", refreshTokenRequest, JsonNode.class);
        setTokenInfo(ts, tokenInfo.getBody());
    }

    public void login(String username, String password) {
        this.username = username;
        this.password = password;
        doLogin();
    }

    private void doLogin() {
        long ts = System.currentTimeMillis();
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);
        ResponseEntity<JsonNode> tokenInfo = loginRestTemplate.postForEntity(baseURL + "/api/auth/login", loginRequest, JsonNode.class);
        setTokenInfo(ts, tokenInfo.getBody());
    }

    private synchronized void setTokenInfo(long ts, JsonNode tokenInfo) {
        this.mainToken = tokenInfo.get("token").asText();
        this.refreshToken = tokenInfo.get("refreshToken").asText();
        this.mainTokenExpTs = JWT.decode(this.mainToken).getExpiresAtAsInstant().toEpochMilli();
        this.refreshTokenExpTs = JWT.decode(refreshToken).getExpiresAtAsInstant().toEpochMilli();
        this.clientServerTimeDiff = JWT.decode(this.mainToken).getIssuedAtAsInstant().toEpochMilli() - ts;
    }

    public Optional<AdminSettings> getAdminSettings(String key) {
        try {
            ResponseEntity<AdminSettings> adminSettings = restTemplate.getForEntity(baseURL + "/api/admin/settings/{key}", AdminSettings.class, key);
            return Optional.ofNullable(adminSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public AdminSettings saveAdminSettings(AdminSettings adminSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/settings", adminSettings, AdminSettings.class).getBody();
    }

    public void sendTestMail(AdminSettings adminSettings) {
        restTemplate.postForLocation(baseURL + "/api/admin/settings/testMail", adminSettings);
    }

    public void sendTestSms(TestSmsRequest testSmsRequest) {
        restTemplate.postForLocation(baseURL + "/api/admin/settings/testSms", testSmsRequest);
    }

    public Optional<SecuritySettings> getSecuritySettings() {
        try {
            ResponseEntity<SecuritySettings> securitySettings = restTemplate.getForEntity(baseURL + "/api/admin/securitySettings", SecuritySettings.class);
            return Optional.ofNullable(securitySettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public SecuritySettings saveSecuritySettings(SecuritySettings securitySettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/securitySettings", securitySettings, SecuritySettings.class).getBody();
    }

    public Optional<JwtSettings> getJwtSettings() {
        try {
            ResponseEntity<JwtSettings> jwtSettings = restTemplate.getForEntity(baseURL + "/api/admin/jwtSettings", JwtSettings.class);
            return Optional.ofNullable(jwtSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public JwtPair saveJwtSettings(JwtSettings jwtSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/jwtSettings", jwtSettings, JwtPair.class).getBody();
    }

    public Optional<RepositorySettings> getRepositorySettings() {
        try {
            ResponseEntity<RepositorySettings> repositorySettings = restTemplate.getForEntity(baseURL + "/api/admin/repositorySettings", RepositorySettings.class);
            return Optional.ofNullable(repositorySettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Boolean repositorySettingsExists() {
        return restTemplate.getForEntity(baseURL + "/api/admin/repositorySettings/exists", Boolean.class).getBody();
    }

    public RepositorySettings saveRepositorySettings(RepositorySettings repositorySettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/repositorySettings", repositorySettings, RepositorySettings.class).getBody();
    }

    public void deleteRepositorySettings() {
        restTemplate.delete(baseURL + "/api/admin/repositorySettings");
    }

    public void checkRepositoryAccess(RepositorySettings repositorySettings) {
        restTemplate.postForLocation(baseURL + "/api/admin/repositorySettings/checkAccess", repositorySettings);
    }

    public Optional<AutoCommitSettings> getAutoCommitSettings() {
        try {
            ResponseEntity<AutoCommitSettings> autoCommitSettings = restTemplate.getForEntity(baseURL + "/api/admin/autoCommitSettings", AutoCommitSettings.class);
            return Optional.ofNullable(autoCommitSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Boolean autoCommitSettingsExists() {
        return restTemplate.getForEntity(baseURL + "/api/admin/autoCommitSettings/exists", Boolean.class).getBody();
    }

    public AutoCommitSettings saveAutoCommitSettings(AutoCommitSettings autoCommitSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/autoCommitSettings", autoCommitSettings, AutoCommitSettings.class).getBody();
    }

    public void deleteAutoCommitSettings() {
        restTemplate.delete(baseURL + "/api/admin/autoCommitSettings");
    }

    public Optional<UpdateMessage> checkUpdates() {
        try {
            ResponseEntity<UpdateMessage> updateMsg = restTemplate.getForEntity(baseURL + "/api/admin/updates", UpdateMessage.class);
            return Optional.ofNullable(updateMsg.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public ObjectNode getSubscriptionPlan() {
        ResponseEntity<ObjectNode> updateMsg = restTemplate.getForEntity(baseURL + "/api/admin/subscriptionPlan", ObjectNode.class);
        return updateMsg.getBody();
    }

    public SystemInfo getSystemInfo() {
        return restTemplate.getForEntity(baseURL + "/api/admin/systemInfo", SystemInfo.class).getBody();
    }

    public FeaturesInfo getFeaturesInfo() {
        return restTemplate.getForEntity(baseURL + "/api/admin/featuresInfo", FeaturesInfo.class).getBody();
    }

    public Optional<Alarm> getAlarmById(AlarmId alarmId) {
        try {
            ResponseEntity<Alarm> alarm = restTemplate.getForEntity(baseURL + "/api/alarm/{alarmId}", Alarm.class, alarmId.getId());
            return Optional.ofNullable(alarm.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<AlarmInfo> getAlarmInfoById(AlarmId alarmId) {
        try {
            ResponseEntity<AlarmInfo> alarmInfo = restTemplate.getForEntity(baseURL + "/api/alarm/info/{alarmId}", AlarmInfo.class, alarmId.getId());
            return Optional.ofNullable(alarmInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Alarm saveAlarm(Alarm alarm) {
        return restTemplate.postForEntity(baseURL + "/api/alarm", alarm, Alarm.class).getBody();
    }

    public void deleteAlarm(AlarmId alarmId) {
        restTemplate.delete(baseURL + "/api/alarm/{alarmId}", alarmId.getId());
    }

    public void ackAlarm(AlarmId alarmId) {
        restTemplate.postForLocation(baseURL + "/api/alarm/{alarmId}/ack", null, alarmId.getId());
    }

    public void clearAlarm(AlarmId alarmId) {
        restTemplate.postForLocation(baseURL + "/api/alarm/{alarmId}/clear", null, alarmId.getId());
    }

    public void assignAlarm(AlarmId alarmId, UserId userId) {
        restTemplate.postForLocation(baseURL + "/api/alarm/{alarmId}/assign/{userId}", null, alarmId.getId(), userId.getId());
    }

    public void unassignAlarm(AlarmId alarmId) {
        restTemplate.delete(baseURL + "/api/alarm/{alarmId}/assign", alarmId.getId());
    }

    public PageData<AlarmInfo> getAlarms(EntityId entityId, AlarmSearchStatus searchStatus, AlarmStatus status, TimePageLink pageLink, Boolean fetchOriginator) {
        String urlSecondPart = "/api/alarm/{entityType}/{entityId}?fetchOriginator={fetchOriginator}";
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("fetchOriginator", String.valueOf(fetchOriginator));
        if (searchStatus != null) {
            params.put("searchStatus", searchStatus.name());
            urlSecondPart += "&searchStatus={searchStatus}";
        }
        if (status != null) {
            params.put("status", status.name());
            urlSecondPart += "&status={status}";
        }

        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + urlSecondPart + "&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AlarmInfo>>() {
                },
                params).getBody();
    }

    public PageData<AlarmInfo> getAllAlarms(AlarmSearchStatus searchStatus, AlarmStatus status, TimePageLink pageLink, Boolean fetchOriginator) {
        String urlSecondPart = "/api/alarms?";
        Map<String, String> params = new HashMap<>();
        if (fetchOriginator != null) {
            params.put("fetchOriginator", String.valueOf(fetchOriginator));
            urlSecondPart += "&fetchOriginator={fetchOriginator}";
        }
        if (searchStatus != null) {
            params.put("searchStatus", searchStatus.name());
            urlSecondPart += "&searchStatus={searchStatus}";
        }
        if (status != null) {
            params.put("status", status.name());
            urlSecondPart += "&status={status}";
        }

        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + urlSecondPart + "&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AlarmInfo>>() {
                },
                params).getBody();
    }

    public PageData<AlarmInfo> getAlarmsV2(EntityId entityId, List<AlarmSearchStatus> statusList, List<AlarmSeverity> severityList,
                                           List<String> typeList, String assignedId, TimePageLink pageLink) {
        String urlSecondPart = "/api/v2/alarm/{entityType}/{entityId}?";
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        if (!CollectionUtils.isEmpty(statusList)) {
            params.put("statusList", listEnumToString(statusList));
            urlSecondPart += "&statusList={statusList}";
        }
        if (!CollectionUtils.isEmpty(severityList)) {
            params.put("severityList", listEnumToString(severityList));
            urlSecondPart += "&severityList={severityList}";
        }
        if (!CollectionUtils.isEmpty(typeList)) {
            params.put("typeList", String.join(",", typeList));
            urlSecondPart += "&typeList={typeList}";
        }
        if (assignedId != null) {
            params.put("assignedId", assignedId);
            urlSecondPart += "&assignedId={assignedId}";
        }

        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + urlSecondPart + "&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AlarmInfo>>() {
                },
                params).getBody();
    }

    public PageData<AlarmInfo> getAllAlarmsV2(List<AlarmSearchStatus> statusList, List<AlarmSeverity> severityList,
                                              List<String> typeList, String assignedId, TimePageLink pageLink) {
        String urlSecondPart = "/api/v2/alarms?";
        Map<String, String> params = new HashMap<>();
        if (!CollectionUtils.isEmpty(statusList)) {
            params.put("statusList", listEnumToString(statusList));
            urlSecondPart += "&statusList={statusList}";
        }
        if (!CollectionUtils.isEmpty(severityList)) {
            params.put("severityList", listEnumToString(severityList));
            urlSecondPart += "&severityList={severityList}";
        }
        if (!CollectionUtils.isEmpty(typeList)) {
            params.put("typeList", String.join(",", typeList));
            urlSecondPart += "&typeList={typeList}";
        }
        if (assignedId != null) {
            params.put("assignedId", assignedId);
            urlSecondPart += "&assignedId={assignedId}";
        }

        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + urlSecondPart + "&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AlarmInfo>>() {
                },
                params).getBody();
    }

    public Optional<AlarmSeverity> getHighestAlarmSeverity(EntityId entityId, AlarmSearchStatus searchStatus, AlarmStatus status) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("searchStatus", searchStatus.name());
        params.put("status", status.name());
        try {
            ResponseEntity<AlarmSeverity> alarmSeverity = restTemplate.getForEntity(baseURL + "/api/alarm/highestSeverity/{entityType}/{entityId}?searchStatus={searchStatus}&status={status}", AlarmSeverity.class, params);
            return Optional.ofNullable(alarmSeverity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public Alarm createAlarm(Alarm alarm) {
        return restTemplate.postForEntity(baseURL + "/api/alarm", alarm, Alarm.class).getBody();
    }

    public PageData<EntitySubtype> getAlarmTypes(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/alarm/types?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntitySubtype>>() {
                },
                params).getBody();
    }

    public AlarmComment saveAlarmComment(AlarmId alarmId, AlarmComment alarmComment) {
        return restTemplate.postForEntity(baseURL + "/api/alarm/{alarmId}/comment", alarmComment, AlarmComment.class, alarmId.getId()).getBody();
    }

    public void deleteAlarmComment(AlarmId alarmId, AlarmCommentId alarmCommentId) {
        restTemplate.delete(baseURL + "/api/alarm/{alarmId}/comment/{alarmCommentId}",
                alarmId.getId(), alarmCommentId.getId());
    }

    public PageData<AlarmCommentInfo> getAlarmComments(AlarmId alarmId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("alarmId", alarmId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/alarm/{alarmId}/comment?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AlarmCommentInfo>>() {
                },
                params).getBody();
    }

    public Optional<Asset> getAssetById(AssetId assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.getForEntity(baseURL + "/api/asset/{assetId}", Asset.class, assetId.getId());
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Asset saveAsset(Asset asset) {
        return saveAsset(asset, null);
    }

    public Asset saveAsset(Asset asset, EntityGroupId entityGroupId) {
        if (entityGroupId == null) {
            return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
        } else {
            return restTemplate.postForEntity(baseURL + "/api/asset?entityGroupId={entityGroupId}",
                    asset, Asset.class, entityGroupId.getId()).getBody();
        }
    }

    public void deleteAsset(AssetId assetId) {
        restTemplate.delete(baseURL + "/api/asset/{assetId}", assetId.getId());
    }

    public PageData<Asset> getTenantAssets(PageLink pageLink, String assetType) {
        Map<String, String> params = new HashMap<>();
        params.put("type", assetType);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<Asset>> assets = restTemplate.exchange(
                baseURL + "/api/tenant/assets?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                },
                params);
        return assets.getBody();
    }

    public Optional<Asset> getTenantAsset(String assetName) {
        try {
            ResponseEntity<Asset> asset = restTemplate.getForEntity(baseURL + "/api/tenant/assets?assetName={assetName}", Asset.class, assetName);
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Asset> getCustomerAssets(CustomerId customerId, PageLink pageLink, String assetType) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", assetType);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<Asset>> assets = restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/assets?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                },
                params);
        return assets.getBody();
    }

    public List<Asset> getAssetsByIds(List<AssetId> assetIds) {
        return restTemplate.exchange(
                        baseURL + "/api/assets?assetIds={assetIds}",
                        HttpMethod.GET,
                        HttpEntity.EMPTY,
                        new ParameterizedTypeReference<List<Asset>>() {
                        },
                        listIdsToString(assetIds))
                .getBody();
    }

    public List<Asset> findByQuery(AssetSearchQuery query) {
        return restTemplate.exchange(
                URI.create(baseURL + "/api/assets"),
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<Asset>>() {
                }).getBody();
    }

    @Deprecated(since = "3.6.2")
    public List<EntitySubtype> getAssetTypes() {
        return restTemplate.exchange(URI.create(
                        baseURL + "/api/asset/types"),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }).getBody();
    }

    public List<EntitySubtype> getAssetProfileNames(boolean activeOnly) {
        return restTemplate.exchange(
                baseURL + "/api/assetProfile/names?activeOnly={activeOnly}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }, activeOnly).getBody();
    }

    public BulkImportResult<Asset> processAssetsBulkImport(BulkImportRequest request) {
        return restTemplate.exchange(
                baseURL + "/api/asset/bulk_import",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<BulkImportResult<Asset>>() {
                }).getBody();
    }

    @Deprecated
    public Optional<Asset> findAsset(String name) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("assetName", name);
        try {
            ResponseEntity<Asset> assetEntity = restTemplate.getForEntity(baseURL + "/api/tenant/assets?assetName={assetName}", Asset.class, params);
            return Optional.of(assetEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public Asset createAsset(Asset asset) {
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    @Deprecated
    public Asset createAsset(String name, String type) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(type);
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    public PageData<AuditLog> getAuditLogsByCustomerId(CustomerId customerId, TimePageLink pageLink, List<ActionType> actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("actionTypes", listEnumToString(actionTypes));
        addTimePageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/api/audit/logs/customer/{customerId}?actionTypes={actionTypes}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    public PageData<AuditLog> getAuditLogsByUserId(UserId userId, TimePageLink pageLink, List<ActionType> actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId.getId().toString());
        params.put("actionTypes", listEnumToString(actionTypes));
        addTimePageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/api/audit/logs/user/{userId}?actionTypes={actionTypes}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    public PageData<AuditLog> getAuditLogsByEntityId(EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("actionTypes", listEnumToString(actionTypes));
        addTimePageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/api/audit/logs/entity/{entityType}/{entityId}?actionTypes={actionTypes}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    public PageData<AuditLog> getAuditLogs(TimePageLink pageLink, List<ActionType> actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("actionTypes", listEnumToString(actionTypes));
        addTimePageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/api/audit/logs?actionTypes={actionTypes}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    public String getActivateToken(UserId userId) {
        String activationLink = getActivationLink(userId);
        return activationLink.substring(activationLink.lastIndexOf(ACTIVATE_TOKEN_REGEX) + ACTIVATE_TOKEN_REGEX.length());
    }

    public Optional<User> getUser() {
        ResponseEntity<User> user = restTemplate.getForEntity(baseURL + "/api/auth/user", User.class);
        return Optional.ofNullable(user.getBody());
    }

    public void logout() {
        restTemplate.postForLocation(baseURL + "/api/auth/logout", null);
    }

    public void changePassword(String currentPassword, String newPassword) {
        ObjectNode changePasswordRequest = JacksonUtil.newObjectNode();
        changePasswordRequest.put("currentPassword", currentPassword);
        changePasswordRequest.put("newPassword", newPassword);
        restTemplate.postForLocation(baseURL + "/api/auth/changePassword", changePasswordRequest);
    }

    public Optional<UserPasswordPolicy> getUserPasswordPolicy() {
        try {
            ResponseEntity<UserPasswordPolicy> userPasswordPolicy = restTemplate.getForEntity(baseURL + "/api/noauth/userPasswordPolicy", UserPasswordPolicy.class);
            return Optional.ofNullable(userPasswordPolicy.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public ResponseEntity<String> checkActivateToken(UserId userId) {
        String activateToken = getActivateToken(userId);
        return restTemplate.getForEntity(baseURL + "/api/noauth/activate?activateToken={activateToken}", String.class, activateToken);
    }

    public void requestResetPasswordByEmail(String email) {
        ObjectNode resetPasswordByEmailRequest = JacksonUtil.newObjectNode();
        resetPasswordByEmailRequest.put("email", email);
        restTemplate.postForLocation(baseURL + "/api/noauth/resetPasswordByEmail", resetPasswordByEmailRequest);
    }

    public Optional<JsonNode> activateUser(UserId userId, String password) {
        return activateUser(userId, password, true);
    }

    public Optional<JsonNode> activateUser(UserId userId, String password, boolean sendActivationMail) {
        ObjectNode activateRequest = JacksonUtil.newObjectNode();
        activateRequest.put("activateToken", getActivateToken(userId));
        activateRequest.put("password", password);
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/noauth/activate?sendActivationMail={sendActivationMail}", activateRequest, JsonNode.class, sendActivationMail);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<ComponentDescriptor> getComponentDescriptorByClazz(String componentDescriptorClazz) {
        try {
            ResponseEntity<ComponentDescriptor> componentDescriptor = restTemplate.getForEntity(baseURL + "/api/component/{componentDescriptorClazz}", ComponentDescriptor.class, componentDescriptorClazz);
            return Optional.ofNullable(componentDescriptor.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<ComponentDescriptor> getComponentDescriptorsByType(ComponentType componentType) {
        return getComponentDescriptorsByType(componentType, RuleChainType.CORE);
    }

    public List<ComponentDescriptor> getComponentDescriptorsByType(ComponentType componentType, RuleChainType ruleChainType) {
        return restTemplate.exchange(
                baseURL + "/api/components/" + componentType.name() + "/?ruleChainType={ruleChainType}",
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<ComponentDescriptor>>() {
                },
                ruleChainType).getBody();
    }

    public List<ComponentDescriptor> getComponentDescriptorsByTypes(List<ComponentType> componentTypes) {
        return getComponentDescriptorsByTypes(componentTypes, RuleChainType.CORE);
    }

    public List<ComponentDescriptor> getComponentDescriptorsByTypes(List<ComponentType> componentTypes, RuleChainType ruleChainType) {
        return restTemplate.exchange(
                        baseURL + "/api/components?componentTypes={componentTypes}&ruleChainType={ruleChainType}",
                        HttpMethod.GET,
                        HttpEntity.EMPTY,
                        new ParameterizedTypeReference<List<ComponentDescriptor>>() {
                        },
                        listEnumToString(componentTypes),
                        ruleChainType)
                .getBody();
    }

    public Optional<Customer> getCustomerById(CustomerId customerId) {
        try {
            ResponseEntity<Customer> customer = restTemplate.getForEntity(baseURL + "/api/customer/{customerId}", Customer.class, customerId.getId());
            return Optional.ofNullable(customer.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> getShortCustomerInfoById(CustomerId customerId) {
        try {
            ResponseEntity<JsonNode> customerInfo = restTemplate.getForEntity(baseURL + "/api/customer/{customerId}/shortInfo", JsonNode.class, customerId.getId());
            return Optional.ofNullable(customerInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public String getCustomerTitleById(CustomerId customerId) {
        return restTemplate.getForObject(baseURL + "/api/customer/{customerId}/title", String.class, customerId.getId());
    }

    public Customer saveCustomer(Customer customer) {
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    public void deleteCustomer(CustomerId customerId) {
        restTemplate.delete(baseURL + "/api/customer/{customerId}", customerId.getId());
    }

    public PageData<Customer> getCustomers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<Customer>> customer = restTemplate.exchange(
                baseURL + "/api/customers?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Customer>>() {
                },
                params);
        return customer.getBody();
    }

    public Optional<Customer> getTenantCustomer(String customerTitle) {
        try {
            ResponseEntity<Customer> customer = restTemplate.getForEntity(baseURL + "/api/tenant/customers?customerTitle={customerTitle}", Customer.class, customerTitle);
            return Optional.ofNullable(customer.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public Optional<Customer> findCustomer(String title) {
        Map<String, String> params = new HashMap<>();
        params.put("customerTitle", title);
        try {
            ResponseEntity<Customer> customerEntity = restTemplate.getForEntity(baseURL + "/api/tenant/customers?customerTitle={customerTitle}", Customer.class, params);
            return Optional.of(customerEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public Customer createCustomer(Customer customer) {
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    @Deprecated
    public Customer createCustomer(String title) {
        Customer customer = new Customer();
        customer.setTitle(title);
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    public Long getServerTime() {
        return restTemplate.getForObject(baseURL + "/api/dashboard/serverTime", Long.class);
    }

    public Long getMaxDatapointsLimit() {
        return restTemplate.getForObject(baseURL + "/api/dashboard/maxDatapointsLimit", Long.class);
    }

    public Optional<DashboardInfo> getDashboardInfoById(DashboardId dashboardId) {
        try {
            ResponseEntity<DashboardInfo> dashboardInfo = restTemplate.getForEntity(baseURL + "/api/dashboard/info/{dashboardId}", DashboardInfo.class, dashboardId.getId());
            return Optional.ofNullable(dashboardInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Dashboard> getDashboardById(DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.getForEntity(baseURL + "/api/dashboard/{dashboardId}", Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Dashboard saveDashboard(Dashboard dashboard) {
        return saveDashboard(dashboard, null);
    }

    public Dashboard saveDashboard(Dashboard dashboard, EntityGroupId entityGroupId) {
        if (entityGroupId == null) {
            return restTemplate.postForEntity(baseURL + "/api/dashboard", dashboard, Dashboard.class).getBody();
        } else {
            return restTemplate.postForEntity(baseURL + "/api/dashboard?entityGroupId={entityGroupId}",
                    dashboard, Dashboard.class, entityGroupId.getId()).getBody();
        }
    }

    public void deleteDashboard(DashboardId dashboardId) {
        restTemplate.delete(baseURL + "/api/dashboard/{dashboardId}", dashboardId.getId());
    }

    public PageData<DashboardInfo> getTenantDashboards(TenantId tenantId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("tenantId", tenantId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/{tenantId}/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                }, params).getBody();
    }

    public PageData<DashboardInfo> getTenantDashboards(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                }, params).getBody();
    }

    @Deprecated
    public Dashboard createDashboard(Dashboard dashboard) {
        return restTemplate.postForEntity(baseURL + "/api/dashboard", dashboard, Dashboard.class).getBody();
    }

    public Optional<Device> getDeviceById(DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.getForEntity(baseURL + "/api/device/{deviceId}", Device.class, deviceId.getId());
            return Optional.ofNullable(device.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Device saveDevice(Device device) {
        return saveDevice(device, null);
    }

    public Device saveDevice(Device device, String accessToken) {
        return saveDevice(device, accessToken, null);
    }

    public Device saveDevice(Device device, String accessToken, EntityGroupId entityGroupId) {
        if (entityGroupId == null) {
            return restTemplate.postForEntity(baseURL + "/api/device?accessToken={accessToken}", device, Device.class, accessToken).getBody();
        } else {
            return restTemplate.postForEntity(baseURL + "/api/device?accessToken={accessToken}&entityGroupId={entityGroupId}",
                    device, Device.class, accessToken, entityGroupId.getId()).getBody();
        }
    }

    public void deleteDevice(DeviceId deviceId) {
        restTemplate.delete(baseURL + "/api/device/{deviceId}", deviceId.getId());
    }

    public Optional<DeviceCredentials> getDeviceCredentialsByDeviceId(DeviceId deviceId) {
        try {
            ResponseEntity<DeviceCredentials> deviceCredentials = restTemplate.getForEntity(baseURL + "/api/device/{deviceId}/credentials", DeviceCredentials.class, deviceId.getId());
            return Optional.ofNullable(deviceCredentials.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public DeviceCredentials saveDeviceCredentials(DeviceCredentials deviceCredentials) {
        return restTemplate.postForEntity(baseURL + "/api/device/credentials", deviceCredentials, DeviceCredentials.class).getBody();
    }

    public Optional<Device> saveDeviceWithCredentials(Device device, DeviceCredentials credentials) {
        try {
            SaveDeviceWithCredentialsRequest request = new SaveDeviceWithCredentialsRequest(device, credentials);
            ResponseEntity<Device> deviceOpt = restTemplate.postForEntity(baseURL + "/api/device-with-credentials", request, Device.class);
            return Optional.ofNullable(deviceOpt.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Device> getTenantDevices(String type, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/devices?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                }, params).getBody();
    }

    public Optional<Device> getTenantDevice(String deviceName) {
        try {
            ResponseEntity<Device> device = restTemplate.getForEntity(baseURL + "/api/tenant/devices?deviceName={deviceName}", Device.class, deviceName);
            return Optional.ofNullable(device.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Device> getCustomerDevices(CustomerId customerId, String deviceType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", deviceType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/devices?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                }, params).getBody();
    }

    public List<Device> getDevicesByIds(List<DeviceId> deviceIds) {
        return restTemplate.exchange(baseURL + "/api/devices?deviceIds={deviceIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY, new ParameterizedTypeReference<List<Device>>() {
                }, listIdsToString(deviceIds)).getBody();
    }

    public List<Device> findByQuery(DeviceSearchQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/devices",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<Device>>() {
                }).getBody();
    }

    @Deprecated(since = "3.6.2")
    public List<EntitySubtype> getDeviceTypes() {
        return restTemplate.exchange(
                baseURL + "/api/device/types",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }).getBody();
    }

    public List<EntitySubtype> getDeviceProfileNames(boolean activeOnly) {
        return restTemplate.exchange(
                baseURL + "/api/deviceProfile/names?activeOnly={activeOnly}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }, activeOnly).getBody();
    }

    public List<DeviceProfileInfo> getDeviceProfileInfosByIds(Set<UUID> ids) {
        URIBuilder builder;
        try {
            builder = new URIBuilder(baseURL);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid base URL: " + baseURL, e);
        }

        builder.appendPath("/api/deviceProfileInfos");

        String commaSeparatedIds = ids.stream()
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .collect(joining(","));

        builder.addParameter("deviceProfileIds", commaSeparatedIds);

        URI uri;
        try {
            uri = builder.build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to construct API URI from base URL and provided params", e);
        }

        return restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<List<DeviceProfileInfo>>() {}).getBody();
    }

    public JsonNode claimDevice(String deviceName, ClaimRequest claimRequest) {
        return restTemplate.exchange(
                baseURL + "/api/customer/device/{deviceName}/claim",
                HttpMethod.POST,
                new HttpEntity<>(claimRequest),
                new ParameterizedTypeReference<JsonNode>() {
                }, deviceName).getBody();
    }

    public void reClaimDevice(String deviceName) {
        restTemplate.delete(baseURL + "/api/customer/device/{deviceName}/claim", deviceName);
    }

    public Device assignDeviceToTenant(TenantId tenantId, DeviceId deviceId) {
        return restTemplate.postForEntity(
                baseURL + "/api/tenant/{tenantId}/device/{deviceId}",
                HttpEntity.EMPTY, Device.class, tenantId, deviceId).getBody();
    }

    public Long countByDeviceProfileAndEmptyOtaPackage(OtaPackageType otaPackageType, DeviceProfileId deviceProfileId) {
        Map<String, String> params = new HashMap<>();
        params.put("otaPackageType", otaPackageType.name());
        params.put("deviceProfileId", deviceProfileId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/devices/count/{otaPackageType}/{deviceProfileId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Long>() {
                },
                params
        ).getBody();
    }

    public BulkImportResult<Device> processDevicesBulkImport(BulkImportRequest request) {
        return restTemplate.exchange(
                baseURL + "/api/device/bulk_import",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<BulkImportResult<Device>>() {
                }).getBody();
    }

    @Deprecated
    public Device createDevice(String name, String type) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        return doCreateDevice(device, null);
    }

    @Deprecated
    public Device createDevice(Device device) {
        return doCreateDevice(device, null);
    }

    @Deprecated
    public Device createDevice(Device device, String accessToken) {
        return doCreateDevice(device, accessToken);
    }

    @Deprecated
    private Device doCreateDevice(Device device, String accessToken) {
        Map<String, String> params = new HashMap<>();
        String deviceCreationUrl = "/api/device";
        if (!StringUtils.isEmpty(accessToken)) {
            deviceCreationUrl = deviceCreationUrl + "?accessToken={accessToken}";
            params.put("accessToken", accessToken);
        }
        return restTemplate.postForEntity(baseURL + deviceCreationUrl, device, Device.class, params).getBody();
    }

    @Deprecated
    public DeviceCredentials getCredentials(DeviceId id) {
        return restTemplate.getForEntity(baseURL + "/api/device/" + id.getId().toString() + "/credentials", DeviceCredentials.class).getBody();
    }

    @Deprecated
    public Optional<Device> findDevice(String name) {
        Map<String, String> params = new HashMap<>();
        params.put("deviceName", name);
        try {
            ResponseEntity<Device> deviceEntity = restTemplate.getForEntity(baseURL + "/api/tenant/devices?deviceName={deviceName}", Device.class, params);
            return Optional.of(deviceEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public DeviceCredentials updateDeviceCredentials(DeviceId deviceId, String token) {
        DeviceCredentials deviceCredentials = getCredentials(deviceId);
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(token);
        return saveDeviceCredentials(deviceCredentials);
    }

    public Optional<DeviceProfile> getDeviceProfileById(DeviceProfileId deviceProfileId) {
        try {
            ResponseEntity<DeviceProfile> deviceProfile = restTemplate.getForEntity(baseURL + "/api/deviceProfile/{deviceProfileId}", DeviceProfile.class, deviceProfileId);
            return Optional.ofNullable(deviceProfile.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<DeviceProfileInfo> getDeviceProfileInfoById(DeviceProfileId deviceProfileId) {
        try {
            ResponseEntity<DeviceProfileInfo> deviceProfileInfo = restTemplate.getForEntity(baseURL + "/api/deviceProfileInfo/{deviceProfileId}", DeviceProfileInfo.class, deviceProfileId);
            return Optional.ofNullable(deviceProfileInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public DeviceProfileInfo getDefaultDeviceProfileInfo() {
        return restTemplate.getForEntity(baseURL + "/api/deviceProfileInfo/default", DeviceProfileInfo.class).getBody();
    }

    public DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile) {
        return restTemplate.postForEntity(baseURL + "/api/deviceProfile", deviceProfile, DeviceProfile.class).getBody();
    }

    public void deleteDeviceProfile(DeviceProfileId deviceProfileId) {
        restTemplate.delete(baseURL + "/api/deviceProfile/{deviceProfileId}", deviceProfileId);
    }

    public DeviceProfile setDefaultDeviceProfile(DeviceProfileId deviceProfileId) {
        return restTemplate.postForEntity(
                baseURL + "/api/deviceProfile/{deviceProfileId}/default",
                HttpEntity.EMPTY, DeviceProfile.class, deviceProfileId).getBody();
    }

    public PageData<DeviceProfile> getDeviceProfiles(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/deviceProfiles?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DeviceProfile>>() {
                }, params).getBody();
    }

    public PageData<DeviceProfileInfo> getDeviceProfileInfos(PageLink pageLink, DeviceTransportType deviceTransportType) {
        Map<String, String> params = new HashMap<>();
        params.put("deviceTransportType", deviceTransportType != null ? deviceTransportType.name() : null);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/deviceProfileInfos?deviceTransportType={deviceTransportType}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DeviceProfileInfo>>() {
                }, params).getBody();
    }

    public Optional<AssetProfile> getAssetProfileById(AssetProfileId assetProfileId) {
        try {
            ResponseEntity<AssetProfile> assetProfile = restTemplate.getForEntity(baseURL + "/api/assetProfile/{assetProfileId}", AssetProfile.class, assetProfileId);
            return Optional.ofNullable(assetProfile.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<AssetProfileInfo> getAssetProfileInfoById(AssetProfileId assetProfileId) {
        try {
            ResponseEntity<AssetProfileInfo> assetProfileInfo = restTemplate.getForEntity(baseURL + "/api/assetProfileInfo/{assetProfileId}", AssetProfileInfo.class, assetProfileId);
            return Optional.ofNullable(assetProfileInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public AssetProfileInfo getDefaultAssetProfileInfo() {
        return restTemplate.getForEntity(baseURL + "/api/assetProfileInfo/default", AssetProfileInfo.class).getBody();
    }

    public AssetProfile saveAssetProfile(AssetProfile assetProfile) {
        return restTemplate.postForEntity(baseURL + "/api/assetProfile", assetProfile, AssetProfile.class).getBody();
    }

    public void deleteAssetProfile(AssetProfileId assetProfileId) {
        restTemplate.delete(baseURL + "/api/assetProfile/{assetProfileId}", assetProfileId);
    }

    public AssetProfile setDefaultAssetProfile(AssetProfileId assetProfileId) {
        return restTemplate.postForEntity(
                baseURL + "/api/assetProfile/{assetProfileId}/default",
                HttpEntity.EMPTY, AssetProfile.class, assetProfileId).getBody();
    }

    public PageData<AssetProfile> getAssetProfiles(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/assetProfiles?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AssetProfile>>() {
                }, params).getBody();
    }

    public PageData<AssetProfileInfo> getAssetProfileInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/assetProfileInfos?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AssetProfileInfo>>() {
                }, params).getBody();
    }

    public List<AssetProfileInfo> getAssetProfilesByIds(Set<UUID> ids) {
        URIBuilder builder;
        try {
            builder = new URIBuilder(baseURL);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid base URL: " + baseURL, e);
        }

        builder.appendPath("/api/assetProfileInfos");

        String commaSeparatedIds = ids.stream()
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .collect(joining(","));

        builder.addParameter("assetProfileIds", commaSeparatedIds);

        URI uri;
        try {
            uri = builder.build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to construct API URI from base URL and provided params", e);
        }

        return restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<List<AssetProfileInfo>>() {}).getBody();
    }

    public Long countEntitiesByQuery(EntityCountQuery query) {
        return restTemplate.postForObject(baseURL + "/api/entitiesQuery/count", query, Long.class);
    }

    public PageData<EntityData> findEntityDataByQuery(EntityDataQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/entitiesQuery/find",
                HttpMethod.POST, new HttpEntity<>(query),
                new ParameterizedTypeReference<PageData<EntityData>>() {
                }).getBody();
    }

    /**
     * @deprecated Use {@link #findAvailableEntityKeysV2(EntityDataQuery, boolean, boolean, Set, boolean)} instead.
     */
    @Deprecated(forRemoval = true)
    public AvailableEntityKeys findAvailableEntityKeysByQuery(EntityDataQuery query, boolean includeTimeseries, boolean includeAttributes, AttributeScope scope) {
        var uri = UriComponentsBuilder.fromUriString(baseURL)
                .path("/api/entitiesQuery/find/keys")
                .queryParam("timeseries", includeTimeseries)
                .queryParam("attributes", includeAttributes)
                .queryParamIfPresent("scope", Optional.ofNullable(scope))
                .build()
                .toUri();
        return restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(query), new ParameterizedTypeReference<AvailableEntityKeys>() {}).getBody();
    }

    @SneakyThrows(URISyntaxException.class)
    public AvailableEntityKeysV2 findAvailableEntityKeysV2(
            EntityDataQuery query, boolean includeTimeseries, boolean includeAttributes, Set<AttributeScope> scopes, boolean includeSamples
    ) {
        var builder = new URIBuilder(baseURL).appendPath("/api/v2/entitiesQuery/find/keys")
                .addParameter("includeTimeseries", String.valueOf(includeTimeseries))
                .addParameter("includeAttributes", String.valueOf(includeAttributes))
                .addParameter("includeSamples", String.valueOf(includeSamples));
        if (scopes != null) {
            for (AttributeScope scope : scopes) {
                builder.addParameter("scopes", scope.name());
            }
        }
        return restTemplate.exchange(builder.build(), HttpMethod.POST, new HttpEntity<>(query), new ParameterizedTypeReference<AvailableEntityKeysV2>() {}).getBody();
    }

    public PageData<AlarmData> findAlarmDataByQuery(AlarmDataQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/alarmsQuery/find",
                HttpMethod.POST, new HttpEntity<>(query),
                new ParameterizedTypeReference<PageData<AlarmData>>() {
                }).getBody();
    }

    public Long countAlarmsByQuery(AlarmCountQuery query) {
        return restTemplate.postForObject(baseURL + "/api/alarmsQuery/count", query, Long.class);
    }

    public void saveRelation(EntityRelation relation) {
        restTemplate.postForLocation(baseURL + "/api/relation", relation);
    }

    public EntityRelation saveRelationV2(EntityRelation relation) {
        return restTemplate.postForEntity(baseURL + "/api/v2/relation", relation, EntityRelation.class).getBody();
    }

    public void deleteRelation(EntityId fromId, String relationType, RelationTypeGroup relationTypeGroup, EntityId toId) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        restTemplate.delete(baseURL + "/api/relation?fromId={fromId}&fromType={fromType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}&toId={toId}&toType={toType}", params);
    }

    public Optional<EntityRelation> deleteRelationV2(EntityId fromId, String relationType, RelationTypeGroup relationTypeGroup, EntityId toId) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        try {
            var relation = restTemplate.exchange(baseURL + "/api/relation?fromId={fromId}&fromType={fromType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}&toId={toId}&toType={toType}", HttpMethod.DELETE, HttpEntity.EMPTY, EntityRelation.class, params);
            return Optional.ofNullable(relation.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public void deleteRelations(EntityId entityId) {
        restTemplate.delete(baseURL + "/api/relations?entityId={entityId}&entityType={entityType}", entityId.getId().toString(), entityId.getEntityType().name());
    }

    public Optional<EntityRelation> getRelation(EntityId fromId, String relationType, RelationTypeGroup relationTypeGroup, EntityId toId) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());

        try {
            ResponseEntity<EntityRelation> entityRelation = restTemplate.getForEntity(
                    baseURL + "/api/relation?fromId={fromId}&fromType={fromType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}&toId={toId}&toType={toType}",
                    EntityRelation.class,
                    params);
            return Optional.ofNullable(entityRelation.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<EntityRelation> findByFrom(EntityId fromId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?fromId={fromId}&fromType={fromType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelation>>() {
                },
                params).getBody();
    }

    public List<EntityRelationInfo> findInfoByFrom(EntityId fromId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations/info?fromId={fromId}&fromType={fromType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelationInfo>>() {
                },
                params).getBody();
    }

    public List<EntityRelation> findByFrom(EntityId fromId, String relationType, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?fromId={fromId}&fromType={fromType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelation>>() {
                },
                params).getBody();
    }

    public List<EntityRelation> findByTo(EntityId toId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?toId={toId}&toType={toType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelation>>() {
                },
                params).getBody();
    }

    public List<EntityRelationInfo> findInfoByTo(EntityId toId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations/info?toId={toId}&toType={toType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelationInfo>>() {
                },
                params).getBody();
    }

    public List<EntityRelation> findByTo(EntityId toId, String relationType, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?toId={toId}&toType={toType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelation>>() {
                },
                params).getBody();
    }

    public List<EntityRelation> findByQuery(EntityRelationsQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/relations",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<EntityRelation>>() {
                }).getBody();
    }

    public List<EntityRelationInfo> findInfoByQuery(EntityRelationsQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/relations/info",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<EntityRelationInfo>>() {
                }).getBody();
    }

    @Deprecated
    public EntityRelation makeRelation(String relationType, EntityId idFrom, EntityId idTo) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(idFrom);
        relation.setTo(idTo);
        relation.setType(relationType);
        return restTemplate.postForEntity(baseURL + "/api/relation", relation, EntityRelation.class).getBody();
    }

    public Optional<EntityView> getEntityViewById(EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.getForEntity(baseURL + "/api/entityView/{entityViewId}", EntityView.class, entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public EntityView saveEntityView(EntityView entityView) {
        return saveEntityView(entityView, null);
    }

    public EntityView saveEntityView(EntityView entityView, EntityGroupId entityGroupId) {
        if (entityGroupId == null) {
            return restTemplate.postForEntity(baseURL + "/api/entityView", entityView, EntityView.class).getBody();
        } else {
            return restTemplate.postForEntity(baseURL + "/api/entityView?entityGroupId={entityGroupId}",
                    entityView, EntityView.class, entityGroupId.getId()).getBody();
        }
    }

    public void deleteEntityView(EntityViewId entityViewId) {
        restTemplate.delete(baseURL + "/api/entityView/{entityViewId}", entityViewId.getId());
    }

    public Optional<EntityView> getTenantEntityView(String entityViewName) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.getForEntity(baseURL + "/api/tenant/entityViews?entityViewName={entityViewName}", EntityView.class, entityViewName);
            return Optional.ofNullable(entityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<EntityView> getCustomerEntityViews(CustomerId customerId, String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", entityViewType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/entityViews?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                }, params).getBody();
    }

    public PageData<EntityView> getTenantEntityViews(String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", entityViewType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/entityViews?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                }, params).getBody();
    }

    public List<EntityView> findByQuery(EntityViewSearchQuery query) {
        return restTemplate.exchange(baseURL + "/api/entityViews", HttpMethod.POST, new HttpEntity<>(query), new ParameterizedTypeReference<List<EntityView>>() {
        }).getBody();
    }

    public List<EntitySubtype> getEntityViewTypes() {
        return restTemplate.exchange(baseURL + "/api/entityView/types", HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<List<EntitySubtype>>() {
        }).getBody();
    }

    public PageData<EventInfo> getEvents(EntityId entityId, EventType eventType, TenantId tenantId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("eventType", eventType.name());
        params.put("tenantId", tenantId.getId().toString());
        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/events/{entityType}/{entityId}/{eventType}?tenantId={tenantId}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EventInfo>>() {
                },
                params).getBody();
    }

    public PageData<EventInfo> getEvents(EntityId entityId, TenantId tenantId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("tenantId", tenantId.getId().toString());
        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/events/{entityType}/{entityId}?tenantId={tenantId}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EventInfo>>() {
                }, params).getBody();
    }

    public OAuth2ClientRegistrationTemplate saveClientRegistrationTemplate(OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        return restTemplate.postForEntity(baseURL + "/api/oauth2/config/template", clientRegistrationTemplate, OAuth2ClientRegistrationTemplate.class).getBody();
    }

    public void deleteClientRegistrationTemplate(OAuth2ClientRegistrationTemplateId oAuth2ClientRegistrationTemplateId) {
        restTemplate.delete(baseURL + "/api/oauth2/config/template/{clientRegistrationTemplateId}", oAuth2ClientRegistrationTemplateId);
    }

    public List<OAuth2ClientRegistrationTemplate> getClientRegistrationTemplates() {
        return restTemplate.exchange(
                baseURL + "/api/oauth2/config/template",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<OAuth2ClientRegistrationTemplate>>() {
                }).getBody();
    }

    public List<OAuth2ClientLoginInfo> getOAuth2Clients(String pkgName, PlatformType platformType) {
        Map<String, String> params = new HashMap<>();
        StringBuilder urlBuilder = new StringBuilder(baseURL);
        urlBuilder.append("/api/noauth/oauth2Clients");
        if (pkgName != null) {
            urlBuilder.append("?pkgName={pkgName}");
            params.put("pkgName", pkgName);
        }
        if (platformType != null) {
            if (pkgName != null) {
                urlBuilder.append("&");
            } else {
                urlBuilder.append("?");
            }
            urlBuilder.append("platform={platform}");
            params.put("platform", platformType.name());
        }
        return restTemplate.exchange(
                urlBuilder.toString(),
                HttpMethod.POST,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<OAuth2ClientLoginInfo>>() {
                }, params).getBody();
    }

    public PageData<OAuth2ClientInfo> getTenantOAuth2Clients() {
        return restTemplate.exchange(
                baseURL + "/api/oauth2/client/infos",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<OAuth2ClientInfo>>() {
                }).getBody();
    }

    public Optional<OAuth2Client> getOauth2ClientById(OAuth2ClientId oAuth2ClientId) {
        try {
            ResponseEntity<OAuth2Client> oauth2Client = restTemplate.getForEntity(baseURL + "/api/oauth2/client/{id}", OAuth2Client.class, oAuth2ClientId.getId());
            return Optional.ofNullable(oauth2Client.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public OAuth2Client saveOAuth2Client(OAuth2Client oAuth2Client) {
        return restTemplate.postForEntity(baseURL + "/api/oauth2/client", oAuth2Client, OAuth2Client.class).getBody();
    }

    public void deleteOauth2CLient(OAuth2ClientId oAuth2ClientId) {
        restTemplate.delete(baseURL + "/api/oauth2/client/{id}", oAuth2ClientId.getId());
    }

    public PageData<DomainInfo> getTenantDomainInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/domain/infos?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DomainInfo>>() {
                },
                params).getBody();
    }

    public Optional<DomainInfo> getDomainInfoById(DomainId domainId) {
        try {
            ResponseEntity<DomainInfo> domainInfo = restTemplate.getForEntity(baseURL + "/api/domain/info/{id}", DomainInfo.class, domainId.getId());
            return Optional.ofNullable(domainInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Domain saveDomain(Domain domain) {
        return restTemplate.postForEntity(baseURL + "/api/domain", domain, Domain.class).getBody();
    }

    public void deleteDomain(DomainId domainId) {
        restTemplate.delete(baseURL + "/api/domain/{id}", domainId.getId());
    }

    public void updateDomainOauth2Clients(DomainId domainId, UUID[] oauth2ClientIds) {
        restTemplate.postForLocation(baseURL + "/api/domain/{id}/oauth2Clients", oauth2ClientIds, domainId.getId());
    }

    public PageData<MobileApp> getTenantMobileApps(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/mobile/app?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<MobileApp>>() {
                },
                params).getBody();
    }

    public Optional<MobileApp> getMobileAppById(MobileAppId mobileAppId) {
        try {
            ResponseEntity<MobileApp> mobileApp = restTemplate.getForEntity(baseURL + "/api/mobile/app/{id}", MobileApp.class, mobileAppId.getId());
            return Optional.ofNullable(mobileApp.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public MobileApp saveMobileApp(MobileApp mobileApp) {
        return restTemplate.postForEntity(baseURL + "/api/mobile/app", mobileApp, MobileApp.class).getBody();
    }

    public void deleteMobileApp(MobileAppId mobileAppId) {
        restTemplate.delete(baseURL + "/api/mobile/app/{id}", mobileAppId.getId());
    }

    public PageData<MobileAppBundleInfo> getTenantMobileBundleInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/mobile/bundle/infos?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<MobileAppBundleInfo>>() {
                },
                params).getBody();
    }

    public Optional<MobileAppBundle> getMobileBundleById(MobileAppBundleId mobileAppBundleId) {
        try {
            ResponseEntity<MobileAppBundle> mobileApp = restTemplate.getForEntity(baseURL + "/api/mobile/bundle/{id}", MobileAppBundle.class, mobileAppBundleId.getId());
            return Optional.ofNullable(mobileApp.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public MobileAppBundle saveMobileBundle(MobileAppBundle mobileAppBundle) {
        return restTemplate.postForEntity(baseURL + "/api/mobile/bundle", mobileAppBundle, MobileAppBundle.class).getBody();
    }

    public void deleteMobileBundle(MobileAppBundleId mobileAppBundleId) {
        restTemplate.delete(baseURL + "/api/mobile/bundle/{id}", mobileAppBundleId.getId());
    }

    public void updateMobileAppBundleOauth2Clients(MobileAppBundleId mobileAppBundleId, UUID[] oauth2ClientIds) {
        restTemplate.postForLocation(baseURL + "/api/mobile/bundle/{id}/oauth2Clients", oauth2ClientIds, mobileAppBundleId.getId());
    }

    public String getLoginProcessingUrl() {
        return restTemplate.getForEntity(baseURL + "/api/oauth2/loginProcessingUrl", String.class).getBody();
    }

    public void handleOneWayDeviceRPCRequest(DeviceId deviceId, JsonNode requestBody) {
        restTemplate.postForLocation(baseURL + "/api/rpc/oneway/{deviceId}", requestBody, deviceId.getId());
    }

    public JsonNode handleTwoWayDeviceRPCRequest(DeviceId deviceId, JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rpc/twoway/{deviceId}",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                },
                deviceId.getId()).getBody();
    }

    public Optional<RuleChain> getRuleChainById(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.getForEntity(baseURL + "/api/ruleChain/{ruleChainId}", RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<RuleChainMetaData> getRuleChainMetaData(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChainMetaData> ruleChainMetaData = restTemplate.getForEntity(baseURL + "/api/ruleChain/{ruleChainId}/metadata", RuleChainMetaData.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChainMetaData.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public RuleChain saveRuleChain(RuleChain ruleChain) {
        return restTemplate.postForEntity(baseURL + "/api/ruleChain", ruleChain, RuleChain.class).getBody();
    }

    public RuleChain saveRuleChain(DefaultRuleChainCreateRequest request) {
        return restTemplate.postForEntity(baseURL + "/api/ruleChain/device/default", request, RuleChain.class).getBody();
    }

    public Optional<RuleChain> setRootRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/ruleChain/{ruleChainId}/root", null, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public RuleChainMetaData saveRuleChainMetaData(RuleChainMetaData ruleChainMetaData) {
        return restTemplate.postForEntity(baseURL + "/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class).getBody();
    }

    public PageData<RuleChain> getRuleChains(PageLink pageLink) {
        return getRuleChains(RuleChainType.CORE, pageLink);
    }

    public PageData<RuleChain> getRuleChains(RuleChainType ruleChainType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", ruleChainType.name());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/ruleChains?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<RuleChain>>() {
                },
                params).getBody();
    }

    public void deleteRuleChain(RuleChainId ruleChainId) {
        restTemplate.delete(baseURL + "/api/ruleChain/{ruleChainId}", ruleChainId.getId());
    }

    public Optional<JsonNode> getLatestRuleNodeDebugInput(RuleNodeId ruleNodeId) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.getForEntity(baseURL + "/api/ruleNode/{ruleNodeId}/debugIn", JsonNode.class, ruleNodeId.getId());
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> testScript(JsonNode inputParams) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/ruleChain/testScript", inputParams, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public RuleChainData exportRuleChains(int limit) {
        return restTemplate.getForEntity(baseURL + "/api/ruleChains/export?limit=" + limit, RuleChainData.class).getBody();
    }

    public void importRuleChains(RuleChainData ruleChainData, boolean overwrite) {
        restTemplate.postForLocation(baseURL + "/api/ruleChains/import?overwrite=" + overwrite, ruleChainData);
    }

    public List<String> getAttributeKeys(EntityId entityId) {
        return restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/keys/attributes",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<String>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString()).getBody();
    }

    public List<String> getAttributeKeysByScope(EntityId entityId, String scope) {
        return restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/keys/attributes/{scope}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<String>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString(),
                scope).getBody();
    }

    public List<AttributeKvEntry> getAttributeKvEntries(EntityId entityId, List<String> keys) {
        List<JsonNode> attributes = restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/attributes?keys={keys}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<JsonNode>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId(),
                listToString(keys)).getBody();

        return RestJsonConverter.toAttributes(attributes);
    }

    public Future<List<AttributeKvEntry>> getAttributeKvEntriesAsync(EntityId entityId, List<String> keys) {
        return getExecutor().submit(() -> getAttributeKvEntries(entityId, keys));
    }

    public List<AttributeKvEntry> getAttributesByScope(EntityId entityId, String scope, List<String> keys) {
        List<JsonNode> attributes = restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/attributes/{scope}?keys={keys}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<JsonNode>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString(),
                scope,
                listToString(keys)).getBody();

        return RestJsonConverter.toAttributes(attributes);
    }

    public List<String> getTimeseriesKeys(EntityId entityId) {
        return restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/keys/timeseries",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<String>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString()).getBody();
    }

    public List<TsKvEntry> getLatestTimeseries(EntityId entityId, List<String> keys) {
        return getLatestTimeseries(entityId, keys, true);
    }

    public List<TsKvEntry> getLatestTimeseries(EntityId entityId, List<String> keys, boolean useStrictDataTypes) {
        Map<String, List<JsonNode>> timeseries = restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries?keys={keys}&useStrictDataTypes={useStrictDataTypes}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, List<JsonNode>>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString(),
                listToString(keys),
                useStrictDataTypes).getBody();

        return RestJsonConverter.toTimeseries(timeseries);
    }

    @Deprecated
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long interval, Aggregation agg, TimePageLink pageLink) {
        return getTimeseries(entityId, keys, interval, agg, pageLink, true);
    }

    @Deprecated
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long interval, Aggregation agg, TimePageLink pageLink, boolean useStrictDataTypes) {
        SortOrder sortOrder = pageLink.getSortOrder();
        return getTimeseries(entityId, keys, interval, agg, sortOrder != null ? sortOrder.getDirection() : null, pageLink.getStartTime(), pageLink.getEndTime(), 100, useStrictDataTypes);
    }

    @Deprecated
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long interval, Aggregation agg, SortOrder.Direction sortOrder, Long startTime, Long endTime, Integer limit, boolean useStrictDataTypes) {
        return getTimeseries(entityId, keys, interval, null, null, agg, sortOrder, startTime, endTime, limit, useStrictDataTypes);
    }

    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long interval, IntervalType intervalType, String timeZone, Aggregation agg, SortOrder.Direction sortOrder, Long startTime, Long endTime, Integer limit, boolean useStrictDataTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", listToString(keys));
        params.put("interval", interval == null ? "0" : interval.toString());
        params.put("agg", agg == null ? "NONE" : agg.name());
        params.put("limit", limit != null ? limit.toString() : "100");
        params.put("orderBy", sortOrder != null ? sortOrder.name() : "DESC");
        params.put("useStrictDataTypes", Boolean.toString(useStrictDataTypes));

        StringBuilder urlBuilder = new StringBuilder(baseURL);
        urlBuilder.append("/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries?keys={keys}&interval={interval}&limit={limit}&agg={agg}&useStrictDataTypes={useStrictDataTypes}&orderBy={orderBy}");

        if (intervalType != null) {
            urlBuilder.append("&intervalType={intervalType}");
            params.put("intervalType", intervalType.name());
        }

        if (timeZone != null) {
            urlBuilder.append("&timeZone={timeZone}");
            params.put("timeZone", timeZone);
        }

        if (startTime != null) {
            urlBuilder.append("&startTs={startTs}");
            params.put("startTs", String.valueOf(startTime));
        }
        if (endTime != null) {
            urlBuilder.append("&endTs={endTs}");
            params.put("endTs", String.valueOf(endTime));
        }

        Map<String, List<JsonNode>> timeseries = restTemplate.exchange(
                urlBuilder.toString(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, List<JsonNode>>>() {
                },
                params).getBody();

        return RestJsonConverter.toTimeseries(timeseries);
    }

    public List<ReadTsKvQueryResult> getTimeseriesByQueries(EntityId entityId, List<BaseReadTsKvQuery> queries) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());

        StringBuilder urlBuilder = new StringBuilder(baseURL);
        urlBuilder.append("/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries");

        JsonNode body = restTemplate.exchange(
                urlBuilder.toString(),
                HttpMethod.POST,
                queries == null ? HttpEntity.EMPTY : new HttpEntity<>(queries),
                new ParameterizedTypeReference<JsonNode>() {
                },
                params).getBody();
        return RestJsonConverter.toReadTsKvQueryResult(body);
    }

    public boolean saveDeviceAttributes(DeviceId deviceId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(baseURL + "/api/plugins/telemetry/{deviceId}/{scope}", request, Object.class, deviceId.getId().toString(), scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean saveEntityAttributesV1(EntityId entityId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/{scope}",
                        request,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean saveEntityAttributesV2(EntityId entityId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/attributes/{scope}",
                        request,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean saveEntityTelemetry(EntityId entityId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/timeseries/{scope}",
                        request,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean saveEntityTelemetryWithTTL(EntityId entityId, String scope, Long ttl, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/timeseries/{scope}/{ttl}",
                        request,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope,
                        ttl)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean pushEntityTelemetry(String credentialsId, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/v1/{credentialsId}/telemetry",
                        request,
                        Object.class,
                        credentialsId)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean deleteEntityTimeseries(EntityId entityId,
                                          List<String> keys,
                                          boolean deleteAllDataForKeys,
                                          Long startTs,
                                          Long endTs,
                                          boolean rewriteLatestIfDeleted,
                                          boolean deleteLatest) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", listToString(keys));
        params.put("deleteAllDataForKeys", String.valueOf(deleteAllDataForKeys));
        params.put("startTs", startTs.toString());
        params.put("endTs", endTs.toString());
        params.put("rewriteLatestIfDeleted", String.valueOf(rewriteLatestIfDeleted));
        params.put("deleteLatest", String.valueOf(deleteLatest));

        return restTemplate
                .exchange(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/timeseries/delete?keys={keys}&deleteAllDataForKeys={deleteAllDataForKeys}&startTs={startTs}&endTs={endTs}&rewriteLatestIfDeleted={rewriteLatestIfDeleted}&deleteLatest={deleteLatest}",
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Object.class,
                        params)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean deleteEntityLatestTimeseries(EntityId entityId, List<String> keys) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", listToString(keys));

        return restTemplate
                .exchange(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/timeseries/latest/delete?keys={keys}",
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Object.class,
                        params)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean deleteEntityAttributes(DeviceId deviceId, String scope, List<String> keys) {
        return restTemplate
                .exchange(
                        baseURL + "/api/plugins/telemetry/{deviceId}/{scope}?keys={keys}",
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Object.class,
                        deviceId.getId().toString(),
                        scope,
                        listToString(keys))
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean deleteEntityAttributes(EntityId entityId, String scope, List<String> keys) {
        return restTemplate
                .exchange(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/{scope}?keys={keys}",
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope,
                        listToString(keys))
                .getStatusCode()
                .is2xxSuccessful();

    }

    public Optional<Tenant> getTenantById(TenantId tenantId) {
        try {
            ResponseEntity<Tenant> tenant = restTemplate.getForEntity(baseURL + "/api/tenant/{tenantId}", Tenant.class, tenantId.getId());
            return Optional.ofNullable(tenant.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<TenantInfo> getTenantInfoById(TenantId tenantId) {
        try {
            ResponseEntity<TenantInfo> tenant = restTemplate.getForEntity(baseURL + "/api/tenant/info/{tenantId}", TenantInfo.class, tenantId);
            return Optional.ofNullable(tenant.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Tenant saveTenant(Tenant tenant) {
        return restTemplate.postForEntity(baseURL + "/api/tenant", tenant, Tenant.class).getBody();
    }

    public void deleteTenant(TenantId tenantId) {
        restTemplate.delete(baseURL + "/api/tenant/{tenantId}", tenantId.getId());
    }

    public PageData<Tenant> getTenants(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenants?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Tenant>>() {
                }, params).getBody();
    }

    public PageData<TenantInfo> getTenantInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenantInfos?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<TenantInfo>>() {
                }, params).getBody();
    }

    public UsageInfo getUsageInfo() {
        return restTemplate.exchange(
                baseURL + "/api/usage",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                UsageInfo.class).getBody();
    }

    public Optional<TenantProfile> getTenantProfileById(TenantProfileId tenantProfileId) {
        try {
            ResponseEntity<TenantProfile> tenantProfile = restTemplate.getForEntity(baseURL + "/api/tenantProfile/{tenantProfileId}", TenantProfile.class, tenantProfileId);
            return Optional.ofNullable(tenantProfile.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<EntityInfo> getTenantProfileInfoById(TenantProfileId tenantProfileId) {
        try {
            ResponseEntity<EntityInfo> entityInfo = restTemplate.getForEntity(baseURL + "/api/tenantProfileInfo/{tenantProfileId}", EntityInfo.class, tenantProfileId);
            return Optional.ofNullable(entityInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public EntityInfo getDefaultTenantProfileInfo() {
        return restTemplate.getForEntity(baseURL + "/api/tenantProfileInfo/default", EntityInfo.class).getBody();
    }

    public TenantProfile saveTenantProfile(TenantProfile tenantProfile) {
        return restTemplate.postForEntity(baseURL + "/api/tenantProfile", tenantProfile, TenantProfile.class).getBody();
    }

    public void deleteTenantProfile(TenantProfileId tenantProfileId) {
        restTemplate.delete(baseURL + "/api/tenantProfile/{tenantProfileId}", tenantProfileId);
    }

    public TenantProfile setDefaultTenantProfile(TenantProfileId tenantProfileId) {
        return restTemplate.exchange(baseURL + "/api/tenantProfile/{tenantProfileId}/default", HttpMethod.POST, HttpEntity.EMPTY, TenantProfile.class, tenantProfileId).getBody();
    }

    public PageData<TenantProfile> getTenantProfiles(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenantProfiles?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<TenantProfile>>() {
                }, params).getBody();
    }

    public PageData<EntityInfo> getTenantProfileInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenantProfileInfos?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityInfo>>() {
                }, params).getBody();
    }

    public Optional<User> getUserById(UserId userId) {
        try {
            ResponseEntity<User> user = restTemplate.getForEntity(baseURL + "/api/user/{userId}", User.class, userId.getId());
            return Optional.ofNullable(user.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Boolean isUserTokenAccessEnabled() {
        return restTemplate.getForEntity(baseURL + "/api/user/tokenAccessEnabled", Boolean.class).getBody();
    }

    public Optional<JsonNode> getUserToken(UserId userId) {
        try {
            ResponseEntity<JsonNode> userToken = restTemplate.getForEntity(baseURL + "/api/user/{userId}/token", JsonNode.class, userId.getId());
            return Optional.ofNullable(userToken.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public User saveUser(User user, boolean sendActivationMail) {
        return saveUser(user, sendActivationMail, null);
    }

    public User saveUser(User user, boolean sendActivationMail, EntityGroupId entityGroupId) {
        if (entityGroupId == null) {
            return restTemplate.postForEntity(baseURL + "/api/user?sendActivationMail={sendActivationMail}", user, User.class, sendActivationMail).getBody();
        } else {
            return restTemplate.postForEntity(baseURL + "/api/user?sendActivationMail={sendActivationMail}&entityGroupId={entityGroupId}", user, User.class, sendActivationMail, entityGroupId.getId()).getBody();
        }
    }

    public void sendActivationEmail(String email) {
        restTemplate.postForLocation(baseURL + "/api/user/sendActivationMail?email={email}", null, email);
    }

    public String getActivationLink(UserId userId) {
        return restTemplate.getForEntity(baseURL + "/api/user/{userId}/activationLink", String.class, userId.getId()).getBody();
    }

    public void deleteUser(UserId userId) {
        restTemplate.delete(baseURL + "/api/user/{userId}", userId.getId());
    }

    public PageData<User> getUsers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                }, params).getBody();
    }

    public PageData<UserEmailInfo> getUsersByQuery(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/users/info?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<UserEmailInfo>>() {
                }, params).getBody();
    }

    public PageData<User> getTenantAdmins(TenantId tenantId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("tenantId", tenantId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/tenant/{tenantId}/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                }, params).getBody();
    }

    public PageData<User> getCustomerUsers(CustomerId customerId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                }, params).getBody();
    }

    public PageData<UserEmailInfo> getUsersForAssign(AlarmId alarmId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("alarmId", alarmId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/users/assign/{alarmId}" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<UserEmailInfo>>() {
                }, params).getBody();
    }

    public void setUserCredentialsEnabled(UserId userId, boolean userCredentialsEnabled) {
        restTemplate.postForLocation(
                baseURL + "/api/user/{userId}/userCredentialsEnabled?userCredentialsEnabled={userCredentialsEnabled}",
                null,
                userId.getId(),
                userCredentialsEnabled);
    }

    public ApiKey saveApiKey(ApiKeyInfo apiKeyInfo) {
        return restTemplate.postForEntity(baseURL + "/api/apiKey", apiKeyInfo, ApiKey.class).getBody();
    }

    public PageData<ApiKeyInfo> getUserApiKeys(UserId userId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/apiKeys/{userId}?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<ApiKeyInfo>>() {}, params).getBody();
    }

    public ApiKeyInfo updateApiKeyDescription(ApiKeyId apiKeyId, String description) {
        return restTemplate.exchange(
                baseURL + "/api/apiKey/{id}/description",
                HttpMethod.PUT,
                new HttpEntity<>(description),
                ApiKeyInfo.class,
                apiKeyId.getId()).getBody();
    }

    public ApiKeyInfo enableApiKey(ApiKeyId apiKeyId, boolean enabled) {
        return restTemplate.exchange(
                baseURL + "/api/apiKey/{id}/enabled/{enabledValue}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                ApiKeyInfo.class,
                apiKeyId.getId(),
                enabled).getBody();
    }

    public void deleteApiKey(ApiKeyId apiKeyId) {
        restTemplate.delete(baseURL + "/api/apiKey/{id}", apiKeyId.getId());
    }

    public PageData<User> getUsersByEntityGroupId(EntityGroupId entityGroupId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityGroupId", entityGroupId.toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/entityGroup/{entityGroupId}/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                }, params).getBody();
    }

    public Optional<WidgetsBundle> getWidgetsBundleById(WidgetsBundleId widgetsBundleId) {
        try {
            ResponseEntity<WidgetsBundle> widgetsBundle =
                    restTemplate.getForEntity(baseURL + "/api/widgetsBundle/{widgetsBundleId}", WidgetsBundle.class, widgetsBundleId.getId());
            return Optional.ofNullable(widgetsBundle.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle) {
        return restTemplate.postForEntity(baseURL + "/api/widgetsBundle", widgetsBundle, WidgetsBundle.class).getBody();
    }

    public void updateWidgetsBundleWidgetTypes(WidgetsBundleId widgetsBundleId, List<WidgetTypeId> widgetTypeIds) {
        var httpEntity = new HttpEntity<>(widgetTypeIds.stream()
                .map(widgetTypeId -> widgetTypeId.getId().toString())
                .collect(Collectors.toList()));
        restTemplate.exchange(baseURL + "/api/widgetsBundle/{widgetsBundleId}/widgetTypes",
                HttpMethod.POST, httpEntity, Void.class, widgetsBundleId.getId());
    }

    public void updateWidgetsBundleWidgetFqns(WidgetsBundleId widgetsBundleId, List<String> widgetTypeFqns) {
        restTemplate.exchange(baseURL + "/api/widgetsBundle/{widgetsBundleId}/widgetTypeFqns",
                HttpMethod.POST, new HttpEntity<>(widgetTypeFqns), Void.class, widgetsBundleId.getId());
    }

    public void deleteWidgetsBundle(WidgetsBundleId widgetsBundleId) {
        restTemplate.delete(baseURL + "/api/widgetsBundle/{widgetsBundleId}", widgetsBundleId.getId());
    }

    public PageData<WidgetsBundle> getWidgetsBundles(PageLink pageLink) {
        return getWidgetsBundles(pageLink, null, null);
    }

    public PageData<WidgetsBundle> getWidgetsBundles(PageLink pageLink, Boolean tenantOnly, Boolean fullSearch) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        addTenantOnlyAndFullSearchToParams(tenantOnly, fullSearch, params);
        return restTemplate.exchange(
                baseURL + "/api/widgetsBundles?" + getUrlParams(pageLink) + getTenantOnlyAndFullSearchUrlParams(tenantOnly, fullSearch),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<WidgetsBundle>>() {
                }, params).getBody();
    }

    public List<WidgetsBundle> getWidgetsBundles() {
        return restTemplate.exchange(
                baseURL + "/api/widgetsBundles",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetsBundle>>() {
                }).getBody();
    }

    public Optional<WidgetTypeDetails> getWidgetTypeById(WidgetTypeId widgetTypeId) {
        try {
            ResponseEntity<WidgetTypeDetails> widgetTypeDetails =
                    restTemplate.getForEntity(baseURL + "/api/widgetType/{widgetTypeId}", WidgetTypeDetails.class, widgetTypeId.getId());
            return Optional.ofNullable(widgetTypeDetails.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<WidgetTypeInfo> getWidgetTypeInfoById(WidgetTypeId widgetTypeId) {
        try {
            ResponseEntity<WidgetTypeInfo> widgetTypeInfo =
                    restTemplate.getForEntity(baseURL + "/api/widgetTypeInfo/{widgetTypeId}", WidgetTypeInfo.class, widgetTypeId.getId());
            return Optional.ofNullable(widgetTypeInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    public WidgetTypeDetails saveWidgetType(WidgetTypeDetails widgetTypeDetails) {
        return saveWidgetType(widgetTypeDetails, null);
    }

    public WidgetTypeDetails saveWidgetType(WidgetTypeDetails widgetTypeDetails, Boolean updateExistingByFqn) {
        if (updateExistingByFqn == null) {
            return restTemplate.postForEntity(baseURL + "/api/widgetType", widgetTypeDetails, WidgetTypeDetails.class).getBody();
        }
        return restTemplate.postForEntity(baseURL + "/api/widgetType?updateExistingByFqn={updateExistingByFqn}", widgetTypeDetails, WidgetTypeDetails.class, updateExistingByFqn).getBody();
    }

    public void deleteWidgetType(WidgetTypeId widgetTypeId) {
        restTemplate.delete(baseURL + "/api/widgetType/{widgetTypeId}", widgetTypeId.getId());
    }

    public PageData<WidgetTypeInfo> getWidgetTypes(PageLink pageLink) {
        return getWidgetTypes(pageLink, null, null, null, null);
    }

    public PageData<WidgetTypeInfo> getWidgetTypes(PageLink pageLink, Boolean tenantOnly, Boolean fullSearch,
                                                   DeprecatedFilter deprecatedFilter, List<String> widgetTypeList) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        addWidgetInfoFiltersToParams(tenantOnly, fullSearch, deprecatedFilter, widgetTypeList, params);
        return restTemplate.exchange(
                baseURL + "/api/widgetTypes?" + getUrlParams(pageLink) +
                        getWidgetTypeInfoPageRequestUrlParams(tenantOnly, fullSearch, deprecatedFilter, widgetTypeList),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<WidgetTypeInfo>>() {
                },
                params).getBody();
    }

    @Deprecated // current name in the controller: getBundleWidgetTypesByBundleAlias
    public List<WidgetType> getBundleWidgetTypes(boolean isSystem, String bundleAlias) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypes?isSystem={isSystem}&bundleAlias={bundleAlias}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetType>>() {
                },
                isSystem,
                bundleAlias).getBody();
    }

    public List<WidgetType> getBundleWidgetTypes(WidgetsBundleId widgetsBundleId) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypes?widgetsBundleId={widgetsBundleId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetType>>() {
                },
                widgetsBundleId.getId()).getBody();
    }

    @Deprecated // current name in the controller: getBundleWidgetTypesDetailsByBundleAlias
    public List<WidgetTypeDetails> getBundleWidgetTypesDetails(boolean isSystem, String bundleAlias) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypesDetails?isSystem={isSystem}&bundleAlias={bundleAlias}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetTypeDetails>>() {
                },
                isSystem,
                bundleAlias).getBody();
    }

    public List<WidgetTypeDetails> getBundleWidgetTypesDetails(WidgetsBundleId widgetsBundleId, boolean inlineImages) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypesDetails?widgetsBundleId={widgetsBundleId}&inlineImages={inlineImages}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetTypeDetails>>() {
                },
                widgetsBundleId.getId(),
                inlineImages).getBody();
    }

    public List<String> getBundleWidgetTypeFqns(WidgetsBundleId widgetsBundleId) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypeFqns?widgetsBundleId={widgetsBundleId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<String>>() {
                },
                widgetsBundleId.getId()).getBody();
    }

    @Deprecated // current name in the controller: getBundleWidgetTypesInfosByBundleAlias
    public List<WidgetTypeInfo> getBundleWidgetTypesInfos(boolean isSystem, String bundleAlias) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypesInfos?isSystem={isSystem}&bundleAlias={bundleAlias}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetTypeInfo>>() {
                },
                isSystem,
                bundleAlias).getBody();
    }

    public PageData<WidgetTypeInfo> getBundleWidgetTypesInfos(WidgetsBundleId widgetsBundleId, PageLink pageLink) {
        return getBundleWidgetTypesInfos(widgetsBundleId, pageLink, null, null, null, null);
    }

    public PageData<WidgetTypeInfo> getBundleWidgetTypesInfos(WidgetsBundleId widgetsBundleId, PageLink pageLink,
                                                              Boolean tenantOnly, Boolean fullSearch,
                                                              DeprecatedFilter deprecatedFilter, List<String> widgetTypeList) {
        Map<String, String> params = new HashMap<>();
        params.put("widgetsBundleId", widgetsBundleId.getId().toString());
        addPageLinkToParam(params, pageLink);
        addWidgetInfoFiltersToParams(tenantOnly, fullSearch, deprecatedFilter, widgetTypeList, params);
        return restTemplate.exchange(
                baseURL + "/api/widgetTypesInfos?widgetsBundleId={widgetsBundleId}&" + getUrlParams(pageLink) +
                        getWidgetTypeInfoPageRequestUrlParams(tenantOnly, fullSearch, deprecatedFilter, widgetTypeList),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<WidgetTypeInfo>>() {
                },
                params).getBody();
    }

    @Deprecated // current name in the controller: getWidgetTypeByBundleAliasAndTypeAlias
    public Optional<WidgetType> getWidgetType(boolean isSystem, String bundleAlias, String alias) {
        try {
            ResponseEntity<WidgetType> widgetType =
                    restTemplate.getForEntity(
                            baseURL + "/api/widgetType?isSystem={isSystem}&bundleAlias={bundleAlias}&alias={alias}",
                            WidgetType.class,
                            isSystem,
                            bundleAlias,
                            alias);
            return Optional.ofNullable(widgetType.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<WidgetType> getWidgetType(String fqn) {
        try {
            ResponseEntity<WidgetType> widgetType =
                    restTemplate.getForEntity(
                            baseURL + "/api/widgetType?fqn={fqn}",
                            WidgetType.class,
                            fqn);
            return Optional.ofNullable(widgetType.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    public Boolean isEdgesSupportEnabled() {
        return restTemplate.getForEntity(baseURL + "/api/edges/enabled", Boolean.class).getBody();
    }

    public Edge saveEdge(Edge edge) {
        return restTemplate.postForEntity(baseURL + "/api/edge", edge, Edge.class).getBody();
    }

    public void deleteEdge(EdgeId edgeId) {
        restTemplate.delete(baseURL + "/api/edge/{edgeId}", edgeId.getId());
    }

    public Optional<Edge> getEdgeById(EdgeId edgeId) {
        try {
            ResponseEntity<Edge> edge = restTemplate.getForEntity(baseURL + "/api/edge/{edgeId}", Edge.class, edgeId.getId());
            return Optional.ofNullable(edge.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Edge> setEdgeRootRuleChain(EdgeId edgeId, RuleChainId ruleChainId) {
        try {
            ResponseEntity<Edge> ruleChain = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/{ruleChainId}/root", null, Edge.class, edgeId.getId(), ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Edge> getEdges(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edges?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Edge>>() {
                }, params).getBody();
    }

    public Optional<RuleChain> assignRuleChainToEdge(EdgeId edgeId, RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/ruleChain/{ruleChainId}", null, RuleChain.class, edgeId.getId(), ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<RuleChain> unassignRuleChainFromEdge(EdgeId edgeId, RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/ruleChain/{ruleChainId}", HttpMethod.DELETE, HttpEntity.EMPTY, RuleChain.class, edgeId.getId(), ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<EntityGroup> assignEntityGroupToEdge(EdgeId edgeId, EntityGroupId entityGroupId, EntityType groupType) {
        try {
            ResponseEntity<EntityGroup> entityGroup = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/entityGroup/{entityGroupId}/{groupType}",
                    null, EntityGroup.class, edgeId.getId(), entityGroupId.getId(), groupType.name());
            return Optional.ofNullable(entityGroup.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<EntityGroup> unassignEntityGroupFromEdge(EdgeId edgeId, EntityGroupId entityGroupId, EntityType groupType) {
        try {
            ResponseEntity<EntityGroup> entityGroup = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/entityGroup/{entityGroupId}/{groupType}",
                    HttpMethod.DELETE, HttpEntity.EMPTY, EntityGroup.class, edgeId.getId(), entityGroupId.getId(), groupType.name());
            return Optional.ofNullable(entityGroup.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<SchedulerEvent> getEdgeSchedulerEvents(EdgeId edgeId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/schedulerEvents?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<SchedulerEvent>>() {
                }, params).getBody();
    }

    public Optional<SchedulerEvent> assignSchedulerEventToEdge(EdgeId edgeId, SchedulerEventId schedulerEventId) {
        try {
            ResponseEntity<SchedulerEvent> schedulerEvent = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/schedulerEvent/{schedulerEventId}",
                    null, SchedulerEvent.class, edgeId.getId(), schedulerEventId.getId());
            return Optional.ofNullable(schedulerEvent.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<SchedulerEvent> unassignSchedulerEventFromEdge(EdgeId edgeId, SchedulerEventId schedulerEventId) {
        try {
            ResponseEntity<SchedulerEvent> schedulerEvent = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/schedulerEvent/{schedulerEventId}",
                    HttpMethod.DELETE, HttpEntity.EMPTY, SchedulerEvent.class, edgeId.getId(), schedulerEventId.getId());
            return Optional.ofNullable(schedulerEvent.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<EntityGroupInfo> getAllEdgeEntityGroups(EdgeId edgeId, EntityType groupType) {
        return restTemplate.exchange(
                baseURL + "/api/allEntityGroups/edge/{edgeId}/{groupType}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityGroupInfo>>() {},
                edgeId.getId(),
                groupType.name()).getBody();
    }

    public PageData<EntityGroupInfo> getEdgeEntityGroups(EdgeId edgeId, EntityType groupType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        params.put("groupType", groupType.name());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/entityGroups/edge/{edgeId}/{groupType}?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityGroupInfo>>() {},
                params).getBody();
    }

    public PageData<RuleChain> getEdgeRuleChains(EdgeId edgeId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/ruleChains?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<RuleChain>>() {
                }, params).getBody();
    }

    public Optional<RuleChain> setAutoAssignToEdgeRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/ruleChain/{ruleChainId}/autoAssignToEdge", null, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<RuleChain> unsetAutoAssignToEdgeRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.exchange(baseURL + "/api/ruleChain/{ruleChainId}/autoAssignToEdge", HttpMethod.DELETE, HttpEntity.EMPTY, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<RuleChain> getAutoAssignToEdgeRuleChains() {
        return restTemplate.exchange(baseURL + "/api/ruleChain/autoAssignToEdgeRuleChains",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<RuleChain>>() {
                }).getBody();
    }

    public Optional<RuleChain> setRootEdgeTemplateRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/ruleChain/{ruleChainId}/edgeTemplateRoot", null, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Edge> getTenantEdges(String type, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/edges?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Edge>>() {
                }, params).getBody();
    }

    public Optional<Edge> getTenantEdge(String edgeName) {
        try {
            ResponseEntity<Edge> edge = restTemplate.getForEntity(baseURL + "/api/tenant/edges?edgeName={edgeName}", Edge.class, edgeName);
            return Optional.ofNullable(edge.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Edge> getCustomerEdges(CustomerId customerId, PageLink pageLink, String edgeType) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", edgeType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/edges?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Edge>>() {
                }, params).getBody();
    }

    public PageData<Edge> getUserEdges(PageLink pageLink, String edgeType) {
        Map<String, String> params = new HashMap<>();
        params.put("type", edgeType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/user/edges?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Edge>>() {
                }, params).getBody();
    }

    public List<Edge> getEdgesByIds(List<EdgeId> edgeIds) {
        return restTemplate.exchange(baseURL + "/api/edges?edgeIds={edgeIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY, new ParameterizedTypeReference<List<Edge>>() {
                }, listIdsToString(edgeIds)).getBody();
    }

    public List<Edge> findByQuery(EdgeSearchQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/edges",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<Edge>>() {
                }).getBody();
    }

    public List<EntitySubtype> getEdgeTypes() {
        return restTemplate.exchange(
                baseURL + "/api/edge/types",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }).getBody();
    }

    public PageData<EdgeEvent> getEdgeEvents(EdgeId edgeId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/events?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EdgeEvent>>() {
                },
                params).getBody();
    }

    public void syncEdge(EdgeId edgeId) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.toString());
        restTemplate.postForEntity(baseURL + "/api/edge/sync/{edgeId}", null, EdgeId.class, params);
    }

    public String findMissingToRelatedRuleChains(EdgeId edgeId) {
        return restTemplate.getForEntity(baseURL + "/api/edge/missingToRelatedRuleChains/{edgeId}", String.class, edgeId.getId()).getBody();
    }

    public BulkImportResult<Edge> processEdgesBulkImport(BulkImportRequest request) {
        return restTemplate.exchange(
                baseURL + "/api/edge/bulk_import",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<BulkImportResult<Edge>>() {
                }).getBody();
    }

    public Optional<EdgeInstructions> getEdgeInstallInstructions(EdgeId edgeId, String method) {
        ResponseEntity<EdgeInstructions> edgeInstallInstructionsResult =
                restTemplate.getForEntity(baseURL + "/api/edge/instructions/install/{edgeId}/{method}", EdgeInstructions.class, edgeId.getId(), method);
        return Optional.ofNullable(edgeInstallInstructionsResult.getBody());
    }

    public Optional<EdgeInstructions> getEdgeUpgradeInstructions(String edgeVersion, String method) {
        ResponseEntity<EdgeInstructions> edgeUpgradeInstructionsResult =
                restTemplate.getForEntity(baseURL + "/api/edge/instructions/upgrade/{edgeVersion}/{method}", EdgeInstructions.class, edgeVersion, method);
        return Optional.ofNullable(edgeUpgradeInstructionsResult.getBody());
    }

    // Agent Controller

    public Agent saveAgent(Agent agent) {
        return restTemplate.postForEntity(baseURL + "/api/agent", agent, Agent.class).getBody();
    }

    public void deleteAgent(AgentId agentId) {
        restTemplate.delete(baseURL + "/api/agent/{agentId}", agentId.getId());
    }

    public Optional<Agent> getAgentById(AgentId agentId) {
        try {
            ResponseEntity<Agent> agent = restTemplate.getForEntity(baseURL + "/api/agent/{agentId}", Agent.class, agentId.getId());
            return Optional.ofNullable(agent.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<AgentInfo> getAgentInfoById(AgentId agentId) {
        try {
            ResponseEntity<AgentInfo> agentInfo = restTemplate.getForEntity(baseURL + "/api/agent/info/{agentId}", AgentInfo.class, agentId.getId());
            return Optional.ofNullable(agentInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Agent> getTenantAgents(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/agents?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Agent>>() {},
                params).getBody();
    }

    public PageData<AgentInfo> getTenantAgentInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/agentInfos?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AgentInfo>>() {},
                params).getBody();
    }

    public PageData<Agent> getCustomerAgents(CustomerId customerId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/agents?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Agent>>() {},
                params).getBody();
    }

    public PageData<AgentInfo> getCustomerAgentInfos(CustomerId customerId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/agentInfos?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AgentInfo>>() {},
                params).getBody();
    }

    // Agent Application Controller

    public Optional<AgentApplicationInfo> getAgentApplicationById(AgentApplicationId agentApplicationId) {
        try {
            ResponseEntity<AgentApplicationInfo> app = restTemplate.getForEntity(
                    baseURL + "/api/agent/app/{agentApplicationId}", AgentApplicationInfo.class, agentApplicationId.getId());
            return Optional.ofNullable(app.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<AgentApplicationInfo> getAgentApplicationsByAgentId(AgentId agentId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("agentId", agentId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/agent/{agentId}/apps?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AgentApplicationInfo>>() {},
                params).getBody();
    }

    public Optional<AgentApplication> getAgentApplicationByRelatedEntity(String entityType, String entityId) {
        try {
            ResponseEntity<AgentApplication> app = restTemplate.getForEntity(
                    baseURL + "/api/agent/apps/{entityType}/{entityId}", AgentApplication.class, entityType, entityId);
            return Optional.ofNullable(app.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public AgentApplication updateAgentApplication(AgentApplication agentApplication) {
        return restTemplate.exchange(
                baseURL + "/api/agent/app", HttpMethod.PUT,
                new HttpEntity<>(agentApplication), AgentApplication.class).getBody();
    }

    public AgentAppInstallResponse installAgentApp(AgentAppEventRequest request) {
        return restTemplate.postForEntity(baseURL + "/api/agent/app/event", request, AgentAppInstallResponse.class).getBody();
    }

    public AgentAppEvent createAgentAppEvent(AgentApplicationId agentApplicationId, AgentAppEventRequest request) {
        return restTemplate.postForEntity(
                baseURL + "/api/agent/app/{agentApplicationId}/event", request, AgentAppEvent.class,
                agentApplicationId.getId()).getBody();
    }

    public AgentAppEvent upgradeAgent(AgentId agentId, AgentUpgradeRequest request) {
        return restTemplate.postForEntity(
                baseURL + "/api/agent/{agentId}/upgrade", request, AgentAppEvent.class,
                agentId.getId()).getBody();
    }

    public void cancelAgentAppEvent(AgentApplicationId agentApplicationId, AgentAppEventId agentAppEventId) {
        restTemplate.postForEntity(
                baseURL + "/api/agent/app/{agentApplicationId}/event/{agentAppEventId}/cancel",
                null, Void.class, agentApplicationId.getId(), agentAppEventId.getId());
    }

    public PageData<AgentAppEvent> getAgentAppEvents(AgentApplicationId agentApplicationId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("agentApplicationId", agentApplicationId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/agent/app/{agentApplicationId}/events?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AgentAppEvent>>() {},
                params).getBody();
    }

    public PageData<AgentAppEvent> getAgentAppEventsByAgentId(AgentId agentId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("agentId", agentId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/agent/{agentId}/events?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AgentAppEvent>>() {},
                params).getBody();
    }

    public AgentApplication mergeForPreview(String templateVersion, AgentApplicationType appType, String composeType, AgentApplication application) {
        return mergeForPreview(templateVersion, appType, composeType, null, application);
    }

    public AgentApplication mergeForPreview(String templateVersion, AgentApplicationType appType, String composeType, EntityId relatedEntityId, AgentApplication application) {
        StringBuilder url = new StringBuilder(baseURL + "/api/agent/app/merge/{templateVersion}/preview?");
        Map<String, String> params = new HashMap<>();
        params.put("templateVersion", templateVersion);
        if (appType != null) {
            url.append("appType={appType}&");
            params.put("appType", appType.name());
        }
        if (composeType != null) {
            url.append("composeType={composeType}&");
            params.put("composeType", composeType);
        }
        if (relatedEntityId != null) {
            url.append("relatedEntityType={relatedEntityType}&relatedEntityId={relatedEntityId}&");
            params.put("relatedEntityType", relatedEntityId.getEntityType().name());
            params.put("relatedEntityId", relatedEntityId.getId().toString());
        }
        return restTemplate.postForEntity(url.toString(), application, AgentApplication.class, params).getBody();
    }

    public AgentApplication assignRelatedEntityToAgentApp(AgentApplicationId agentApplicationId, EntityId relatedEntityId) {
        Map<String, String> params = new HashMap<>();
        params.put("agentApplicationId", agentApplicationId.getId().toString());
        params.put("entityType", relatedEntityId.getEntityType().name());
        params.put("entityId", relatedEntityId.getId().toString());
        return restTemplate.postForEntity(
                baseURL + "/api/agent/app/{agentApplicationId}/relatedEntity/{entityType}/{entityId}",
                HttpEntity.EMPTY, AgentApplication.class, params).getBody();
    }

    public AgentApplication unassignRelatedEntityFromAgentApp(AgentApplicationId agentApplicationId) {
        return restTemplate.exchange(
                baseURL + "/api/agent/app/{agentApplicationId}/relatedEntity",
                HttpMethod.DELETE, HttpEntity.EMPTY, AgentApplication.class,
                agentApplicationId.getId().toString()).getBody();
    }

    // Agent App Template Controller

    public Optional<AgentAppTemplate> getLatestAgentAppTemplate(AgentApplicationType appType, AgentAppConfigType configType) {
        try {
            ResponseEntity<AgentAppTemplate> template = restTemplate.getForEntity(
                    baseURL + "/api/agent/app/template/{appType}/{configType}/latest", AgentAppTemplate.class,
                    appType.name(), configType.name());
            return Optional.ofNullable(template.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<AgentAppTemplate> getAgentAppTemplates() {
        return restTemplate.exchange(
                baseURL + "/api/agent/app/templates", HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<AgentAppTemplate>>() {}).getBody();
    }

    // Agent App Profile Controller

    public AgentAppProfile saveAgentAppProfile(AgentAppProfile profile) {
        return restTemplate.postForEntity(baseURL + "/api/agent/app/profile", profile, AgentAppProfile.class).getBody();
    }

    public Optional<AgentAppProfile> getAgentAppProfileById(AgentAppProfileId profileId) {
        try {
            ResponseEntity<AgentAppProfile> profile = restTemplate.getForEntity(
                    baseURL + "/api/agent/app/profile/{profileId}", AgentAppProfile.class, profileId.getId());
            return Optional.ofNullable(profile.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public void deleteAgentAppProfile(AgentAppProfileId profileId) {
        restTemplate.delete(baseURL + "/api/agent/app/profile/{profileId}", profileId.getId());
    }

    public AgentAppProfile materializeAgentAppProfile(AgentApplicationType appType, String templateVersion, String composeType) {
        StringBuilder url = new StringBuilder(baseURL)
                .append("/api/agent/app/profile/materialize/{appType}/{templateVersion}");
        Map<String, String> params = new HashMap<>();
        params.put("appType", appType.name());
        params.put("templateVersion", templateVersion);
        if (composeType != null) {
            url.append("?composeType={composeType}");
            params.put("composeType", composeType);
        }
        return restTemplate.postForEntity(url.toString(), null, AgentAppProfile.class, params).getBody();
    }

    // Agent Profile Controller

    public AgentProfile saveAgentProfile(AgentProfile agentProfile) {
        return restTemplate.postForEntity(baseURL + "/api/agent/profile", agentProfile, AgentProfile.class).getBody();
    }

    public Optional<AgentProfile> getAgentProfileById(AgentProfileId agentProfileId) {
        try {
            ResponseEntity<AgentProfile> agentProfile = restTemplate.getForEntity(
                    baseURL + "/api/agent/profile/{agentProfileId}", AgentProfile.class, agentProfileId.getId());
            return Optional.ofNullable(agentProfile.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public void deleteAgentProfile(AgentProfileId agentProfileId) {
        restTemplate.delete(baseURL + "/api/agent/profile/{agentProfileId}", agentProfileId.getId());
    }

    public void assignAppProfileToAgentProfile(AgentProfileId agentProfileId, AgentAppProfileId profileId) {
        restTemplate.postForEntity(
                baseURL + "/api/agent/profile/{agentProfileId}/appProfile/{applicationProfileId}",
                null, Void.class, agentProfileId.getId(), profileId.getId());
    }

    public void setAppProfileRelatesOnAutoDiscovery(AgentProfileId agentProfileId, AgentAppProfileId profileId, boolean relate) {
        restTemplate.postForEntity(
                baseURL + "/api/agent/profile/{agentProfileId}/appProfile/{applicationProfileId}/autoDiscovery?relate={relate}",
                null, Void.class, agentProfileId.getId(), profileId.getId(), relate);
    }

    public AgentBulkAction bulkOperation(AgentProfileId agentProfileId, AgentAppProfileId appProfileId, BulkOperationRequest request) {
        return restTemplate.postForEntity(
                baseURL + "/api/agent/profile/{agentProfileId}/appProfile/{applicationProfileId}/bulk",
                request, AgentBulkAction.class, agentProfileId.getId(), appProfileId.getId()).getBody();
    }

    public Optional<AgentBulkAction> getAgentBulkAction(AgentBulkActionId bulkActionId) {
        try {
            ResponseEntity<AgentBulkAction> bulkAction = restTemplate.getForEntity(
                    baseURL + "/api/agent/bulk/{bulkActionId}", AgentBulkAction.class, bulkActionId.getId());
            return Optional.ofNullable(bulkAction.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public UUID saveEntitiesVersion(VersionCreateRequest request) {
        return restTemplate.postForEntity(baseURL + "/api/entities/vc/version", request, UUID.class).getBody();
    }

    public Optional<VersionCreationResult> getVersionCreateRequestStatus(UUID requestId) {
        try {
            ResponseEntity<VersionCreationResult> versionCreateResult = restTemplate.getForEntity(baseURL + "/api/entities/vc/version/{requestId}/status", VersionCreationResult.class, requestId);
            return Optional.ofNullable(versionCreateResult.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<EntityVersion> listEntityVersions(EntityId externalEntityId, EntityId internalEntityId, String branch, PageLink pageLink) {
        String url = baseURL + "/api/entities/vc/version/{entityType}/{externalEntityUuid}?branch={branch}&" + getUrlParams(pageLink);
        Map<String, String> params = new HashMap<>();
        params.put("entityType", externalEntityId.getEntityType().name());
        params.put("externalEntityUuid", externalEntityId.getId().toString());
        params.put("branch", branch);
        addPageLinkToParam(params, pageLink);
        if (internalEntityId != null) {
            url += "&internalEntityId={internalEntityId}";
            params.put("internalEntityId", internalEntityId.getId().toString());
        }
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityVersion>>() {
                },
                params).getBody();
    }

    public PageData<EntityVersion> listEntityTypeVersions(EntityType entityType, String branch, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityType.name());
        params.put("branch", branch);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/entities/vc/version/{entityType}?branch={branch}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityVersion>>() {
                },
                params).getBody();
    }

    public PageData<EntityVersion> listVersions(String branch, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("branch", branch);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/entities/vc/version?branch={branch}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityVersion>>() {
                },
                params).getBody();
    }

    public List<VersionedEntityInfo> listEntitiesAtVersion(EntityType entityType, String versionId) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityType.name());
        params.put("versionId", versionId);
        return restTemplate.exchange(
                baseURL + "/api/entities/vc/entity/{entityType}/{versionId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<VersionedEntityInfo>>() {
                },
                params).getBody();
    }

    public List<VersionedEntityInfo> listAllEntitiesAtVersion(String versionId) {
        Map<String, String> params = new HashMap<>();
        params.put("versionId", versionId);
        return restTemplate.exchange(
                baseURL + "/api/entities/vc/entity/{versionId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<VersionedEntityInfo>>() {
                },
                params).getBody();
    }

    public EntityDataInfo getEntityDataInfo(EntityId externalEntityId, EntityId internalEntityId, String versionId) {
        String url = baseURL + "/api/entities/vc/info/{versionId}/{entityType}/{externalEntityUuid}";
        Map<String, String> params = new HashMap<>();
        params.put("versionId", versionId);
        params.put("entityType", externalEntityId.getEntityType().name());
        params.put("externalEntityUuid", externalEntityId.getId().toString());
        if (internalEntityId != null) {
            url += "?internalEntityId={internalEntityId}";
            params.put("internalEntityId", internalEntityId.getId().toString());
        }
        return restTemplate.getForEntity(url,
                EntityDataInfo.class, params).getBody();
    }

    public EntityDataDiff compareEntityDataToVersion(EntityId internalEntityId, String versionId) {
        return restTemplate.getForEntity(baseURL + "/api/entities/vc/diff/{entityType}/{internalEntityUuid}?versionId={versionId}",
                EntityDataDiff.class, internalEntityId.getEntityType(), internalEntityId.getId(), versionId).getBody();
    }

    public UUID loadEntitiesVersion(VersionLoadRequest request) {
        return restTemplate.postForEntity(baseURL + "/api/entities/vc/entity", request, UUID.class).getBody();
    }

    public Optional<VersionLoadResult> getVersionLoadRequestStatus(UUID requestId) {
        try {
            ResponseEntity<VersionLoadResult> versionLoadResult = restTemplate.getForEntity(baseURL + "/api/entities/vc/entity/{requestId}/status", VersionLoadResult.class, requestId);
            return Optional.ofNullable(versionLoadResult.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<BranchInfo> listBranches() {
        return restTemplate.exchange(
                baseURL + "/api/entities/vc/branches",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<BranchInfo>>() {
                }).getBody();
    }

    public ResponseEntity<Resource> downloadResource(TbResourceId resourceId) {
        Map<String, String> params = new HashMap<>();
        params.put("resourceId", resourceId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/resource/{resourceId}/download",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {
                },
                params
        );
    }

    public TbResourceInfo getResourceInfoById(TbResourceId resourceId) {
        Map<String, String> params = new HashMap<>();
        params.put("resourceId", resourceId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/resource/info/{resourceId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<TbResourceInfo>() {
                },
                params
        ).getBody();
    }

    public TbResource getResourceId(TbResourceId resourceId) {
        Map<String, String> params = new HashMap<>();
        params.put("resourceId", resourceId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/resource/{resourceId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<TbResource>() {
                },
                params
        ).getBody();
    }

    public TbResource saveResource(TbResource resource) {
        return restTemplate.postForEntity(
                baseURL + "/api/resource",
                resource,
                TbResource.class
        ).getBody();
    }

    public PageData<TbResourceInfo> getResources(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/resource?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<TbResourceInfo>>() {
                },
                params
        ).getBody();
    }

    public void deleteResource(TbResourceId resourceId) {
        restTemplate.delete("/api/resource/{resourceId}", resourceId.getId().toString());
    }

    public TbResourceInfo getImageInfo(String type, String key) {
        return restTemplate.getForObject(baseURL + "/api/images/{type}/{key}/info", TbResourceInfo.class, Map.of(
                "type", type,
                "key", key
        ));
    }

    public PageData<TbResourceInfo> getImages(PageLink pageLink, boolean includeSystemImages) {
        return this.getImages(pageLink, null, includeSystemImages);
    }

    public PageData<TbResourceInfo> getImages(PageLink pageLink, ResourceSubType imageSubType, boolean includeSystemImages) {
        Map<String, String> params = new HashMap<>();
        var url = baseURL + "/api/images?includeSystemImages={includeSystemImages}&";
        addPageLinkToParam(params, pageLink);
        params.put("includeSystemImages", String.valueOf(includeSystemImages));
        if (imageSubType != null) {
            url += "imageSubType={imageSubType}&";
            params.put("imageSubType", imageSubType.name());
        }
        return restTemplate.exchange(url + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<TbResourceInfo>>() {},
                params
        ).getBody();
    }

    public TbResourceInfo uploadImage(String fileName, byte[] data, String contentType, String title) {
        HttpEntity<MultiValueMap<String, Object>> request = createMultipartRequest(fileName, data, contentType, Map.of(
                "title", Strings.nullToEmpty(title)
        ));
        return restTemplate.postForObject(baseURL + "/api/image", request, TbResourceInfo.class);
    }

    public TbResourceInfo updateImage(String type, String key, String fileName, byte[] data, String contentType) {
        HttpEntity<MultiValueMap<String, Object>> request = createMultipartRequest(fileName, data, contentType, Map.of());
        return restTemplate.exchange(baseURL + "/api/images/{type}/{key}", HttpMethod.PUT, request, TbResourceInfo.class, Map.of(
                "type", type,
                "key", key
        )).getBody();
    }

    public TbResourceInfo updateImageInfo(String type, String key, TbResourceInfo request) {
        return restTemplate.exchange(baseURL + "/api/images/{type}/{key}/info", HttpMethod.PUT, new HttpEntity<>(request), TbResourceInfo.class, Map.of(
                "type", type,
                "key", key
        )).getBody();
    }

    public void updateImagePublicStatus(String type, String key, boolean isPublic) {
        restTemplate.put(baseURL + "/api/images/{type}/{key}/public/{isPublic}", null, Map.of(
                "type", type,
                "key", key,
                "isPublic", isPublic
        ));
    }

    public byte[] downloadImage(String type, String key) throws IOException {
        Resource image = restTemplate.exchange(baseURL + "/api/images/{type}/{key}", HttpMethod.GET, null, Resource.class, Map.of(
                "type", type,
                "key", key
        )).getBody();
        return IOUtils.toByteArray(image.getInputStream());
    }

    public byte[] downloadImagePreview(String type, String key) throws IOException {
        Resource image = restTemplate.exchange(baseURL + "/api/images/{type}/{key}/preview", HttpMethod.GET, null, Resource.class, Map.of(
                "type", type,
                "key", key
        )).getBody();
        return IOUtils.toByteArray(image.getInputStream());
    }

    public byte[] downloadPublicImage(String publicResourceKey) throws IOException {
        Resource image = restTemplate.exchange(baseURL + "/api/images/public/{publicResourceKey}", HttpMethod.GET, null, Resource.class, Map.of(
                "publicResourceKey", publicResourceKey
        )).getBody();
        return IOUtils.toByteArray(image.getInputStream());
    }

    public ResourceExportData exportImage(String type, String key) {
        return restTemplate.getForObject(baseURL + "/api/images/{type}/{key}/export", ResourceExportData.class, Map.of(
                "type", type,
                "key", key
        ));
    }

    public TbResourceInfo importImage(ResourceExportData exportData) {
        return restTemplate.exchange(baseURL + "/api/image/import", HttpMethod.PUT, new HttpEntity<>(exportData), TbResourceInfo.class).getBody();
    }

    public TbImageDeleteResult deleteImage(String type, String key, boolean force) {
        return restTemplate.exchange(baseURL + "/api/images/{type}/{key}?force={force}", HttpMethod.DELETE, null, TbImageDeleteResult.class, Map.of(
                "type", type,
                "key", key,
                "force", force
        )).getBody();
    }

    public ResponseEntity<Resource> downloadOtaPackage(OtaPackageId otaPackageId) {
        Map<String, String> params = new HashMap<>();
        params.put("otaPackageId", otaPackageId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/otaPackage/{otaPackageId}/download",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {
                },
                params
        );
    }

    public OtaPackageInfo getOtaPackageInfoById(OtaPackageId otaPackageId) {
        Map<String, String> params = new HashMap<>();
        params.put("otaPackageId", otaPackageId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/otaPackage/info/{otaPackageId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<OtaPackageInfo>() {
                },
                params
        ).getBody();
    }

    public OtaPackage getOtaPackageById(OtaPackageId otaPackageId) {
        Map<String, String> params = new HashMap<>();
        params.put("otaPackageId", otaPackageId.getId().toString());

        return restTemplate.exchange(
                baseURL + "/api/otaPackage/{otaPackageId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<OtaPackage>() {
                },
                params
        ).getBody();
    }

    public OtaPackageInfo saveOtaPackageInfo(OtaPackageInfo otaPackageInfo, boolean isUrl) {
        Map<String, String> params = new HashMap<>();
        params.put("isUrl", Boolean.toString(isUrl));
        return restTemplate.postForEntity(baseURL + "/api/otaPackage?isUrl={isUrl}", otaPackageInfo, OtaPackageInfo.class, params).getBody();
    }

    public OtaPackageInfo saveOtaPackageData(OtaPackageId otaPackageId, String checkSum, ChecksumAlgorithm checksumAlgorithm, String fileName, byte[] fileBytes) throws Exception {
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createMultipartRequest(fileName, fileBytes, null, Collections.emptyMap());

        Map<String, String> params = new HashMap<>();
        params.put("otaPackageId", otaPackageId.getId().toString());
        params.put("checksumAlgorithm", checksumAlgorithm.name());
        String url = "/api/otaPackage/{otaPackageId}?checksumAlgorithm={checksumAlgorithm}";

        if (checkSum != null) {
            url += "&checkSum={checkSum}";
        }

        return restTemplate.postForEntity(
                baseURL + url, requestEntity, OtaPackageInfo.class, params
        ).getBody();
    }

    public PageData<OtaPackageInfo> getOtaPackages(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/otaPackages?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<OtaPackageInfo>>() {
                },
                params
        ).getBody();
    }

    public PageData<OtaPackageInfo> getOtaPackages(DeviceProfileId deviceProfileId,
                                                   OtaPackageType otaPackageType,
                                                   boolean hasData,
                                                   PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("hasData", String.valueOf(hasData));
        params.put("deviceProfileId", deviceProfileId.getId().toString());
        params.put("type", otaPackageType.name());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/otaPackages/{deviceProfileId}/{type}/{hasData}?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<OtaPackageInfo>>() {
                },
                params
        ).getBody();
    }

    public void deleteOtaPackage(OtaPackageId otaPackageId) {
        restTemplate.delete(baseURL + "/api/otaPackage/{otaPackageId}", otaPackageId.getId().toString());
    }

    public PageData<Queue> getQueuesByServiceType(String serviceType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("serviceType", serviceType);
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/queues?serviceType={serviceType}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Queue>>() {
                },
                params
        ).getBody();
    }

    public Queue getQueueById(QueueId queueId) {
        return restTemplate.exchange(
                baseURL + "/api/queues/" + queueId,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Queue>() {
                }
        ).getBody();
    }

    public Queue saveQueue(Queue queue, String serviceType) {
        return restTemplate.postForEntity(baseURL + "/api/queues?serviceType=" + serviceType, queue, Queue.class).getBody();
    }

    public void deleteQueue(QueueId queueId) {
        restTemplate.delete(baseURL + "/api/queues/" + queueId);
    }

    @Deprecated
    public Optional<JsonNode> getAttributes(String accessToken, String clientKeys, String sharedKeys) {
        Map<String, String> params = new HashMap<>();
        params.put("accessToken", accessToken);
        params.put("clientKeys", clientKeys);
        params.put("sharedKeys", sharedKeys);
        try {
            ResponseEntity<JsonNode> telemetryEntity = restTemplate.getForEntity(baseURL + "/api/v1/{accessToken}/attributes?clientKeys={clientKeys}&sharedKeys={sharedKeys}", JsonNode.class, params);
            return Optional.of(telemetryEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public CalculatedField saveCalculatedField(CalculatedField calculatedField) {
        return restTemplate.postForEntity(baseURL + "/api/calculatedField", calculatedField, CalculatedField.class).getBody();
    }

    public Optional<CalculatedField> getCalculatedFieldById(CalculatedFieldId calculatedFieldId) {
        try {
            ResponseEntity<CalculatedField> calculatedField = restTemplate.getForEntity(baseURL + "/api/calculatedField/{calculatedFieldId}", CalculatedField.class, calculatedFieldId.getId());
            return Optional.ofNullable(calculatedField.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<CalculatedField> getCalculatedFieldsByEntityId(EntityId entityId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/" + entityId.getEntityType() + "/" + entityId.getId() + "/calculatedFields?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<CalculatedField>>() {
                }, params).getBody();

    }

    @SneakyThrows(URISyntaxException.class)
    public PageData<CalculatedFieldInfo> getCalculatedFields(PageLink pageLink,
                                                             Set<CalculatedFieldType> types,
                                                             EntityType entityType,
                                                             Set<UUID> entities,
                                                             Set<String> names) {
        var urlBuilder = new URIBuilder(baseURL).appendPath("/api/calculatedFields");
        urlBuilder.addParameter("pageSize", String.valueOf(pageLink.getPageSize()));
        urlBuilder.addParameter("page", String.valueOf(pageLink.getPage()));
        if (!isEmpty(pageLink.getTextSearch())) {
            urlBuilder.addParameter("textSearch", pageLink.getTextSearch());
        }
        if (pageLink.getSortOrder() != null) {
            urlBuilder.addParameter("sortProperty", pageLink.getSortOrder().getProperty());
            urlBuilder.addParameter("sortOrder", pageLink.getSortOrder().getDirection().name());
        }
        if (!CollectionUtils.isEmpty(types)) {
            for (CalculatedFieldType type : types) {
                urlBuilder.addParameter("types", type.name());
            }
        }
        if (entityType != null) {
            urlBuilder.addParameter("entityType", entityType.name());
        }
        if (!CollectionUtils.isEmpty(entities)) {
            for (UUID entity : entities) {
                urlBuilder.addParameter("entities", entity.toString());
            }
        }
        if (!CollectionUtils.isEmpty(names)) {
            for (String name : names) {
                urlBuilder.addParameter("name", name);
            }
        }
        return restTemplate.exchange(
                urlBuilder.build(),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<CalculatedFieldInfo>>() {}).getBody();
    }

    public void deleteCalculatedField(CalculatedFieldId calculatedFieldId) {
        restTemplate.delete(baseURL + "/api/calculatedField/{calculatedFieldId}", calculatedFieldId.getId());
    }

    public Optional<JsonNode> getLatestCalculatedFieldDebugEvent(CalculatedFieldId calculatedFieldId) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.getForEntity(baseURL + "/api/calculatedField/{calculatedFieldId}/debug", JsonNode.class, calculatedFieldId.getId());
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> testCalculatedFieldScript(JsonNode inputParams) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/calculatedField/testScript", inputParams, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public void reprocessCalculatedFieldAndWait(CalculatedFieldId calculatedFieldId, long startTs, long endTs) {
        restTemplate.getForEntity(buildCalculatedFieldReprocessUri(calculatedFieldId, startTs, endTs, "reprocessAndWait"), Void.class);
    }

    public Job reprocessCalculatedField(CalculatedFieldId calculatedFieldId, long startTs, long endTs) {
        return restTemplate.getForEntity(buildCalculatedFieldReprocessUri(calculatedFieldId, startTs, endTs, "reprocess"), Job.class).getBody();
    }

    private URI buildCalculatedFieldReprocessUri(CalculatedFieldId calculatedFieldId, long startTs, long endTs, String action) {
        URIBuilder builder;
        try {
            builder = new URIBuilder(baseURL);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid base URL: " + baseURL, e);
        }

        builder.appendPath("/api/calculatedField/" + calculatedFieldId.getId() + "/" + action);
        builder.addParameter("startTs", String.valueOf(startTs));
        builder.addParameter("endTs", String.valueOf(endTs));

        try {
            return builder.build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to construct API URI from base URL and provided params", e);
        }
    }

    public PageData<Notification> getNotifications(Boolean unreadOnly, NotificationDeliveryMethod deliveryMethod, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(baseURL).append("/api/notifications?").append(getUrlParams(pageLink));
        addPageLinkToParam(params, pageLink);

        if (unreadOnly != null) {
            urlBuilder.append("&unreadOnly={unreadOnly}");
            params.put("unreadOnly", unreadOnly.toString());
        }
        if (deliveryMethod != null) {
            urlBuilder.append("&deliveryMethod={deliveryMethod}");
            params.put("deliveryMethod", deliveryMethod.name());
        }

        return restTemplate.exchange(urlBuilder.toString(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Notification>>() {
                }, params).getBody();
    }

    public Integer getUnreadNotificationsCount(NotificationDeliveryMethod deliveryMethod) {
        String uri = "/api/notifications/unread/count?";
        Map<String, String> params = new HashMap<>();
        if (deliveryMethod != null) {
            params.put("deliveryMethod", deliveryMethod.name());
            uri += "&deliveryMethod={deliveryMethod}";
        }
        return restTemplate.exchange(
                baseURL + uri,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Integer.class, params).getBody();
    }

    public void markNotificationAsRead(NotificationId notificationId) {
        restTemplate.exchange(
                baseURL + "/api/notification/{id}/read",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Void.class,
                notificationId.getId());
    }

    public void markAllNotificationsAsRead(NotificationDeliveryMethod deliveryMethod) {
        String uri = "/api/notifications/read?";
        Map<String, String> params = new HashMap<>();
        if (deliveryMethod != null) {
            params.put("deliveryMethod", deliveryMethod.name());
            uri += "&deliveryMethod={deliveryMethod}";
        }
        restTemplate.exchange(
                baseURL + uri,
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Void.class,
                params);
    }


    public void deleteNotification(NotificationId notificationId) {
        restTemplate.delete(baseURL + "/api/notification/{id}", notificationId.getId());
    }

    public NotificationRequest saveNotificationRequest(NotificationRequest notificationRequest) {
        return restTemplate.postForEntity(baseURL + "/api/notification/request", notificationRequest, NotificationRequest.class).getBody();
    }

    public NotificationRequestPreview getNotificationRequestPreview(NotificationRequest notificationRequest, int recipientsPreviewSize) {
        return restTemplate.postForEntity(baseURL + "/api/notification/request/preview?recipientsPreviewSize={recipientsPreviewSize}", notificationRequest, NotificationRequestPreview.class, recipientsPreviewSize).getBody();
    }

    public Optional<NotificationRequestInfo> getNotificationRequestById(NotificationRequestId notificationRequestId) {
        try {
            ResponseEntity<NotificationRequestInfo> notificationRequest = restTemplate.getForEntity(baseURL + "/api/notification/request/{id}", NotificationRequestInfo.class, notificationRequestId.getId());
            return Optional.ofNullable(notificationRequest.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<NotificationRequestInfo> getNotificationRequests(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/notification/requests?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<NotificationRequestInfo>>() {
                }, params).getBody();
    }

    public void deleteNotificationRequest(NotificationRequestId notificationRequestId) {
        restTemplate.delete(baseURL + "/api/notification/request/{id}", notificationRequestId.getId());
    }

    public NotificationSettings saveNotificationSettings(NotificationSettings notificationSettings) {
        return restTemplate.postForEntity(baseURL + "/api/notification/settings", notificationSettings, NotificationSettings.class).getBody();
    }

    public Optional<NotificationSettings> getNotificationSettings() {
        try {
            ResponseEntity<NotificationSettings> notificationSettings = restTemplate.getForEntity(baseURL + "/api/notification/settings", NotificationSettings.class);
            return Optional.ofNullable(notificationSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<NotificationDeliveryMethod> getAvailableDeliveryMethods() {
        return restTemplate.exchange(URI.create(
                        baseURL + "/api/notification/deliveryMethods"),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<NotificationDeliveryMethod>>() {
                }).getBody();
    }

    public UserNotificationSettings saveUserNotificationSettings(UserNotificationSettings userNotificationSettings) {
        return restTemplate.postForEntity(baseURL + "/api/notification/settings/user", userNotificationSettings, UserNotificationSettings.class).getBody();
    }

    public Optional<UserNotificationSettings> getUserNotificationSettings() {
        try {
            ResponseEntity<UserNotificationSettings> userNotificationSettings = restTemplate.getForEntity(baseURL + "/api/notification/settings/user", UserNotificationSettings.class);
            return Optional.ofNullable(userNotificationSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public NotificationTarget saveNotificationTarget(NotificationTarget notificationTarget) {
        return restTemplate.postForEntity(baseURL + "/api/notification/target", notificationTarget, NotificationTarget.class).getBody();
    }

    public Optional<NotificationTarget> getNotificationTargetById(NotificationTargetId notificationTargetId) {
        try {
            ResponseEntity<NotificationTarget> response = restTemplate.getForEntity(
                    baseURL + "/api/notification/target/{id}", NotificationTarget.class, notificationTargetId.getId());
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public void deleteNotificationTarget(NotificationTargetId notificationTargetId) {
        restTemplate.delete(baseURL + "/api/notification/target/{id}", notificationTargetId.getId());
    }

    public PageData<NotificationTarget> getNotificationTargets(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/notification/targets?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<NotificationTarget>>() {
                }, params).getBody();
    }

    public NotificationTemplate saveNotificationTemplate(NotificationTemplate notificationTemplate) {
        return restTemplate.postForEntity(baseURL + "/api/notification/template", notificationTemplate, NotificationTemplate.class).getBody();
    }

    public Optional<NotificationTemplate> getNotificationTemplateById(NotificationTemplateId notificationTemplateId) {
        try {
            ResponseEntity<NotificationTemplate> response = restTemplate.getForEntity(
                    baseURL + "/api/notification/template/{id}", NotificationTemplate.class, notificationTemplateId.getId());
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public void deleteNotificationTemplate(NotificationTemplateId notificationTemplateId) {
        restTemplate.delete(baseURL + "/api/notification/template/{id}", notificationTemplateId.getId());
    }

    public PageData<NotificationTemplate> getNotificationTemplates(NotificationType[] notificationTypes, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        StringBuilder url = new StringBuilder(baseURL).append("/api/notification/templates?").append(getUrlParams(pageLink));
        if (notificationTypes != null && notificationTypes.length > 0) {
            params.put("notificationTypes", listEnumToString(List.of(notificationTypes)));
            url.append("&notificationTypes={notificationTypes}");
        }
        return restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<NotificationTemplate>>() {
                }, params).getBody();
    }

    public NotificationRule saveNotificationRule(NotificationRule notificationRule) {
        return restTemplate.postForEntity(baseURL + "/api/notification/rule", notificationRule, NotificationRule.class).getBody();
    }

    public Optional<NotificationRuleInfo> getNotificationRuleById(NotificationRuleId notificationRuleId) {
        try {
            ResponseEntity<NotificationRuleInfo> response = restTemplate.getForEntity(
                    baseURL + "/api/notification/rule/{id}", NotificationRuleInfo.class, notificationRuleId.getId());
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public void deleteNotificationRule(NotificationRuleId notificationRuleId) {
        restTemplate.delete(baseURL + "/api/notification/rule/{id}", notificationRuleId.getId());
    }

    public PageData<NotificationRuleInfo> getNotificationRules(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/notification/rules?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<NotificationRuleInfo>>() {
                }, params).getBody();
    }

    public AiModel saveAiModel(AiModel aiModel) {
        return restTemplate.postForEntity(baseURL + "/api/ai/model", aiModel, AiModel.class).getBody();
    }

    public Optional<AiModel> getAiModel(AiModelId aiModelId) {
        try {
            ResponseEntity<AiModel> response = restTemplate.getForEntity(
                    baseURL + "/api/ai/model/{aiModelId}", AiModel.class, aiModelId.getId());
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public void deleteAiModel(AiModelId aiModelId) {
        restTemplate.delete(baseURL + "/api/ai/model/{aiModelId}", aiModelId.getId());
    }


    private String getTimeUrlParams(TimePageLink pageLink) {
        String urlParams = getUrlParams(pageLink);
        if (pageLink.getStartTime() != null) {
            urlParams += "&startTime={startTime}";
        }
        if (pageLink.getEndTime() != null) {
            urlParams += "&endTime={endTime}";
        }
        return urlParams;
    }

    private String getUrlParams(PageLink pageLink) {
        String urlParams = "pageSize={pageSize}&page={page}";
        if (!isEmpty(pageLink.getTextSearch())) {
            urlParams += "&textSearch={textSearch}";
        }
        if (pageLink.getSortOrder() != null) {
            urlParams += "&sortProperty={sortProperty}&sortOrder={sortOrder}";
        }
        return urlParams;
    }

    private String getWidgetTypeInfoPageRequestUrlParams(Boolean tenantOnly, Boolean fullSearch,
                                                         DeprecatedFilter deprecatedFilter,
                                                         List<String> widgetTypeList) {
        String urlParams = getTenantOnlyAndFullSearchUrlParams(tenantOnly, fullSearch);
        if (deprecatedFilter != null) {
            urlParams += "&deprecatedFilter={deprecatedFilter}";
        }
        if (!CollectionUtils.isEmpty(widgetTypeList)) {
            urlParams += "&widgetTypeList={widgetTypeList}";
        }
        return urlParams;
    }

    private String getTenantOnlyAndFullSearchUrlParams(Boolean tenantOnly, Boolean fullSearch) {
        String urlParams = "";
        if (tenantOnly != null) {
            urlParams = "&tenantOnly={tenantOnly}";
        }
        if (fullSearch != null) {
            urlParams += "&fullSearch={fullSearch}";
        }
        return urlParams;
    }

    private void addTimePageLinkToParam(Map<String, String> params, TimePageLink pageLink) {
        this.addPageLinkToParam(params, pageLink);
        if (pageLink.getStartTime() != null) {
            params.put("startTime", String.valueOf(pageLink.getStartTime()));
        }
        if (pageLink.getEndTime() != null) {
            params.put("endTime", String.valueOf(pageLink.getEndTime()));
        }
    }

    private void addPageLinkToParam(Map<String, String> params, PageLink pageLink) {
        params.put("pageSize", String.valueOf(pageLink.getPageSize()));
        params.put("page", String.valueOf(pageLink.getPage()));
        if (!isEmpty(pageLink.getTextSearch())) {
            params.put("textSearch", pageLink.getTextSearch());
        }
        if (pageLink.getSortOrder() != null) {
            params.put("sortProperty", pageLink.getSortOrder().getProperty());
            params.put("sortOrder", pageLink.getSortOrder().getDirection().name());
        }
    }

    private void addWidgetInfoFiltersToParams(Boolean tenantOnly, Boolean fullSearch, DeprecatedFilter deprecatedFilter,
                                              List<String> widgetTypeList, Map<String, String> params) {
        addTenantOnlyAndFullSearchToParams(tenantOnly, fullSearch, params);
        if (deprecatedFilter != null) {
            params.put("deprecatedFilter", deprecatedFilter.name());
        }
        if (!CollectionUtils.isEmpty(widgetTypeList)) {
            params.put("widgetTypeList", listToString(widgetTypeList));
        }
    }

    private void addTenantOnlyAndFullSearchToParams(Boolean tenantOnly, Boolean fullSearch, Map<String, String> params) {
        if (tenantOnly != null) {
            params.put("tenantOnly", tenantOnly.toString());
        }
        if (fullSearch != null) {
            params.put("fullSearch", fullSearch.toString());
        }
    }

    private String listToString(List<String> list) {
        return String.join(",", list);
    }

    private String listIdsToString(List<? extends EntityId> list) {
        return listToString(list.stream().map(id -> id.getId().toString()).collect(Collectors.toList()));
    }

    private String listEnumToString(List<? extends Enum> list) {
        return listToString(list.stream().map(Enum::name).collect(Collectors.toList()));
    }

    private HttpEntity<MultiValueMap<String, Object>> createMultipartRequest(String fileName, byte[] fileData, String fileContentType, Map<String, Object> otherParts) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=file; filename=" + fileName);
        if (fileContentType != null) {
            fileMap.add(HttpHeaders.CONTENT_TYPE, fileContentType);
        }
        HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(new ByteArrayResource(fileData), fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.setAll(otherParts);
        body.add("file", fileEntity);
        return new HttpEntity<>(body, headers);
    }

    @Override
    public void close() {
        if (executor.isInitialized()) {
            getExecutor().shutdown();
        }
    }

    @Deprecated
    public Optional<JsonNode> getEntityAttributesById(EntityId entityId, String keys) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", keys);
        try {
            ResponseEntity<JsonNode> telemetryEntity = restTemplate.getForEntity(baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/attributes?keys={keys}", JsonNode.class, params);
            return Optional.ofNullable(telemetryEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Asset> getUserAssets(String assetType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", assetType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/user/assets?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                },
                params).getBody();
    }

    public PageData<AssetInfo> getAllAssetInfos(Boolean includeCustomers, AssetProfileId assetProfileId, PageLink pageLink) {
        URIBuilder builder;
        try {
            builder = new URIBuilder(baseURL);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid base URL: " + baseURL, e);
        }

        builder.appendPath("/api/assetInfos/all");
        builder.addParameter("pageSize", String.valueOf(pageLink.getPageSize()));
        builder.addParameter("page", String.valueOf(pageLink.getPage()));

        if (includeCustomers != null) {
            builder.addParameter("includeCustomers", includeCustomers.toString());
        }
        if (assetProfileId != null) {
            builder.addParameter("assetProfileId", assetProfileId.toString());
        }

        if (pageLink.getTextSearch() != null) {
            builder.addParameter("textSearch", pageLink.getTextSearch());
        }

        if (pageLink.getSortOrder() != null) {
            builder.addParameter("sortProperty", pageLink.getSortOrder().getProperty());
            builder.addParameter("sortOrder", pageLink.getSortOrder().getDirection().name());
        }

        URI uri;
        try {
            uri = builder.build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to construct API URI from base URL and provided params", e);
        }

        return restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<PageData<AssetInfo>>() {}).getBody();
    }

    public PageData<Asset> getAssetsByEntityGroupId(EntityGroupId entityGroupId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityGroupId", entityGroupId.toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/entityGroup/{entityGroupId}/assets?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                },
                params).getBody();
    }

    public JsonNode activateUser(JsonNode activateRequest) {
        return restTemplate.postForEntity(baseURL + "/api/noauth/activate/", activateRequest, JsonNode.class).getBody();
    }

    public Optional<BlobEntityInfo> getBlobEntityInfoById(BlobEntityId blobEntityId) {
        try {
            ResponseEntity<BlobEntityInfo> blobEntityInfo = restTemplate.getForEntity(baseURL + "/api/blobEntity/info/{blobEntityId}", BlobEntityInfo.class, blobEntityId.getId());
            return Optional.ofNullable(blobEntityInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public ResponseEntity<Resource> downloadBlobEntity(BlobEntityId blobEntityId) {
        return restTemplate.exchange(
                baseURL + "/api/blobEntity/{blobEntityId}/download",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<ResponseEntity<Resource>>() {
                },
                blobEntityId.getId()).getBody();
    }

    public void deleteBlobEntity(BlobEntityId blobEntityId) {
        restTemplate.delete(baseURL + "/api/blobEntity/{blobEntityId}", blobEntityId.getId());
    }

    public PageData<BlobEntityInfo> getBlobEntities(String type, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/blobEntities?type={type}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<BlobEntityInfo>>() {
                }, params).getBody();
    }

    public List<BlobEntityInfo> getBlobEntitiesByIds(List<BlobEntityId> blobEntityIds) {
        return restTemplate.exchange(
                baseURL + "/api/blobEntities?blobEntityIds={blobEntityIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<BlobEntityInfo>>() {
                },
                listIdsToString(blobEntityIds)).getBody();
    }

    public Optional<Converter> getConverterById(ConverterId converterId) {
        try {
            ResponseEntity<Converter> converter = restTemplate.getForEntity(baseURL + "/api/converter/{converterId}", Converter.class, converterId.getId());
            return Optional.ofNullable(converter.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Converter saveConverter(Converter converter) {
        return restTemplate.postForEntity(baseURL + "/api/converter", converter, Converter.class).getBody();
    }

    public PageData<Converter> getConverters(PageLink pageLink) {
        return getConverters(pageLink, false);
    }

    public PageData<Converter> getConverters(PageLink pageLink, boolean isEdgeTemplate) {
        Map<String, String> params = new HashMap<>();
        params.put("isEdgeTemplate", Boolean.toString(isEdgeTemplate));
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/converters?isEdgeTemplate={isEdgeTemplate}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Converter>>() {
                },
                params).getBody();
    }

    public void deleteConverter(ConverterId converterId) {
        restTemplate.delete(baseURL + "/api/converter/{converterId}", converterId.getId());
    }

    public Optional<JsonNode> getLatestConverterDebugInput(ConverterId converterId) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.getForEntity(baseURL + "/api/converter/{converterId}/debugIn", JsonNode.class, converterId.getId());
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> getConverterDebugInputForIntegration(String integrationName, IntegrationType integrationType, ConverterType converterType) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("integrationName", integrationName);
            params.put("integrationType", integrationType.name());
            params.put("converterType", converterType.name());
            params.put("converterId", EntityId.NULL_UUID.toString());
            ResponseEntity<JsonNode> jsonNode = restTemplate.exchange(
                    baseURL + "/api/converter/{converterId}/debugIn?integrationName={integrationName}&integrationType={integrationType}&converterType={converterType}",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    JsonNode.class,
                    params);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> testUpLinkConverter(JsonNode inputParams) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/converter/testUpLink", inputParams, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> unwrapRawPayload(JsonNode inputParams, IntegrationType integrationType) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/converter/unwrap/" + integrationType, inputParams, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> testDownLinkConverter(JsonNode inputParams) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/converter/testDownLink", inputParams, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<Converter> getConvertersByIds(List<ConverterId> converterIds) {
        return restTemplate.exchange(
                baseURL + "/api/converters?converterIds={converterIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Converter>>() {
                },
                listIdsToString(converterIds)).getBody();
    }

    public PageData<Customer> getUserCustomers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/user/customers?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Customer>>() {
                },
                params).getBody();
    }

    public PageData<Customer> getCustomersByEntityGroupId(EntityGroupId entityGroupId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityGroupId", entityGroupId.toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/entityGroup/{entityGroupId}/customers?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Customer>>() {
                },
                params).getBody();
    }

    public PageData<CustomMenuInfo> getCustomMenuInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        String url = baseURL + "/api/customMenu/infos?" + getUrlParams(pageLink);

        ResponseEntity<PageData<CustomMenuInfo>> response = restTemplate.exchange(
                url, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<>() {}, params);

        return response.getBody();
    }

    public CustomMenu saveCustomMenu(CustomMenuInfo customMenuInfo, UUID[] ids, Boolean force) {
        Map<String, Object> params = new HashMap<>();
        if (ids != null && ids.length > 0) {
            params.put("assignToList", ids);
        }
        if (force != null) {
            params.put("force", force);
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseURL + "/api/customMenu");

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CustomMenuInfo> requestEntity = new HttpEntity<>(customMenuInfo, headers);
        return restTemplate.postForEntity(builder.toUriString(), requestEntity, CustomMenu.class).getBody();
    }

    public Optional<JsonNode> getCustomTranslation(String localeCode) {
        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(baseURL + "/api/translation/custom/{localeCode}", JsonNode.class, localeCode);
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> getMergedCustomTranslation(String localeCode) {
        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(baseURL + "/api/translation/custom/merged/{localeCode}", JsonNode.class, localeCode);
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public void saveCustomTranslation(String localeCode, JsonNode customTranslationValue) {
        restTemplate.postForEntity(baseURL + "/api/translation/custom/{localeCode}", customTranslationValue, Void.class, localeCode);
    }

    public void deleteCustomTranslation(String localeCode) {
        restTemplate.delete(baseURL + "/api/translation/custom/{localeCode}", localeCode);
    }

    public PageData<DashboardInfo> getUserDashboards(PageLink pageLink, String operation, UserId userId) {
        Map<String, String> params = new HashMap<>();
        params.put("operation", operation);
        params.put("userId", userId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/user/dashboards?operation={operation}&userId={userId}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                },
                params).getBody();
    }

    public PageData<DashboardInfo> getGroupDashboards(EntityGroupId entityGroupId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityGroupId", entityGroupId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/entityGroup/{entityGroupId}/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                },
                params).getBody();
    }

    public List<DashboardInfo> getDashboardsByIds(List<DashboardId> dashboardIds) {
        return restTemplate.exchange(
                baseURL + "/api/dashboards?dashboardIds={dashboardIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<DashboardInfo>>() {
                },
                listIdsToString(dashboardIds)).getBody();
    }

    public void importGroupDashboards(EntityGroupId entityGroupId, List<Dashboard> dashboardList, boolean overwrite) {
        restTemplate.postForLocation(
                baseURL + "/api/entityGroup/{entityGroupId}/dashboards/import?overwrite={overwrite}",
                dashboardList,
                entityGroupId,
                overwrite);
    }

    public List<Dashboard> exportGroupDashboards(EntityGroupId entityGroupId, int limit) {
        return restTemplate.exchange(
                baseURL + "/api/entityGroup/{entityGroupId}/dashboards/export?limit={limit}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Dashboard>>() {
                },
                entityGroupId,
                limit).getBody();
    }

    public PageData<Device> getUserDevices(String deviceType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", deviceType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/user/devices?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                },
                params).getBody();
    }

    public PageData<DeviceInfo> getAllDeviceInfos(Boolean includeCustomers, DeviceProfileId deviceProfileId, Boolean active, PageLink pageLink) {
        URIBuilder builder;
        try {
            builder = new URIBuilder(baseURL);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid base URL: " + baseURL, e);
        }

        builder.appendPath("/api/deviceInfos/all");
        builder.addParameter("pageSize", String.valueOf(pageLink.getPageSize()));
        builder.addParameter("page", String.valueOf(pageLink.getPage()));

        if (includeCustomers != null) {
            builder.addParameter("includeCustomers", includeCustomers.toString());
        }
        if (deviceProfileId != null) {
            builder.addParameter("deviceProfileId", deviceProfileId.toString());
        }
        if (active != null) {
            builder.addParameter("active", active.toString());
        }

        if (pageLink.getTextSearch() != null) {
            builder.addParameter("textSearch", pageLink.getTextSearch());
        }

        if (pageLink.getSortOrder() != null) {
            builder.addParameter("sortProperty", pageLink.getSortOrder().getProperty());
            builder.addParameter("sortOrder", pageLink.getSortOrder().getDirection().name());
        }

        URI uri;
        try {
            uri = builder.build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to construct API URI from base URL and provided params", e);
        }

        return restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<PageData<DeviceInfo>>() {}).getBody();
    }

    public PageData<DashboardInfo> getAllDashboards(PageLink pageLink, boolean includeCustomers) {
        URIBuilder builder;
        try {
            builder = new URIBuilder(baseURL);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid base URL: " + baseURL, e);
        }

        builder.appendPath("/api/dashboards/all");
        builder.addParameter("pageSize", String.valueOf(pageLink.getPageSize()));
        builder.addParameter("page", String.valueOf(pageLink.getPage()));
        builder.addParameter("includeCustomers", String.valueOf(includeCustomers));

        if (pageLink.getTextSearch() != null) {
            builder.addParameter("textSearch", pageLink.getTextSearch());
        }

        if (pageLink.getSortOrder() != null) {
            builder.addParameter("sortProperty", pageLink.getSortOrder().getProperty());
            builder.addParameter("sortOrder", pageLink.getSortOrder().getDirection().name());
        }

        URI uri;
        try {
            uri = builder.build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to construct API URI from base URL and provided params", e);
        }

        return restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<PageData<DashboardInfo>>() {}).getBody();
    }

    @Deprecated
    public Device createDevice(String name, String type, String label) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        device.setLabel(label);
        return doCreateDevice(device, null);
    }

    @Deprecated
    public Device createDevice(String name, String type, String label, CustomerId customerId) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        device.setLabel(label);
        device.setCustomerId(customerId);
        return doCreateDevice(device, null);
    }

    public PageData<Device> getDevicesByEntityGroupId(EntityGroupId entityGroupId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityGroupId", entityGroupId.toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/entityGroup/{entityGroupId}/devices?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                },
                params).getBody();
    }

    public Optional<EntityGroupInfo> getEntityGroupById(EntityGroupId entityGroupId) {
        try {
            ResponseEntity<EntityGroupInfo> entityGroupInfo = restTemplate.getForEntity(baseURL + "/api/entityGroup/{entityGroupId}", EntityGroupInfo.class, entityGroupId.getId());
            return Optional.ofNullable(entityGroupInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public EntityGroupInfo saveEntityGroup(EntityGroup entityGroup) {
        return restTemplate.postForEntity(baseURL + "/api/entityGroup", entityGroup, EntityGroupInfo.class).getBody();
    }

    public void deleteEntityGroup(EntityGroupId entityGroupId) {
        restTemplate.delete(baseURL + "/api/entityGroup/{entityGroupId}", entityGroupId.getId());
    }

    public List<EntityGroupInfo> getEntityGroupsByType(EntityType groupType) {
        return restTemplate.exchange(
                baseURL + "/api/entityGroups/{groupType}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityGroupInfo>>() {
                },
                groupType.name()).getBody();
    }

    public List<EntityGroupInfo> getEntityGroupsByOwnerAndType(EntityId ownerId, EntityType groupType) {
        return restTemplate.exchange(
                baseURL + "/api/entityGroups/{ownerType}/{ownerId}/{groupType}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityGroupInfo>>() {
                },
                ownerId.getEntityType().name(),
                ownerId.getId(),
                groupType.name()).getBody();
    }

    public Optional<EntityGroupInfo> getEntityGroupAllByOwnerAndType(EntityId ownerId, EntityType groupType) {
        try {
            ResponseEntity<EntityGroupInfo> entityGroupInfo =
                    restTemplate
                            .getForEntity(
                                    baseURL + "/api/entityGroup/all/{ownerType}/{ownerId}/{groupType}",
                                    EntityGroupInfo.class,
                                    ownerId.getEntityType().name(),
                                    ownerId.getId(),
                                    groupType.name());
            return Optional.ofNullable(entityGroupInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<EntityGroupInfo> getEntityGroupInfoByOwnerAndNameAndType(EntityId ownerId, EntityType groupType, String groupName) {
        try {
            EntityGroupInfo entity = restTemplate.getForEntity(
                    baseURL + "/api/entityGroup/{ownerType}/{ownerId}/{groupType}/{groupName}"
                    , EntityGroupInfo.class,
                    ownerId.getEntityType().name(),
                    ownerId.getId(),
                    groupType.name(),
                    groupName
            ).getBody();
            return Optional.ofNullable(entity);
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public void addEntitiesToEntityGroup(EntityGroupId entityGroupId, List<EntityId> entityIds) {
        Object[] entityIdsArray = entityIds.stream().map(entityId -> entityId.getId().toString()).toArray();
        restTemplate.postForEntity(baseURL + "/api/entityGroup/{entityGroupId}/addEntities", entityIdsArray, Object.class, entityGroupId.getId());
    }

    public void removeEntitiesFromEntityGroup(EntityGroupId entityGroupId, List<EntityId> entityIds) {
        Object[] entityIdsArray = entityIds.stream().map(entityId -> entityId.getId().toString()).toArray();
        restTemplate.postForEntity(baseURL + "/api/entityGroup/{entityGroupId}/deleteEntities", entityIdsArray, Object.class, entityGroupId.getId());
    }

    public Optional<ShortEntityView> getGroupEntity(EntityGroupId entityGroupId, EntityId entityId) {
        try {
            ResponseEntity<ShortEntityView> shortEntityView =
                    restTemplate.getForEntity(baseURL + "/api/entityGroup/{entityGroupId}/{entityId}", ShortEntityView.class, entityGroupId.getId(), entityId.getId());
            return Optional.ofNullable(shortEntityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<ShortEntityView> getEntities(EntityGroupId entityGroupId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityGroupId", entityGroupId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/entityGroup/{entityGroupId}/entities?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<ShortEntityView>>() {
                }, params).getBody();
    }

    public List<EntityGroupId> getEntityGroupsForEntity(EntityId entityId) {
        return restTemplate.exchange(
                baseURL + "/api/entityGroups/{entityType}/{entityId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityGroupId>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId()).getBody();
    }

    public List<EntityGroup> getEntityGroupsByIds(List<EntityGroupId> entityGroupIds) {
        return restTemplate.exchange(
                baseURL + "/api/entityGroups?entityGroupIds={entityGroupIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityGroup>>() {
                },
                listIdsToString(entityGroupIds)).getBody();
    }

    public PageData<ContactBased<?>> getOwners(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/owners?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<ContactBased<?>>>() {
                },
                params).getBody();
    }

    public void makeEntityGroupPublic(EntityGroupId entityGroupId) {
        restTemplate.postForLocation(baseURL + "/api/entityGroup/{entityGroupId}/makePublic", null, entityGroupId.getId());
    }

    public void makeEntityGroupPrivate(EntityGroupId entityGroupId) {
        restTemplate.postForLocation(baseURL + "/api/entityGroup/{entityGroupId}/makePrivate", null, entityGroupId);
    }

    public void shareEntityGroup(EntityGroupId entityGroupId, ShareGroupRequest shareGroupRequest) {
        restTemplate.postForLocation(baseURL + "/api/entityGroup/{entityGroupId}/share", shareGroupRequest, entityGroupId);
    }

    public void shareEntityGroupToChildOwnerUserGroup(EntityGroupId entityGroupId, EntityGroupId userGroupId, RoleId roleId) {
        restTemplate.postForLocation(
                baseURL + "/api/entityGroup/{entityGroupId}/{userGroupId}/{roleId}/share",
                null,
                entityGroupId,
                userGroupId,
                roleId);
    }

    public PageData<EntityView> getUserEntityViews(String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", entityViewType);
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/user/entityViews?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                },
                params).getBody();
    }

    public PageData<EntityView> getEntityViewsByEntityGroupId(EntityGroupId entityGroupId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityGroupId", entityGroupId.toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/entityGroup/{entityGroupId}/entityViews?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                },
                params).getBody();
    }

    public List<EntityView> getEntityViewsByIds(List<EntityViewId> entityViewIds) {
        return restTemplate.exchange(
                baseURL + "/api/entityViews?entityViewIds={entityViewIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityView>>() {
                },
                listIdsToString(entityViewIds)).getBody();
    }

    public Optional<GroupPermission> getGroupPermissionById(GroupPermissionId groupPermissionId) {
        try {
            ResponseEntity<GroupPermission> groupPermission =
                    restTemplate.getForEntity(
                            baseURL + "/api/groupPermission/{groupPermissionId}",
                            GroupPermission.class,
                            groupPermissionId.getId());
            return Optional.ofNullable(groupPermission.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<GroupPermissionInfo> getGroupPermissionInfoById(GroupPermissionId groupPermissionId, boolean isUserGroup) {
        try {
            ResponseEntity<GroupPermissionInfo> groupPermission =
                    restTemplate.getForEntity(
                            baseURL + "/api/groupPermission/info/{groupPermissionId}?isUserGroup={isUserGroup}",
                            GroupPermissionInfo.class,
                            groupPermissionId,
                            isUserGroup);
            return Optional.ofNullable(groupPermission.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public GroupPermission saveGroupPermission(GroupPermission groupPermission) {
        return restTemplate.postForEntity(baseURL + "/api/groupPermission", groupPermission, GroupPermission.class).getBody();
    }

    public void deleteGroupPermission(GroupPermissionId groupPermissionId) {
        restTemplate.delete(baseURL + "/api/groupPermission/{groupPermissionId}", groupPermissionId.getId());
    }

    public List<GroupPermissionInfo> getUserGroupPermissions(EntityGroupId userGroupId) {
        return restTemplate.exchange(
                baseURL + "/api/userGroup/{userGroupId}/groupPermissions",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<GroupPermissionInfo>>() {
                },
                userGroupId.getId()).getBody();
    }

    public List<GroupPermissionInfo> loadUserGroupPermissionInfos(List<GroupPermission> permissions) {
        return restTemplate.exchange(
                baseURL + "/api/userGroup/groupPermissions/info",
                HttpMethod.POST,
                new HttpEntity<>(permissions),
                new ParameterizedTypeReference<List<GroupPermissionInfo>>() {
                }).getBody();
    }

    public List<GroupPermissionInfo> getEntityGroupPermissions(EntityGroupId entityGroupId) {
        return restTemplate.exchange(
                baseURL + "/api/entityGroup/{entityGroupId}/groupPermissions",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<GroupPermissionInfo>>() {
                },
                entityGroupId.getId()).getBody();
    }

    public Optional<Integration> getIntegrationById(IntegrationId integrationId) {
        try {
            ResponseEntity<Integration> integration = restTemplate.getForEntity(baseURL + "/api/integration/{integrationId}", Integration.class, integrationId.getId());
            return Optional.ofNullable(integration.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Integration> getIntegrationByRoutingKey(String routingKey) {
        try {
            ResponseEntity<Integration> integration = restTemplate.getForEntity(baseURL + "/api/integration/routingKey/{routingKey}", Integration.class, routingKey);
            return Optional.ofNullable(integration.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Integration saveIntegration(Integration integration) {
        return restTemplate.postForEntity(baseURL + "/api/integration", integration, Integration.class).getBody();
    }


    public PageData<Integration> getIntegrations(PageLink pageLink) {
        return getIntegrations(pageLink, false);
    }

    public PageData<Integration> getIntegrations(PageLink pageLink, boolean isEdgeTemplate) {
        Map<String, String> params = new HashMap<>();
        params.put("isEdgeTemplate", Boolean.toString(isEdgeTemplate));
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/integrations?isEdgeTemplate={isEdgeTemplate}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Integration>>() {
                }, params).getBody();
    }

    public PageData<IntegrationInfo> getIntegrationInfos(PageLink pageLink, boolean isEdgeTemplate) {
        Map<String, String> params = new HashMap<>();
        params.put("isEdgeTemplate", Boolean.toString(isEdgeTemplate));
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/integrationInfos?isEdgeTemplate={isEdgeTemplate}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<IntegrationInfo>>() {
                }, params).getBody();
    }

    public void checkIntegrationConnection(Integration integration) {
        restTemplate.postForLocation(baseURL + "/api/integration/check", integration);
    }

    public void deleteIntegration(IntegrationId integrationId) {
        restTemplate.delete(baseURL + "/api/integration/{integrationId}", integrationId);
    }

    public List<Integration> getIntegrationsByIds(List<IntegrationId> integrationIds) {
        return restTemplate.exchange(
                baseURL + "/api/integrations?integrationIds={integrationIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Integration>>() {
                },
                listIdsToString(integrationIds)).getBody();
    }

    public Optional<Integration> assignIntegrationToEdge(EdgeId edgeId, IntegrationId integrationId) {
        try {
            ResponseEntity<Integration> integration = restTemplate.postForEntity(baseURL + "/api/edge/{edgeId}/integration/{integrationId}",
                    null, Integration.class, edgeId.getId(), integrationId.getId());
            return Optional.ofNullable(integration.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Integration> unassignIntegrationFromEdge(EdgeId edgeId, IntegrationId integrationId) {
        try {
            ResponseEntity<Integration> integration = restTemplate.exchange(baseURL + "/api/edge/{edgeId}/integration/{integrationId}",
                    HttpMethod.DELETE, HttpEntity.EMPTY, Integration.class, edgeId.getId(), integrationId.getId());
            return Optional.ofNullable(integration.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Integration> getEdgeIntegrations(EdgeId edgeId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/integrations?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Integration>>() {
                }, params).getBody();
    }

    public PageData<IntegrationInfo> getEdgeIntegrationInfos(EdgeId edgeId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("edgeId", edgeId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/edge/{edgeId}/integrationInfos?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<IntegrationInfo>>() {
                }, params).getBody();
    }

    public String findMissingAttributesForAllRelatedEdges(IntegrationId integrationId) {
        return restTemplate.getForEntity(baseURL + "/api/edge/integration/{integrationId}/allMissingAttributes", String.class, integrationId.getId()).getBody();
    }

    public String findMissingAttributesForEdge(EdgeId edgeId, List<IntegrationId> integrationIds) {
        return restTemplate.getForEntity(baseURL + "/api/edge/integration/{edgeId}/missingAttributes?integrationIds={integrationIds}",
                String.class, edgeId.getId(), listIdsToString(integrationIds)).getBody();
    }

    public void changeOwnerToTenant(EntityId ownerId, EntityId entityId) {
        changeOwnerToTenant(ownerId, entityId, new String[]{});
    }

    public void changeOwnerToTenant(EntityId ownerId, EntityId entityId, String[] strEntityGroupIds) {
        restTemplate.postForEntity(baseURL + "/api/owner/TENANT/{ownerId}/{entityType}/{entityId}", strEntityGroupIds, Object.class, ownerId.getId(), entityId.getEntityType(), entityId.getId());
    }

    public void changeOwnerToCustomer(EntityId ownerId, EntityId entityId) {
        changeOwnerToCustomer(ownerId, entityId, new String[]{});
    }

    public void changeOwnerToCustomer(EntityId ownerId, EntityId entityId, String[] strEntityGroupIds) {
        restTemplate.postForEntity(baseURL + "/api/owner/CUSTOMER/{ownerId}/{entityType}/{entityId}", strEntityGroupIds, Object.class, ownerId.getId(), entityId.getEntityType(), entityId.getId());
    }

    public JsonNode downloadDashboardReport(DashboardId dashboardId, JsonNode reportParams) {
        return restTemplate.exchange(
                baseURL + "/api/report/{dashboardId}/download",
                HttpMethod.POST,
                new HttpEntity<>(reportParams),
                new ParameterizedTypeReference<JsonNode>() {
                },
                dashboardId.getId()).getBody();
    }

    public JsonNode downloadTestReport(DashboardReportConfig reportConfig, String reportsServerEndpointUrl) {
        return restTemplate.exchange(
                baseURL + "/api/report/test?reportsServerEndpointUrl={reportsServerEndpointUrl}",
                HttpMethod.POST,
                new HttpEntity<>(reportConfig),
                new ParameterizedTypeReference<JsonNode>() {
                },
                reportsServerEndpointUrl).getBody();
    }

    public Optional<Role> getRoleById(RoleId roleId) {
        try {
            ResponseEntity<Role> role = restTemplate.getForEntity(baseURL + "/api/role/{roleId}", Role.class, roleId.getId());
            return Optional.ofNullable(role.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Role saveRole(Role role) {
        return restTemplate.postForEntity(baseURL + "/api/role", role, Role.class).getBody();
    }

    public void deleteRole(RoleId roleId) {
        restTemplate.delete(baseURL + "/api/role/{roleId}", roleId.getId());
    }

    public PageData<Role> getRoles(RoleType type, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type.name());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/roles?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Role>>() {
                }, params).getBody();
    }

    public List<Role> getRolesByIds(List<RoleId> roleIds) {
        return restTemplate.exchange(
                baseURL + "/api/roles?roleIds={roleIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Role>>() {
                },
                listIdsToString(roleIds)).getBody();
    }

    public Role createGroupRole(String roleName, List<Operation> operations) {
        Role role = new Role();
        role.setName(roleName);
        role.setType(RoleType.GROUP);
        ArrayNode permissions = JacksonUtil.newArrayNode();
        operations.stream().map(Operation::name).forEach(permissions::add);
        role.setPermissions(permissions);
        return saveRole(role);
    }

    public JsonNode handleRuleEngineRequest(JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rule-engine",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                }).getBody();
    }

    public JsonNode handleRuleEngineRequest(EntityId entityId, JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rule-engine/{entityType}/{entityId}",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                },
                entityId.getEntityType(),
                entityId.getId()).getBody();
    }

    public JsonNode handleRuleEngineRequest(EntityId entityId, int timeout, JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rule-engine/{entityType}/{entityId}/{timeout}",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                },
                entityId.getEntityType(),
                entityId.getId(),
                timeout).getBody();
    }

    public JsonNode handleRuleEngineRequest(EntityId entityId, String queueName, int timeout, JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rule-engine/{entityType}/{entityId}/{queueName}/{timeout}",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                },
                entityId.getEntityType(),
                entityId.getId(),
                queueName,
                timeout).getBody();
    }

    public Optional<SchedulerEventInfo> getSchedulerEventInfoById(SchedulerEventId schedulerEventId) {
        try {
            ResponseEntity<SchedulerEventInfo> schedulerEventInfo = restTemplate.getForEntity(baseURL + "/api/schedulerEvent/info/{schedulerEventId}", SchedulerEventInfo.class, schedulerEventId.getId());
            return Optional.ofNullable(schedulerEventInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<SchedulerEvent> getSchedulerEventById(SchedulerEventId schedulerEventId) {
        try {
            ResponseEntity<SchedulerEvent> schedulerEvent = restTemplate.getForEntity(baseURL + "/api/schedulerEvent/{schedulerEventId}", SchedulerEvent.class, schedulerEventId.getId());
            return Optional.ofNullable(schedulerEvent.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public SchedulerEvent saveSchedulerEvent(SchedulerEvent schedulerEvent) {
        return restTemplate.postForEntity(baseURL + "/api/schedulerEvent", schedulerEvent, SchedulerEvent.class).getBody();
    }

    public void deleteSchedulerEvent(SchedulerEventId schedulerEventId) {
        restTemplate.delete(baseURL + "/api/schedulerEvent/{schedulerEventId}", schedulerEventId.getId());
    }

    public List<SchedulerEventInfo> getSchedulerEvents(String type) {
        return restTemplate.exchange(
                baseURL + "/api/schedulerEvents&type={type}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<SchedulerEventInfo>>() {
                },
                type).getBody();
    }

    public List<SchedulerEventInfo> getSchedulerEventsByIds(List<SchedulerEventId> schedulerEventIds) {
        return restTemplate.exchange(
                baseURL + "/api/schedulerEvents?schedulerEventIds={schedulerEventIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<SchedulerEventInfo>>() {
                },
                listIdsToString(schedulerEventIds)).getBody();
    }

    public SecretInfo saveSecret(Secret secret) {
        return restTemplate.postForEntity(baseURL + "/api/secret", secret, SecretInfo.class).getBody();
    }

    public SecretInfo updateSecretDescription(SecretId secretId, String description) {
        return restTemplate.exchange(
                baseURL + "/api/secret/{id}/description",
                HttpMethod.PUT,
                new HttpEntity<>(description),
                SecretInfo.class,
                secretId.getId()).getBody();
    }

    public SecretInfo updateSecretValue(SecretId secretId, String value) {
        return restTemplate.exchange(
                baseURL + "/api/secret/{id}/value",
                HttpMethod.PUT,
                new HttpEntity<>(value),
                SecretInfo.class,
                secretId.getId()).getBody();
    }

    public ResponseEntity<TbSecretDeleteResult> deleteSecret(SecretId secretId) {
        return restTemplate.exchange(
                baseURL + "/api/secret/{id}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {},
                secretId.getId());
    }

    public PageData<SecretInfo> getSecretInfos(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/secrets?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<SecretInfo>>() {},
                params).getBody();
    }

    public List<String> getSecretNames() {
        return restTemplate.exchange(
                        baseURL + "/api/secret/names",
                        HttpMethod.GET,
                        HttpEntity.EMPTY,
                        new ParameterizedTypeReference<List<String>>() {})
                .getBody();
    }

    public Optional<SecretInfo> getSecretInfoById(UUID secretId) {
        try {
            ResponseEntity<SecretInfo> secretInfo = restTemplate.getForEntity(baseURL + "/api/secret/{id}/info", SecretInfo.class, secretId);
            return Optional.ofNullable(secretInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<SecretInfo> getSecretInfoByName(String name) {
        try {
            ResponseEntity<SecretInfo> secretInfo = restTemplate.getForEntity(baseURL + "/api/secret?name={name}", SecretInfo.class, name);
            return Optional.ofNullable(secretInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public SelfRegistrationParams saveSelfRegistrationParams(SelfRegistrationParams selfRegistrationParams) {
        return restTemplate.postForEntity(baseURL + "/api/selfRegistration/selfRegistrationParams", selfRegistrationParams, SelfRegistrationParams.class).getBody();
    }

    public Optional<SelfRegistrationParams> getSelfRegistrationParams() {
        try {
            ResponseEntity<SelfRegistrationParams> selfRegistrationParams = restTemplate.getForEntity(baseURL + "/api/selfRegistration/selfRegistrationParams}", SelfRegistrationParams.class);
            return Optional.ofNullable(selfRegistrationParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<SignUpSelfRegistrationParams> getSignUpSelfRegistrationParams() {
        try {
            ResponseEntity<SignUpSelfRegistrationParams> selfRegistrationParams = restTemplate.getForEntity(baseURL + "/api/noauth/selfRegistration/signUpSelfRegistrationParams", SignUpSelfRegistrationParams.class);
            return Optional.ofNullable(selfRegistrationParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public String getPrivacyPolicy() {
        return restTemplate.getForEntity(baseURL + "/api/noauth/selfRegistration/privacyPolicy", String.class).getBody();
    }

    public SignUpResult signUp(SignUpRequest signUpRequest) {
        return restTemplate.postForEntity(baseURL + "/api/noauth/signup", signUpRequest, SignUpResult.class).getBody();
    }


    public void resendEmailActivation(String email) {
        restTemplate.postForEntity(baseURL + "/api/noauth/resendEmailActivation?email={email}", null, Object.class, email);
    }

    public ResponseEntity<String> activateEmail(String emailCode) {
        return restTemplate.exchange(
                baseURL + "/api/noauth/activateEmail?emailCode={emailCode}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<ResponseEntity<String>>() {
                },
                emailCode).getBody();
    }

    public Optional<JsonNode> activateUserByEmailCode(String emailCode) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/noauth/activateByEmailCode?emailCode={emailCode}", null, JsonNode.class, emailCode);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Boolean privacyPolicyAccepted() {
        return restTemplate.getForEntity(baseURL + "/api/signup/privacyPolicyAccepted", Boolean.class).getBody();
    }

    public Optional<JsonNode> acceptPrivacyPolicy() {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/signup/acceptPrivacyPolicy", null, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public Optional<JsonNode> getLatestTimeseriesAsOptionalJson(EntityId entityId, String keys) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", keys);
        try {
            ResponseEntity<JsonNode> currentUserResponceEntity = restTemplate.getForEntity(baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries?keys={keys}", JsonNode.class, params);
            return Optional.ofNullable(currentUserResponceEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<Tenant> getTenantsByIds(List<TenantId> tenantIds) {
        return restTemplate.exchange(
                baseURL + "/api/tenants?tenantIds={tenantIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Tenant>>() {
                },
                listIdsToString(tenantIds)).getBody();
    }

    public PageData<User> getAllCustomerUsers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/customer/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                },
                params).getBody();
    }

    public PageData<User> getUserUsers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/user/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                },
                params).getBody();
    }

    public List<User> getUsersByIds(List<UserId> userIds) {
        return restTemplate.exchange(
                baseURL + "/api/users?userIds={userIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<User>>() {
                },
                listIdsToString(userIds)).getBody();
    }

    public Optional<AllowedPermissionsInfo> getAllowedPermissions() {
        try {
            ResponseEntity<AllowedPermissionsInfo> allowedPermissionsInfo = restTemplate.getForEntity(baseURL + "/api/permissions/allowedPermissions", AllowedPermissionsInfo.class);
            return Optional.ofNullable(allowedPermissionsInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<WhiteLabelingParams> getWhiteLabelParams(String logoImageChecksum, String faviconChecksum) {
        try {
            ResponseEntity<WhiteLabelingParams> whiteLabelingParams =
                    restTemplate.getForEntity(
                            baseURL + "/api/whiteLabel/whiteLabelParams?logoImageChecksum={logoImageChecksum}&faviconChecksum={faviconChecksum}",
                            WhiteLabelingParams.class,
                            logoImageChecksum, faviconChecksum);
            return Optional.ofNullable(whiteLabelingParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<LoginWhiteLabelingParams> getLoginWhiteLabelParams(String logoImageChecksum, String faviconChecksum) {
        try {
            ResponseEntity<LoginWhiteLabelingParams> loginWhiteLabelingParams =
                    restTemplate.getForEntity(
                            baseURL + "/api/noauth/whiteLabel/loginWhiteLabelParams?logoImageChecksum={logoImageChecksum}&faviconChecksum={faviconChecksum}",
                            LoginWhiteLabelingParams.class,
                            logoImageChecksum,
                            faviconChecksum);
            return Optional.ofNullable(loginWhiteLabelingParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<WhiteLabelingParams> getCurrentWhiteLabelParams() {
        try {
            ResponseEntity<WhiteLabelingParams> whiteLabelingParams =
                    restTemplate.getForEntity(baseURL + "/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);
            return Optional.ofNullable(whiteLabelingParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<LoginWhiteLabelingParams> getCurrentLoginWhiteLabelParams() {
        try {
            ResponseEntity<LoginWhiteLabelingParams> loginWhiteLabelingParams =
                    restTemplate.getForEntity(baseURL + "/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
            return Optional.ofNullable(loginWhiteLabelingParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public WhiteLabelingParams saveWhiteLabelParams(WhiteLabelingParams whiteLabelingParams) {
        return restTemplate.postForEntity(baseURL + "/api/whiteLabel/whiteLabelParams",
                whiteLabelingParams,
                WhiteLabelingParams.class).getBody();
    }

    public void deleteWhiteLabelParams() {
        restTemplate.delete(baseURL + "/api/whiteLabel/currentWhiteLabelParams");
    }

    public LoginWhiteLabelingParams saveLoginWhiteLabelParams(LoginWhiteLabelingParams loginWhiteLabelingParams) {
        return restTemplate.postForEntity(
                baseURL + "/api/whiteLabel/loginWhiteLabelParams",
                loginWhiteLabelingParams,
                LoginWhiteLabelingParams.class).getBody();
    }

    public void deleteLoginWhiteLabelParams() {
        restTemplate.delete(baseURL + "/api/whiteLabel/currentLoginWhiteLabelParams");
    }

    public WhiteLabelingParams previewWhiteLabelParams(WhiteLabelingParams whiteLabelingParams) {
        return restTemplate.postForEntity(
                baseURL + "/api/whiteLabel/previewWhiteLabelParams",
                whiteLabelingParams,
                WhiteLabelingParams.class).getBody();
    }

    public Boolean isWhiteLabelingAllowed() {
        return restTemplate.getForEntity(baseURL + "/api/whiteLabel/isWhiteLabelingAllowed", Boolean.class).getBody();
    }

    public Boolean isCustomerWhiteLabelingAllowed() {
        return restTemplate.getForEntity(
                baseURL + "/api/whiteLabel/isCustomerWhiteLabelingAllowed",
                Boolean.class).getBody();
    }

    public String getLoginThemeCss(PaletteSettings paletteSettings, boolean darkForeground) {
        return restTemplate.postForEntity(
                baseURL + "/api/whiteLabel/appThemeCss?darkForeground={darkForeground}",
                paletteSettings, String.class, darkForeground).getBody();
    }

    public String getAppThemeCss(PaletteSettings paletteSettings) {
        return restTemplate.postForEntity(
                baseURL + "/api/whiteLabel/appThemeCss",
                paletteSettings, String.class).getBody();
    }

    public ReportTemplate findReportTemplate(ReportTemplateId templateId) {
        try {
            ResponseEntity<ReportTemplate> reportTemplate =
                    restTemplate.getForEntity(baseURL + "/api/reportTemplate/{reportTemplateId}", ReportTemplate.class, templateId.getId());
            return reportTemplate.getBody();
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            } else {
                throw exception;
            }
        }
    }

    public ReportTemplate saveReportTemplate(ReportTemplate reportTemplate) {
        return restTemplate.postForEntity(baseURL + "/api/reportTemplate", reportTemplate, ReportTemplate.class).getBody();
    }

    public void deleteReportTemplate(ReportTemplateId reportTemplateId) {
        restTemplate.delete(baseURL + "/api/reportTemplate/{reportTemplateId}", reportTemplateId.getId());
    }

    public Report createReport(Report report, byte[] data) {
        HttpEntity<MultiValueMap<String, Object>> request = createMultipartRequest(report.getName(), data, report.getFormat().getContentType(), Map.of(
                "info", JacksonUtil.toString(report)
        ));
        return restTemplate.postForObject(baseURL + "/api/v2/report", request, Report.class);
    }

    public Optional<Report> getReportById(ReportId reportId) {
        try {
            ResponseEntity<Report> report = restTemplate.getForEntity(baseURL + "/api/v2/report/{reportId}", Report.class, reportId.getId());
            return Optional.ofNullable(report.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public byte[] downloadReport(ReportId reportId) {
        return restTemplate.getForObject(baseURL + "/api/v2/report/{reportId}/download", byte[].class, reportId.getId());
    }

    public void deleteReport(ReportId reportId) {
        restTemplate.delete(baseURL + "/api/v2/report/{reportId}", reportId.getId());
    }

    public PageData<Report> getReports(Boolean includeCustomers, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        String urlSecondPart = "/api/v2/reports?";
        if (includeCustomers != null) {
            params.put("includeCustomers", String.valueOf(includeCustomers));
            urlSecondPart += "includeCustomers={includeCustomers}&";
        }
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + urlSecondPart + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Report>>() {
                },
                params).getBody();
    }

    public List<ReportInfo> getReportInfosByIds(List<ReportId> reportIds) {
        return restTemplate.exchange(
                baseURL + "/api/v2/reportInfos?strReportIds={strReportIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<ReportInfo>>() {
                },
                Map.of("strReportIds", listIdsToString(reportIds))).getBody();
    }

    public PageData<ReportInfo> getReportInfos(UUID reportTemplateId, UUID userId, Boolean includeCustomers, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        String urlSecondPart = "/api/v2/reportInfos/all?";
        if (reportTemplateId != null) {
            params.put("reportTemplateId", reportTemplateId.toString());
            urlSecondPart += "reportTemplateId={reportTemplateId}&";
        }
        if (userId != null) {
            params.put("userId", userId.toString());
            urlSecondPart += "userId={userId}&";
        }
        if (includeCustomers != null) {
            params.put("includeCustomers", String.valueOf(includeCustomers));
            urlSecondPart += "includeCustomers={includeCustomers}&";
        }
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + urlSecondPart + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<ReportInfo>>() {
                },
                params).getBody();
    }

    public Job requestReport(ReportRequest reportRequest) {
        return restTemplate.postForEntity(baseURL + "/api/v2/report/request", reportRequest, Job.class).getBody();
    }

    public ReportTemplateInfo getReportTemplateInfoById(ReportTemplateId reportTemplateId) {
        try {
            ResponseEntity<ReportTemplateInfo> info = restTemplate.getForEntity(
                    baseURL + "/api/reportTemplate/info/{reportTemplateId}", ReportTemplateInfo.class, reportTemplateId.getId());
            return info.getBody();
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            } else {
                throw exception;
            }
        }
    }

    public PageData<ReportTemplateInfo> getAllReportTemplateInfos(String[] typeList, String[] formatList, Boolean includeCustomers, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        String urlSecondPart = "/api/reportTemplateInfos/all?";
        if (typeList != null && typeList.length > 0) {
            params.put("typeList", String.join(",", typeList));
            urlSecondPart += "typeList={typeList}&";
        }
        if (formatList != null && formatList.length > 0) {
            params.put("formatList", String.join(",", formatList));
            urlSecondPart += "formatList={formatList}&";
        }
        if (includeCustomers != null) {
            params.put("includeCustomers", String.valueOf(includeCustomers));
            urlSecondPart += "includeCustomers={includeCustomers}&";
        }
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + urlSecondPart + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<ReportTemplateInfo>>() {
                },
                params).getBody();
    }

    public List<ReportTemplateInfo> getReportTemplatesByIds(List<ReportTemplateId> reportTemplateIds) {
        return restTemplate.exchange(
                baseURL + "/api/reportTemplates?reportTemplateIds={reportTemplateIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<ReportTemplateInfo>>() {
                },
                Map.of("reportTemplateIds", listIdsToString(reportTemplateIds))).getBody();
    }

    public Optional<Job> getJobById(JobId jobId) {
        try {
            ResponseEntity<Job> job = restTemplate.getForEntity(baseURL + "/api/job/{id}", Job.class, jobId.getId());
            return Optional.ofNullable(job.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Job> getJobs(List<JobType> types, List<JobStatus> statuses, List<UUID> entities,
                                 Long startTime, Long endTime, Boolean includeCustomers, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        String urlSecondPart = "/api/jobs?";
        if (!CollectionUtils.isEmpty(types)) {
            params.put("types", listEnumToString(types));
            urlSecondPart += "types={types}&";
        }
        if (!CollectionUtils.isEmpty(statuses)) {
            params.put("statuses", listEnumToString(statuses));
            urlSecondPart += "statuses={statuses}&";
        }
        if (!CollectionUtils.isEmpty(entities)) {
            params.put("entities", entities.stream().map(UUID::toString).collect(joining(",")));
            urlSecondPart += "entities={entities}&";
        }
        if (startTime != null) {
            params.put("startTime", String.valueOf(startTime));
            urlSecondPart += "startTime={startTime}&";
        }
        if (endTime != null) {
            params.put("endTime", String.valueOf(endTime));
            urlSecondPart += "endTime={endTime}&";
        }
        if (includeCustomers != null) {
            params.put("includeCustomers", String.valueOf(includeCustomers));
            urlSecondPart += "includeCustomers={includeCustomers}&";
        }
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + urlSecondPart + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Job>>() {
                },
                params).getBody();
    }

    public void cancelJob(JobId jobId) {
        restTemplate.postForObject(baseURL + "/api/job/{id}/cancel", null, Void.class, jobId.getId());
    }

    public void reprocessJob(JobId jobId) {
        restTemplate.postForObject(baseURL + "/api/job/{id}/reprocess", null, Void.class, jobId.getId());
    }

    public void deleteJob(JobId jobId) {
        restTemplate.delete(baseURL + "/api/job/{id}", jobId.getId());
    }

    @SneakyThrows
    private ExecutorService getExecutor() {
        return executor.get();
    }

}
