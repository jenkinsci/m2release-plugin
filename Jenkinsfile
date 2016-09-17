#!/usr/bin/env groovy

List mavenEnv = [
    "JAVA_HOME=${tool 'jdk7'}",
    "PATH+MAVEN=${tool 'mvn'}/bin",
]

String mavenCommand = 'mvn clean install -Dmaven.test.failure.ignore=true'

stage('Windows') {
    node('windows') {
        checkout scm
        withEnv(mavenEnv) {
            bat mavenCommand
        }
        junit '**/target/surefire-reports/**/*.xml'
    }
}

stage('Linux') {
    node('linux') {
        checkout scm
        withEnv(mavenEnv) {
            sh mavenCommand
        }
        junit '**/target/surefire-reports/**/*.xml'
    }
}
