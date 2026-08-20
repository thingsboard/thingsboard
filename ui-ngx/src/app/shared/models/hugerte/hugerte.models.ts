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

import { EditorOptions } from 'hugerte';

export const HUGERTE_ASSETS_PATH = 'assets/hugerte';

export const HUGERTE_BODY_ID = 'hugerte';

export function defaultHugeRteOptions(overrides: Partial<EditorOptions>): Partial<EditorOptions> {
  return {
    base_url: '/' + HUGERTE_ASSETS_PATH,
    suffix: '.min',
    body_id: HUGERTE_BODY_ID,
    autofocus: false,
    branding: false,
    relative_urls: false,
    urlconverter_callback: (url) => url,
    ...overrides
  };
}
