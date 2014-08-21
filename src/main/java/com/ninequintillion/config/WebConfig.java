package com.ninequintillion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.mustache.MustacheViewResolver;
import org.springframework.web.servlet.view.mustache.java.MustacheJTemplateFactory;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"com.ninequintillion.controllers"})
public class WebConfig extends WebMvcConfigurerAdapter {

    @Bean
    public ViewResolver getViewResolver(ResourceLoader resourceLoader) {
        MustacheJTemplateFactory mustacheJTemplateFactory = new MustacheJTemplateFactory();
        mustacheJTemplateFactory.setResourceLoader(resourceLoader);

        MustacheViewResolver mustacheViewResolver = new MustacheViewResolver();
        mustacheViewResolver.setPrefix("/WEB-INF/views/");
        mustacheViewResolver.setSuffix(".html");
        mustacheViewResolver.setCache(true);
        mustacheViewResolver.setContentType("text/html;charset=utf-8");
        mustacheViewResolver.setTemplateFactory(mustacheJTemplateFactory);
        return mustacheViewResolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
    }

}
