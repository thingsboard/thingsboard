// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectHasRepository } from '@core/auth/auth.selectors';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { RepositorySettingsComponent } from '@home/components/vc/repository-settings.component';
import { UntypedFormGroup } from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { Observable } from 'rxjs';
import { TbPopoverComponent } from '@shared/components/popover.component';

@Component({
    selector: 'tb-version-control',
    templateUrl: './version-control.component.html',
    styleUrls: ['./version-control.component.scss'],
    standalone: false
})
export class VersionControlComponent implements OnInit, HasConfirmForm {

  @ViewChild('repositorySettingsComponent', {static: false}) repositorySettingsComponent: RepositorySettingsComponent;

  @Input()
  detailsMode = false;

  @Input()
  popoverComponent: TbPopoverComponent;

  @Input()
  active = true;

  @Input()
  singleEntityMode = false;

  @Input()
  externalEntityId: EntityId;

  @Input()
  entityId: EntityId;

  @Input()
  entityName: string;

  @Input()
  onBeforeCreateVersion: () => Observable<any>;

  @Output()
  versionRestored = new EventEmitter<void>();

  hasRepository$ = this.store.pipe(select(selectHasRepository));

  constructor(private store: Store<AppState>) {

  }

  ngOnInit() {

  }

  confirmForm(): UntypedFormGroup {
    return this.repositorySettingsComponent?.repositorySettingsForm;
  }

}
