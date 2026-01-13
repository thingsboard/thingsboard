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

import { MissingTranslationHandler, MissingTranslationHandlerParams } from '@ngx-translate/core';
import { customTranslationsPrefix } from '@app/shared/models/constants';

export class TbMissingTranslationHandler implements MissingTranslationHandler {
  handle(params: MissingTranslationHandlerParams) {
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
      params.translateService.setTranslation(params.translateService.currentLang, translations, true);
    }
  }
}
