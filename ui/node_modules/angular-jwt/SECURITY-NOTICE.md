# Security vulnerability details for angular-jwt < 0.1.10

1. [Domain whitelisting bypass](#domain-whitelisting-bypass)

## Domain whitelisting bypass

### Description

The [domain whitelisting](https://github.com/auth0/angular-jwt#whitelisting-domains) feature can be bypassed. For example, if the setting is initialized with 

```js
jwtInterceptorProvider.whiteListedDomains = ['whitelisted.Example.com'];
```

An attacker can set up a domain `whitelistedXexample.com` that will pass the whitelist filter. The root cause for this is that `angular-jwt` always treats `whiteListedDomains` entries as regular expressions and causes `.` separator to match any character.

### Mitigation

Updated package is available on [NPM](https://npmjs.com):

```bash
$ npm install angular-jwt@0.1.10
```

To make it easier to keep up with security updates in the future, please make sure your `package.json` file is updated to take patch and minor level updates of our libraries:

```json
{
  "dependencies": {
    "angular-jwt": "^0.1.10"
  }
}
```

### Upgrade notes

1. This fix patches your application but has no impact on your data or user sessions.

### References

1. [CVE-2018-11537](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-11537)

### Credits

- Stephan Hauser
