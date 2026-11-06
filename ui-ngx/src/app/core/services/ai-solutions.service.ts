// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, Injectable, signal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { ChatLevel } from '@shared/models/ai-chat.models';
import { SolutionsCreatorService } from '@core/http/solutions-creator.service';
import { DialogService } from '@core/services/dialog.service';
import {
  SolutionCreatorInfo,
  SolutionInfo,
  SolutionInstallProgress,
  SolutionStep,
  SolutionUpdateRequest
} from '@shared/models/solution-creator.models';

@Injectable({providedIn: 'root'})
export class AiSolutionsService {

  private _solutions = signal<SolutionInfo[]>([]);
  readonly solutions = this._solutions.asReadonly();

  private _loaded = signal(false);
  readonly loaded = this._loaded.asReadonly();

  private _running = signal<ReadonlySet<string>>(new Set());
  readonly running = this._running.asReadonly();
  private _failed = signal<ReadonlyMap<string, string>>(new Map());

  private api = inject(SolutionsCreatorService);
  private dialog = inject(DialogService);
  private translate = inject(TranslateService);

  load(): Observable<SolutionInfo[]> {
    return this.api.getSolutions({ignoreLoading: true, ignoreErrors: true}).pipe(
      tap({
        next: solutions => {
          this._solutions.set(solutions);
          this._loaded.set(true);
        },
        error: () => this._loaded.set(true)
      })
    );
  }

  setRunning(solutionId: string | undefined, running: boolean): void {
    if (!solutionId || this._running().has(solutionId) === running) {
      return;
    }
    if (running) {
      this.update(solutionId, {status: undefined, statusReason: undefined});
    }
    this._running.update(set => {
      const next = new Set(set);
      if (running) {
        next.add(solutionId);
      } else {
        next.delete(solutionId);
      }
      return next;
    });
  }

  isRunning(solutionId: string): boolean {
    return this._running().has(solutionId);
  }

  setFailed(solutionId: string | undefined, reason: string | null): void {
    if (!solutionId || this._failed().get(solutionId) === (reason ?? undefined)) {
      return;
    }
    this._failed.update(map => {
      const next = new Map(map);
      if (reason) {
        next.set(solutionId, reason);
      } else {
        next.delete(solutionId);
      }
      return next;
    });
  }

  levelOf(solution: SolutionInfo, open = false): ChatLevel | null {
    if (this._running().has(solution.id)) {
      return 'running';
    }
    if (this._failed().has(solution.id)) {
      return 'error';
    }
    if (open) {
      return solution.status === 'FAILED' ? 'error' : null;
    }
    return solution.status === 'NEEDS_YOU' ? 'needs' : solution.status === 'RUNNING' ? 'running' : solution.status === 'FAILED' ? 'error' : null;
  }

  tipOf(solution: SolutionInfo, open = false): string {
    const level = this.levelOf(solution, open);
    if (!level) {
      return '';
    }
    const reason = level === 'error' ? this._failed().get(solution.id) ?? solution.statusReason : undefined;
    return reason || this.translate.instant('ai-assistant.status-tip-' + level);
  }

  find(solutionId: string | undefined): SolutionInfo | undefined {
    return solutionId ? this._solutions().find(solution => solution.id === solutionId) : undefined;
  }

  titleOf(solution: SolutionInfo): string {
    return solution.solutionTitle || this.translate.instant('solution-creator.new-solution');
  }

  upsert(info: SolutionCreatorInfo): void {
    const solution: SolutionInfo = {
      id: info.id,
      createdTime: info.createdTime,
      solutionTitle: info.data?.solutionTitle ?? '',
      built: !!info.metadata?.built,
      installed: !!info.metadata?.installed
    };
    this._solutions.update(list => list.some(item => item.id === solution.id)
      ? list.map(item => item.id === solution.id ? {...item, ...solution, createdTime: item.createdTime ?? solution.createdTime} : item)
      : [solution, ...list]);
  }

  update(solutionId: string, fields: Partial<SolutionInfo>): void {
    this._solutions.update(list => list.map(item => item.id === solutionId ? {...item, ...fields} : item));
  }

  rename(solution: SolutionInfo, title: string): Observable<SolutionCreatorInfo> {
    return this.api.updateSolutionData(solution.id, 'solutionTitle', JSON.stringify(title), {ignoreLoading: true, ignoreErrors: true}).pipe(
      tap(() => this.update(solution.id, {solutionTitle: title}))
    );
  }

  setPinned(solution: SolutionInfo, pinned: boolean): Observable<void> {
    return this.api.updateSolution(solution.id, {pinned}, {ignoreLoading: true, ignoreErrors: true}).pipe(
      tap(() => this.update(solution.id, {pinned}))
    );
  }

  sendFeedback(solutionId: string, step: SolutionStep, feedback: SolutionUpdateRequest['feedback']): Observable<void> {
    return this.api.updateSolution(solutionId, {step, feedback}, {ignoreLoading: true});
  }

  installProgress(solutionId: string): Observable<SolutionInstallProgress> {
    return this.api.getInstallProgress(solutionId, {ignoreLoading: true, ignoreErrors: true});
  }

  confirmAndUninstall(solution: SolutionInfo): Observable<SolutionCreatorInfo | null> {
    return this.dialog.confirm(
      this.translate.instant('solution-creator.uninstall-solution-dialog-title', {name: this.titleOf(solution)}),
      this.translate.instant('solution-creator.uninstall-solution-dialog-text'),
      this.translate.instant('action.no'),
      this.translate.instant('solution-creator.uninstall-solution-dialog-confirm')
    ).pipe(
      switchMap(confirmed => confirmed
        ? this.api.uninstallSolution(solution.id, {ignoreErrors: true}).pipe(
          tap(() => this.update(solution.id, {installed: false}))
        )
        : of(null))
    );
  }

  confirmAndDelete(solution: SolutionInfo): Observable<boolean> {
    return this.dialog.confirm(
      this.translate.instant('solution-creator.delete-solution-title', {name: this.titleOf(solution)}),
      this.translate.instant('solution-creator.delete-solution-text'),
      this.translate.instant('action.no'),
      this.translate.instant('solution-creator.delete-solution-confirm')
    ).pipe(
      switchMap(confirmed => confirmed
        ? this.api.deleteSolution(solution.id, {ignoreErrors: true}).pipe(
          tap(() => this._solutions.update(list => list.filter(item => item.id !== solution.id))),
          map(() => true)
        )
        : of(false))
    );
  }
}
