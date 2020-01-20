#npv - Node Package Version
Prints out the current package version or any other package information you want.

##Install
```bash
npm install npv
```

##Usage
Printing out the current package version

```bash
npv
1.0.0
```

Package name is quite easy...
```bash
npv name
my-node-project
```

...or just pass the property path.
```bash
npv repository.url
https://github.com/...
```

Storing package version in an environment variable    
```bash
export PACKAGE_VERSION=$(npv)
```

## License
Released under the MIT license.
