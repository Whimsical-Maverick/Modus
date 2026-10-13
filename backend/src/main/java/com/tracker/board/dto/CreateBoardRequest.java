package com.tracker.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardRequest(@NotBlank @Size(max=120) String name)
{

}
