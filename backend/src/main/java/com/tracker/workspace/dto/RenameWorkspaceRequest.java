package com.tracker.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameWorkspaceRequest(@NotBlank @Size(max=120) String name)
{

}
