package com.aipm.cowriting.config;

import com.aipm.cowriting.application.service.ResourceOwnershipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OwnershipWebMvcConfig implements WebMvcConfigurer {

    private final ObjectProvider<ResourceOwnershipService> ownershipProvider;

    public OwnershipWebMvcConfig(ObjectProvider<ResourceOwnershipService> ownershipProvider) {
        this.ownershipProvider = ownershipProvider;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new OwnershipInterceptor(ownershipProvider)).addPathPatterns("/api/v1/**");
    }

    private static final class OwnershipInterceptor implements HandlerInterceptor {
        private final ObjectProvider<ResourceOwnershipService> provider;
        private final Map<Pattern, BiConsumer<ResourceOwnershipService, UUID>> checks = new LinkedHashMap<>();

        private OwnershipInterceptor(ObjectProvider<ResourceOwnershipService> provider) {
            this.provider = provider;
            add("/workspaces/([0-9a-fA-F-]{36})(?:/|$)", ResourceOwnershipService::requireWorkspace);
            add("/materials/([0-9a-fA-F-]{36})(?:/|$)", ResourceOwnershipService::requireMaterial);
            add("/documents/([0-9a-fA-F-]{36})(?:/|$)", ResourceOwnershipService::requireDocument);
            add("/sections/([0-9a-fA-F-]{36})(?:/|$)", ResourceOwnershipService::requireSection);
            add("/section-versions/([0-9a-fA-F-]{36})(?:/|$)", ResourceOwnershipService::requireSectionVersion);
            add("/drafts/([0-9a-fA-F-]{36})(?:/|$)", ResourceOwnershipService::requireDraft);
            add("/review-items/([0-9a-fA-F-]{36})(?:/|$)", ResourceOwnershipService::requireReview);
            add("/appeals/([0-9a-fA-F-]{36})(?:/|$)", ResourceOwnershipService::requireAppeal);
            add("/evidence-bindings/([0-9a-fA-F-]{36})(?:/|$)", ResourceOwnershipService::requireEvidenceBinding);
            add("/co-write-previews/([0-9a-fA-F-]{36})(?:/|$)", ResourceOwnershipService::requireCoWritePreview);
            add("/section-co-write-previews/([0-9a-fA-F-]{36})(?:/|$)", ResourceOwnershipService::requireSectionCoWritePreview);
            add("/(?:jobs|exports)/([0-9a-fA-F-]{36})(?:/|$)", ResourceOwnershipService::requireJob);
        }

        private void add(String pattern, BiConsumer<ResourceOwnershipService, UUID> check) {
            checks.put(Pattern.compile(pattern), check);
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            ResourceOwnershipService ownership = provider.getIfAvailable();
            if (ownership == null) return true;
            String path = request.getRequestURI();
            for (Map.Entry<Pattern, BiConsumer<ResourceOwnershipService, UUID>> entry : checks.entrySet()) {
                Matcher matcher = entry.getKey().matcher(path);
                if (matcher.find()) {
                    entry.getValue().accept(ownership, UUID.fromString(matcher.group(1)));
                    break;
                }
            }
            return true;
        }
    }
}
