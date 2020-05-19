module.exports = get_blob()

function get_blob() {
  if(global.Blob) {
    try {
      new Blob(['asdf'], {type: 'text/plain'})
      return Blob
    } catch(err) {}
  }

  var Builder = global.WebKitBlobBuilder ||
                global.MozBlobBuilder ||
                global.MSBlobBuilder

  return function(parts, bag) {
    var builder = new Builder
      , endings = bag.endings
      , type = bag.type

    if(endings) for(var i = 0, len = parts.length; i < len; ++i) {
      builder.append(parts[i], endings)
    } else for(var i = 0, len = parts.length; i < len; ++i) {
      builder.append(parts[i])
    }

    return type ? builder.getBlob(type) : builder.getBlob()
  }
}
