// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { DefaultConverterUpdate, defaultConverterUpdateRequest } from '@home/components/iot-hub/iot-hub-default-converter';
import { downloadEntityJson, readableMessage, requestFailureMessage, splitTextWithLink } from '@home/components/iot-hub/iot-hub-utils';

export interface IotHubConverterUpdateDialogData {
  update: DefaultConverterUpdate;
  // The version of the converter the dialog downloads: the server refuses the update if someone saved the converter since
  version?: number;
  // Someone changed the converter since a package installed or last updated it
  modified: boolean;
  // The names of the other installed packages that use the converter
  usedBy: string[];
}

const USED_BY_SHOWN = 3;

/**
 * Brings an installed device package's default converter to ThingsBoard's default converter now, without a new version of
 * the package. Closes with true once it is updated.
 */
@Component({
  selector: 'tb-iot-hub-converter-update-dialog',
  standalone: false,
  templateUrl: './iot-hub-converter-update-dialog.component.html',
  styleUrls: ['./iot-hub-converter-update-dialog.component.scss']
})
export class TbIotHubConverterUpdateDialogComponent extends DialogComponent<TbIotHubConverterUpdateDialogComponent, boolean> {

  readonly modifiedHint = splitTextWithLink(this.translate.instant('iot-hub.device-update-modified'));
  readonly usedByShown: string[];
  readonly usedByHidden: string[];
  usedByExpanded = false;
  updating = false;
  errorMessage: string | null = null;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubConverterUpdateDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubConverterUpdateDialogData,
    private iotHubApiService: IotHubApiService,
    private translate: TranslateService
  ) {
    super(store, router, dialogRef);
    // Collapse only when at least 2 packages would be hidden
    const shownCount = data.usedBy.length > USED_BY_SHOWN + 1 ? USED_BY_SHOWN : data.usedBy.length;
    this.usedByShown = data.usedBy.slice(0, shownCount);
    this.usedByHidden = data.usedBy.slice(shownCount);
  }

  downloadCurrent(): void {
    downloadEntityJson(this.data.update.converter, this.data.update.converter.name);
  }

  update(): void {
    this.updating = true;
    this.errorMessage = null;
    this.iotHubApiService.updateDevicePackage(this.data.update.installedItem.id.id,
      defaultConverterUpdateRequest(this.data.update, this.data.version), { ignoreErrors: true }).subscribe({
      next: result => {
        if (result.success) {
          this.dialogRef.close(true);
          return;
        }
        // Nothing was changed: the server rolled it back
        this.updating = false;
        this.errorMessage = readableMessage(result.errorMessage) ?? this.translate.instant('iot-hub.device-install-unexpected-error');
      },
      error: error => {
        this.updating = false;
        this.errorMessage = requestFailureMessage(error, this.translate);
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
