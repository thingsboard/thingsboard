// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  booleanAttribute,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { JobFilter, JobStatus, jobStatusTranslations, JobType, jobTypeTranslations } from '@shared/models/job.models';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { POSITION_MAP } from '@shared/models/overlay.models';
import { TemplatePortal } from '@angular/cdk/portal';
import { fromEvent, Subscription } from 'rxjs';
import { StringItemsOption } from '@shared/components/string-items-list.component';
import { EntityType } from '@shared/models/entity-type.models';
import { groupResourceByGroupType, Operation, resourceByEntityType } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { deepClone } from '@core/utils';

@Component({
    selector: 'tb-task-filter-config',
    templateUrl: './task-filter-config.component.html',
    styleUrls: ['./task-filter-config.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TaskFilterConfigComponent),
            multi: true
        }
    ],
    standalone: false
})
export class TaskFilterConfigComponent implements ControlValueAccessor, OnDestroy {

  @ViewChild('taskFilterPanel', {static: true})
  taskFilterPanel: TemplateRef<any>;

  @Input({transform: booleanAttribute})
  disabled: boolean;

  jobStatuses: JobStatus[] = Object.values(JobStatus);
  jobStatusTranslations = jobStatusTranslations;

  jobTypes: StringItemsOption[] = Object.values(JobType).map(type => ({
    name: this.translate.instant(jobTypeTranslations.get(type)),
    value: type
  }))

  buttonDisplayValue = this.translate.instant('task.task-filter');

  filteredEntityType = [EntityType.DEVICE, EntityType.ASSET, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE];

  tasksFilterConfigForm = this.fb.group({
    statuses: [],
    types: [],
    entities: []
  });

  private taskFilterConfig: JobFilter;
  private propagateChange = (_: any) => {};

  private tasksFilterOverlayRef: OverlayRef;
  private resizeWindows: Subscription;

  constructor(private translate: TranslateService,
              private fb: FormBuilder,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private userPermissionsService: UserPermissionsService ) {
    this.filteredEntityType = this.filteredEntityType.filter(entityType => {
      let hasGenericRead = false;
      if (resourceByEntityType.has(entityType)) {
        const resource = resourceByEntityType.get(entityType);
        hasGenericRead = this.userPermissionsService.hasGenericPermission(resource, Operation.READ);
      }
      let hasGroupRead = false;
      if (groupResourceByGroupType.has(entityType)) {
        hasGroupRead = this.userPermissionsService.hasReadGroupsPermission(entityType);
      }
      return hasGenericRead || hasGroupRead;
    })
  }

  ngOnDestroy(): void {
    this.resizeWindows?.unsubscribe();
    this.tasksFilterOverlayRef?.dispose();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.tasksFilterConfigForm.disable({emitEvent: false});
    } else {
      this.tasksFilterConfigForm.enable({emitEvent: false});
    }
  }

  writeValue(jobFilter: JobFilter): void {
    this.taskFilterConfig = deepClone(jobFilter) ?? {};
    this.updateButtonDisplayValue();
    this.tasksFilterConfigForm.patchValue(jobFilter, {emitEvent: false});
  }

  toggleJobFilterPanel($event: Event) {
    $event.stopPropagation();
    const config = new OverlayConfig({
      panelClass: 'tb-filter-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxHeight: '80vh',
      height: 'min-content',
      minWidth: ''
    });
    config.hasBackdrop = true;
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(this.nativeElement)
      .withPositions([POSITION_MAP.bottomLeft]);

    this.tasksFilterOverlayRef = this.overlay.create(config);
    this.tasksFilterOverlayRef.backdropClick().subscribe(() => {
      this.resizeWindows?.unsubscribe();
      this.tasksFilterOverlayRef.dispose();
    });
    this.tasksFilterOverlayRef.attach(new TemplatePortal(this.taskFilterPanel,
      this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => {
      this.tasksFilterOverlayRef.updatePosition();
    });
  }

  update(): void {
    this.tasksFilterConfigForm.markAsPristine();
    this.taskFilterConfig = deepClone(this.tasksFilterConfigForm.value);
    this.updateButtonDisplayValue();
    this.propagateChange(this.tasksFilterConfigForm.value);
    this.resizeWindows?.unsubscribe();
    this.tasksFilterOverlayRef.dispose();
  }

  cancel(): void {
    this.tasksFilterConfigForm.reset(this.taskFilterConfig);
    this.tasksFilterConfigForm.markAsPristine();
    this.resizeWindows?.unsubscribe();
    this.tasksFilterOverlayRef.dispose();
  }

  reset(): void {
    this.tasksFilterConfigForm.reset();
    this.tasksFilterConfigForm.markAsDirty()
  }

  private updateButtonDisplayValue() {
    const filterTextParts: string[] = [];
    if (this.taskFilterConfig.statuses?.length) {
      filterTextParts.push(this.taskFilterConfig.statuses.map(s =>
        this.translate.instant(jobStatusTranslations.get(s))).join(', '));
    }
    if (this.taskFilterConfig.types?.length) {
      filterTextParts.push(this.taskFilterConfig.types.map(s =>
        this.translate.instant(jobTypeTranslations.get(s))).join(', '));
    }
    if (this.taskFilterConfig.entities?.length) {
      filterTextParts.push(this.translate.instant('entity.entities'));
    }
    this.buttonDisplayValue = filterTextParts.length
      ? this.translate.instant('task.task-filter-params', { filterParams: filterTextParts.join(', ') })
      : this.translate.instant('task.task-filter');
  }
}
