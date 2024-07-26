package com.example.networker_test.controller.lawsupport;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/lawsupport")
public class LawSupportController {
	@GetMapping("/main")
	public String mainPage() {
		return "lawsupport";
	}

}
