///
/// Copyright © 2016-2023 The Thingsboard Authors
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

export interface Unit {
  name: string;
  symbol: string;
  tags: string[];
}

export const units: Array<Unit> = [
  {
    name: 'unit.celsius',
    symbol: '°C',
    tags: ['temperature']
  },
  {
    name: 'unit.kelvin',
    symbol: 'K',
    tags: ['temperature']
  },
  {
    name: 'unit.fahrenheit',
    symbol: '°F',
    tags: ['temperature']
  },
  {
    name: 'unit.percentage',
    symbol: '%',
    tags: ['percentage']
  },
  {
    name: 'unit.second',
    symbol: 's',
    tags: ['time']
  },
  {
    name: 'unit.minute',
    symbol: 'min',
    tags: ['time']
  },
  {
    name: 'unit.hour',
    symbol: 'h',
    tags: ['time']
  }
];

export const unitBySymbol = (symbol: string): Unit => units.find(u => u.symbol === symbol);

const searchUnitTags = (unit: Unit, searchText: string): boolean =>
  !!unit.tags.find(t => t.toUpperCase().includes(searchText.toUpperCase()));

export const searchUnits = (_units: Array<Unit>, searchText: string): Array<Unit> => _units.filter(
    u => u.symbol.toUpperCase().includes(searchText.toUpperCase()) ||
      u.name.toUpperCase().includes(searchText.toUpperCase()) ||
      searchUnitTags(u, searchText)
);
