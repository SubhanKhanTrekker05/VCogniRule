package com.ooad.mhs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC settings: registers {@link AdminAuthInterceptor} for the admin pages.
 *
 * Every /admin/** URL needs a logged-in admin, except the login page itself
 * (/admin), the login form submit (/admin/login) and logout (/admin/logout).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebConfig(AdminAuthInterceptor adminAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/admin/**")
            .excludePathPatterns("/admin", "/admin/login", "/admin/logout");
    }
}
