package com.fullstack.Backend.responses.users;

import lombok.Data;

import java.util.Set;

@Data
public class KeywordSuggestionResponse {
	Set<String> keywordList;
}
