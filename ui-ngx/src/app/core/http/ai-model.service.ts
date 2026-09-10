// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { AiModel, AiModelWithUserMsg, CheckConnectivityResult } from '@shared/models/ai-model.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';

@Injectable({
  providedIn: 'root'
})
export class AiModelService {

  constructor(
    private http: HttpClient
  ) {}

  public saveAiModel(aiModel: AiModel, config?: RequestConfig): Observable<AiModel> {
    return this.http.post<AiModel>('/api/ai/model', aiModel, defaultHttpOptionsFromConfig(config));
  }

  public getAiModels(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AiModel>> {
    return this.http.get<PageData<AiModel>>(`/api/ai/model${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getAiModelById(aiModelId: string, config?: RequestConfig): Observable<AiModel> {
    return this.http.get<AiModel>(`/api/ai/model/${aiModelId}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteAiModel(aiModelId: string, config?: RequestConfig) {
    return this.http.delete(`/api/ai/model/${aiModelId}`, defaultHttpOptionsFromConfig(config));
  }

  public checkConnectivity(aiModelWithUserMsg: AiModelWithUserMsg, config?: RequestConfig): Observable<CheckConnectivityResult> {
    return this.http.post<CheckConnectivityResult>('/api/ai/model/chat', aiModelWithUserMsg, defaultHttpOptionsFromConfig(config));
  }

}
