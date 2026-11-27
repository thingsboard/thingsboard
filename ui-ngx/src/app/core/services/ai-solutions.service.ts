// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { EMPTY, Observable, of, Subscription, timer } from 'rxjs';
import { catchError, distinctUntilChanged, filter, finalize, map, skip, switchMap, takeWhile, tap } from 'rxjs/operators';
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

const SOLUTIONS_POLL_MS = 5000;

@Injectable({providedIn: 'root'})
export class AiSolutionsService {

  private following: Subscription | null = null;

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

  private generation = 0;

  private userWatch = inject(Store<AppState>).select(selectAuthUser).pipe(
    map(user => user?.userId ?? null),
    distinctUntilChanged(),
    skip(1),
    takeUntilDestroyed(inject(DestroyRef))
  ).subscribe(() => this.reset());

  private reset(): void {
    this.generation++;
    this.ensuring = false;
    this.following?.unsubscribe();
    this.following = null;
    this._solutions.set([]);
    this._loaded.set(false);
    this._running.set(new Set());
    this._failed.set(new Map());
  }

  private ensuring = false;

  ensureLoaded(): void {
    if (this._loaded() || this.ensuring) {
      return;
    }
    this.ensuring = true;
    const generation = this.generation;
    const done = () => {
      if (generation === this.generation) {
        this.ensuring = false;
      }
    };
    this.load().subscribe({error: done, complete: done});
  }

  load(): Observable<SolutionInfo[]> {
    const generation = this.generation;
    return this.api.getSolutions({ignoreLoading: true, ignoreErrors: true}).pipe(
      filter(() => generation === this.generation),
      tap({
        next: solutions => {
          this._solutions.set(solutions);
          this._loaded.set(true);
          this.followRunning();
        },
        error: () => this._loaded.set(true)
      })
    );
  }

  keepRunning(solutionId: string | undefined): void {
    if (!solutionId) {
      return;
    }
    this.update(solutionId, {status: 'RUNNING', statusReason: undefined});
    this.followRunning();
  }

  followRunning(): void {
    if (this.following) {
      return;
    }
    this.following = timer(SOLUTIONS_POLL_MS, SOLUTIONS_POLL_MS).pipe(
      takeWhile(() => this._solutions().some(solution => solution.status === 'RUNNING' && !this._running().has(solution.id))),
      switchMap(() => this.load().pipe(catchError(() => EMPTY))),
      finalize(() => this.following = null)
    ).subscribe();
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
      solutionTitle: info.data?.solutionTitle || info.data?.aiSolutionTitle || this.find(info.id)?.solutionTitle || '',
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

  discard(solutionId: string): Observable<void> {
    return this.api.deleteSolution(solutionId, {ignoreLoading: true, ignoreErrors: true}).pipe(
      tap(() => this._solutions.update(list => list.filter(item => item.id !== solutionId)))
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
