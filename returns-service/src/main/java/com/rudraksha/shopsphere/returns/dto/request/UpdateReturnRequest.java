package com.rudraksha.shopsphere.returns.dto.request;

import com.rudraksha.shopsphere.returns.entity.Return;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReturnRequest {
    private Return.ReturnStatus status;
    private String trackingNumber;
    private String description;
}
