package io.semval4j.examples.review;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ReviewService {

    private final List<ProductReview> reviews = new CopyOnWriteArrayList<>();

    public void save(ProductReview review) {
        reviews.add(review);
    }

    public List<ProductReview> findAll() {
        return List.copyOf(reviews);
    }
}
