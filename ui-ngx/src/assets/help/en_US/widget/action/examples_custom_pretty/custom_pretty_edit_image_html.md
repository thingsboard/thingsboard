#### HTML template of dialog to add/edit image in entity attribute

```html
{:code-style="max-height: 400px;"}
<form [formGroup]="editEntity" (ngSubmit)="save()" class="edit-entity-form">
  <mat-toolbar color="tb-primary" class="tb-bottom-border flex flex-row">
    <h2>Edit {{entityName}} image</h2>
    <span class="flex-1"></span>
    <button mat-icon-button (click)="cancel()" type="button">
      <mat-icon class="material-icons">close</mat-icon>
    </button>
  </mat-toolbar>
  <mat-progress-bar color="warn" mode="indeterminate" *ngIf="(isLoading$ | async) || loading">
  </mat-progress-bar>
  <div style="height: 4px;" *ngIf="!(isLoading$ | async) && !loading"></div>
  <div mat-dialog-content class="flex flex-col">
    <div formGroupName="attributes" class="flex flex-col">
      <tb-image-input
        label="Entity image"
        formControlName="image"
      ></tb-image-input>
    </div>
  </div>
  <div mat-dialog-actions class="flex flex-row items-center justify-end">
    <button mat-button color="primary"
            type="button"
            [disabled]="(isLoading$ | async)"
            (click)="cancel()" cdkFocusInitial>
      Cancel
    </button>
    <button mat-button mat-raised-button color="primary"
            type="submit"
            [disabled]="(isLoading$ | async) || editEntity.invalid || !editEntity.dirty">
      Save
    </button>
  </div>
</form>
{:copy-code}
```

<br>
<br>
