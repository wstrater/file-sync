#!/bin/bash

git ls-files --other --exclude-standard | grep "\.java" | xargs git add 

git status
