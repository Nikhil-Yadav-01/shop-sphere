package com.rudraksha.shopsphere.recommendation.service.impl;

import com.rudraksha.shopsphere.recommendation.dto.request.UserInteractionRequest;
import com.rudraksha.shopsphere.recommendation.dto.response.UserInteractionResponse;
import com.rudraksha.shopsphere.recommendation.entity.UserInteraction;
import com.rudraksha.shopsphere.recommendation.repository.UserInteractionRepository;
import com.rudraksha.shopsphere.recommendation.service.UserInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInteractionServiceImpl implements UserInteractionService {

    private final UserInteractionRepository userInteractionRepository;

    @Override
    public UserInteractionResponse recordInteraction(UserInteractionRequest request) {
        UserInteraction interaction = UserInteraction.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .interactionType(request.getInteractionType())
                .interactionScore(calculateInteractionScore(request.getInteractionType()))
                .userCategory(request.getUserCategory())
                .build();

        UserInteraction saved = userInteractionRepository.save(interaction);
        return mapToResponse(saved);
    }

    @Override
    public UserInteractionResponse getInteractionById(String id) {
        UserInteraction interaction = userInteractionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Interaction not found with id: " + id));
        return mapToResponse(interaction);
    }

    @Override
    public List<UserInteractionResponse> getUserInteractions(String userId) {
        return userInteractionRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserInteractionResponse> getProductInteractions(String productId) {
        return userInteractionRepository.findByProductId(productId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserInteractionResponse updateInteraction(String id, UserInteractionRequest request) {
        UserInteraction interaction = userInteractionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Interaction not found with id: " + id));

        interaction.setInteractionType(request.getInteractionType());
        interaction.setUserCategory(request.getUserCategory());
        interaction.setInteractionScore(calculateInteractionScore(request.getInteractionType()));

        UserInteraction updated = userInteractionRepository.save(interaction);
        return mapToResponse(updated);
    }

    @Override
    public void deleteInteraction(String id) {
        userInteractionRepository.deleteById(id);
    }

    @Override
    public void deleteUserInteractions(String userId) {
        userInteractionRepository.deleteByUserId(userId);
    }

    @Override
    public List<UserInteractionResponse> getInteractionsByType(String type) {
        UserInteraction.InteractionType interactionType = UserInteraction.InteractionType.valueOf(type.toUpperCase());
        return userInteractionRepository.findByInteractionType(interactionType).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Integer calculateInteractionScore(UserInteraction.InteractionType type) {
        return switch (type) {
            case PURCHASE -> 100;
            case REVIEW -> 50;
            case ADD_TO_CART -> 30;
            case WISHLIST -> 25;
            case VIEW -> 10;
            case CLICK -> 5;
        };
    }

    private UserInteractionResponse mapToResponse(UserInteraction interaction) {
        return UserInteractionResponse.builder()
                .id(interaction.getId())
                .userId(interaction.getUserId())
                .productId(interaction.getProductId())
                .interactionType(interaction.getInteractionType())
                .interactionScore(interaction.getInteractionScore())
                .userCategory(interaction.getUserCategory())
                .createdAt(interaction.getCreatedAt())
                .build();
    }
}
