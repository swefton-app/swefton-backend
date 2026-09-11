package com.swefton.backend.session;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.swefton.backend.modules.user.enums.RoleCode;

@Component
public class SessionUser implements ISessionUser {

    @Override
    public Long getUserId(){
        return Long.valueOf(
            getAuthentication()
                .getToken()
                .getSubject()
        );
    }

    @Override
    public RoleCode getRole(){
        String role=getAuthentication()
            .getToken()
            .getClaimAsString("role");

        if(role==null||role.isBlank()){
            throw new AuthenticationCredentialsNotFoundException(
                "User role not found"
            );
        }

        return RoleCode.valueOf(role);
    }

    @Override
    public boolean hasRole(RoleCode role){
        return getRole()==role;
    }

    private JwtAuthenticationToken getAuthentication(){
        Authentication authentication=SecurityContextHolder
            .getContext()
            .getAuthentication();

        if(!(authentication instanceof JwtAuthenticationToken jwtAuthentication)){
            throw new AuthenticationCredentialsNotFoundException(
                "Authenticated user not found"
            );
        }

        return jwtAuthentication;
    }
}
