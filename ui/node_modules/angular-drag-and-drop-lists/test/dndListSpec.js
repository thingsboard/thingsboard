describe('dndList', function() {
  var dragTypeWorkaround,
      dropEffectWorkaround;

  beforeEach(inject(function(dndDropEffectWorkaround, dndDragTypeWorkaround) {
    dragTypeWorkaround = dndDragTypeWorkaround;
    dropEffectWorkaround = dndDropEffectWorkaround;
    // Initialise internal state by calling dragstart.
    createEvent('dragstart')._triggerOn(compileAndLink('<div dnd-draggable="{}"></div>'));
  }));

  describe('constructor', function() {
    it('hides the placeholder element', function() {
      var element = compileAndLink('<dnd-list><img class="dndPlaceholder"></dnd-list>');
      expect(element.children().length).toBe(0);
    });
  });

  describe('dragenter handler', function() {
    commonTests('dragenter');
  });

  describe('dragover handler', function() {
    commonTests('dragover');

    var element, event;

    beforeEach(function() {
      element = compileAndLink('<div dnd-list="list"></div>');
      element.scope().list = [];
      event = createEvent('dragover');
      event.originalEvent.target = element[0];
    });

    it('adds dndDragover CSS class', function() {
      verifyDropAllowed(element, event);
      expect(element.hasClass('dndDragover')).toBe(true);
    });

    it('adds placeholder element', function() {
      verifyDropAllowed(element, event);
      expect(element.children().length).toBe(1);
      expect(element.children()[0].tagName).toBe('LI');
    });

    it('reuses custom placeholder element if it exists', function() {
      element = compileAndLink('<dnd-list><img class="dndPlaceholder"></dnd-list>');
      verifyDropAllowed(element, event);
      expect(element.children().length).toBe(1);
      expect(element.children()[0].tagName).toBe('IMG');
    });

    it('invokes dnd-dragover callback', function() {
      element = createListWithItemsAndCallbacks();
      verifyDropAllowed(element, event);
      expect(element.scope().dragover.event).toBe(event.originalEvent);
      expect(element.scope().dragover.index).toBe(3);
      expect(element.scope().dragover.external).toBe(false);
      expect(element.scope().dragover.type).toBeUndefined();
      expect(element.scope().dragover.item).toBeUndefined();
    });

    it('invokes dnd-dragover callback with correct type', function() {
      element = createListWithItemsAndCallbacks();
      dragTypeWorkaround.dragType = 'mytype';
      verifyDropAllowed(element, event);
      expect(element.scope().dragover.type).toBe('mytype');
    });

    it('invokes dnd-dragover callback for external elements', function() {
      element = createListWithItemsAndCallbacks();
      dragTypeWorkaround.isDragging = undefined;
      verifyDropAllowed(element, event);
      expect(element.scope().dragover.external).toBe(true);
    });

    it('dnd-dragover callback can cancel the drop', function() {
      element = compileAndLink('<div dnd-list="list" dnd-dragover="false"></div>');
      verifyDropDisallowed(element, event);
    });

    describe('placeholder positioning (vertical)', positioningTests(false, false));
    describe('placeholder positioning (vertical, IE)', positioningTests(false, true));
    describe('placeholder positioning (horizontal)', positioningTests(true, false));
    describe('placeholder positioning (horizontal, IE)', positioningTests(true, true));

    function positioningTests(horizontal, relative) {
      return function() {
        var offsetYField = (relative ? 'layer' : 'offset') + (horizontal ? 'X' : 'Y');
        var offsetHeightField = 'offset' + (horizontal ? 'Width' : 'Height');
        var offsetTopField = 'offset' + (horizontal ? 'Left' : 'Top');

        beforeEach(function() {
          element = createListWithItemsAndCallbacks(horizontal);
          angular.element(document.body).append(element);
          if (horizontal) {
            element.children().css('float','left');
          }
        });

        afterEach(function() {
          element.remove();
        });

        it('adds actual placeholder element', function() {
          event.originalEvent.target = element.children()[0];
          event.originalEvent[offsetYField] = 1;
          verifyDropAllowed(element, event);
          expect(element.scope().dragover.index).toBe(0);
          expect(angular.element(element.children()[0]).hasClass('dndPlaceholder')).toBe(true);
        });

        it('inserts before element if mouse is in first half', function() {
          event.originalEvent.target = element.children()[1];
          event.originalEvent[offsetYField] = event.originalEvent.target[offsetHeightField] / 2 - 1;
          if (relative) {
            event.originalEvent[offsetYField] += event.originalEvent.target[offsetTopField];
            event.originalEvent.target = element[0];
          }
          verifyDropAllowed(element, event);
          expect(element.scope().dragover.index).toBe(1);
        });

        it('inserts after element if mouse is in second half', function() {
          event.originalEvent.target = element.children()[1];
          event.originalEvent[offsetYField] = event.originalEvent.target[offsetHeightField] / 2 + 1;
          if (relative) {
            event.originalEvent[offsetYField] += event.originalEvent.target[offsetTopField];
            event.originalEvent.target = element[0];
          }
          verifyDropAllowed(element, event);
          expect(element.scope().dragover.index).toBe(2);
        });
      };
    }
  });

  describe('drop handler', function() {
    commonTests('drop');

    var element, dragoverEvent, dropEvent;

    beforeEach(function() {
      element = createListWithItemsAndCallbacks();
      dragoverEvent = createEvent('dragover');
      dragoverEvent.originalEvent.target = element.children()[0];
      dragoverEvent._triggerOn(element);
      dropEvent = createEvent('drop');
    });

    it('inserts into the list and removes dndDragover class', function() {
      expect(element.hasClass("dndDragover")).toBe(true);
      verifyDropAllowed(element, dropEvent);
      expect(element.scope().list).toEqual([1, {example: 'data'}, 2, 3]);
      expect(element.hasClass("dndDragover")).toBe(false);
      expect(element.children().length).toBe(3);
    });

    it('inserts in correct position', function() {
      dragoverEvent.originalEvent.target = element.children()[2];
      dragoverEvent._triggerOn(element);
      verifyDropAllowed(element, dropEvent);
      expect(element.scope().list).toEqual([1, 2, {example: 'data'}, 3]);
      expect(element.scope().inserted.index).toBe(2);
    });

    it('invokes the dnd-inserted callback', function() {
      verifyDropAllowed(element, dropEvent);
      expect(element.scope().inserted.event).toBe(dropEvent.originalEvent);
      expect(element.scope().inserted.index).toBe(1);
      expect(element.scope().inserted.external).toBe(false);
      expect(element.scope().inserted.type).toBeUndefined();
      expect(element.scope().inserted.item).toBe(element.scope().list[1]);
    });

    it('dnd-drop can transform the object', function() {
      var testObject = {transformed: true};
      element.scope().dropHandler = function(params) {
        expect(params.event).toBe(dropEvent.originalEvent);
        expect(params.index).toBe(1);
        expect(params.external).toBe(false);
        expect(params.type).toBeUndefined();
        expect(params.item).toEqual({example: 'data'});
        return testObject;
      };
      verifyDropAllowed(element, dropEvent);
      expect(element.scope().list[1]).toBe(testObject);
    });

    it('dnd-drop can cancel the drop', function() {
      element.scope().dropHandler = function() { return false; };
      expect(dropEvent._triggerOn(element)).toBe(true);
      expect(dropEvent._defaultPrevented).toBe(true);
      expect(element.scope().list).toEqual([1, 2, 3]);
      expect(element.scope().inserted).toBeUndefined();
      verifyDragoverStopped(element, dropEvent, 3);
    });

    it('dnd-drop can take care of inserting the element', function() {
      element.scope().dropHandler = function() { return true; };
      verifyDropAllowed(element, dropEvent);
      expect(element.scope().list).toEqual([1, 2, 3]);
    });

    it('invokes callbacks with correct type', function() {
      dragTypeWorkaround.dragType = 'mytype';
      verifyDropAllowed(element, dropEvent);
      expect(element.scope().drop.type).toBe('mytype');
      expect(element.scope().inserted.type).toBe('mytype');
    });

    it('invokes callbacks for external elements', function() {
      dragTypeWorkaround.isDragging = undefined;
      verifyDropAllowed(element, dropEvent);
      expect(element.scope().drop.external).toBe(true);
      expect(element.scope().inserted.external).toBe(true);
    });

    it('can handle Text mime type', function() {
      dropEvent._data = {'Text': '{"lorem":"ipsum"}'};
      dropEvent._dt.types = ['Text'];
      verifyDropAllowed(element, dropEvent);
      expect(element.scope().list[1]).toEqual({lorem: 'ipsum'});
    });

    it('cancels drop when JSON is invalid', function() {
      dropEvent._data = {'text/plain': 'Lorem ipsum'};
      dropEvent._dt.types = ['Text'];
      expect(dropEvent._triggerOn(element)).toBe(true);
      expect(dropEvent._defaultPrevented).toBe(true);
      verifyDragoverStopped(element, dropEvent, 3);
    });

    describe('dropEffect calculation', function() {
      testDropEffect('move', 'move');
      testDropEffect('blub', 'blub');
      testDropEffect('copy', 'none', 'copy');
      testDropEffect('move', 'none', 'move');
      testDropEffect('move', 'none', 'link');
      testDropEffect('copy', 'none', 'link', true);

      function testDropEffect(expected, dropEffect, effectAllowed, ctrlKey) {
        it('stores ' + expected + ' for ' + [dropEffect, effectAllowed, ctrlKey], function() {
          dropEvent._dt.dropEffect = dropEffect;
          dropEvent._dt.effectAllowed = effectAllowed;
          dropEvent.originalEvent.ctrlKey = ctrlKey;
          verifyDropAllowed(element, dropEvent);
          expect(dropEffectWorkaround.dropEffect).toBe(expected);
        });
      }
    });
  });

  describe('dragleave handler', function() {
    var element, event;

    beforeEach(function() {
      element = createListWithItemsAndCallbacks();
      event = createEvent('dragover');
      event.originalEvent.target = element[0];
      event._triggerOn(element);
    });

    it('removes the dndDragover CSS class', function() {
      expect(element.hasClass('dndDragover')).toBe(true);
      createEvent('dragleave')._triggerOn(element);
      expect(element.hasClass('dndDragover')).toBe(false);
    });

    it('removes the placeholder after a timeout', inject(function($timeout) {
      expect(element.children().length).toBe(4);
      createEvent('dragleave')._triggerOn(element);
      $timeout.flush(50);
      expect(element.children().length).toBe(4);
      $timeout.flush(50);
      expect(element.children().length).toBe(3);
    }));

    it('does not remove the placeholder if dndDragover was set again', inject(function($timeout) {
      createEvent('dragleave')._triggerOn(element);
      element.addClass('dndDragover');
      $timeout.flush(1000);
      expect(element.children().length).toBe(4);
    }));
  });

  function commonTests(eventType) {
    describe('(common tests)', function() {
      var element, event;

      beforeEach(function() {
        element = compileAndLink('<div dnd-list="[]"></div>');
        event = createEvent(eventType);
        event.originalEvent.target = element[0];
      });

      it('disallows dropping from external sources', function() {
        dragTypeWorkaround.isDragging = false;
        verifyDropDisallowed(element, event);
      });

      it('allows dropping from external sources if dnd-external-sources is set', function() {
        element = compileAndLink('<div dnd-list="[]" dnd-external-sources="true"></div>');
        dragTypeWorkaround.isDragging = false;
        verifyDropAllowed(element, event);
      });

      it('disallows mimetypes other than text', function() {
        event._dt.types = ['text/html'];
        verifyDropDisallowed(element, event);
      });

      it('allows drop if dataTransfer.types contains "Text"', function() {
        event._dt.types = ['image/jpeg', 'Text'];
        verifyDropAllowed(element, event);
      });

      // Old Internet Explorer versions don't have dataTransfer.types.
      it('allows drop if dataTransfer.types is undefined', function() {
        event._dt.types = undefined;
        verifyDropAllowed(element, event);
      });

      it('disallows dropping if dnd-disable-if is true', function() {
        element = compileAndLink('<div dnd-list="[]" dnd-disable-if="disabled"></div>');
        element.scope().disabled = true;
        verifyDropDisallowed(element, event);
      });

      it('allows drop if dnd-disable-if is false', function() {
        element = compileAndLink('<div dnd-list="[]" dnd-disable-if="disabled"></div>');
        verifyDropAllowed(element, event);
      });

      it('disallows dropping untyped elements if dnd-allowed-types is set', function() {
        element = compileAndLink('<div dnd-list="[]" dnd-allowed-types="[\'mytype\']"></div>');
        verifyDropDisallowed(element, event);
      });

      it('disallows dropping elements of the wrong type if dnd-allowed-types is set', function() {
        element = compileAndLink('<div dnd-list="[]" dnd-allowed-types="[\'mytype\']"></div>');
        dragTypeWorkaround.dragType = 'othertype';
        verifyDropDisallowed(element, event);
      });

      it('allows dropping elements of the correct type if dnd-allowed-types is set', function() {
        element = compileAndLink('<div dnd-list="[]" dnd-allowed-types="[\'mytype\']"></div>');
        dragTypeWorkaround.dragType = 'mytype';
        verifyDropAllowed(element, event);
      });

      it('allows dropping external elements even if dnd-allowed-types is set', function() {
        element = compileAndLink('<div dnd-list="[]" dnd-allowed-types="[\'mytype\']" ' +
                                 'dnd-external-sources="true"></div>');
        dragTypeWorkaround.isDragging = false;
        verifyDropAllowed(element, event);
      });
    });
  }

  function verifyDropAllowed(element, event) {
    if (event.originalEvent.type == 'dragenter') {
      expect(event._triggerOn(element)).toBeUndefined();
      expect(event._propagationStopped).toBe(false);
    } else {
      expect(event._triggerOn(element)).toBe(false);
      expect(event._propagationStopped).toBe(true);
    }
    expect(event._defaultPrevented).toBe(true);
  }

  function verifyDropDisallowed(element, event) {
    expect(event._triggerOn(element)).toBe(true);
    expect(event._defaultPrevented).toBe(false);
    verifyDragoverStopped(element, event);
  }

  function verifyDragoverStopped(element, event, children) {
    expect(element.hasClass("dndDragover")).toBe(false);
    expect(element.children().length).toBe(children || 0);
    expect(event._propagationStopped).toBe(false);
  }

  function createListWithItemsAndCallbacks(horizontal) {
    var params = '{event: event, index: index, item: item, external: external, type: type}';
    var element = compileAndLink('<ul dnd-list="list" dnd-external-sources="true" ' +
                  'dnd-horizontal-list="' + (horizontal || 'false') + '" ' +
                  'dnd-dragover="dragover = ' + params + '" ' +
                  'dnd-drop="dropHandler(' + params + ')" ' +
                  'dnd-inserted="inserted = ' + params + '">' +
                  '<li>A</li><li>B</li><li>C</li></ul>');
    element.scope().dropHandler = function(params) {
      element.scope().drop = params;
      return params.item;
    };
    element.scope().list = [1, 2, 3];
    element.css('position', 'relative');
    return element;
  }
});
