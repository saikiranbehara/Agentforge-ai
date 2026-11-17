package com.agentforge.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest {
    private Boolean approved;
    private String feedback;
}
