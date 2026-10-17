#!/bin/sh
set -eu
mkdir -p "$1/native"
cc -std=c11 -O2 -fPIC -shared -Wall -Wextra -Wno-misleading-indentation "$(dirname "$0")/photoraw.c" -o "$1/native/libphotoraw.so" $(pkg-config --cflags --libs libraw lensfun lcms2) -lm
