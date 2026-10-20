import { useEffect, useState, type FormEvent } from 'react'
import { api, errorMessage } from './api'
import type {
  ActivityEntry, ArchivedWorkspace, WorkspaceInvitation, WorkspaceMember, WorkspaceRole,
} from './types'

function dateLabel(value: string) {
  return new Date(value).toLocaleString()
}

function PanelError({ message, dismiss }: { message: string; dismiss: () => void }) {
  return message ? <p className="notice error" role="alert">{message}
    <button className="notice-close" onClick={dismiss} aria-label="Dismiss error">×</button>
  </p> : null
}

export function WorkspaceSettings({ workspaceId, workspaceName, onRenamed, onArchived }: {
  workspaceId: string
  workspaceName: string
  onRenamed: (name: string) => void
  onArchived: () => void
}) {
  const [role, setRole] = useState<WorkspaceRole | null>(null)
  const [members, setMembers] = useState<WorkspaceMember[]>([])
  const [sent, setSent] = useState<WorkspaceInvitation[]>([])
  const [name, setName] = useState(workspaceName)
  const [email, setEmail] = useState('')
  const [confirmArchive, setConfirmArchive] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => { setName(workspaceName) }, [workspaceName])
  useEffect(() => {
    let active = true
    setRole(null); setMembers([]); setSent([]); setError(''); setConfirmArchive(false); setLoading(true)
    Promise.all([
      api.get<{ role: WorkspaceRole }>(`/workspaces/${workspaceId}/membership`),
      api.get<WorkspaceMember[]>(`/workspaces/${workspaceId}/members`),
    ]).then(async ([roleResponse, membersResponse]) => {
      if (!active) return
      setRole(roleResponse.data.role)
      setMembers(membersResponse.data)
      if (roleResponse.data.role === 'OWNER') {
        const { data } = await api.get<WorkspaceInvitation[]>(`/workspaces/${workspaceId}/invitations`)
        if (active) setSent(data)
      }
    }).catch((err: unknown) => { if (active) setError(errorMessage(err)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [workspaceId])

  async function rename(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!name.trim() || name.trim() === workspaceName) return
    setBusy(true); setError(''); setNotice('')
    try {
      const { data } = await api.patch<{ name: string }>(`/workspaces/${workspaceId}`, { name: name.trim() })
      onRenamed(data.name)
      setNotice('Workspace renamed.')
    } catch (err) { setError(errorMessage(err)) }
    finally { setBusy(false) }
  }

  async function invite(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!email.trim()) return
    setBusy(true); setError(''); setNotice('')
    try {
      const { data } = await api.post<WorkspaceInvitation>(
        `/workspaces/${workspaceId}/invitations`, { email: email.trim() })
      setSent((current) => [data, ...current])
      setEmail('')
      setNotice(`Invitation created for ${data.invitedEmail}. They can accept it from their inbox.`)
    } catch (err) { setError(errorMessage(err)) }
    finally { setBusy(false) }
  }

  async function revoke(invitationId: string) {
    setBusy(true); setError(''); setNotice('')
    try {
      await api.delete(`/workspaces/${workspaceId}/invitations/${invitationId}`)
      setSent((current) => current.filter((item) => item.id !== invitationId))
      setNotice('Invitation revoked.')
    } catch (err) { setError(errorMessage(err)) }
    finally { setBusy(false) }
  }

  async function archive() {
    setBusy(true); setError('')
    try {
      await api.delete(`/workspaces/${workspaceId}`)
      onArchived()
    } catch (err) { setError(errorMessage(err)); setBusy(false) }
  }

  return <div className="hub-page">
    <div className="hub-heading"><p className="eyebrow">WORKSPACE MANAGEMENT</p>
      <h2>{workspaceName}</h2><p>People, invitations, and workspace settings.</p></div>
    <PanelError message={error} dismiss={() => setError('')} />
    {notice && <p className="notice success" role="status">{notice}</p>}
    <section className="hub-card">
      <div className="section-title"><h3>Members</h3><span>{members.length} people</span></div>
      <p className="section-caption">Members join only after accepting an invitation.</p>
      {loading ? <p className="section-caption">Loading members…</p>
        : <div className="member-list">{members.map((member) => <div className="member-row" key={member.userId}>
        <span className="member-avatar">{member.username.slice(0, 1).toUpperCase()}</span>
        <span className="member-identity"><strong>{member.username}</strong><small>{member.email}</small></span>
        <span className="role-badge">{member.role}</span>
      </div>)}</div>}
    </section>
    {role === 'OWNER' && <>
      <section className="hub-card">
        <div className="section-title"><h3>Invite someone</h3><span>Owner only</span></div>
        <p className="section-caption">Enter an existing account’s email. The invite appears in their Modus inbox; no email is sent.</p>
        <form className="hub-form" onSubmit={invite}>
          <label>Email address<input type="email" value={email} required placeholder="teammate@example.com"
            onChange={(event) => setEmail(event.target.value)} /></label>
          <button className="button primary" disabled={busy}>Send invitation</button>
        </form>
        {sent.length > 0 && <div className="sent-list"><h4>Pending invitations</h4>
          {sent.map((item) => <div className="sent-row" key={item.id}>
            <span><strong>{item.invitedEmail}</strong><small>Expires {dateLabel(item.expiresAt)}</small></span>
            <button className="button subtle" disabled={busy} onClick={() => void revoke(item.id)}>Revoke</button>
          </div>)}
        </div>}
      </section>
      <section className="hub-card">
        <div className="section-title"><h3>Rename workspace</h3><span>Owner only</span></div>
        <form className="hub-form" onSubmit={rename}>
          <label>Workspace name<input value={name} maxLength={120} required
            onChange={(event) => setName(event.target.value)} /></label>
          <button className="button secondary" disabled={busy || !name.trim() || name.trim() === workspaceName}>Save name</button>
        </form>
      </section>
      <section className="hub-card danger-card">
        <div className="section-title"><h3>Archive workspace</h3><span>Owner only</span></div>
        <p className="section-caption">The workspace and its boards become inaccessible until you restore it. Data is not deleted.</p>
        {!confirmArchive ? <button className="button danger" onClick={() => setConfirmArchive(true)}>Archive workspace</button>
          : <div className="confirm-actions"><span>Archive {workspaceName}?</span>
            <button className="button danger" disabled={busy} onClick={() => void archive()}>Yes, archive</button>
            <button className="button subtle" disabled={busy} onClick={() => setConfirmArchive(false)}>Cancel</button>
          </div>}
      </section>
    </>}
    {role === 'MEMBER' && <p className="section-caption">Only the workspace owner can invite people, rename, or archive this workspace.</p>}
  </div>
}

export function InvitationInbox({ onAccepted }: { onAccepted: (workspaceId: string) => void }) {
  const [invitations, setInvitations] = useState<WorkspaceInvitation[]>([])
  const [busyId, setBusyId] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    setLoading(true)
    api.get<WorkspaceInvitation[]>('/workspace-invitations')
      .then(({ data }) => { if (active) setInvitations(data) })
      .catch((err: unknown) => { if (active) setError(errorMessage(err)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [refreshKey])

  async function respond(item: WorkspaceInvitation, action: 'accept' | 'decline') {
    setBusyId(item.id); setError('')
    try {
      await api.post(`/workspace-invitations/${item.id}/${action}`)
      setInvitations((current) => current.filter((entry) => entry.id !== item.id))
      if (action === 'accept') onAccepted(item.workspaceId)
    } catch (err) { setError(errorMessage(err)) }
    finally { setBusyId(null) }
  }

  return <div className="hub-page">
    <div className="hub-heading"><p className="eyebrow">YOUR INBOX</p><h2>Invitations</h2>
      <p>Join a workspace only when you choose to accept.</p></div>
    <PanelError message={error} dismiss={() => setError('')} />
    <button className="button subtle refresh-button" onClick={() => setRefreshKey((key) => key + 1)}>Refresh invitations</button>
    {loading ? <div className="hub-empty">Loading invitations…</div>
      : invitations.length === 0 ? <div className="hub-empty">No pending invitations right now.</div>
      : invitations.map((item) => <article className="hub-card invite-card" key={item.id}>
        <div><p className="eyebrow">WORKSPACE INVITATION</p><h3>{item.workspaceName}</h3>
          <p>Invited as {item.invitedEmail}</p><small>Expires {dateLabel(item.expiresAt)}</small></div>
        <div className="invite-actions"><button className="button primary" disabled={busyId !== null}
          onClick={() => void respond(item, 'accept')}>Accept</button>
          <button className="button subtle" disabled={busyId !== null}
            onClick={() => void respond(item, 'decline')}>Decline</button></div>
      </article>)}
  </div>
}

export function ArchivedWorkspaces({ onRestored }: { onRestored: (workspaceId: string) => void }) {
  const [archived, setArchived] = useState<ArchivedWorkspace[]>([])
  const [busyId, setBusyId] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    api.get<ArchivedWorkspace[]>('/workspaces/archived')
      .then(({ data }) => { if (active) setArchived(data) })
      .catch((err: unknown) => { if (active) setError(errorMessage(err)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  async function restore(id: string) {
    setBusyId(id); setError('')
    try {
      await api.post(`/workspaces/${id}/restore`)
      setArchived((current) => current.filter((item) => item.id !== id))
      onRestored(id)
    } catch (err) { setError(errorMessage(err)) }
    finally { setBusyId(null) }
  }

  return <div className="hub-page">
    <div className="hub-heading"><p className="eyebrow">WORKSPACE ARCHIVE</p><h2>Archived workspaces</h2>
      <p>Owners can restore a workspace and its boards whenever they need it again.</p></div>
    <PanelError message={error} dismiss={() => setError('')} />
    {loading ? <div className="hub-empty">Loading archived workspaces…</div>
      : archived.length === 0 ? <div className="hub-empty">No archived workspaces.</div>
      : archived.map((item) => <article className="hub-card archive-row" key={item.id}>
        <span><strong>{item.name}</strong><small>Archived {dateLabel(item.archivedAt)}</small></span>
        <button className="button secondary" disabled={busyId !== null}
          onClick={() => void restore(item.id)}>Restore</button>
      </article>)}
  </div>
}

export function BoardActivity({ boardId }: { boardId: string }) {
  const [entries, setEntries] = useState<ActivityEntry[]>([])
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    setLoading(true)
    setEntries([])
    api.get<ActivityEntry[]>(`/boards/${boardId}/activity`)
      .then(({ data }) => { if (active) setEntries(data) })
      .catch((err: unknown) => { if (active) setError(errorMessage(err)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [boardId, refreshKey])

  return <div className="hub-page">
    <div className="hub-heading"><p className="eyebrow">BOARD HISTORY</p><h2>Activity</h2>
      <p>Recent card moves are written in the background after the move commits.</p></div>
    <PanelError message={error} dismiss={() => setError('')} />
    <button className="button subtle refresh-button" onClick={() => setRefreshKey((key) => key + 1)}>Refresh activity</button>
    {loading ? <div className="hub-empty">Loading activity…</div>
      : entries.length === 0 ? <div className="hub-empty">No activity recorded for this board yet.</div>
      : <div className="hub-card activity-list">{entries.map((item) => <div className="activity-row" key={item.id}>
        <span className="activity-dot" /><span><strong>{item.description}</strong>
          <small>{dateLabel(item.createdAt)}</small></span>
      </div>)}</div>}
  </div>
}
