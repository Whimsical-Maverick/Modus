package com.tracker.card;

import com.tracker.board.BoardList;
import com.tracker.board.BoardListRepository;
import com.tracker.card.dto.CardResponse;
import com.tracker.card.dto.CreateCardRequest;
import com.tracker.workspace.WorkspaceMembershipService;
import lombok.RequiredArgsConstructor;
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
}