package com.tracker.card;

import com.tracker.board.BoardList;
import com.tracker.board.BoardListRepository;
import com.tracker.card.dto.CardResponse;
import com.tracker.card.dto.CreateCardRequest;
import com.tracker.card.dto.MoveCardRequest;
import com.tracker.realtime.events.CardMovedEvent;
import com.tracker.realtime.events.BoardContentDeletedEvent;
import com.tracker.user.UserRepository;
import com.tracker.workspace.WorkspaceMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardService {

    private final BoardListRepository boardListRepository;
    private final CardRepository cardRepository;
    private final WorkspaceMembershipService membershipService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void deleteCard(UUID cardId, UUID currentUserId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
        BoardList list = card.getList();
        membershipService.requireMember(list.getBoard().getWorkspace().getId(), currentUserId);

        cardRepository.delete(card);
        cardRepository.flush();
        eventPublisher.publishEvent(new BoardContentDeletedEvent(
                list.getBoard().getId(), "CARD_DELETED", cardId));
    }

    @Transactional
    public CardResponse createCard(
            UUID listId,
            UUID currentUserId,
            CreateCardRequest request
    ) {
        BoardList list = boardListRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "List not found"));

        // Follow list → board → workspace before allowing the write.
        membershipService.requireMember(
                list.getBoard().getWorkspace().getId(),
                currentUserId
        );

        Card card = new Card();
        card.setList(list);
        card.setTitle(request.title().trim());

        Card saved = cardRepository.save(card);

        return new CardResponse(
                saved.getId(),
                listId,
                saved.getTitle(),
                saved.getVersion()
        );
    }

    @Transactional
    public CardResponse moveCard(UUID cardId, UUID currentUserId, MoveCardRequest request)
    {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Card not found"));

        BoardList oldList = card.getList();
        UUID boardId = oldList.getBoard().getId();

        membershipService.requireMember(
                oldList.getBoard().getWorkspace().getId(),
                currentUserId
        );

        BoardList newList = boardListRepository.findById(request.newListId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Destination list not found"));

        if (!newList.getBoard().getId().equals(boardId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Destination list must belong to the same board"
            );
        }

        if (card.getVersion() != request.expectedVersion()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Card has changed; reload it and try again"
            );
        }

        // A move to the current list changes nothing.
        if (oldList.getId().equals(newList.getId())) {
            return new CardResponse(
                    card.getId(),
                    oldList.getId(),
                    card.getTitle(),
                    card.getVersion()
            );
        }

        String actorName = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"))
                .getUsername();

        card.setList(newList);

        //Update the db to the current state
        cardRepository.flush();

        CardResponse response = new CardResponse(
                card.getId(),
                newList.getId(),
                card.getTitle(),
                card.getVersion()
        );

        eventPublisher.publishEvent(new CardMovedEvent(
                boardId,
                card.getId(),
                oldList.getId(),
                newList.getId(),
                card.getVersion(),
                currentUserId,
                actorName,
                card.getTitle(),
                oldList.getName(),
                newList.getName()
        ));

        return response;
    }
}
