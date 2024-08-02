package com.example.networker_test.dto.order.orderinfo;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class UserOrderInfoDTO {
    private String orderId;
    private String image;  // 첫 번째 상품의 이미지 URL
    private String productName;  // 첫 번째 상품의 이름
    private int count;  // 총 구매한 상품의 개수
    private int totalAmount;  // 총 결제 금액
    private Date createdAt;  // 결제 일시
}
