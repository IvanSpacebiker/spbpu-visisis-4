package com.kzkv.visisis.lab4.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public record Issue(
		String key,
		LocalDateTime created,
		LocalDateTime resolved,
		String status,
		String reporter,
		String assignee,
		String priority,
		List<StatusDuration> statusDurations
) {

	private static final DateTimeFormatter JIRA_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	public Issue(Map<String, Object> map) {
		this(
				(String) map.get("key"),
				parseDate(getNested(map, "fields.created")),
				parseDate(getNested(map, "fields.resolutiondate")),
				getNested(map, "fields.status.name"),
				getNested(map, "fields.reporter.displayName"),
				getNested(map, "fields.assignee.displayName"),
				getNested(map, "fields.priority.name"),
				parseChangelog(map)
		);
	}

	private static LocalDateTime parseDate(Object obj) {
		if (obj == null) return null;
		String str = obj.toString();
		if (str.length() < 19) return null;
		return LocalDateTime.parse(str.substring(0, 19), JIRA_FORMAT);
	}

	private static String getNested(Map<String, Object> map, String path) {
		String[] keys = path.split("\\.");
		Object current = map;
		for (String key : keys) {
			if (!(current instanceof Map)) return null;
			current = ((Map<?, ?>) current).get(key);
			if (current == null) return null;
		}
		return current instanceof String ? (String) current : current != null ? current.toString() : null;
	}

	private static List<StatusDuration> parseChangelog(Map<String, Object> issueMap) {
		LocalDateTime created = parseDate(getNested(issueMap, "fields.created"));
		LocalDateTime resolved = parseDate(getNested(issueMap, "fields.resolutiondate"));
		if (created == null || resolved == null) {
			return List.of();
		}

		Object changelogObj = issueMap.get("changelog");
		if (!(changelogObj instanceof Map)) return List.of();

		List<Map<?, ?>> histories = (List<Map<?, ?>>) ((Map<?, ?>) changelogObj).get("histories");
		if (histories == null) return List.of();

		histories = histories.stream()
				.filter(h -> h.get("created") != null)
				.sorted(Comparator.comparing(h -> parseDate(h.get("created").toString())))
				.toList();

		List<StatusEntry> entries = new ArrayList<>();
		String currentStatus = getNested(issueMap, "fields.status.name");
		LocalDateTime lastTime = created;

		for (Map<?, ?> history : histories) {
			LocalDateTime changeTime = parseDate(history.get("created").toString());
			if (changeTime == null || changeTime.isBefore(lastTime)) continue;

			List<Map<?, ?>> items = (List<Map<?, ?>>) history.get("items");
			if (items == null) continue;

			for (Map<?, ?> item : items) {
				if ("status".equals(item.get("field"))) {
					String fromStatus = (String) item.get("fromString");
					if (fromStatus != null) {
						long days = java.time.Duration.between(lastTime, changeTime).toDays();
						if (days >= 0) {
							entries.add(new StatusEntry(fromStatus, days));
						}
					}
					lastTime = changeTime;
					currentStatus = (String) item.get("toString");
				}
			}
		}

		if (lastTime != null && resolved != null && !lastTime.isAfter(resolved)) {
			long days = java.time.Duration.between(lastTime, resolved).toDays();
			if (days >= 0) {
				entries.add(new StatusEntry(currentStatus, days));
			}
		}

		return entries.stream()
				.collect(Collectors.groupingBy(
						StatusEntry::status,
						Collectors.summingLong(StatusEntry::days)
				))
				.entrySet().stream()
				.map(e -> new StatusDuration(e.getKey(), e.getValue()))
				.toList();
	}


}