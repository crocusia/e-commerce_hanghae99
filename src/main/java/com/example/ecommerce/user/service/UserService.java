package com.example.ecommerce.user.service;

import com.example.ecommerce.user.domain.User;
import com.example.ecommerce.user.dto.BalanceDeductRequest;
import com.example.ecommerce.user.dto.UserCreateRequest;
import com.example.ecommerce.user.dto.UserResponse;
import com.example.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserResponse createUser(UserCreateRequest request) {
        User user = request.toEntity();
        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findByIdOrElseThrow(userId);
        return UserResponse.from(user);
    }

    public UserResponse deductBalance(BalanceDeductRequest request) {
        User user = userRepository.findByIdOrElseThrow(request.userId());

        log.info("잔액 차감 시작 - userId: {}, 차감 금액: {}, 현재 잔액: {}",
            request.userId(), request.amount(), user.getBalance());

        user.deductBalance(request.amount());
        User savedUser = userRepository.save(user);

        log.info("잔액 차감 완료 - userId: {}, 차감 후 잔액: {}",
            savedUser.getId(), savedUser.getBalance());

        return UserResponse.from(savedUser);
    }
}
