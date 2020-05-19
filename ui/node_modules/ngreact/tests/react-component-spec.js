'use strict';

// phantom doesn't support Function.bind
require('es5-shim');
require('../ngReact');

var React = require( 'react' );
var ReactTestUtils = require( 'react-addons-test-utils' );
var ReactDOM = require( 'react-dom' );
var angular = require( 'angular' );
require( 'angular-mocks' );

var Hello = React.createClass({
  propTypes: {
    fname : React.PropTypes.string,
    lname : React.PropTypes.string,
    changeName: React.PropTypes.func
  },

  handleClick(){
    this.props.changeName();
  },

  render(){
    var {fname, lname} = this.props;
    return <div onClick={this.handleClick}>Hello {fname} {lname}</div>;
  }
});

describe('react-component', () => {

  var compileElement, provide;

  beforeEach(angular.mock.module('react'));

  beforeEach(angular.mock.module(($provide) => {provide = $provide;}));

  beforeEach(inject(($rootScope, $compile) => {
    compileElement = ( html, scope ) => {
      scope = scope || $rootScope;
      var elm = angular.element(html);
      $compile(elm)(scope);
      scope.$digest();
      return elm;
    };
  }));

  describe('creation', () => {

    beforeEach(() => {
      window.GlobalHello = Hello;
      window.Views = {
        Hello: Hello
      };
      provide.value('InjectedHello', Hello);
    });

    afterEach(() => {
      window.GlobalHello = undefined;
      window.Views = undefined;
    });

    it('should create global component with name', () => {
      var elm = compileElement( '<react-component name="GlobalHello"/>');
      expect(elm.text().trim()).toEqual('Hello');
    });

    it('should create global component with nested name', () => {
      var elm = compileElement( '<react-component name="Views.Hello"/>');
      expect(elm.text().trim()).toEqual('Hello');
    });

    it('should create injectable component with name', () => {
      var elm = compileElement( '<react-component name="InjectedHello"/>' );
      expect(elm.text().trim()).toEqual('Hello');
    });
  });

  describe('properties', () => {

    beforeEach(() => {
      provide.value('Hello', Hello);
    });

    it('should bind to properties on scope', inject(($rootScope) => {
      var scope = $rootScope.$new();
      scope.person = { fname: 'Clark', lname: 'Kent' };

      var elm = compileElement(
        '<react-component name="Hello" props="person"/>',
        scope
      );
      expect(elm.text().trim()).toEqual('Hello Clark Kent');
    }));

    it('should rerender when scope is updated', inject(($rootScope) => {

      var scope = $rootScope.$new();
      scope.person = { fname: 'Clark', lname: 'Kent' };

      var elm = compileElement(
        '<react-component name="Hello" props="person"/>',
        scope
      );

      expect(elm.text().trim()).toEqual('Hello Clark Kent');

      scope.person.fname = 'Bruce';
      scope.person.lname = 'Banner';
      scope.$apply();

      expect(elm.text().trim()).toEqual('Hello Bruce Banner');
    }));

    it('should accept callbacks on scope', inject(($rootScope) => {

      var scope = $rootScope.$new();
      scope.person = {
        fname: 'Clark', lname: 'Kent',
        changeName: () => {
          scope.person.fname = 'Bruce';
          scope.person.lname = 'Banner';
        }
      };

      var elm = compileElement(
        '<react-component name="Hello" props="person"/>',
        scope
      );
      expect(elm.text().trim()).toEqual('Hello Clark Kent');

      ReactTestUtils.Simulate.click( elm[0].firstChild );

      expect(elm.text().trim()).toEqual('Hello Bruce Banner');
    }));

    it('should scope.$apply() callback invocations made after changing props directly', inject(($rootScope) => {
      var scope = $rootScope.$new();
      scope.changeCount = 0;
      scope.person = {
        fname: 'Clark', lname: 'Kent',
        changeName: () => {
          scope.changeCount += 1;
        }
      };

      var template =
        `<div>
          <p>{{changeCount}}</p>
          <react-component name="Hello" props="person"/>
        </div>`;

      var elm = compileElement(template, scope);

      expect(elm.children().eq(0).text().trim()).toEqual('0');

      // first callback invocation
      ReactTestUtils.Simulate.click( elm[0].children.item(1).lastChild );

      expect(elm.children().eq(0).text().trim()).toEqual('1');

      // change props directly
      scope.person.fname = 'Peter';
      scope.$apply();

      expect(elm.children().eq(0).text().trim()).toEqual('1');

      // second callback invocation
      ReactTestUtils.Simulate.click( elm[0].children.item(1).lastChild );

      expect(elm.children().eq(0).text().trim()).toEqual('2');
    }));
  });

  describe('watch-depth', () => {

    describe('value', () => {
      var elm, scope;

      beforeEach(inject(($rootScope) => {
        provide.value('Hello', Hello);
        scope = $rootScope.$new();
        scope.person = { fname: 'Clark', lname: 'Kent' };
      }));

      it('should rerender when a property of scope object is updated', () => inject(() => {

        elm = compileElement(
            '<react-component name="Hello" props="person" watch-depth="value"/>',
            scope);

        expect(elm.text().trim()).toEqual('Hello Clark Kent');

        scope.person.fname = 'Bruce';
        scope.person.lname = 'Banner';
        scope.$apply();

        expect(elm.text().trim()).toEqual('Hello Bruce Banner');
      }));

      it('should rerender when a property of scope object is updated', () => inject(() => {

        //watch-depth will default to value
        elm = compileElement(
            '<react-component name="Hello" props="person" watch-depth="blahblah"/>',
            scope);

        expect(elm.text().trim()).toEqual('Hello Clark Kent');

        scope.person.fname = 'Bruce';
        scope.person.lname = 'Banner';
        scope.$apply();

        expect(elm.text().trim()).toEqual('Hello Bruce Banner');
      }));
    });

    describe('reference', () => {
      var elm, scope;

      beforeEach(inject(($rootScope) => {
        provide.value('Hello', Hello);
        scope = $rootScope.$new();
        scope.person = { fname: 'Clark', lname: 'Kent' };

        elm = compileElement(
            '<react-component name="Hello" props="person" watch-depth="reference"/>',
            scope);
      }));

      it('should rerender when scope object is updated', () => inject(() => {

        expect(elm.text().trim()).toEqual('Hello Clark Kent');

        scope.person = { fname: 'Bruce', lname: 'Banner' };
        scope.$apply();

        expect(elm.text().trim()).toEqual('Hello Bruce Banner');
      }));

      it('should NOT rerender when a property of scope object is updated', () => inject(() => {

        expect(elm.text().trim()).toEqual('Hello Clark Kent');

        scope.person.fname = 'Bruce';
        scope.person.lname = 'Banner';
        scope.$apply();

        expect(elm.text().trim()).toEqual('Hello Clark Kent');
      }));
    });

  });

  describe('destruction', () => {

    beforeEach(() => {
      provide.value('Hello', Hello);
    });

    it('should unmount component when scope is destroyed', inject(($rootScope) => {

      var scope = $rootScope.$new();
      var elm = compileElement(
        '<react-component name="Hello" props="person"/>',
        scope
      );
      scope.$destroy();

      //unmountComponentAtNode returns:
      // * true if a component was unmounted and
      // * false if there was no component to unmount.
      expect(ReactDOM.unmountComponentAtNode(elm[0])).toEqual(false);
    }));
  });

  describe('deferred destruction', function() {

    beforeEach(() => {
      provide.value('Hello', Hello);
    });

    it('should not unmount component when scope is destroyed', inject(($rootScope) => {
      var scope = $rootScope.$new();
      scope.person = { firstName: 'Clark', lastName: 'Kent' };
      scope.callback = jasmine.createSpy('callback');

      var elm = compileElement(
        '<react-component name="Hello" props="person" on-scope-destroy="callback()"/>',
        scope
      );
      scope.$destroy();

      //unmountComponentAtNode returns:
      // * true if a component was unmounted and
      // * false if there was no component to unmount.
      expect(ReactDOM.unmountComponentAtNode(elm[0])).toEqual(true);

      expect(scope.callback.calls.count()).toEqual(1);
    }));

    it('should pass unmount function as a "unmountComponent" parameter to callback', inject(($rootScope) => {
      var scope = $rootScope.$new();
      scope.person = { firstName: 'Clark', lastName: 'Kent' };
      scope.callback = function(unmountFn) {
        unmountFn();
      };
      
      spyOn(scope, 'callback').and.callThrough();

      var elm = compileElement(
        '<react-component name="Hello" props="person" on-scope-destroy="callback(unmountComponent)"/>',
        scope
      );
      scope.$destroy();
      //unmountComponentAtNode returns:
      // * true if a component was unmounted and
      // * false if there was no component to unmount.
      expect(ReactDOM.unmountComponentAtNode(elm[0])).toEqual(false);

      expect(scope.callback.calls.count()).toEqual(1);
    }));
  });
});