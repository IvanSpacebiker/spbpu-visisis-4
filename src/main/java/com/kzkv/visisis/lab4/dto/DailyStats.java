package com.kzkv.visisis.lab4.dto;

import java.time.LocalDate;

public record DailyStats(LocalDate date, int created, int resolved, int cumulativeCreated, int cumulativeResolved) {}