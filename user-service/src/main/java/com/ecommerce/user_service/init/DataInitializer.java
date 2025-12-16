//package com.ecommerce.user_service.init;
//
//import com.ecommerce.user_service.model.User;
//import com.ecommerce.user_service.repository.UserRepository;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class DataInitializer {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @PostConstruct
//    public void init() {
//        // Add some initial users to database if using JPA
//        if (userRepository.count() == 0) {
//
//            // User 1: John Doe
//            User john = User.builder()
//                    .username("john_doe")
//                    .email("john@example.com")
//                    .password(passwordEncoder.encode("password123")) // Encrypt password
//                    .firstName("John")
//                    .lastName("Doe")  // ← Use lastName, NOT fullName
//                    .address("123 Main St")
//                    .phoneNumber("555-0101")
//                    .city("New York")
//                    .state("NY")
//                    .country("USA")
//                    .zipCode("10001")
//                    .enabled(true)
//                    .emailVerified(true)
//                    .build();
//
//            // User 2: Jane Smith
//            User jane = User.builder()
//                    .username("jane_smith")
//                    .email("jane@example.com")
//                    .password(passwordEncoder.encode("password123"))
//                    .firstName("Jane")
//                    .lastName("Smith")  // ← Use lastName, NOT fullName
//                    .address("456 Oak Ave")
//                    .phoneNumber("555-0102")
//                    .city("Los Angeles")
//                    .state("CA")
//                    .country("USA")
//                    .zipCode("90001")
//                    .enabled(true)
//                    .emailVerified(true)
//                    .build();
//
//            // User 3: Bob Wilson
//            User bob = User.builder()
//                    .username("bob_wilson")
//                    .email("bob@example.com")
//                    .password(passwordEncoder.encode("password123"))
//                    .firstName("Bob")
//                    .lastName("Wilson")  // ← Use lastName, NOT fullName
//                    .address("789 Pine Rd")
//                    .phoneNumber("555-0103")
//                    .city("Chicago")
//                    .state("IL")
//                    .country("USA")
//                    .zipCode("60601")
//                    .enabled(true)
//                    .emailVerified(true)
//                    .build();
//
//            userRepository.save(john);
//            userRepository.save(jane);
//            userRepository.save(bob);
//
//            System.out.println("✅ Initial users created in database");
//            System.out.println("   - John Doe (username: john_doe, password: password123)");
//            System.out.println("   - Jane Smith (username: jane_smith, password: password123)");
//            System.out.println("   - Bob Wilson (username: bob_wilson, password: password123)");
//        }
//    }
//}