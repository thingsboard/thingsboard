// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { forkJoin, Observable, of } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import {
  EDGE_ASSIGNMENT_SECTIONS,
  EdgeAssignmentRow,
  EdgeAssignments,
  EdgeAssignmentSection,
  EdgeAssignmentSource,
  sectionRows
} from '@shared/models/edge-profile.models';
import { EdgeProfileService } from '@core/http/edge-profile.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { groupResourceByGroupType, Operation, Resource } from '@shared/models/security.models';
import { Authority } from '@shared/models/authority.enum';
import { EdgeService } from '@core/http/edge.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { RuleChainService } from '@core/http/rule-chain.service';
import { IntegrationService } from '@core/http/integration.service';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { DialogService } from '@core/services/dialog.service';
import { AddEntityGroupsToEdgeDialogComponent } from '@home/dialogs/add-entity-groups-to-edge-dialog.component';
import { AddEntityGroupsToEdgeDialogData } from '@home/dialogs/add-entity-groups-to-edge-dialog.models';
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from '@home/dialogs/add-entities-to-edge-dialog.component';
import {
  AddEdgeProfileAssignmentDialogComponent,
  AddEdgeProfileAssignmentDialogData
} from '@home/components/edge/add-edge-profile-assignment-dialog.component';
import { getEntityDetailsPageURL } from '@core/utils';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { NULL_UUID } from '@shared/models/id/has-uuid';

export const EDGE_ASSIGNMENTS_PAGE_SIZE = 5;

interface SectionState {
  open: boolean;
  page: number;
}

@Component({
  selector: 'tb-edge-assignments',
  templateUrl: './edge-assignments.component.html',
  styleUrls: ['./edge-assignments.component.scss'],
  standalone: false
})
export class EdgeAssignmentsComponent implements OnChanges {

  @Input() mode: 'profile' | 'edge' = 'profile';
  @Input() entityId: EntityId;
  @Input() active = true;
  @Input() readonly = false;

  // assign, unassign and set-root all write the entity server side and bump its version; the details form around this
  // tab still holds the copy it loaded, and saving that copy afterwards fails on the version check
  @Output() assignmentsChanged = new EventEmitter<void>();

  sections = EDGE_ASSIGNMENT_SECTIONS;
  entityGroupsSections = EDGE_ASSIGNMENT_SECTIONS.filter(s => s.group === 'entity');
  logicSections = EDGE_ASSIGNMENT_SECTIONS.filter(s => s.group === 'logic');
  sourceValues = EdgeAssignmentSource;

  assignments: EdgeAssignments;
  state: { [key: string]: SectionState } = {};
  loading = false;
  textSearchMode = false;

  @ViewChild('searchInput') searchInputField: ElementRef;

  // filteredRows() is reached only from template bindings, so without this every change-detection pass would re-filter
  // and re-sort every section — and a pass fires on each keystroke in the search box
  private filteredCache = new Map<string, EdgeAssignmentRow[]>();
  private searchTextValue = '';
  private sourceFilterValue: 'all' | EdgeAssignmentSource = 'all';

  get searchText(): string {
    return this.searchTextValue;
  }

  set searchText(value: string) {
    this.searchTextValue = value || '';
    this.onFilterChanged();
  }

  get sourceFilter(): 'all' | EdgeAssignmentSource {
    return this.sourceFilterValue;
  }

  set sourceFilter(value: 'all' | EdgeAssignmentSource) {
    this.sourceFilterValue = value;
    this.onFilterChanged();
  }

  constructor(private edgeProfileService: EdgeProfileService,
              private userPermissionsService: UserPermissionsService,
              private edgeService: EdgeService,
              private entityGroupService: EntityGroupService,
              private ruleChainService: RuleChainService,
              private integrationService: IntegrationService,
              private schedulerEventService: SchedulerEventService,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private translate: TranslateService,
              private router: Router,
              private store: Store<AppState>) {
    this.sections.forEach(s => this.state[s.key] = {open: s.key === 'deviceGroups' || s.key === 'ruleChains', page: 0});
  }

  ngOnChanges(changes: SimpleChanges) {
    if ((changes.entityId || changes.active) && this.entityId && this.active) {
      this.reload();
    }
  }

  private reloadAfterChange() {
    this.reload();
    this.assignmentsChanged.emit();
  }

  reload() {
    this.loading = true;
    const request$: Observable<EdgeAssignments> = this.mode === 'profile'
      ? this.edgeProfileService.getEdgeProfileAssignments(this.entityId.id)
      : this.edgeService.getEdgeAssignments(this.entityId.id);
    request$.subscribe({
      next: a => { this.assignments = a; this.onFilterChanged(); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  // Rows changed, so the memoised lists are stale. Paging is reset here too: a shorter list can leave the current page
  // past the end, and clamping it inside pageRows() would mean a template read quietly mutating component state.
  private onFilterChanged() {
    this.filteredCache.clear();
    this.sections.forEach(s => this.state[s.key].page = 0);
  }

  get isProfile(): boolean {
    return this.mode === 'profile';
  }

  get isTenantAdmin(): boolean {
    return getCurrentAuthUser(this.store).authority === Authority.TENANT_ADMIN;
  }

  // On the edge side rule chains and integrations are tenant-level and the server returns them to tenant
  // administrators only, so a customer user is not shown empty sections with an Add button that would 403.
  visibleSections(sections: EdgeAssignmentSection[]): EdgeAssignmentSection[] {
    if (this.isProfile || this.isTenantAdmin) {
      return sections;
    }
    return sections.filter(s => s.entityType !== EntityType.RULE_CHAIN && s.entityType !== EntityType.INTEGRATION);
  }

  // Per-section write access mirrors the RBAC checks of the assign/unassign endpoints, so a customer user who may
  // manage entity groups on their edge can do it here, and a tenant admin without RULE_CHAIN write is not offered it.
  canEdit(section: EdgeAssignmentSection): boolean {
    if (this.readonly) {
      return false;
    }
    if (this.isProfile) {
      return true;
    }
    switch (section.entityType) {
      case EntityType.RULE_CHAIN:
        return this.isTenantAdmin && this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.WRITE);
      case EntityType.INTEGRATION:
        return this.isTenantAdmin && this.userPermissionsService.hasGenericPermission(Resource.INTEGRATION, Operation.WRITE);
      case EntityType.SCHEDULER_EVENT:
        return this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.WRITE);
      case EntityType.ENTITY_GROUP:
        return this.userPermissionsService.hasGenericPermission(groupResourceByGroupType.get(section.groupType), Operation.WRITE);
      default:
        return false;
    }
  }

  get profileUrl(): string | null {
    return this.assignments?.edgeProfileId ? getEntityDetailsPageURL(this.assignments.edgeProfileId.id, EntityType.EDGE_PROFILE) : null;
  }

  // the profile route guards on EDGE_PROFILE read, so offering the link without it just lands the user on a
  // permission-denied dialog
  get canOpenProfile(): boolean {
    return !!this.profileUrl && this.userPermissionsService.hasGenericPermission(Resource.EDGE_PROFILE, Operation.READ);
  }

  get rootRuleChainName(): string {
    return this.assignments?.ruleChains.find(r => r.root)?.name || '';
  }

  // a profile with no root of its own cannot be the source of this edge's root either, so that case is an override too
  get rootOverridden(): boolean {
    const a = this.assignments;
    return !this.isProfile && !!a?.rootRuleChainId && a.rootRuleChainId.id !== a.profileRootRuleChainId?.id;
  }

  get profileRootRuleChainName(): string {
    const id = this.assignments?.profileRootRuleChainId?.id;
    return this.assignments?.ruleChains.find(r => r.id.id === id)?.name || '';
  }

  toggle(section: EdgeAssignmentSection) {
    this.state[section.key].open = !this.state[section.key].open;
  }

  get allOpen(): boolean {
    return this.sections.every(s => this.state[s.key].open);
  }

  toggleAll() {
    const next = !this.allOpen;
    this.sections.forEach(s => this.state[s.key].open = next);
  }

  allRows(section: EdgeAssignmentSection): EdgeAssignmentRow[] {
    return sectionRows(this.assignments, section);
  }

  filteredRows(section: EdgeAssignmentSection): EdgeAssignmentRow[] {
    const cached = this.filteredCache.get(section.key);
    if (cached) {
      return cached;
    }
    const text = this.searchTextValue.toLowerCase();
    const rows = this.allRows(section)
      .filter(r => !text || this.matchesSearch(section, r, text))
      .filter(r => this.sourceFilterValue === 'all' || r.source === this.sourceFilterValue)
      .sort((a, b) => a.name.localeCompare(b.name));
    this.filteredCache.set(section.key, rows);
    return rows;
  }

  // matches every column the section renders, so a search for an owner or a profile name finds the row the user sees
  private matchesSearch(section: EdgeAssignmentSection, row: EdgeAssignmentRow, text: string): boolean {
    if (row.name.toLowerCase().includes(text)) {
      return true;
    }
    if (!this.isRuleChains(section) && row.ownerName?.toLowerCase().includes(text)) {
      return true;
    }
    return !this.isProfile && this.sourceLabel(row).toLowerCase().includes(text);
  }

  // the Source column shows the profile name for a profile row and "This edge" for a local one
  sourceLabel(row: EdgeAssignmentRow): string {
    return row.source === EdgeAssignmentSource.PROFILE
      ? (this.assignments?.edgeProfileName ?? '')
      : this.translate.instant('edge.this-edge');
  }

  enterFilterMode(): void {
    this.textSearchMode = true;
    setTimeout(() => this.searchInputField?.nativeElement.focus(), 10);
  }

  exitFilterMode(): void {
    this.textSearchMode = false;
    this.searchText = '';
  }

  pageRows(section: EdgeAssignmentSection): EdgeAssignmentRow[] {
    const rows = this.filteredRows(section);
    const start = this.state[section.key].page * EDGE_ASSIGNMENTS_PAGE_SIZE;
    return rows.slice(start, start + EDGE_ASSIGNMENTS_PAGE_SIZE);
  }

  range(section: EdgeAssignmentSection): string {
    const total = this.filteredRows(section).length;
    if (!total) {
      return this.translate.instant('edge-profile.range-empty');
    }
    const start = this.state[section.key].page * EDGE_ASSIGNMENTS_PAGE_SIZE + 1;
    const end = Math.min(start + EDGE_ASSIGNMENTS_PAGE_SIZE - 1, total);
    return this.translate.instant('edge-profile.range', {start, end, total});
  }

  hasPrev(section: EdgeAssignmentSection): boolean {
    return this.state[section.key].page > 0;
  }

  hasNext(section: EdgeAssignmentSection): boolean {
    return (this.state[section.key].page + 1) * EDGE_ASSIGNMENTS_PAGE_SIZE < this.filteredRows(section).length;
  }

  prev(section: EdgeAssignmentSection, $event: Event) {
    $event.stopPropagation();
    if (this.hasPrev(section)) {
      this.state[section.key].page--;
    }
  }

  next(section: EdgeAssignmentSection, $event: Event) {
    $event.stopPropagation();
    if (this.hasNext(section)) {
      this.state[section.key].page++;
    }
  }

  countProfile(section: EdgeAssignmentSection): number {
    return this.allRows(section).filter(r => r.source === EdgeAssignmentSource.PROFILE).length;
  }

  countEdge(section: EdgeAssignmentSection): number {
    return this.allRows(section).filter(r => r.source === EdgeAssignmentSource.EDGE).length;
  }
  isRuleChains(section: EdgeAssignmentSection): boolean {
    return section.key === 'ruleChains';
  }

  isLocked(row: EdgeAssignmentRow): boolean {
    return !this.isProfile && row.source === EdgeAssignmentSource.PROFILE;
  }

  canSetRoot(section: EdgeAssignmentSection, row: EdgeAssignmentRow): boolean {
    return this.canEdit(section) && this.isRuleChains(section) && !row.root;
  }

  setRootTooltip(row: EdgeAssignmentRow): string {
    if (this.isProfile) {
      return this.translate.instant(row.root ? 'edge-profile.already-root' : 'edge-profile.set-root');
    }
    return this.translate.instant(row.root ? 'edge.already-root' : 'edge.set-root');
  }

  canRemove(section: EdgeAssignmentSection, row: EdgeAssignmentRow): boolean {
    return this.canEdit(section) && !row.root && !this.isLocked(row);
  }

  removeTooltip(row: EdgeAssignmentRow): string {
    // a profile-sourced row (root included) can only be changed on the profile, so that message wins over the root one
    if (this.isLocked(row)) {
      return this.translate.instant('edge.comes-from-profile', {profileName: this.assignments?.edgeProfileName});
    }
    if (row.root) {
      return this.translate.instant(this.isProfile ? 'edge-profile.root-cannot-be-removed' : 'edge.root-cannot-be-unassigned');
    }
    return this.translate.instant(this.isProfile ? 'edge-profile.remove-from-profile' : 'edge.unassign-from-this-edge');
  }

  setRoot(row: EdgeAssignmentRow, $event: Event) {
    $event.stopPropagation();
    const title = this.translate.instant(this.isProfile ? 'edge-profile.set-root-title' : 'edge.set-root-title', {ruleChainName: row.name});
    const text = this.isProfile
      ? this.translate.instant('edge-profile.set-root-text', {count: this.assignments.edgesCount})
      : this.translate.instant('edge.set-root-text', {profileName: this.assignments.edgeProfileName});
    this.dialogService.confirm(title, text, this.translate.instant('action.no'), this.translate.instant('action.yes'), true)
      .subscribe(res => {
        if (res) {
          const request$: Observable<unknown> = this.isProfile
            ? this.edgeProfileService.setRootRuleChain(this.entityId.id, row.id.id)
            : this.edgeService.setEdgeRootRuleChain(this.entityId.id, row.id.id);
          request$.subscribe(() => this.reloadAfterChange());
        }
      });
  }

  add(section: EdgeAssignmentSection, $event: Event) {
    $event.stopPropagation();
    if (this.isProfile) {
      // the tenant is only the starting owner: the dialog lets the user pick a customer, whose groups the profile
      // then fans out to the edges that customer owns
      const tenantId = new TenantId(getCurrentAuthUser(this.store).tenantId);
      this.dialog.open<AddEdgeProfileAssignmentDialogComponent, AddEdgeProfileAssignmentDialogData, boolean>(
        AddEdgeProfileAssignmentDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {edgeProfileId: this.entityId.id, section, ownerId: tenantId, edgeId: null, groupType: section.groupType}
        }).afterClosed().subscribe(res => {
          if (res) {
            this.reloadAfterChange();
          }
        });
      return;
    }
    if (section.entityType === EntityType.ENTITY_GROUP) {
      this.edgeService.getEdge(this.entityId.id).subscribe(edge => {
        const ownerId = edge.customerId && edge.customerId.id !== NULL_UUID ? new CustomerId(edge.customerId.id) : new TenantId(edge.tenantId.id);
        this.dialog.open<AddEntityGroupsToEdgeDialogComponent, AddEntityGroupsToEdgeDialogData, boolean>(
          AddEntityGroupsToEdgeDialogComponent, {
            disableClose: true,
            panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
            data: {
              ownerId,
              groupType: section.groupType,
              edgeId: this.entityId.id,
              addEntityGroupsToEdgeTitle: section.assignLabel,
              confirmSelectTitle: 'action.assign',
              notFoundText: 'entity-group.no-entity-groups-matching',
              requiredText: 'entity-group.target-entity-group-required'
            }
          }).afterClosed().subscribe(res => {
            if (res) {
              this.reloadAfterChange();
            }
          });
      });
    } else {
      this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData, string[]>(
        AddEntitiesToEdgeDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {edgeId: this.entityId.id, entityType: section.entityType}
        }).afterClosed().subscribe(res => {
          if (res) {
            if (!this.isProfile && section.entityType === EntityType.RULE_CHAIN) {
              this.warnAboutMissingRelatedRuleChains();
            } else {
              this.reloadAfterChange();
            }
          }
        });
    }
  }

  /**
   * An edge rule chain whose input node forwards to a chain the edge does not have leaves messages dead-ending there,
   * and the assign endpoint does not check for it. The old per-edge rule chains page warned about this; the warning
   * lives here now that assignment happens from this tab.
   */
  private warnAboutMissingRelatedRuleChains() {
    this.edgeService.findMissingToRelatedRuleChains(this.entityId.id).subscribe(missingRuleChains => {
      if (!missingRuleChains || !Object.keys(missingRuleChains).length) {
        this.reloadAfterChange();
        return;
      }
      const formatted = Object.keys(missingRuleChains)
        .map(name => `- '${name}': '${missingRuleChains[name].join('\', \'')}'`)
        .join('<br>');
      this.dialogService.alert(
        this.translate.instant('edge.missing-related-rule-chains-title'),
        this.translate.instant('edge.missing-related-rule-chains-text', {missingRuleChains: formatted}),
        this.translate.instant('action.close'), true
      ).subscribe(() => this.reloadAfterChange());
    });
  }

  remove(section: EdgeAssignmentSection, row: EdgeAssignmentRow, $event: Event) {
    $event.stopPropagation();
    const title = this.isProfile
      ? this.translate.instant('edge-profile.remove-row-title', {name: row.name, profileName: this.assignments.edgeProfileName})
      : this.translate.instant('edge.unassign-row-title', {name: row.name});
    const isGroup = section.entityType === EntityType.ENTITY_GROUP;
    const text = this.isProfile
      ? this.translate.instant(isGroup ? 'edge-profile.remove-group-row-text' : 'edge-profile.remove-row-text', {count: this.assignments.edgesCount})
      : this.translate.instant(isGroup ? 'edge.unassign-group-row-text' : 'edge.unassign-row-text');
    this.dialogService.confirm(title, text, this.translate.instant('action.no'), this.translate.instant('action.yes'), true)
      .subscribe(res => {
        if (res) {
          this.removeRequest(section, row).subscribe(() => this.reloadAfterChange());
        }
      });
  }

  private removeRequest(section: EdgeAssignmentSection, row: EdgeAssignmentRow): Observable<any> {
    const id = this.entityId.id;
    if (this.isProfile) {
      switch (section.entityType) {
        case EntityType.RULE_CHAIN: return this.edgeProfileService.removeRuleChain(id, row.id.id);
        case EntityType.INTEGRATION: return this.edgeProfileService.removeIntegration(id, row.id.id);
        case EntityType.SCHEDULER_EVENT: return this.edgeProfileService.removeSchedulerEvent(id, row.id.id);
        case EntityType.ENTITY_GROUP: return this.edgeProfileService.removeEntityGroup(id, row.id.id, section.groupType);
        default: throw new Error(`Unsupported edge assignment section ${section.key}`);
      }
    }
    switch (section.entityType) {
      case EntityType.RULE_CHAIN: return this.ruleChainService.unassignRuleChainFromEdge(id, row.id.id);
      case EntityType.INTEGRATION: return this.integrationService.unassignIntegrationFromEdge(id, row.id.id);
      case EntityType.SCHEDULER_EVENT: return this.schedulerEventService.unassignSchedulerEventFromEdge(id, row.id.id);
      case EntityType.ENTITY_GROUP: return this.entityGroupService.unassignEntityGroupFromEdge(id, row.id.id, section.groupType);
      default: throw new Error(`Unsupported edge assignment section ${section.key}`);
    }
  }

  openProfile($event: Event) {
    $event.stopPropagation();
    if (this.profileUrl) {
      this.router.navigateByUrl(this.profileUrl);
    }
  }
}
