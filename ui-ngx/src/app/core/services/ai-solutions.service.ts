// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, Injectable, signal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { SolutionsCreatorService } from '@core/http/solutions-creator.service';
import { DialogService } from '@core/services/dialog.service';
import { SolutionCreatorInfo, SolutionInfo } from '@shared/models/solution-creator.models';

@Injectable({providedIn: 'root'})
export class AiSolutionsService {

  private _solutions = signal<SolutionInfo[]>([]);
  readonly solutions = this._solutions.asReadonly();

  private _loaded = signal(false);
  readonly loaded = this._loaded.asReadonly();

  private _running = signal<ReadonlySet<string>>(new Set());
  readonly running = this._running.asReadonly();

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
