package com.bbd.procurement.global.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/error")
public class ErrorTestController {

    @GetMapping
    public ResponseEntity<Void> test1() {
        throw new ApiException(HttpStatus.NOT_FOUND, "404", "Not Found", "찾을 수 없습니다.");
    }

}
