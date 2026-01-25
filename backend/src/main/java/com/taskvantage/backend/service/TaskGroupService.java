package com.taskvantage.backend.service;

import com.taskvantage.backend.model.TaskGroup;

import java.util.List;
import java.util.Optional;

public interface TaskGroupService {

    TaskGroup createGroup(TaskGroup group);

    Optional<TaskGroup> getGroupById(Long id);

    List<TaskGroup> getGroupsByUserId(Long userId);

    TaskGroup updateGroup(TaskGroup group);

    void deleteGroup(Long id);
}
