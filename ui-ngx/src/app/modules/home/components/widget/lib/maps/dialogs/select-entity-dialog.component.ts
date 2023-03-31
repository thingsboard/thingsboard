///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { FormattedData } from '@shared/models/widget.models';
import { GenericFunction } from '@home/components/widget/lib/maps/map-models';
import { fillDataPattern, processDataPattern, safeExecute } from '@core/utils';
import { parseWithTranslation } from '@home/components/widget/lib/maps/common-maps-utils';

export interface SelectEntityDialogData {
  entities: FormattedData[];
  labelSettings: {
    showLabel: boolean;
    useLabelFunction: boolean;
    parsedLabelFunction: GenericFunction;
    label: string;
  };
}

@Component({
  selector: 'tb-select-entity-dialog',
  templateUrl: './select-entity-dialog.component.html',
  styleUrls: ['./select-entity-dialog.component.scss']
})
export class SelectEntityDialogComponent extends DialogComponent<SelectEntityDialogComponent, FormattedData> {
  selectEntityFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SelectEntityDialogData,
              public dialogRef: MatDialogRef<SelectEntityDialogComponent, FormattedData>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.selectEntityFormGroup = this.fb.group(
      {
        entity: ['', Validators.required]
      }
    );
  }

  public parseName(entity) {
    let name;
    if (this.data.labelSettings?.showLabel) {
      const pattern = this.data.labelSettings.useLabelFunction ? safeExecute(this.data.labelSettings.parsedLabelFunction,
        [entity, this.data.entities, entity.dsIndex]) : this.data.labelSettings.label;
      const markerLabelText = parseWithTranslation.prepareProcessPattern(pattern, true);
      const replaceInfoLabelMarker = processDataPattern(pattern, entity);
      name = fillDataPattern(markerLabelText, replaceInfoLabelMarker, entity);
    } else {
      name = entity.entityName;
    }
    return name;
  }

  save(): void {
    this.dialogRef.close(this.selectEntityFormGroup.value.entity);
  }
}
