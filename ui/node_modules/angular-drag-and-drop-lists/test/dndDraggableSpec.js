describe('dndDraggable', function() {

  var SIMPLE_HTML = '<div dnd-draggable="{hello: \'world\'}"></div>';

  describe('constructor', function() {
    it('sets the draggable attribute', function() {
      var element = compileAndLink(SIMPLE_HTML);
      expect(element.attr('draggable')).toBe('true');
    });

    it('watches and handles the dnd-disabled-if expression', function() {
      var element = compileAndLink('<div dnd-draggable dnd-disable-if="disabled"></div>');
      expect(element.attr('draggable')).toBe('true');

      element.scope().disabled = true;
      element.scope().$digest();
      expect(element.attr('draggable')).toBe('false');

      element.scope().disabled = false;
      element.scope().$digest();
      expect(element.attr('draggable')).toBe('true');
    });
  });

  describe('dragstart handler', function() {
    var element, event;

    beforeEach(function() {
      element = compileAndLink(SIMPLE_HTML);
      event = createEvent('dragstart');
    });

    it('calls setData with serialized data', function() {
      event._triggerOn(element);
      expect(event._data).toEqual({'Text': '{"hello":"world"}'});
    });

    it('stops propagation', function() {
      event._triggerOn(element);
      expect(event._propagationStopped).toBe(true);
    });

    it('sets effectAllowed to move by default', function() {
      event._triggerOn(element);
      expect(event._dt.effectAllowed).toBe('move');
    });

    it('sets effectAllowed from dnd-effect-allowed', function() {
      element = compileAndLink('<div dnd-draggable dnd-effect-allowed="copyMove"></div>');
      event._triggerOn(element);
      expect(event._dt.effectAllowed).toBe('copyMove');
    });

    it('adds CSS classes to element', inject(function($timeout) {
      event._triggerOn(element);
      expect(element.hasClass('dndDragging')).toBe(true);
      expect(element.hasClass('dndDraggingSource')).toBe(false);

      $timeout.flush(0);
      expect(element.hasClass('dndDraggingSource')).toBe(true);
    }));

    it('invokes dnd-dragstart callback', function() {
      element = compileAndLink('<div dnd-draggable dnd-dragstart="ev = event"></div>');
      event._triggerOn(element);
      expect(element.scope().ev).toBe(event.originalEvent);
    });

    it('initializes workarounds', inject(function(dndDropEffectWorkaround, dndDragTypeWorkaround) {
      event._triggerOn(element);
      expect(dndDragTypeWorkaround.isDragging).toBe(true);
      expect(dndDragTypeWorkaround.dragType).toBeUndefined();
      expect(dndDropEffectWorkaround.dropEffect).toBe('none');
    }));

    it('initializes workarounds respecting dnd-type', inject(function(dndDragTypeWorkaround) {
      element = compileAndLink('<div dnd-draggable dnd-type="2 * 2"></div>');
      event._triggerOn(element);
      expect(dndDragTypeWorkaround.dragType).toEqual(4);
    }));

    it('does not start dragging if dnd-disable-if is true', function() {
      element = compileAndLink('<div dnd-draggable dnd-disable-if="true"></div>');
      expect(event._triggerOn(element)).toBe(true);
      expect(event._defaultPrevented).toBe(false);
      expect(event._propagationStopped).toBe(false);
    });

    it('sets the dragImage if event was triggered on a dnd-handle', function() {
      var dragImage;
      event._dt.setDragImage = function(img) { dragImage = img; };
      event.originalEvent._dndHandle = true;
      event._triggerOn(element);
      expect(dragImage).toBe(element[0]);
    });
  });

  describe('dragend handler', function() {
    var element, event;

    beforeEach(function() {
      element = compileAndLink(SIMPLE_HTML);
      event = createEvent('dragend');
    });

    it('stops propagation', function() {
      event._triggerOn(element);
      expect(event._propagationStopped).toBe(true);
    });

    it('removes CSS classes from element', inject(function($timeout) {
      element.addClass('dndDragging');
      element.addClass('dndDraggingSource');
      event._triggerOn(element);

      expect(element.hasClass('dndDragging')).toBe(false);
      expect(element.hasClass('dndDraggingSource')).toBe(true);

      $timeout.flush(0);
      expect(element.hasClass('dndDraggingSource')).toBe(false);
    }));

    it('resets workarounds', inject(function(dndDragTypeWorkaround) {
      event._triggerOn(element);
      expect(dndDragTypeWorkaround.isDragging).toBe(false);
    }));

    var dropEffects = {move: 'moved', copy: 'copied', none: 'canceled'};
    angular.forEach(dropEffects, function(callback, dropEffect) {
      it('calls callbacks for dropEffect ' + dropEffect, inject(function(dndDropEffectWorkaround) {
        var html = '<div dnd-draggable dnd-dragend="de = dropEffect" '
                 + 'dnd-' + callback + '="ev = event"></div>';
        element = compileAndLink(html);
        dndDropEffectWorkaround.dropEffect = dropEffect;

        event._triggerOn(element);
        expect(element.scope().ev).toBe(event.originalEvent);
        expect(element.scope().de).toBe(dropEffect);
      }));
    });
  });

  describe('click handler', function() {
    it('does nothing if dnd-selected is not set', function() {
      var element = compileAndLink(SIMPLE_HTML);
      var event = createEvent('click');
      event._triggerOn(element);
      expect(event._propagationStopped).toBe(false);
    });

    it('invokes dnd-selected callback and stops propagation', function() {
      var element = compileAndLink('<div dnd-draggable dnd-selected="selected = true"></div>');
      var event = createEvent('click');
      event._triggerOn(element);
      expect(event._propagationStopped).toBe(true);
      expect(element.scope().selected).toBe(true);
    });
  });
});
