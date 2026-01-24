package com.taskvantage.backend.controller;

import com.taskvantage.backend.model.TaskGroup;
import com.taskvantage.backend.service.TaskGroupService;
import com.taskvantage.backend.Security.AuthorizationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/task-groups")
public class TaskGroupController {

    private static final Logger logger = LoggerFactory.getLogger(TaskGroupController.class);

    private final TaskGroupService taskGroupService;
    private final AuthorizationUtil authorizationUtil;

    @Autowired
    public TaskGroupController(TaskGroupService taskGroupService, AuthorizationUtil authorizationUtil) {
        this.taskGroupService = taskGroupService;
        this.authorizationUtil = authorizationUtil;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createGroup(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody TaskGroup group) {
        Map<String, Object> response = new HashMap<>();

        ResponseEntity<Map<String, Object>> authError = authorizationUtil.validateUserAccess(authorizationHeader, group.getUserId());
        if (authError != null) {
            return authError;
        }

        if (group.getName() == null || group.getName().isEmpty()) {
            response.put("message", "Group name is required");
            return ResponseEntity.badRequest().body(response);
        }

        TaskGroup createdGroup = taskGroupService.createGroup(group);
        logger.info("Task group created successfully: {}", createdGroup.getName());

        response.put("group", createdGroup);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getGroupsByUserId(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long userId) {

        ResponseEntity<Map<String, Object>> authError = authorizationUtil.validateUserAccess(authorizationHeader, userId);
        if (authError != null) {
            return authError;
        }

        List<TaskGroup> groups = taskGroupService.getGroupsByUserId(userId);
        return ResponseEntity.ok(groups);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateGroup(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id,
            @RequestBody TaskGroup group) {
        Map<String, Object> response = new HashMap<>();

        Optional<TaskGroup> existingGroup = taskGroupService.getGroupById(id);
        if (existingGroup.isEmpty()) {
            response.put("message", "Group not found");
            return ResponseEntity.status(404).body(response);
        }

        ResponseEntity<Map<String, Object>> authError = authorizationUtil.validateResourceOwnership(authorizationHeader, existingGroup.get().getUserId());
        if (authError != null) {
            return authError;
        }

        group.setId(id);
        group.setUserId(existingGroup.get().getUserId());
        group.setCreatedAt(existingGroup.get().getCreatedAt());

        if (group.getDisplayOrder() == null) {
            group.setDisplayOrder(existingGroup.get().getDisplayOrder());
        }

        TaskGroup updatedGroup = taskGroupService.updateGroup(group);
        response.put("group", updatedGroup);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteGroup(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        Optional<TaskGroup> existingGroup = taskGroupService.getGroupById(id);
        if (existingGroup.isEmpty()) {
            response.put("message", "Group not found");
            return ResponseEntity.status(404).body(response);
        }

        ResponseEntity<Map<String, Object>> authError = authorizationUtil.validateResourceOwnership(authorizationHeader, existingGroup.get().getUserId());
        if (authError != null) {
            return authError;
        }

        taskGroupService.deleteGroup(id);
        response.put("message", "Group deleted successfully");
        return ResponseEntity.ok(response);
    }
}
