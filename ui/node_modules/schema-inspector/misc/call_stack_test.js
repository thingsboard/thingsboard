if (typeof require === 'function') {
  var inspector = require('../');
}

var task = {  inputs: [
    {
      trash: 'fsdfsf',
      inputOptions: ['-flags +loop+mv4', '-cmp +chroma', '-partitions +parti4x4+parti8x8+partp4x4+partp8x8+partb8x8',
        '-coder 0', '-me_method hex', '-subq 7', '-me_range 16', '-bf 0', '-keyint_min 25', '-sc_threshold 40', '-i_qfactor 0.71',
        '-qmin 10', '-qmax 51', '-qdiff 4', '-trellis 1', '-level 30', '-refs 5', '-wpredp 0',
        '-coder 0', '-me_method hex', '-subq 7', '-me_range 16', '-bf 0', '-keyint_min 25', '-sc_threshold 40', '-i_qfactor 0.71',
        '-qmin 10', '-qmax 51', '-qdiff 4', '-trellis 1', '-level 30', '-refs 5', '-wpredp 0',
        '-coder 0', '-me_method hex', '-subq 7', '-me_range 16', '-bf 0', '-keyint_min 25', '-sc_threshold 40', '-i_qfactor 0.71',
        '-qmin 10', '-qmax 51', '-qdiff 4', '-trellis 1', '-level 30', '-refs 5', '-wpredp 0',
        '-coder 0', '-me_method hex', '-subq 7', 'last string and all crushed.'
      ]
    }
  ]
};

var schema = {
  type: "object",
  strict: true,
  properties: {
    inputs: {
      type: 'array',
      items: {
        type: 'object',
        optional: true,
        strict: true,
        properties: {
          inputOptions: {
            type: 'array',
            optional: true,
            items: {
              type: 'string',
              rules: ['trim']
            }
          }
        }
      }
    }
  }
};

function checkTaskParams() {
  inspector.sanitize(schema, task, function (err, sanitize) {
    console.log(sanitize);
    inspector.validate(schema, task, function (err, result) {
      console.log(result);
      console.log('Validator result', result);
      if (!result.valid) {
        console.log("Use propalo", {info: result.format()})
      } else {
        console.log("Ok");
        console.log(task.inputs[0].inputOptions);
      }
    });
  });
}

checkTaskParams();
