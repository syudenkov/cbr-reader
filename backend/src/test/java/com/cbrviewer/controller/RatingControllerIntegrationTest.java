package com.cbrviewer.controller;

import com.cbrviewer.dto.RatingRequest;
import com.cbrviewer.model.ComicFile;
import com.cbrviewer.model.Rating;
import com.cbrviewer.model.User;
import com.cbrviewer.repository.ComicFileRepository;
import com.cbrviewer.repository.RatingRepository;
import com.cbrviewer.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RatingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User testUser;
    private User otherUser;
    private Long testFileId = 1L;
    private MockHttpSession authenticatedSession;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User(
            "testuser",
            "test@example.com",
            passwordEncoder.encode("password123"),
            "USER"
        );
        testUser = userRepository.save(testUser);

        otherUser = new User(
            "otheruser",
            "other@example.com",
            passwordEncoder.encode("password123"),
            "USER"
        );
        otherUser = userRepository.save(otherUser);

        // Create authenticated session
        authenticatedSession = new MockHttpSession();
        authenticatedSession.setAttribute("userId", testUser.getId());
        authenticatedSession.setAttribute("username", testUser.getUsername());
        authenticatedSession.setAttribute("role", testUser.getRole());
    }

    @Nested
    @DisplayName("POST /api/ratings/{fileId}")
    class SubmitRatingTests {

        @Test
        @DisplayName("should submit rating successfully")
        void shouldSubmitRating() throws Exception {
            RatingRequest request = new RatingRequest(5);

            mockMvc.perform(post("/api/ratings/{fileId}", testFileId)
                    .with(csrf())
                    .session(authenticatedSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating", is(5)))
                .andExpect(jsonPath("$.fileId", is(testFileId.intValue())));
        }

        @Test
        @DisplayName("should update existing rating")
        void shouldUpdateExistingRating() throws Exception {
            // Submit initial rating
            Rating initialRating = new Rating(testUser.getId(), testFileId, 3);
            ratingRepository.save(initialRating);

            // Update rating
            RatingRequest request = new RatingRequest(5);

            mockMvc.perform(post("/api/ratings/{fileId}", testFileId)
                    .with(csrf())
                    .session(authenticatedSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating", is(5)));
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void shouldRejectUnauthenticated() throws Exception {
            RatingRequest request = new RatingRequest(5);

            mockMvc.perform(post("/api/ratings/{fileId}", testFileId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/ratings/{fileId}/average")
    class GetAverageRatingTests {

        @Test
        @DisplayName("should return null average when no ratings")
        void shouldReturnNullAverageWhenNoRatings() throws Exception {
            mockMvc.perform(get("/api/ratings/{fileId}/average", testFileId)
                    .session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId", is(testFileId.intValue())))
                .andExpect(jsonPath("$.averageRating").doesNotExist())
                .andExpect(jsonPath("$.totalRatings", is(0)));
        }

        @Test
        @DisplayName("should return correct average for multiple ratings")
        void shouldReturnCorrectAverage() throws Exception {
            // Add ratings from different users
            ratingRepository.save(new Rating(testUser.getId(), testFileId, 5));
            ratingRepository.save(new Rating(otherUser.getId(), testFileId, 3));

            mockMvc.perform(get("/api/ratings/{fileId}/average", testFileId)
                    .session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId", is(testFileId.intValue())))
                .andExpect(jsonPath("$.averageRating", is(4.0)))
                .andExpect(jsonPath("$.totalRatings", is(2)));
        }

        @Test
        @DisplayName("should require authentication")
        void shouldRequireAuthentication() throws Exception {
            mockMvc.perform(get("/api/ratings/{fileId}/average", testFileId))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/ratings/{fileId}")
    class GetUserRatingTests {

        @Test
        @DisplayName("should return user rating when exists")
        void shouldReturnUserRating() throws Exception {
            ratingRepository.save(new Rating(testUser.getId(), testFileId, 4));

            mockMvc.perform(get("/api/ratings/{fileId}", testFileId)
                    .session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating", is(4)))
                .andExpect(jsonPath("$.fileId", is(testFileId.intValue())));
        }

        @Test
        @DisplayName("should return null when user has not rated")
        void shouldReturnNullWhenNotRated() throws Exception {
            mockMvc.perform(get("/api/ratings/{fileId}", testFileId)
                    .session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/ratings/{fileId}", testFileId))
                .andExpect(status().isForbidden());
        }
    }
}
