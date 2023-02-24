#### HTML template of dialog to clone device

```html
{:code-style="max-height: 400px;"}
<form [formGroup]="cloneDeviceFormGroup" (ngSubmit)="save()" style="min-width:320px;">
    <mat-toolbar fxLayout="row" color="primary">
        <h2>Clone device: {{ deviceName }}</h2>
        <span fxFlex></span>
        <button mat-icon-button (click)="cancel()"
                type="button">
            <mat-icon class="material-icons">close
            </mat-icon>
        </button>
    </mat-toolbar>
    <mat-progress-bar color="warn" mode="indeterminate"
                      *ngIf="isLoading$ | async">
    </mat-progress-bar>
    <div style="height: 4px;" *ngIf="!(isLoading$ | async)"></div>
    <div mat-dialog-content fxLayout="column">
        <mat-form-field fxFlex class="mat-block">
            <mat-label>Clone device name</mat-label>
            <input matInput formControlName="cloneName" required>
            <mat-error *ngIf="cloneDeviceFormGroup.get('cloneName').hasError('required')">
                Clone device name is required
            </mat-error>
        </mat-form-field>
    </div>
    <div mat-dialog-actions fxLayout="row"
         fxLayoutAlign="end center">
        <button mat-button color="primary" type="button"
                [disabled]="(isLoading$ | async)"
                (click)="cancel()" cdkFocusInitial>
            Cancel
        </button>
        <button mat-button mat-raised-button color="primary"
                type="submit"
                [disabled]="(isLoading$ | async) || cloneDeviceFormGroup.invalid || !cloneDeviceFormGroup.dirty">
            Save
        </button>
    </div>
</form>
{:copy-code}
```

<br>
<br>
