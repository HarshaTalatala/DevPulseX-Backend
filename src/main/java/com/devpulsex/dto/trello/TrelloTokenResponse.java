package com.devpulsex.dto.trello;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TrelloTokenResponse {
    @JsonProperty("oauth_token")
    private String token;
    
    @JsonProperty("oauth_token_secret")
    private String tokenSecret;
}
