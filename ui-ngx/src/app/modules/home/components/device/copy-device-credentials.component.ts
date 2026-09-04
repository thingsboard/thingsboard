// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { EntityId } from '@shared/models/id/entity-id';
import { DeviceService } from '@core/http/device.service';
import { DeviceCredentials, DeviceCredentialsType } from '@shared/models/device.models';
import { isDefinedAndNotNull, isEqual } from '@core/utils';
import { BehaviorSubject, Subject, Subscription } from 'rxjs';
import { distinctUntilChanged, filter, mergeMap, tap } from 'rxjs/operators';
import { EntityType } from '@shared/models/entity-type.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'tb-copy-device-credentials',
    templateUrl: './copy-device-credentials.component.html',
    styleUrls: [],
    standalone: false
})
export class CopyDeviceCredentialsComponent implements OnInit, OnDestroy {

  private deviceId$ = new BehaviorSubject<EntityId>(null);

  private tooltipMessage: string;

  public hideButton = true;

  public credential: string;

  public loading = false;

  public buttonLabel: string;

  @Input()
  set deviceId(deviceId: EntityId) {
    this.deviceId$.next(deviceId);
  }

  @Input() disabled: boolean;

  @Input()
  credentials$: Subject<DeviceCredentials>;

  private credentialsSubscription: Subscription = null;

  constructor(private store: Store<AppState>,
              private translate: TranslateService,
              private deviceService: DeviceService,
              private cd: ChangeDetectorRef
  ) {
    this.deviceId$.pipe(
      filter(device => isDefinedAndNotNull(device) && device.entityType === EntityType.DEVICE),
      distinctUntilChanged((prev, curr) => prev.id === curr.id),
      tap(() => this.loading = true),
      mergeMap(device => this.deviceService.getDeviceCredentials(device.id))
    ).subscribe(deviceCredentials => {
      this.processingValue(deviceCredentials);
      this.loading = false;
    });
  }

  ngOnInit(): void {
    this.credentialsSubscription = this.credentials$.pipe(
      filter(credential => isDefinedAndNotNull(credential)),
      distinctUntilChanged((prev, curr) => isEqual(prev, curr))
    ).subscribe(deviceCredentials => {
      this.processingValue(deviceCredentials);
      this.cd.detectChanges();
    });
  }

  ngOnDestroy(): void {
    this.deviceId$.unsubscribe();
    if (this.credentialsSubscription !== null) {
      this.credentialsSubscription.unsubscribe();
    }
  }

  private processingValue(credential: DeviceCredentials): void {
    switch (credential.credentialsType) {
      case DeviceCredentialsType.ACCESS_TOKEN:
        this.hideButton = false;
        this.credential = credential.credentialsId;
        this.buttonLabel = this.translate.instant('device.copyAccessToken');
        this.tooltipMessage = this.translate.instant('device.accessTokenCopiedMessage');
        break;
      case DeviceCredentialsType.MQTT_BASIC:
        this.hideButton = false;
        this.credential = this.convertObjectToString(JSON.parse(credential.credentialsValue));
        this.buttonLabel = this.translate.instant('device.copy-mqtt-authentication');
        this.tooltipMessage = this.translate.instant('device.mqtt-authentication-copied-message');
        break;
      default:
        this.hideButton = true;
        this.credential = null;
        this.buttonLabel = '';
        this.tooltipMessage = '';
    }
  }

  private convertObjectToString(obj: object): string {
    Object.keys(obj).forEach(k => (!obj[k] && obj[k] !== undefined) && delete obj[k]);
    return JSON.stringify(obj).replace(/"([^"]+)":/g, '$1:');
  }

  onCopyCredential() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.tooltipMessage,
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
