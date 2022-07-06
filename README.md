
![Logo](https://www.codeonce.fr/assets/img/codeonce/icon.png)

#### _IOEvent, an open source microservice framework, created for developers and enterprises for Event-Driven microservices communication and visualization based on the choreography saga._
---
[IOEvent](https://dnrillbtq5f8k.cloudfront.net/)

---

## What's IOEvent ?
---

IOEvent is essentially a new framework for choreographing microservices. It aims to help microservices applications to link the microservices that make up the complete application with simple and easy lines of code without going further into the configuration of the "brorker" technology and without using an orchestrator like the central workflow engines.

This Framework ensures the transfer of information and communication asynchronously without waiting for a response or worrying about what happens next. Each service observes its environment. Developers will be able to connect its microservices in a short time ensuring the performance of its application thanks to the fast transfer of information between microservices, in addition allowing it to set up a monitoring of execution by process and a "dashboard" of monitoring of execution in BPM.


## Features
---

👩‍💻 **Linking Microservices -** IOEvent allows to link between microservices using simple code that defines the Input and Output of each event by attaching it to any object type.

⚡️ **Simple Configurations -** IOEvent provides a default framework configuration for the technologies used by the framework, as well as simple configuration options to customize the configuration of my application.

🧠 **Execution Tracking -** IOEvent allows to track the process execution information (number of instances, time spent per instance in a microservice).

💬 **Process Supervising -** IOEvent provides a dashboard to display the process diagram created by the microservices with the link created between the microservices and display the current instances in each microservice with all the information about them.




## Getting started
---
You can start using IOEvent by following this steps:

**Step 0 : System Requirements**

IOEvent is developed by Spring Boot 2.6.3, it requires Java 11 and Apache Kafka 2.8.0 .


**Step 1 : Start with Kafka**

To start an Apache Kafka server, you can use this docker-compose.yml file :


---


	version: '3.3'

		volumes:
			zookeeper-data:
				driver: local
			zookeeper-log:
				driver: local
			kafka-data:
				driver: local

		services:

		  zookeeper:
				restart: always
				image: confluentinc/cp-zookeeper
				volumes:
				- zookeeper-data:/var/lib/zookeeper/data:Z
				- zookeeper-log:/var/lib/zookeeper/log:Z
				environment:
				ZOOKEEPER_CLIENT_PORT: '2181'
				ZOOKEEPER_ADMIN_ENABLE_SERVER: 'false'

		    kafka:
				restart: always
				image: confluentinc/cp-kafka:6.2.1
				container_name: kafka
				volumes:
					- kafka-data:/var/lib/kafka:Z
				ports:
					- "9092:9092"
					- "29092:29092"
				environment:
					KAFKA_BROKER_ID: '0'
					KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
					KAFKA_NUM_PARTITIONS: '12'
					KAFKA_COMPRESSION_TYPE: 'gzip'
					KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: '1'
					KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: '1'
					KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: '1'
					KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
					KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092
					KAFKA_CONFLUENT_SUPPORT_METRICS_ENABLE: 'false'
					KAFKA_JMX_PORT: '9091'
					KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
					KAFKA_AUTHORIZER_CLASS_NAME: 'kafka.security.auth.SimpleAclAuthorizer'
					KAFKA_ALLOW_EVERYONE_IF_NO_ACL_FOUND: 'true'
				links:
					- zookeeper


---


Let’s start the Kafka server by spinning up the containers using the docker-compose command :

---

		$ docker-compose up -d
	
---


**Step 2 : Add IOEvent Dependency**

To add IOEvent dependency, edit your pom.xml and add the ioevent-spring-boot-starter :

---

	<dependency>
			<groupId>io.ioevent</groupId>
			<artifactId>ioevent-spring-boot-starter</artifactId>
			<version>1.0.0</version>
    </dependency>
	
---

**Step 3 : Add IOEvent Property**

To import IOEvent properties, you can add the following to your application.properties or application.yaml file:

---

	spring:
 	  application:
   		name: "Workflow_Name"   
  	  kafka:
   		bootstrap-servers: localhost:29092  
	ioevent: 
   		prefix: "Workflow_Prefix"   
   		group_id: "GROUP ID"
    	api_key: "API_Key"
  	
	  

---

**Step 4 : Add @EnableIOEvent**

---

		@SpringBootApplication
		@EnableIOEvent
		public class MyApplication {

        public static void main(String[] args) {
            SpringApplication.run(MyApplication.class, args);
    }
	}



---

Now you can start implementing IOEvent methods!! 🛠 😄



## Documentation
---
All documentation can be found on [IOEvent](https://d2wab6xn4w2e46.cloudfront.net/) 📚

