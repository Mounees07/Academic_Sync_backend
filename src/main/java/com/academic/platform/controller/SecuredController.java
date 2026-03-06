package com.academic.platform.controller;

import com.academic.platform.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/secured")
public class SecuredController {

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping("/profile")
    public Map<String, Object> getMyProfile() {
        String uid = securityUtils.getCurrentUserUid();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a secured endpoint!");
        response.put("uid", uid);
        response.put("status", "success");

        return response;
    }
}
