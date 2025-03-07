#!/bin/bash

HOSTS=PAVLOV_YC_ALT_SP

cd ../

ansible-playbook deploy_etcd.yml -l $HOSTS
