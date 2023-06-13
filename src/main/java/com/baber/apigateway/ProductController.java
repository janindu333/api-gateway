package com.baber.apigateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ProductController {

	@GetMapping(path = "/products")
	public String getProducts(){ 
		return "products";
	}

	 
}
