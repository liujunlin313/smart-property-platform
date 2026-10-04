package com.smartproperty.platform.map.boundary.controller;

import com.smartproperty.platform.map.boundary.dto.BoundarySaveRequest;
import com.smartproperty.platform.map.boundary.service.SpatialBoundaryService;
import com.smartproperty.platform.map.boundary.vo.SpatialBoundaryVO;
import com.smartproperty.platform.map.boundary.vo.SpatialSearchResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/map")
public class SpatialBoundaryController {
    private final SpatialBoundaryService spatialBoundaryService;
    public SpatialBoundaryController(SpatialBoundaryService spatialBoundaryService) { this.spatialBoundaryService = spatialBoundaryService; }
    @GetMapping("/search") public List<SpatialSearchResultVO> search(@RequestParam String keyword) { return spatialBoundaryService.search(keyword); }
    @GetMapping("/boundaries/{subjectType}/{subjectId}") public SpatialBoundaryVO getBoundary(@PathVariable String subjectType, @PathVariable Long subjectId) { return spatialBoundaryService.getBoundary(subjectType, subjectId); }
    @PutMapping("/boundaries/{subjectType}/{subjectId}") public SpatialBoundaryVO saveBoundary(@PathVariable String subjectType, @PathVariable Long subjectId, @Valid @RequestBody BoundarySaveRequest body) { return spatialBoundaryService.saveBoundary(subjectType, subjectId, body); }
}
