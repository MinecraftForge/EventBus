pipeline {
  agent {
    docker {
        image 'gradle:latest'
        args '-v gradlecache:/home/gradle/.gradle'
    }
  }
  stages {
    stage('fetch') {
      steps {
        git(url: 'https://github.com/cpw/eventbus.git', changelog: true)
      }
    }
    stage('buildandtest') {
      steps {
        sh './gradlew build cleanTest test'
      }
    }
    stage('publish') {
      environment {
        FORGE_MAVEN = credentials('forge-maven-forge-user')
      }
      steps {
        sh './gradlew publish -PforgeMavenUser=${FORGE_MAVEN_USR} -PforgeMavenPassword=${FORGE_MAVEN_PSW}'
        sh 'curl --user ${FORGE_MAVEN} http://files.minecraftforge.net/maven/manage/promote/latest/net.minecraftforge.eventbus/${BUILD_NUMBER}'
      }
    }
  }
  post {
    always {
      archiveArtifacts artifacts: 'build/libs/**/*.jar', fingerprint: true
      junit 'build/test-results/test/*.xml'
      jacoco sourcePattern: '**/src/*/java'
    }
  }
}