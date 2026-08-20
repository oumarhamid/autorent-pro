package com.autorentpro.identity.api;

import com.autorentpro.identity.application.UserAdministrationService;

import java.util.List;

public record UserPageResponse(
        List<UserResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static UserPageResponse from(
            UserAdministrationService.UserPage page
    ) {
        return new UserPageResponse(
                page.items()
                        .stream()
                        .map(UserResponse::from)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    public UserPageResponse {
        items = List.copyOf(items);
    }
}