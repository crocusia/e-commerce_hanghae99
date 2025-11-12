package com.example.ecommerce.user.repository;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.user.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1L);

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public User findByIdOrElseThrow(Long id) {
        return findById(id).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            Long newId = idGenerator.getAndIncrement();
            User newUser = User.builder()
                .id(newId)
                .name(user.getName())
                .email(user.getEmail())
                .balance(user.getBalance())
                .build();
            store.put(newId, newUser);
            return newUser;
        }

        store.put(user.getId(), user);
        return user;
    }
}