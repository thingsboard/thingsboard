import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { DialogComponent } from "@shared/components/dialog.component";
import { Store } from "@ngrx/store";
import { AppState } from "@core/core.state";
import { Router } from "@angular/router";

export interface EdgeInstructionsData {
  instructions: string;
}

@Component({
  selector: 'tb-edge-instructions',
  templateUrl: './edge-instructions-dialog.component.html',
  styleUrls: ['./edge-instructions-dialog.component.scss']
})
export class EdgeInstructionsDialogComponent extends DialogComponent<EdgeInstructionsDialogComponent, EdgeInstructionsData> {

  instructions: string = this.data.instructions;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<EdgeInstructionsDialogComponent, EdgeInstructionsData>,
              @Inject(MAT_DIALOG_DATA) public data: EdgeInstructionsData) {
    super(store, router, dialogRef);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
