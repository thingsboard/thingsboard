// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { EdgeService } from '@core/http/edge.service';
import { Edge } from '@shared/models/edge.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AddEntityDialogComponent } from '@home/components/entity/add-entity-dialog.component';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { EdgeComponent } from '@home/pages/edge/edge.component';

@Injectable({ providedIn: 'root' })
export class EdgeCreateDialogService {

  constructor(private dialog: MatDialog,
              private edgeService: EdgeService) {}

  create(): Observable<Edge> {
    const config = new EntityTableConfig<Edge>();
    config.entityType = EntityType.EDGE;
    config.entityComponent = EdgeComponent;
    config.entityTranslations = entityTypeTranslations.get(EntityType.EDGE);
    config.entityResources = entityTypeResources.get(EntityType.EDGE);
    config.entityTitle = (edge) => edge ? edge.name : '';
    config.addDialogStyle = {maxHeight: '100vh'};
    config.saveEntity = (edge) => this.edgeService.saveEdge(edge);
    return this.dialog.open<AddEntityDialogComponent, AddEntityDialogData<Edge>, Edge>(
      AddEntityDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { entitiesTableConfig: config }
      }
    ).afterClosed();
  }
}
