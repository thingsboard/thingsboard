// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import {
  SolutionCreatorInfo,
  SolutionDataKey,
  SolutionDataValues,
  SolutionInfo,
  SolutionInstallProgress,
  SolutionInstallResult,
  SolutionStep,
  SolutionUpdateRequest
} from '@shared/models/solution-creator.models';

@Injectable({
  providedIn: 'root'
})
export class SolutionsCreatorService {

  constructor(
    private http: HttpClient,
  ) {
  }

  public getSolutionById(solutionId: string, config?: RequestConfig): Observable<SolutionCreatorInfo> {
    return this.http.get<SolutionCreatorInfo>(`/api/ai/solution/${solutionId}`, defaultHttpOptionsFromConfig(config));
  }

  public startSolution(config?: RequestConfig): Observable<SolutionCreatorInfo> {
    return this.http.post<SolutionCreatorInfo>(`/api/ai/solution/start`, {}, defaultHttpOptionsFromConfig(config));
  }

  public clearStep(solutionId: string, step: SolutionStep, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/ai/solution/${solutionId}/${step}/clear`, defaultHttpOptionsFromConfig(config));
  }

  public stopSolution(solutionId: string, config?: RequestConfig): Observable<boolean> {
    return this.http.post<boolean>(`/api/ai/solution/${solutionId}/stop`, null, defaultHttpOptionsFromConfig(config));
  }

  public createSolution(solutionId: string, config?: RequestConfig): Observable<SolutionCreatorInfo> {
    return this.http.post<SolutionCreatorInfo>(`/api/ai/solution/${solutionId}/create`, '', defaultHttpOptionsFromConfig(config));
  }

  public chatSolution(solutionId: string, step: SolutionStep, msg: string, config?: RequestConfig,
                      resources: string[] = []): Observable<SolutionCreatorInfo> {
    return this.http.post<SolutionCreatorInfo>(`/api/ai/solution/${solutionId}/${step}/messages`,
      {message: msg, resources}, defaultHttpOptionsFromConfig(config));
  }

  public updateSolutionData(solutionId: string, dataKey: SolutionDataKey, msg: SolutionDataValues, config?: RequestConfig): Observable<SolutionCreatorInfo> {
    return this.http.put<SolutionCreatorInfo>(`/api/ai/solution/${solutionId}/${dataKey}`, msg, defaultHttpOptionsFromConfig(config));
  }

  public installSolution(solutionId: string, config?: RequestConfig): Observable<SolutionInstallResult> {
    return this.http.post<SolutionInstallResult>(`/api/ai/solution/${solutionId}/install`, '',  defaultHttpOptionsFromConfig(config));
  }

  public uninstallSolution(solutionId: string, config?: RequestConfig): Observable<SolutionCreatorInfo> {
    return this.http.delete<SolutionCreatorInfo>(`/api/ai/solution/${solutionId}/uninstall`, defaultHttpOptionsFromConfig(config));
  }

  public getSolutions(config?: RequestConfig): Observable<Array<SolutionInfo>> {
    return this.http.get<Array<SolutionInfo>>(`/api/ai/solution/infos`,  defaultHttpOptionsFromConfig(config))
  }

  public deleteSolution(solutionId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/ai/solution/${solutionId}`, defaultHttpOptionsFromConfig(config));
  }

  public updateSolution(solutionId: string, request: SolutionUpdateRequest, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/ai/solution/${solutionId}/update`, request, defaultHttpOptionsFromConfig(config));
  }

  public getInstallProgress(solutionId: string, config?: RequestConfig): Observable<SolutionInstallProgress> {
    return this.http.get<SolutionInstallProgress>(`/api/ai/solution/${solutionId}/progress`, defaultHttpOptionsFromConfig(config));
  }
}
