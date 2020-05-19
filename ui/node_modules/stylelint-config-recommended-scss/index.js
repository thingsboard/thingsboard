"use strict";

module.exports = {
  extends: ["stylelint-config-recommended"],
  plugins: ["stylelint-scss"],
  rules: {
    "at-rule-no-unknown": null,
    "scss/at-rule-no-unknown": true
  }
};
