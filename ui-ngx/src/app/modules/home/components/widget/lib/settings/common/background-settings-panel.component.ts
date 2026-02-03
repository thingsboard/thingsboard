///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import {
  backgroundStyle,
  overlayStyle,
  BackgroundSettings,
  BackgroundType,
  backgroundTypeTranslations, ComponentStyle
} from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-background-settings-panel',
    templateUrl: './background-settings-panel.component.html',
    providers: [],
    styleUrls: ['./background-settings-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class BackgroundSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  backgroundSettings: BackgroundSettings;

  @Input()
  popover: TbPopoverComponent<BackgroundSettingsPanelComponent>;

  @Output()
  backgroundSettingsApplied = new EventEmitter<BackgroundSettings>();

  backgroundType = BackgroundType;

  backgroundTypes = Object.keys(BackgroundType) as BackgroundType[];

  backgroundTypeTranslationsMap = backgroundTypeTranslations;

  backgroundSettingsFormGroup: UntypedFormGroup;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};

  constructor(private fb: UntypedFormBuilder,
              private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.backgroundSettingsFormGroup = this.fb.group(
      {
        type: [this.backgroundSettings?.type, []],
        imageUrl: [this.backgroundSettings?.imageUrl, []],
        color: [this.backgroundSettings?.color, []],
        overlay: this.fb.group({
          enabled: [this.backgroundSettings?.overlay?.enabled, []],
          color: [this.backgroundSettings?.overlay?.color, []],
          blur: [this.backgroundSettings?.overlay?.blur, []]
        })
      }
    );
    this.backgroundSettingsFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      setTimeout(() => {this.popover?.updatePosition();}, 0);
    });
    this.backgroundSettingsFormGroup.get('overlay').get('enabled').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
    this.backgroundSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateBackgroundStyle();
    });
    this.updateValidators();
    this.updateBackgroundStyle();
  }

  cancel() {
    this.popover?.hide();
  }

  applyColorSettings() {
    const backgroundSettings = this.backgroundSettingsFormGroup.getRawValue();
    this.backgroundSettingsApplied.emit(backgroundSettings);
  }

  private updateValidators() {
    const overlayEnabled: boolean = this.backgroundSettingsFormGroup.get('overlay').get('enabled').value;
    if (overlayEnabled) {
      this.backgroundSettingsFormGroup.get('overlay').get('color').enable({emitEvent: false});
      this.backgroundSettingsFormGroup.get('overlay').get('blur').enable({emitEvent: false});
    } else {
      this.backgroundSettingsFormGroup.get('overlay').get('color').disable({emitEvent: false});
      this.backgroundSettingsFormGroup.get('overlay').get('blur').disable({emitEvent: false});
    }
    this.backgroundSettingsFormGroup.get('overlay').get('color').updateValueAndValidity({emitEvent: false});
    this.backgroundSettingsFormGroup.get('overlay').get('blur').updateValueAndValidity({emitEvent: false});
  }

  private updateBackgroundStyle() {
    const background: BackgroundSettings = this.backgroundSettingsFormGroup.value;
    this.backgroundStyle$ = backgroundStyle(background, this.imagePipe, this.sanitizer, true);
    this.overlayStyle = overlayStyle(background.overlay);
    this.cd.markForCheck();
  }

}
