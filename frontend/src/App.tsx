import { useEffect, useState, type FormEvent } from 'react'
import { Client } from '@stomp/stompjs'
import axios from 'axios'
import {
  api, errorMessage, getAccessToken, login, logout, refreshAccessToken,
  register, subscribeToAuth,
} from './api'
import type { BoardDetails, BoardSummary, Card, CardMovedEvent, Workspace } from './types'

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
  }, [token])

  useEffect(() => {
    if (!workspaceId) { setBoards([]); setBoardId(null); return }
    let active = true
    api.get<BoardSummary[]>(`/workspaces/${workspaceId}/boards`).then(({ data }) => {
      if (!active) return
      setBoards(data)
      setBoardId((current) => data.some((item) => item.id === current) ? current : data[0]?.id ?? null)
    }).catch((err: unknown) => { if (active) setError(errorMessage(err)) })
    return () => { active = false }
  }, [workspaceId])

  useEffect(() => {
    if (!boardId) { setBoard(null); return }
    let active = true
    api.get<BoardDetails>(`/boards/${boardId}`).then(({ data }) => {
      if (active) setBoard((current) => mergeNewerMoves(data, current))
    }).catch((err: unknown) => { if (active) setError(errorMessage(err)) })
    return () => { active = false }
  }, [boardId])

  useEffect(() => {
    if (!boardId || !token) return
    let active = true
    const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${scheme}://${window.location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/board/${boardId}`, (message) => {
          const event = JSON.parse(message.body) as CardMovedEvent
          if (event.boardId !== boardId) return
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
      onStompError: () => setError('Live updates disconnected. Reload the board if it looks out of date.'),
    })
    client.activate()
    return () => { active = false; void client.deactivate() }
  }, [boardId, token])

  async function createWorkspace(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!newWorkspace.trim()) return
    try {
      const { data } = await api.post<Workspace>('/workspaces', { name: newWorkspace.trim() })
      setWorkspaces((current) => [...current, data])
      setWorkspaceId(data.id)
      setNewWorkspace(''); setError('')
    } catch (err) { setError(errorMessage(err)) }
  }

  async function createBoard(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!workspaceId || !newBoard.trim()) return
    try {
      const { data } = await api.post<BoardSummary>(`/workspaces/${workspaceId}/boards`, { name: newBoard.trim() })
      setBoards((current) => [...current, data])
      setBoardId(data.id)
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
            onClick={() => setWorkspaceId(workspace.id)}>
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
              onClick={() => setBoardId(item.id)}>▦ <span>{item.name}</span></button>)}
          </nav>
          <form className="sidebar-form" onSubmit={createBoard}>
            <label htmlFor="board-name">New board</label>
            <div className="inline-input"><input id="board-name" value={newBoard}
              onChange={(e) => setNewBoard(e.target.value)} placeholder="Board name" maxLength={120} />
              <button aria-label="Create board" type="submit">+</button></div>
          </form>
        </>}
        <div className="sidebar-bottom"><span className="status-dot" /> Connected to your team
          <button className="logout-button" onClick={() => void logout().catch((err: unknown) => setError(errorMessage(err)))}>Log out ↗</button>
        </div>
      </aside>
      <main className="main-content">
        <header className="topbar">
          <div><span className="breadcrumb">WORKSPACE / BOARD</span>
            <h1>{board?.name ?? (workspaceId ? 'Choose a board' : 'Your workspace starts here')}</h1></div>
          <span className="live-pill"><span className="status-dot" /> Live board</span>
        </header>
        {error && <div className="notice error page-notice" role="alert">{error}<button onClick={() => setError('')} aria-label="Dismiss error">×</button></div>}
        {board ? <>
          <div className="board-toolbar"><p>Move work from idea to done, together.</p><span>{board.lists.reduce((sum, list) => sum + list.cards.length, 0)} cards</span></div>
          <div className="board-columns">
            {board.lists.map((list) => <section key={list.id} className="board-column"
              onDragOver={(event) => event.preventDefault()}
              onDrop={(event) => {
                event.preventDefault()
                const cardId = event.dataTransfer.getData('text/plain')
                const card = board.lists.flatMap((item) => item.cards).find((item) => item.id === cardId)
                if (card) void moveCard(card, list.id)
              }}>
              <div className="column-heading"><span className="column-marker" /><h2>{list.name}</h2><span className="count">{list.cards.length}</span></div>
              <div className="card-stack">
                {list.cards.map((card) => <article className="task-card" key={card.id} draggable
                  onDragStart={(event) => event.dataTransfer.setData('text/plain', card.id)}>
                  <span className="card-kicker">TASK</span><h3>{card.title}</h3>
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
