#!/bin/bash

# git add -u

git status | grep "modified:" | awk '{print $3}'

git status
