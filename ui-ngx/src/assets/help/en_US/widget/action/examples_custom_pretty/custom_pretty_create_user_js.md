#### Function displaying dialog to create new user

```javascript
{:code-style="max-height: 400px;"}
const $injector = widgetContext.$scope.$injector;
const customDialog = $injector.get(widgetContext.servicesMap.get('customDialog'));
const userService = $injector.get(widgetContext.servicesMap.get('userService'));
const $scope = widgetContext.$scope;
const rxjs = widgetContext.rxjs;

openAddUserDialog();

function openAddUserDialog() {
    customDialog.customDialog(htmlTemplate, AddUserDialogController).subscribe();
}

function AddUserDialogController(instance) {
    let vm = instance;

    vm.currentUser = widgetContext.currentUser;

    vm.addEntityFormGroup = vm.fb.group({
        email: ['', [vm.validators.required, vm.validators.pattern(/^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\_\-0-9]+\.)+[a-zA-Z]{2,}))$/)]],
        firstName: [''],
        lastName: ['', ],
        userActivationMethod: ['', [vm.validators.required]]
    });

    vm.activationMethods = [
        {
            value: 'displayActivationLink',
            name: 'Display activation link'
        },
        {
            value: 'sendActivationMail',
            name: 'Send activation email'
        }
    ];

    vm.cancel = function() {
        vm.dialogRef.close(null);
    };

    vm.save = function() {
        let formObj = vm.addEntityFormGroup.getRawValue();
        let attributes = [];
        let sendActivationMail = false;
        let newUser = {
            email: formObj.email,
            firstName: formObj.firstName,
            lastName: formObj.lastName,
            authority: 'TENANT_ADMIN'
        };

        if (formObj.userActivationMethod === 'sendActivationMail') {
            sendActivationMail = true;
        }

        userService.saveUser(newUser, sendActivationMail).pipe(
            rxjs.mergeMap((user) => {
                let activationObs;
                if (sendActivationMail) {
                    activationObs = rxjs.of(null);
                } else {
                    activationObs = userService.getActivationLink(user.id.id);
                }
                return activationObs.pipe(
                    rxjs.mergeMap((activationLink) => {
                        return activationLink ? customDialog.customDialog(activationLinkDialogTemplate, ActivationLinkDialogController, {"activationLink": activationLink}) : rxjs.of(null);
                    })
                );
            })
        ).subscribe(() => {
            vm.dialogRef.close(null);
        });
    };
}

function ActivationLinkDialogController(instance) {
    let vm = instance;

    vm.activationLink = vm.data.activationLink;

    vm.onActivationLinkCopied = () => {
        $scope.showSuccessToast("User activation link has been copied to clipboard", 1200, "bottom", "left", "activationLinkDialogContent");
    };

    vm.close = () => {
        vm.dialogRef.close(null);
    };
}

let activationLinkDialogTemplate = `<form style="min-width: 400px; position: relative;">
  <mat-toolbar color="primary">
    <h2 translate>user.activation-link</h2>
    <span fxFlex></span>
    <button mat-button mat-icon-button
            (click)="close()"
            type="button">
      <mat-icon class="material-icons">close</mat-icon>
    </button>
  </mat-toolbar>
  <mat-progress-bar color="warn" mode="indeterminate" *ngIf="isLoading$ | async">
  </mat-progress-bar>
  <div style="height: 4px;" *ngIf="!(isLoading$ | async)"></div>
  <div mat-dialog-content tb-toast toastTarget="activationLinkDialogContent">
    <div class="mat-content" fxLayout="column">
      <span [innerHTML]="'user.activation-link-text' | translate: {activationLink: activationLink}"></span>
      <div fxLayout="row" fxLayoutAlign="start center">
        <pre class="tb-highlight" fxFlex><code>{{ activationLink }}</code></pre>
        <button mat-icon-button
                color="primary"
                ngxClipboard
                cbContent="{{ activationLink }}"
                (cbOnSuccess)="onActivationLinkCopied()"
                matTooltip="{{ 'user.copy-activation-link' | translate }}"
                matTooltipPosition="above">
          <mat-icon svgIcon="mdi:clipboard-arrow-left"></mat-icon>
        </button>
      </div>
    </div>
  </div>
  <div mat-dialog-actions fxLayoutAlign="end center">
    <button mat-button color="primary"
            type="button"
            cdkFocusInitial
            [disabled]="(isLoading$ | async)"
            (click)="close()">
      {{ 'action.ok' | translate }}
    </button>
  </div>
</form>`;
{:copy-code}
```

<br>
<br>
