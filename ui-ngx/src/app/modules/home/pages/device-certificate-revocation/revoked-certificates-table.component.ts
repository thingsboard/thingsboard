// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, filter, map, mergeMap } from 'rxjs/operators';
import {
  DateEntityTableColumn,
  EntityLinkTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType, EntityTypeResource } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { Direction } from '@shared/models/page/sort-order';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { Device } from '@shared/models/device.models';
import {
  DeviceCertificateRevocation,
  DeviceCertificateRevocationInfo,
  DeviceCertificateRevocationRequest
} from '@shared/models/device-certificate-revocation.models';
import { DeviceCertificateRevocationService } from '@core/http/device-certificate-revocation.service';
import { DeviceService } from '@core/http/device.service';
import { DialogService } from '@core/services/dialog.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import {
  RevokeDeviceCertificateDialogComponent,
  RevokeDeviceCertificateDialogData
} from '@home/components/device/revoke-device-certificate-dialog.component';

const escapeHtml = (text: string): string =>
  (text ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

@Component({
  selector: 'tb-revoked-certificates-table',
  templateUrl: './revoked-certificates-table.component.html',
  standalone: false
})
export class RevokedCertificatesTableComponent implements OnInit {

  readonly revokedCertificatesTableConfig = new EntityTableConfig<DeviceCertificateRevocationInfo>();

  constructor(private revocationService: DeviceCertificateRevocationService,
              private deviceService: DeviceService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private datePipe: DatePipe) {
  }

  ngOnInit() {
    const config = this.revokedCertificatesTableConfig;
    const canWrite = this.userPermissionsService.hasGenericPermission(Resource.DEVICE, Operation.WRITE_CREDENTIALS);
    config.tableTitle = this.translate.instant('device.certificate-revocation.revoked-certificates');
    config.detailsPanelEnabled = false;
    config.searchEnabled = true;
    config.selectionEnabled = canWrite;
    config.entitiesDeleteEnabled = false;
    config.deleteEnabled = () => false;
    config.entityTranslations = {
      add: 'device.certificate-revocation.revoke-certificate',
      noEntities: 'device.certificate-revocation.no-revoked-certificates',
      search: 'device.certificate-revocation.search',
      selectedEntities: 'device.certificate-revocation.selected-certificates'
    };
    config.entityResources = {} as EntityTypeResource<DeviceCertificateRevocationInfo>;
    config.entitiesFetchFunction = pageLink => this.fetchRevocations(pageLink);
    config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};
    config.addEnabled = canWrite;
    config.addEntity = () => this.revokeCertificate();

    config.columns.push(
      new DateEntityTableColumn<DeviceCertificateRevocationInfo>('createdTime', 'device.certificate-revocation.revoked-time',
        this.datePipe, '150px'),
      // Cells are rendered as HTML, and the subject is chosen by whoever made the certificate. Tooltips are plain text.
      new EntityTableColumn<DeviceCertificateRevocationInfo>('subject', 'device.certificate-revocation.subject', '30%',
        revocation => escapeHtml(revocation.subject), () => ({}), true, () => ({}), revocation => revocation.subject),
      new EntityTableColumn<DeviceCertificateRevocationInfo>('serialNumber', 'device.certificate-revocation.serial-number', '20%',
        revocation => revocation.serialNumber?.toUpperCase()),
      new EntityTableColumn<DeviceCertificateRevocationInfo>('notAfter', 'device.certificate-revocation.expires', '150px',
        revocation => revocation.expired ? this.translate.instant('device.certificate-revocation.expired') :
          this.datePipe.transform(revocation.notAfter, 'yyyy-MM-dd'),
        revocation => revocation.expired ? {color: 'rgba(0, 0, 0, 0.38)'} : {}, true, () => ({}),
        revocation => revocation.expired ? this.translate.instant('device.certificate-revocation.expired-hint',
          {date: this.datePipe.transform(revocation.notAfter, 'yyyy-MM-dd')}) : undefined),
      new EntityLinkTableColumn<DeviceCertificateRevocationInfo>('deviceName', 'device.certificate-revocation.device', '20%',
        revocation => escapeHtml(revocation.deviceName),
        revocation => revocation.deviceId ? getEntityDetailsPageURL(revocation.deviceId.id, EntityType.DEVICE) : '', false),
      new EntityTableColumn<DeviceCertificateRevocationInfo>('reason', 'device.certificate-revocation.reason', '30%',
        revocation => escapeHtml(revocation.reason), () => ({}), true, () => ({}), revocation => revocation.reason)
    );

    config.cellActionDescriptors.push({
      name: this.translate.instant('device.certificate-revocation.restore'),
      icon: 'settings_backup_restore',
      isEnabled: () => canWrite,
      onAction: ($event, revocation) => this.restore($event, [revocation])
    });
    config.groupActionDescriptors.push({
      name: this.translate.instant('device.certificate-revocation.restore'),
      icon: 'settings_backup_restore',
      isEnabled: canWrite,
      onAction: ($event, revocations) => this.restore($event, revocations)
    });
  }

  private fetchRevocations(pageLink: PageLink): Observable<PageData<DeviceCertificateRevocationInfo>> {
    return this.revocationService.getRevocations(pageLink).pipe(
      mergeMap(pageData => {
        const deviceIds = Array.from(new Set(pageData.data.filter(revocation => revocation.deviceId)
          .map(revocation => revocation.deviceId.id)));
        if (!deviceIds.length) {
          return of(pageData);
        }
        // A deleted device is missing from the result and its revocation is shown without a device
        return this.deviceService.getDevices(deviceIds, {ignoreErrors: true}).pipe(
          catchError(() => of([] as Device[])),
          map(devices => {
            const deviceNames = new Map(devices.map(device => [device.id.id, device.name]));
            return {
              ...pageData,
              data: pageData.data.map(revocation => ({
                ...revocation,
                deviceName: revocation.deviceId ? deviceNames.get(revocation.deviceId.id) : undefined
              }))
            };
          })
        );
      })
    );
  }

  private revokeCertificate(): Observable<DeviceCertificateRevocation> {
    return this.dialog.open<RevokeDeviceCertificateDialogComponent, RevokeDeviceCertificateDialogData,
      DeviceCertificateRevocationRequest>(RevokeDeviceCertificateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {}
    }).afterClosed().pipe(
      mergeMap(request => request ? this.revocationService.revokeCertificate(request) : of(null))
    );
  }

  private restore($event: Event, revocations: DeviceCertificateRevocationInfo[]) {
    if ($event) {
      $event.stopPropagation();
    }
    if (!revocations?.length) {
      return;
    }
    const title = revocations.length === 1 ?
      this.translate.instant('device.certificate-revocation.restore-certificate-title', {subject: revocations[0].subject}) :
      this.translate.instant('device.certificate-revocation.restore-certificates-title', {count: revocations.length});
    const content = this.translate.instant('device.certificate-revocation.restore-certificate-text');
    this.dialogService.confirm(title, content, this.translate.instant('action.no'), this.translate.instant('action.yes')).pipe(
      filter(result => result),
      mergeMap(() => forkJoin(revocations.map(revocation => this.revocationService.deleteRevocation(revocation.id.id))))
    ).subscribe(() => this.revokedCertificatesTableConfig.updateData());
  }
}
