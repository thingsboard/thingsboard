class StepperCtrl {

    public static $inject = [
        '$mdComponentRegistry',
        '$attrs',
        '$log'
    ];

    /* Bindings */

    public linear: boolean;
    public alternative: boolean;
    public vertical: boolean;
    public mobileStepText: boolean;
    public labelStep: string = 'Step';
    public labelOf: string = 'of';

    /* End of bindings */

    public steps = [];
    public currentStep = 0;

    private hasFeedback: boolean;
    private feedbackMessage: string;
    private registeredStepper;

    constructor(
        private $mdComponentRegistry,
        private $attrs,
        private $log
    ) { }


    $onInit() {
        if (this.$attrs.mdMobileStepText === '') {
            this.mobileStepText = true;
        }
        if (this.$attrs.mdLinear === '') {
            this.linear = true;
        }
        if (this.$attrs.mdAlternative === '') {
            this.alternative = true;
        }
    }

    $postLink() {
        if (!this.$attrs.id) {
            this.$log.warn('You must set an id attribute to your stepper');
        }
        this.registeredStepper = this.$mdComponentRegistry.register(this, this.$attrs.id);
    }

    $onDestroy() {
        this.registeredStepper && this.registeredStepper();
    }

    /**
     * Register component step to this stepper.
     * 
     * @param {StepCtrl} step The step to add.
     * @returns number - The step number.
     */
    $addStep(step: StepCtrl) {
        return this.steps.push(step) - 1;
    }

    /**
     * Complete the current step and move one to the next. 
     * Using this method on editable steps (in linear stepper) 
     * it will search by the next step without "completed" state to move. 
     * When invoked it dispatch the event onstepcomplete to the step element.
     * 
     * @returns boolean - True if move and false if not move (e.g. On the last step)
     */
    public next() {
        if (this.currentStep < this.steps.length) {
            this.clearError();
            this.currentStep++;
            this.clearFeedback();
            return true;
        }
        return false;
    }

    /**
     * Move to the previous step without change the state of current step. 
     * Using this method in linear stepper it will check if previous step is editable to move.
     * 
     * @returns boolean - True if move and false if not move (e.g. On the first step)
     */
    public back() {
        if (this.currentStep > 0) {
            this.clearError();
            this.currentStep--;
            this.clearFeedback();
            return true;
        }
        return false;
    }

    /**
     * Move to the next step without change the state of current step. 
     * This method works only in optional steps.
     * 
     * @returns boolean - True if move and false if not move (e.g. On non-optional step)
     */
    public skip() {
        let step = this.steps[this.currentStep];
        if (step.optional) {
            this.currentStep++;
            this.clearFeedback();
            return true;
        }
        return false;
    }


    /**
     * Defines the current step state to "error" and shows the message parameter on 
     * title message element.When invoked it dispatch the event onsteperror to the step element.
     * 
     * @param {string} message The error message
     */
    public error(message: string) {
        let step = this.steps[this.currentStep];
        step.hasError = true;
        step.message = message;
        this.clearFeedback();
    }

    /**
     * Defines the current step state to "normal" and removes the message parameter on 
     * title message element.
     */
    public clearError() {
        let step = this.steps[this.currentStep];
        step.hasError = false;
    }

    /**
     * Move "active" to specified step id parameter. 
     * The id used as reference is the integer number shown on the label of each step (e.g. 2).
     * 
     * @param {number} stepNumber (description)
     * @returns boolean - True if move and false if not move (e.g. On id not found)
     */
    public goto(stepNumber: number) {
        if (stepNumber < this.steps.length) {
            this.currentStep = stepNumber;
            this.clearFeedback();
            return true;
        }
        return false;
    }

    /**
     * Shows a feedback message and a loading indicador.
     * 
     * @param {string} [message] The feedbackMessage
     */
    public showFeedback(message?: string) {
        this.hasFeedback = true;
        this.feedbackMessage = message;
    }

    /**
     * Removes the feedback.
     */
    public clearFeedback() {
        this.hasFeedback = false;
    }


    isCompleted(stepNumber: number) {
        return this.linear && stepNumber < this.currentStep;
    };

    isActiveStep(step: StepCtrl) {
        return this.steps.indexOf(step) === this.currentStep;
    }
}

interface StepperService {
    (handle: string): StepperCtrl;
}

let StepperServiceFactory = ['$mdComponentRegistry',
    function ($mdComponentRegistry) {
        return <StepperService>function (handle: string) {
            let instance: StepperCtrl = $mdComponentRegistry.get(handle);

            if (!instance) {
                $mdComponentRegistry.notFoundError(handle);
            }

            return instance;
        };
    }];


class StepCtrl {

    public static $inject = [
        '$element',
        '$compile',
        '$scope'
    ];

    /* Bindings */

    public label: string;
    public optional: string;

    /* End of bindings */

    public stepNumber: number;

    public $stepper: StepperCtrl;

    /**
     *
     */
    constructor(
        private $element,
        private $compile: ng.ICompileService,
        private $scope: ng.IScope
    ) { }

    $postLink() {
        this.stepNumber = this.$stepper.$addStep(this);
    }

    isActive() {
        let state = this.$stepper.isActiveStep(this);
        return state;
    }
}

angular.module('mdSteppers', ['ngMaterial'])
    .factory('$mdStepper', StepperServiceFactory)

    .directive('mdStepper', () => {
        return {
            transclude: true,
            scope: {
                linear: '<?mdLinear',
                alternative: '<?mdAlternative',
                vertical: '<?mdVertical',
                mobileStepText: '<?mdMobileStepText',
                labelStep: '@?mdLabelStep',
                labelOf: '@?mdLabelOf'
            },
            bindToController: true,
            controller: StepperCtrl,
            controllerAs: 'stepper',
            templateUrl: 'mdSteppers/mdStepper.tpl.html'
            // link: function (scope, element, attrs) {
            //     scope.stepper.mobileStepText = !!attrs.$attr['mdMobileStepText'];
            // }
        };
    })
    .directive('mdStep', ['$compile', '$mdTheming', ($compile, $mdTheming) => {
        return {
            require: '^^mdStepper',
            transclude: true,
            scope: {
                label: '@mdLabel',
                optional: '@?mdOptional'
            },
            bindToController: true,
            controller: StepCtrl,
            controllerAs: '$ctrl',
            link: (scope: any, iElement: ng.IRootElementService, iAttrs, stepperCtrl: StepperCtrl) => {
                $mdTheming(iElement);
                function addOverlay() {
                    let hasOverlay = !!iElement.find('.md-step-body-overlay')[0];
                    if (!hasOverlay) {
                        let overlay = angular.element(`<div class="md-step-body-overlay"></div>
                            <div class="md-step-body-loading">
                                <md-progress-circular md-mode="indeterminate"></md-progress-circular>
                            </div>`);
                        $compile(overlay)(scope);
                        iElement.find('.md-steppers-scope').append(overlay);
                    }
                }

                scope.$ctrl.$stepper = stepperCtrl;
                scope.$watch(function () {
                    return scope.$ctrl.isActive();
                }, function (isActive) {
                    if (isActive) {
                        iElement.addClass('md-active');
                        addOverlay();
                    } else {
                        iElement.removeClass('md-active');
                    }
                });
            },
            templateUrl: 'mdSteppers/mdStep.tpl.html'
        };
    }])

    .config(['$mdIconProvider', ($mdIconProvider) => {
        $mdIconProvider.icon('steppers-check', 'mdSteppers/ic_check_24px.svg');
        $mdIconProvider.icon('steppers-warning', 'mdSteppers/ic_warning_24px.svg');
    }])
    .run(["$templateCache", function ($templateCache) {
        $templateCache.put("mdSteppers/ic_check_24px.svg", "<svg height=\"24\" viewBox=\"0 0 24 24\" width=\"24\" xmlns=\"http://www.w3.org/2000/svg\">\r\n    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>\r\n    <path d=\"M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z\"/>\r\n</svg>");
        $templateCache.put("mdSteppers/ic_warning_24px.svg", "<svg height=\"24\" viewBox=\"0 0 24 24\" width=\"24\" xmlns=\"http://www.w3.org/2000/svg\">\r\n    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>\r\n    <path d=\"M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z\"/>\r\n</svg>");
    }]);

