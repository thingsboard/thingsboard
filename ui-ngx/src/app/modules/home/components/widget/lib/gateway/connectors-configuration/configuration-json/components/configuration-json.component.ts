///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  ElementRef,
  forwardRef,
  Injector,
  Input,
} from '@angular/core';
import {
  AsyncValidator,
  ControlValueAccessor,
  NG_ASYNC_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { ConfigurationValidateService } from '../services/configuration-validate.service';
import { JsonObjectEditComponent } from '@shared/components/json-object-edit.component';
import { Store } from '@ngrx/store';
import { RafService } from '@core/services/raf.service';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ConnectorType } from '@home/components/widget/lib/gateway/gateway-form.models';
import { CustomAnnotation } from '../models/configuration-validate.models';

@Component({
  selector: 'tb-configuration-json',
  templateUrl: '../../../../../../../../../shared/components/json-object-edit.component.html',
  styleUrls: ['../../../../../../../../../shared/components/json-object-edit.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ConfigurationJsonComponent),
      multi: true
    },
    {
      provide: NG_ASYNC_VALIDATORS,
      useExisting: forwardRef(() => ConfigurationJsonComponent),
      multi: true,
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ]
})
export class ConfigurationJsonComponent extends JsonObjectEditComponent implements ControlValueAccessor, AsyncValidator {
  @Input() connectorType: ConnectorType;
  @Input() deviceId: string;

  private annotations: CustomAnnotation[] = [];
  private annotationChanged = false;

  constructor(
    injector: Injector,
    private configurationValidateService: ConfigurationValidateService
  ) {
    super(
      injector.get(ElementRef),
      injector.get(Store),
      injector.get(RafService),
      injector.get(ChangeDetectorRef)
    );
  }

  protected onGetAce(ace): void {
    super.onGetAce(ace);
    this.jsonEditor.renderer.on('afterRender', () => {
      const currentAnnotations = this.jsonEditor.session.getAnnotations();
      if (!currentAnnotations.length && this.annotations.length) {
        this.annotationChanged = true;
      }
      if (this.annotationChanged) {
        const syntaxAnnotation = currentAnnotations.find((annotation: CustomAnnotation) => !annotation?.custom);
        this.annotationChanged = false;
        this.jsonEditor.session.setAnnotations(syntaxAnnotation ? [syntaxAnnotation, ...this.annotations] : this.annotations);
      }
    });
  }

  override validate(): Observable<ValidationErrors | null> {
    return this.configurationValidateService
      .checkConnectorConfiguration(this.deviceId, this.connectorType, this.contentValue)
      .pipe(
        map(() => {
          if (this.annotations.length !== 0) {
            this.annotations = [];
            this.annotationChanged = true;
          }
          return this.objectValid ? null : {jsonParseError: {valid: false}};
        }),
        catchError(validationWithErrors => {
          const newAnnotations = validationWithErrors.error.annotations?.map(
            (annotation: CustomAnnotation) => ({...annotation, custom: true})
          ) ?? [];

          if (!this.areAnnotationsEqual(this.annotations, newAnnotations)) {
            this.annotations = newAnnotations;
            this.annotationChanged = true;
          }

          return of({jsonParseError: {valid: false}});
        })
      );
  }

  private areAnnotationsEqual(
    oldAnnotations: CustomAnnotation[],
    newAnnotations: CustomAnnotation[]
  ): boolean {
    if (oldAnnotations?.length !== newAnnotations.length) {
      return false;
    }

    return oldAnnotations?.every((annotation: CustomAnnotation, index: number) =>
      JSON.stringify(annotation) === JSON.stringify(newAnnotations[index])
    ) ?? false;
  }
}
