#!/usr/bin/env groovy

pipeline {

    agent any
    
    tools {
        jdk 'jdk11'
        maven 'M3'
    }

    triggers { pollSCM('*/5 * * * *') }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    stages {
    
    	stage('Build Stage') { 
    		steps {
	      		sh "mvn clean install"
	    	}
	 	}
	 	
	 	stage('SonarQube analysis') {
	    	steps {
		    	withSonarQubeEnv('AWS SONAR') {
	      			sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.2:sonar'
	    		}
		    }		
  	   }

  	   stage('Push grizzly starter dependencies to community maven repository') {
              	   		when { branch "develop" }
              	   		steps {
              	      		sh "mvn clean deploy"
              	      	}
              	   }
  	  
    }
    post {
        always {
            junit '**/surefire-reports/*.xml'
        }
        
        failure {
             emailext (
		      subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
		      body: """<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
		        <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>""",
		      recipientProviders: [[$class: 'DevelopersRecipientProvider'],[$class: 'CulpritsRecipientProvider']]
		    )
        }
    }
}