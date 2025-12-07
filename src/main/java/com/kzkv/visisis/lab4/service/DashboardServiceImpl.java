package com.kzkv.visisis.lab4.service;

import com.kzkv.visisis.lab4.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

	private final IssueService issueService;

	@Override
	public List<BinCount> getIssuesByOpenedTime(String projectKey, int maxResults) {
		String jql = "project = \"" + sanitizeProjectKey(projectKey) + "\" AND status = Closed";
		List<Issue> issues = issueService.getIssues(jql, maxResults);

		return issues.stream()
				.filter(issue -> issue.resolved() != null)
				.map(issue -> Duration.between(issue.created(), issue.resolved()).toDays())
				.collect(Collectors.groupingBy(
						days -> days,
						Collectors.counting()
				))
				.entrySet().stream()
				.map(e -> new BinCount(e.getKey().toString(), e.getValue().intValue()))
				.toList();
	}

	@Override
	public Map<String, List<BinCount>> getStatusTimeDistribution(String projectKey, int maxResults) {
		String jql = "project = \"" + sanitizeProjectKey(projectKey) + "\" AND status = Closed";
		List<Issue> issues = issueService.getIssues(jql, maxResults);

		List<StatusDuration> allDurations = issues.stream()
				.flatMap(issue -> issue.statusDurations().stream())
				.toList();

		return allDurations.stream()
				.collect(Collectors.groupingBy(
						StatusDuration::status,
						Collectors.groupingBy(
								d -> String.valueOf(d.days()),
								TreeMap::new,
								Collectors.counting()
						)
				))
				.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> e.getValue().entrySet().stream()
								.map(entry -> new BinCount(entry.getKey(), entry.getValue().intValue()))
								.toList()
				));
	}

	@Override
	public List<DailyStats> getDailyTaskStats(String projectKey, int maxResults) {
		String jql = "project = \"" + sanitizeProjectKey(projectKey) + "\" AND (created IS NOT NULL OR resolutiondate IS NOT NULL)";
		List<Issue> issues = issueService.getIssues(jql, maxResults);

		Map<LocalDate, Integer> createdMap = new TreeMap<>();
		Map<LocalDate, Integer> resolvedMap = new TreeMap<>();

		for (Issue issue : issues) {
			if (issue.created() != null) {
				LocalDate day = issue.created().toLocalDate();
				createdMap.merge(day, 1, Integer::sum);
			}
			if (issue.resolved() != null) {
				LocalDate day = issue.resolved().toLocalDate();
				resolvedMap.merge(day, 1, Integer::sum);
			}
		}

		if (createdMap.isEmpty() && resolvedMap.isEmpty()) {
			return List.of();
		}

		LocalDate start = Stream.concat(createdMap.keySet().stream(), resolvedMap.keySet().stream())
				.min(LocalDate::compareTo).orElse(LocalDate.now().minusMonths(1));
		LocalDate end = Stream.concat(createdMap.keySet().stream(), resolvedMap.keySet().stream())
				.max(LocalDate::compareTo).orElse(LocalDate.now());

		List<DailyStats> result = new ArrayList<>();
		int cumCreated = 0, cumResolved = 0;

		for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
			int created = createdMap.getOrDefault(date, 0);
			int resolved = resolvedMap.getOrDefault(date, 0);
			cumCreated += created;
			cumResolved += resolved;
			result.add(new DailyStats(date, created, resolved, cumCreated, cumResolved));
		}

		return result;
	}

	@Override
	public List<UserCount> getTopUsers(String projectKey, int maxResults) {
		String jql = "project = \"" + sanitizeProjectKey(projectKey) + "\" AND status = Closed";
		List<Issue> issues = issueService.getIssues(jql, maxResults);

		Map<String, Integer> userCounts = new HashMap<>();
		for (Issue issue : issues) {
			if (issue.reporter() != null) {
				userCounts.merge(issue.reporter(), 1, Integer::sum);
			}
			if (issue.assignee() != null) {
				userCounts.merge(issue.assignee(), 1, Integer::sum);
			}
		}

		return userCounts.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.limit(30)
				.map(e -> new UserCount(e.getKey(), e.getValue()))
				.toList();
	}

	@Override
	public List<BinCount> getAssignedIssuesTimeDistribution(String projectKey, int maxResults) {
		String jql = "project = \"" + sanitizeProjectKey(projectKey) + "\" AND status = Closed";
		List<Issue> issues = issueService.getIssues(jql, maxResults);

		return issues.stream()
				.filter(issue -> issue.resolved() != null && issue.statusDurations().stream().anyMatch(statusDuration -> statusDuration.status().equals("In Progress")))
				.map(issue -> issue.statusDurations().stream()
						.filter(statusDuration -> statusDuration.status().equals("In Progress"))
						.findFirst()
						.map(StatusDuration::days)
						.orElse(0L))
				.collect(Collectors.groupingBy(
						days -> days,
						Collectors.counting()
				))
				.entrySet().stream()
				.map(e -> new BinCount(e.getKey().toString(), e.getValue().intValue()))
				.toList();
	}

	@Override
	public List<BinCount> getIssuesByPriority(String projectKey, int maxResults) {
		String jql = "project = \"" + sanitizeProjectKey(projectKey) + "\"";
		List<Issue> issues = issueService.getIssues(jql, maxResults);

		return issues.stream()
				.filter(i -> i.priority() != null)
				.collect(Collectors.groupingBy(
						Issue::priority,
						Collectors.counting()
				))
				.entrySet().stream()
				.map(e -> new BinCount(e.getKey(), e.getValue().intValue()))
				.toList();
	}

	private String sanitizeProjectKey(String key) {
		if (!key.matches("[A-Za-z0-9_-]+")) {
			throw new IllegalArgumentException("Invalid project key: " + key);
		}
		return key;
	}
}