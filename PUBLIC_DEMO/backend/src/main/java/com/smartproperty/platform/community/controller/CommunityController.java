package com.smartproperty.platform.community.controller;

import com.smartproperty.platform.community.service.CommunityService;
import com.smartproperty.platform.community.vo.CommunityListVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("stage1CommunityController")
@RequestMapping("/api/admin/communities")
public class CommunityController {
    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public List<CommunityListVO> listCommunities() {
        return communityService.listCommunities();
    }
}
