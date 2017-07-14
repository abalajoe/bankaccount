#! /usr/bin/env bash

# Bank Account App Script
# @joeabala

# Jar file and conf file
conf_file="config.properties"
log_file="log4j.properties"
jar="bankaccount.jar"

# Run App
java -Dlog4j.configuration=file:$log_file -Dbankaccount.config=$conf_file -jar $jar