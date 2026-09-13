// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, defaultHttpOptionsFromParams, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageData } from '@shared/models/page/page-data';
import {
  CalculatedFieldAlarmRule,
  CalculatedFieldAlarmRuleInfo,
  CalculatedFieldsQuery,
  CalculatedFieldTestScriptInputParams
} from '@shared/models/calculated-field.models';
import { PageLink } from '@shared/models/page/page-link';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityTestScriptResult } from '@shared/models/entity.models';
import { CalculatedFieldEventBody } from '@shared/models/event.models';

@Injectable({
  providedIn: 'root'
})
export class AlarmRulesService {

  constructor(
    private http: HttpClient
  ) { }

  public getAlarmRuleById(alarmRuleId: string, config?: RequestConfig): Observable<CalculatedFieldAlarmRule> {
    return this.http.get<CalculatedFieldAlarmRule>(`/api/alarm/rule/${alarmRuleId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveAlarmRule(alarmRule: CalculatedFieldAlarmRule, config?: RequestConfig): Observable<CalculatedFieldAlarmRule> {
    return this.http.post<CalculatedFieldAlarmRule>('/api/alarm/rule', alarmRule, defaultHttpOptionsFromConfig(config));
  }

  public deleteAlarmRule(alarmRuleId: string, config?: RequestConfig): Observable<boolean> {
    return this.http.delete<boolean>(`/api/alarm/rule/${alarmRuleId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAlarmRules(pageLink: PageLink, query: CalculatedFieldsQuery, config?: RequestConfig): Observable<PageData<CalculatedFieldAlarmRuleInfo>> {
    return this.http.get<PageData<CalculatedFieldAlarmRuleInfo>>(`/api/alarm/rules${pageLink.toQuery()}`, defaultHttpOptionsFromParams(query, config));
  }

  public getAlarmRulesByEntityId({ entityType, id }: EntityId, pageLink: PageLink, config?: RequestConfig): Observable<PageData<CalculatedFieldAlarmRule>> {
    return this.http.get<PageData<CalculatedFieldAlarmRule>>(`/api/alarm/rules/${entityType}/${id}${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public testScript(inputParams: CalculatedFieldTestScriptInputParams, config?: RequestConfig): Observable<EntityTestScriptResult> {
    return this.http.post<EntityTestScriptResult>('/api/alarm/rule/testScript', inputParams, defaultHttpOptionsFromConfig(config));
  }

  public getLatestAlarmRuleDebugEvent(id: string, config?: RequestConfig): Observable<CalculatedFieldEventBody> {
    return this.http.get<CalculatedFieldEventBody>(`/api/alarm/rule/${id}/debug`, defaultHttpOptionsFromConfig(config));
  }

  public getAlarmRuleNames(pageLink: PageLink, config?: RequestConfig): Observable<PageData<string>> {
    return this.http.get<PageData<string>>(`/api/alarm/rules/names${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }
}
