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

import { Component, Inject, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  ResourceReferences,
  ResourceInfoWithReferences,
  ResourceInfo
} from '@shared/models/resource.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { ImageReferencesComponent } from '@shared/components/image/image-references.component';
import { TranslateService } from '@ngx-translate/core';
import { Datasource } from "@shared/models/widget.models";

interface ResourcesInUseDialogDataConfiguration {
  title: string;
  message: string;
  columns: string[];
  deleteText: string;
  selectedText: string;
  datasource?: Datasource;
}

export interface ResourcesInUseDialogData {
  multiple: boolean;
  resources: ResourceInfoWithReferences[];
  configuration: ResourcesInUseDialogDataConfiguration;
}

@Component({
    selector: 'tb-resources-in-use-dialog',
    templateUrl: './resources-in-use-dialog.component.html',
    styleUrls: ['./resources-in-use-dialog.component.scss'],
    standalone: false
})
export class ResourcesInUseDialogComponent extends
  DialogComponent<ResourcesInUseDialogComponent, ResourceInfo[]> implements OnInit {

  displayPreview: boolean;
  configuration: ResourcesInUseDialogDataConfiguration;
  references: ResourceReferences;

  dataSource: Datasource;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ResourcesInUseDialogData,
              public dialogRef: MatDialogRef<ResourcesInUseDialogComponent, ResourceInfo[]>,
              public translate: TranslateService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private popoverService: TbPopoverService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.configuration = this.data.configuration;
    this.displayPreview = this.data.configuration.columns.includes('preview');
    if (this.data.multiple) {
      this.dataSource = this.data.configuration.datasource;
    } else {
      this.references = this.data.resources[0].references;
    }
  }

  cancel() {
    this.dialogRef.close(null);
  }

  delete() {
    if (this.data.multiple) {
      this.dialogRef.close(this.dataSource.selection.selected);
    } else {
      this.dialogRef.close(this.data.resources);
    }
  }

  toggleShowReferences($event: Event, resource: ResourceInfoWithReferences, referencesButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = referencesButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const referencesPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, ImageReferencesComponent, 'top', true, null,
        {
          references: resource.references
        }, {}, {}, {},
        false,
        visible => {
          const addClasses =
            visible ? 'mdc-button--unelevated mat-mdc-unelevated-button' : 'mdc-button--outlined mat-mdc-outlined-button';
          const removeClasses =
            visible ? 'mdc-button--outlined mat-mdc-outlined-button' : 'mdc-button--unelevated mat-mdc-unelevated-button';
          addClasses.split(' ').forEach(clazz => {
            this.renderer.addClass(trigger, clazz);
          });
          removeClasses.split(' ').forEach(clazz => {
            this.renderer.removeClass(trigger, clazz);
          });
        });
      referencesPopover.tbComponentRef.instance.popoverComponent = referencesPopover;
    }
  }
}
