package com.moneytransfer.user;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void getProfile_returnsAvatarUrl() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("user1");
        user.setEmail("user1@example.com");
        user.setFullName("User One");
        user.setAvatarUrl("/uploads/avatars/user1.png");
        when(userService.findById(1L)).thenReturn(Optional.of(user));

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.get("userId")).thenReturn(1);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user1", null);
        auth.setDetails(claims);

        mockMvc.perform(get("/api/users/profile").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("/uploads/avatars/user1.png"));
    }
}
