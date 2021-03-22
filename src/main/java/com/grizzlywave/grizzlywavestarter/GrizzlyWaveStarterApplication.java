package com.grizzlywave.grizzlywavestarter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * Grizzly Wave Starter Main Class 
 **/
@SpringBootApplication
public class GrizzlyWaveStarterApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(GrizzlyWaveStarterApplication.class, args);
	}
	
/*	@Autowired
    waveInitAnnotation waveInitAnnotation;
	@Autowired
	TopicServices topicService;
    @EventListener(ApplicationReadyEvent.class)
	public void shouldLogWaveInitAnnotationMethod() throws IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException {
		
		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
		    LOGGER.info(waveInitAnnotation.initOrder(new Order(2, 2, 200)).toString());
			topicService.getAllTopic();
	}*/

}
