// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { defer, map, Observable, shareReplay, switchMap } from 'rxjs';
import { IModulesMap } from '@modules/common/modules-map.models';

class LazyModulesMap implements IModulesMap {

  private modulesMap$: Observable<IModulesMap> = defer(() => import('@modules/common/modules-map')).pipe(
    switchMap(module => module.modulesMap.init().pipe(map(() => module.modulesMap))),
    shareReplay(1)
  );

  init(): Observable<any> {
    return this.modulesMap$;
  }
}

export const lazyModulesMap = new LazyModulesMap();
