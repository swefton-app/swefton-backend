package com.swefton.backend.modules.auth.middleware;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.swefton.backend.infrastructure.web.request.CachedBodyHttpServletRequest;
import com.swefton.backend.modules.auth.api.AuthApi;
import com.swefton.backend.modules.auth.dto.request.LoginRequest;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class EmailVerifiedFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(request.getMethod().equalsIgnoreCase("POST")
                && request.getServletPath().equals(AuthApi.BASE_PATH + "/login"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        LoginRequest loginRequest = objectMapper.readValue(wrappedRequest.getInputStream(), LoginRequest.class);
        String email = loginRequest.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user == null || user.isEmailConfirmed()) {
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse("EMAIL_NOT_VERIFIED", "Email is not verified"));
    }

    private record ErrorResponse(String code, String message) {
    }
}
