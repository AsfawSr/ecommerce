//package com.ecommerce.user_service.init;
//
//import com.ecommerce.user_service.model.User;
//import com.ecommerce.user_service.repository.UserRepository;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class DataInitializer {
//
//    private final UserRepository userRepository;
//
//    @PostConstruct
//    public void init() {
//        // Add some initial users to database if using JPA
//        if (userRepository.count() == 0) {
//            userRepository.save(User.builder()
//                    .username("john_doe")
//                    .email("john@example.com")
//                    .fullName("John Doe")
//                    .address("123 Main St")
//                    .phone("555-0101")
//                    .active(true)
//                    .build());
//
//            userRepository.save(User.builder()
//                    .username("jane_smith")
//                    .email("jane@example.com")
//                    .fullName("Jane Smith")
//                    .address("456 Oak Ave")
//                    .phone("555-0102")
//                    .active(true)
//                    .build());
//
//            System.out.println("✅ Initial users created in database");
//        }
//    }
//}