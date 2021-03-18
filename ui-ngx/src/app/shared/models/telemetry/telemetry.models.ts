///
/// Copyright © 2016-2021 The Thingsboard Authors
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


import { EntityType } from '@shared/models/entity-type.models';
import { AggregationType } from '../time/time.models';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { map } from 'rxjs/operators';
import { NgZone } from '@angular/core';
import {
  AlarmData,
  AlarmDataQuery, EntityCountQuery,
  EntityData,
  EntityDataQuery,
  EntityKey,
  TsValue
} from '@shared/models/query/query.models';
import { PageData } from '@shared/models/page/page-data';
import { alarmFields } from '@shared/models/alarm.models';
import { entityFields } from '@shared/models/entity.models';
import { isUndefined } from '@core/utils';

export enum DataKeyType {
  timeseries = 'timeseries',
  attribute = 'attribute',
  function = 'function',
  alarm = 'alarm',
  entityField = 'entityField',
  count = 'count'
}

export enum LatestTelemetry {
  LATEST_TELEMETRY = 'LATEST_TELEMETRY'
}

export enum AttributeScope {
  CLIENT_SCOPE = 'CLIENT_SCOPE',
  SERVER_SCOPE = 'SERVER_SCOPE',
  SHARED_SCOPE = 'SHARED_SCOPE'
}

export enum TelemetryFeature {
  ATTRIBUTES = 'ATTRIBUTES',
  TIMESERIES = 'TIMESERIES'
}

export type TelemetryType = LatestTelemetry | AttributeScope;

export function toTelemetryType(val: string): TelemetryType {
  if (LatestTelemetry[val]) {
    return LatestTelemetry[val];
  } else {
    return AttributeScope[val];
  }
}

export const telemetryTypeTranslations = new Map<TelemetryType, string>(
  [
    [LatestTelemetry.LATEST_TELEMETRY, 'attribute.scope-latest-telemetry'],
    [AttributeScope.CLIENT_SCOPE, 'attribute.scope-client'],
    [AttributeScope.SERVER_SCOPE, 'attribute.scope-server'],
    [AttributeScope.SHARED_SCOPE, 'attribute.scope-shared']
  ]
);

export const isClientSideTelemetryType = new Map<TelemetryType, boolean>(
  [
    [LatestTelemetry.LATEST_TELEMETRY, true],
    [AttributeScope.CLIENT_SCOPE, true],
    [AttributeScope.SERVER_SCOPE, false],
    [AttributeScope.SHARED_SCOPE, false]
  ]
);

export interface AttributeData {
  lastUpdateTs?: number;
  key: string;
  value: any;
}

export interface TimeseriesData {
  [key: string]: Array<TsValue>;
}

export enum DataSortOrder {
  ASC = 'ASC',
  DESC = 'DESC'
}

export interface WebsocketCmd {
  cmdId: number;
}

export interface TelemetryPluginCmd extends WebsocketCmd {
  keys: string;
}

export abstract class SubscriptionCmd implements TelemetryPluginCmd {
  cmdId: number;
  keys: string;
  entityType: EntityType;
  entityId: string;
  scope?: AttributeScope;
  unsubscribe: boolean;
  abstract getType(): TelemetryFeature;
}

export class AttributesSubscriptionCmd extends SubscriptionCmd {
  getType() {
    return TelemetryFeature.ATTRIBUTES;
  }
}

export class TimeseriesSubscriptionCmd extends SubscriptionCmd {
  startTs: number;
  timeWindow: number;
  interval: number;
  limit: number;
  agg: AggregationType;

  getType() {
    return TelemetryFeature.TIMESERIES;
  }
}

export class GetHistoryCmd implements TelemetryPluginCmd {
  cmdId: number;
  keys: string;
  entityType: EntityType;
  entityId: string;
  startTs: number;
  endTs: number;
  interval: number;
  limit: number;
  agg: AggregationType;
}

export interface EntityHistoryCmd {
  keys: Array<string>;
  startTs: number;
  endTs: number;
  interval: number;
  limit: number;
  agg: AggregationType;
  fetchLatestPreviousPoint?: boolean;
}

export interface LatestValueCmd {
  keys: Array<EntityKey>;
}

export interface TimeSeriesCmd {
  keys: Array<string>;
  startTs: number;
  timeWindow: number;
  interval: number;
  limit: number;
  agg: AggregationType;
  fetchLatestPreviousPoint?: boolean;
}

export class EntityDataCmd implements WebsocketCmd {
  cmdId: number;
  query?: EntityDataQuery;
  historyCmd?: EntityHistoryCmd;
  latestCmd?: LatestValueCmd;
  tsCmd?: TimeSeriesCmd;

  public isEmpty(): boolean {
    return !this.query && !this.historyCmd && !this.latestCmd && !this.tsCmd;
  }
}

export class EntityCountCmd implements WebsocketCmd {
  cmdId: number;
  query?: EntityCountQuery;
}

export class AlarmDataCmd implements WebsocketCmd {
  cmdId: number;
  query?: AlarmDataQuery;

  public isEmpty(): boolean {
    return !this.query;
  }
}

export class EntityDataUnsubscribeCmd implements WebsocketCmd {
  cmdId: number;
}

export class EntityCountUnsubscribeCmd implements WebsocketCmd {
  cmdId: number;
}

export class AlarmDataUnsubscribeCmd implements WebsocketCmd {
  cmdId: number;
}

export class TelemetryPluginCmdsWrapper {
  attrSubCmds: Array<AttributesSubscriptionCmd>;
  tsSubCmds: Array<TimeseriesSubscriptionCmd>;
  historyCmds: Array<GetHistoryCmd>;
  entityDataCmds: Array<EntityDataCmd>;
  entityDataUnsubscribeCmds: Array<EntityDataUnsubscribeCmd>;
  alarmDataCmds: Array<AlarmDataCmd>;
  alarmDataUnsubscribeCmds: Array<AlarmDataUnsubscribeCmd>;
  entityCountCmds: Array<EntityCountCmd>;
  entityCountUnsubscribeCmds: Array<EntityCountUnsubscribeCmd>;

  constructor() {
    this.attrSubCmds = [];
    this.tsSubCmds = [];
    this.historyCmds = [];
    this.entityDataCmds = [];
    this.entityDataUnsubscribeCmds = [];
    this.alarmDataCmds = [];
    this.alarmDataUnsubscribeCmds = [];
    this.entityCountCmds = [];
    this.entityCountUnsubscribeCmds = [];
  }

  public hasCommands(): boolean {
    return this.tsSubCmds.length > 0 ||
      this.historyCmds.length > 0 ||
      this.attrSubCmds.length > 0 ||
      this.entityDataCmds.length > 0 ||
      this.entityDataUnsubscribeCmds.length > 0 ||
      this.alarmDataCmds.length > 0 ||
      this.alarmDataUnsubscribeCmds.length > 0 ||
      this.entityCountCmds.length > 0 ||
      this.entityCountUnsubscribeCmds.length > 0;
  }

  public clear() {
    this.attrSubCmds.length = 0;
    this.tsSubCmds.length = 0;
    this.historyCmds.length = 0;
    this.entityDataCmds.length = 0;
    this.entityDataUnsubscribeCmds.length = 0;
    this.alarmDataCmds.length = 0;
    this.alarmDataUnsubscribeCmds.length = 0;
    this.entityCountCmds.length = 0;
    this.entityCountUnsubscribeCmds.length = 0;
  }

  public preparePublishCommands(maxCommands: number): TelemetryPluginCmdsWrapper {
    const preparedWrapper = new TelemetryPluginCmdsWrapper();
    let leftCount = maxCommands;
    preparedWrapper.tsSubCmds = this.popCmds(this.tsSubCmds, leftCount);
    leftCount -= preparedWrapper.tsSubCmds.length;
    preparedWrapper.historyCmds = this.popCmds(this.historyCmds, leftCount);
    leftCount -= preparedWrapper.historyCmds.length;
    preparedWrapper.attrSubCmds = this.popCmds(this.attrSubCmds, leftCount);
    leftCount -= preparedWrapper.attrSubCmds.length;
    preparedWrapper.entityDataCmds = this.popCmds(this.entityDataCmds, leftCount);
    leftCount -= preparedWrapper.entityDataCmds.length;
    preparedWrapper.entityDataUnsubscribeCmds = this.popCmds(this.entityDataUnsubscribeCmds, leftCount);
    leftCount -= preparedWrapper.entityDataUnsubscribeCmds.length;
    preparedWrapper.alarmDataCmds = this.popCmds(this.alarmDataCmds, leftCount);
    leftCount -= preparedWrapper.alarmDataCmds.length;
    preparedWrapper.alarmDataUnsubscribeCmds = this.popCmds(this.alarmDataUnsubscribeCmds, leftCount);
    leftCount -= preparedWrapper.alarmDataUnsubscribeCmds.length;
    preparedWrapper.entityCountCmds = this.popCmds(this.entityCountCmds, leftCount);
    leftCount -= preparedWrapper.entityCountCmds.length;
    preparedWrapper.entityCountUnsubscribeCmds = this.popCmds(this.entityCountUnsubscribeCmds, leftCount);
    return preparedWrapper;
  }

  private popCmds<T>(cmds: Array<T>, leftCount: number): Array<T> {
    const toPublish = Math.min(cmds.length, leftCount);
    if (toPublish > 0) {
      return cmds.splice(0, toPublish);
    } else {
      return [];
    }
  }
}

export interface SubscriptionData {
  [key: string]: [number, any][];
}

export interface SubscriptionDataHolder {
  data: SubscriptionData;
}

export interface SubscriptionUpdateMsg extends SubscriptionDataHolder {
  subscriptionId: number;
  errorCode: number;
  errorMsg: string;
}

export enum CmdUpdateType {
  ENTITY_DATA = 'ENTITY_DATA',
  ALARM_DATA = 'ALARM_DATA',
  COUNT_DATA = 'COUNT_DATA'
}

export interface CmdUpdateMsg {
  cmdId: number;
  errorCode: number;
  errorMsg: string;
  cmdUpdateType: CmdUpdateType;
}

export interface DataUpdateMsg<T> extends CmdUpdateMsg {
  data?: PageData<T>;
  update?: Array<T>;
}

export interface EntityDataUpdateMsg extends DataUpdateMsg<EntityData> {
  cmdUpdateType: CmdUpdateType.ENTITY_DATA;
}

export interface AlarmDataUpdateMsg extends DataUpdateMsg<AlarmData> {
  cmdUpdateType: CmdUpdateType.ALARM_DATA;
  allowedEntities: number;
  totalEntities: number;
}

export interface EntityCountUpdateMsg extends CmdUpdateMsg {
  cmdUpdateType: CmdUpdateType.COUNT_DATA;
  count: number;
}

export type WebsocketDataMsg = AlarmDataUpdateMsg | EntityDataUpdateMsg | EntityCountUpdateMsg | SubscriptionUpdateMsg;

export function isEntityDataUpdateMsg(message: WebsocketDataMsg): message is EntityDataUpdateMsg {
  const updateMsg = (message as CmdUpdateMsg);
  return updateMsg.cmdId !== undefined && updateMsg.cmdUpdateType === CmdUpdateType.ENTITY_DATA;
}

export function isAlarmDataUpdateMsg(message: WebsocketDataMsg): message is AlarmDataUpdateMsg {
  const updateMsg = (message as CmdUpdateMsg);
  return updateMsg.cmdId !== undefined && updateMsg.cmdUpdateType === CmdUpdateType.ALARM_DATA;
}

export function isEntityCountUpdateMsg(message: WebsocketDataMsg): message is EntityCountUpdateMsg {
  const updateMsg = (message as CmdUpdateMsg);
  return updateMsg.cmdId !== undefined && updateMsg.cmdUpdateType === CmdUpdateType.COUNT_DATA;
}

export class SubscriptionUpdate implements SubscriptionUpdateMsg {
  subscriptionId: number;
  errorCode: number;
  errorMsg: string;
  data: SubscriptionData;

  constructor(msg: SubscriptionUpdateMsg) {
    this.subscriptionId = msg.subscriptionId;
    this.errorCode = msg.errorCode;
    this.errorMsg = msg.errorMsg;
    this.data = msg.data;
  }

  public prepareData(keys: string[]) {
    if (!this.data) {
      this.data = {};
    }
    if (keys) {
      keys.forEach((key) => {
        if (!this.data[key]) {
          this.data[key] = [];
        }
      });
    }
  }

  public updateAttributeData(origData: Array<AttributeData>): Array<AttributeData> {
    for (const key of Object.keys(this.data)) {
      const keyData = this.data[key];
      if (keyData.length) {
        const existing = origData.find((data) => data.key === key);
        if (existing) {
          existing.lastUpdateTs = keyData[0][0];
          existing.value = keyData[0][1];
        } else {
          origData.push(
            {
              key,
              lastUpdateTs: keyData[0][0],
              value: keyData[0][1]
            }
          );
        }
      }
    }
    return origData;
  }
}

export class CmdUpdate implements CmdUpdateMsg {
  cmdId: number;
  errorCode: number;
  errorMsg: string;
  cmdUpdateType: CmdUpdateType;

  constructor(msg: CmdUpdateMsg) {
    this.cmdId = msg.cmdId;
    this.errorCode = msg.errorCode;
    this.errorMsg = msg.errorMsg;
    this.cmdUpdateType = msg.cmdUpdateType;
  }
}

export class DataUpdate<T> extends CmdUpdate implements DataUpdateMsg<T> {
  data?: PageData<T>;
  update?: Array<T>;

  constructor(msg: DataUpdateMsg<T>) {
    super(msg);
    this.data = msg.data;
    this.update = msg.update;
  }
}

export class EntityDataUpdate extends DataUpdate<EntityData> {
  constructor(msg: EntityDataUpdateMsg) {
    super(msg);
  }

  public prepareData(tsOffset: number) {
    if (this.data) {
      this.processEntityData(this.data.data, tsOffset);
    }
    if (this.update) {
      this.processEntityData(this.update, tsOffset);
    }
  }

  private processEntityData(data: Array<EntityData>, tsOffset: number) {
    for (const entityData of data) {
      if (entityData.timeseries) {
        for (const key of Object.keys(entityData.timeseries)) {
          const tsValues = entityData.timeseries[key];
          for (const tsValue of tsValues) {
            if (tsValue.ts) {
              tsValue.ts += tsOffset;
            }
          }
        }
      }
      if (entityData.latest) {
        for (const entityKeyType of Object.keys(entityData.latest)) {
          const keyTypeValues = entityData.latest[entityKeyType];
          for (const key of Object.keys(keyTypeValues)) {
            const tsValue = keyTypeValues[key];
            if (tsValue.ts) {
              tsValue.ts += tsOffset;
            }
            if (key === entityFields.createdTime.keyName && tsValue.value) {
              tsValue.value = (Number(tsValue.value) + tsOffset) + '';
            }
          }
        }
      }
    }
  }
}

export class AlarmDataUpdate extends DataUpdate<AlarmData> {
  allowedEntities: number;
  totalEntities: number;

  constructor(msg: AlarmDataUpdateMsg) {
    super(msg);
    this.allowedEntities = msg.allowedEntities;
    this.totalEntities = msg.totalEntities;
  }

  public prepareData(tsOffset: number) {
    if (this.data) {
      this.processAlarmData(this.data.data, tsOffset);
    }
    if (this.update) {
      this.processAlarmData(this.update, tsOffset);
    }
  }

  private processAlarmData(data: Array<AlarmData>, tsOffset: number) {
    for (const alarmData of data) {
      alarmData.createdTime += tsOffset;
      if (alarmData.ackTs) {
        alarmData.ackTs += tsOffset;
      }
      if (alarmData.clearTs) {
        alarmData.clearTs += tsOffset;
      }
      if (alarmData.endTs) {
        alarmData.endTs += tsOffset;
      }
      if (alarmData.latest) {
        for (const entityKeyType of Object.keys(alarmData.latest)) {
          const keyTypeValues = alarmData.latest[entityKeyType];
          for (const key of Object.keys(keyTypeValues)) {
            const tsValue = keyTypeValues[key];
            if (tsValue.ts) {
              tsValue.ts += tsOffset;
            }
            if (key in [entityFields.createdTime.keyName,
                        alarmFields.startTime.keyName,
                        alarmFields.endTime.keyName,
                        alarmFields.ackTime.keyName,
                        alarmFields.clearTime.keyName] && tsValue.value) {
              tsValue.value = (Number(tsValue.value) + tsOffset) + '';
            }
          }
        }
      }
    }
  }
}

export class EntityCountUpdate extends CmdUpdate {
  count: number;

  constructor(msg: EntityCountUpdateMsg) {
    super(msg);
    this.count = msg.count;
  }
}

export interface TelemetryService {
  subscribe(subscriber: TelemetrySubscriber);
  update(subscriber: TelemetrySubscriber);
  unsubscribe(subscriber: TelemetrySubscriber);
}

export class TelemetrySubscriber {

  private dataSubject = new ReplaySubject<SubscriptionUpdate>(1);
  private entityDataSubject = new ReplaySubject<EntityDataUpdate>(1);
  private alarmDataSubject = new ReplaySubject<AlarmDataUpdate>(1);
  private entityCountSubject = new ReplaySubject<EntityCountUpdate>(1);
  private reconnectSubject = new Subject();

  private zone: NgZone;

  private tsOffset = undefined;

  public subscriptionCommands: Array<WebsocketCmd>;

  public data$ = this.dataSubject.asObservable();
  public entityData$ = this.entityDataSubject.asObservable();
  public alarmData$ = this.alarmDataSubject.asObservable();
  public entityCount$ = this.entityCountSubject.asObservable();
  public reconnect$ = this.reconnectSubject.asObservable();

  public static createEntityAttributesSubscription(telemetryService: TelemetryService,
                                                   entityId: EntityId, attributeScope: TelemetryType,
                                                   zone: NgZone, keys: string[] = null): TelemetrySubscriber {
    let subscriptionCommand: SubscriptionCmd;
    if (attributeScope === LatestTelemetry.LATEST_TELEMETRY) {
      subscriptionCommand = new TimeseriesSubscriptionCmd();
    } else {
      subscriptionCommand = new AttributesSubscriptionCmd();
    }
    subscriptionCommand.entityType = entityId.entityType as EntityType;
    subscriptionCommand.entityId = entityId.id;
    subscriptionCommand.scope = attributeScope as AttributeScope;
    if (keys) {
      subscriptionCommand.keys = keys.join(',');
    }
    const subscriber = new TelemetrySubscriber(telemetryService);
    subscriber.zone = zone;
    subscriber.subscriptionCommands.push(subscriptionCommand);
    return subscriber;
  }

  constructor(private telemetryService: TelemetryService) {
    this.subscriptionCommands = [];
  }

  public subscribe() {
    this.telemetryService.subscribe(this);
  }

  public update() {
    this.telemetryService.update(this);
  }

  public unsubscribe() {
    this.telemetryService.unsubscribe(this);
    this.complete();
  }

  public complete() {
    this.dataSubject.complete();
    this.entityDataSubject.complete();
    this.alarmDataSubject.complete();
    this.entityCountSubject.complete();
    this.reconnectSubject.complete();
  }

  public setTsOffset(tsOffset: number): boolean {
    if (this.tsOffset !== tsOffset) {
      const changed = !isUndefined(this.tsOffset);
      this.tsOffset = tsOffset;
      return changed;
    } else {
      return false;
    }
  }

  public onData(message: SubscriptionUpdate) {
    const cmdId = message.subscriptionId;
    let keys: string[];
    const cmd = this.subscriptionCommands.find((command) => command.cmdId === cmdId);
    if (cmd) {
      const telemetryPluginCmd = cmd as TelemetryPluginCmd;
      if (telemetryPluginCmd.keys && telemetryPluginCmd.keys.length) {
        keys = telemetryPluginCmd.keys.split(',');
      }
    }
    message.prepareData(keys);
    if (this.zone) {
     this.zone.run(
       () => {
         this.dataSubject.next(message);
       }
     );
    } else {
      this.dataSubject.next(message);
    }
  }

  public onEntityData(message: EntityDataUpdate) {
    if (this.tsOffset) {
      message.prepareData(this.tsOffset);
    }
    if (this.zone) {
      this.zone.run(
        () => {
          this.entityDataSubject.next(message);
        }
      );
    } else {
      this.entityDataSubject.next(message);
    }
  }

  public onAlarmData(message: AlarmDataUpdate) {
    if (this.tsOffset) {
      message.prepareData(this.tsOffset);
    }
    if (this.zone) {
      this.zone.run(
        () => {
          this.alarmDataSubject.next(message);
        }
      );
    } else {
      this.alarmDataSubject.next(message);
    }
  }

  public onEntityCount(message: EntityCountUpdate) {
    if (this.zone) {
      this.zone.run(
        () => {
          this.entityCountSubject.next(message);
        }
      );
    } else {
      this.entityCountSubject.next(message);
    }
  }

  public onReconnected() {
    this.reconnectSubject.next();
  }

  public attributeData$(): Observable<Array<AttributeData>> {
    const attributeData = new Array<AttributeData>();
    return this.data$.pipe(
      map((message) => message.updateAttributeData(attributeData))
    );
  }
}
