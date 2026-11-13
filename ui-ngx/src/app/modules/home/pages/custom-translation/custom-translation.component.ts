// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { ContentType } from '@shared/models/constants';
import { Operation, Resource } from '@shared/models/security.models';
import { ActivatedRoute, Router } from '@angular/router';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { FormBuilder } from '@angular/forms';

@Component({
    selector: 'tb-custom-translation',
    templateUrl: './custom-translation.component.html',
    styleUrls: ['../admin/settings-card.scss', './custom-translation.component.scss'],
    standalone: false
})
export class CustomTranslationComponent {

  isDirty = false;

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);

  contentType = ContentType;
  mode = this.fb.control('basic');

  translation: object;

  tableFullScreen = false;
  editorFullScreen = false;

  localeCode: string;
  localeName: string;
  countryName: string;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private userPermissionsService: UserPermissionsService,
              private fb: FormBuilder) {
    this.localeCode = this.route.snapshot.paramMap.get('localeCode');
    this.localeName = decodeURIComponent(this.route.snapshot.queryParamMap.get('name'));
    this.countryName = decodeURIComponent(this.route.snapshot.queryParamMap.get('country'));
  }

  goBack() {
    this.router.navigate(['../'], { relativeTo: this.route });
  }

  changeTableFullScreen($event: boolean) {
    this.tableFullScreen = $event;
  }

  changeditorFullScreen($event: boolean) {
    this.editorFullScreen = $event;
  }
}
