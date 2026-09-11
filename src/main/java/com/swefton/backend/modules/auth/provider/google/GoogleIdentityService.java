package com.swefton.backend.modules.auth.provider.google;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Service
public class GoogleIdentityService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentityService(@Value("${app.google.client-id}") String clientId) {
        try {
            this.verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(List.of(clientId))
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not initialize Google authentication", exception);
        }
    }

    public GoogleIdentity verify(String credential) {
        try {
            GoogleIdToken idToken = verifier.verify(credential);

            if (idToken == null) {
                throw invalidCredential();
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String subject = payload.getSubject();
            String email = payload.getEmail();

            if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
                throw invalidCredential();
            }

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Google email is not verified");
            }

            return GoogleIdentity.builder()
                    .subject(subject)
                    .email(email)
                    .emailVerified(true)
                    .firstName((String) payload.get("given_name"))
                    .lastName((String) payload.get("family_name"))
                    .pictureUrl((String) payload.get("picture"))
                    .build();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Google credential",
                    exception);
        }
    }

    private ResponseStatusException invalidCredential() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google credential");
    }
}
