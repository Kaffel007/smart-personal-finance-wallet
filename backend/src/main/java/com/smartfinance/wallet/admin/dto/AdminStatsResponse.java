package com.smartfinance.wallet.admin.dto;

public record AdminStatsResponse(long totalUsers, long activeUsers, long blockedUsers, long adminUsers) {
}
