package com.vthr.erp_hrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(excludeName = {
		"org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@EnableScheduling
public class VthrSolutionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(VthrSolutionsApplication.class, args);
	}

}
