///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { EntityType } from '@shared/models/entity-type.models';
import { Observable } from 'rxjs';
import {
  ImportDialogCsvComponent,
  ImportDialogCsvData
} from '@home/components/import-export/import-dialog-csv.component';

@Injectable()
export class HomeDialogsService {
  constructor(
    private dialog: MatDialog
  ) {
  }

  public importEntities(entityType: EntityType): Observable<boolean> {
    switch (entityType) {
      case EntityType.DEVICE:
        return this.openImportDialogCSV(entityType, 'device.import', 'device.device-file');
      case EntityType.ASSET:
        return this.openImportDialogCSV(entityType, 'asset.import', 'asset.asset-file');
    }
  }

  private openImportDialogCSV(entityType: EntityType, importTitle: string, importFileLabel: string): Observable<boolean> {
    return this.dialog.open<ImportDialogCsvComponent, ImportDialogCsvData,
      any>(ImportDialogCsvComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityType,
        importTitle,
        importFileLabel
      }
    }).afterClosed();
  }
}
