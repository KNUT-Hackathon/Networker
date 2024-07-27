package com.example.networker_test.controller.main;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MainController {
	@GetMapping("/main")
	@ResponseBody
	public String mainMethod() {
		return "board";
	}
	
	@GetMapping("/")
	public String rootURL() {
		return "redirect:/main";
	}
	

}
