// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { DialogService } from '@core/services/dialog.service';
import { EdgeProfileService } from '@core/http/edge-profile.service';
import { Edge } from '@shared/models/edge.models';

// a profile switch unassigns the old profile's rows and the edge deletes their entities, so it is never silent
export function confirmEdgeProfileChange(edge: Edge, originalEdge: Edge, edgeProfileService: EdgeProfileService,
                                         dialogService: DialogService, translate: TranslateService): Observable<boolean> {
  const fromProfileId = originalEdge?.id ? originalEdge.edgeProfileId?.id : null;
  const toProfileId = edge.edgeProfileId?.id;
  if (!fromProfileId || !toProfileId || fromProfileId === toProfileId) {
    return of(true);
  }
  return forkJoin([
    edgeProfileService.getEdgeProfile(fromProfileId),
    edgeProfileService.getEdgeProfile(toProfileId)
  ]).pipe(
    mergeMap(([fromProfile, toProfile]) => dialogService.confirm(
      translate.instant('edge.change-edge-profile-title', {edgeName: edge.name, toProfile: toProfile.name}),
      translate.instant('edge.change-edge-profile-text', {fromProfile: fromProfile.name, toProfile: toProfile.name}),
      translate.instant('action.no'),
      translate.instant('action.yes')
    ))
  );
}
