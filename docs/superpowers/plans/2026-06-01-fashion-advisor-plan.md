# Fashion Advisor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a web app for clothing recognition (DJL) and smart outfit suggestion with rule-based color harmony.

**Architecture:** Spring Boot 3.2 + Thymeleaf + MySQL, DJL for image classification, custom K-Means color analysis, rule-based suggestion engine.

**Tech Stack:** Java 21, Spring Boot 3.2.4, MySQL, DJL 0.28.0 (PyTorch), JWT auth, Lombok

**Location:** `C:\Users\Public\fashion-advisor` (new project, separate from money-transfer)

---

### Task 1: Project Scaffold

**Files:**
- Create: `C:\Users\Public\fashion-advisor\pom.xml`
- Create: `C:\Users\Public\fashion-advisor\src\main\java\com\fashionadvisor\FashionAdvisorApplication.java`
- Create: `C:\Users\Public\fashion-advisor\src\main\resources\application.yml`
- Create: `C:\Users\Public\fashion-advisor\src\test\resources\application.yml`
- Create: `C:\Users\Public\fashion-advisor\schema.sql`

- [ ] **Step 1: Create project directory structure**

Run:
```powershell
New-Item -ItemType Directory -Path "C:\Users\Public\fashion-advisor\src\main\java\com\fashionadvisor\config" -Force
New-Item -ItemType Directory -Path "C:\Users\Public\fashion-advisor\src\main\java\com\fashionadvisor\auth" -Force
New-Item -ItemType Directory -Path "C:\Users\Public\fashion-advisor\src\main\java\com\fashionadvisor\user" -Force
New-Item -ItemType Directory -Path "C:\Users\Public\fashion-advisor\src\main\java\com\fashionadvisor\clothing" -Force
New-Item -ItemType Directory -Path "C:\Users\Public\fashion-advisor\src\main\java\com\fashionadvisor\outfit" -Force
New-Item -ItemType Directory -Path "C:\Users\Public\fashion-advisor\src\main\java\com\fashionadvisor\recognition" -Force
New-Item -ItemType Directory -Path "C:\Users\Public\fashion-advisor\src\main\java\com\fashionadvisor\web" -Force
New-Item -ItemType Directory -Path "C:\Users\Public\fashion-advisor\src\main\resources\templates" -Force
New-Item -ItemType Directory -Path "C:\Users\Public\fashion-advisor\src\main\resources\static\css" -Force
New-Item -ItemType Directory -Path "C:\Users\Public\fashion-advisor\src\main\resources\static\js" -Force
New-Item -ItemType Directory -Path "C:\Users\Public\fashion-advisor\src\test\java\com\fashionadvisor" -Force
New-Item -ItemType Directory -Path "C:\Users\Public\fashion-advisor\docs\superpowers\specs" -Force
```

- [ ] **Step 2: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.4</version>
    </parent>
    <groupId>com.fashionadvisor</groupId>
    <artifactId>fashion-advisor</artifactId>
    <version>1.0.0</version>
    <name>fashion-advisor</name>

    <properties>
        <java.version>21</java.version>
        <djl.version>0.28.0</djl.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <dependency>
            <groupId>org.thymeleaf.extras</groupId>
            <artifactId>thymeleaf-extras-springsecurity6</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>ai.djl</groupId>
            <artifactId>api</artifactId>
            <version>${djl.version}</version>
        </dependency>
        <dependency>
            <groupId>ai.djl.pytorch</groupId>
            <artifactId>pytorch-engine</artifactId>
            <version>${djl.version}</version>
        </dependency>
        <dependency>
            <groupId>ai.djl.pytorch</groupId>
            <artifactId>pytorch-model-zoo</artifactId>
            <version>${djl.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Create `application.yml`**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fashion_advisor?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  thymeleaf:
    cache: false

app:
  jwt:
    secret: fashion-advisor-jwt-secret-key-for-development-only-2026
    access-token-expiration: 900000
    refresh-token-expiration: 604800000

logging:
  level:
    com.fashionadvisor: DEBUG
```

- [ ] **Step 4: Create test `application.yml`**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  thymeleaf:
    cache: false

app:
  jwt:
    secret: test-secret-key-for-fashion-advisor-testing-2026
    access-token-expiration: 900000
    refresh-token-expiration: 604800000

logging:
  level:
    com.fashionadvisor: WARN
```

- [ ] **Step 5: Create `schema.sql`**

```sql
CREATE DATABASE IF NOT EXISTS fashion_advisor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fashion_advisor;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    email VARCHAR(100),
    role VARCHAR(20) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clothing_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    image_base64 LONGTEXT NOT NULL,
    item_type VARCHAR(30) NOT NULL,
    color_name VARCHAR(50),
    color_hex VARCHAR(7),
    pattern VARCHAR(30) DEFAULT 'SOLID',
    season VARCHAR(20) DEFAULT 'ALL',
    occasion VARCHAR(50) DEFAULT 'CASUAL',
    is_favorite BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS outfits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100),
    occasion VARCHAR(50),
    season VARCHAR(20),
    is_favorite BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS outfit_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    outfit_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    FOREIGN KEY (outfit_id) REFERENCES outfits(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES clothing_items(id) ON DELETE CASCADE
);
```

- [ ] **Step 6: Create `FashionAdvisorApplication.java`**

```java
package com.fashionadvisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FashionAdvisorApplication {
    public static void main(String[] args) {
        SpringApplication.run(FashionAdvisorApplication.class, args);
    }
}
```

- [ ] **Step 7: Verify Maven compiles**

Run:
```powershell
cd C:\Users\Public\fashion-advisor
& "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" compile -q
```
Expected: BUILD SUCCESS (Maven warnings only)

- [ ] **Step 8: Commit**

```powershell
git init
git add -A
git commit -m "feat: scaffold Spring Boot project with DJL, JWT, MySQL"
```

---

### Task 2: User Entity + Auth (JWT)

**Files:**
- Create: `src/main/java/com/fashionadvisor/user/User.java`
- Create: `src/main/java/com/fashionadvisor/user/UserRepository.java`
- Create: `src/main/java/com/fashionadvisor/auth/JwtUtil.java`
- Create: `src/main/java/com/fashionadvisor/auth/JwtAuthFilter.java`
- Create: `src/main/java/com/fashionadvisor/auth/AuthService.java`
- Create: `src/main/java/com/fashionadvisor/auth/AuthController.java`
- Create: `src/main/java/com/fashionadvisor/auth/TokenBlacklistService.java`
- Create: `src/main/java/com/fashionadvisor/config/SecurityConfig.java`
- Create: `src/test/java/com/fashionadvisor/auth/JwtUtilTest.java`

- [ ] **Step 1: Create `User.java`**

```java
package com.fashionadvisor.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name")
    private String fullName;

    private String email;

    @Column(length = 20)
    private String role = "USER";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public User(String username, String password, String fullName, String email) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: Create `UserRepository.java`**

```java
package com.fashionadvisor.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

- [ ] **Step 3: Create `JwtUtil.java`**

```java
package com.fashionadvisor.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long accessExpiration;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.access-token-expiration}") long accessExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
    }

    public String generateAccessToken(Long userId, String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(secretKey)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

- [ ] **Step 4: Create `JwtAuthFilter.java`**

```java
package com.fashionadvisor.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    public JwtAuthFilter(JwtUtil jwtUtil, TokenBlacklistService blacklistService) {
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            if (blacklistService.isBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            try {
                Claims claims = jwtUtil.validateToken(token);
                String role = claims.get("role", String.class);
                List<SimpleGrantedAuthority> authorities = role != null
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        : List.of();
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                auth.setDetails(claims);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
```

- [ ] **Step 5: Create `TokenBlacklistService.java`**

```java
package com.fashionadvisor.auth;

import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklistService {
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public void blacklist(String token) { blacklist.add(token); }
    public boolean isBlacklisted(String token) { return blacklist.contains(token); }
}
```

- [ ] **Step 6: Create `SecurityConfig.java`**

```java
package com.fashionadvisor.config;

import com.fashionadvisor.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
                .requestMatchers("/login", "/register").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form.loginPage("/login").permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login").permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 7: Create `AuthService.java`**

```java
package com.fashionadvisor.auth;

import com.fashionadvisor.user.User;
import com.fashionadvisor.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, TokenBlacklistService blacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
    }

    public Map<String, Object> register(String username, String password, String fullName, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = new User(username, passwordEncoder.encode(password), fullName, email);
        userRepository.save(user);
        String token = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        return Map.of("token", token, "userId", user.getId(), "username", user.getUsername());
    }

    public Map<String, Object> login(String username, String password, HttpServletResponse response) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String token = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(900);
        response.addCookie(cookie);
        return Map.of("token", token, "userId", user.getId(), "username", user.getUsername(), "role", user.getRole());
    }

    public void logout(String token, HttpServletResponse response) {
        if (token != null) blacklistService.blacklist(token);
        Cookie cookie = new Cookie("access_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
```

- [ ] **Step 8: Create `AuthController.java`**

```java
package com.fashionadvisor.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            var result = authService.register(
                    body.get("username"), body.get("password"),
                    body.get("fullName"), body.get("email"));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletResponse response) {
        try {
            var result = authService.login(body.get("username"), body.get("password"), response);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractToken(request);
        authService.logout(token, response);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) return cookie.getValue();
            }
        }
        return null;
    }
}
```

- [ ] **Step 9: Create test `JwtUtilTest.java`**

```java
package com.fashionadvisor.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private final JwtUtil jwtUtil = new JwtUtil("test-secret-key-for-unit-test-at-least-32-chars!!", 900000);

    @Test
    void generateAndValidateToken() {
        String token = jwtUtil.generateAccessToken(1L, "testuser", "USER");
        assertNotNull(token);
        Claims claims = jwtUtil.validateToken(token);
        assertEquals("testuser", claims.getSubject());
        assertEquals(1, claims.get("userId", Integer.class));
        assertEquals("USER", claims.get("role"));
    }

    @Test
    void invalidTokenThrows() {
        assertThrows(Exception.class, () -> jwtUtil.validateToken("invalid.token.here"));
    }
}
```

- [ ] **Step 10: Compile and test**

Run:
```powershell
cd C:\Users\Public\fashion-advisor
& "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" test -q
```
Expected: BUILD SUCCESS, 1 test class, 2 tests passing

- [ ] **Step 11: Commit**

```powershell
git add -A
git commit -m "feat: add user entity and JWT authentication"
```

---

### Task 3: Clothing Item CRUD

**Files:**
- Create: `src/main/java/com/fashionadvisor/clothing/ClothingItem.java`
- Create: `src/main/java/com/fashionadvisor/clothing/ClothingItemRepository.java`
- Create: `src/main/java/com/fashionadvisor/clothing/ClothingService.java`
- Create: `src/main/java/com/fashionadvisor/clothing/ClothingController.java`
- Create: `src/test/java/com/fashionadvisor/clothing/ClothingServiceTest.java`

- [ ] **Step 1: Create `ClothingItem.java`**

```java
package com.fashionadvisor.clothing;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clothing_items")
public class ClothingItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "image_base64", nullable = false, columnDefinition = "LONGTEXT")
    private String imageBase64;

    @Column(name = "item_type", nullable = false, length = 30)
    private String itemType;

    @Column(name = "color_name", length = 50)
    private String colorName;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(length = 30)
    private String pattern = "SOLID";

    @Column(length = 20)
    private String season = "ALL";

    @Column(length = 50)
    private String occasion = "CASUAL";

    @Column(name = "is_favorite")
    private boolean favorite = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getColorName() { return colorName; }
    public void setColorName(String colorName) { this.colorName = colorName; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public String getOccasion() { return occasion; }
    public void setOccasion(String occasion) { this.occasion = occasion; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 2: Create `ClothingItemRepository.java`**

```java
package com.fashionadvisor.clothing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClothingItemRepository extends JpaRepository<ClothingItem, Long> {
    List<ClothingItem> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ClothingItem> findByUserIdAndItemType(Long userId, String itemType);
    List<ClothingItem> findByUserIdAndOccasion(Long userId, String occasion);
    List<ClothingItem> findByUserIdAndSeason(Long userId, String season);
}
```

- [ ] **Step 3: Create `ClothingService.java`**

```java
package com.fashionadvisor.clothing;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClothingService {
    private final ClothingItemRepository repository;

    public ClothingService(ClothingItemRepository repository) {
        this.repository = repository;
    }

    public ClothingItem addItem(Long userId, String imageBase64, String itemType,
                                 String colorName, String colorHex, String pattern,
                                 String season, String occasion) {
        ClothingItem item = new ClothingItem();
        item.setUserId(userId);
        item.setImageBase64(imageBase64);
        item.setItemType(itemType);
        item.setColorName(colorName);
        item.setColorHex(colorHex);
        item.setPattern(pattern != null ? pattern : "SOLID");
        item.setSeason(season != null ? season : "ALL");
        item.setOccasion(occasion != null ? occasion : "CASUAL");
        return repository.save(item);
    }

    public List<ClothingItem> getUserItems(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public ClothingItem updateItem(Long id, Long userId, String itemType, String colorName,
                                    String colorHex, String pattern, String season,
                                    String occasion, boolean favorite) {
        ClothingItem item = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        if (!item.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied");
        }
        if (itemType != null) item.setItemType(itemType);
        if (colorName != null) item.setColorName(colorName);
        if (colorHex != null) item.setColorHex(colorHex);
        if (pattern != null) item.setPattern(pattern);
        if (season != null) item.setSeason(season);
        if (occasion != null) item.setOccasion(occasion);
        item.setFavorite(favorite);
        return repository.save(item);
    }

    public void deleteItem(Long id, Long userId) {
        ClothingItem item = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        if (!item.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied");
        }
        repository.delete(item);
    }
}
```

- [ ] **Step 4: Create `ClothingController.java`**

```java
package com.fashionadvisor.clothing;

import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clothing")
public class ClothingController {
    private final ClothingService clothingService;

    public ClothingController(ClothingService clothingService) {
        this.clothingService = clothingService;
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof Claims claims)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return ((Integer) claims.get("userId")).longValue();
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(Authentication auth, @RequestBody Map<String, String> body) {
        try {
            Long userId = getUserId(auth);
            ClothingItem item = clothingService.addItem(userId,
                    body.get("imageBase64"), body.get("itemType"),
                    body.get("colorName"), body.get("colorHex"),
                    body.get("pattern"), body.get("season"), body.get("occasion"));
            return ResponseEntity.ok(Map.of("id", item.getId(), "message", "Item added"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/items")
    public ResponseEntity<?> getItems(Authentication auth) {
        try {
            Long userId = getUserId(auth);
            return ResponseEntity.ok(clothingService.getUserItems(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<?> updateItem(Authentication auth, @PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        try {
            Long userId = getUserId(auth);
            ClothingItem item = clothingService.updateItem(id, userId,
                    body.get("itemType"), body.get("colorName"), body.get("colorHex"),
                    body.get("pattern"), body.get("season"), body.get("occasion"),
                    Boolean.parseBoolean(body.getOrDefault("favorite", "false")));
            return ResponseEntity.ok(Map.of("message", "Item updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<?> deleteItem(Authentication auth, @PathVariable Long id) {
        try {
            Long userId = getUserId(auth);
            clothingService.deleteItem(id, userId);
            return ResponseEntity.ok(Map.of("message", "Item deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
```

- [ ] **Step 5: Create test `ClothingServiceTest.java`**

```java
package com.fashionadvisor.clothing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ClothingServiceTest {

    @Autowired private ClothingService clothingService;
    @Autowired private ClothingItemRepository repository;

    @Test
    void addAndGetItems() {
        ClothingItem item = clothingService.addItem(1L, "base64img", "SHIRT",
                "Blue", "#0000ff", "SOLID", "ALL", "CASUAL");
        assertNotNull(item.getId());
        assertEquals("SHIRT", item.getItemType());

        List<ClothingItem> items = clothingService.getUserItems(1L);
        assertFalse(items.isEmpty());
        assertEquals("Blue", items.get(0).getColorName());
    }

    @Test
    void deleteItem() {
        ClothingItem item = clothingService.addItem(1L, "img", "SHIRT",
                "Red", "#ff0000", "SOLID", "ALL", "CASUAL");
        clothingService.deleteItem(item.getId(), 1L);
        assertTrue(repository.findById(item.getId()).isEmpty());
    }
}
```

- [ ] **Step 6: Compile and test**

```powershell
cd C:\Users\Public\fashion-advisor
& "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" test -q
```
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```powershell
git add -A
git commit -m "feat: add clothing item CRUD"
```

---

### Task 4: Outfit CRUD

**Files:**
- Create: `src/main/java/com/fashionadvisor/outfit/Outfit.java`
- Create: `src/main/java/com/fashionadvisor/outfit/OutfitItem.java`
- Create: `src/main/java/com/fashionadvisor/outfit/OutfitRepository.java`
- Create: `src/main/java/com/fashionadvisor/outfit/OutfitItemRepository.java`
- Create: `src/main/java/com/fashionadvisor/outfit/OutfitService.java`
- Create: `src/main/java/com/fashionadvisor/outfit/OutfitController.java`

- [ ] **Step 1: Create `Outfit.java`**

```java
package com.fashionadvisor.outfit;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "outfits")
public class Outfit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String name;

    @Column(length = 50)
    private String occasion;

    @Column(length = 20)
    private String season;

    @Column(name = "is_favorite")
    private boolean favorite = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Transient
    private List<ClothingItemInfo> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOccasion() { return occasion; }
    public void setOccasion(String occasion) { this.occasion = occasion; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ClothingItemInfo> getItems() { return items; }
    public void setItems(List<ClothingItemInfo> items) { this.items = items; }

    public static class ClothingItemInfo {
        private Long id;
        private String itemType;
        private String colorName;
        private String colorHex;
        private String imageBase64;

        public ClothingItemInfo() {}
        public ClothingItemInfo(Long id, String itemType, String colorName, String colorHex, String imageBase64) {
            this.id = id; this.itemType = itemType; this.colorName = colorName;
            this.colorHex = colorHex; this.imageBase64 = imageBase64;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        public String getColorName() { return colorName; }
        public void setColorName(String colorName) { this.colorName = colorName; }
        public String getColorHex() { return colorHex; }
        public void setColorHex(String colorHex) { this.colorHex = colorHex; }
        public String getImageBase64() { return imageBase64; }
        public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    }
}
```

- [ ] **Step 2: Create `OutfitItem.java`**

```java
package com.fashionadvisor.outfit;

import jakarta.persistence.*;

@Entity
@Table(name = "outfit_items")
public class OutfitItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outfit_id", nullable = false)
    private Long outfitId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    public OutfitItem() {}
    public OutfitItem(Long outfitId, Long itemId) { this.outfitId = outfitId; this.itemId = itemId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOutfitId() { return outfitId; }
    public void setOutfitId(Long outfitId) { this.outfitId = outfitId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
}
```

- [ ] **Step 3: Create `OutfitRepository.java`**

```java
package com.fashionadvisor.outfit;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutfitRepository extends JpaRepository<Outfit, Long> {
    List<Outfit> findByUserIdOrderByCreatedAtDesc(Long userId);
}
```

- [ ] **Step 4: Create `OutfitItemRepository.java`**

```java
package com.fashionadvisor.outfit;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutfitItemRepository extends JpaRepository<OutfitItem, Long> {
    List<OutfitItem> findByOutfitId(Long outfitId);
    void deleteByOutfitId(Long outfitId);
}
```

- [ ] **Step 5: Create `OutfitService.java`**

```java
package com.fashionadvisor.outfit;

import com.fashionadvisor.clothing.ClothingItem;
import com.fashionadvisor.clothing.ClothingItemRepository;
import com.fashionadvisor.outfit.Outfit.ClothingItemInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OutfitService {
    private final OutfitRepository outfitRepository;
    private final OutfitItemRepository outfitItemRepository;
    private final ClothingItemRepository clothingRepository;

    public OutfitService(OutfitRepository outfitRepository, OutfitItemRepository outfitItemRepository,
                         ClothingItemRepository clothingRepository) {
        this.outfitRepository = outfitRepository;
        this.outfitItemRepository = outfitItemRepository;
        this.clothingRepository = clothingRepository;
    }

    @Transactional
    public Outfit createOutfit(Long userId, String name, String occasion, String season, List<Long> itemIds) {
        Outfit outfit = new Outfit();
        outfit.setUserId(userId);
        outfit.setName(name);
        outfit.setOccasion(occasion);
        outfit.setSeason(season);
        outfit = outfitRepository.save(outfit);

        for (Long itemId : itemIds) {
            outfitItemRepository.save(new OutfitItem(outfit.getId(), itemId));
        }
        return loadItems(outfit);
    }

    public List<Outfit> getUserOutfits(Long userId) {
        return outfitRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::loadItems)
                .collect(Collectors.toList());
    }

    public Outfit getOutfit(Long id, Long userId) {
        Outfit outfit = outfitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outfit not found"));
        if (!outfit.getUserId().equals(userId)) throw new IllegalArgumentException("Access denied");
        return loadItems(outfit);
    }

    private Outfit loadItems(Outfit outfit) {
        List<OutfitItem> ois = outfitItemRepository.findByOutfitId(outfit.getId());
        List<ClothingItemInfo> infos = ois.stream()
                .map(oi -> clothingRepository.findById(oi.getItemId()))
                .filter(opt -> opt.isPresent())
                .map(opt -> {
                    ClothingItem ci = opt.get();
                    return new ClothingItemInfo(ci.getId(), ci.getItemType(),
                            ci.getColorName(), ci.getColorHex(), ci.getImageBase64());
                })
                .collect(Collectors.toList());
        outfit.setItems(infos);
        return outfit;
    }
}
```

- [ ] **Step 6: Create `OutfitController.java`**

```java
package com.fashionadvisor.outfit;

import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/outfits")
public class OutfitController {
    private final OutfitService outfitService;

    public OutfitController(OutfitService outfitService) { this.outfitService = outfitService; }

    private Long getUserId(Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof Claims claims))
            throw new IllegalArgumentException("Not authenticated");
        return ((Integer) claims.get("userId")).longValue();
    }

    @PostMapping
    public ResponseEntity<?> createOutfit(Authentication auth, @RequestBody Map<String, Object> body) {
        try {
            Long userId = getUserId(auth);
            String name = (String) body.get("name");
            String occasion = (String) body.get("occasion");
            String season = (String) body.get("season");
            @SuppressWarnings("unchecked")
            List<Integer> itemIds = ((List<Integer>) body.get("itemIds"));
            Outfit outfit = outfitService.createOutfit(userId, name, occasion, season,
                    itemIds.stream().map(Long::valueOf).collect(Collectors.toList()));
            return ResponseEntity.ok(Map.of("id", outfit.getId(), "message", "Outfit created"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getOutfits(Authentication auth) {
        try { return ResponseEntity.ok(outfitService.getUserOutfits(getUserId(auth)));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOutfit(Authentication auth, @PathVariable Long id) {
        try { return ResponseEntity.ok(outfitService.getOutfit(id, getUserId(auth)));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
```

- [ ] **Step 7: Compile and test**

```powershell
cd C:\Users\Public\fashion-advisor
& "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" test -q
```
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```powershell
git add -A
git commit -m "feat: add outfit CRUD"
```

---

### Task 5: DJL Clothing Recognition & Color Analysis

**Files:**
- Create: `src/main/java/com/fashionadvisor/recognition/DjlConfig.java`
- Create: `src/main/java/com/fashionadvisor/recognition/ClothingRecognitionService.java`
- Create: `src/main/java/com/fashionadvisor/recognition/ColorAnalysisService.java`
- Create: `src/main/java/com/fashionadvisor/recognition/RecognitionController.java`

- [ ] **Step 1: Create `DjlConfig.java`**

```java
package com.fashionadvisor.recognition;

import ai.djl.Application;
import ai.djl.ModelZoo;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DjlConfig {
    private static final Logger log = LoggerFactory.getLogger(DjlConfig.class);

    @Bean(destroyMethod = "close")
    public ZooModel<Image, Classifications> classificationModel() throws Exception {
        log.info("Loading DJL classification model (ResNet50)...");
        Criteria<Image, Classifications> criteria = Criteria.builder()
                .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                .setTypes(Image.class, Classifications.class)
                .optFilter("backbone", "resnet50")
                .build();
        ZooModel<Image, Classifications> model = ModelZoo.loadModel(criteria);
        log.info("DJL model loaded successfully");
        return model;
    }
}
```

- [ ] **Step 2: Create `ClothingRecognitionService.java`**

```java
package com.fashionadvisor.recognition;

import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.ZooModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.*;

@Service
public class ClothingRecognitionService {
    private static final Logger log = LoggerFactory.getLogger(ClothingRecognitionService.class);

    private final ZooModel<Image, Classifications> model;

    private static final Map<String, String> LABEL_MAP = new LinkedHashMap<>();
    static {
        LABEL_MAP.put("shirt", "SHIRT"); LABEL_MAP.put("suit", "SHIRT");
        LABEL_MAP.put("trouser", "PANTS"); LABEL_MAP.put("jean", "PANTS");
        LABEL_MAP.put("shorts", "PANTS"); LABEL_MAP.put("skirt", "SKIRT");
        LABEL_MAP.put("dress", "DRESS"); LABEL_MAP.put("bra", "DRESS");
        LABEL_MAP.put("shoe", "SHOES"); LABEL_MAP.put("sneaker", "SHOES");
        LABEL_MAP.put("boot", "SHOES"); LABEL_MAP.put("sandal", "SHOES");
        LABEL_MAP.put("jacket", "JACKET"); LABEL_MAP.put("coat", "JACKET");
        LABEL_MAP.put("hoodie", "JACKET"); LABEL_MAP.put("hat", "ACCESSORY");
        LABEL_MAP.put("backpack", "ACCESSORY"); LABEL_MAP.put("bag", "ACCESSORY");
        LABEL_MAP.put("tie", "ACCESSORY"); LABEL_MAP.put("belt", "ACCESSORY");
    }

    private static final List<String> LABELS = List.of(
            "SHIRT", "PANTS", "SKIRT", "DRESS", "SHOES", "JACKET", "ACCESSORY");

    public ClothingRecognitionService(ZooModel<Image, Classifications> model) {
        this.model = model;
    }

    public String classify(byte[] imageBytes) {
        try {
            Image img = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imageBytes));
            Classifications result = model.newPredictor().predict(img);
            String bestLabel = result.best().getClassName().toLowerCase().trim();
            log.info("DJL classified as: {}", bestLabel);

            for (var entry : LABEL_MAP.entrySet()) {
                if (bestLabel.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return "SHIRT";
        } catch (Exception e) {
            log.error("Classification failed", e);
            return "SHIRT";
        }
    }
}
```

- [ ] **Step 3: Create `ColorAnalysisService.java`**

```java
package com.fashionadvisor.recognition;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.List;

@Service
public class ColorAnalysisService {

    private static final Map<String, String> COLOR_MAP = new LinkedHashMap<>();
    static {
        COLOR_MAP.put("do", "#FF0000"); COLOR_MAP.put("hong", "#FF69B4");
        COLOR_MAP.put("cam", "#FFA500"); COLOR_MAP.put("vang", "#FFD700");
        COLOR_MAP.put("xanh la", "#008000"); COLOR_MAP.put("xanh duong", "#0000FF");
        COLOR_MAP.put("tim", "#800080"); COLOR_MAP.put("nau", "#8B4513");
        COLOR_MAP.put("den", "#000000"); COLOR_MAP.put("trang", "#FFFFFF");
        COLOR_MAP.put("xam", "#808080"); COLOR_MAP.put("be", "#F5F5DC");
    }

    public Map<String, String> analyze(byte[] imageBytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) return Map.of("colorName", "Unknown", "colorHex", "#CCCCCC");

            List<Color> pixels = new ArrayList<>();
            int step = Math.max(1, Math.min(img.getWidth(), img.getHeight()) / 50);
            for (int x = 0; x < img.getWidth(); x += step) {
                for (int y = 0; y < img.getHeight(); y += step) {
                    pixels.add(new Color(img.getRGB(x, y)));
                }
            }

            Color dominant = kMeans(pixels, 5);
            String hex = String.format("#%02x%02x%02x", dominant.getRed(), dominant.getGreen(), dominant.getBlue());
            String name = findColorName(dominant);

            return Map.of("colorName", name, "colorHex", hex);
        } catch (Exception e) {
            return Map.of("colorName", "Unknown", "colorHex", "#CCCCCC");
        }
    }

    private Color kMeans(List<Color> pixels, int k) {
        Random rand = new Random(42);
        List<Color> centroids = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            Color c = pixels.get(rand.nextInt(pixels.size()));
            centroids.add(new Color(c.getRGB()));
        }

        int[] counts = new int[k];
        int[] sumR = new int[k], sumG = new int[k], sumB = new int[k];

        for (int iter = 0; iter < 10; iter++) {
            Arrays.fill(counts, 0);
            Arrays.fill(sumR, 0); Arrays.fill(sumG, 0); Arrays.fill(sumB, 0);

            for (Color p : pixels) {
                int best = 0;
                double bestDist = Double.MAX_VALUE;
                for (int i = 0; i < k; i++) {
                    double d = colorDistance(p, centroids.get(i));
                    if (d < bestDist) { bestDist = d; best = i; }
                }
                counts[best]++; sumR[best] += p.getRed();
                sumG[best] += p.getGreen(); sumB[best] += p.getBlue();
            }

            for (int i = 0; i < k; i++) {
                if (counts[i] > 0) {
                    centroids.set(i, new Color(sumR[i] / counts[i], sumG[i] / counts[i], sumB[i] / counts[i]));
                }
            }
        }

        int largest = 0;
        for (int i = 1; i < k; i++) {
            if (counts[i] > counts[largest]) largest = i;
        }
        return centroids.get(largest);
    }

    private double colorDistance(Color a, Color b) {
        int dr = a.getRed() - b.getRed();
        int dg = a.getGreen() - b.getGreen();
        int db = a.getBlue() - b.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    private String findColorName(Color c) {
        String bestName = "Unknown";
        double bestDist = Double.MAX_VALUE;
        for (var entry : COLOR_MAP.entrySet()) {
            Color cc = Color.decode(entry.getValue());
            double d = colorDistance(c, cc);
            if (d < bestDist) { bestDist = d; bestName = entry.getKey(); }
        }
        return bestName;
    }
}
```

- [ ] **Step 4: Create `RecognitionController.java`**

```java
package com.fashionadvisor.recognition;

import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/clothing")
public class RecognitionController {
    private final ClothingRecognitionService recognitionService;
    private final ColorAnalysisService colorAnalysisService;

    public RecognitionController(ClothingRecognitionService recognitionService,
                                  ColorAnalysisService colorAnalysisService) {
        this.recognitionService = recognitionService;
        this.colorAnalysisService = colorAnalysisService;
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof Claims claims))
            throw new IllegalArgumentException("Not authenticated");
        return ((Integer) claims.get("userId")).longValue();
    }

    @PostMapping("/recognize")
    public ResponseEntity<?> recognize(Authentication auth, @RequestBody Map<String, String> body) {
        try {
            getUserId(auth);
            String base64 = body.get("imageBase64");
            if (base64 == null) return ResponseEntity.badRequest().body(Map.of("error", "imageBase64 required"));

            byte[] imageBytes = Base64.getDecoder().decode(base64);
            String itemType = recognitionService.classify(imageBytes);
            Map<String, String> colorInfo = colorAnalysisService.analyze(imageBytes);

            return ResponseEntity.ok(Map.of(
                    "itemType", itemType,
                    "colorName", colorInfo.get("colorName"),
                    "colorHex", colorInfo.get("colorHex"),
                    "message", "AI suggested: " + itemType + ", color: " + colorInfo.get("colorName")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
```

- [ ] **Step 5: Compile**

```powershell
cd C:\Users\Public\fashion-advisor
& "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```powershell
git add -A
git commit -m "feat: add DJL clothing recognition and K-Means color analysis"
```

---

### Task 6: Outfit Suggestion Service

**Files:**
- Create: `src/main/java/com/fashionadvisor/outfit/OutfitSuggestionService.java`
- Modify: `src/main/java/com/fashionadvisor/outfit/OutfitController.java` (add suggest endpoint)

- [ ] **Step 1: Create `OutfitSuggestionService.java`**

```java
package com.fashionadvisor.outfit;

import com.fashionadvisor.clothing.ClothingItem;
import com.fashionadvisor.clothing.ClothingItemRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OutfitSuggestionService {
    private final ClothingItemRepository clothingRepository;

    public OutfitSuggestionService(ClothingItemRepository clothingRepository) {
        this.clothingRepository = clothingRepository;
    }

    public List<List<ClothingItem>> suggest(Long userId, String occasion, String season) {
        List<ClothingItem> all = clothingRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<ClothingItem> tops = filter(all, List.of("SHIRT", "DRESS"));
        List<ClothingItem> bottoms = filter(all, List.of("PANTS", "SKIRT"));
        List<ClothingItem> shoes = filter(all, List.of("SHOES"));
        List<ClothingItem> jackets = filter(all, List.of("JACKET"));
        List<ClothingItem> accessories = filter(all, List.of("ACCESSORY"));

        if (occasion != null) {
            tops = filterByOccasion(tops, occasion);
            bottoms = filterByOccasion(bottoms, occasion);
            shoes = filterByOccasion(shoes, occasion);
        }
        if (season != null) {
            tops = filterBySeason(tops, season);
            bottoms = filterBySeason(bottoms, season);
            shoes = filterBySeason(shoes, season);
        }

        List<List<ClothingItem>> suggestions = new ArrayList<>();
        for (ClothingItem top : tops) {
            for (ClothingItem bottom : bottoms) {
                for (ClothingItem shoe : shoes) {
                    double score = colorHarmonyScore(top, bottom)
                            + colorHarmonyScore(top, shoe)
                            + colorHarmonyScore(bottom, shoe);
                    suggestions.add(List.of(top, bottom, shoe));
                }
                if (!jackets.isEmpty()) {
                    ClothingItem jacket = jackets.get(0);
                    double score = colorHarmonyScore(top, bottom)
                            + colorHarmonyScore(top, jacket);
                    suggestions.add(List.of(jacket, top, bottom, shoes.isEmpty() ? null : shoes.get(0)));
                }
                if (!accessories.isEmpty()) {
                    ClothingItem acc = accessories.get(0);
                    suggestions.add(List.of(top, bottom, acc));
                }
            }
        }

        suggestions.sort((a, b) -> Double.compare(
                scoreSuggestion(b, occasion, season),
                scoreSuggestion(a, occasion, season)));

        return suggestions.stream().limit(20).collect(Collectors.toList());
    }

    private List<ClothingItem> filter(List<ClothingItem> items, List<String> types) {
        return items.stream()
                .filter(i -> types.contains(i.getItemType()))
                .collect(Collectors.toList());
    }

    private List<ClothingItem> filterByOccasion(List<ClothingItem> items, String occasion) {
        List<ClothingItem> matched = items.stream()
                .filter(i -> occasion.equalsIgnoreCase(i.getOccasion()))
                .collect(Collectors.toList());
        return matched.isEmpty() ? items : matched;
    }

    private List<ClothingItem> filterBySeason(List<ClothingItem> items, String season) {
        List<ClothingItem> matched = items.stream()
                .filter(i -> season.equalsIgnoreCase(i.getSeason()) || "ALL".equalsIgnoreCase(i.getSeason()))
                .collect(Collectors.toList());
        return matched.isEmpty() ? items : matched;
    }

    private double colorHarmonyScore(ClothingItem a, ClothingItem b) {
        if (a == null || b == null) return 0;
        Color ca = parseColor(a.getColorHex());
        Color cb = parseColor(b.getColorHex());
        if (ca == null || cb == null) return 0.5;

        float[] hsvA = Color.RGBtoHSB(ca.getRed(), ca.getGreen(), ca.getBlue(), null);
        float[] hsvB = Color.RGBtoHSB(cb.getRed(), cb.getGreen(), cb.getBlue(), null);

        float dh = Math.abs(hsvA[0] - hsvB[0]);
        dh = Math.min(dh, 1 - dh);

        if (dh < 0.05f) return 0.8;   // monochrome
        if (dh < 0.10f) return 0.7;   // analogous
        if (dh > 0.45f && dh < 0.55f) return 0.9; // complementary
        if (dh > 0.20f && dh < 0.40f) return 0.6;
        return 0.3;
    }

    private double scoreSuggestion(List<ClothingItem> items, String occasion, String season) {
        double score = 0;
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                score += colorHarmonyScore(items.get(i), items.get(j));
            }
        }
        return score;
    }

    private Color parseColor(String hex) {
        try {
            if (hex != null && hex.startsWith("#")) return Color.decode(hex);
            return null;
        } catch (Exception e) { return null; }
    }
}
```

- [ ] **Step 2: Add suggest endpoint to `OutfitController.java`**

Add before the closing brace of `OutfitController`:
```java
    @PostMapping("/suggest")
    public ResponseEntity<?> suggest(Authentication auth, @RequestBody Map<String, String> body) {
        try {
            Long userId = getUserId(auth);
            String occasion = body.get("occasion");
            String season = body.get("season");
            List<List<ClothingItem>> suggestions = outfitSuggestionService.suggest(userId, occasion, season);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
```

Add field and constructor param to `OutfitController`:
```java
    private final OutfitSuggestionService outfitSuggestionService;

    public OutfitController(OutfitService outfitService, OutfitSuggestionService outfitSuggestionService) {
        this.outfitService = outfitService;
        this.outfitSuggestionService = outfitSuggestionService;
    }
```

- [ ] **Step 3: Compile**

```powershell
cd C:\Users\Public\fashion-advisor
& "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```powershell
git add -A
git commit -m "feat: add outfit suggestion service with color harmony algorithm"
```

---

### Task 7: Thymeleaf Web Pages

**Files:**
- Create: `src/main/java/com/fashionadvisor/web/PageController.java`
- Create: `src/main/resources/templates/fragments/layout.html`
- Create: `src/main/resources/templates/login.html`
- Create: `src/main/resources/templates/register.html`
- Create: `src/main/resources/templates/dashboard.html`
- Create: `src/main/resources/templates/wardrobe.html`
- Create: `src/main/resources/templates/upload.html`
- Create: `src/main/resources/templates/outfits.html`
- Create: `src/main/resources/static/css/style.css`
- Create: `src/main/resources/static/js/app.js`

- [ ] **Step 1: Create `PageController.java`**

```java
package com.fashionadvisor.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/register") public String register() { return "register"; }
    @GetMapping("/dashboard") public String dashboard() { return "dashboard"; }
    @GetMapping("/wardrobe") public String wardrobe() { return "wardrobe"; }
    @GetMapping("/upload") public String upload() { return "upload"; }
    @GetMapping("/outfits") public String outfits() { return "outfits"; }
}
```

- [ ] **Step 2: Create `fragments/layout.html`**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:fragment="head(title)">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title}">Fashion Advisor</title>
    <link rel="stylesheet" th:href="@{/css/style.css}">
</head>
<body th:fragment="body">
    <div th:replace="~{fragments/layout :: navbar}"></div>
    <div class="container">
        <div th:replace="~{fragments/layout :: sidebar}"></div>
        <main class="main-content">
            <div th:replace="~{fragments/layout :: content}"></div>
        </main>
    </div>
    <script th:src="@{/js/app.js}"></script>
</body>
</html>
```

**Template files reference:** All templates follow the banking-style UI pattern from the money-transfer project (`layout.html` with fragments, responsive CSS grid, card-based layout). Key templates:

- `login.html` — form + face login button (disabled until face feature added)
- `register.html` — registration form
- `dashboard.html` — stats cards + recent items table
- `wardrobe.html` — grid of clothing cards with filter/sort
- `upload.html` — drag-drop upload → AI analyze → confirm/save
- `outfits.html` — suggestion results + saved outfits grid

**Note for implementer:** Copy the `banking.css` and `banking.js` patterns from the money-transfer project as base. Adjust colors to a purple/pink fashion theme instead of blue.

- [ ] **Step 8: Compile**

```powershell
cd C:\Users\Public\fashion-advisor
& "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```powershell
git add -A
git commit -m "feat: add Thymeleaf pages for all features"
```

---

### Task 8: Integration Test

**Files:**
- Create: `src/test/java/com/fashionadvisor/FashionAdvisorApplicationTests.java`

- [ ] **Step 1: Create test**

```java
package com.fashionadvisor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FashionAdvisorApplicationTests {
    @Test
    void contextLoads() {}
}
```

- [ ] **Step 2: Run all tests**

```powershell
cd C:\Users\Public\fashion-advisor
& "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" test
```
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 3: Final commit**

```powershell
git add -A
git commit -m "chore: add integration test and finalize"
```
