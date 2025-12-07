package com.kzkv.visisis.lab4.service;

import com.kzkv.visisis.lab4.dto.Issue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueServiceImpl implements IssueService {

	private final RestTemplate restTemplate;

	@Value("${url.jira}")
	private String urlTemplate;

	@Override
	@Cacheable("issues")
	public List<Issue> getIssues(String jql, int maxResults) {
		String url = urlTemplate.formatted(jql, maxResults);
		try {
			ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
			return getIssuesFromResponse(response);
		} catch (Exception e) {
			log.error("Error fetching issues from Jira", e);
			return List.of();
		}
	}

	private static List<Issue> getIssuesFromResponse(ResponseEntity<Map> response) {
		if (response.getBody() == null || !(response.getBody().get("issues") instanceof List<?>)) {
			return List.of();
		}
		List<Map> issues = (List<Map>) response.getBody().get("issues");
		return issues.stream()
				.map(Issue::new)
				.toList();
	}

}
