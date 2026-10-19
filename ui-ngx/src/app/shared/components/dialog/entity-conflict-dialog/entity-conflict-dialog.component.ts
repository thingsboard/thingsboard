// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SharedModule } from '@shared/shared.module';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { CommonModule } from '@angular/common';
import { entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityInfoData, VersionedEntity } from '@shared/models/entity.models';
import { EntityId } from '@shared/models/id/entity-id';
import { RuleChainMetaData } from '@shared/models/rule-chain.models';

interface EntityConflictDialogData {
  message: string;
  entity: VersionedEntity;
}

@Component({
    selector: 'tb-entity-conflict-dialog',
    templateUrl: 'entity-conflict-dialog.component.html',
    styleUrls: ['./entity-conflict-dialog.component.scss'],
    imports: [
        CommonModule,
        SharedModule,
    ]
})
export class EntityConflictDialogComponent {

  entityId: EntityId;
  entityTypeLabel: string;

  readonly entityTypeTranslations = entityTypeTranslations;
  private readonly defaultEntityLabel = 'entity.entity';

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: EntityConflictDialogData,
    private dialogRef: MatDialogRef<EntityConflictDialogComponent>,
    private importExportService: ImportExportService,
  ) {
    this.entityId = (data.entity as EntityInfoData).id ?? (data.entity as RuleChainMetaData).ruleChainId;
    this.entityTypeLabel = entityTypeTranslations.has(this.entityId.entityType)
      ? (entityTypeTranslations.get(this.entityId.entityType).type)
      : this.defaultEntityLabel;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onDiscard(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onLinkClick(event: MouseEvent): void {
    event.preventDefault();
    this.importExportService.exportEntity(this.data.entity);
  }
}
