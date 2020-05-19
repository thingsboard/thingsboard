## Rules

Rules in eslint-plugin-angular are divided into several categories to help you better understand their value.

<% _.each(categoryOrder, function (categoryName) { %>
### <%= categories[categoryName].headline %>

<%= categories[categoryName].description %>
<% _.each(rulesByCategory[categoryName], function (rule) { %>
 * [<%= rule.ruleName %>](<%= rule.documentationPath %>) - <%= rule.linkDescription %><%= formatStyleguideReferenceListShort(rule) %><% }) %>
<% }) %>

----
