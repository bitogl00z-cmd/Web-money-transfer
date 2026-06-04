package com.moneytransfer.face;

import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaceServiceTest {

    @Mock private JavaCVFaceEngine javaCVFaceEngine;
    @Mock private UserRepository userRepository;
    private FaceService faceService;

    @BeforeEach
    void setUp() {
        faceService = new FaceService(javaCVFaceEngine, userRepository, Runnable::run);
    }

    @Test
    void testRegisterFaceSync_Success() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String result = faceService.registerFaceSync(1L, "dGVzdA==");

        assertEquals("Face registered successfully", result);
        assertTrue(user.isFaceEnabled());
        verify(userRepository).save(user);
    }

    @Test
    void testRegisterFaceSync_UserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> faceService.registerFaceSync(99L, "dGVzdA=="));
    }

    @Test
    void testVerifyFaceBase64_Success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setFaceEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(javaCVFaceEngine.verifyFace(eq(1L), any())).thenReturn(true);

        CompletableFuture<Boolean> future = faceService.verifyFaceBase64(1L, "dGVzdA==");

        assertTrue(future.get());
    }

    @Test
    void testVerifyFaceBase64_NoFaceRegistered() {
        User user = new User();
        user.setId(1L);
        user.setFaceEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> faceService.verifyFaceBase64(1L, "dGVzdA=="));
    }
}
