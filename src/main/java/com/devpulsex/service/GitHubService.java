package com.devpulsex.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.devpulsex.dto.github.GithubInsightsResponse;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private final WebClient webClient = WebClient.builder().build();

    public GithubInsightsResponse fetchInsights(String username, String accessToken) {
        log.info("Fetching GitHub insights for user: {}", username);
        try {
            // Fetch user profile data
            JsonNode userProfile = fetchUserProfile(username, accessToken);
            
            // Fetch activity metrics
            int repoCount = fetchRepoCount(accessToken);
            int prTotal = fetchTotalPullRequests(username, accessToken);
            int issueTotal = fetchTotalIssues(username, accessToken);
            int recentCommits = fetchRecentCommitCount(username, accessToken);
            int recentPRs = fetchRecentPRCount(username, accessToken);
            int recentIssuesCount = fetchRecentIssuesCount(username, accessToken);
            int totalStars = fetchTotalStars(accessToken);
            String mostActiveRepo = fetchMostActiveRepo(username, accessToken);
            
            // Extract profile data
            int followers = userProfile != null ? userProfile.path("followers").asInt(0) : 0;
            int following = userProfile != null ? userProfile.path("following").asInt(0) : 0;
            int publicGists = userProfile != null ? userProfile.path("public_gists").asInt(0) : 0;
            
            log.info("GitHub insights fetched successfully for {}: repos={}, PRs={}, commits={}, stars={}",
                    username, repoCount, prTotal, recentCommits, totalStars);
            
            return GithubInsightsResponse.builder()
                    .username(username)
                    .repoCount(repoCount)
                    .totalPullRequests(prTotal)
                    .recentCommits(recentCommits)
                    .totalIssues(issueTotal)
                    .openIssues(fetchOpenIssuesCount(username, accessToken))
                    .closedIssues(issueTotal - fetchOpenIssuesCount(username, accessToken))
                    .totalStars(totalStars)
                    .followers(followers)
                    .following(following)
                    .publicGists(publicGists)
                    .recentPRs(recentPRs)
                    .recentIssues(recentIssuesCount)
                    .mostActiveRepo(mostActiveRepo)
                    .profileUrl("https://github.com/" + username)
                    .build();
        } catch (Exception e) {
            log.error("Error fetching GitHub insights for user: {}", username, e);
            // Return empty insights on error rather than failing
            return GithubInsightsResponse.builder()
                    .username(username)
                    .repoCount(0)
                    .totalPullRequests(0)
                    .recentCommits(0)
                    .totalIssues(0)
                    .openIssues(0)
                    .closedIssues(0)
                    .totalStars(0)
                    .followers(0)
                    .following(0)
                    .publicGists(0)
                    .recentPRs(0)
                    .recentIssues(0)
                    .mostActiveRepo("")
                    .profileUrl("https://github.com/" + username)
                    .build();
        }
    }
    
    private JsonNode fetchUserProfile(String username, String accessToken) {
        try {
            return webClient.get()
                    .uri("https://api.github.com/users/" + username)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.warn("Error fetching user profile: {}", e.getMessage());
            return null;
        }
    }
    
    private int fetchTotalIssues(String username, String accessToken) {
        try {
            JsonNode search = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/search/issues")
                            .queryParam("q", "type:issue+author:" + username)
                            .queryParam("per_page", "1")
                            .build())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return search != null && search.has("total_count") ? search.get("total_count").asInt() : 0;
        } catch (Exception e) {
            log.warn("Error fetching total issues: {}", e.getMessage());
            return 0;
        }
    }
    
    private int fetchOpenIssuesCount(String username, String accessToken) {
        try {
            JsonNode search = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/search/issues")
                            .queryParam("q", "type:issue+author:" + username + "+state:open")
                            .queryParam("per_page", "1")
                            .build())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return search != null && search.has("total_count") ? search.get("total_count").asInt() : 0;
        } catch (Exception e) {
            log.warn("Error fetching open issues: {}", e.getMessage());
            return 0;
        }
    }
    
    private int fetchRecentPRCount(String username, String accessToken) {
        try {
            Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
            String dateStr = cutoff.toString().substring(0, 10);
            JsonNode search = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/search/issues")
                            .queryParam("q", "type:pr+author:" + username + "+created:>=" + dateStr)
                            .queryParam("per_page", "1")
                            .build())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return search != null && search.has("total_count") ? search.get("total_count").asInt() : 0;
        } catch (Exception e) {
            log.warn("Error fetching recent PRs: {}", e.getMessage());
            return 0;
        }
    }
    
    private int fetchRecentIssuesCount(String username, String accessToken) {
        try {
            Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
            String dateStr = cutoff.toString().substring(0, 10);
            JsonNode search = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/search/issues")
                            .queryParam("q", "type:issue+author:" + username + "+created:>=" + dateStr)
                            .queryParam("per_page", "1")
                            .build())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return search != null && search.has("total_count") ? search.get("total_count").asInt() : 0;
        } catch (Exception e) {
            log.warn("Error fetching recent issues: {}", e.getMessage());
            return 0;
        }
    }
    
    private int fetchTotalStars(String accessToken) {
        try {
            JsonNode repos = webClient.get()
                    .uri("https://api.github.com/user/repos?per_page=100")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (repos == null || !repos.isArray()) return 0;
            int stars = 0;
            for (JsonNode repo : repos) {
                stars += repo.path("stargazers_count").asInt(0);
            }
            return stars;
        } catch (Exception e) {
            log.warn("Error fetching total stars: {}", e.getMessage());
            return 0;
        }
    }
    
    private String fetchMostActiveRepo(String username, String accessToken) {
        try {
            JsonNode events = webClient.get()
                    .uri("https://api.github.com/users/" + username + "/events?per_page=100")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (events == null || !events.isArray()) return "";
            
            // Count events per repo
            java.util.Map<String, Integer> repoActivity = new java.util.HashMap<>();
            Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
            for (JsonNode event : events) {
                String createdAt = event.path("created_at").asText("");
                if (!createdAt.isEmpty() && Instant.parse(createdAt).isAfter(cutoff)) {
                    String repoName = event.path("repo").path("name").asText("");
                    if (!repoName.isEmpty()) {
                        repoActivity.put(repoName, repoActivity.getOrDefault(repoName, 0) + 1);
                    }
                }
            }
            
            return repoActivity.entrySet().stream()
                    .max(java.util.Map.Entry.comparingByValue())
                    .map(java.util.Map.Entry::getKey)
                    .orElse("");
        } catch (Exception e) {
            log.warn("Error fetching most active repo: {}", e.getMessage());
            return "";
        }
    }

    private int fetchRepoCount(String accessToken) {
        try {
            // Fetch first page with 100 repos and count length; sufficient for basic analytics
            JsonNode repos = webClient.get()
                    .uri("https://api.github.com/user/repos?per_page=100")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return repos != null ? repos.size() : 0;
        } catch (Exception e) {
            log.warn("Error fetching repo count: {}", e.getMessage());
            return 0;
        }
    }

    private int fetchTotalPullRequests(String username, String accessToken) {
        try {
            // Use search API to get total PRs authored by the user
            JsonNode search = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/search/issues")
                            .queryParam("q", "type:pr+author:" + username)
                            .queryParam("per_page", "1")
                            .build())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return search != null && search.has("total_count") ? search.get("total_count").asInt() : 0;
        } catch (Exception e) {
            log.warn("Error fetching PR count: {}", e.getMessage());
            return 0;
        }
    }

    private int fetchRecentCommitCount(String username, String accessToken) {
        try {
            // Use public events to approximate recent commits in last 7 days
            JsonNode events = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/users/" + username + "/events")
                            .queryParam("per_page", "100")
                            .build())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (events == null || !events.isArray()) return 0;
            Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
            int commits = 0;
            for (JsonNode ev : events) {
                String type = ev.path("type").asText("");
                String createdAt = ev.path("created_at").asText("");
                if ("PushEvent".equals(type) && !createdAt.isEmpty()) {
                    Instant t = Instant.parse(createdAt);
                    if (t.isAfter(cutoff)) {
                        // payload.size is number of commits in push
                        commits += ev.path("payload").path("size").asInt(0);
                    }
                }
            }
            return commits;
        } catch (Exception e) {
            log.warn("Error fetching recent commits: {}", e.getMessage());
            return 0;
        }
    }

    public JsonNode fetchRepositories(String accessToken) {
        log.info("Fetching GitHub repositories");
        try {
            String uri = "https://api.github.com/user/repos?per_page=100&sort=updated";
            JsonNode response = webClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            
            log.info("Successfully fetched {} repositories", response != null ? response.size() : 0);
            return response;
        } catch (Exception e) {
            log.error("Error fetching repositories: {}", e.getMessage());
            return null;
        }
    }
}
