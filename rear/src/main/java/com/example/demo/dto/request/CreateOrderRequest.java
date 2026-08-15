package com.example.demo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "创建订单请求")
public class CreateOrderRequest {
    @Schema(description = "用户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;
    @Schema(description = "场次 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long scheduleId;
    @Schema(description = "订单总金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalPrice;
    @Schema(description = "选座列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SeatRequest> seats;

    @Data
    @Schema(description = "选座请求")
    public static class SeatRequest {
        @Schema(description = "行号", example = "5")
        private int row;
        @Schema(description = "列号", example = "8")
        private int col;
    }
}