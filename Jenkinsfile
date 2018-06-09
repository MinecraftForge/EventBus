pipeline {
  agent any
  stages {
    stage('fetch') {
      steps {
        git(url: 'https://github.com/cpw/eventbus.git', changelog: true)
      }
    }
    stage('build') {
      steps {
        sh './gradlew cleanTest test publish'
      }
    }
  }
}