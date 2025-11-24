package com.devpulsex.dto.trello;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TrelloMemberProfile {
    private String id;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("fullName")
    private String fullName;
    
    @JsonProperty("email")
    private String email;
}
