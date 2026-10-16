package com.swefton.backend.session;

public interface ISessionUser {

    Long getUserId();

    String getRole();

    boolean hasRole(String role);
}
