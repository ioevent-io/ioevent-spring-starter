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
              	   withMaven(
        // Maven installation declared in the Jenkins "Global Tool Configuration"
        maven: 'maven-3', // (1)
        // Use `$WORKSPACE/.repository` for local repository folder to avoid shared repositories
        mavenLocalRepo: '.repository', // (2)
        // Maven settings.xml file defined with the Jenkins Config File Provider Plugin
        // We recommend to define Maven settings.xml globally at the folder level using
        // navigating to the folder configuration in the section "Pipeline Maven Configuration / Override global Maven configuration"
        // or globally to the entire master navigating to  "Manage Jenkins / Global Tools Configuration"
        mavenSettingsConfig: 'MySettings' // (3)
    ) {

      // Run the maven build
      sh "mvn clean deploy"

    } // withMaven will discover the generated Maven artifacts, JUnit Surefire & FailSafe & FindBugs & SpotBugs reports...
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