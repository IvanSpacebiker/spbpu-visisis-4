package com.kzkv.visisis.lab4.service;

import com.kzkv.visisis.lab4.dto.BinCount;
import com.kzkv.visisis.lab4.dto.DailyStats;
import com.kzkv.visisis.lab4.dto.UserCount;

import java.util.List;
import java.util.Map;

public interface DashboardService {

	List<BinCount> getIssuesByOpenedTime(String projectKey, int maxResults);

	Map<String, List<BinCount>> getStatusTimeDistribution(String projectKey, int maxResults);

	List<DailyStats> getDailyTaskStats(String projectKey, int maxResults);

	List<UserCount> getTopUsers(String projectKey, int maxResults);

	List<BinCount> getAssignedIssuesTimeDistribution(String projectKey, int maxResults);

	List<BinCount> getIssuesByPriority(String projectKey, int maxResults);

}
