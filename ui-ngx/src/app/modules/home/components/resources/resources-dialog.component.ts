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

import { AfterViewInit, Component, Inject, SkipSelf, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormGroupDirective, NgForm, UntypedFormControl } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { map } from 'rxjs/operators';
import { ResourcesLibraryComponent } from "@home/components/resources/resources-library.component";
import { ErrorStateMatcher } from "@angular/material/core";
import { Resource, ResourceType } from "@shared/models/resource.models";
import { ResourceService } from "@core/http/resource.service";

export interface ResourcesDialogData {
  resources?: Resource;
  isAdd?: boolean;
}

@Component({
  selector: 'tb-resources-dialog',
  templateUrl: './resources-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ResourcesDialogComponent}],
  styleUrls: ['./resources-dialog.component.scss']
})
export class ResourcesDialogComponent extends DialogComponent<ResourcesDialogComponent, Resource> implements ErrorStateMatcher, AfterViewInit {

  readonly entityType = EntityType;

  ResourceType = ResourceType;

  isAdd = false;

  submitted = false;

  resources: Resource;

  @ViewChild('resourcesComponent', {static: true}) resourcesComponent: ResourcesLibraryComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<ResourcesDialogComponent, Resource>,
              @Inject(MAT_DIALOG_DATA) public data: ResourcesDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private resourceService: ResourceService) {
    super(store, router, dialogRef);

    if (this.data.isAdd) {
      this.isAdd = true;
    }

    if (this.data.resources) {
      this.resources = this.data.resources;
    }
  }

  ngAfterViewInit(): void {
    if (this.isAdd) {
      setTimeout(() => {
        this.resourcesComponent.entityForm.markAsDirty();
      }, 0);
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    if (this.resourcesComponent.entityForm.valid) {
      const resource = {...this.resourcesComponent.entityFormValue()};
      if (Array.isArray(resource.data)) {
        const resources = [];
        resource.data.forEach((data, index) => {
          resources.push({
            resourceType: resource.resourceType,
            data,
            fileName: resource.fileName[index],
            title: resource.title
          });
        });
        this.resourceService.saveResources(resources, {resendRequest: true}).pipe(
          map((response) => response[0])
        ).subscribe(result => this.dialogRef.close(result));
      } else {
        if (resource.resourceType !== ResourceType.GENERAL) {
          delete resource.descriptor;
        }
        this.resourceService.saveResource(resource).subscribe(result => this.dialogRef.close(result));
      }
    }
  }
}
