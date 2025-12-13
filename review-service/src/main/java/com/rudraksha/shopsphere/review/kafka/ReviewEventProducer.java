package com.rudraksha.shopsphere.review.kafka;

import com.rudraksha.shopsphere.review.dto.response.ReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String REVIEW_CREATED_TOPIC = "review-created";
    private static final String REVIEW_UPDATED_TOPIC = "review-updated";
    private static final String REVIEW_DELETED_TOPIC = "review-deleted";
    private static final String REVIEW_APPROVED_TOPIC = "review-approved";

    public void publishReviewCreated(ReviewResponse review) {
        log.info("Publishing review created event for review: {}", review.getId());
        kafkaTemplate.send(REVIEW_CREATED_TOPIC, review.getId(), review);
    }

    public void publishReviewUpdated(ReviewResponse review) {
        log.info("Publishing review updated event for review: {}", review.getId());
        kafkaTemplate.send(REVIEW_UPDATED_TOPIC, review.getId(), review);
    }

    public void publishReviewDeleted(String reviewId) {
        log.info("Publishing review deleted event for review: {}", reviewId);
        kafkaTemplate.send(REVIEW_DELETED_TOPIC, reviewId, "Review deleted: " + reviewId);
    }

    public void publishReviewApproved(ReviewResponse review) {
        log.info("Publishing review approved event for review: {}", review.getId());
        kafkaTemplate.send(REVIEW_APPROVED_TOPIC, review.getId(), review);
    }
}
