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

import { ResourcesService } from '@core/services/resources.service';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { isNotEmptyStr } from '@core/utils';

export interface MaterialIcon {
  name: string;
  displayName?: string;
  tags: string[];
}

export const iconByName = (icons: Array<MaterialIcon>, name: string): MaterialIcon => icons.find(i => i.name === name);

const searchIconTags = (icon: MaterialIcon, searchText: string): boolean =>
  !!icon.tags.find(t => t.toUpperCase().includes(searchText.toUpperCase()));

const searchIcons = (_icons: Array<MaterialIcon>, searchText: string): Array<MaterialIcon> => _icons.filter(
  i => i.name.toUpperCase().includes(searchText.toUpperCase()) ||
    i.displayName.toUpperCase().includes(searchText.toUpperCase()) ||
    searchIconTags(i, searchText)
);

const getCommonMaterialIcons = (icons: Array<MaterialIcon>, chunkSize: number): Array<MaterialIcon> => icons.slice(0, chunkSize * 4);

export const getMaterialIcons = (resourcesService: ResourcesService,  chunkSize = 11,
                                 all = false, searchText: string): Observable<MaterialIcon[][]> =>
  resourcesService.loadJsonResource<Array<MaterialIcon>>('/assets/metadata/material-icons.json',
    (icons) => {
      for (const icon of icons) {
        const words = icon.name.replace(/_/g, ' ').split(' ');
        for (let i = 0; i < words.length; i++) {
          words[i] = words[i].charAt(0).toUpperCase() + words[i].slice(1);
        }
        icon.displayName = words.join(' ');
      }
      return icons;
    }
  ).pipe(
    map((icons) => {
      if (isNotEmptyStr(searchText)) {
        return searchIcons(icons, searchText);
      } else if (!all) {
        return getCommonMaterialIcons(icons, chunkSize);
      } else {
        return icons;
      }
    }),
    map((icons) => {
      const iconChunks: MaterialIcon[][] = [];
      for (let i = 0; i < icons.length; i += chunkSize) {
        const chunk = icons.slice(i, i + chunkSize);
        iconChunks.push(chunk);
      }
      return iconChunks;
    })
  );
