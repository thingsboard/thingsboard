describe('indexof', function () {
  var indexof = require('indexof')
    , assert = require('component-assert');

  it('handles arrays', function () {
    var array = ['a', 'b', 'c'];
    assert(2 == indexof(array, 'c'));
  });

  it('handles node lists', function () {
    var ul = document.querySelector('ul');
    var lis = ul.querySelectorAll('li');
    var li = lis[2];
    assert(2 == indexof(lis, li));
  });
});