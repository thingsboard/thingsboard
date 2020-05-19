# opencollective-cli
![](https://ci.appveyor.com/api/projects/status/5mf75q34cpr74s53?svg=true)

Command Line Interface for Open Collective.

## Install

    $ npm install -g opencollective
    
This will populate a `opencollective` (and its shortcut `oc`) as a command line.

You can also add this as a dependency in your `package.json` to automatically show the `postinstall` donate message:

    $ npm install --save opencollective
    
Then run

    $ opencollective setup


## Commands

    $ opencollective [collective] [info|stats]
    
Shows the latest stats of the collective (number of contributors, number of backers, annual budget and current balance).

![](https://cl.ly/1n2u281p2o1k/Screen%20Shot%202017-05-01%20at%204.41.58%20PM.png)

    $ opencollective [collective] donate [amount] [frequency]

Opens the donate page of your collective. E.g. $ opencollective webpack donate 5 monthly

    $ opencollective postinstall [--plain]
    
Reads the details of your collective in the `package.json` of the current directory and invite the user to donate after installing your package.
Add this command in the `postinstall` script of your `package.json`.

![](https://cl.ly/0u2a0z0Y3X37/Screen%20Shot%202017-03-24%20at%202.37.46%20PM.png)

If you add the `--plain` option, it won't show any emoji and ascii art (better for old terminals).

    $ opencollective setup

Interactive setup to add your collective info into your `package.json` and add the backers/sponsors badge and avatars in your `README.md`.


## Coming soon

    $ opencollective login
    
    $ opencollective logout
    
    $ opencollective cc | billing
    
    $ opencollective cc ls
    $ opencollective cc add
    $ opencollective cc rm
    
    $ opencollective apply [github_repo_url]
    
    $ opencollective show <collective>
    $ opencollective open <collective>
    
    $ opencollective ls // list the collectives you are contributing to.

    
Stop your contribution to <collective>. Warning: may make someone sad somewhere on this planet.

## Credits

Shamelessly inspired by the excellent [now-cli](https://github.com/zeit/now-cli)