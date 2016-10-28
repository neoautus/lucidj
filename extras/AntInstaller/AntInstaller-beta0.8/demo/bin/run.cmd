@echo off

if "%JAVA_HOME%" == "" goto nojava

set CLASSPATH=.\classes

echo CLASSPATH:%CLASSPATH%
echo JAVA_HOME:%JAVA_HOME%

%JAVA_HOME%\bin\java -cp %CLASSPATH% org.tp23.demo.Demonstration

goto end
 
:nojava
echo Set JAVA_HOME environment variable
goto end


:end