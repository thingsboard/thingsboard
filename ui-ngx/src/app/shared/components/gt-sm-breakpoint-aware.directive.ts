// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Directive, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';

@Directive()
export abstract class GtSmBreakpointAwareDirective {

  protected breakpointObserver: BreakpointObserver = inject(BreakpointObserver);

  isGtSm = true;

  protected constructor() {
    this.breakpointObserver.observe(MediaBreakpoints['gt-sm']).pipe(
      takeUntilDestroyed()
    ).subscribe(state => {
      this.isGtSm = state.matches;
    });
  }

}
