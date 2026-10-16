package com.swefton.backend.modules.user.repository;

import java.util.List;

import com.swefton.backend.modules.user.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    @Query("""
            SELECT address
            FROM UserAddress address
            WHERE address.user.id = :userId
            AND address.deletedAt IS NULL
            ORDER BY address.createdAt DESC
            """)
    List<UserAddress> findByUserId(@Param("userId") Long userId);
}
    
