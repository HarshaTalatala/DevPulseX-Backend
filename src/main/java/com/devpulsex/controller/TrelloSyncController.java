package com.devpulsex.controller;

import com.devpulsex.dto.task.TaskDto;
import com.devpulsex.model.Project;
import com.devpulsex.model.TaskStatus;
import com.devpulsex.repository.ProjectRepository;
import com.devpulsex.service.TaskService;
import com.devpulsex.service.TrelloService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/trello")
@Tag(name = "Trello Sync", description = "Sync Trello cards to DevPulseX tasks")
public class TrelloSyncController {

    private final TrelloService trelloService;
    private final ProjectRepository projectRepository;
    private final TaskService taskService;

    public TrelloSyncController(TrelloService trelloService, ProjectRepository projectRepository, TaskService taskService) {
        this.trelloService = trelloService;
        this.projectRepository = projectRepository;
        this.taskService = taskService;
    }

    @PostMapping("/project/{projectId}/sync-tasks")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Sync Trello cards into DevPulseX tasks (admin/manager only)")
    public Map<String, Object> syncTasks(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        String boardId = project.getTrelloBoardId();
        if (boardId == null || boardId.isBlank()) {
            throw new IllegalArgumentException("Project has no trelloBoardId configured");
        }
        JsonNode lists = trelloService.getBoardLists(boardId);
        int created = 0; int updated = 0; int ignored = 0;
        if (lists != null && lists.isArray()) {
            for (JsonNode l : lists) {
                String listId = l.path("id").asText();
                String listName = l.path("name").asText();
                TaskStatus inferred = trelloService.mapListNameToStatus(listName);
                JsonNode cards = trelloService.getListCards(listId);
                if (cards != null && cards.isArray()) {
                    for (JsonNode c : cards) {
                        TaskDto dto = TaskDto.builder()
                                .title(c.path("name").asText())
                                .description(c.path("desc").asText(null))
                                .projectId(projectId)
                                .status(inferred)
                                .build();
                        // naive: create a new task for each card (no idempotency key). In production, match by external id.
                        taskService.create(dto);
                        created++;
                    }
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        result.put("ignored", ignored);
        return result;
    }
}
