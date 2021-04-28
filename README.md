# README #


### Grizzly Wave Starter ###

this framework uses the Event Streams to establish connection and exchange messages between microservices using annotation


### How do I get set up? ###

1.Run the docker-compose that containes the Kafka broker, Kafka HQ and MongoDB

2.Open the starter project in  your editor then add Grizzly wave dependency to your own project pom.xml file :

		<dependency>
			<groupId>com.grizzly-wave</groupId>
			<artifactId>grizzly-wave-starter</artifactId>
			<version>0.0.1-SNAPSHOT</version>
		</dependency>

3.Add @EnableGrizzlyWave Annotation in your main class

4.set your own properties in application.properties specifying your :
*  Kafka Properties :  spring.kafka.bootstrap-servers: (ex: 192.168.99.100:9092 )

* Eureka Client Properties : eureka.client.serviceUrl.defaultZone: http://localhost:8761/eureka/

* Grizzly Wave properties : grizzly-wave.topic_names: MyTopic //to create your own topic manually 
   							grizzly-wave.prefix: MyOrganization // prefix for the topics (default "Wave-")
    						grizzly-wave.auto_create_topic: false // to disable topics auto creation

5.use the annotaion @WaveWorkFlow(name = "MyWorkFLow") on classes which implemenst methods with (@WaveInit , @WaveTransition , @WaveEnd) annotations

6.finally you can start creating your Saga using our annotations (@WaveInit , @WaveTransition , @WaveEnd).
