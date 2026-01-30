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

import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { LabelWidgetLabel } from '@home/components/widget/lib/settings/cards/label-widget-label.component';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

@Component({
    selector: 'tb-label-widget-settings',
    templateUrl: './label-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class LabelWidgetSettingsComponent extends WidgetSettingsComponent {

  labelWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.labelWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      backgroundImageUrl: 'data:image/svg+xml;base64,PHN2ZyBpZD0ic3ZnMiIgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGhlaWdodD0iMTAwIiB3aWR0aD0iMTAwIiB2ZXJzaW9uPSIxLjEiIHhtbG5zOmNjPSJodHRwOi8vY3JlYXRpdmVjb21tb25zLm9yZy9ucyMiIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgdmlld0JveD0iMCAwIDEwMCAxMDAiPgogPGcgaWQ9ImxheWVyMSIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMCAtOTUyLjM2KSI+CiAgPHJlY3QgaWQ9InJlY3Q0Njg0IiBzdHJva2UtbGluZWpvaW49InJvdW5kIiBoZWlnaHQ9Ijk5LjAxIiB3aWR0aD0iOTkuMDEiIHN0cm9rZT0iIzAwMCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiB5PSI5NTIuODYiIHg9Ii40OTUwNSIgc3Ryb2tlLXdpZHRoPSIuOTkwMTAiIGZpbGw9IiNlZWUiLz4KICA8dGV4dCBpZD0idGV4dDQ2ODYiIHN0eWxlPSJ3b3JkLXNwYWNpbmc6MHB4O2xldHRlci1zcGFjaW5nOjBweDt0ZXh0LWFuY2hvcjptaWRkbGU7dGV4dC1hbGlnbjpjZW50ZXIiIGZvbnQtd2VpZ2h0PSJib2xkIiB4bWw6c3BhY2U9InByZXNlcnZlIiBmb250LXNpemU9IjEwcHgiIGxpbmUtaGVpZ2h0PSIxMjUlIiB5PSI5NzAuNzI4MDkiIHg9IjQ5LjM5NjQ3NyIgZm9udC1mYW1pbHk9IlJvYm90byIgZmlsbD0iIzY2NjY2NiI+PHRzcGFuIGlkPSJ0c3BhbjQ2OTAiIHg9IjUwLjY0NjQ3NyIgeT0iOTcwLjcyODA5Ij5JbWFnZSBiYWNrZ3JvdW5kIDwvdHNwYW4+PHRzcGFuIGlkPSJ0c3BhbjQ2OTIiIHg9IjQ5LjM5NjQ3NyIgeT0iOTgzLjIyODA5Ij5pcyBub3QgY29uZmlndXJlZDwvdHNwYW4+PC90ZXh0PgogIDxyZWN0IGlkPSJyZWN0NDY5NCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIgaGVpZ2h0PSIxOS4zNiIgd2lkdGg9IjY5LjM2IiBzdHJva2U9IiMwMDAiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgeT0iOTkyLjY4IiB4PSIxNS4zMiIgc3Ryb2tlLXdpZHRoPSIuNjM5ODYiIGZpbGw9Im5vbmUiLz4KIDwvZz4KPC9zdmc+Cg==',
      labels: []
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.labelWidgetSettingsForm = this.fb.group({
      backgroundImageUrl: [settings.backgroundImageUrl, [Validators.required]],
      labels: this.prepareLabelsFormArray(settings.labels)
    });
  }

  protected doUpdateSettings(settingsForm: UntypedFormGroup, settings: WidgetSettings) {
    settingsForm.setControl('labels', this.prepareLabelsFormArray(settings.labels), {emitEvent: false});
  }

  private prepareLabelsFormArray(labels: LabelWidgetLabel[] | undefined): UntypedFormArray {
    const labelsControls: Array<AbstractControl> = [];
    if (labels) {
      labels.forEach((label) => {
        labelsControls.push(this.fb.control(label, [Validators.required]));
      });
    }
    return this.fb.array(labelsControls);
  }

  labelsFormArray(): UntypedFormArray {
    return this.labelWidgetSettingsForm.get('labels') as UntypedFormArray;
  }

  public trackByLabelControl(index: number, labelControl: AbstractControl): any {
    return labelControl;
  }

  public removeLabel(index: number) {
    (this.labelWidgetSettingsForm.get('labels') as UntypedFormArray).removeAt(index);
  }

  public addLabel() {
    const label: LabelWidgetLabel = {
      pattern: '${#0}',
      x: 50,
      y: 50,
      backgroundColor: 'rgba(0,0,0,0)',
      font: {
        family: 'Roboto',
        size: 6,
        style: 'normal',
        weight: '500',
        color: '#fff'
      }
    };
    const labelsArray = this.labelWidgetSettingsForm.get('labels') as UntypedFormArray;
    const labelControl = this.fb.control(label, [Validators.required]);
    (labelControl as any).new = true;
    labelsArray.push(labelControl);
    this.labelWidgetSettingsForm.updateValueAndValidity();
  }

  labelDrop(event: CdkDragDrop<string[]>) {
    const labelsArray = this.labelWidgetSettingsForm.get('labels') as UntypedFormArray;
    const label = labelsArray.at(event.previousIndex);
    labelsArray.removeAt(event.previousIndex);
    labelsArray.insert(event.currentIndex, label);
  }
}
