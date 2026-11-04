// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, OnInit, Output, signal } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { SolutionsCreatorService } from '@core/http/solutions-creator.service';
import { SolutionCreatorInfo, SolutionInfo } from '@shared/models/solution-creator.models';
import { ActivatedRoute, Router } from '@angular/router';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { AiRenameDialogComponent, AiRenameDialogData } from '@home/components/ai/ai-rename-dialog.component';
import { AppState } from '@core/core.state';
import { select, Store } from '@ngrx/store';
import { selectUserSettingsProperty } from '@core/auth/auth.selectors';
import { distinctUntilChanged, skip, take } from 'rxjs/operators';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { ActionPreferencesPutUserSettings } from '@core/auth/auth.actions';

@Component({
  selector: 'tb-solution-side-bar',
  templateUrl: './solution-side-bar.component.html',
  styleUrls: ['./solution-side-bar.component.scss'],
  standalone: false
})
export class SolutionSideBarComponent extends PageComponent implements OnInit {

  isExpanded = signal(false);
  solutions = signal<Array<SolutionInfo>>([]);

  enableTransition = false;

  @Output()
  updatedSolution = new EventEmitter<SolutionCreatorInfo>();

  @Output()
  installedChanged = new EventEmitter<{solutionId: string, installed: boolean}>();

  constructor(
    private solutionCreator: SolutionsCreatorService,
    private dialog: DialogService,
    private translate: TranslateService,
    private router: Router,
    private route: ActivatedRoute,
    protected store: Store<AppState>,
  ) {
    super();
    this.store.pipe(select(selectUserSettingsProperty('isSolutionSidebarExpanded'))).pipe(
      take(1)
    ).subscribe((settings: boolean) => {
      if (settings) {
        this.isExpanded.set(settings);
      }
    })

    toObservable(this.isExpanded).pipe(
      takeUntilDestroyed(),
      skip(1),
      distinctUntilChanged()
    ).subscribe(value => {
      this.store.dispatch(new ActionPreferencesPutUserSettings({isSolutionSidebarExpanded: value }));
    });
  }

  ngOnInit() {
    this.solutionCreator.getSolutions({ignoreLoading: true}).subscribe((solutions) => {
      this.solutions.set(solutions);
    })
  }

  toggleSidebar($event: Event): void {
    $event?.stopPropagation();
    this.enableTransition = true;
    this.isExpanded.set(!this.isExpanded());
  }

  expandSidebar(): void {
    if (!this.isExpanded()) {
      this.enableTransition = true;
      this.isExpanded.set(true);
    }
  }

  deleteSolution(solution: SolutionInfo): void {
    this.dialog.confirm(
      this.translate.instant('solution-creator.delete-solution-title', {
        name: solution.solutionTitle || this.translate.instant('solution-creator.new-solution')
      }),
      this.translate.instant('solution-creator.delete-solution-text'),
      this.translate.instant('action.no'),
      this.translate.instant('solution-creator.delete-solution-confirm')
    ).subscribe((value) => {
      if (value) {
        this.solutionCreator.deleteSolution(solution.id).subscribe(() => {
          this.solutions.update(solutions => solutions.filter(s => s.id !== solution.id));
          if (this.route.snapshot.queryParamMap.has('solutionId') && this.route.snapshot.queryParamMap.get('solutionId') === solution.id) {
            this.router.navigate(['/ai-solution-creator']).then(() => {});
          }
        })
      }
    })
  }

  addNewSolutionTemplate(solution: SolutionInfo): void {
    const findSolutionIndex = this.solutions().findIndex(s => s.id === solution.id);
    if (findSolutionIndex !== -1) {
      this.updateSolutionField(solution, solution);
    } else {
      this.solutions.update(solutions => [solution, ...solutions]);
    }
  }

  renameSolution(solution: SolutionInfo): void {
    this.dialog.dialog.open<AiRenameDialogComponent, AiRenameDialogData, string>(AiRenameDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        title: this.translate.instant('solution-creator.rename-solution-title'),
        value: solution.solutionTitle || this.translate.instant('solution-creator.new-solution')
      }
    }).afterClosed().subscribe(newTitle => {
      if (newTitle && newTitle !== solution.solutionTitle) {
        this.solutionCreator.updateSolutionData(solution.id, 'solutionTitle', JSON.stringify(newTitle))
          .subscribe(value => {
            solution.solutionTitle = newTitle;
            this.updatedSolution.next(value);
          });
      }
    });
  }

  uninstallSolution(solution: SolutionInfo): void {
    this.dialog.confirm(
      this.translate.instant('solution-creator.uninstall-solution-dialog-title', {name: solution.solutionTitle}),
      this.translate.instant('solution-creator.uninstall-solution-dialog-text'),
      this.translate.instant('action.no'),
      this.translate.instant('solution-creator.uninstall-solution-dialog-confirm')
    ).subscribe((value) => {
      if (value) {
        this.solutionCreator.uninstallSolution(solution.id, {ignoreErrors: true}).subscribe(() => {
          this.updateSolutionField(solution, {installed: false});
          this.installedChanged.emit({solutionId: solution.id, installed: false});
        });
      }
    });
  }

  updateSolutionField(solution: SolutionInfo | {id: string}, fields: Partial<SolutionInfo>): void {
    const defined = Object.fromEntries(Object.entries(fields).filter(([, v]) => v !== undefined));
    this.solutions.update(solutions =>
      solutions.map(s => s.id === solution.id ? {...s, ...defined} : s)
    );
  }
}
