package com.kzkv.visisis.lab4.service;

import com.kzkv.visisis.lab4.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private IssueService issueService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Issue closedIssue1;
    private Issue closedIssue2;
    private Issue openIssue;

    @BeforeEach
    void setUp() {
        LocalDateTime created1 = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime resolved1 = LocalDateTime.of(2024, 6, 5, 14, 0);
        closedIssue1 = new Issue(
                "PROJ-1", created1, resolved1, "Closed", "Alice", "Bob", "High",
                List.of(
                        new StatusDuration("Open", 2),
                        new StatusDuration("In Progress", 2),
                        new StatusDuration("Closed", 1)
                )
        );

        LocalDateTime created2 = LocalDateTime.of(2024, 6, 2, 9, 0);
        LocalDateTime resolved2 = LocalDateTime.of(2024, 6, 6, 11, 0);
        closedIssue2 = new Issue(
                "PROJ-2", created2, resolved2, "Closed", "Charlie", "Bob", "Medium",
                List.of(
                        new StatusDuration("Open", 1),
                        new StatusDuration("In Progress", 3),
                        new StatusDuration("Closed", 1)
                )
        );

        openIssue = new Issue(
                "PROJ-3", LocalDateTime.of(2024, 6, 3, 10, 0), null, "Open", "Alice", "Dana", "Low",
                List.of()
        );
    }

    @Test
    void getIssuesByOpenedTime_ShouldReturnDurationCounts() {
        when(issueService.getIssues("project = \"PROJ\" AND status = Closed", 100))
                .thenReturn(List.of(closedIssue1, closedIssue2));

        List<BinCount> result = dashboardService.getIssuesByOpenedTime("PROJ", 100);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bin()).isEqualTo("4");
        assertThat(result.get(0).count()).isEqualTo(2);
    }

    @Test
    void getStatusTimeDistribution_ShouldGroupByStatusAndDays() {
        when(issueService.getIssues("project = \"PROJ\" AND status = Closed", 100))
                .thenReturn(List.of(closedIssue1, closedIssue2));

        Map<String, List<BinCount>> result = dashboardService.getStatusTimeDistribution("PROJ", 100);

        assertThat(result).hasSize(3);
        assertThat(result.get("Open")).extracting("bin", "count")
                .contains(tuple("2", 1), tuple("1", 1));
        assertThat(result.get("In Progress")).extracting("bin", "count")
                .contains(tuple("2", 1), tuple("3", 1));
    }

    @Test
    void getDailyTaskStats_ShouldAggregateByDate() {
        when(issueService.getIssues("project = \"PROJ\" AND (created IS NOT NULL OR resolutiondate IS NOT NULL)", 100))
                .thenReturn(List.of(closedIssue1, closedIssue2, openIssue));

        List<DailyStats> result = dashboardService.getDailyTaskStats("PROJ", 100);

        assertThat(result).hasSize(6); // от 2024-06-01 до 2024-06-06

        DailyStats day1 = result.get(0); // 2024-06-01
        assertThat(day1.date()).isEqualTo(java.time.LocalDate.of(2024, 6, 1));
        assertThat(day1.created()).isEqualTo(1);
        assertThat(day1.resolved()).isEqualTo(0);

        DailyStats day5 = result.get(4); // 2024-06-05
        assertThat(day5.resolved()).isEqualTo(1);

        DailyStats day6 = result.get(5); // 2024-06-06
        assertThat(day6.resolved()).isEqualTo(1);
    }

    @Test
    void getTopUsers_ShouldCountReportersAndAssignees() {
        when(issueService.getIssues("project = \"PROJ\" AND status = Closed", 100))
                .thenReturn(List.of(closedIssue1, closedIssue2));

        List<UserCount> result = dashboardService.getTopUsers("PROJ", 100);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).user()).isEqualTo("Bob");   // assignee twice
        assertThat(result.get(0).count()).isEqualTo(2);
        assertThat(result.get(1).user()).isEqualTo("Alice"); // reporter once
        assertThat(result.get(1).count()).isEqualTo(1);
        assertThat(result.get(2).user()).isEqualTo("Charlie");
    }

    @Test
    void getAssignedIssuesTimeDistribution_ShouldFilterByInProgress() {
        when(issueService.getIssues("project = \"PROJ\" AND status = Closed", 100))
                .thenReturn(List.of(closedIssue1, closedIssue2));

        List<BinCount> result = dashboardService.getAssignedIssuesTimeDistribution("PROJ", 100);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting("bin", "count")
                .contains(
                        tuple("2", 1),
                        tuple("3", 1)
                );
    }

    @Test
    void getIssuesByPriority_ShouldCountPriorities() {
        Issue high = new Issue("P-1", null, null, null, null, null, "High", List.of());
        Issue medium = new Issue("P-2", null, null, null, null, null, "Medium", List.of());
        Issue medium2 = new Issue("P-3", null, null, null, null, null, "Medium", List.of());

        when(issueService.getIssues("project = \"PROJ\"", 100))
                .thenReturn(List.of(high, medium, medium2));

        List<BinCount> result = dashboardService.getIssuesByPriority("PROJ", 100);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting("bin", "count")
                .contains(
                        tuple("High", 1),
                        tuple("Medium", 2)
                );
    }

    @Test
    void sanitizeProjectKey_ShouldAllowValidKeys() {
        dashboardService.getIssuesByPriority("My-PROJ_123", 10);
        // no exception → OK
    }

    @Test
    void sanitizeProjectKey_ShouldRejectInvalidKeys() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dashboardService.getIssuesByPriority("PROJ; DROP TABLE", 10)
        );
        assertThat(ex.getMessage()).contains("Invalid project key");
    }
}