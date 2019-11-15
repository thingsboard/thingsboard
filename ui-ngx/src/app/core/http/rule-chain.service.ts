///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import {Injectable} from '@angular/core';
import { defaultHttpOptions, defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import {Observable} from 'rxjs/index';
import {HttpClient} from '@angular/common/http';
import {PageLink} from '@shared/models/page/page-link';
import {PageData} from '@shared/models/page/page-data';
import {RuleChain} from '@shared/models/rule-chain.models';

@Injectable({
  providedIn: 'root'
})
export class RuleChainService {

  constructor(
    private http: HttpClient
  ) { }

  public getRuleChains(pageLink: PageLink, config?: RequestConfig): Observable<PageData<RuleChain>> {
    return this.http.get<PageData<RuleChain>>(`/api/ruleChains${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.get<RuleChain>(`/api/ruleChain/${ruleChainId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveRuleChain(ruleChain: RuleChain, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>('/api/ruleChain', ruleChain, defaultHttpOptionsFromConfig(config));
  }

  public deleteRuleChain(ruleChainId: string, config?: RequestConfig) {
    return this.http.delete(`/api/ruleChain/${ruleChainId}`, defaultHttpOptionsFromConfig(config));
  }

  public setRootRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>(`/api/ruleChain/${ruleChainId}/root`, null, defaultHttpOptionsFromConfig(config));
  }

}
