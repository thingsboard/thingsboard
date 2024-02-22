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

/* eslint-disable */
export const POSTAL_CODE_PATTERNS = {
  'United States': '(\\d{5}([\\-]\\d{4})?)',
  'Australia': '[0-9]{4}',
  'Austria': '[0-9]{4}',
  'Belgium': '[0-9]{4}',
  'Brazil': '[0-9]{5}[\\-]?[0-9]{3}',
  'Canada': '^(?!.*[DFIOQU])[A-VXY][0-9][A-Z][ -]?[0-9][A-Z][0-9]$',
  'Denmark': '[0-9]{3,4}',
  'Faroe Islands': '[0-9]{3,4}',
  'Netherlands': '[1-9][0-9]{3}\\s?[a-zA-Z]{2}',
  'Germany': '[0-9]{5}',
  'Hungary': '[0-9]{4}',
  'Italy': '[0-9]{5}',
  'Japan': '\\d{3}-\\d{4}',
  'Luxembourg': '(L\\s*(-|—|–))\\s*?[\\d]{4}',
  'Poland': '[0-9]{2}\\-[0-9]{3}',
  'Spain': '((0[1-9]|5[0-2])|[1-4][0-9])[0-9]{3}',
  'Sweden': '\\d{3}\\s?\\d{2}',
  'United Kingdom': '[A-Za-z]{1,2}[0-9Rr][0-9A-Za-z]? [0-9][ABD-HJLNP-UW-Zabd-hjlnp-uw-z]{2}'
};
/* eslint-enable */

