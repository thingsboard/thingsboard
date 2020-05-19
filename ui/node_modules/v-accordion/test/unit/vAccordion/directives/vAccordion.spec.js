describe('vAccordion', function () {

  var $compile;
  var $rootScope;
  var accordionConfig;
  var scope;

  var generateTemplate = function (options) {
    var dafaults = {
      attributes: '',
      content: ''
    };

    if (options) {
      angular.extend(dafaults, options);
    }

    var template = '<v-accordion ' + dafaults.attributes + '>\n';
        template += dafaults.content + '\n';
        template += '</v-accordion>';

    return template;
  };



  beforeEach(module('vAccordion'));

  beforeEach(inject(function (_$rootScope_, _$compile_, _accordionConfig_) {
    $rootScope = _$rootScope_;
    scope = $rootScope.$new();
    $compile = _$compile_;
    accordionConfig = _accordionConfig_;
  }));

  afterEach(function () {
    scope.$destroy();
  });



  it('should transclude scope', function () {
    var message = 'Hello World!';

    var template = generateTemplate({ content: '{{ message }}' });
    var accordion = $compile(template)(scope);

    scope.message = message;
    scope.$digest();

    expect(accordion.html()).toContain(message);
  });


  it('should allow multiple selections to be expanded if `multiple` attribute is defined', function () {
    var options = { attributes: 'multiple' };
    var template = generateTemplate(options);
    var accordion = $compile(template)(scope);

    expect(accordion.isolateScope().allowMultiple).toBe(true);
  });


  it('should add the ARIA `tablist` role', function () {
    var template = generateTemplate();
    var accordion = $compile(template)(scope);

    expect(accordion.attr('role')).toBe('tablist');
  });


  it('should set `aria-multiselectable` attribute to `true` if `multiple` attribute is defined', function () {
    var options = { attributes: 'multiple' };
    var template = generateTemplate(options);
    var accordion = $compile(template)(scope);

    expect(accordion.attr('aria-multiselectable')).toBeDefined();
  });


  it('should extend custom control object', function () {
    scope.control = { someProperty: 'test' };

    var options = { attributes: 'control="control"' };
    var template = generateTemplate(options);
    var accordion = $compile(template)(scope);

    expect(accordion.isolateScope().internalControl.someProperty).toBeDefined();
    expect(accordion.isolateScope().internalControl.someProperty).toBe('test');
  });


  it('should throw an error if the API method is overriden', function () {
    scope.control = { toggle: function () {} };

    var options = { attributes: 'control="control"' };
    var template = generateTemplate(options);

    expect(function () { $compile(template)(scope); }).toThrow();
  });


  it('should set accordion `internalControl` as `$accordion` property on transcluded scope', function () {
    var options = {
      attributes: 'id="accordion"',
      content: '<v-pane><v-pane-header></v-pane-header><v-pane-content></v-pane-content></v-pane>'
    };

    var template = generateTemplate(options);
    var accordion = $compile(template)(scope);
    var pane = accordion.find('v-pane');
    var transcludedScope = pane.scope();

    expect(scope.$accordion).not.toBeDefined();
    expect(transcludedScope.$accordion).toBeDefined();
    expect(transcludedScope.$accordion.id).toEqual('accordion');
    expect(transcludedScope.$accordion.toggle).toBeDefined();
    expect(transcludedScope.$accordion.expand).toBeDefined();
    expect(transcludedScope.$accordion.collapse).toBeDefined();
    expect(transcludedScope.$accordion.expandAll).toBeDefined();
    expect(transcludedScope.$accordion.collapseAll).toBeDefined();
    expect(transcludedScope.$accordion.hasExpandedPane).toBeDefined();
  });

});
