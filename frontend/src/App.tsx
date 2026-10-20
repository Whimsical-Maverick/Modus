import { useEffect, useState, type FormEvent } from 'react'
import { Client } from '@stomp/stompjs'
import axios from 'axios'
import {
  api, errorMessage, getAccessToken, login, logout, refreshAccessToken,
  register, subscribeToAuth,
} from './api'
import type { BoardContentDeletedEvent, BoardDetails, BoardSummary, Card, CardMovedEvent, Workspace, WorkspaceRole } from './types'
import { ArchivedWorkspaces, BoardActivity, InvitationInbox, WorkspaceSettings } from './WorkspaceHub'

type View = 'board' | 'workspace' | 'inbox' | 'archived' | 'activity'
type DeleteTarget = { kind: 'card' | 'list' | 'board'; id: string; name: string; count: number }

function applyCardMove(board: BoardDetails, moved: Card): BoardDetails {
  if (!board.lists.some((list) => list.id === moved.listId)) return board
  const current = board.lists.flatMap((list) => list.cards)
    .find((card) => card.id === moved.id)
  if (!current || current.version >= moved.version) return board

  return {
    ...board,
    lists: board.lists.map((list) => ({
      ...list,
      cards: list.id === moved.listId
        ? [...list.cards.filter((card) => card.id !== moved.id), moved]
        : list.cards.filter((card) => card.id !== moved.id),
    })),
  }
}

function mergeNewerMoves(snapshot: BoardDetails, current: BoardDetails | null): BoardDetails {
  if (!current || current.id !== snapshot.id) return snapshot
  return current.lists.flatMap((list) => list.cards).reduce((result, card) =>
    applyCardMove(result, card), snapshot)
}

function AuthScreen() {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')
    try {
      if (mode === 'register') {
        await register(username, email, password)
        setMode('login')
        setMessage('Account created. Log in to continue.')
      } else {
        await login(email, password)
      }
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="auth-layout">
      <div className="auth-intro">
        <div className="brand"><span className="brand-mark">M</span> Modus</div>
        <div className="eyebrow">A clearer way to work together</div>
        <h1>Make room for<br /><em>the good work.</em></h1>
        <p>Workspaces, boards, and cards that keep your team moving in the same direction.</p>
        <div className="auth-preview" aria-hidden="true">
          <span className="preview-label">PROJECT FLOW</span>
          <div className="preview-columns">
            <div><strong>To Do</strong><span>Plan the launch</span><span>Review copy</span></div>
            <div><strong>In Progress</strong><span className="preview-active">Build the board ↗</span></div>
            <div><strong>Done</strong><span>Set the direction ✓</span></div>
          </div>
        </div>
      </div>
      <section className="auth-card" aria-label={mode === 'login' ? 'Log in' : 'Create account'}>
        <p className="eyebrow">WELCOME TO MODUS</p>
        <h2>{mode === 'login' ? 'Good to see you.' : 'Get started.'}</h2>
        <p className="muted">{mode === 'login' ? 'Log in to pick up where you left off.' : 'Create an account to organize your work.'}</p>
        <form onSubmit={submit}>
          {mode === 'register' && <label>Username<input autoComplete="username" required value={username} onChange={(e) => setUsername(e.target.value)} /></label>}
          <label>Email<input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></label>
          <label>Password<input type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required value={password} onChange={(e) => setPassword(e.target.value)} /></label>
          {mode === 'register' && <p className="hint">Use uppercase, lowercase, and a number.</p>}
          {error && <p className="notice error" role="alert">{error}</p>}
          {message && <p className="notice success" role="status">{message}</p>}
          <button className="button primary full" disabled={busy}>{busy ? 'One moment…' : mode === 'login' ? 'Log in' : 'Create account'}</button>
        </form>
        <p className="switch-mode">{mode === 'login' ? 'New here?' : 'Already have an account?'}{' '}
          <button className="text-button" type="button" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(''); setMessage('') }}>
            {mode === 'login' ? 'Create an account' : 'Log in'}
          </button>
        </p>
      </section>
    </main>
  )
}

export default function App() {
  const [token, setToken] = useState(getAccessToken())
  const [ready, setReady] = useState(false)
  const [workspaces, setWorkspaces] = useState<Workspace[]>([])
  const [workspaceId, setWorkspaceId] = useState<string | null>(null)
  const [boards, setBoards] = useState<BoardSummary[]>([])
  const [boardId, setBoardId] = useState<string | null>(null)
  const [board, setBoard] = useState<BoardDetails | null>(null)
  const [error, setError] = useState('')
  const [newWorkspace, setNewWorkspace] = useState('')
  const [newBoard, setNewBoard] = useState('')
  const [newList, setNewList] = useState('')
  const [cardTitles, setCardTitles] = useState<Record<string, string>>({})
  const [view, setView] = useState<View>('board')
  const [workspaceRefresh, setWorkspaceRefresh] = useState(0)
  const [liveConnected, setLiveConnected] = useState(false)
  const [workspaceRole, setWorkspaceRole] = useState<WorkspaceRole | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<DeleteTarget | null>(null)
  const [deleting, setDeleting] = useState(false)
  const selectedWorkspace = workspaces.find((workspace) => workspace.id === workspaceId)

  function selectWorkspace(id: string) {
    if (id === workspaceId) {
      setDeleteTarget(null); setView('board')
      return
    }
    setBoards([]); setBoardId(null); setBoard(null)
    setDeleteTarget(null); setWorkspaceRole(null)
    setWorkspaceId(id); setView('board')
  }

  function selectBoard(id: string) {
    setDeleteTarget(null)
    if (id === boardId) {
      setView('board')
      return
    }
    setBoard(null); setBoardId(id); setView('board')
  }

  useEffect(() => subscribeToAuth(setToken), [])
  useEffect(() => {
    refreshAccessToken().catch((err: unknown) => {
      if (!axios.isAxiosError(err) || err.response?.status !== 401) {
        setError(errorMessage(err))
      }
    }).finally(() => setReady(true))
  }, [])

  useEffect(() => {
    if (!token) {
      setWorkspaces([]); setWorkspaceId(null); setBoards([]); setBoardId(null); setBoard(null)
      return
    }
    let active = true
    api.get<Workspace[]>('/workspaces').then(({ data }) => {
      if (!active) return
      setWorkspaces(data)
      setWorkspaceId((current) => data.some((item) => item.id === current) ? current : data[0]?.id ?? null)
    }).catch((err: unknown) => { if (active) setError(errorMessage(err)) })
    return () => { active = false }
  }, [token, workspaceRefresh])

  useEffect(() => {
    if (!workspaceId) { setBoards([]); setBoardId(null); return }
    if (view !== 'board') return
    let active = true
    api.get<BoardSummary[]>(`/workspaces/${workspaceId}/boards`).then(({ data }) => {
      if (!active) return
      setBoards(data)
      setBoardId((current) => data.some((item) => item.id === current) ? current : data[0]?.id ?? null)
    }).catch((err: unknown) => { if (active) setError(errorMessage(err)) })
    return () => { active = false }
  }, [workspaceId, view])

  useEffect(() => {
    if (!workspaceId) { setWorkspaceRole(null); return }
    let active = true
    setWorkspaceRole(null)
    api.get<{ role: WorkspaceRole }>(`/workspaces/${workspaceId}/membership`)
      .then(({ data }) => { if (active) setWorkspaceRole(data.role) })
      .catch((err: unknown) => { if (active) setError(errorMessage(err)) })
    return () => { active = false }
  }, [workspaceId])

  useEffect(() => {
    if (!boardId) { setBoard(null); return }
    if (view !== 'board') return
    let active = true
    api.get<BoardDetails>(`/boards/${boardId}`).then(({ data }) => {
      if (active) setBoard((current) => mergeNewerMoves(data, current))
    }).catch((err: unknown) => { if (active) setError(errorMessage(err)) })
    return () => { active = false }
  }, [boardId, view])

  useEffect(() => {
    if (!boardId || !token) return
    let active = true
    setLiveConnected(false)
    const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${scheme}://${window.location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        if (active) setLiveConnected(true)
        client.subscribe(`/topic/board/${boardId}`, (message) => {
          const event = JSON.parse(message.body) as CardMovedEvent | BoardContentDeletedEvent
          if (event.boardId !== boardId) return
          if ('type' in event) {
            if (event.type === 'BOARD_DELETED') {
              setBoards((current) => current.filter((item) => item.id !== event.entityId))
              setBoardId((current) => current === event.entityId ? null : current)
              setBoard((current) => current?.id === event.entityId ? null : current)
              setDeleteTarget(null)
            } else if (event.type === 'LIST_DELETED') {
              setBoard((current) => current?.id === boardId
                ? { ...current, lists: current.lists.filter((list) => list.id !== event.entityId) }
                : current)
            } else {
              setBoard((current) => current?.id === boardId
                ? { ...current, lists: current.lists.map((list) => ({
                  ...list, cards: list.cards.filter((card) => card.id !== event.entityId),
                })) }
                : current)
            }
            return
          }
          setBoard((current) => current?.id === boardId ? applyCardMove(current, {
            id: event.cardId,
            listId: event.newListId,
            title: current.lists.flatMap((list) => list.cards)
              .find((card) => card.id === event.cardId)?.title ?? '',
            version: event.version,
          }) : current)
        })
        // The simple broker does not replay missed events after a disconnect.
        api.get<BoardDetails>(`/boards/${boardId}`).then(({ data }) => {
          if (active) setBoard((current) => mergeNewerMoves(data, current))
        }).catch((err: unknown) => { if (active) setError(errorMessage(err)) })
      },
      onWebSocketClose: () => { if (active) setLiveConnected(false) },
      onStompError: () => {
        if (active) {
          setLiveConnected(false)
          setError('Live updates disconnected. Reload the board if it looks out of date.')
        }
      },
    })
    client.activate()
    return () => { active = false; setLiveConnected(false); void client.deactivate() }
  }, [boardId, token])

  async function createWorkspace(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!newWorkspace.trim()) return
    try {
      const { data } = await api.post<Workspace>('/workspaces', { name: newWorkspace.trim() })
      setWorkspaces((current) => [...current, data])
      selectWorkspace(data.id)
      setNewWorkspace(''); setError('')
    } catch (err) { setError(errorMessage(err)) }
  }

  async function createBoard(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!workspaceId || !newBoard.trim()) return
    try {
      const { data } = await api.post<BoardSummary>(`/workspaces/${workspaceId}/boards`, { name: newBoard.trim() })
      setBoards((current) => [...current, data])
      selectBoard(data.id)
      setNewBoard(''); setError('')
    } catch (err) { setError(errorMessage(err)) }
  }

  async function createList(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!board || !newList.trim()) return
    try {
      const sortOrder = Math.max(-1, ...board.lists.map((list) => list.sortOrder)) + 1
      const { data } = await api.post<{ id: string; name: string; sortOrder: number }>(
        `/boards/${board.id}/lists`, { name: newList.trim(), sortOrder })
      setBoard((current) => current && ({ ...current,
        lists: [...current.lists, { ...data, cards: [] }] }))
      setNewList(''); setError('')
    } catch (err) { setError(errorMessage(err)) }
  }

  async function createCard(event: FormEvent<HTMLFormElement>, listId: string) {
    event.preventDefault()
    const title = cardTitles[listId]?.trim()
    if (!title) return
    try {
      const { data } = await api.post<Card>(`/board-lists/${listId}/cards`, { title })
      setBoard((current) => current && ({ ...current,
        lists: current.lists.map((list) => list.id === listId
          ? { ...list, cards: [...list.cards, data] } : list) }))
      setCardTitles((current) => ({ ...current, [listId]: '' }))
      setError('')
    } catch (err) { setError(errorMessage(err)) }
  }

  async function moveCard(card: Card, newListId: string) {
    if (!board || card.listId === newListId) return
    try {
      const { data } = await api.patch<Card>(`/cards/${card.id}/move`, {
        newListId, expectedVersion: card.version,
      })
      setBoard((current) => current && applyCardMove(current, data))
      setError('')
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 409 && boardId) {
        try {
          const { data } = await api.get<BoardDetails>(`/boards/${boardId}`)
          setBoard((current) => mergeNewerMoves(data, current))
          setError('Someone else moved this card first. The board has been reloaded.')
        } catch (reloadError) {
          setError(errorMessage(reloadError))
        }
      } else {
        setError(errorMessage(err))
      }
    }
  }

  async function confirmDelete() {
    if (!deleteTarget || deleting) return
    setDeleting(true)
    try {
      const { kind, id } = deleteTarget
      await api.delete(kind === 'board' ? `/boards/${id}`
        : kind === 'list' ? `/board-lists/${id}` : `/cards/${id}`)
      if (kind === 'board') {
        setBoards((current) => current.filter((item) => item.id !== id))
        setBoardId((current) => current === id ? null : current)
        setBoard((current) => current?.id === id ? null : current)
      } else if (kind === 'list') {
        setBoard((current) => current && ({ ...current,
          lists: current.lists.filter((list) => list.id !== id) }))
      } else {
        setBoard((current) => current && ({ ...current,
          lists: current.lists.map((list) => ({
            ...list, cards: list.cards.filter((card) => card.id !== id),
          })) }))
      }
      setDeleteTarget(null); setError('')
    } catch (err) { setError(errorMessage(err)) }
    finally { setDeleting(false) }
  }

  if (!ready) return <div className="loading-screen"><span className="brand-mark">M</span> Opening Modus…</div>
  if (!token) return <AuthScreen />

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">M</span> Modus</div>
        <p className="sidebar-label">YOUR WORKSPACES</p>
        <nav aria-label="Workspaces" className="nav-list">
          {workspaces.map((workspace) => <button key={workspace.id}
            className={`nav-item ${workspaceId === workspace.id ? 'selected' : ''}`}
            onClick={() => selectWorkspace(workspace.id)}>
            <span className="nav-dot">{workspace.name.slice(0, 1).toUpperCase()}</span>{workspace.name}
          </button>)}
        </nav>
        <form className="sidebar-form" onSubmit={createWorkspace}>
          <label htmlFor="workspace-name">New workspace</label>
          <div className="inline-input"><input id="workspace-name" value={newWorkspace}
            onChange={(e) => setNewWorkspace(e.target.value)} placeholder="Workspace name" maxLength={120} />
            <button aria-label="Create workspace" type="submit">+</button></div>
        </form>
        {workspaceId && <>
          <p className="sidebar-label boards-label">BOARDS</p>
          <nav aria-label="Boards" className="nav-list">
            {boards.map((item) => <button key={item.id}
              className={`nav-item board-nav ${boardId === item.id ? 'selected' : ''}`}
              onClick={() => selectBoard(item.id)}>▦ <span>{item.name}</span></button>)}
          </nav>
          <form className="sidebar-form" onSubmit={createBoard}>
            <label htmlFor="board-name">New board</label>
            <div className="inline-input"><input id="board-name" value={newBoard}
              onChange={(e) => setNewBoard(e.target.value)} placeholder="Board name" maxLength={120} />
              <button aria-label="Create board" type="submit">+</button></div>
          </form>
        </>}
        <p className="sidebar-label boards-label">MORE</p>
        <nav aria-label="More" className="nav-list utility-nav">
          {workspaceId && <button className={`nav-item ${view === 'workspace' ? 'selected' : ''}`}
            onClick={() => setView('workspace')}>⚙ <span>Workspace</span></button>}
          <button className={`nav-item ${view === 'inbox' ? 'selected' : ''}`}
            onClick={() => setView('inbox')}>✉ <span>Invitations</span></button>
          <button className={`nav-item ${view === 'archived' ? 'selected' : ''}`}
            onClick={() => setView('archived')}>▤ <span>Archived</span></button>
          {boardId && <button className={`nav-item ${view === 'activity' ? 'selected' : ''}`}
            onClick={() => setView('activity')}>◷ <span>Activity</span></button>}
        </nav>
        <div className="sidebar-bottom"><span className="status-dot" /> Connected to your team
          <button className="logout-button" onClick={() => void logout().catch((err: unknown) => setError(errorMessage(err)))}>Log out ↗</button>
        </div>
      </aside>
      <main className="main-content">
        <header className="topbar">
          <div><span className="breadcrumb">{view === 'board' ? 'WORKSPACE / BOARD' : 'MODUS / ' + view.toUpperCase()}</span>
            <h1>{view === 'board' ? board?.name ?? (workspaceId ? 'Choose a board' : 'Your workspace starts here')
              : view === 'workspace' ? selectedWorkspace?.name ?? 'Workspace'
              : view === 'inbox' ? 'Invitations'
              : view === 'archived' ? 'Archived workspaces' : 'Board activity'}</h1></div>
          {view === 'board' && board && <span className={`live-pill ${liveConnected ? '' : 'offline'}`}>
            <span className="status-dot" /> {liveConnected ? 'Live board' : 'Connecting…'}</span>}
        </header>
        {error && <div className="notice error page-notice" role="alert">{error}<button onClick={() => setError('')} aria-label="Dismiss error">×</button></div>}
        {deleteTarget && <div className="delete-confirm" role="alertdialog" aria-label={`Delete ${deleteTarget.kind}`}>
          <span>Delete {deleteTarget.name} permanently?{deleteTarget.kind === 'board'
            ? ` This also deletes ${deleteTarget.count} cards and all lists on this board.`
            : deleteTarget.kind === 'list' ? ` This also deletes ${deleteTarget.count} cards.` : ''}</span>
          <div className="delete-confirm-actions">
            <button type="button" className="button danger" disabled={deleting} onClick={() => void confirmDelete()}>
              {deleting ? 'Deleting…' : 'Delete permanently'}</button>
            <button type="button" className="button subtle" disabled={deleting} onClick={() => setDeleteTarget(null)}>Cancel</button>
          </div>
        </div>}
        {view === 'workspace' && workspaceId && selectedWorkspace ? <WorkspaceSettings
          key={workspaceId} workspaceId={workspaceId} workspaceName={selectedWorkspace.name}
          onRenamed={(name) => setWorkspaces((current) => current.map((item) =>
            item.id === workspaceId ? { ...item, name } : item))}
          onArchived={() => {
            setWorkspaceId(null); setBoardId(null); setBoard(null)
            setWorkspaceRefresh((current) => current + 1); setView('archived')
          }} />
        : view === 'inbox' ? <InvitationInbox onAccepted={(acceptedWorkspaceId) => {
          selectWorkspace(acceptedWorkspaceId)
          setWorkspaceRefresh((current) => current + 1)
        }} />
        : view === 'archived' ? <ArchivedWorkspaces onRestored={(restoredWorkspaceId) => {
          selectWorkspace(restoredWorkspaceId)
          setWorkspaceRefresh((current) => current + 1)
        }} />
        : view === 'activity' && boardId ? <BoardActivity boardId={boardId} />
        : view === 'board' && board ? <>
          <div className="board-toolbar"><p>Move work from idea to done, together.</p><div className="board-toolbar-actions">
            <span>{board.lists.reduce((sum, list) => sum + list.cards.length, 0)} cards</span>
            {workspaceRole === 'OWNER' && <button type="button" className="delete-link" onClick={() => setDeleteTarget({
              kind: 'board', id: board.id, name: `board “${board.name}”`,
              count: board.lists.reduce((sum, list) => sum + list.cards.length, 0),
            })}>Delete board</button>}
          </div></div>
          <div className="board-columns">
            {board.lists.map((list) => <section key={list.id} className="board-column"
              onDragOver={(event) => event.preventDefault()}
              onDrop={(event) => {
                event.preventDefault()
                const cardId = event.dataTransfer.getData('text/plain')
                const card = board.lists.flatMap((item) => item.cards).find((item) => item.id === cardId)
                if (card) void moveCard(card, list.id)
              }}>
              <div className="column-heading"><span className="column-marker" /><h2>{list.name}</h2><span className="count">{list.cards.length}</span>
                {workspaceRole === 'OWNER' && <button type="button" className="delete-icon" aria-label={`Delete list ${list.name}`}
                  onClick={() => setDeleteTarget({ kind: 'list', id: list.id, name: `list “${list.name}”`, count: list.cards.length })}>×</button>}
              </div>
              <div className="card-stack">
                {list.cards.map((card) => <article className="task-card" key={card.id} draggable
                  onDragStart={(event) => event.dataTransfer.setData('text/plain', card.id)}>
                  <div className="card-top"><span className="card-kicker">TASK</span>
                    <button type="button" className="delete-icon" aria-label={`Delete card ${card.title}`}
                      onClick={() => setDeleteTarget({ kind: 'card', id: card.id, name: `card “${card.title}”`, count: 0 })}>×</button>
                  </div><h3>{card.title}</h3>
                  <div className="card-footer"><span>#{card.id.slice(0, 6)}</span>
                    <label className="move-control">Move to <select aria-label={`Move ${card.title} to list`}
                      value={card.listId} onChange={(event) => void moveCard(card, event.target.value)}>
                      {board.lists.map((destination) => <option key={destination.id} value={destination.id}>{destination.name}</option>)}
                    </select></label></div>
                </article>)}
              </div>
              <form className="add-card" onSubmit={(event) => void createCard(event, list.id)}>
                <input aria-label={`New card in ${list.name}`} placeholder="Add a card…"
                  maxLength={200} value={cardTitles[list.id] ?? ''}
                  onChange={(event) => setCardTitles((current) => ({ ...current, [list.id]: event.target.value }))} />
                <button type="submit" aria-label={`Add card to ${list.name}`}>+</button>
              </form>
            </section>)}
            <form className="new-column" onSubmit={createList}>
              <span>＋ New list</span>
              <input aria-label="New list name" placeholder="e.g. In Progress" maxLength={120}
                value={newList} onChange={(event) => setNewList(event.target.value)} />
              <button type="submit" className="button secondary">Add list</button>
            </form>
          </div>
        </> : <div className="empty-state"><div className="empty-icon">▦</div>
          <h2>{workspaceId ? 'Make your first board' : 'Create a workspace to begin'}</h2>
          <p>{workspaceId ? 'Give your work a home, then add lists and cards.' : 'Your team’s best work starts with a shared space.'}</p>
        </div>}
      </main>
    </div>
  )
}
