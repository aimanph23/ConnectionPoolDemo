package com.example.connectionpool.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String address;
}
