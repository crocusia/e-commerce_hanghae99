package com.example.ecommerce.user.repository;

import com.example.ecommerce.user.domain.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);

    User findByIdOrElseThrow(Long id);

    User save(User user);
}