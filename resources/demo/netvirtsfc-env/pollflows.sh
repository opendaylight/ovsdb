#!/usr/bin/env bash
TABLE=$1
watch -n 1 -d "sudo /vagrant/flowcount.sh $1"

