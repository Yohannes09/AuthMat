//package com.payme.tests.repository;
//
//import com.payme.authentication.AuthenticationApplication;
//import com.payme.authentication.repository.UserRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//import com.payme.tests.MockedUsers;
//
//@Slf4j
//@SpringBootTest(classes = AuthenticationApplication.class)
//@ActiveProfiles("test")
//public class UserRepositoryTests {
//
//    @Autowired
//    private UserRepository securityUserRepository;
//
//    @BeforeEach
//    void setUp(){
//        var mockedUsers = new MockedUsers();
//
//        log.info("Beginning setup process.");
//        securityUserRepository.saveAll(mockedUsers.getMockedUsers());
//        log.info("Setup complete. ");
//    }
//
//    @AfterEach
//    void rollBack(){
//        log.info("Beginning  rollback. ");
//        securityUserRepository.deleteAll();
//        log.info("Rollback complete. ");
//    }
//
//    @Test
//    void testFindByUsernameOrEmail(){
//        assertTrue(securityUserRepository.findByUsernameOrEmail("JohnDoe").isPresent());
//        assertTrue(securityUserRepository.findByUsernameOrEmail("JohnDoe@example.com").isPresent());
//    }
//
//    @Test
//    void testFindByUsernameOrEmailInvalidUser(){
//        assertFalse(securityUserRepository.findByUsernameOrEmail("Arthur").isPresent());
//    }
//
//    @Test
//    void testIsCredentialTaken(){
//        log.info("Running isCredentialTaken() test ");
//        // Email is different but username is taken.
//        assertTrue(securityUserRepository.isUsernameOrEmailTaken(
//                "Johndoe", "spring@example.com")
//        );
//
//        assertFalse(securityUserRepository.isUsernameOrEmailTaken(
//                "Java", "java@example.com")
//        );
//        log.info("isCredentialTaken() test completed ");
//    }
//
//}
