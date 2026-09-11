package com.boxai.security.config;

import com.boxai.security.interceptor.PlatformAdminInterceptor;
import com.boxai.security.interceptor.TenantStatusInterceptor;
import com.boxai.security.interceptor.WorkspaceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantStatusInterceptor tenantStatusInterceptor;
    private final WorkspaceInterceptor workspaceInterceptor;
    private final PlatformAdminInterceptor platformAdminInterceptor;

    public WebMvcConfig(TenantStatusInterceptor tenantStatusInterceptor,
                        WorkspaceInterceptor workspaceInterceptor,
                        PlatformAdminInterceptor platformAdminInterceptor) {
        this.tenantStatusInterceptor = tenantStatusInterceptor;
        this.workspaceInterceptor = workspaceInterceptor;
        this.platformAdminInterceptor = platformAdminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(platformAdminInterceptor).addPathPatterns("/api/v1/admin/**");
        registry.addInterceptor(tenantStatusInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/admin/**");
        registry.addInterceptor(workspaceInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/admin/**");
    }
}
