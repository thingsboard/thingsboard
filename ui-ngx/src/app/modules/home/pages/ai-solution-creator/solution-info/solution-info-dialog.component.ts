// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, ElementRef, Inject, Renderer2, ViewContainerRef } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { EntityResult, SolutionCreatorInfo } from '@shared/models/solution-creator.models';
import { baseDetailsPageByEntityType, EntityType, entityTypeTranslations, groupUrlPrefixByEntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/auth/auth.service';
import { AlarmSeverity, alarmSeverityBackgroundColors, alarmSeverityColors } from '@shared/models/alarm.models';
import {
  aggregateByName,
  AggregatedEntityResult,
  issuesCountSummary,
  IssueStatus
} from '@home/pages/ai-solution-creator/solution-info/issues-aggregation';
import { TbPopoverService } from '@shared/components/popover.service';
import { IssuesPopoverComponent } from '@home/pages/ai-solution-creator/solution-info/issues-popover.component';

export interface SolutionInfoDialogData {
  solution: SolutionCreatorInfo;
}

@Component({
  selector: 'tb-solution-info-dialog',
  templateUrl: './solution-info-dialog.component.html',
  styleUrls: ['../../../../../shared/components/markdown.component.scss', './solution-info-dialog.component.scss'],
  standalone: false
})
export class SolutionInfoDialogComponent extends DialogComponent<SolutionInfoDialogData, void> {

  EntityType = EntityType;
  entityTypeTranslations = entityTypeTranslations;

  entityTypes = [
    EntityType.DASHBOARD,
    EntityType.DEVICE,
    EntityType.DEVICE_PROFILE,
    EntityType.CALCULATED_FIELD,
    EntityType.RULE_CHAIN,
    EntityType.ASSET,
    EntityType.ASSET_PROFILE,
    EntityType.ALARM,
    EntityType.ROLE,
    EntityType.CUSTOMER,
    EntityType.USER,
    EntityType.ENTITY_GROUP
  ];

  solution: SolutionCreatorInfo;

  issueStatus = IssueStatus;

  currentUrl = this.router.url;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<SolutionInfoDialogData, void>,
    @Inject(MAT_DIALOG_DATA) public data: SolutionInfoDialogData,
    private translate: TranslateService,
    private authService: AuthService,
    private popoverService: TbPopoverService,
    private renderer: Renderer2,
    private viewContainerRef: ViewContainerRef,
    private cd: ChangeDetectorRef,
  ) {
    super(store, router, dialogRef);
    this.solution = this.data.solution;
  }

  close(): void {
    this.dialogRef.close(null);
  }

  aggregatedFor(entityType: EntityType): AggregatedEntityResult[] {
    return aggregateByName(entityType, this.solution.metadata?.installResult?.entityResults?.[entityType]);
  }

  totalIssues(entry: AggregatedEntityResult): number {
    return entry.errors.length + entry.warnings.length;
  }

  issuesCountSummary(entry: AggregatedEntityResult): string {
    return issuesCountSummary(entry.errors, entry.warnings, this.translate);
  }

  entityLink(entityId: EntityId): string {
    return `${baseDetailsPageByEntityType.get(entityId.entityType as EntityType)}/${entityId.id}`;
  }

  entityGroupLink(entity: EntityResult): string {
    const groupType = entity.additionalData?.groupType as EntityType;
    const prefix = groupUrlPrefixByEntityType.get(groupType);
    return prefix ? `${prefix}/${entity.id.id}` : null;
  }

  openDashboard(entity: EntityResult) {
    const isTenant = !this.solution.metadata?.installResult?.entityResults?.[EntityType.USER]
      .find(user => user.id?.id === entity.additionalData.userId)?.additionalData?.customer
    this.authService.redirectUrl = isTenant ? `/dashboards/all/${entity.id.id}` : `/dashboards/${entity.id.id}`;
    this.loginAsUser(entity.additionalData.userId);
    this.close();
  }

  loginAsUser(userId: string): void {
    this.authService.loginAsUser(userId).subscribe(() => {});
  }

  getUserName(userId: string): string {
    return this.findUser(userId)?.additionalData?.name ?? '';
  }

  getUserAuthority(userId: string): string {
    return this.findUser(userId)?.additionalData?.customer ? 'user.customer' : 'tenant.tenant';
  }

  getAlarmSeverityColor(severity: AlarmSeverity): string {
    return alarmSeverityColors.has(severity) ? alarmSeverityColors.get(severity) : null;
  }

  getAlarmSeverityBackgroundColors(severity: AlarmSeverity): string {
    return alarmSeverityBackgroundColors.has(severity) ? alarmSeverityBackgroundColors.get(severity) : null;
  }

  openPopover($event: Event, entry: AggregatedEntityResult, element?: ElementRef) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.totalIssues(entry) === 0) return;
    const trigger = element ? element.nativeElement : $event.target;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        hostView: this.viewContainerRef,
        componentType: IssuesPopoverComponent,
        preferredPlacement: ['leftTopOnly', 'leftOnly', 'leftBottomOnly'],
        context: {entry},
        hideOnClickOutside: true,
        showCloseButton: true,
        isModal: false
      });
      this.cd.markForCheck();
    }
}
  private findUser(userId: string): EntityResult | undefined {
    return this.solution.metadata?.installResult?.entityResults?.[EntityType.USER]?.find(user => user.id?.id === userId);
  }
}
