package com.kzkv.visisis.lab4;

import com.kzkv.visisis.lab4.dto.BinCount;
import com.kzkv.visisis.lab4.dto.DailyStats;
import com.kzkv.visisis.lab4.dto.UserCount;
import com.kzkv.visisis.lab4.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	@GetMapping
	public String dashboard(
			@RequestParam(defaultValue = "KAFKA") String projectKey,
			@RequestParam(defaultValue = "200") int maxResults,
			Model model) {

		model.addAttribute("projectKey", projectKey);

		List<BinCount> issuesByOpenedTime = dashboardService.getIssuesByOpenedTime(projectKey, maxResults);
		Map<String, List<BinCount>> statusTimeDistribution = dashboardService.getStatusTimeDistribution(projectKey, maxResults);
		List<DailyStats> dailyTaskStats = dashboardService.getDailyTaskStats(projectKey, maxResults);
		List<UserCount> topUsers = dashboardService.getTopUsers(projectKey, maxResults);
		List<BinCount> assignedIssuesTimeDistribution = dashboardService.getAssignedIssuesTimeDistribution(projectKey, maxResults);
		List<BinCount> issuesByPriority = dashboardService.getIssuesByPriority(projectKey, maxResults);

		log.info("issuesByOpenedTime={}", !issuesByOpenedTime.isEmpty());
		log.info("statusTimeDistribution={}", !statusTimeDistribution.isEmpty());
		log.info("dailyTaskStats={}", !dailyTaskStats.isEmpty());
		log.info("topUsers={}", !topUsers.isEmpty());
		log.info("assignedIssuesTimeDistribution={}", !assignedIssuesTimeDistribution.isEmpty());
		log.info("issuesByPriority={}", !issuesByPriority.isEmpty());

		model.addAttribute("timeToCloseData", issuesByOpenedTime);
		model.addAttribute("statusTimeData", statusTimeDistribution);
		model.addAttribute("dailyStats", dailyTaskStats);
		model.addAttribute("topUsers", topUsers);
		model.addAttribute("assignedTimeData", assignedIssuesTimeDistribution);
		model.addAttribute("priorityData", issuesByPriority);

		return "dashboard";
	}

}
