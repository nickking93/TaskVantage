package com.taskvantage.backend.service;

import com.taskvantage.backend.dto.RecommendationResponse;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RecommendationServiceTest {

    private RecommendationService recommendationService;
    private TaskRepository taskRepositoryMock;

    @BeforeEach
    public void setUp() {
        taskRepositoryMock = Mockito.mock(TaskRepository.class);
        recommendationService = new RecommendationService(taskRepositoryMock);
    }

    @Nested
    class DayOfWeekTests {
        @Test
        void testRecommendationsForSameWeekday() {
            Long userId = 1L;
            DayOfWeek today = ZonedDateTime.now(ZoneOffset.UTC).getDayOfWeek();

            // Create historical tasks completed on the same weekday
            Task historicalTask = new Task(1L, "Weekly Report", "Create weekly status report");
            historicalTask.setUserId(userId);
            historicalTask.setStatus("Completed");
            historicalTask.setCompletionDateTime(
                    ZonedDateTime.now(ZoneOffset.UTC).minusWeeks(1)
                            .with(today));  // Set to same day last week

            // Create candidate task scheduled for the same weekday
            Task candidateTask = new Task(2L, "Team Meeting", "Weekly team sync");
            candidateTask.setScheduledStart(
                    ZonedDateTime.now(ZoneOffset.UTC).plusDays(7)
                            .with(today));  // Set to same day next week

            when(taskRepositoryMock.findRecentTasksByUserId(userId))
                    .thenReturn(List.of(historicalTask));
            when(taskRepositoryMock.findPotentialTasksForUser(userId))
                    .thenReturn(List.of(candidateTask));

            List<Task> recommendations = recommendationService.getRecommendationsForUser(userId, 1);

            assertEquals(1, recommendations.size());
            assertEquals("Team Meeting", recommendations.get(0).getTitle(),
                    "Should recommend task scheduled for same weekday");
            assertTrue(Boolean.TRUE.equals(recommendations.get(0).getRecommended()));
        }

        @Test
        void testDayBoostFactors() {
            Task task = new Task();
            DayOfWeek today = ZonedDateTime.now(ZoneOffset.UTC).getDayOfWeek();

            // Test scheduled start boost
            task.setScheduledStart(ZonedDateTime.now(ZoneOffset.UTC));
            assertEquals(2.0, recommendationService.computeDayOfWeekBoost(task, today),
                    "Should apply 2.0x boost for scheduled tasks on same day");

            // Test completion day boost
            task.setScheduledStart(null);
            task.setCompletionDateTime(ZonedDateTime.now(ZoneOffset.UTC));
            assertEquals(1.8, recommendationService.computeDayOfWeekBoost(task, today),
                    "Should apply 1.8x boost for completed tasks on same day");

            // Test different day
            task.setCompletionDateTime(ZonedDateTime.now(ZoneOffset.UTC).plusDays(1));
            assertEquals(1.0, recommendationService.computeDayOfWeekBoost(task, today),
                    "Should not boost tasks on different days");
        }
    }

    @Nested
    class RecencyWeightTests {
        @Test
        void testCalculateRecencyWeight_RecentTask() {
            Task task = new Task();
            task.setCreationDate(ZonedDateTime.now().minusDays(1));
            task.setLastModifiedDate(ZonedDateTime.now().minusHours(2));

            double weight = recommendationService.calculateRecencyWeight(task);

            assertTrue(weight > 0.95, "Recent task should have weight close to 1.0");
        }

        @Test
        void testCalculateRecencyWeight_OldTask() {
            Task task = new Task();
            task.setCreationDate(ZonedDateTime.now().minusDays(60));
            task.setLastModifiedDate(ZonedDateTime.now().minusDays(60));

            double weight = recommendationService.calculateRecencyWeight(task);

            assertTrue(weight < 0.1, "Old task should have low weight");
            assertTrue(weight > 0, "Weight should be positive");
        }

        @Test
        void testCalculateRecencyWeight_UsesMostRecentTimestamp() {
            Task task = new Task();
            task.setCreationDate(ZonedDateTime.now().minusDays(30));
            task.setLastModifiedDate(ZonedDateTime.now().minusDays(15));
            task.setCompletionDateTime(ZonedDateTime.now().minusDays(2)); // More recent

            double weight = recommendationService.calculateRecencyWeight(task);

            assertTrue(weight > 0.8, "Should use most recent timestamp (completion date)");
        }
    }

    @Test
    public void testGetRecommendationsForUser_WithHistory() {
        Long userId = 1L;
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        DayOfWeek today = now.getDayOfWeek();

        // Create user's historical tasks - both on the same weekday
        Task task1 = new Task(1L, "API Documentation", "Write API documentation");
        task1.setUserId(userId);
        task1.setStatus("Completed");
        task1.setCompletionDateTime(now.minusWeeks(1).with(today)); // Same weekday last week
        task1.setLastModifiedDate(task1.getCompletionDateTime());

        Task task2 = new Task(2L, "Database Schema", "Create database documentation");
        task2.setUserId(userId);
        task2.setStatus("Completed");
        task2.setCompletionDateTime(now.minusWeeks(2).with(today)); // Same weekday two weeks ago
        task2.setLastModifiedDate(task2.getCompletionDateTime());

        List<Task> userHistory = List.of(task1, task2);

        // Create candidate tasks for recommendations
        Task task3 = new Task(3L, "Update API Docs", "Review and update API documentation");
        task3.setScheduledStart(now); // Set to today, explicitly same day

        Task task4 = new Task(4L, "Code Review", "Review pull requests");
        task4.setScheduledStart(now.plusDays(1)); // Different day

        Task task5 = new Task(5L, "Buy Groceries", "Get weekly groceries");
        task5.setScheduledStart(now.plusDays(2)); // Different day

        List<Task> candidateTasks = List.of(task3, task4, task5);

        // Mock repository responses
        when(taskRepositoryMock.findRecentTasksByUserId(userId)).thenReturn(userHistory);
        when(taskRepositoryMock.findPotentialTasksForUser(userId)).thenReturn(candidateTasks);

        // Get recommendations
        List<Task> recommendations = recommendationService.getRecommendationsForUser(userId, 2);

        // Verify results
        assertEquals(2, recommendations.size(), "Should return requested number of recommendations");
        assertEquals("Update API Docs", recommendations.get(0).getTitle(),
                "First recommendation should be most aligned with schedule");
    }

    @Test
    public void testGetRecommendationsForUser_NoHistory() {
        Long userId = 1L;

        // Mock empty user history
        when(taskRepositoryMock.findRecentTasksByUserId(userId)).thenReturn(List.of());

        // Mock popular tasks
        Task task1 = new Task(1L, "Get Started", "Complete onboarding tasks");
        Task task2 = new Task(2L, "First Project", "Begin your first project");

        List<Task> popularTasks = List.of(task1, task2);
        when(taskRepositoryMock.findPopularTasks(anyInt())).thenReturn(popularTasks);

        List<Task> recommendations = recommendationService.getRecommendationsForUser(userId, 2);

        assertEquals(2, recommendations.size(), "Should return default recommendations");
        assertEquals("Get Started", recommendations.get(0).getTitle(),
                "Should return onboarding tasks for new users");
    }

    @Test
    void testTaskBasedRecommendationsAreRuleBased() {
        Long userId = 1L;
        Long taskId = 10L;

        Task targetTask = new Task(taskId, "Plan sprint", "Organize sprint meeting");
        targetTask.setUserId(userId);

        Task relatedTask = new Task(11L, "Sprint retro", "Review sprint outcomes");
        relatedTask.setScheduledStart(ZonedDateTime.now(ZoneOffset.UTC));

        when(taskRepositoryMock.findById(taskId)).thenReturn(Optional.of(targetTask));
        when(taskRepositoryMock.findRecentTasksByUserId(userId)).thenReturn(List.of(targetTask));
        when(taskRepositoryMock.findRelatedTasks(taskId, userId, targetTask.getTitle(), targetTask.getDescription()))
                .thenReturn(List.of(relatedTask));

        RecommendationResponse response = recommendationService.getRecommendedTasks(userId, taskId, 2);

        assertEquals("success", response.getStatus());
        assertEquals(1, response.getRecommendations().size());
        Task recommended = response.getRecommendations().get(0);
        assertTrue(Boolean.TRUE.equals(recommended.getRecommended()));
        assertNotNull(recommended.getRecommendationScore());
    }
}
