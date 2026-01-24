package com.taskvantage.backend.service;

import com.taskvantage.backend.model.TaskGroup;
import com.taskvantage.backend.repository.TaskGroupRepository;
import com.taskvantage.backend.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class TaskGroupServiceImpl implements TaskGroupService {

    private static final Logger logger = LoggerFactory.getLogger(TaskGroupServiceImpl.class);
    private final TaskGroupRepository taskGroupRepository;
    private final TaskRepository taskRepository;

    @Autowired
    public TaskGroupServiceImpl(TaskGroupRepository taskGroupRepository, TaskRepository taskRepository) {
        this.taskGroupRepository = taskGroupRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional
    public TaskGroup createGroup(TaskGroup group) {
        Integer maxOrder = taskGroupRepository.findMaxDisplayOrderByUserId(group.getUserId());
        group.setDisplayOrder(maxOrder + 1);
        group.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
        logger.info("Creating task group '{}' for user {}", group.getName(), group.getUserId());
        return taskGroupRepository.save(group);
    }

    @Override
    public Optional<TaskGroup> getGroupById(Long id) {
        return taskGroupRepository.findById(id);
    }

    @Override
    public List<TaskGroup> getGroupsByUserId(Long userId) {
        return taskGroupRepository.findByUserIdOrderByDisplayOrderAsc(userId);
    }

    @Override
    @Transactional
    public TaskGroup updateGroup(TaskGroup group) {
        logger.info("Updating task group {} for user {}", group.getId(), group.getUserId());
        return taskGroupRepository.save(group);
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        logger.info("Deleting task group {} - clearing groupId from associated tasks", id);
        taskRepository.clearGroupIdByGroupId(id);
        taskGroupRepository.deleteById(id);
    }
}
