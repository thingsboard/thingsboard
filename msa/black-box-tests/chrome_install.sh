#!/bin/bash

set -e # Any subsequent commands which fail will cause the shell script to exit immediately
CHROME_PATH=/tmp/google-chrome

echo "Installing google-chrome to the $CHROME_PATH"

rm -rf $CHROME_PATH
mkdir -p $CHROME_PATH
wget --no-verbose -P $CHROME_PATH https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
dpkg-deb -xv $CHROME_PATH/google-chrome-stable_current_amd64.deb /tmp/google-chrome/
rm $CHROME_PATH/google-chrome-stable_current_amd64.deb

echo "Use google-binary as ChromeOptions options = new ChromeOptions(); options.setBinary(\"${CHROME_PATH}/opt/google/chrome/chrome\");"
