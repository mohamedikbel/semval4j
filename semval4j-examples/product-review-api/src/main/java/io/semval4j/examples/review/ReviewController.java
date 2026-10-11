package io.semval4j.examples.review;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ordinary Spring controller: no provider calls, no SemVal4j types.
 * Semantic validation happens transparently through {@code @Valid}.
 */
@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews")
    public ResponseEntity<Void> create(@Valid @RequestBody ProductReview review) {
        reviewService.save(review);
        return ResponseEntity.noContent().build();
    }
}
