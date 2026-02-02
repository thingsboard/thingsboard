///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { AuthUser } from '@shared/models/user.model';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router } from '@angular/router';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
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

type WidgetTypeBundle = WithOptional<WidgetTypeInfo, 'widgetType'>;

@Component({
  selector: 'tb-widgets-bundle-widget',
  templateUrl: './widgets-bundle-widgets.component.html',
  styleUrls: ['./widgets-bundle-widgets.component.scss']
})
export class WidgetsBundleWidgetsComponent extends PageComponent implements OnInit {

  authUser: AuthUser;

  isReadOnly: boolean;
  editMode = false;
  addMode = false;
  isDirty = false;

  widgetsBundle: WidgetsBundle;
  widgets: Array<WidgetTypeBundle>;
  excludeWidgetTypeIds: Array<string>;

  addWidgetFormControl = new FormControl(null, [Validators.required]);

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
  }

  trackByWidget(index: number, widget: WidgetTypeBundle): any {
    return widget;
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
