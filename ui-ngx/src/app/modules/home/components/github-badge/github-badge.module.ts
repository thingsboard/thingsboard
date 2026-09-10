// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { GithubBadgeComponent } from '@home/components/github-badge/github-badge.component';

@NgModule({
  declarations:
    [
      GithubBadgeComponent
    ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    GithubBadgeComponent
  ]
})
export class GithubBadgeModule { }
