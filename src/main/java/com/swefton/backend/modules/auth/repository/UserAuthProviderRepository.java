package com.swefton.backend.modules.auth.repository;

import com.swefton.backend.modules.auth.entity.UserAuthProvider;
import com.swefton.backend.modules.auth.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAuthProviderRepository
        extends JpaRepository<UserAuthProvider,Long>{

    Optional<UserAuthProvider> findByProviderAndProviderSubject(
        AuthProvider provider,
        String providerSubject
    );

    boolean existsByUserIdAndProvider(
        Long userId,
        AuthProvider provider
    );
}