// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, OnDestroy } from '@angular/core';
import { GitHubService } from '@core/http/git-hub.service';
import { Store } from '@ngrx/store';
import { selectAuthUser, selectIsAuthenticated } from '@core/auth/auth.selectors';
import { distinctUntilChanged, filter, map, switchMap, take, takeUntil } from 'rxjs/operators';
import { Authority } from '@shared/models/authority.enum';
import { AppState } from '@core/core.state';
import { LocalStorageService } from '@core/local-storage/local-storage.service';
import { Subject } from 'rxjs';

const SETTINGS_KEY = 'HIDE_GITHUB_STAR_BUTTON';

@Component({
    selector: 'tb-github-badge',
    templateUrl: './github-badge.component.html',
    styleUrl: './github-badge.component.scss',
    standalone: false
})
export class GithubBadgeComponent implements OnDestroy {

  githubStar = 0;

  private stopWatch$ = new Subject<void>();

  constructor(private gitHubService: GitHubService,
              private localStorageService: LocalStorageService,
              private store: Store<AppState>,) {
    const hide = this.localStorageService.getItem(SETTINGS_KEY) ?? false;

    if (!hide) {
      this.store.select(selectIsAuthenticated).pipe(
        filter((data) => data),
        switchMap(() => this.store.select(selectAuthUser).pipe(take(1))),
        map((authUser) => {
          return [Authority.TENANT_ADMIN, Authority.SYS_ADMIN].includes(authUser?.authority ?? Authority.ANONYMOUS)
        }),
        distinctUntilChanged(),
        takeUntil(this.stopWatch$),
      ).subscribe(value => {
        if (value) {
          this.gitHubService.getGitHubStar().subscribe(star => {
            this.githubStar = star;
          });
        } else {
          this.githubStar = 0
        }
      });
    }
  }

  hideGithubStar($event: Event) {
    $event?.stopPropagation();
    this.localStorageService.setItem(SETTINGS_KEY, true);
    this.githubStar = 0;

    this.stopWatch$.next();
    this.stopWatch$.complete();
  }

  ngOnDestroy() {
    this.stopWatch$.next();
    this.stopWatch$.complete();
  }
}
