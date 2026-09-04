// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { MissingTranslationHandler, MissingTranslationHandlerParams, StrictTranslation } from '@ngx-translate/core';
import { customTranslationsPrefix } from '@app/shared/models/constants';
import { Observable } from 'rxjs';

export class TbMissingTranslationHandler implements MissingTranslationHandler {
  handle(params: MissingTranslationHandlerParams): StrictTranslation | Observable<StrictTranslation> {
    if (params.key && !params.key.startsWith(customTranslationsPrefix)) {
      console.warn('Translation for \'' + params.key + '\' doesn\'t exist');
      let translations: any;
      const parts = params.key.split('.');
      for (let i=parts.length-1; i>=0; i--) {
        const newTranslations = {};
        if (i === parts.length-1) {
          newTranslations[parts[i]] = params.key;
        } else {
          newTranslations[parts[i]] = translations;
        }
        translations = newTranslations;
      }
      params.translateService.setTranslation(params.translateService.getCurrentLang(), translations, true);
    }
    return undefined;
  }
}
