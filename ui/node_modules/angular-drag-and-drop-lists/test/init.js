var $compile,
    $rootScope;

beforeEach(module('dndLists'));

beforeEach(inject(function(_$compile_, _$rootScope_){
  $compile = _$compile_;
  $rootScope = _$rootScope_;
}));

function compileAndLink(html) {
  var scope = $rootScope.$new();
  var element = $compile(html)(scope);
  scope.$digest();
  return element;
}

function createEvent(type, dataTransfer) {
  var dataTransfer = dataTransfer || {};
  var event = {
    originalEvent: {
      dataTransfer: dataTransfer,
      preventDefault: function() {
        event._defaultPrevented = true;
      },
      stopPropagation: function() {
        event._propagationStopped = true;
      },
      type: type,
    },
    _data: {},
    _defaultPrevented: false,
    _dt: dataTransfer,
    _propagationStopped: false,
    _triggerOn: function(element) {
      // Retrieve event handlers from jQuery and invoke.
      return $._data($(element).get(0), "events")[type][0].handler(event);
    },
  };

  switch (type) {
    case 'dragstart':
      dataTransfer.setData = function(type, value) {
        event._data[type] = value;
      };
      break;
    case 'drop':
      dataTransfer.getData = function(type) { return event._data[type]; };
      // continue
    case 'dragover':
      event._data['image/jpeg'] = '???';
      event._data['text/plain'] = '{"example":"data"}';
      dataTransfer.types = ["image/jpeg", "text/plain"];
      dataTransfer.effectAllowed = 'move';
      dataTransfer.dropEffect = 'move';
      break;
  }

  return event;
}
