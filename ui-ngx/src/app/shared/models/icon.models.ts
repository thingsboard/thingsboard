import { Unit, units } from '@shared/models/unit.models';
import { ResourcesService } from '@core/services/resources.service';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { isEmptyStr, isNotEmptyStr } from '@core/utils';

export interface MaterialIcon {
  name: string;
  tags: string[];
}

export const iconByName = (icons: Array<MaterialIcon>, name: string): MaterialIcon => icons.find(i => i.name === name);

const searchIconTags = (icon: MaterialIcon, searchText: string): boolean =>
  !!icon.tags.find(t => t.toUpperCase().includes(searchText.toUpperCase()));

const searchIcons = (_icons: Array<MaterialIcon>, searchText: string): Array<MaterialIcon> => _icons.filter(
  i => i.name.toUpperCase().includes(searchText.toUpperCase()) ||
    searchIconTags(i, searchText)
);

const getCommonMaterialIcons = (icons: Array<MaterialIcon>): Array<MaterialIcon> => icons.slice(0, 44);

export const getMaterialIcons = (resourcesService: ResourcesService,  all = false, searchText: string): Observable<string[]> =>
  resourcesService.loadJsonResource<Array<MaterialIcon>>('/assets/metadata/material-icons.json').pipe(
    map((icons) => {
      if (isNotEmptyStr(searchText)) {
        return searchIcons(icons, searchText);
      } else if (!all) {
        return getCommonMaterialIcons(icons);
      } else {
        return icons;
      }
    }),
    map((icons) => icons.map(icon => icon.name))
  );
