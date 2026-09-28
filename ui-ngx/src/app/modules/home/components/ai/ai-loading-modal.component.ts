// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Inject, OnInit, ViewEncapsulation } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { from, of } from 'rxjs';
import { concatMap, delay } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { generatedStateImages } from '@home/components/ai/ai.models';

export interface AiLoadingModalData {
  messages: Array<{text: string; delay: number}>;
  estimateWaitTime: number;
  image?: string;
}

@Component({
  selector: 'tb-ai-loading-modal',
  templateUrl: './ai-loading-modal.component.html',
  styleUrl: './ai-loading-modal.component.scss',
  encapsulation: ViewEncapsulation.None,
  standalone: false,
  host: {
    style: 'width: 100%; height: 100%; min-height: 100%; display: flex; overflow: hidden;'
  }
})
export class AiLoadingModalComponent extends DialogComponent<AiLoadingModalComponent> implements OnInit {

  generateImage: string = generatedStateImages[Math.floor(Math.random() * generatedStateImages.length)];
  statusMessage: string = '';
  statusMessageChanged = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AiLoadingModalData,
              public dialogRef: MatDialogRef<AiLoadingModalComponent>,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);
  }

  ngOnInit() {
    this.initGenerateState();
  }

  private initGenerateState(): void {
    from(this.data.messages).pipe(
      concatMap(message => of(message).pipe(delay(message.delay))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((msg) => {
      this.statusMessageChanged = true;
      this.statusMessage = msg.text;
      setTimeout(() => {
        this.statusMessageChanged = false;
      }, 500);
    });
  }

}
