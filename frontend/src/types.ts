export type Workspace = { id: string; name: string }
export type WorkspaceRole = 'OWNER' | 'MEMBER'
export type WorkspaceMember = { userId: string; username: string; email: string; role: WorkspaceRole }
export type WorkspaceInvitation = {
  id: string
  workspaceId: string
  workspaceName: string
  invitedUserId: string
  invitedEmail: string
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'REVOKED'
  createdAt: string
  expiresAt: string
}
export type ArchivedWorkspace = { id: string; name: string; archivedAt: string }
export type ActivityEntry = {
  id: string
  cardId: string
  action: string
  description: string
  createdAt: string
}
export type BoardSummary = { id: string; workspaceId: string; name: string }
export type Card = { id: string; listId: string; title: string; version: number }
export type BoardList = {
  id: string
  name: string
  sortOrder: number
  cards: Card[]
}
export type BoardDetails = { id: string; name: string; lists: BoardList[] }

export type CardMovedEvent = {
  boardId: string
  cardId: string
  oldListId: string
  newListId: string
  version: number
}
