// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeAssignments;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.profile.EdgeAssignmentsService;
import org.thingsboard.server.service.entitiy.edge.profile.TbEdgeProfileService;

import static org.thingsboard.server.controller.ControllerConstants.EDGE_PROFILE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class EdgeProfileController extends BaseController {

    static final String EDGE_PROFILE_ID = "edgeProfileId";

    private final TbEdgeProfileService tbEdgeProfileService;
    private final EdgeAssignmentsService edgeAssignmentsService;

    @ApiOperation(value = "Get Edge Profile (getEdgeProfileById)",
            notes = "Fetch the Edge Profile object based on the provided Edge Profile Id." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/edgeProfile/{edgeProfileId}")
    public EdgeProfile getEdgeProfileById(@Parameter(description = EDGE_PROFILE_ID_PARAM_DESCRIPTION, required = true)
                                          @PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId) throws ThingsboardException {
        checkParameter(EDGE_PROFILE_ID, strEdgeProfileId);
        return checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.READ);
    }

    @ApiOperation(value = "Get Default Edge Profile (getDefaultEdgeProfile)",
            notes = "Fetch the Edge Profile marked as default in the current tenant." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/edgeProfile/default")
    public EdgeProfile getDefaultEdgeProfile() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.EDGE_PROFILE, Operation.READ);
        return checkNotNull(edgeProfileService.findDefaultEdgeProfile(getTenantId()));
    }

    @ApiOperation(value = "Get Edge Profiles (getEdgeProfiles)",
            notes = "Returns a page of Edge Profile objects owned by the current tenant. " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/edgeProfiles", params = {"pageSize", "page"})
    public PageData<EdgeProfile> getEdgeProfiles(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.EDGE_PROFILE, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(edgeProfileService.findEdgeProfilesByTenantId(getTenantId(), pageLink));
    }

    @ApiOperation(value = "Create or Update Edge Profile (saveEdgeProfile)",
            notes = "Creates or updates the Edge Profile. Rows added to profileData are assigned to every edge on the profile; "
                    + "rows missing from profileData are never unassigned by this call — use the remove endpoints. "
                    + "The 'default' flag is ignored on update; use the default endpoint." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/edgeProfile")
    public EdgeProfile saveEdgeProfile(@RequestBody EdgeProfile edgeProfile) throws Exception {
        edgeProfile.setTenantId(getTenantId());
        checkEntity(edgeProfile.getId(), edgeProfile, Resource.EDGE_PROFILE);
        return tbEdgeProfileService.save(edgeProfile, getCurrentUser());
    }

    @ApiOperation(value = "Delete Edge Profile (deleteEdgeProfile)",
            notes = "Deletes the Edge Profile. The default profile and a profile in use by edges cannot be deleted." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/edgeProfile/{edgeProfileId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteEdgeProfile(@Parameter(description = EDGE_PROFILE_ID_PARAM_DESCRIPTION, required = true)
                                  @PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId) throws Exception {
        checkParameter(EDGE_PROFILE_ID, strEdgeProfileId);
        EdgeProfile edgeProfile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.DELETE);
        tbEdgeProfileService.delete(edgeProfile, getCurrentUser());
    }

    @ApiOperation(value = "Make Edge Profile Default (setDefaultEdgeProfile)",
            notes = "Marks the edge profile as default within the tenant. The profile must have a root rule chain." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/edgeProfile/{edgeProfileId}/default")
    public EdgeProfile setDefaultEdgeProfile(@Parameter(description = EDGE_PROFILE_ID_PARAM_DESCRIPTION, required = true)
                                             @PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId) throws ThingsboardException {
        checkParameter(EDGE_PROFILE_ID, strEdgeProfileId);
        EdgeProfile edgeProfile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.WRITE);
        EdgeProfile previousDefault = edgeProfileService.findDefaultEdgeProfile(getTenantId());
        return tbEdgeProfileService.setDefaultEdgeProfile(edgeProfile, previousDefault, getCurrentUser());
    }

    @ApiOperation(value = "Get Edge Profile Assignments (getEdgeProfileAssignments)",
            notes = "Resolved rule chains, entity groups, integrations and scheduler events of the profile with names and owners. " +
                    "A profile can reference groups of any customer of the tenant, so this is tenant-level data." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/edgeProfile/{edgeProfileId}/assignments")
    public EdgeAssignments getEdgeProfileAssignments(@PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId) throws ThingsboardException {
        EdgeProfile profile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.READ);
        return edgeAssignmentsService.forProfile(getTenantId(), profile);
    }

    @ApiOperation(value = "Add Rule Chain To Edge Profile (addRuleChainToEdgeProfile)",
            notes = "Assigns the rule chain to the profile and fans it out to every edge on the profile." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/edgeProfile/{edgeProfileId}/ruleChain/{ruleChainId}")
    public EdgeProfile addRuleChainToEdgeProfile(@PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId,
                                                 @PathVariable("ruleChainId") String strRuleChainId) throws ThingsboardException {
        EdgeProfile profile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.WRITE);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        checkRuleChain(ruleChainId, Operation.READ);
        return tbEdgeProfileService.addRuleChain(profile, ruleChainId, getCurrentUser());
    }

    @ApiOperation(value = "Remove Rule Chain From Edge Profile (removeRuleChainFromEdgeProfile)",
            notes = "Removes the rule chain from the profile and unassigns it from every edge that received it through this profile. " +
                    "If unassigning fails on some edges, re-add the rule chain to the profile and remove it again to retry those edges." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/edgeProfile/{edgeProfileId}/ruleChain/{ruleChainId}")
    public EdgeProfile removeRuleChainFromEdgeProfile(@PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId,
                                                      @PathVariable("ruleChainId") String strRuleChainId) throws ThingsboardException {
        EdgeProfile profile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.WRITE);
        return tbEdgeProfileService.removeRuleChain(profile, new RuleChainId(toUUID(strRuleChainId)), getCurrentUser());
    }

    @ApiOperation(value = "Set Edge Profile Root Rule Chain (setEdgeProfileRootRuleChain)",
            notes = "Sets the root rule chain of the profile and propagates it, asynchronously, to every edge on the profile that still runs the previous profile root; "
                    + "an edge whose root was overridden with setEdgeRootRuleChain keeps it." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/edgeProfile/{edgeProfileId}/{ruleChainId}/root")
    public EdgeProfile setEdgeProfileRootRuleChain(@PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId,
                                                   @PathVariable("ruleChainId") String strRuleChainId) throws ThingsboardException {
        EdgeProfile profile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.WRITE);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        checkRuleChain(ruleChainId, Operation.READ);
        return tbEdgeProfileService.setRootRuleChain(profile, ruleChainId, getCurrentUser());
    }

    @ApiOperation(value = "Add Entity Group To Edge Profile (addEntityGroupToEdgeProfile)",
            notes = "Assigns the entity group to the profile and fans it out to every edge on the profile." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/edgeProfile/{edgeProfileId}/entityGroup/{entityGroupId}/{groupType}")
    public EdgeProfile addEntityGroupToEdgeProfile(@PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId,
                                                   @PathVariable("entityGroupId") String strEntityGroupId,
                                                   @PathVariable("groupType") String strGroupType) throws ThingsboardException {
        EdgeProfile profile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.WRITE);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        checkEntityGroupId(entityGroupId, Operation.READ);
        EntityType groupType = checkStrEntityGroupType("groupType", strGroupType);
        return tbEdgeProfileService.addEntityGroup(profile, entityGroupId, groupType, getCurrentUser());
    }

    @ApiOperation(value = "Remove Entity Group From Edge Profile (removeEntityGroupFromEdgeProfile)",
            notes = "Removes the entity group from the profile and unassigns it from every edge that received it through this profile. " +
                    "If unassigning fails on some edges, re-add the entity group to the profile and remove it again to retry those edges." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/edgeProfile/{edgeProfileId}/entityGroup/{entityGroupId}/{groupType}")
    public EdgeProfile removeEntityGroupFromEdgeProfile(@PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId,
                                                        @PathVariable("entityGroupId") String strEntityGroupId,
                                                        @PathVariable("groupType") String strGroupType) throws ThingsboardException {
        EdgeProfile profile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.WRITE);
        // the actual type is derived from the stored profile below; still validate the path segment so a bad one is a clean 400, not ignored
        checkStrEntityGroupType("groupType", strGroupType);
        return tbEdgeProfileService.removeEntityGroup(profile, new EntityGroupId(toUUID(strEntityGroupId)), getCurrentUser());
    }

    @ApiOperation(value = "Add Integration To Edge Profile (addIntegrationToEdgeProfile)",
            notes = "Assigns the integration to the profile and fans it out to every edge on the profile." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/edgeProfile/{edgeProfileId}/integration/{integrationId}")
    public EdgeProfile addIntegrationToEdgeProfile(@PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId,
                                                   @PathVariable("integrationId") String strIntegrationId) throws ThingsboardException {
        EdgeProfile profile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.WRITE);
        IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
        checkIntegrationId(integrationId, Operation.READ);
        return tbEdgeProfileService.addIntegration(profile, integrationId, getCurrentUser());
    }

    @ApiOperation(value = "Remove Integration From Edge Profile (removeIntegrationFromEdgeProfile)",
            notes = "Removes the integration from the profile and unassigns it from every edge that received it through this profile. " +
                    "If unassigning fails on some edges, re-add the integration to the profile and remove it again to retry those edges." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/edgeProfile/{edgeProfileId}/integration/{integrationId}")
    public EdgeProfile removeIntegrationFromEdgeProfile(@PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId,
                                                        @PathVariable("integrationId") String strIntegrationId) throws ThingsboardException {
        EdgeProfile profile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.WRITE);
        return tbEdgeProfileService.removeIntegration(profile, new IntegrationId(toUUID(strIntegrationId)), getCurrentUser());
    }

    @ApiOperation(value = "Add Scheduler Event To Edge Profile (addSchedulerEventToEdgeProfile)",
            notes = "Assigns the scheduler event to the profile and fans it out to every edge on the profile." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/edgeProfile/{edgeProfileId}/schedulerEvent/{schedulerEventId}")
    public EdgeProfile addSchedulerEventToEdgeProfile(@PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId,
                                                      @PathVariable("schedulerEventId") String strSchedulerEventId) throws ThingsboardException {
        EdgeProfile profile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.WRITE);
        SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
        checkSchedulerEventInfoId(schedulerEventId, Operation.READ);
        return tbEdgeProfileService.addSchedulerEvent(profile, schedulerEventId, getCurrentUser());
    }

    @ApiOperation(value = "Remove Scheduler Event From Edge Profile (removeSchedulerEventFromEdgeProfile)",
            notes = "Removes the scheduler event from the profile and unassigns it from every edge that received it through this profile. " +
                    "If unassigning fails on some edges, re-add the scheduler event to the profile and remove it again to retry those edges." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/edgeProfile/{edgeProfileId}/schedulerEvent/{schedulerEventId}")
    public EdgeProfile removeSchedulerEventFromEdgeProfile(@PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId,
                                                           @PathVariable("schedulerEventId") String strSchedulerEventId) throws ThingsboardException {
        EdgeProfile profile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.WRITE);
        return tbEdgeProfileService.removeSchedulerEvent(profile, new SchedulerEventId(toUUID(strSchedulerEventId)), getCurrentUser());
    }

    @ApiOperation(value = "Re-apply Edge Profile (applyEdgeProfile)",
            notes = "Re-applies the profile to every edge on it, asynchronously: the call returns once the work is queued. Missing rows are assigned and re-sent, "
                    + "rows this profile stamped but no longer contains are unassigned, and an edge without a root rule chain takes the profile root; "
                    + "an edge whose root was overridden with setEdgeRootRuleChain keeps it. Idempotent." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/edgeProfile/{edgeProfileId}/apply")
    @ResponseStatus(HttpStatus.OK)
    public void applyEdgeProfile(@PathVariable(EDGE_PROFILE_ID) String strEdgeProfileId) throws ThingsboardException {
        EdgeProfile profile = checkEdgeProfileId(new EdgeProfileId(toUUID(strEdgeProfileId)), Operation.WRITE);
        tbEdgeProfileService.reapply(profile, getCurrentUser());
    }

}
