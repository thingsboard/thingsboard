// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { isDefinedAndNotNull } from '@core/utils';
import { EntityId } from '@shared/models/id/entity-id';

@Injectable({
  providedIn: 'root'
})
export class RuleEngineService {
  constructor(
    private http: HttpClient
  ) { }

  public makeRequestToRuleEngine(requestBody: { [key: string]: any },
                                 config?: RequestConfig) {
    return this.http.post(`/api/rule-engine/`, requestBody, defaultHttpOptionsFromConfig(config));
  }

  public makeRequestToRuleEngineFromEntity(entityId: EntityId, requestBody: { [key: string]: any },
                                           timeout?: number, config?: RequestConfig) {
    let url = `/api/rule-engine/${entityId.entityType}/${entityId.id}`;
    if (isDefinedAndNotNull(timeout)) {
      url += `/${timeout}`;
    }
    return this.http.post(url, requestBody, defaultHttpOptionsFromConfig(config));
  }

}
