// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, DestroyRef, inject, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { AuthUser } from '@shared/models/user.model';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router } from '@angular/router';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { widgetType as WidgetDataType, WidgetTypeInfo } from '@shared/models/widget.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { WidgetService } from '@core/http/widget.service';
import { FormControl, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { SelectWidgetTypeDialogComponent } from '@home/pages/widget/select-widget-type-dialog.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AiAssistantViewType } from '@shared/models/ai-chat.models';
import { TranslateService } from '@ngx-translate/core';
import { buffer, debounceTime, map } from 'rxjs/operators';

const AI_RELOAD_DEBOUNCE_MS = 1000;

type WidgetTypeBundle = WithOptional<WidgetTypeInfo, 'widgetType'>;

@Component({
    selector: 'tb-widgets-bundle-widget',
    templateUrl: './widgets-bundle-widgets.component.html',
    styleUrls: ['./widgets-bundle-widgets.component.scss'],
    standalone: false
})
export class WidgetsBundleWidgetsComponent extends PageComponent implements OnInit, OnDestroy {

  authUser: AuthUser;

  isReadOnly: boolean;
  editMode = false;
  addMode = false;
  isDirty = false;

  widgetsBundle: WidgetsBundle;
  widgets: Array<WidgetTypeBundle>;
  excludeWidgetTypeIds: Array<string>;

  addWidgetFormControl = new FormControl(null, [Validators.required]);

  private panelService = inject(AiAssistantPanelService);
  private userPermissionsService = inject(UserPermissionsService);
  private translate = inject(TranslateService);
  private destroyRef = inject(DestroyRef);

  constructor(protected store: Store<AppState>,
              private router: Router,
              private route: ActivatedRoute,
              private widgetsService: WidgetService,
              private importExport: ImportExportService,
              private cd: ChangeDetectorRef,
              private dialog: MatDialog) {
    super(store);
    this.authUser = getCurrentAuthUser(this.store);
    this.widgetsBundle = this.route.snapshot.data.widgetsBundle;
    this.widgets = [...this.route.snapshot.data.widgets];
    if (this.authUser.authority === Authority.TENANT_ADMIN) {
      this.isReadOnly = !this.widgetsBundle || this.widgetsBundle.tenantId.id === NULL_UUID;
    } else {
      this.isReadOnly = this.authUser.authority !== Authority.SYS_ADMIN;
    }
    if (!this.isReadOnly && !this.widgets.length) {
      this.editMode = true;
    }
    this.addWidgetFormControl.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((newWidget) => {
      if (newWidget) {
        this.addWidget(newWidget);
      }
    });
  }

  ngOnInit(): void {
    this.setupAiAssistant();
  }

  ngOnDestroy(): void {
    if (this.aiAssistantAvailable) {
      this.panelService.teardown();
    }
  }

  get aiAssistantAvailable(): boolean {
    return getCurrentAuthState(this.store).aiEnabled && this.authUser.authority === Authority.TENANT_ADMIN
      && this.userPermissionsService.hasGenericPermission(Resource.AI, Operation.ALL);
  }

  private setupAiAssistant() {
    if (!this.aiAssistantAvailable || !this.widgetsBundle?.id) {
      return;
    }
    this.panelService.setEnabled(true);
    this.panelService.setConfig({
      title: this.translate.instant('widget.ai-assistant-title'),
      initialPromptPlaceholder: this.translate.instant('widget.ai-assistant-initial-prompt-placeholder')
    });
    this.panelService.setClientContextForView({
      type: AiAssistantViewType.WIDGETS_BUNDLE,
      entityId: {entityType: EntityType.WIDGETS_BUNDLE, id: this.widgetsBundle.id.id}
    });
    this.panelService.setViewName(this.widgetsBundle.title);
    this.panelService.updatedData$.pipe(
      buffer(this.panelService.updatedData$.pipe(debounceTime(AI_RELOAD_DEBOUNCE_MS))),
      map(batches => batches.flat()),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(affected => {
      const id = this.widgetsBundle.id.id;
      if (!this.isDirty && affected.some(entityId => (entityId.entityType === EntityType.WIDGETS_BUNDLE && entityId.id === id)
        || entityId.entityType === EntityType.WIDGET_TYPE)) {
        this.reloadFromServer();
      }
    });
  }

  private reloadFromServer() {
    this.widgetsService.getWidgetsBundle(this.widgetsBundle.id.id, {ignoreLoading: true}).subscribe(bundle => {
      this.widgetsBundle = bundle;
      this.panelService.setViewName(bundle.title);
    });
    this.widgetsService.getBundleWidgetTypeInfosList(this.widgetsBundle.id.id).subscribe(widgets => {
      this.widgets = [...widgets];
      if (!this.isReadOnly && !this.widgets.length) {
        this.editMode = true;
      }
      this.cd.markForCheck();
    });
  }

  widgetDrop(event: CdkDragDrop<string[]>) {
    const widget = this.widgets[event.previousIndex];
    this.widgets.splice(event.previousIndex, 1);
    this.widgets.splice(event.currentIndex, 0, widget);
    this.isDirty = true;
  }

  addWidgetMode() {
    this.addWidgetFormControl.patchValue(null, {emitEvent: false});
    this.excludeWidgetTypeIds = this.widgets.map(w => w.id.id);
    this.addMode = true;
  }

  cancelAddWidgetMode() {
    this.addMode = false;
  }

  private addWidget(newWidget: WidgetTypeBundle) {
    this.widgets.push(newWidget);
    this.isDirty = true;
    this.addMode = false;
  }

  openWidgetEditor($event: Event, widgetType: WidgetTypeBundle) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigate([widgetType.id.id], {relativeTo: this.route}).then(()=> {});
  }

  exportWidgetType($event: Event, widgetType: WidgetTypeBundle) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportWidgetType(widgetType.id.id);
  }

  removeWidgetType($event: Event, widgetType: WidgetTypeBundle) {
    if ($event) {
      $event.stopPropagation();
    }
    const index = this.widgets.indexOf(widgetType);
    this.widgets.splice(index, 1);
    this.isDirty = true;
  }

  goBack() {
    this.router.navigate(['..'], { relativeTo: this.route });
  }

  exportWidgetsBundle() {
    this.importExport.exportWidgetsBundle(this.widgetsBundle.id.id);
  }

  edit() {
    this.editMode = true;
  }

  cancel() {
    if (this.isDirty) {
      this.widgetsService.getBundleWidgetTypeInfosList(this.widgetsBundle.id.id).subscribe(
        (widgets) => {
          this.widgets = [...widgets];
          this.isDirty = false;
          this.addMode = false;
          this.editMode = !this.widgets.length;
          this.cd.markForCheck();
        }
      );
    } else {
      this.addMode = false;
      this.editMode = !this.widgets.length;
    }
  }

  save() {
    const widgetTypeIds = this.widgets.map(w => w.id.id);
    this.widgetsService.updateWidgetsBundleWidgetTypes(this.widgetsBundle.id.id, widgetTypeIds).subscribe(() => {
      this.isDirty = false;
      this.editMode = false;
      this.addMode = false;
    });
  }

  addWidgetType($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<SelectWidgetTypeDialogComponent, any,
      WidgetDataType>(SelectWidgetTypeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe(
      (type) => {
        if (type) {
          this.router.navigate([type], {relativeTo: this.route}).then(() => {});
        }
      }
    );
  }

  importWidgetType() {
    this.importExport.importWidgetType().subscribe(
      (widgetType) => {
        if (widgetType) {
          const isExistWidget = this.widgets.some(widget => widget.id.id === widgetType.id.id);
          if (isExistWidget) {
            this.widgets = this.widgets.map(widget => widget.id.id !== widgetType.id.id ? widget : widgetType);
          }
          if (this.editMode) {
            this.widgets.push(widgetType);
            this.isDirty = true;
            this.cd.markForCheck();
          } else if (!isExistWidget) {
            this.widgetsService.addWidgetFqnToWidgetBundle(this.widgetsBundle.id.id, widgetType.fqn).subscribe(() => {
              this.widgets.push(widgetType);
              this.cd.markForCheck();
            });
          }
        }
      }
    );
  }

}
