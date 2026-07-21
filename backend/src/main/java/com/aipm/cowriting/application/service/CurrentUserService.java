package com.aipm.cowriting.application.service;

import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public UUID userId() {
        return UUID.fromString(jwt().getToken().getSubject());
    }

    public String email() {
        return jwt().getToken().getClaimAsString("email");
    }

    public String suggestedDisplayName() {
        Object claim = jwt().getToken().getClaim("user_metadata");
        if (claim instanceof Map<?, ?> metadata) {
            for (String key : new String[]{"full_name", "name"}) {
                Object value = metadata.get(key);
                if (value instanceof String text && !text.isBlank()) return text.trim();
            }
        }
        String email = email();
        if (email == null || email.isBlank()) {
            return "学术用户";
        }
        int separator = email.indexOf('@');
        return separator > 0 ? email.substring(0, separator) : email;
    }

    public String suggestedAvatarUrl() {
        Object claim = jwt().getToken().getClaim("user_metadata");
        if (claim instanceof Map<?, ?> metadata) {
            for (String key : new String[]{"avatar_url", "picture"}) {
                Object value = metadata.get(key);
                if (value instanceof String text && !text.isBlank()) return text.trim();
            }
        }
        return null;
    }

    private JwtAuthenticationToken jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt && authentication.isAuthenticated()) {
            return jwt;
        }
        throw new BusinessException(
                ErrorCode.UNAUTHORIZED,
                HttpStatus.UNAUTHORIZED.value(),
                "请先登录"
        );
    }
}
