// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { defer, Observable, switchMap, shareReplay, from } from 'rxjs';
import { IModulesMap } from '@modules/common/modules-map.models';

class LazyModulesMap implements IModulesMap {

  private modulesMap$: Observable<IModulesMap> = defer(() =>
    from(import('@modules/common/modules-map')).pipe(
      switchMap(module => module.modulesMap.init().pipe(switchMap(() => [module.modulesMap])))
    )
  ).pipe(
    shareReplay({bufferSize: 1, refCount: false})
  );

  init(): Observable<any> {
    return this.modulesMap$;
  }
}

export const lazyModulesMap = new LazyModulesMap();
