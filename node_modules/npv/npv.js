var path = require('path')

module.exports = function (keys, targetDir) {
  if (keys == null || keys.length < 1) {
    keys = ['version']
  }
  
  if (targetDir == null) {
    targetDir = path.dirname(module.parent.filename)
  }

  var targetJson = require(path.resolve(targetDir, 'package.json'))

  var result = {}
  for (var i = 0; i < keys.length; i++) {
    var key = keys[i]
    result[key] = targetJson.hasOwnProperty(key) ? targetJson[key] : null
  }
  return result
}
