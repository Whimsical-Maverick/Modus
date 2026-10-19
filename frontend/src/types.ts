export type Workspace = { id: string; name: string }
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
