package com.kzkv.visisis.lab4.service;

import com.kzkv.visisis.lab4.dto.Issue;

import java.util.List;

public interface IssueService {

	List<Issue> getIssues(String jql, int maxResults);

}
