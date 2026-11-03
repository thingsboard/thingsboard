// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Pipe, PipeTransform } from '@angular/core';
import { UtilsService } from '@core/services/utils.service';

@Pipe({
    name: 'customTranslate',
    standalone: false
})
export class CustomTranslatePipe implements PipeTransform {

  constructor(private utils: UtilsService) { }

  transform(translationValue: string, defaultValue?: string): string {
    if (!defaultValue) {
      defaultValue = translationValue;
    }
    return this.utils.customTranslation(translationValue, defaultValue);
  }
}
