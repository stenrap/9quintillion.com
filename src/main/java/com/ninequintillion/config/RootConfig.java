package com.ninequintillion.config;

import com.ninequintillion.services.BracketService;
import com.ninequintillion.services.BracketServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.ninequintillion.services"})
public class RootConfig {



}
