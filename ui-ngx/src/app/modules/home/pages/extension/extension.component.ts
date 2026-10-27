// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, Inject, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { MatDialog } from '@angular/material/dialog';
import {
  cpuQuantityToCores,
  defaultResourceSize,
  Extension,
  extensionFullUrl,
  ExtensionResourceSizeInfo,
  ExtensionState,
  ExtensionStatus,
  extensionStateBackgroundColors,
  extensionStateColors,
  extensionStateTranslations,
  ResourceSize
} from '@shared/models/extension.models';
import { ExtensionService } from '@core/http/extension.service';
import {
  ExtensionLogsDialogComponent,
  ExtensionLogsDialogData
} from '@home/pages/extension/extension-logs-dialog.component';
import {
  ExtensionApiDocsDialogComponent,
  ExtensionApiDocsDialogData
} from '@home/pages/extension/extension-api-docs-dialog.component';
import { isDefinedAndNotNull } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'tb-extension',
  templateUrl: './extension.component.html',
  styleUrls: ['./extension.component.scss'],
  standalone: false
})
export class ExtensionComponent extends EntityComponent<Extension> implements OnInit {

  extensionState = ExtensionState;
  extensionStateTranslations = extensionStateTranslations;
  extensionStateColors = extensionStateColors;
  extensionStateBackgroundColors = extensionStateBackgroundColors;

  resourceSizes: Array<ExtensionResourceSizeInfo> = [];

  status: ExtensionStatus = null;
  statusLoading = false;
  // monotonic counter of status requests; only the newest one may write `status`
  private statusRequestSeq = 0;

  constructor(protected store: Store<AppState>,
              private extensionService: ExtensionService,
              private dialog: MatDialog,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Extension,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Extension>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    super.ngOnInit();
    this.extensionService.getResourceSizes({ignoreErrors: true}).subscribe({
      next: (sizes) => {
        this.resourceSizes = sizes;
        this.cd.markForCheck();
      },
      error: () => {}
    });
    this.reloadStatus();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: Extension): UntypedFormGroup {
    const spec = this.containerSpecValue(entity);
    return this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      containerSpec: this.fb.group({
        image: [spec.image, [Validators.required]],
        tag: [spec.tag, [Validators.required]],
        port: [spec.port, [Validators.required, Validators.min(1), Validators.max(65535)]],
        replicas: [spec.replicas, [Validators.required, Validators.min(1), Validators.max(20)]],
        resources: this.fb.group({
          size: [spec.size]
        }),
        env: this.fb.array(this.envControls(entity))
      })
    });
  }

  updateForm(entity: Extension) {
    const spec = this.containerSpecValue(entity);
    this.entityForm.patchValue({
      name: entity.name,
      containerSpec: {
        image: spec.image,
        tag: spec.tag,
        port: spec.port,
        replicas: spec.replicas,
        resources: {
          size: spec.size
        }
      }
    });
    const envArray = this.entityForm.get('containerSpec.env') as UntypedFormArray;
    envArray.clear({emitEvent: false});
    this.envControls(entity).forEach(group => envArray.push(group, {emitEvent: false}));
    this.reloadStatus();
  }

  prepareFormValue(formValue: any): any {
    const value = super.prepareFormValue(formValue);
    const envList: Array<{key: string; value: string}> = value.containerSpec.env || [];
    const env: {[key: string]: string} = {};
    envList.forEach(item => {
      if (item.key) {
        env[item.key] = item.value;
      }
    });
    value.containerSpec.env = Object.keys(env).length ? env : null;
    if (this.entity && this.entity.containerSpec && isDefinedAndNotNull(this.entity.containerSpec.registryAuth)) {
      value.containerSpec.registryAuth = this.entity.containerSpec.registryAuth;
    }
    return value;
  }

  // full URL of the extension data plane, with the domain, so it can be copied and called as is;
  // the status snapshot carries the route, the entity itself only the slug
  get extensionUrl(): string {
    return extensionFullUrl({url: this.status?.url || this.entity?.url, slug: this.entity?.slug});
  }

  onExtensionUrlCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('extension.url-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  // combined <image>:<tag> shown as a single read-only "Docker image" field
  get dockerImage(): string {
    const spec = this.entity?.containerSpec;
    return spec ? `${spec.image}:${spec.tag}` : '';
  }

  // stored size may be null (never set explicitly) — the backend then applies the catalog
  // default, so mirror that here instead of showing an empty field
  private get effectiveResourceSize(): ResourceSize | null {
    const size = this.entity?.containerSpec?.resources?.size;
    if (size) {
      return size;
    }
    const def = this.resourceSizes.find(s => s.isDefault);
    return def ? def.name : null;
  }

  get resourceSize(): string {
    return this.effectiveResourceSize || '-';
  }

  // cpu/memory detail for the effective resource tier (null until the catalog loads)
  get resourceSizeInfo(): ExtensionResourceSizeInfo | null {
    const size = this.effectiveResourceSize;
    return size ? (this.resourceSizes.find(s => s.name === size) || null) : null;
  }

  // CPU in cores for display ('250m' -> '0.25'), consistent with the quota footer
  cpuCores(quantity: string): string {
    return cpuQuantityToCores(quantity);
  }

  reloadStatus() {
    // One component instance serves every row of the table, so a slow answer for the previous row
    // can land after the current row's. The request is keyed by the id it was made for and only the
    // newest request may write: an obsolete answer is dropped, never shown under another name.
    const requestedId = this.entity?.id?.id;
    const seq = ++this.statusRequestSeq;
    this.status = null;
    if (!requestedId) {
      this.statusLoading = false;
      return;
    }
    this.statusLoading = true;
    this.extensionService.getExtensionStatus(requestedId, {ignoreErrors: true}).pipe(
      // both outcomes end the same way, so the spinner is cleared in one place
      finalize(() => {
        if (seq === this.statusRequestSeq) {
          this.statusLoading = false;
        }
        this.cd.markForCheck();
      })
    ).subscribe({
      next: (status) => {
        if (seq === this.statusRequestSeq && requestedId === this.entity?.id?.id) {
          this.status = status;
        }
      },
      error: () => {
        if (seq === this.statusRequestSeq) {
          this.status = null;
        }
      }
    });
  }

  podPhases(status: ExtensionStatus): Array<{phase: string; count: number}> {
    if (status?.podPhaseCounts) {
      return Object.keys(status.podPhaseCounts).map(phase => ({phase, count: status.podPhaseCounts[phase]}));
    }
    return [];
  }

  showLogs($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<ExtensionLogsDialogComponent, ExtensionLogsDialogData>(ExtensionLogsDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        extensionId: this.entity.id.id,
        extensionName: this.entity.name
      }
    });
  }

  showApiDocs($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<ExtensionApiDocsDialogComponent, ExtensionApiDocsDialogData>(ExtensionApiDocsDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        extensionId: this.entity.id.id,
        extensionName: this.entity.name
      }
    });
  }

  // Form defaults of the container spec, shared by buildForm and updateForm so the two can never
  // drift apart. A missing spec (create, or a partially loaded entity) falls back to the same
  // values the backend applies.
  private containerSpecValue(entity: Extension): {image: string; tag: string; port: number; replicas: number; size: ResourceSize} {
    const spec = entity?.containerSpec;
    return {
      image: spec ? spec.image : '',
      tag: spec ? spec.tag : '',
      port: spec ? spec.port : 8090,
      replicas: spec ? spec.replicas : 1,
      size: spec?.resources?.size ? spec.resources.size : defaultResourceSize
    };
  }

  // env is not editable from the details panel, but it stays in the form so that the
  // container spec still round-trips unchanged on save.
  private envControls(entity: Extension): Array<UntypedFormGroup> {
    const env = entity?.containerSpec?.env;
    if (!env) {
      return [];
    }
    return Object.keys(env).map(key => this.fb.group({
      key: [key, [Validators.required]],
      value: [env[key]]
    }));
  }
}
