package com.swefton.backend.config.openapi;

import java.util.ArrayList;
import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI sweftonOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Swefton API")
                        .version("v1")
                        .description("REST API for authentication, user profiles, trainers and gyms. "
                                + "Use Authorize with a JWT access token for protected endpoints."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token returned by POST /api/v1/auth/login.")))
                .tags(apiTags());
    }

    @Bean
    public OpenApiCustomizer sweftonOpenApiCustomizer() {
        return openApi -> {
            openApi.getPaths().forEach((path, pathItem) ->
                    pathItem.readOperationsMap().forEach((method, operation) -> {
                        AccessRule accessRule = accessRule(path, method.name());
                        operation.setTags(List.of(tagFor(path)));
                        operation.addExtension("x-allowed-roles", accessRule.allowedRoles());
                        operation.addExtension("x-access-rule", accessRule.description());
                        operation.setDescription(withAccessDescription(operation, accessRule));

                        if (!accessRule.publicEndpoint()) {
                            operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
                        }

                        documentSuccessfulResponse(path, method.name(), operation.getResponses());
                        documentErrorResponses(path, method.name(), operation.getResponses(), accessRule);
                    }));
        };
    }

    private List<Tag> apiTags() {
        return List.of(
                new Tag().name("Authentication").description("Registration, email verification and JWT sessions."),
                new Tag().name("User account").description("Authenticated user's onboarding, profile, preferences and address."),
                new Tag().name("User images").description("Profile, cover, logo and gallery images."),
                new Tag().name("Public trainers").description("Public trainer discovery and categories."),
                new Tag().name("Client appointments").description("Trainer appointment operations for clients."),
                new Tag().name("Trainer management").description("Trainer services, availability and appointments."),
                new Tag().name("Trainer documents").description("Private trainer licence and CV files."),
                new Tag().name("Public gyms").description("Public gym discovery, facilities and image content."),
                new Tag().name("Gym management").description("Gym owner and authorized gym staff operations."),
                new Tag().name("Gym memberships").description("Membership, class booking and attendance operations for members."),
                new Tag().name("Administration").description("ADMIN-only catalogue and verification operations."));
    }

    private String withAccessDescription(Operation operation, AccessRule accessRule) {
        String access = "Access: " + accessRule.description();
        if (operation.getDescription() == null || operation.getDescription().isBlank()) {
            return access;
        }
        if (operation.getDescription().contains("Access:")) {
            return operation.getDescription();
        }
        return operation.getDescription() + "\n\n" + access;
    }

    private void documentSuccessfulResponse(String path, String method, ApiResponses responses) {
        if (responses == null) {
            return;
        }

        String expectedCode = successfulStatus(path, method);
        if ("200".equals(expectedCode)) {
            ApiResponse response = responses.get("200");
            if (response != null && (response.getDescription() == null || response.getDescription().isBlank())) {
                response.setDescription("Request completed successfully.");
            }
            return;
        }

        ApiResponse documented = responses.get(expectedCode);
        if (documented != null) {
            responses.remove("200");
            if (documented.getDescription() == null || documented.getDescription().isBlank()) {
                documented.setDescription("201".equals(expectedCode)
                        ? "Resource created successfully."
                        : "Request completed successfully; no response body.");
            }
            return;
        }

        ApiResponse inferred = responses.remove("200");
        if ("204".equals(expectedCode)) {
            responses.addApiResponse("204", new ApiResponse().description("Request completed successfully; no response body."));
            return;
        }

        if (inferred == null) {
            inferred = new ApiResponse();
        }
        inferred.setDescription("Resource created successfully.");
        responses.addApiResponse("201", inferred);
    }

    private void documentErrorResponses(
            String path,
            String method,
            ApiResponses responses,
            AccessRule accessRule) {

        if (responses == null) {
            return;
        }

        addResponse(responses, "400", "Invalid path, query, multipart or request-body data.");

        if (!accessRule.publicEndpoint()) {
            addResponse(responses, "401", "Missing, invalid, expired or revoked JWT access token.");
            addResponse(responses, "403", "The authenticated user does not satisfy the required role, ownership or staff permission.");
        }

        if (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/refresh")) {
            addResponse(responses, "401", "The credentials or refresh token are invalid.");
            addResponse(responses, "403", "The account is disabled or the email address has not been verified.");
        }

        if (path.contains("{") || path.startsWith("/api/v1/user")) {
            addResponse(responses, "404", "The requested resource was not found or is not visible to this user.");
        }

        if (!"GET".equals(method)
                && !"DELETE".equals(method)
                && (!path.startsWith("/api/v1/auth/") || path.equals("/api/v1/auth/register"))) {
            addResponse(responses, "409", "The request conflicts with the resource's current state or an existing record.");
        }

        if (isMultipartEndpoint(path, method)) {
            addResponse(responses, "413", "The uploaded file is larger than the configured limit.");
            addResponse(responses, "415", "The uploaded media type is not supported.");
        }

        if ("/api/v1/auth/register".equals(path)) {
            addResponse(responses, "503", "The email service is temporarily unavailable.");
        }

        addResponse(responses, "500", "Unexpected server or storage error.");
    }

    private void addResponse(ApiResponses responses, String code, String description) {
        if (!responses.containsKey(code)) {
            responses.addApiResponse(code, new ApiResponse()
                    .description(description)
                    .content(new Content()));
        }
    }

    private String successfulStatus(String path, String method) {
        if (isNoContent(path, method)) {
            return "204";
        }
        if (isCreated(path, method)) {
            return "201";
        }
        return "200";
    }

    private boolean isNoContent(String path, String method) {
        if ("DELETE".equals(method)) {
            return path.equals("/api/v1/user")
                    || path.startsWith("/api/v1/trainer/")
                    || path.startsWith("/api/v1/gym-management/");
        }
        if (!"POST".equals(method)) {
            return false;
        }
        return path.equals("/api/v1/auth/logout")
                || path.equals("/api/v1/auth/verify-email")
                || path.equals("/api/v1/auth/resend-verification-code")
                || path.endsWith("/staff");
    }

    private boolean isCreated(String path, String method) {
        if (!"POST".equals(method)) {
            return false;
        }
        if (path.startsWith("/api/v1/auth/")) {
            return path.equals("/api/v1/auth/register");
        }
        return !path.endsWith("/approve")
                && !path.endsWith("/reject")
                && !path.endsWith("/suspend")
                && !path.endsWith("/submit")
                && !path.endsWith("/cancel");
    }

    private boolean isMultipartEndpoint(String path, String method) {
        return "POST".equals(method)
                && (path.equals("/api/v1/images")
                        || path.equals("/api/v1/trainer/documents")
                        || path.endsWith("/images"));
    }

    private AccessRule accessRule(String path, String method) {
        if (isPublic(path, method)) {
            return new AccessRule(true, List.of("PUBLIC"), "PUBLIC - no JWT required.");
        }
        if (path.startsWith("/api/v1/admin/")) {
            return new AccessRule(false, List.of("ADMIN"), "ADMIN role with a valid JWT.");
        }
        if (path.startsWith("/api/v1/trainer/")) {
            return new AccessRule(false, List.of("TRAINER"), "TRAINER role with a valid JWT.");
        }
        if (path.startsWith("/api/v1/appointments")) {
            return new AccessRule(false, List.of("USER"), "USER role with a valid JWT.");
        }
        if (path.startsWith("/api/v1/gym-memberships")) {
            return new AccessRule(false, List.of("USER"), "USER role with a valid JWT.");
        }
        if (path.startsWith("/api/v1/gym-management")) {
            return gymManagementAccess(path, method);
        }
        return new AccessRule(
                false,
                List.of("USER", "TRAINER", "GYM_OWNER", "ADMIN"),
                "Any authenticated account role with a valid JWT.");
    }

    private AccessRule gymManagementAccess(String path, String method) {
        if (path.equals("/api/v1/gym-management")
                || path.matches("/api/v1/gym-management/\\{gymId}/?(?:staff|submit)?")) {
            return new AccessRule(false, List.of("GYM_OWNER"), "GYM_OWNER for a gym owned by that user.");
        }
        if (path.contains("/attendance") || path.contains("/memberships")) {
            return new AccessRule(
                    false,
                    List.of("USER", "TRAINER", "GYM_OWNER", "ADMIN"),
                    "Gym owner, MANAGER or RECEPTIONIST assigned to this gym.");
        }
        return new AccessRule(
                false,
                List.of("USER", "TRAINER", "GYM_OWNER", "ADMIN"),
                "Gym owner or MANAGER assigned to this gym.");
    }

    private boolean isPublic(String path, String method) {
        if (path.startsWith("/api/v1/public/")) {
            return true;
        }
        if ("GET".equals(method) && path.startsWith("/api/v1/images/")) {
            return true;
        }
        return path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/verify-email")
                || path.equals("/api/v1/auth/resend-verification-code");
    }

    private String tagFor(String path) {
        if (path.startsWith("/api/v1/auth")) return "Authentication";
        if (path.startsWith("/api/v1/user")) return "User account";
        if (path.startsWith("/api/v1/images")) return "User images";
        if (path.startsWith("/api/v1/public/trainers") || path.equals("/api/v1/public/categories")) {
            return "Public trainers";
        }
        if (path.startsWith("/api/v1/appointments")) return "Client appointments";
        if (path.startsWith("/api/v1/trainer/documents")) return "Trainer documents";
        if (path.startsWith("/api/v1/trainer")) return "Trainer management";
        if (path.startsWith("/api/v1/public/gyms")
                || path.startsWith("/api/v1/public/facilities")
                || path.startsWith("/api/v1/public/gym-images")) {
            return "Public gyms";
        }
        if (path.startsWith("/api/v1/gym-management")) return "Gym management";
        if (path.startsWith("/api/v1/gym-memberships")) return "Gym memberships";
        if (path.startsWith("/api/v1/admin")) return "Administration";
        return "Other";
    }

    private record AccessRule(boolean publicEndpoint, List<String> allowedRoles, String description) {
        private AccessRule {
            allowedRoles = new ArrayList<>(allowedRoles);
            description = description.trim();
        }
    }
}
