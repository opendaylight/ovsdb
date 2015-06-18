#!/usr/bin/env bash
#

sudo service openvswitch-switch stop
sudo rm -rf /var/log/openvswitch/*
sudo rm -rf /etc/openvswitch/conf.db
sudo service openvswitch-switch start

