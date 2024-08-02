package com.example.networker_test.repository.order;

import com.example.networker_test.domain.order.CartItems;
import com.example.networker_test.domain.order.OrderInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemsRepository extends JpaRepository<CartItems, Integer> {
    Object countByOrderInfo(OrderInfo orderInfo);

    List<CartItems> findByOrderInfo(OrderInfo orderInfo);
}
