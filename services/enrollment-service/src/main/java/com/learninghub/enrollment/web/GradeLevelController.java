package com.learninghub.enrollment.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

import com.learninghub.enrollment.domain.GradeLevel;
import com.learninghub.enrollment.repo.GradeLevelRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Grade levels (high-school classes): GET /api/enrollment/grade-levels for any signed-in user (maintained by Flyway; Ask 27: no CRUD). */
@RestController
@Tag(name = "Grade levels", description = "The four high-school classes (Freshman, Sophomore, Junior, Senior) for any signed-in user")
@RequestMapping("/api/enrollment/grade-levels")
public class GradeLevelController {
    private final GradeLevelRepository levels;

    public GradeLevelController(GradeLevelRepository repo) {
        this.levels = repo;
    }

    @Operation(summary = "List grade levels", description = "Ordered by sort_order.")
    @ApiResponse(responseCode = "200", description = "Grade levels", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.GRADE_LEVELS)))
    @GetMapping
    public List<GradeLevel> list() {
        return levels.findAllByOrderBySortOrderAsc();
    }
}
