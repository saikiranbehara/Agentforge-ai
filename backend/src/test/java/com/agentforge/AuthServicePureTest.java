package com.agentforge;

import com.agentforge.dto.AuthRequest;
import com.agentforge.dto.AuthResponse;
import com.agentforge.dto.RegisterRequest;
import com.agentforge.entity.User;
import com.agentforge.exception.BadRequestException;
import com.agentforge.repository.UserRepository;
import com.agentforge.security.JwtTokenProvider;
import com.agentforge.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServicePureTest {

    private InMemoryUserRepository userRepository;
    private SimplePasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        passwordEncoder = new SimplePasswordEncoder();
        tokenProvider = new JwtTokenProvider(
                "MyVerySecureAgentForgeSuperSecretKeyForJwtSigning1234567890",
                3600000L
        );
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider);
    }

    @Test
    void testRegisterAndLoginWorkflow() {
        RegisterRequest register = new RegisterRequest("Alex Rivers", "alex@agentforge.ai", "secretPass123");
        AuthResponse registered = authService.register(register);

        assertNotNull(registered);
        assertNotNull(registered.getToken());
        assertEquals("Alex Rivers", registered.getName());
        assertEquals("alex@agentforge.ai", registered.getEmail());

        // Attempt login
        AuthRequest login = new AuthRequest("alex@agentforge.ai", "secretPass123");
        AuthResponse loggedIn = authService.login(login);

        assertNotNull(loggedIn);
        assertNotNull(loggedIn.getToken());
        assertEquals(registered.getId(), loggedIn.getId());
    }

    @Test
    void testDuplicateRegister_ThrowsException() {
        RegisterRequest register = new RegisterRequest("Alex Rivers", "alex@agentforge.ai", "secretPass123");
        authService.register(register);

        assertThrows(BadRequestException.class, () -> authService.register(register));
    }

    // Lightweight in-memory test repository
    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Long, User> store = new HashMap<>();
        private long seq = 1L;

        @Override
        public Optional<User> findByEmail(String email) {
            return store.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return store.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        }

        @Override
        public <S extends User> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(seq++);
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<User> findById(Long aLong) {
            return Optional.ofNullable(store.get(aLong));
        }

        @Override public boolean existsById(Long aLong) { return store.containsKey(aLong); }
        @Override public List<User> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<User> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long aLong) { store.remove(aLong); }
        @Override public void delete(User entity) { store.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends User> entities) {}
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<User> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public User getOne(Long aLong) { return store.get(aLong); }
        @Override public User getById(Long aLong) { return store.get(aLong); }
        @Override public User getReferenceById(Long aLong) { return store.get(aLong); }
        @Override public <S extends User> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends User> List<S> findAll(Example<S> example) { return List.of(); }
        @Override public <S extends User> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
        @Override public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
        @Override public <S extends User> long count(Example<S> example) { return 0; }
        @Override public <S extends User> boolean exists(Example<S> example) { return false; }
        @Override public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public List<User> findAll(Sort sort) { return List.of(); }
        @Override public Page<User> findAll(Pageable pageable) { return Page.empty(); }
    }

    private static class SimplePasswordEncoder implements PasswordEncoder {
        @Override public String encode(CharSequence rawPassword) { return "hashed:" + rawPassword; }
        @Override public boolean matches(CharSequence rawPassword, String encodedPassword) { return ("hashed:" + rawPassword).equals(encodedPassword); }
    }
}
