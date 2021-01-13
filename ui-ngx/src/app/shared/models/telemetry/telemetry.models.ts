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


import { EntityType } from '@shared/models/entity-type.models';
import { AggregationType } from '../time/time.models';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { map } from 'rxjs/operators';
import { NgZone } from '@angular/core';
import {
  AlarmData,
  AlarmDataQuery, DataEntityKey,
  EntityData,
  EntityDataQuery,
  EntityKey,
  TsValue
} from '@shared/models/query/query.models';
import { PageData } from '@shared/models/page/page-data';

export enum DataKeyType {
  timeseries = 'timeseries',
  attribute = 'attribute',
  function = 'function',
  alarm = 'alarm',
  entityField = 'entityField'
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
  keys: Array<DataEntityKey>;
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

  constructor() {
    this.attrSubCmds = [];
    this.tsSubCmds = [];
    this.historyCmds = [];
    this.entityDataCmds = [];
    this.entityDataUnsubscribeCmds = [];
    this.alarmDataCmds = [];
    this.alarmDataUnsubscribeCmds = [];
  }

  public hasCommands(): boolean {
    return this.tsSubCmds.length > 0 ||
      this.historyCmds.length > 0 ||
      this.attrSubCmds.length > 0 ||
      this.entityDataCmds.length > 0 ||
      this.entityDataUnsubscribeCmds.length > 0 ||
      this.alarmDataCmds.length > 0 ||
      this.alarmDataUnsubscribeCmds.length > 0;
  }

  public clear() {
    this.attrSubCmds.length = 0;
    this.tsSubCmds.length = 0;
    this.historyCmds.length = 0;
    this.entityDataCmds.length = 0;
    this.entityDataUnsubscribeCmds.length = 0;
    this.alarmDataCmds.length = 0;
    this.alarmDataUnsubscribeCmds.length = 0;
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

export enum DataUpdateType {
  ENTITY_DATA = 'ENTITY_DATA',
  ALARM_DATA = 'ALARM_DATA'
}

export interface DataUpdateMsg<T> {
  cmdId: number;
  data?: PageData<T>;
  update?: Array<T>;
  errorCode: number;
  errorMsg: string;
  dataUpdateType: DataUpdateType;
}

export interface EntityDataUpdateMsg extends DataUpdateMsg<EntityData> {
  dataUpdateType: DataUpdateType.ENTITY_DATA;
}

export interface AlarmDataUpdateMsg extends DataUpdateMsg<AlarmData> {
  dataUpdateType: DataUpdateType.ALARM_DATA;
  allowedEntities: number;
  totalEntities: number;
}

export type WebsocketDataMsg = AlarmDataUpdateMsg | EntityDataUpdateMsg | SubscriptionUpdateMsg;

export function isEntityDataUpdateMsg(message: WebsocketDataMsg): message is EntityDataUpdateMsg {
  const updateMsg = (message as DataUpdateMsg<any>);
  return updateMsg.cmdId !== undefined && updateMsg.dataUpdateType === DataUpdateType.ENTITY_DATA;
}

export function isAlarmDataUpdateMsg(message: WebsocketDataMsg): message is AlarmDataUpdateMsg {
  const updateMsg = (message as DataUpdateMsg<any>);
  return updateMsg.cmdId !== undefined && updateMsg.dataUpdateType === DataUpdateType.ALARM_DATA;
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

export class DataUpdate<T> implements DataUpdateMsg<T> {
  cmdId: number;
  errorCode: number;
  errorMsg: string;
  data?: PageData<T>;
  update?: Array<T>;
  dataUpdateType: DataUpdateType;

  constructor(msg: DataUpdateMsg<T>) {
    this.cmdId = msg.cmdId;
    this.errorCode = msg.errorCode;
    this.errorMsg = msg.errorMsg;
    this.data = msg.data;
    this.update = msg.update;
    this.dataUpdateType = msg.dataUpdateType;
  }
}

export class EntityDataUpdate extends DataUpdate<EntityData> {
  constructor(msg: EntityDataUpdateMsg) {
    super(msg);
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
  private reconnectSubject = new Subject();

  private zone: NgZone;

  public subscriptionCommands: Array<WebsocketCmd>;

  public data$ = this.dataSubject.asObservable();
  public entityData$ = this.entityDataSubject.asObservable();
  public alarmData$ = this.alarmDataSubject.asObservable();
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
    this.reconnectSubject.complete();
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
