@echo off
setlocal

set JAVA_CMD=java
if not "%JAVA_HOME%"=="" set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
if exist "C:\Users\Caytm\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.8-hotspot\bin\java.exe" set JAVA_CMD="C:\Users\Caytm\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.8-hotspot\bin\java.exe"

set WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar

if not exist "%WRAPPER_JAR%" (
    echo Maven wrapper jar not found: %WRAPPER_JAR%
    exit /b 1
)

%JAVA_CMD% %MAVEN_OPTS% -cp "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%~dp0" org.apache.maven.wrapper.MavenWrapperMain %*
