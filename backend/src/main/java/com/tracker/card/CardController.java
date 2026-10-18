package com.tracker.card;

import com.tracker.card.dto.CardResponse;
import com.tracker.card.dto.CreateCardRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CardController
{
    private final CardService cardService;

    @PostMapping("/board-lists/{listId}/cards")
    public ResponseEntity<CardResponse> createCard(
            @PathVariable UUID listId,
            @Valid @RequestBody CreateCardRequest request,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        CardResponse response =
                cardService.createCard(listId, currentUserId, request);

        return ResponseEntity
                .created(URI.create("/api/v1/cards/" + response.id()))
                .body(response);
    }
}

