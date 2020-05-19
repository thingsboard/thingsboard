var container, element, content;

QUnit.module('main', {
  setup: function() {
    var fixture = '<div id="test-playground"><div id="container"><div id="resizable-element"><div id="content"></div></div></div></div>';
    $("#qunit-fixture").append(fixture);
    
    container = document.getElementById('container');
    element = document.getElementById('resizable-element');
    content = document.getElementById('content');

    $('#container').hide();
    addResizeListener(element, detectCallback);
    $('#container').show();
    shouldDetect = true;
    detected = false;
  },
  teardown: function() {
    $('#styleTest').remove();
    try {
      removeResizeListener(element, detectCallback);
    } catch(e) {}
  }
});

var newWidth = 0, newHeight = 0, shouldDetect = true, detected = false;
var detectCallback = function() {
  detected = true;
};

var validateEvent = function(assert) {
  setTimeout(function() {
    if(shouldDetect) {
      assert.ok(shouldDetect === true && detected === true, 'resize event fired OK');
    }
    assert.ok($(content).width() == newWidth, 'Resize OK');
    
    QUnit.start();
  }, 2000);
};

QUnit.asyncTest( "JS addResizeListener css resize test", function( assert ) {
  expect( 2 );

  newWidth = 100;

  var myCss = '<style id="styleTest">#content {width: ' + newWidth + 'px;}</style>';
  $('head').append(myCss);

  validateEvent(assert);
});

QUnit.asyncTest( "JS addResizeListener script resize test", function( assert ) {
  expect( 2 );

  newWidth = 30;

  $(content).width(newWidth);

  validateEvent(assert);
});

QUnit.asyncTest( "JS addResizeListener script reattach element test", function( assert ) {
  expect( 2 );
  
  var elem = $(content).detach();
  
  setTimeout(function() {
    $(container).append("div").append(elem);
    //elem.appendTo(container);
    newWidth = 68;
    $(content).width(newWidth);
  }, 500);

  validateEvent(assert);
});

QUnit.asyncTest( "JS removeResizeListener test", function( assert ) {
  expect( 1 );
  
  newWidth = 0;
  shouldDetect = false;

  removeResizeListener(element, detectCallback);
  
  $(content).width(newWidth);
  $(content).height(0);
  
  validateEvent(assert);
});