package com.example.sportsparser;

import com.example.sportsparser.model.Sport;
import com.example.sportsparser.service.SportsParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ExecutionException;

@SpringBootApplication
public class SportsparserApplication {


    private final SportsParserService sportsParserService;

    public SportsparserApplication(SportsParserService sportsParserService) {
        this.sportsParserService = sportsParserService;
    }

    public static void main(String[] args) {
        SpringApplication.run(SportsparserApplication.class, args);


    }

    @Bean
    CommandLineRunner run() {
        return args -> {
            sportsParserService.fetchAndDisplayMatchData();
        };
    }


}
