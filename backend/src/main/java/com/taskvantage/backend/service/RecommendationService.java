package com.taskvantage.backend.service;

import com.taskvantage.backend.dto.RecommendationResponse;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    private static final double MAX_DAY_BOOST = 2.0; // Increased for more impact
    private static final double BASE_DAY_BOOST = 1.0;
    private static final int FREQUENCY_THRESHOLD_HIGH = 5; // Tasks done 5+ times are considered highly frequent
    private static final int FREQUENCY_THRESHOLD_MEDIUM = 3; // Tasks done 3-4 times are considered moderately frequent
    private static final float DEFAULT_POPULAR_SCORE = 0.35f;

    private static final String REASON_WEEKDAY_MATCH = "WEEKDAY_MATCH";
    private static final String REASON_SIMILAR_CONTENT = "SIMILAR_CONTENT";
    private static final String REASON_POPULAR = "POPULAR";

    private record RecommendationReason(String code, String text) {}

    private final TaskRepository taskRepository;

    @Autowired
    public RecommendationService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
        logger.debug("RecommendationService initialized");
    }

    public List<Task> getRecommendationsForUser(Long userId, int limit) {
        logger.debug("Generating recommendations for user ID: {}, limit: {}", userId, limit);

        List<Task> userRecentTasks = taskRepository.findRecentTasksByUserId(userId);
        logger.debug("Found {} recent tasks for user ID {}", userRecentTasks.size(), userId);

        if (userRecentTasks.isEmpty()) {
            logger.debug("No recent tasks found for user ID {}. Returning default recommendations.", userId);
            return getDefaultRecommendations(limit);
        }

        try {
            DayOfWeek currentDayOfWeek = ZonedDateTime.now(ZoneOffset.UTC).getDayOfWeek();
            List<Task> candidateTasks = taskRepository.findPotentialTasksForUser(userId);

            if (candidateTasks.isEmpty()) {
                logger.debug("No candidate tasks for user ID {}. Falling back to default recommendations.", userId);
                return getDefaultRecommendations(limit);
            }

            // Group tasks by title and description to avoid duplicates
            Map<String, List<Task>> groupedTasks = candidateTasks.stream()
                    .collect(Collectors.groupingBy(task -> task.getTitle().toLowerCase() + "::" + task.getDescription().toLowerCase()));

            // Score and rank tasks
            List<Task> scoredTasks = groupedTasks.values().stream()
                    .map(group -> scoreTask(group.get(0), currentDayOfWeek, userRecentTasks))
                    .sorted((t1, t2) -> Float.compare(t2.getRecommendationScore(), t1.getRecommendationScore()))
                    .collect(Collectors.toList());

            List<Task> recommendations = prioritizeWeekdayMatch(scoredTasks, limit);

            if (recommendations.isEmpty()) {
                logger.debug("No personalized recommendations generated for user ID {}. Returning default recommendations.", userId);
                return getDefaultRecommendations(limit);
            }

            logger.debug("Successfully generated {} recommendations for user ID {}", recommendations.size(), userId);
            return recommendations;

        } catch (Exception e) {
            logger.error("Error generating recommendations for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    double computeDayOfWeekBoost(Task task, DayOfWeek targetDay) {
        // Check scheduled start day if available
        if (task.getScheduledStart() != null &&
                task.getScheduledStart().getDayOfWeek() == targetDay) {
            return MAX_DAY_BOOST; // Highest boost for explicitly scheduled tasks
        }

        // Check completion history
        if (task.getCompletionDateTime() != null &&
                task.getCompletionDateTime().getDayOfWeek() == targetDay) {
            return MAX_DAY_BOOST - 0.2; // Slightly lower boost for historical pattern
        }

        // Default case - no day-based boost
        return BASE_DAY_BOOST;
    }

    double calculateFrequencyWeight(Task task, List<Task> userHistory) {
        // Count exact matches (same title and description)
        long exactMatches = userHistory.stream()
                .filter(historyTask ->
                        historyTask.getTitle().equalsIgnoreCase(task.getTitle()) &&
                                historyTask.getDescription().equalsIgnoreCase(task.getDescription()))
                .count();

        // Apply tiered frequency weighting
        if (exactMatches >= FREQUENCY_THRESHOLD_HIGH) {
            return 3.0; // High frequency weight
        } else if (exactMatches >= FREQUENCY_THRESHOLD_MEDIUM) {
            return 2.0; // Medium frequency weight
        } else {
            return 1.0 + (exactMatches * 0.2); // Linear scaling for lower frequencies
        }
    }

    double calculateRecencyWeight(Task task) {
        ZonedDateTime mostRecentTimestamp = Stream.of(
                        task.getLastModifiedDate(),
                        task.getCompletionDateTime(),
                        task.getCreationDate()
                )
                .filter(Objects::nonNull)
                .max(ZonedDateTime::compareTo)
                .orElse(task.getCreationDate());

        double ageInDays = Duration.between(mostRecentTimestamp, ZonedDateTime.now(ZoneOffset.UTC)).toDays();

        // Exponential decay with 14-day half-life (more aggressive than before)
        double halfLife = 14.0;
        double lambda = Math.log(2) / halfLife;
        double weight = Math.exp(-lambda * ageInDays);

        logger.trace("Calculated recency weight {} for task {} (age: {} days)", weight, task.getId(), ageInDays);
        return weight;
    }

    public RecommendationResponse getRecommendedTasksByWeekday(Long userId, int limit) {
        logger.debug("Getting weekday recommendations for user ID: {}, limit: {}", userId, limit);

        RecommendationResponse response = new RecommendationResponse();
        try {
            // Use getRecommendationsForUser with the user's ID and limit
            List<Task> recommendations = getRecommendationsForUser(userId, limit);

            // Prepare response
            response.setRecommendations(recommendations);
            response.setStatus("success");
            response.setMessage("Recommendations for the current weekday fetched successfully.");
            logger.debug("Weekday recommendations generated successfully for user ID: {}", userId);
        } catch (Exception e) {
            logger.error("Error generating weekday recommendations: {}", e.getMessage(), e);
            response.setStatus("error");
            response.setMessage("Failed to generate weekday recommendations: " + e.getMessage());
            response.setRecommendations(Collections.emptyList());
        }

        return response;
    }

    private List<Task> getDefaultRecommendations(int limit) {
        logger.debug("Fetching default recommendations with limit {}", limit);
        try {
            List<Task> popularTasks = taskRepository.findPopularTasks(limit);
            logger.debug("Retrieved {} default recommendations", popularTasks.size());
            return applyDefaultRecommendationMetadata(
                    popularTasks,
                    new RecommendationReason(REASON_POPULAR, "Popular with other TaskVantage users")
            );
        } catch (Exception e) {
            logger.error("Error fetching default recommendations: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public RecommendationResponse getRecommendedTasks(Long userId, Long taskId, int limit) {
        logger.debug("Getting recommended tasks for user ID: {}, taskId: {}, limit: {}", userId, taskId, limit);

        RecommendationResponse response = new RecommendationResponse();

        try {
            List<Task> recommendations;

            if (taskId != null) {
                // Task-based recommendations
                Task targetTask = taskRepository.findById(taskId).orElseThrow(() -> {
                    logger.error("Task not found with ID: {}", taskId);
                    return new IllegalArgumentException("Task not found for ID: " + taskId);
                });

                List<Task> userRecentTasks = taskRepository.findRecentTasksByUserId(userId);
                DayOfWeek currentDayOfWeek = ZonedDateTime.now(ZoneOffset.UTC).getDayOfWeek();
                List<Task> relatedTasks = taskRepository.findRelatedTasks(
                        taskId, userId, targetTask.getTitle(), targetTask.getDescription());

                List<Task> scoredRelatedTasks = relatedTasks.stream()
                        .map(task -> scoreTask(task, currentDayOfWeek, userRecentTasks))
                        .sorted((t1, t2) -> Float.compare(t2.getRecommendationScore(), t1.getRecommendationScore()))
                        .collect(Collectors.toList());

                recommendations = prioritizeWeekdayMatch(scoredRelatedTasks, limit);
            } else {
                recommendations = getRecommendationsForUser(userId, limit);
            }

            if (recommendations.isEmpty()) {
                logger.debug("No recommendations generated for request. Returning default recommendations instead.");
                recommendations = getDefaultRecommendations(limit);
            }

            response.setRecommendations(recommendations);
            response.setStatus("success");
            response.setMessage("Recommendations fetched successfully.");
            logger.debug("Successfully generated recommendations response with {} items", recommendations.size());

        } catch (Exception e) {
            logger.error("Error generating recommendations: {}", e.getMessage(), e);
            response.setStatus("error");
            response.setMessage("Failed to fetch recommendations: " + e.getMessage());
            response.setRecommendations(Collections.emptyList());
        }

        return response;
    }

    private Task scoreTask(Task task, DayOfWeek currentDayOfWeek, List<Task> userRecentTasks) {
        double dayBoost = computeDayOfWeekBoost(task, currentDayOfWeek);
        double recencyWeight = calculateRecencyWeight(task);
        double frequencyWeight = calculateFrequencyWeight(task, userRecentTasks);

        double dayComponent = (dayBoost > BASE_DAY_BOOST) ? 0.4 : 0.1;
        double frequencyComponent = Math.min(0.3, (frequencyWeight - 1.0) * 0.1);
        double recencyComponent = Math.min(0.15, recencyWeight * 0.5);

        double calculatedScore;
        if (dayBoost > BASE_DAY_BOOST && frequencyWeight > 2.0) {
            calculatedScore = 0.85 + (frequencyComponent * 0.1) + (recencyComponent * 0.05);
        } else if (dayBoost > BASE_DAY_BOOST) {
            calculatedScore = 0.65 + (frequencyComponent * 0.15) + (recencyComponent * 0.1);
        } else if (frequencyWeight > 2.0) {
            calculatedScore = 0.45 + (frequencyComponent * 0.2) + (recencyComponent * 0.15);
        } else {
            calculatedScore = 0.25 + (frequencyComponent * 0.3) + (recencyComponent * 0.2);
        }

        float finalScore = clampScore(calculatedScore);
        RecommendationReason reason = buildReason(dayBoost, frequencyWeight, currentDayOfWeek);

        logger.debug(
                "Score breakdown for task '{}': dayBoost={}, frequencyWeight={}, recencyWeight={}, finalScore={}, reason={}",
                task.getTitle(),
                dayBoost,
                frequencyWeight,
                recencyWeight,
                finalScore,
                reason.text()
        );

        applyRecommendationMetadata(task, finalScore, reason);
        return task;
    }

    private RecommendationReason buildReason(double dayBoost, double frequencyWeight, DayOfWeek currentDayOfWeek) {
        String dayName = currentDayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault());

        if (dayBoost > BASE_DAY_BOOST) {
            if (frequencyWeight > 2.0) {
                return new RecommendationReason(REASON_WEEKDAY_MATCH, "Regularly completed on " + dayName);
            }
            return new RecommendationReason(REASON_WEEKDAY_MATCH, "Scheduled for " + dayName);
        }

        if (frequencyWeight > 2.0) {
            return new RecommendationReason(REASON_SIMILAR_CONTENT, "You complete this task frequently");
        }

        return new RecommendationReason(REASON_SIMILAR_CONTENT, "Related to tasks you've worked on recently");
    }

    private void applyRecommendationMetadata(Task task, float score, RecommendationReason reason) {
        float safeScore = clampScore(score);

        task.setRecommendationScore(safeScore);
        task.setRecommended(true);
        task.setRecommendedBy(reason.code());
        task.setRecommendationReason(reason.text());
        task.setLastRecommendedOn(ZonedDateTime.now(ZoneOffset.UTC));
    }

    private List<Task> applyDefaultRecommendationMetadata(List<Task> tasks, RecommendationReason reason) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            float fallbackScore = DEFAULT_POPULAR_SCORE - (i * 0.03f);
            float baseScore = task.getRecommendationScore() != null ? task.getRecommendationScore() : fallbackScore;
            applyRecommendationMetadata(task, baseScore, reason);
        }

        return tasks;
    }

    private float clampScore(double score) {
        return (float) Math.min(0.99, Math.max(0.01, score));
    }

    private List<Task> prioritizeWeekdayMatch(List<Task> scoredTasks, int limit) {
        if (scoredTasks == null || scoredTasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Task> result = new ArrayList<>(scoredTasks);

        // Find first weekday match not already at the front and move it to position 0
        for (int i = 1; i < result.size(); i++) {
            if (REASON_WEEKDAY_MATCH.equalsIgnoreCase(result.get(i).getRecommendedBy())) {
                Task match = result.remove(i);
                result.add(0, match);
                break;
            }
        }

        return result.size() > limit ? result.subList(0, limit) : result;
    }
}
