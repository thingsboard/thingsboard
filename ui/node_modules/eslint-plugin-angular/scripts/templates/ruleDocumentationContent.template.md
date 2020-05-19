<!-- WARNING: Generated documentation. Edit docs and examples in the rule and examples file ('<%= sourcePath %>', '<%= examplesPath %>'). -->

# <%= ruleName %> - <%= lead %>

<% if(deprecated) { %>
**This rule is deprecated and will be removed in future versions. Explanation: <%= deprecationReason %>**
<% } %>

<%= description %>

<% if(sinceAngularVersion) { %>
**Rule based on Angular <%= sinceAngularVersion %>**
<% } %>

<% if(styleguideReferences.length > 0) { %>
**Styleguide Reference**
<% _.each(styleguideReferences, function(styleRef) { %>
* <%= formatStyleguideReference(styleRef) %><% }); } %>

<% if(groupedExamples.length > 0) { %>
## Examples

<% _.each(groupedExamples, function (exampleGroup) { %>
<% if(exampleGroup.config) { %>
The following patterns are <%= exampleGroup.valid ? '**not** ' : '' %>considered problems when configured <%= formatConfigAsMarkdown(exampleGroup.examples) %>:
<% } else { %>
The following patterns are <%= exampleGroup.valid ? '**not** ' : '' %>considered problems<%= hasOnlyOneConfig ? '' : ' with default config'%>;
<% } %>
    /*eslint angular/<%= ruleName %>: <%= formatConfigAsJson(exampleGroup.examples) %>*/
    <% _.each(exampleGroup.examples, function (example) { %>
    // <%= example.valid ? 'valid' : 'invalid' %> <%= example.filename ? 'with filename: ' + example.filename : '' %>
    <%= indent(example.code, 4) %> <%= example.errorMessage ? '// error: ' + example.errorMessage : '' %>
    <%= example.errorMessages ? '// error: ' + example.errorMessages.join(', ') : '' %>
<% }) %>
<% }) %>
<% } %>

## Version

This rule was introduced in eslint-plugin-angular <%= version %>

## Links

* [Rule source](/<%= sourcePath %>)
* [Example source](/<%= examplesPath %>)
