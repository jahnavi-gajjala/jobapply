import { useEffect, useState } from "react";
import { Activity, BriefcaseBusiness, FileUser, LayoutDashboard, Search, Sparkles, ScanSearch, Send, KanbanSquare, Sun, Moon, Plus, RefreshCw, LogOut, Menu, X } from "lucide-react";
import api from "./services/api";

const blank = { totalApplied: 0, underReview: 0, interviews: 0, offers: 0, rejected: 0, saved: 0 };
const statuses = ["SAVED", "PREPARING", "PENDING_APPROVAL", "SUBMITTED", "UNDER_REVIEW", "INTERVIEW", "REJECTED", "OFFER", "WITHDRAWN"];

function Auth({ onAuth }) {
  const [mode, setMode] = useState("login");
  const [fullName, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault(); setError(""); setBusy(true);
    try {
      const payload = mode === "login" ? { email, password } : { fullName, email, password };
      const res = await api.post(mode === "login" ? "/auth/login" : "/auth/register", payload);
      localStorage.setItem("jobapply_token", res.data.token);
      localStorage.setItem("jobapply_user", JSON.stringify(res.data.user));
      onAuth(res.data);
    } catch (err) {
      setError(err.response?.data?.message || "Could not sign in. Check your connection and try again.");
    } finally { setBusy(false); }
  }
  return <div className="auth-shell"><form className="auth-card" onSubmit={submit}>
    <div className="brand"><span className="brand-mark"><BriefcaseBusiness /></span><b>jobapply</b></div>
    <p className="eyebrow">YOUR CAREER COMMAND CENTER</p>
    <h1>{mode === "login" ? "Welcome back." : "Create your account."}</h1>
    <p className="muted">Keep your resumes, jobs and applications organized in one place.</p>
    {mode === "register" && <label>Full name<input required value={fullName} onChange={e => setName(e.target.value)} /></label>}
    <label>Email address<input type="email" required value={email} onChange={e => setEmail(e.target.value)} /></label>
    <label>Password<input type="password" minLength="8" required value={password} onChange={e => setPassword(e.target.value)} /></label>
    {error && <p className="error">{error}</p>}
    <button className="primary full" disabled={busy}>{busy ? "Please wait…" : mode === "login" ? "Sign in" : "Create account"}</button>
    <p className="switch">{mode === "login" ? "New to jobapply?" : "Already registered?"} <button type="button" onClick={() => { setMode(mode === "login" ? "register" : "login"); setError(""); }}>{mode === "login" ? "Create an account" : "Sign in"}</button></p>
  </form></div>;
}

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem("jobapply_token"));
  const [user, setUser] = useState(() => { try { return JSON.parse(localStorage.getItem("jobapply_user")); } catch { return null; } });
  const [dark, setDark] = useState(true);
  const [page, setPage] = useState("Overview");
  const [menu, setMenu] = useState(false);
  const [stats, setStats] = useState(blank);
  const [apps, setApps] = useState([]);
  const [jobs, setJobs] = useState([]);
  const [resumes, setResumes] = useState([]);
  const [notice, setNotice] = useState("");
  const [query, setQuery] = useState("");
  const [job, setJob] = useState({ companyName: "", jobTitle: "", jobDescription: "", location: "", jobType: "FULL_TIME", experienceLevel: "FRESHER", graduationYear: 2027, applicationUrl: "" });
  const [file, setFile] = useState(null);
  const [resumeTitle, setResumeTitle] = useState("");
  const [resumeId, setResumeId] = useState("");
  const [jobId, setJobId] = useState("");
  const [report, setReport] = useState(null);
  const [draft, setDraft] = useState("");
  const nav = [["Overview", LayoutDashboard], ["My Resume", FileUser], ["Find Jobs", Search], ["ATS Analyzer", ScanSearch], ["AI Resume Tailor", Sparkles], ["Applications", KanbanSquare], ["Auto Apply", Send]];

  async function refresh() {
    if (!token) return;
    try {
      const [s, a, j, r] = await Promise.all([api.get("/applications/stats"), api.get("/applications"), api.get("/jobs"), api.get("/resumes")]);
      setStats(s.data); setApps(a.data); setJobs(j.data); setResumes(r.data);
      if (!resumeId && r.data.length) setResumeId(String(r.data[0].id));
    } catch (err) {
      if (err.response?.status === 401) logout();
      else setNotice("Could not refresh your workspace. Check that the backend is running.");
    }
  }
  useEffect(() => { if (token) refresh(); }, [token]);
  function logout() {
    localStorage.removeItem("jobapply_token"); localStorage.removeItem("jobapply_user");
    setToken(null); setUser(null); setStats(blank); setApps([]); setJobs([]); setResumes([]);
  }
  function authenticated(data) { setUser(data.user); setToken(data.token); setPage("Overview"); }
  async function createJob(e) {
    e.preventDefault();
    try { await api.post("/jobs", job); setJob({ companyName: "", jobTitle: "", jobDescription: "", location: "", jobType: "FULL_TIME", experienceLevel: "FRESHER", graduationYear: 2027, applicationUrl: "" }); setNotice("Job added privately to your workspace."); await refresh(); }
    catch (err) { setNotice(err.response?.data?.message || "Could not add this job."); }
  }
  async function saveJob(id) {
    try { await api.post("/applications", { jobId: id }); setNotice("Job saved to your tracker."); await refresh(); }
    catch (err) { setNotice(err.response?.data?.message || "Could not save this job."); }
  }
  async function setStatus(id, status) {
    try { await api.patch("/applications/" + id + "/status", { status }); setNotice("Application status updated."); await refresh(); }
    catch { setNotice("Could not update the application status."); }
  }
  async function uploadResume(e) {
    e.preventDefault(); if (!file) return;
    const form = new FormData(); form.append("file", file); if (resumeTitle) form.append("title", resumeTitle);
    try { await api.post("/resumes/upload", form); setFile(null); setResumeTitle(""); setNotice("Resume text extracted and saved to your account."); await refresh(); }
    catch (err) { setNotice(err.response?.data?.message || "Resume upload failed."); }
  }
  async function analyze() {
    if (!resumeId || !jobId) { setNotice("Choose a resume and a job first."); return; }
    try { const res = await api.post("/ats/analyze", { resumeId: Number(resumeId), jobId: Number(jobId) }); setReport(res.data); }
    catch (err) { setNotice(err.response?.data?.message || "ATS analysis failed."); }
  }
  async function tailor() {
    if (!resumeId || !jobId) { setNotice("Choose a resume and a job first."); return; }
    try { const res = await api.post("/ai/tailor-resume", { resumeId: Number(resumeId), jobId: Number(jobId) }); setDraft(res.data.tailoredText); }
    catch (err) { setNotice(err.response?.data?.message || "Could not create a resume draft."); }
  }
  async function downloadTailoredDocx() {
    if (!resumeId || !jobId) { setNotice("Choose a resume and a job first."); return; }
    try {
      const response = await api.post("/ai/tailor-resume/export/docx",
        { resumeId: Number(resumeId), jobId: Number(jobId) }, { responseType: "blob" });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement("a");
      link.href = url; link.download = "jobapply-tailored-resume.docx";
      document.body.appendChild(link); link.click(); link.remove();
      URL.revokeObjectURL(url);
      setNotice("DOCX draft downloaded. Review formatting and every detail before applying.");
    } catch { setNotice("Could not export the tailored DOCX."); }
  }
  if (!token || !user) return <div className={dark ? "app dark" : "app light"}><Auth onAuth={authenticated} /></div>;
  const visibleJobs = jobs.filter(j => (j.jobTitle + " " + j.companyName + " " + (j.location || "")).toLowerCase().includes(query.toLowerCase()));
  const toolsPanel = <div className="panel tool-panel">
    <label>Resume<select value={resumeId} onChange={e => setResumeId(e.target.value)}><option value="">Choose resume</option>{resumes.map(r => <option value={r.id} key={r.id}>{r.title}</option>)}</select></label>
    <label>Job<select value={jobId} onChange={e => setJobId(e.target.value)}><option value="">Choose job</option>{jobs.map(j => <option value={j.id} key={j.id}>{j.jobTitle} — {j.companyName}</option>)}</select></label>
  </div>;
  return <div className={dark ? "app dark" : "app light"}>
    <aside className={"sidebar " + (menu ? "open" : "")}>
      <div className="brand"><span className="brand-mark"><BriefcaseBusiness /></span><b>jobapply</b><button className="icon-button close-menu" onClick={() => setMenu(false)}><X /></button></div>
      <p className="eyebrow">WORKSPACE</p><nav>{nav.map(([name, Icon]) => <button key={name} className={"nav-item " + (page === name ? "active" : "")} onClick={() => { setPage(name); setMenu(false); }}><Icon size={17} />{name}</button>)}</nav>
      <div className="sidebar-bottom"><div className="user-row"><div className="avatar">{(user.fullName || "U").slice(0, 2).toUpperCase()}</div><div><b>{user.fullName}</b><small>{user.email}</small></div></div><button className="nav-item" onClick={logout}><LogOut size={17} />Sign out</button></div>
    </aside>
    {menu && <button className="overlay" onClick={() => setMenu(false)} />}
    <main className="main">
      <header className="topbar"><div className="topbar-left"><button className="icon-button menu-button" onClick={() => setMenu(true)}><Menu /></button><span>Workspace / <b>{page}</b></span></div><div className="top-actions"><button className="icon-button" onClick={refresh} title="Refresh"><RefreshCw /></button><button className="icon-button" onClick={() => setDark(!dark)} title="Toggle theme">{dark ? <Sun /> : <Moon />}</button></div></header>
      <div className="content">
        {notice && <button className="notice" onClick={() => setNotice("")}>{notice} ×</button>}
        {page === "Overview" && <>
          <div className="page-heading"><p className="eyebrow">YOUR CAREER COMMAND CENTER</p><h1>Good things start <em>with a plan.</em></h1><p className="muted">Welcome back, {user.fullName}. Here is your job search at a glance.</p></div>
          <section className="hero"><div><span className="pill"><Sparkles size={14} /> CAREER WORKSPACE</span><h2>A smarter way to land your next role.</h2><p>Track applications, prepare resumes and keep your opportunities organized.</p><button className="primary" onClick={() => setPage("Find Jobs")}><Plus size={16} /> Explore jobs</button></div><div className="hero-count"><span>APPLICATIONS TRACKED</span><strong>{stats.totalApplied}</strong><small>based on statuses you mark</small></div></section>
          <div className="stats">{[["Applied", stats.totalApplied], ["Under review", stats.underReview], ["Interviews", stats.interviews], ["Offers", stats.offers], ["Saved", stats.saved], ["Rejected", stats.rejected]].map(([k, v]) => <article className="panel stat" key={k}><span>{k}</span><b>{v}</b></article>)}</div>
          <section className="panel"><div className="panel-head"><h3>Recent applications</h3><button className="link-button" onClick={() => setPage("Applications")}>View all</button></div>{apps.length ? apps.slice(0, 5).map(a => <div className="list-row" key={a.id}><div><b>{a.jobTitle}</b><small>{a.companyName}</small></div><span className="status">{a.status.replace("_", " ")}</span></div>) : <p className="muted">No applications yet. Save a job to start tracking it.</p>}</section>
        </>}
        {page === "Find Jobs" && <>
          <div className="page-heading"><p className="eyebrow">OPPORTUNITY DISCOVERY</p><h1>Find your <em>next role.</em></h1><p className="muted">Add job descriptions you find elsewhere. Add a job description you found elsewhere. Entries you add are private to your account.</p></div>
          <form className="panel form-panel" onSubmit={createJob}><h3>Add a job</h3><div className="form-grid">
            <input required placeholder="Company name" value={job.companyName} onChange={e => setJob({ ...job, companyName: e.target.value })} />
            <input required placeholder="Job title" value={job.jobTitle} onChange={e => setJob({ ...job, jobTitle: e.target.value })} />
            <input placeholder="Location" value={job.location} onChange={e => setJob({ ...job, location: e.target.value })} />
            <input placeholder="Application URL" value={job.applicationUrl} onChange={e => setJob({ ...job, applicationUrl: e.target.value })} />
          </div><textarea placeholder="Paste the job description…" value={job.jobDescription} onChange={e => setJob({ ...job, jobDescription: e.target.value })} /><button className="primary">Add to my jobs</button></form>
          <input className="search" placeholder="Search company, job title or location…" value={query} onChange={e => setQuery(e.target.value)} />
          {visibleJobs.map(j => <article className="panel list-row job-row" key={j.id}><div><b>{j.jobTitle}</b><small>{j.companyName} · {j.location || "Location not specified"}</small></div><div className="row-actions"><button className="secondary" onClick={() => saveJob(j.id)}>Save job</button>{j.applicationUrl && <a href={j.applicationUrl} target="_blank" rel="noreferrer">Employer site ↗</a>}</div></article>)}
          {!visibleJobs.length && <p className="muted">No jobs found. Add a job description above.</p>}
        </>}
        {page === "My Resume" && <>
          <div className="page-heading"><p className="eyebrow">CAREER PROFILE</p><h1>My <em>resume.</em></h1><p className="muted">Upload a text-based PDF or DOCX (maximum 5 MB).</p></div>
          <form className="panel form-panel" onSubmit={uploadResume}><label>Resume title<input value={resumeTitle} onChange={e => setResumeTitle(e.target.value)} placeholder="Master resume" /></label><label>Choose resume<input type="file" accept=".pdf,.docx" onChange={e => setFile(e.target.files?.[0] || null)} /></label><button className="primary" disabled={!file}>Upload and parse</button></form>
          <section className="panel"><h3>Your resumes</h3>{resumes.length ? resumes.map(r => <div className="list-row" key={r.id}><div><b>{r.title}</b><small>{r.fileName} · {r.characterCount} extracted characters</small></div><div className="row-actions">{r.isMaster ? <span className="status">MASTER</span> : <button className="secondary" onClick={async () => { try { await api.put("/resumes/" + r.id + "/master"); setNotice("Master resume updated."); await refresh(); } catch { setNotice("Could not update your master resume."); } }}>Set as master</button>}<button className="secondary" onClick={async () => { if (!window.confirm("Delete this resume?")) return; try { await api.delete("/resumes/" + r.id); setNotice("Resume deleted."); if (String(r.id) === resumeId) setResumeId(""); await refresh(); } catch { setNotice("Could not delete this resume."); } }}>Delete</button></div></div>) : <p className="muted">No resume uploaded yet.</p>}</section>
        </>}
        {page === "ATS Analyzer" && <>
          <div className="page-heading"><p className="eyebrow">RESUME INTELLIGENCE</p><h1>ATS <em>compatibility.</em></h1><p className="muted">An explainable estimate based on the job description, not an employer ATS score.</p></div>
          {toolsPanel}<button className="primary" onClick={analyze}>Analyze compatibility</button>
          {report && <section className="panel report"><div className="panel-head"><h3>Estimated score</h3><b className="score">{report.overallScore}/100</b></div><p className="muted">{report.disclaimer}</p><p>Keyword coverage {report.keywordCoverage}% · Structure {report.structureScore}%</p><h4>Matched terms</h4><p>{report.matchedKeywords.join(", ") || "None identified"}</p><h4>Terms to review</h4><p>{report.missingKeywords.join(", ") || "No missing terms identified"}</p><h4>Recommendations</h4><ul>{report.recommendations.map(t => <li key={t}>{t}</li>)}</ul></section>}
        </>}
        {page === "AI Resume Tailor" && <>
          <div className="page-heading"><p className="eyebrow">RESUME INTELLIGENCE</p><h1>Tailor your <em>resume.</em></h1><p className="muted">Creates a keyword-prioritized draft from your existing resume text.</p></div>
          {toolsPanel}<button className="primary" onClick={tailor}>Create tailored draft</button>
          {draft && <section className="panel"><div className="panel-head"><h3>Draft — review before using</h3><div className="row-actions"><button className="secondary" onClick={() => navigator.clipboard?.writeText(draft)}>Copy text</button><button className="primary" onClick={downloadTailoredDocx}>Download DOCX</button></div></div><textarea className="draft" value={draft} onChange={e => setDraft(e.target.value)} /><p className="muted">This MVP reorders existing lines; it does not create a formatted document or invent qualifications.</p></section>}
        </>}
        {page === "Applications" && <>
          <div className="page-heading"><p className="eyebrow">YOUR JOB SEARCH PIPELINE</p><h1>Application <em>tracker.</em></h1><p className="muted">Saved and preparation records are separate from submitted applications.</p></div>
          <section className="panel"><h3>All applications ({apps.length})</h3>{apps.length ? <div className="table-wrap"><table><thead><tr><th>Role</th><th>Added</th><th>Submitted</th><th>Status</th><th>Notes</th></tr></thead><tbody>{apps.map(a => <tr key={a.id}><td><b>{a.jobTitle}</b><small>{a.companyName}</small></td><td>{new Date(a.createdAt).toLocaleDateString()}</td><td>{a.submittedAt ? new Date(a.submittedAt).toLocaleDateString() : "—"}</td><td><select value={a.status} onChange={e => setStatus(a.id, e.target.value)}>{statuses.map(s => <option key={s}>{s}</option>)}</select></td><td><button className="secondary" onClick={async () => { const value = window.prompt("Private notes for this application:", a.notes || ""); if (value === null) return; try { await api.put("/applications/" + a.id + "/notes", { notes: value }); setNotice("Application notes saved."); await refresh(); } catch { setNotice("Could not save application notes."); } }}> {a.notes ? "Edit notes" : "Add notes"} </button></td></tr>)}</tbody></table></div> : <p className="muted">No records. Save a job from Find Jobs to begin.</p>}</section>
        </>}
        {page === "Auto Apply" && <><div className="page-heading"><p className="eyebrow">AUTOMATION</p><h1>Auto <em>Apply.</em></h1><p className="muted">Provider integrations are not enabled in this MVP.</p></div><section className="panel"><h3>Approval-first automation</h3><p className="muted">Automatic submission is not implemented. jobapply will only mark an application submitted after a supported provider confirms the submission. Your current tracker supports manual status updates.</p></section></>}
        <footer>© 2026 jobapply · Built for your next opportunity</footer>
      </div>
    </main>
  </div>;
}
