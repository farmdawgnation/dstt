#!/bin/bash

set -e

echo "DSTT Installer"
read -p "Where would you like to install DSTT? [/usr/local/bin is default] " INSTALLPATH

if [ -z "$INSTALLPATH" ]; then
  echo "Defaulting to /usr/local/bin"
  INSTALLPATH="/usr/local/bin"
fi

if [ ! -d $INSTALLPATH ]; then
  echo "Install path does not exist or is not a directory." >&2
  exit 1
fi

if [ ! -w $INSTALLPATH ]; then
  echo "Install path is not writable. Try using sudo to install there." >&2
  exit 1
fi

cp dstt-{VERSION}-standalone.jar $INSTALLPATH
cp dstt $INSTALLPATH

echo "Installed to $INSTALLPATH"
