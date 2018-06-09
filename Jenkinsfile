pipeline {
  agent any
  stages {
    stage('fetch') {
      steps {
        git(url: 'https://github.com/cpw/eventbus.git', changelog: true)
      }
    }
    stage('buildandtest') {
      steps {
        sh './gradlew cleanTest test'
      }
    }
    stage('publish') {
      environment {
        FORGE_MAVEN_USER = 'credentials(\'forge-maven-user\')'
        FORGE_MAVEN_PASSWORD = 'credentials(\'forge-maven-password\')'
      }
      steps {
        sh './gradlew publish -PforgeMavenUser=${FORGEMAVENUSER} -PforgeMavenPassword=${FORGEMAVENPASSWORD}'
      }
    }
  }
}