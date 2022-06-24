# README #


### Grizzly Wave Starter ###

this framework uses the Event Streams to establish connection and exchange messages between microservices using annotation


### How do I get set up? ###

1.Run the docker-compose that containes the Kafka broker, Kafka HQ , Ksql and MongoDB
	  
	  docker-compose up -d

2.Open the starter project in  your editor then add Grizzly wave starter dependency to your new project in pom.xml file :

		<dependency>
			<groupId>com.grizzly-wave</groupId>
			<artifactId>grizzly-wave-starter</artifactId>
			<version>0.0.1-SNAPSHOT</version>
		</dependency>

3.Add @EnableGrizzlyWave Annotation in your main class

4.set your own properties .
	exemple:
    
            grizzly-wave:
                prefix: PhoneMS
                group_id: PhoneMS-Order
            spring:
              application:
                name: Phone-Order-MS
              kafka:
                 bootstrap-servers: 192.168.99.100:9092
              data:
                mongodb:
                  database: order
                  host: 192.168.99.100
                  port: 27017
            server:
              port: 8087
            Ksql:
              server: 192.168.99.100
              

5.finally you can start creating your Saga using our annotation (@IOEvent).
