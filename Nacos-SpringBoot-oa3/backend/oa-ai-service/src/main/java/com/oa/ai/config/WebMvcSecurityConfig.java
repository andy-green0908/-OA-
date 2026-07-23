package com.oa.ai.config;

import com.oa.ai.security.EmployeeAuthInterceptor;
import com.oa.ai.security.InternalTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcSecurityConfig implements WebMvcConfigurer {

    private final EmployeeAuthInterceptor employeeAuthInterceptor;
    private final InternalTokenInterceptor internalTokenInterceptor;

    public WebMvcSecurityConfig(EmployeeAuthInterceptor employeeAuthInterceptor,
                                InternalTokenInterceptor internalTokenInterceptor) {
        this.employeeAuthInterceptor = employeeAuthInterceptor;
        this.internalTokenInterceptor = internalTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(employeeAuthInterceptor)
                .addPathPatterns("/chat/**")
                .excludePathPatterns("/chat/health");
        registry.addInterceptor(internalTokenInterceptor)
                .addPathPatterns("/kb/**");
    }
}
