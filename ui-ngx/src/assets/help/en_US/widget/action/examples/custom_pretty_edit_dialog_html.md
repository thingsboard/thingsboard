#### HTML template of dialog to edit a device or an asset

```html
{:code-style="max-height: 400px;"}
<form #editEntityForm="ngForm" [formGroup]="editEntityFormGroup"
      (ngSubmit)="save()"  class="edit-entity-form">
  <mat-toolbar fxLayout="row" color="primary">
    <h2>Edit  </h2>
    <span fxFlex></span>
    <button mat-icon-button (click)="cancel()" type="button">
      <mat-icon class="material-icons">close</mat-icon>
    </button>
  </mat-toolbar>
  <mat-progress-bar color="warn" mode="indeterminate" *ngIf="isLoading$ | async">
  </mat-progress-bar>
  <div style="height: 4px;" *ngIf="!(isLoading$ | async)"></div>
  <div mat-dialog-content fxLayout="column">
    <div fxLayout="row" fxLayoutGap="8px" fxLayout.xs="column"  fxLayoutGap.xs="0">
      <mat-form-field fxFlex class="mat-block">
        <mat-label>Entity Name</mat-label>
        <input matInput formControlName="entityName" required readonly="">
      </mat-form-field>
      <mat-form-field fxFlex class="mat-block">
        <mat-label>Entity Label</mat-label>
        <input matInput formControlName="entityLabel">
      </mat-form-field>
    </div>
    <div fxLayout="row" fxLayoutGap="8px" fxLayout.xs="column"  fxLayoutGap.xs="0">
      <mat-form-field fxFlex class="mat-block">
        <mat-label>Entity Type</mat-label>
        <input matInput formControlName="entityType" readonly>
      </mat-form-field>
      <mat-form-field fxFlex class="mat-block">
        <mat-label>Type</mat-label>
        <input matInput formControlName="type" readonly>
      </mat-form-field>
    </div>
    <div formGroupName="attributes" fxLayout="column">
      <div fxLayout="row" fxLayoutGap="8px" fxLayout.xs="column"  fxLayoutGap.xs="0">
        <mat-form-field fxFlex class="mat-block">
          <mat-label>Latitude</mat-label>
          <input type="number" step="any" matInput formControlName="latitude">
        </mat-form-field>
        <mat-form-field fxFlex class="mat-block">
          <mat-label>Longitude</mat-label>
          <input type="number" step="any" matInput formControlName="longitude">
        </mat-form-field>
      </div>
      <div fxLayout="row" fxLayoutGap="8px" fxLayout.xs="column"  fxLayoutGap.xs="0">
        <mat-form-field fxFlex class="mat-block">
          <mat-label>Address</mat-label>
          <input matInput formControlName="address">
        </mat-form-field>
        <mat-form-field fxFlex class="mat-block">
          <mat-label>Owner</mat-label>
          <input matInput formControlName="owner">
        </mat-form-field>
      </div>
      <div fxLayout="row" fxLayoutGap="8px" fxLayout.xs="column"  fxLayoutGap.xs="0">
        <mat-form-field fxFlex class="mat-block">
          <mat-label>Integer Value</mat-label>
          <input type="number" step="1" matInput formControlName="number">
          <mat-error *ngIf="editEntityFormGroup.get('attributes.number').hasError('pattern')">
            Invalid integer value.
          </mat-error>
        </mat-form-field>
        <div class="boolean-value-input" fxLayout="column" fxLayoutAlign="center start" fxFlex>
          <label class="checkbox-label">Boolean Value</label>
          <mat-checkbox formControlName="booleanValue" style="margin-bottom: 40px;">

          </mat-checkbox>
        </div>
      </div>
    </div>
    <div class="relations-list old-relations">
      <div class="mat-body-1" style="padding-bottom: 10px; color: rgba(0,0,0,0.57);">Relations</div>
      <div class="body" [fxShow]="oldRelations().length">
        <div class="row" fxLayout="row" fxLayoutAlign="start center" formArrayName="oldRelations"
             *ngFor="let relation of oldRelations().controls; let i = index;">
          <div [formGroupName]="i" class="mat-elevation-z2" fxFlex fxLayout="row" style="padding: 5px 0 5px 5px;">
            <div fxFlex fxLayout="column">
              <div fxLayout="row" fxLayoutGap="8px" fxLayout.xs="column"  fxLayoutGap.xs="0">
                <mat-form-field class="mat-block" style="min-width: 100px;">
                  <mat-label>Direction</mat-label>
                  <mat-select formControlName="direction" name="direction">
                    <mat-option *ngFor="let direction of entitySearchDirection | keyvalue" [value]="direction.value">

                    </mat-option>
                  </mat-select>
                  <mat-error *ngIf="relation.get('direction').hasError('required')">
                    Relation direction is required.
                  </mat-error>
                </mat-form-field>
                <tb-relation-type-autocomplete
                  fxFlex class="mat-block"
                  formControlName="relationType"
                  required="true">
                </tb-relation-type-autocomplete>
              </div>
              <div fxLayout="row" fxLayout.xs="column">
                <tb-entity-select
                  fxFlex class="mat-block"
                  required="true"
                  formControlName="relatedEntity">
                </tb-entity-select>
              </div>
            </div>
            <div fxLayout="column" fxLayoutAlign="center center">
              <button mat-icon-button color="primary"
                      aria-label="Remove"
                      type="button"
                      (click)="removeOldRelation(i)"
                      matTooltip="Remove relation"
                      matTooltipPosition="above">
                <mat-icon>close</mat-icon>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="relations-list">
      <div class="mat-body-1" style="padding-bottom: 10px; color: rgba(0,0,0,0.57);">New Relations</div>
      <div class="body" [fxShow]="relations().length">
        <div class="row" fxLayout="row" fxLayoutAlign="start center" formArrayName="relations" *ngFor="let relation of relations().controls; let i = index;">
          <div [formGroupName]="i" class="mat-elevation-z2" fxFlex fxLayout="row" style="padding: 5px 0 5px 5px;">
            <div fxFlex fxLayout="column">
              <div fxLayout="row" fxLayoutGap="8px" fxLayout.xs="column"  fxLayoutGap.xs="0">
                <mat-form-field class="mat-block" style="min-width: 100px;">
                  <mat-label>Direction</mat-label>
                  <mat-select formControlName="direction" name="direction">
                    <mat-option *ngFor="let direction of entitySearchDirection | keyvalue" [value]="direction.value">

                    </mat-option>
                  </mat-select>
                  <mat-error *ngIf="relation.get('direction').hasError('required')">
                    Relation direction is required.
                  </mat-error>
                </mat-form-field>
                <tb-relation-type-autocomplete
                  fxFlex class="mat-block"
                  formControlName="relationType"
                  [required]="true">
                </tb-relation-type-autocomplete>
              </div>
              <div fxLayout="row" fxLayout.xs="column">
                <tb-entity-select
                  fxFlex class="mat-block"
                  [required]="true"
                  formControlName="relatedEntity">
                </tb-entity-select>
              </div>
            </div>
            <div fxLayout="column" fxLayoutAlign="center center">
              <button mat-icon-button color="primary"
                      aria-label="Remove"
                      type="button"
                      (click)="removeRelation(i)"
                      matTooltip="Remove relation"
                      matTooltipPosition="above">
                <mat-icon>close</mat-icon>
              </button>
            </div>
          </div>
        </div>
      </div>
      <div>
        <button mat-raised-button color="primary"
                type="button"
                (click)="addRelation()"
                matTooltip="Add Relation"
                matTooltipPosition="above">
          Add
        </button>
      </div>
    </div>
  </div>
  <div mat-dialog-actions fxLayout="row" fxLayoutAlign="end center">
    <button mat-button color="primary"
            type="button"
            [disabled]="(isLoading$ | async)"
            (click)="cancel()" cdkFocusInitial>
      Cancel
    </button>
    <button mat-button mat-raised-button color="primary"
            type="submit"
            [disabled]="(isLoading$ | async) || editEntityForm.invalid || !editEntityForm.dirty">
      Save
    </button>
  </div>
</form>
{:copy-code}
```

<br>
<br>
