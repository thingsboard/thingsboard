// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { NotificationService } from '@core/http/notification.service';
import { DeviceService } from '@core/http/device.service';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { DashboardService } from '@core/http/dashboard.service';
import { AlarmService } from '@core/http/alarm.service';
import { AttributeService } from '@core/http/attribute.service';
import { UserService } from '@core/http/user.service';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { CalculatedFieldType } from '@shared/models/calculated-field.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { AlarmInfo, AlarmSeverity, alarmSeverityTranslations } from '@shared/models/alarm.models';
import { ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { DeviceInfo } from '@shared/models/device.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { HttpClient } from '@angular/common/http';
import { AiRecordRef, AiRecordState, AiRecordView } from '@home/components/ai/ai-record.models';

const CONFIG: RequestConfig = {ignoreLoading: true, ignoreErrors: true};

const NOTIFICATION_PAGES: Partial<Record<EntityType, string>> = {
  [EntityType.NOTIFICATION_RULE]: '/notification/rules',
  [EntityType.NOTIFICATION_TEMPLATE]: '/notification/templates',
  [EntityType.NOTIFICATION_TARGET]: '/notification/recipients'
};

const PROFILE_TYPES = new Set<string>([EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE]);

const HUB_KINDS: Record<string, string> = {
  [ItemType.DEVICE]: EntityType.DEVICE,
  [ItemType.WIDGET]: EntityType.WIDGET_TYPE,
  [ItemType.CALCULATED_FIELD]: EntityType.CALCULATED_FIELD,
  [ItemType.ALARM_RULE]: 'ALARM_RULE',
  [ItemType.RULE_CHAIN]: EntityType.RULE_CHAIN,
  [ItemType.SOLUTION_TEMPLATE]: 'SOLUTION_TEMPLATE'
};

export interface AiRecordLoadOptions {
  image?: boolean;
}

@Injectable({providedIn: 'root'})
export class AiRecordLoaderService {

  private entityService = inject(EntityService);
  private calculatedFieldsService = inject(CalculatedFieldsService);
  private notificationService = inject(NotificationService);
  private deviceService = inject(DeviceService);
  private deviceProfileService = inject(DeviceProfileService);
  private dashboardService = inject(DashboardService);
  private alarmService = inject(AlarmService);
  private attributeService = inject(AttributeService);
  private userService = inject(UserService);
  private iotHubApi = inject(IotHubApiService);
  private translate = inject(TranslateService);
  private http = inject(HttpClient);

  loadAll(refs: AiRecordRef[], options: AiRecordLoadOptions = {}): Observable<AiRecordView[]> {
    if (!refs.length) {
      return of([]);
    }
    return forkJoin(refs.map(ref => this.load(ref, options).pipe(catchError(() => of(null)))))
      .pipe(map(views => views.filter(view => !!view)));
  }

  load(ref: AiRecordRef, options: AiRecordLoadOptions = {}): Observable<AiRecordView | null> {
    return 'versionId' in ref ? this.hub(ref.versionId) : this.entity({entityType: ref.entityType, id: ref.id} as EntityId, options);
  }

  hub(versionId: string): Observable<AiRecordView | null> {
    if (!this.iotHubApi.baseUrl) {
      return of(null);
    }
    return this.iotHubApi.getVersionInfo(versionId, CONFIG).pipe(map(item => item ? this.hubView(item) : null));
  }

  hubView(item: MpItemVersionView): AiRecordView {
    const typeKey = itemTypeTranslations.get(item.type);
    const installs = item.totalInstallCount ?? item.installCount;
    return {
      key: `HUB:${item.id}`,
      entityType: HUB_KINDS[item.type] ?? 'SOLUTION_TEMPLATE',
      alarmRule: item.type === ItemType.ALARM_RULE,
      name: item.name,
      meta: '',
      url: `/iot-hub/version/${item.id}`,
      hub: {
        type: typeKey ? this.translate.instant(typeKey) : item.type,
        creator: item.creatorDisplayName || undefined,
        verified: !!item.creatorVerified,
        installs: installs ? this.translate.instant('ai-assistant.record.installs', {count: installs.toLocaleString(this.locale())}) : undefined
      },
      hubItem: item,
      description: item.description || undefined,
      imageUrl: this.hubImage(item),
      square: item.type === ItemType.DEVICE,
      wide: item.type === ItemType.SOLUTION_TEMPLATE
    };
  }

  entity(entity: EntityId, options: AiRecordLoadOptions = {}): Observable<AiRecordView | null> {
    const entityType = entity.entityType as EntityType;
    const key = `${entityType}:${entity.id}`;
    const url = getEntityDetailsPageURL(entity.id, entityType) || undefined;
    switch (entityType) {
      case EntityType.DEVICE:
        return this.deviceService.getDeviceInfo(entity.id, CONFIG).pipe(switchMap(device => this.device(key, url, device, options)));
      case EntityType.DASHBOARD:
        return this.dashboardService.getDashboard(entity.id, CONFIG).pipe(map(dashboard => {
          const widgets = Object.keys(dashboard.configuration?.widgets ?? {}).length;
          return {
            key, entityType, alarmRule: false, name: dashboard.title, url, widgets, image: dashboard.image || undefined,
            meta: this.meta(this.typeLabel(entityType), widgets ? this.translate.instant('ai-assistant.record.widgets', {count: widgets}) : null)
          };
        }));
      case EntityType.CALCULATED_FIELD:
        return this.calculatedFieldsService.getCalculatedFieldById(entity.id, CONFIG).pipe(switchMap(field => {
          const alarmRule = field.type === CalculatedFieldType.ALARM;
          const type = this.translate.instant(alarmRule ? 'ai-assistant.review.type.ALARM_RULE' : 'entity.type-calculated-field');
          const view = (target?: string): AiRecordView => ({
            key, entityType, alarmRule, name: field.name, meta: this.meta(type, target),
            url: alarmRule ? `/alarms/alarm-rules/${entity.id}` : `/calculatedFields/${entity.id}`
          });
          const owner = field.entityId;
          if (!owner?.id) {
            return of(view());
          }
          return this.entityService.getEntity(owner.entityType as EntityType, owner.id, CONFIG).pipe(
            map(found => view(found?.name ? this.onTarget(owner.entityType, found.name) : undefined)),
            catchError(() => of(view()))
          );
        }));
      case EntityType.ALARM:
        return this.alarmService.getAlarmInfo(entity.id, CONFIG).pipe(map(alarm => this.alarm(key, alarm)));
      case EntityType.USER:
        return this.userService.getUser(entity.id, CONFIG).pipe(map(user => {
          const name = [user.firstName, user.lastName].filter(Boolean).join(' ') || user.email;
          const invited = user.additionalInfo?.userActivated === false;
          return {
            key, entityType, alarmRule: false, name, url,
            meta: this.meta(this.typeLabel(entityType), name !== user.email ? user.email : null),
            state: invited ? {tone: 'wait', label: this.translate.instant('ai-assistant.record.invited')} : undefined
          };
        }));
      case EntityType.NOTIFICATION_RULE:
        return this.notificationService.getNotificationRuleById(entity.id, CONFIG)
          .pipe(map(rule => this.simple(key, entityType, rule.name, NOTIFICATION_PAGES[entityType])));
      case EntityType.NOTIFICATION_TEMPLATE:
        return this.notificationService.getNotificationTemplateById(entity.id, CONFIG)
          .pipe(map(template => this.simple(key, entityType, template.name, NOTIFICATION_PAGES[entityType])));
      default:
        return this.entityService.getEntity(entityType, entity.id, CONFIG).pipe(map(found => {
          const view = this.simple(key, entityType, found?.name ?? (found as {title?: string})?.title, NOTIFICATION_PAGES[entityType] ?? url);
          const subtype = (found as {type?: string})?.type;
          if (view && subtype && (entityType === EntityType.ASSET || entityType === EntityType.ENTITY_VIEW)) {
            view.meta = this.meta(view.meta, subtype);
          }
          return view;
        }));
    }
  }

  private device(key: string, url: string, device: DeviceInfo, options: AiRecordLoadOptions): Observable<AiRecordView> {
    const gateway = !!device.additionalInfo?.gateway;
    const view: AiRecordView = {
      key, entityType: gateway ? 'GATEWAY' : EntityType.DEVICE, alarmRule: false, name: device.name, url,
      meta: this.meta(this.translate.instant(gateway ? 'ai-assistant.record.gateway' : 'entity.type-device'), device.type),
      square: true
    };
    const activity = device.active ? of(true) : this.attributeService.getEntityAttributes(device.id, AttributeScope.SERVER_SCOPE, ['lastActivityTime'], CONFIG)
      .pipe(
        map(attributes => Number(attributes?.find(attribute => attribute.key === 'lastActivityTime')?.value) > 0),
        switchMap(seen => seen ? of(true) : this.http.get<string[]>(`/api/plugins/telemetry/DEVICE/${device.id.id}/keys/timeseries`,
          defaultHttpOptionsFromConfig(CONFIG)).pipe(map(keys => !!keys?.length))),
        catchError(() => of(true))
      );
    const image = options.image && device.deviceProfileId?.id
      ? this.deviceProfileService.getDeviceProfileInfo(device.deviceProfileId.id, CONFIG).pipe(map(profile => profile?.image || undefined), catchError(() => of(undefined)))
      : of(undefined);
    return forkJoin([activity, image]).pipe(map(([seen, picture]) => ({
      ...view,
      image: picture,
      state: this.deviceState(device.active, seen, gateway)
    })));
  }

  private deviceState(active: boolean, seen: boolean, gateway: boolean): AiRecordState {
    if (active) {
      return {tone: 'ok', label: this.translate.instant('ai-assistant.record.active')};
    }
    if (!seen) {
      return {tone: 'wait', label: this.translate.instant(gateway ? 'ai-assistant.record.waiting-connect' : 'ai-assistant.record.waiting-data')};
    }
    return {tone: 'off', label: this.translate.instant('ai-assistant.record.inactive')};
  }

  private alarm(key: string, alarm: AlarmInfo): AiRecordView {
    const severity = alarm.severity ?? AlarmSeverity.INDETERMINATE;
    const word = this.translate.instant(alarmSeverityTranslations.get(severity) ?? severity);
    const tip = [word, this.translate.instant(alarm.cleared ? 'ai-assistant.record.cleared' : 'ai-assistant.record.alarm-active').toLowerCase(),
      this.translate.instant(alarm.acknowledged ? 'ai-assistant.record.acknowledged' : 'ai-assistant.record.not-acknowledged').toLowerCase()].join(', ');
    const when = alarm.cleared && alarm.clearTs
      ? this.translate.instant('ai-assistant.record.cleared-at', {time: this.time(alarm.clearTs)})
      : alarm.startTs ? this.translate.instant('ai-assistant.record.since', {time: this.time(alarm.startTs)}) : null;
    return {
      key, entityType: EntityType.ALARM, alarmRule: false, name: alarm.type,
      meta: this.meta(alarm.originatorName, when),
      url: '/alarms',
      alarm: {severity, cleared: !!alarm.cleared, acknowledged: !!alarm.acknowledged, tip}
    };
  }

  private hubImage(item: MpItemVersionView): string | undefined {
    if (!item.image) {
      return undefined;
    }
    const [path, query] = item.image.split('?');
    const preview = path.endsWith('/preview') ? path : `${path}/preview`;
    return this.iotHubApi.resolveResourceUrl(query ? `${preview}?${query}` : preview);
  }

  private simple(key: string, entityType: EntityType, name: string, url?: string): AiRecordView | null {
    return name ? {key, entityType, alarmRule: false, name, meta: this.typeLabel(entityType), url} : null;
  }

  private onTarget(entityType: string, name: string): string {
    return this.translate.instant(PROFILE_TYPES.has(entityType) ? 'ai-assistant.record.on-profile' : 'ai-assistant.record.on', {name});
  }

  private meta(...parts: Array<string | null | undefined>): string {
    return parts.filter(part => !!part).join(' · ');
  }

  private typeLabel(entityType: EntityType): string {
    const translation = entityTypeTranslations.get(entityType)?.type;
    return translation ? this.translate.instant(translation) : entityType;
  }

  private locale(): string | undefined {
    return this.translate.currentLang?.replace('_', '-') || undefined;
  }

  private time(ts: number): string {
    const date = new Date(ts);
    const today = new Date().toDateString() === date.toDateString();
    return new Intl.DateTimeFormat(this.locale(), today ? {hour: '2-digit', minute: '2-digit'}
      : {day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'}).format(date);
  }
}
