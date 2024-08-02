package com.example.networker_test.controller.order;

import com.example.networker_test.dto.order.orderinfo.OrderInfoDTO;
import com.example.networker_test.dto.order.orderinfo.UserOrderInfoDTO;
import com.example.networker_test.dto.order.paymentinfo.PaymentInfoDTO;
import com.example.networker_test.dto.order.request.OrderRequest;
import com.example.networker_test.service.order.OrderService;
import org.json.simple.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    private final OrderService orderService;


    public OrderController(OrderService orderService){
        this.orderService = orderService;
    }

    @RequestMapping(value = "/confirm")
    public ResponseEntity<JSONObject> confirmPayment(@RequestBody String jsonBody) throws Exception {
        return orderService.confirmPayment(jsonBody);
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> processOrder(@RequestBody OrderRequest.OrderData orderData) {
        return orderService.processOrder(orderData);
    }

    @GetMapping("/payinfo")
    public ResponseEntity<?> getPaymentInfo(@RequestParam String orderId) {
        return orderService.getPaymentInfo(orderId);
    }

    @GetMapping("/orderinfo")
    public ResponseEntity<List<UserOrderInfoDTO>> getUserOrderInfo(Authentication authentication) {
        String userId = authentication.getName();
        List<UserOrderInfoDTO> orders = orderService.getUserOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

}
