# react-schema-form

[![Join the chat at https://gitter.im/networknt/react-schema-form](https://badges.gitter.im/networknt/react-schema-form.svg)](https://gitter.im/networknt/react-schema-form?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![npm package](https://img.shields.io/npm/v/react-schema-form.svg?style=flat-square)](https://www.npmjs.org/package/react-schema-form)

[React](http://facebook.github.io/react/) forms based on json schema for form generation and validation. This is a port of the [angular schema form](https://github.com/Textalk/angular-schema-form) project using
[material-ui](http://www.material-ui.com/) for the underlying components.

# Live demo
[demo](http://networknt.github.io/react-schema-form/)

While you are trying the demo forms, you can update the schema and form in the json editor to see the instant re-rendered form. This is a way to build form interactively.

# Examples
If you don't have babel-cli installed globally, please do it first.

```
sudo npm install -g babel-cli
```

Clone the project and run

```
npm install
npm start
```

Then open localhost:8080 in a browser.

# Installation

```sh
npm install react-schema-form --save
```

There is one added on component react-schema-form-rc-select for multiple select and dynamically loading dropdown from server. To install it.
```
npm install react-schema-form-rc-select --save
```

# Usage
```js
var { SchemaForm } = require('react-schema-form');

<SchemaForm schema={this.state.schema} form={this.state.form} model={this.props.model} onModelChange={this.props.onModelChange} />

// for example:
_onChange: function() {
    this.setState({
        schema: FormStore.getForm('com.networknt.light.example').schema,
        form: FormStore.getForm('com.networknt.light.example').form
    });
}
```
# Examples

There are some simple forms in the demo to show how each fields to be rendered.
For more real world exmaples, please check [Light Framework Forms](https://github.com/networknt/light/tree/master/server/src/main/resources/form)

There are still some angular schema form in this folder and migration is in progress.

If you are interested in how these forms are utilized in the framework, please take a look at a react component [Form.jsx](https://github.com/networknt/light/blob/master/edibleforestgarden/src/app/components/Form.jsx)

Basically, All forms in this folder will be loaded to an Graph Database and UI is rendered by formId and form model will be validated on the browser as well as
backend APIs.

# Form format

React-schema-form implements the form format as defined by the json-schema-form standard.

The documentation for that format is located at the [json-schema-form wiki](https://github.com/json-schema-form/json-schema-form/wiki/Documentation).

# Customization
react-schema-form provides most fields including FieldSet and Array and they might cover most use cases; however, you might have requirement that needs something that is not built in. In this case, you
can implement your own field and inject it into the generic mapper for the builder to leverage your component. By passing a mapper as a props to the SchemaForm, you can replace built in component with
yours or you can define a brand new type and provide your component to render it.

[react-schema-form-rc-select](https://github.com/networknt/react-schema-form-rc-select) is an example to provide multiple select to the react schema form.

```js
require('rc-select/assets/index.css');
import RcSelect from 'react-schema-form-rc-select/lib/RcSelect';
...

        var mapper = {
            "rc-select": RcSelect
        };

        var schemaForm = '';
        if (this.state.form.length > 0) {
            schemaForm = (
                <SchemaForm schema={this.state.schema} form={this.state.form} model={this.state.model} onModelChange={this.onModelChange} mapper={mapper} />
            );
        }


```


# Contributing

See our [CONTRIBUTING.md](https://github.com/networknt/react-schema-form/CONTRIBUTING.md) for information on how to contribute.


# License

MIT Licensed. Copyright (c) Network New Technologies Inc. 2016.
