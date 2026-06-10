package com.moneytransfer.user;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private com.moneytransfer.auth.JwtAuthFilter jwtAuthFilter;

    private Claims mockClaims(Long userId) {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.get("userId")).thenReturn(userId.intValue());
        return claims;
    }

    private UsernamePasswordAuthenticationToken createAuth(String username, Long userId) {
        Claims claims = mockClaims(userId);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null);
        auth.setDetails(claims);
        return auth;
    }

    @Test
    void getProfile_avatarUrlNull_returnsEmptyString() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setUsername("user2");
        user.setEmail("user2@example.com");
        user.setFullName("User Two");
        user.setAvatarUrl(null);
        when(userService.findById(2L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/profile").principal(createAuth("user2", 2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(""));
    }

    @Test
    void getProfile_userNotFound_returns404() throws Exception {
        when(userService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/profile").principal(createAuth("nonexistent", 99L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProfile_returnsAvatarUrl() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("user1");
        user.setEmail("user1@example.com");
        user.setFullName("User One");
        user.setAvatarUrl("/uploads/avatars/user1.png");
        when(userService.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/profile").principal(createAuth("user1", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("/uploads/avatars/user1.png"));
    }

    @Test
    void uploadAvatar_acceptsValidImageAndReturnsUrl() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("user1");
        when(userService.findById(1L)).thenReturn(Optional.of(user));

        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "avatar.png", "image/png", new byte[]{1, 2, 3});

        when(userService.updateAvatar(1L, avatar)).thenReturn("/uploads/avatars/user1_avatar.png");

        mockMvc.perform(multipart("/api/users/avatar").file(avatar).principal(createAuth("user1", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("/uploads/avatars/user1_avatar.png"));
    }

    @Test
    void uploadAvatar_noFile_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/users/avatar").principal(createAuth("user1", 1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Avatar file is required"));
    }

    @Test
    void uploadAvatar_nonImage_returnsBadRequest() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "test.txt", "text/plain", new byte[]{1, 2, 3});
        when(userService.updateAvatar(1L, avatar)).thenThrow(new IllegalArgumentException("Avatar must be an image"));

        mockMvc.perform(multipart("/api/users/avatar").file(avatar).principal(createAuth("user1", 1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Avatar must be an image"));
    }

    @Test
    void uploadAvatar_fileTooLarge_returnsBadRequest() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "large.png", "image/png", new byte[]{1, 2, 3});
        when(userService.updateAvatar(1L, avatar)).thenThrow(new IllegalArgumentException("Avatar must be smaller than 5MB"));

        mockMvc.perform(multipart("/api/users/avatar").file(avatar).principal(createAuth("user1", 1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Avatar must be smaller than 5MB"));
    }

    @Test
    void uploadAvatar_userNotFound_returnsBadRequest() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "avatar.png", "image/png", new byte[]{1, 2, 3});
        when(userService.updateAvatar(1L, avatar)).thenThrow(new IllegalArgumentException("User not found"));

        mockMvc.perform(multipart("/api/users/avatar").file(avatar).principal(createAuth("user1", 1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void uploadAvatar_serviceError_returnsInternalServerError() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "avatar.png", "image/png", new byte[]{1, 2, 3});
        when(userService.updateAvatar(1L, avatar)).thenThrow(new RuntimeException("Failed to save avatar file"));

        mockMvc.perform(multipart("/api/users/avatar").file(avatar).principal(createAuth("user1", 1L)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to upload avatar"));
    }
}
