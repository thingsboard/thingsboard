module.exports = {
  scripts: {
    production: [
      'lib/app.js'
    ],
    thirdParty: [
      'node_modules/react/dist/react-with-addons.js',
      'node_modules/angular/angular.js',
      'node_modules/angular-mocks/angular-mocks.js',
      'node_modules/ngreact/ngReact.js',
      'node_modules/react-dom/dist/react-dom.js'
    ],
    tests: [
      'test/*-service.spec.js',
      'test/*-controller.spec.js',
      'test/*-component.spec.js',
      'test/*-directive.spec.js'
    ]
  }
};
