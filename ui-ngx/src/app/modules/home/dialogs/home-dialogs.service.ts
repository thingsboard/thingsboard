// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { EntityType } from '@shared/models/entity-type.models';
import { Observable } from 'rxjs';
import {
  ImportDialogCsvComponent,
  ImportDialogCsvData
} from '@shared/import-export/import-dialog-csv.component';

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
      case EntityType.EDGE:
        return this.openImportDialogCSV(entityType, 'edge.import', 'edge.edge-file');
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
