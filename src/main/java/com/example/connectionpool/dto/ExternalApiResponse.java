package com.example.connectionpool.dto;

import lombok.Data;

@Data
public class ExternalApiResponse {
    private Long userId;
    private Long id;
    private String title;
    private String body;
}

