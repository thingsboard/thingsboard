///
/// Copyright © 2016-2022 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { EntityId } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { getAceDiff } from '@shared/models/ace/ace.models';

@Component({
  selector: 'tb-entity-version-diff',
  templateUrl: './entity-version-diff.component.html',
  styleUrls: ['./entity-version-diff.component.scss']
})
export class EntityVersionDiffComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('diffViewer', {static: true})
  diffViewerElmRef: ElementRef<HTMLElement>;

  @Input()
  branch: string;

  @Input()
  versionName: string;

  @Input()
  versionId: string;

  @Input()
  externalEntityId: EntityId;

  @Input()
  onClose: () => void;

  differ: AceDiff;

  constructor(protected store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    getAceDiff().subscribe((aceDiff) => {
      this.differ = new aceDiff.default(
        {
          element: this.diffViewerElmRef.nativeElement,
          left: {
            copyLinkEnabled: false,
            editable: false,
            content: 'Left content!'
          },
          right: {
            copyLinkEnabled: false,
            editable: false,
            content: 'Right content!'
          }
        } as AceDiff.AceDiffConstructorOpts
      );
    });
  }

  ngOnDestroy(): void {
    if (this.differ) {
      this.differ.destroy();
    }
  }

  close(): void {
    if (this.onClose) {
      this.onClose();
    }
  }
}
