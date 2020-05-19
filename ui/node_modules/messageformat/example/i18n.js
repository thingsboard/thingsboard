var en = function (n, ord) {
  var s = String(n).split('.'), v0 = !s[1], t0 = Number(s[0]) == n,
      n10 = t0 && s[0].slice(-1), n100 = t0 && s[0].slice(-2);
  if (ord) return (n10 == 1 && n100 != 11) ? 'one'
      : (n10 == 2 && n100 != 12) ? 'two'
      : (n10 == 3 && n100 != 13) ? 'few'
      : 'other';
  return (n == 1 && v0) ? 'one' : 'other';
};
var fr = function (n, ord) {
  if (ord) return (n == 1) ? 'one' : 'other';
  return (n >= 0 && n < 2) ? 'one' : 'other';
};
var number = function (value, name, offset) {
  if (!offset) return value;
  if (isNaN(value)) throw new Error('Can\'t apply offset:' + offset + ' to argument `' + name +
                                    '` with non-numerical value ' + JSON.stringify(value) + '.');
  return value - offset;
};
var plural = function (value, offset, lcfunc, data, isOrdinal) {
  if ({}.hasOwnProperty.call(data, value)) return data[value];
  if (offset) value -= offset;
  var key = lcfunc(value, isOrdinal);
  if (key in data) return data[key];
  return data.other;
};

(function (root, G) {
  if (typeof define === "function" && define.amd) { define(G); }
  else if (typeof exports === "object") { module.exports = G; }
  else { root.i18n = G; }
})(this, {
  en: {
    colors: {
      red: function(d) { return "red"; },
      blue: function(d) { return "blue"; },
      green: function(d) { return "green"; }
    },
    sub: {
      folder: {
        plural: {
          test: function(d) { return "Your " + plural(d.NUM, 0, en, { one: "message goes", other: number(d.NUM, "NUM") + " messages go" }) + " here."; }
        }
      }
    }
  },
  fr: {
    colors: {
      red: function(d) { return "rouge"; },
      blue: function(d) { return "bleu"; },
      green: function(d) { return "vert"; }
    },
    sub: {
      folder: {
        plural: {
          test: function(d) { return plural(d.NUM, 0, fr, { one: "Votre message se trouve", other: "Vos " + number(d.NUM, "NUM") + " messages se trouvent" }) + " ici."; }
        }
      }
    }
  }
});
