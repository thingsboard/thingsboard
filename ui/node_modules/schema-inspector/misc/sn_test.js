if (typeof require === 'function') {
  var SchemaInspector = require('../');
}

(function (SchemaInspector, $) {

  var display = typeof alert === 'function' ? alert : function (o) {
    return console.log(require('util').inspect(o, true, null));
  };

  function format(json) {
    return JSON.stringify(json, null, 2)
    .replace(/\n/g, '<br>')
    .replace(/ /g, '&nbsp;&nbsp;')
    .replace(/("[^"]*"):/g, '<font color="blue">$1</font>:')
    .replace(/:(&nbsp;)+("[^"]*")(,?)/g, ':$1<font color="green">$2</font>$3')
    .replace(/:(&nbsp;)+(\d+)(,?)/g, ':$1<font color="violet">$2</font>$3')
    .replace(/:(&nbsp;)+(true|false|null)(,?)/g, ':$1<font color="red">$2</font>$3')
    .replace(/:(&nbsp;)+(undefined)(,?)/g, ':$1<font color="grey">$2</font>$3');
  }

  // ---------------------------------------------------------------------------

  var schema = {
    type: 'object',
    properties: {
      json: { type: 'string', rules: 'title' }
    }
  };

  var obj = {
    json: 'coucOu TouT le moNDe'
  };

  // ---------------------------------------------------------------------------

  var r = SchemaInspector.sanitize(schema, obj);

  var done = function () {
    var html = '<p>'
      + 'Sanitization = '
      + format(r)
      + '</p>';
    $('div.resultSanitization').html(html);
  };

  if ($ !== null) {
    $(done);
  }
  else {
    console.log(obj);
    console.log(r);
    console.log('format:', r.format());
  }
}).call(this, SchemaInspector, typeof jQuery !== 'undefined' ? jQuery : null);