package com.payme.tests.service;

import com.authmat.application.Application;
//import com.payme.authentication.service.token.JwtService;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SpringBootTest(classes = Application.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class JwtAuthenticationServiceTests {
//
//    @Autowired private JwtAuthenticationService jwtAuthenticationService;
//    @MockBean private AuthenticationManager authenticationManager;
//    @MockBean private UserRepository userRepository;
//    @MockBean private JwtService jwtService;
//    @MockBean private BCryptPasswordEncoder passwordEncoder;
//    @MockBean private TokenRepository tokenRepository;
//    @MockBean private RoleRepository roleRepository;
//
//    private MockedUsers mockedUsers = new MockedUsers();
//    private RegistrationRequest dto = new RegistrationRequest("John", "Doe", "testUser", "test@example.com", "password");
//    private LoginRequest loginDto = new LoginRequest("JohnDoe", "password");
//    private User user = mockedUsers.getMockedUsers().get(0);
//
//
//    @Test
//    void testRegister() {
//        Role mockRole = new Role(1L, DefaultRoles.USER);
//
//        when(userRepository.isCredentialTaken(anyString(), anyString()))
//                .thenReturn(false);
//        when(userRepository.save(any(User.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//        when(passwordEncoder.encode(anyString()))
//                .thenReturn("encodedPassword");
//        when(roleRepository.findByRole(any(DefaultRoles.class)))
//                .thenReturn(Optional.of(mockRole));
//
//        jwtAuthenticationService.register(dto);
//
//        verify(userRepository, times(1)).save(any(User.class));
//    }
//
//    @Test
//    void testRegisterWithDuplicateCredentials() {
//        when(userRepository.isCredentialTaken(eq("testUser"), eq("test@example.com"))).thenThrow(DuplicateCredentialException.class);
//
//        assertThrows(DuplicateCredentialException.class, () -> jwtAuthenticationService.register(dto));
//
//        verify(userRepository, times(0)).save(any(User.class));
//    }
//
//    @Test
//    void testInvalidCredentialsLogin() {
//        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
//                .thenThrow(new BadCredentialsException("Could not find user: " + loginDto.usernameOrEmail()));
//
//        assertThrows(BadCredentialsException.class, () -> jwtAuthenticationService.login(loginDto));
//
//        verify(authenticationManager, times(1))
//                .authenticate(any(UsernamePasswordAuthenticationToken.class));
//        verify(userRepository, never())
//                .findByUsernameOrEmail(anyString());
//    }
//
//    @Test
//    void testValidCredentialsLogin(){
//        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
//                .thenReturn(new UsernamePasswordAuthenticationToken(user, loginDto.password()));
//        when(tokenRepository.findAllByUserId(any(UUID.class)))
//                .thenReturn(new ArrayList<>());
//        when(jwtService.generateToken(any(User.class)))
//                .thenReturn("mockToken");
//
//        jwtAuthenticationService.login(loginDto);
//
//        verify(authenticationManager, times(1))
//                .authenticate(any(UsernamePasswordAuthenticationToken.class));
//        verify(tokenRepository, times(1))
//                .findAllByUserId(any(UUID.class));
//        verify(tokenRepository, times(1))
//                .save(any(Token.class));
//    }

}
