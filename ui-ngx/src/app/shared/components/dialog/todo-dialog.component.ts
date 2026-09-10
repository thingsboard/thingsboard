// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';

@Component({
    selector: 'tb-todo-dialog',
    templateUrl: './todo-dialog.component.html',
    styleUrls: ['./todo-dialog.component.scss'],
    standalone: false
})
export class TodoDialogComponent {
  constructor(public dialogRef: MatDialogRef<TodoDialogComponent>) {
  }
}
