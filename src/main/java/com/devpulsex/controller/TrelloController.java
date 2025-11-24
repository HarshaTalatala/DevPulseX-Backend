package com.devpulsex.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devpulsex.service.TrelloService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/trello")
@Tag(name = "Trello", description = "Trello integration endpoints")
public class TrelloController {

    private final TrelloService trelloService;

    public TrelloController(TrelloService trelloService) {
        this.trelloService = trelloService;
    }

    @GetMapping("/boards/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('DEVELOPER')")
    @Operation(summary = "Get Trello boards for user")
    public Object getBoards(@PathVariable String userId) {
        return trelloService.getUserBoards(userId);
    }

    @GetMapping("/boards/{boardId}/lists")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('DEVELOPER')")
    @Operation(summary = "Get lists on a Trello board")
    public Object getLists(@PathVariable String boardId) {
        return trelloService.getBoardLists(boardId);
    }

    @GetMapping("/lists/{listId}/cards")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('DEVELOPER')")
    @Operation(summary = "Get cards on a Trello list")
    public Object getCards(@PathVariable String listId) {
        return trelloService.getListCards(listId);
    }

    @GetMapping("/project/{projectId}/sync")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Fetch Trello data for project (admin/manager only)")
    public Object syncProject(@PathVariable Long projectId) {
        return trelloService.getTrelloDashboardForProject(projectId);
    }
}
