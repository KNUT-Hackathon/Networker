package com.example.networker_test.repository.order;

import com.example.networker_test.domain.order.OrderInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface OrderInfoRepository extends JpaRepository<OrderInfo, String> {

    OrderInfo findByOrderId(String orderId);
    List<OrderInfo> findByUserid(String userid);
}
