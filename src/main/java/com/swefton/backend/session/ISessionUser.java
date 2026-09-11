package com.swefton.backend.session;

import com.swefton.backend.modules.user.enums.RoleCode;

public interface ISessionUser {

    Long getUserId();

    RoleCode getRole();

    boolean hasRole(RoleCode role);
}
