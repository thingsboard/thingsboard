#### HTML template of dialog to create new user

```html
{:code-style="max-height: 400px;"}
<form [formGroup]="addEntityFormGroup" (ngSubmit)="save()" style="min-width:480px;">
  <mat-toolbar class="flex flex-row" color="primary">
    <h2>Add new User</h2>
    <span class="flex-1"></span>
    <button mat-icon-button (click)="cancel()" type="button">
      <mat-icon class="material-icons">close</mat-icon>
    </button>
  </mat-toolbar>
  <mat-progress-bar color="warn" mode="indeterminate"
                    *ngIf="isLoading$ | async">
  </mat-progress-bar>
  <div style="height: 4px;" *ngIf="!(isLoading$ | async)">
  </div>
  <div mat-dialog-content class="flex flex-col">
    <div class="flex flex-row gap-2 xs:flex-col xs:gap-0">
      <mat-form-field class="mat-block flex-1">
        <mat-label>Email</mat-label>
        <input matInput formControlName="email" required
               ng-pattern='/^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\_\-0-9]+\.)+[a-zA-Z]{2,}))$/'>
        <mat-error *ngIf="addEntityFormGroup.get('email').hasError('required')">
          Email is required
        </mat-error>
        <mat-error *ngIf="addEntityFormGroup.get('email').hasError('pattern')">
          Invalid email format
        </mat-error>
      </mat-form-field>
    </div>
    <div class="flex flex-row gap-2 xs:flex-col xs:gap-0">
      <mat-form-field class="mat-block flex-1">
        <mat-label>First Name</mat-label>
        <input matInput
               formControlName="firstName">
      </mat-form-field>
    </div>
    <div class="flex flex-row gap-2 xs:flex-col xs:gap-0">
      <mat-form-field class="mat-block flex-1">
        <mat-label>Last Name</mat-label>
        <input matInput
               formControlName="lastName">
      </mat-form-field>
    </div>
    <div class="flex flex-row gap-2 xs:flex-col xs:gap-0">
      <mat-form-field class="mat-block flex-1">
        <mat-label>User activation method</mat-label>
        <mat-select matInput formControlName="userActivationMethod">
          <mat-option *ngFor="let activationMethod of activationMethods" [value]="activationMethod.value">
            {{activationMethod.name}}
          </mat-option>
        </mat-select>
        <mat-error *ngIf="addEntityFormGroup.get('userActivationMethod').hasError('required')">Please choose activation method</mat-error>
        <mat-hint>e.g. Send activation email</mat-hint>
      </mat-form-field>
    </div>
  </div>
  <div mat-dialog-actions class="flex flex-row items-center justify-end">
    <button mat-button color="primary" type="button"
            [disabled]="(isLoading$ | async)"
            (click)="cancel()" cdkFocusInitial>
      Cancel
    </button>
    <button mat-button mat-raised-button color="primary"
            type="submit"
            [disabled]="(isLoading$ | async) || addEntityFormGroup.invalid || !addEntityFormGroup.dirty">
      Create
    </button>
  </div>
</form>
{:copy-code}
```

<br>
<br>
