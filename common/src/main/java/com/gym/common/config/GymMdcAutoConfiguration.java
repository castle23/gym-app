package com.gym.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
public class GymMdcAutoConfiguration implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<GymMdcFilter> mdcFilter() {
        FilterRegistrationBean<GymMdcFilter> bean = new FilterRegistrationBean<>(new GymMdcFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public GymRoleInterceptor gymRoleInterceptor() {
        return new GymRoleInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gymRoleInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/health", "/info", "/metrics", "/actuator/**");
    }
}
