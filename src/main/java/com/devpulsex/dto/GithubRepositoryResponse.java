package com.devpulsex.dto;

import java.util.List;

public class GithubRepositoryResponse {
    private Long id;
    private String name;
    private String fullName;
    private String description;
    private String url;
    private String language;
    private Integer stars;
    private Integer forks;
    private Integer openIssues;
    private Boolean isPrivate;
    private String createdAt;
    private String updatedAt;
    private String defaultBranch;
    private List<String> topics;

    public GithubRepositoryResponse() {}

    public GithubRepositoryResponse(Long id, String name, String fullName, String description, 
                                   String url, String language, Integer stars, Integer forks, 
                                   Integer openIssues, Boolean isPrivate, String createdAt, 
                                   String updatedAt, String defaultBranch, List<String> topics) {
        this.id = id;
        this.name = name;
        this.fullName = fullName;
        this.description = description;
        this.url = url;
        this.language = language;
        this.stars = stars;
        this.forks = forks;
        this.openIssues = openIssues;
        this.isPrivate = isPrivate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.defaultBranch = defaultBranch;
        this.topics = topics;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }

    public Integer getForks() { return forks; }
    public void setForks(Integer forks) { this.forks = forks; }

    public Integer getOpenIssues() { return openIssues; }
    public void setOpenIssues(Integer openIssues) { this.openIssues = openIssues; }

    public Boolean getIsPrivate() { return isPrivate; }
    public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> topics) { this.topics = topics; }
}
