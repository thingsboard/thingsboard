#!/bin/bash
cd /tmp
echo "Forking $1/$2";
curl -X POST -H "Authorization: token $TOKEN" https://api.github.com/repos/$1/$2/forks?org=opencollective
git clone git@github.com:opencollective/$2.git
cd $2;
git checkout -b opencollective
npm install --save opencollective
opencollective setup
open README.md
echo "Please double check README.md";
echo "Continue? (Y/n)";
read continue;
if [ "$continue" = "n" ]; then exit 0; fi;
git add README.md
git commit -m "Added backers and sponsors on the README"
git add package.json
git commit -m "Added call to donate after npm install"
git push origin opencollective

curl -X POST -H "Authorization: token $TOKEN" https://api.github.com/repos/$1/$2/pulls --data "{\"title\":\"Activating Open Collective\", \"body\": \"Please merge this pull request to activate your Open Collective https://opencollective.com/$2\", \"head\":\"opencollective:opencollective\",\"base\":\"master\"}"

# We clean the directory and go back to where we were
rm -rf $2
cd -
