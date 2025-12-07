package com.kzkv.visisis.lab4.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IssueTest {

    @Test
    void shouldParseValidIssueMap() {
        Map<String, Object> issueMap = Map.of(
                "key", "PROJ-123",
                "fields", Map.of(
                        "created", "2024-06-01T10:00:00.000+0300",
                        "resolutiondate", "2024-06-05T14:30:00.000+0300",
                        "status", Map.of("name", "Closed"),
                        "reporter", Map.of("displayName", "Alice"),
                        "assignee", Map.of("displayName", "Bob"),
                        "priority", Map.of("name", "High")
                ),
                "changelog", Map.of(
                        "histories", List.of(
                                Map.of(
                                        "created", "2024-06-02T09:00:00.000+0300",
                                        "items", List.of(
                                                Map.of("field", "status", "fromString", "Open", "toString", "In Progress")
                                        )
                                ),
                                Map.of(
                                        "created", "2024-06-04T16:00:00.000+0300",
                                        "items", List.of(
                                                Map.of("field", "status", "fromString", "In Progress", "toString", "Closed")
                                        )
                                )
                        )
                )
        );

        Issue issue = new Issue(issueMap);

        assertThat(issue.key()).isEqualTo("PROJ-123");
        assertThat(issue.created()).isEqualTo(LocalDateTime.of(2024, 6, 1, 10, 0));
        assertThat(issue.resolved()).isEqualTo(LocalDateTime.of(2024, 6, 5, 14, 30));
        assertThat(issue.status()).isEqualTo("Closed");
        assertThat(issue.reporter()).isEqualTo("Alice");
        assertThat(issue.assignee()).isEqualTo("Bob");
        assertThat(issue.priority()).isEqualTo("High");

        assertThat(issue.statusDurations()).hasSize(3);
    }

    @Test
    void shouldHandleMissingChangelog() {
        Map<String, Object> issueMap = Map.of(
                "key", "PROJ-125",
                "fields", Map.of(
                        "created", "2024-06-01T10:00:00.000+0300",
                        "resolutiondate", "2024-06-03T10:00:00.000+0300",
                        "status", Map.of("name", "Closed"),
                        "reporter", Map.of("displayName", "Alice"),
                        "assignee", Map.of("displayName", "Bob"),
                        "priority", Map.of("name", "Low")
                )
                // no changelog
        );

        Issue issue = new Issue(issueMap);
        assertThat(issue.statusDurations()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2024-06-01T10:00:00",
            "2024-06-01T10:00:00.123+0300"
    })
    void shouldParseVariousDateFormats(String dateStr) {
        Map<String, Object> issueMap = Map.of(
                "key", "PROJ-126",
                "fields", Map.of(
                        "created", dateStr,
                        "resolutiondate", dateStr,
                        "status", Map.of("name", "Closed"),
                        "reporter", Map.of("displayName", "Alice"),
                        "assignee", Map.of("displayName", "Bob"),
                        "priority", Map.of("name", "Low")
                ),
                "changelog", Map.of("histories", List.of())
        );

        Issue issue = new Issue(issueMap);
        assertThat(issue.created()).isEqualTo(LocalDateTime.of(2024, 6, 1, 10, 0));
        assertThat(issue.resolved()).isEqualTo(LocalDateTime.of(2024, 6, 1, 10, 0));
    }
}