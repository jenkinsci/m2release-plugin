#!/usr/bin/env groovy

String mavenCommand = 'mvn clean install -Dmaven.test.failure.ignore=true'

stage('Windows') {
    node('windows') {
        checkout scm
        withEnv([
            "JAVA_HOME=${tool 'jdk7'}",
            "PATH+MAVEN=${tool 'mvn'}/bin",
        ]) {
            bat mavenCommand
        }
        junit '**/target/surefire-reports/**/*.xml'
    }
}

stage('Linux') {
    node('linux') {
        checkout scm
        withEnv([
            "JAVA_HOME=${tool 'jdk7'}",
            "PATH+MAVEN=${tool 'mvn'}/bin",
        ]) {
            sh mavenCommand
        }
        junit '**/target/surefire-reports/**/*.xml'
    }
}
