#!/bin/bash
# Run without Maven: put mysql-connector-j-8.x.jar inside ./lib first
set -e
mkdir -p out
javac -cp "lib/*" -d out src/main/java/pharmacy/*.java
java -cp "out:lib/*" pharmacy.Main
