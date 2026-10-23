@echo off
REM Run without Maven: put mysql-connector-j-8.x.jar inside the lib folder first
if not exist out mkdir out
javac -cp "lib/*" -d out src/main/java/pharmacy/*.java
if errorlevel 1 exit /b 1
java -cp "out;lib/*" pharmacy.Main
