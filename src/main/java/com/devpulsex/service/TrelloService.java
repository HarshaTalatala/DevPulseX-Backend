package com.devpulsex.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.devpulsex.dto.task.TaskDto;
import com.devpulsex.exception.ResourceNotFoundException;
import com.devpulsex.exception.TrelloApiException;
import com.devpulsex.integration.trello.TrelloClient;
import com.devpulsex.model.Project;
import com.devpulsex.model.TaskStatus;
import com.devpulsex.integration.trello.TrelloTokenEncryptor;
import com.devpulsex.model.User;
import com.devpulsex.repository.ProjectRepository;
import com.devpulsex.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class TrelloService {

    private static final Logger log = LoggerFactory.getLogger(TrelloService.class);

    private final TrelloClient trelloClient;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TrelloTokenEncryptor tokenEncryptor;

    public TrelloService(TrelloClient trelloClient, ProjectRepository projectRepository,
                         UserRepository userRepository, TrelloTokenEncryptor tokenEncryptor) {
        this.trelloClient = trelloClient;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.tokenEncryptor = tokenEncryptor;
    }

    public JsonNode getUserBoards(Authentication authentication) {
        String token = requireUserToken(authentication);
        try {
            log.info("Fetching Trello boards for user: {}", authentication.getName());
            JsonNode boards = trelloClient.getBoards(token);
            log.info("Successfully fetched {} boards for user: {}", boards.size(), authentication.getName());
            return boards;
        }
        catch (Exception e) {
            log.error("Failed to fetch Trello boards for user {}: {}", authentication.getName(), e.getMessage(), e);
            throw new TrelloApiException("Failed to fetch Trello boards: " + e.getMessage(), e);
        }
    }
    public JsonNode getBoardLists(String boardId, Authentication authentication) {
        String token = requireUserToken(authentication);
        try {
            log.info("Fetching lists for Trello board: {}", boardId);
            JsonNode lists = trelloClient.getLists(boardId, token);
            log.info("Successfully fetched {} lists from board: {}", lists.size(), boardId);
            return lists;
        }
        catch (Exception e) {
            log.error("Failed to fetch Trello lists for board {}: {}", boardId, e.getMessage(), e);
            throw new TrelloApiException("Failed to fetch Trello lists: " + e.getMessage(), e);
        }
    }
    
    public JsonNode getListCards(String listId, Authentication authentication) {
        String token = requireUserToken(authentication);
        try {
            log.info("Fetching cards for Trello list: {}", listId);
            JsonNode cards = trelloClient.getCards(listId, token);
            log.info("Successfully fetched {} cards from list: {}", cards.size(), listId);
            return cards;
        }
        catch (Exception e) {
            log.error("Failed to fetch Trello cards for list {}: {}", listId, e.getMessage(), e);
            throw new TrelloApiException("Failed to fetch Trello cards: " + e.getMessage(), e);
        }
    }

    public List<TaskDto> mapCardsToTaskDtos(JsonNode cards, Long projectId) {
        List<TaskDto> tasks = new ArrayList<>();
        if (cards == null || !cards.isArray()) return tasks;
        for (JsonNode c : cards) {
            String name = c.path("name").asText();
            String desc = c.path("desc").asText(null);
            TaskStatus status = TaskStatus.TODO; // default
            // Attempt to derive status from labels or card name patterns if desired
            tasks.add(TaskDto.builder()
                    .title(name)
                    .description(desc)
                    .projectId(projectId)
                    .status(status)
                    .build());
        }
        return tasks;
    }

    public Project enrichProjectWithTrelloData(Project project) {
        // Placeholder for future enrichment logic (e.g., caching board meta)
        return project;
    }

    public TaskStatus mapListNameToStatus(String listName) {
        if (listName == null) return TaskStatus.TODO;
        String ln = listName.toLowerCase();
        if (ln.contains("done") || ln.contains("complete") || ln.matches(".*\\bdone\\b.*")) return TaskStatus.DONE;
        if (ln.contains("doing") || ln.contains("progress") || ln.contains("review")) return TaskStatus.IN_PROGRESS;
        if (ln.contains("block")) return TaskStatus.BLOCKED;
        return TaskStatus.TODO;
    }

    public Map<String, Object> buildBoardAggregate(String boardId, Authentication authentication) {
        JsonNode lists = getBoardLists(boardId, authentication);
        List<Map<String, Object>> listAgg = new ArrayList<>();
        if (lists != null && lists.isArray()) {
            for (JsonNode l : lists) {
                String listId = l.path("id").asText();
                String listName = l.path("name").asText();
                JsonNode cards = getListCards(listId, authentication);
                List<Map<String, Object>> cardViews = new ArrayList<>();
                if (cards != null && cards.isArray()) {
                    for (JsonNode c : cards) {
                        Map<String, Object> cardMap = new HashMap<>();
                        cardMap.put("id", c.path("id").asText());
                        cardMap.put("name", c.path("name").asText());
                        cardMap.put("desc", c.path("desc").asText());
                        // labels
                        List<String> labels = new ArrayList<>();
                        if (c.path("labels").isArray()) {
                            for (JsonNode lab : c.path("labels")) {
                                labels.add(lab.path("name").asText());
                            }
                        }
                        cardMap.put("labels", labels);
                        // members
                        List<String> members = new ArrayList<>();
                        if (c.path("idMembers").isArray()) {
                            for (JsonNode m : c.path("idMembers")) {
                                members.add(m.asText());
                            }
                        }
                        cardMap.put("memberIds", members);
                        cardViews.add(cardMap);
                    }
                }
                Map<String, Object> listMap = new HashMap<>();
                listMap.put("listId", listId);
                listMap.put("listName", listName);
                listMap.put("cards", cardViews);
                listAgg.add(listMap);
            }
        }
        Map<String, Object> root = new HashMap<>();
        root.put("lists", listAgg);
        return root;
    }

    @SuppressWarnings("null")
    public Map<String, Object> getTrelloDashboardForProject(Long projectId, Authentication authentication) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        if (project.getTrelloBoardId() == null || project.getTrelloBoardId().isBlank()) {
            throw new IllegalArgumentException("Project has no trelloBoardId configured");
        }
        return buildBoardAggregate(project.getTrelloBoardId(), authentication);
    }

    private String requireUserToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Authentication required but not authenticated for Trello calls");
            throw new IllegalArgumentException("Authentication required for Trello calls");
        }
        String email = authentication.getName();
        log.debug("Retrieving Trello token for user: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found for email: {}", email);
                    return new ResourceNotFoundException("User not found: " + email);
                });
        if (user.getTrelloAccessToken() == null || user.getTrelloAccessToken().isBlank()) {
            log.warn("User {} has no linked Trello token. trelloId={}, trelloUsername={}", 
                    email, user.getTrelloId(), user.getTrelloUsername());
            throw new IllegalStateException("User has no linked Trello token!");
        }
        try {
            String decrypted = tokenEncryptor.decrypt(user.getTrelloAccessToken());
            log.debug("Successfully decrypted Trello token for user: {}", email);
            return decrypted;
        } catch (Exception e) {
            log.error("Failed to decrypt Trello token for user {}: {}", email, e.getMessage(), e);
            throw new IllegalStateException("Failed to decrypt Trello token: " + e.getMessage(), e);
        }
    }
}
