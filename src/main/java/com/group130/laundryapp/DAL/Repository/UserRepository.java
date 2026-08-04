package com.group130.laundryapp.DAL.Repository;

import com.group130.laundryapp.Domain.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByAccountId(UUID accountId);

    @Query("SELECT u FROM User u WHERE u.city = :city")
    Page<User> findByCity(@Param("city") String city, Pageable pageable);
}
