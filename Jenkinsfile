#!/usr/bin/env groovy

node('windows') {
    withEnv([
        "JAVA_HOME=${tool 'jdk7'}",
        "PATH+MAVEN=${tool 'mvn'}/bin",
    ]){
        bat 'mvn clean install -Dmaven.test.failure.ignore=true'
    }
    junit '**/target/surefire-reports/**/*.xml'
}
