package com.example.connectionpool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionPoolInfo {
    private String poolName;
    private Integer totalConnections;
    private Integer activeConnections;
    private Integer idleConnections;
    private Integer threadsAwaitingConnection;
    private Integer maximumPoolSize;
    private Integer minimumIdle;
    private Long connectionTimeout;
    private Long idleTimeout;
    private Long maxLifetime;
    private String status;
    private Long timestamp;
}

