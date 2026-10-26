import { useEffect, useRef, useState } from 'react'
import type { ChangeEvent, FormEvent, ReactNode } from 'react'
import { Link, NavLink, Navigate, Route, Routes, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { ArrowRight, Atom, Bell, BookOpen, Check, ChevronRight, CircleHelp, FlaskConical, Leaf, LockKeyhole, Menu, Microscope, Monitor, ShieldCheck, Trophy, X } from 'lucide-react'
import { api, downloadDocument, type District } from './api'
import { useAuth } from './auth'

const subjects = [
  { name: 'Mathematics', icon: Atom }, { name: 'Physics', icon: Microscope },
  { name: 'Chemistry', icon: FlaskConical }, { name: 'Biology', icon: Leaf },
  { name: 'Agricultural Science', icon: Leaf }, { name: 'ICT / Technology', icon: Monitor },
]
type Sponsor = { id: string; name: string; organization?: string; description?: string; websiteUrl?: string; logoUrl?: string }
type PolicySection = { id: number; section: string; title: string; content: string }

type PartnerAd = {
  brand: string
  mark: string
  headline: string
  description: string
  accent: string
  logoSrc?: string
  videoSrc?: string
  posterSrc?: string
}

const partnerAds: PartnerAd[] = [
  {
    brand: 'SAMPLE BRAND 01',
    mark: 'ST',
    headline: 'Ideas grow with opportunity.',
    description: 'A sample message showing where a future education partner can share its work.',
    accent: '#28564a',
  },
  {
    brand: 'SAMPLE BRAND 02',
    mark: 'SF',
    headline: 'Make room for discovery.',
    description: 'Partner with young people exploring science, technology and new possibilities.',
    accent: '#315877',
  },
  {
    brand: 'SAMPLE BRAND 03',
    mark: 'IF',
    headline: 'Curiosity builds tomorrow.',
    description: 'This space is ready for a confirmed partner logo, video or short promotion.',
    accent: '#70523d',
  },
]

function PartnerAdvertisement() {
  const [adIndex, setAdIndex] = useState(0)
  const [visible, setVisible] = useState(false)
  const [dismissed, setDismissed] = useState(false)
  const ad = partnerAds[adIndex]

  useEffect(() => {
    if (dismissed) return
    let timeout = 0
    let active = true
    const showNext = () => {
      if (!active) return
      timeout = window.setTimeout(() => {
        if (!active) return
        setVisible(true)
        timeout = window.setTimeout(() => {
          if (!active) return
          setVisible(false)
          timeout = window.setTimeout(() => {
            if (!active) return
            setAdIndex(index => (index + 1) % partnerAds.length)
            showNext()
          }, 3600)
        }, 7200)
      }, 1200)
    }
    showNext()
    return () => {
      active = false
      window.clearTimeout(timeout)
    }
  }, [dismissed])

  return <aside className={`partner-ad-shell ${visible ? 'partner-ad-visible' : ''}`} aria-hidden={!visible}>
    <section className="partner-ad-card" aria-label={`${ad.brand} advertisement`}>
      <div className="partner-ad-visual" style={{ backgroundColor: ad.accent }}>
        {ad.logoSrc
          ? <img src={ad.logoSrc} alt="" loading="lazy"/>
          : ad.videoSrc && visible
            ? <video autoPlay muted loop playsInline preload="metadata" poster={ad.posterSrc}><source src={ad.videoSrc} type="video/mp4"/></video>
            : <span>{ad.mark}</span>}
      </div>
      <div className="partner-ad-copy">
        <span className="partner-ad-label">{ad.brand}</span>
        <b>{ad.headline}</b>
        <p>{ad.description}</p>
      </div>
      <button className="partner-ad-close" type="button" onClick={() => { setVisible(false); setDismissed(true) }} aria-label="Close partner advertisement" tabIndex={visible ? 0 : -1}>×</button>
    </section>
  </aside>
}

function useSponsors() {
  const [data, setData] = useState<Sponsor[] | null>(null)
  const [error, setError] = useState('')
  useEffect(() => { api<Sponsor[]>('/sponsors').then(setData).catch(e => setError(e.message)) }, [])
  return { data, error }
}

function sponsorLogo(sponsor: Sponsor) {
  return sponsor.logoUrl || (sponsor.name.toLowerCase().includes('shamaden imole') ? '/images/shamaden-imole-foundation.jfif' : undefined)
}

function Brand({ admin = false, to }: { admin?: boolean; to?: string }) {
  return <Link className="brand" to={to || (admin ? '/admin' : '/')} aria-label="HSSC home">
    <span className="brand-logo"><img src="/images/hssc-logo.png" alt="" /><video autoPlay muted loop playsInline preload="metadata" poster="/images/hssc-logo.png" aria-hidden="true"><source src="/assets/hssc-logo-animation.mp4" type="video/mp4"/></video></span>
    <span>HSSC<small>{admin ? 'ADMINISTRATION' : 'HIGH SCHOOL STEM CHAMPIONSHIP'}</small></span>
  </Link>
}

function BrandLoader() {
  return <span className="brand-loader" aria-hidden="true"><img src="/images/hssc-logo.png" alt="" /><video autoPlay muted loop playsInline preload="metadata" poster="/images/hssc-logo.png"><source src="/assets/hssc-logo-animation.mp4" type="video/mp4"/></video></span>
}

function SponsorTicker() {
  const { data } = useSponsors()
  const sponsors = data?.length ? data : null
  const renderGroup = (duplicate: boolean) => <div className="sponsor-marquee-group" aria-hidden={duplicate}>
    {sponsors ? [...sponsors, ...sponsors].map((sponsor, index) => {
      const content = <>{sponsor.logoUrl && <img src={sponsor.logoUrl} alt="" loading="lazy"/>}<b>{sponsor.name}</b></>
      return sponsor.websiteUrl
        ? <a className="sponsor-ticker-item" href={sponsor.websiteUrl} target="_blank" rel="noreferrer" key={`${sponsor.id}-${index}`}>{content}</a>
        : <span className="sponsor-ticker-item" key={`${sponsor.id}-${index}`}>{content}</span>
    }) : Array.from({ length: 8 }, (_, index) => <span className="sponsor-ticker-item sponsor-ticker-placeholder" key={index}>Sponsor opportunities</span>)}
  </div>
  return <aside className="sponsor-ticker" aria-label="HSSC sponsors">
    <span className="sponsor-ticker-label">HSSC PARTNERS</span>
    <div className="sponsor-marquee"><div className="sponsor-marquee-track">{renderGroup(false)}{renderGroup(true)}</div></div>
  </aside>
}

function Header() {
  const [open, setOpen] = useState(false)
  const { user, signOut } = useAuth()
  const dashboardPath = user?.role === 'SCHOOL' ? '/school/dashboard' : '/dashboard'
  const closeMenu = () => setOpen(false)
  return <header className={user && user.role !== 'ADMIN' ? 'site-header user-dashboard-header' : 'site-header'}>
    <Brand to={user && user.role !== 'ADMIN' ? dashboardPath : undefined}/>
    <button className="mobile-menu" onClick={() => setOpen(!open)} aria-label={open ? 'Close navigation' : 'Open navigation'} aria-expanded={open}>{open ? <X /> : <Menu />}</button>
    <nav className={open ? 'main-nav nav-open' : 'main-nav'} aria-label="Main navigation">
      {user ? <>
        {user.role === 'SCHOOL' ? (
          <>
            <NavLink to="/policy" onClick={closeMenu}>Privacy Policy</NavLink>
            <NavLink to="/sponsors" onClick={closeMenu}>Sponsors</NavLink>
            <NavLink to="/school/dashboard" onClick={closeMenu}>Dashboard</NavLink>
            <div className="mobile-dashboard-links" aria-label="School dashboard navigation">
              <NavLink to="/school/students" onClick={closeMenu}>Participating students</NavLink>
              <NavLink to="/school/profile" onClick={closeMenu}>School registration</NavLink>
              <NavLink to="/exam" onClick={closeMenu}>Exam portal</NavLink>
              <NavLink to="/results" onClick={closeMenu}>Results</NavLink>
              <NavLink to="/leaderboard" onClick={closeMenu}>Leaderboard</NavLink>
            </div>
          </>
        ) : (
          <>
            <NavLink to={dashboardPath} onClick={closeMenu}>Dashboard</NavLink>
            {(user.role === 'STUDENT' || user.studentId) && <><NavLink to="/exam" onClick={closeMenu}>Exam Portal</NavLink><NavLink to="/results" onClick={closeMenu}>View Results</NavLink><NavLink to="/leaderboard" onClick={closeMenu}>Leaderboard</NavLink><NavLink to="/dashboard" onClick={closeMenu}>Overview</NavLink></>}
          </>
        )}
        <button className="nav-signin" onClick={() => { closeMenu(); signOut() }}>Sign out</button>
      </> : <>
        <NavLink to="/about" onClick={closeMenu}>About Us</NavLink><NavLink to="/sponsors" onClick={closeMenu}>Sponsors</NavLink>
        <Link to="/register-school" onClick={closeMenu}>Register</Link><Link className="nav-signin" to="/sign-in" onClick={closeMenu}>Sign in <ArrowRight size={15}/></Link>
      </>}
    </nav>
  </header>
}

function Footer() {
  return <footer className="footer"><div className="footer-main"><Brand/><p>Curiosity today. Possibility tomorrow.</p><div className="footer-links"><Link to="/about">About HSSC</Link><Link to="/policy">Competition policy</Link><Link to="/sponsors">Our sponsors</Link><Link to="/register-school">Register your school</Link></div></div><div className="footer-bottom"><span>© {new Date().getFullYear()} High School STEM Championship</span><span>Built for the next generation of thinkers.</span></div></footer>
}

function HeroVideo() {
  const [videoAvailable, setVideoAvailable] = useState(false)
  useEffect(() => {
    let active = true
    fetch('/assets/hssc-hero.mp4', { method: 'HEAD' })
      .then(response => { if (active && response.ok) setVideoAvailable(true) })
      .catch(() => { if (active) setVideoAvailable(false) })
    return () => { active = false }
  }, [])
  return <div className="hero-video" aria-hidden="true">
    {videoAvailable && <video autoPlay muted loop playsInline preload="metadata" onError={() => setVideoAvailable(false)}>
      <source src="/assets/hssc-hero.mp4" type="video/mp4"/>
    </video>}
    <div className="hero-video-overlay"/>
  </div>
}

function Landing() {
  const sponsorState = useSponsors()
  return <>
    <main>
      <section className="hero hero-video-stage">
        <HeroVideo/>
        <div className="hero-copy"><div className="eyebrow"><span className="eyebrow-line"/>A brighter future starts with a question</div><h1>High School<br/><em>STEM Championship.</em></h1><p className="hero-lede">A science championship for secondary-school students to put ideas into action and explore what they can achieve.</p>
          <div className="hero-actions"><Link className="button button-dark" to="/register-school">Register your school <ArrowRight size={17}/></Link><Link className="button button-outline" to="/sign-in">Sign in</Link></div>
          <div className="hero-meta"><span><ShieldCheck size={16}/> For secondary school students</span><span><span className="meta-dot"/> Lagos, Nigeria</span></div>
        </div>
        <div className="hero-index">EST. FOR THE CURIOUS <span>01 — 06</span></div>
      </section>
      <section className="ticker"><span>THINK</span><i>✳</i><span>QUESTION</span><i>✳</i><span>DISCOVER</span><i>✳</i><span>GO FURTHER</span><i>✳</i><span>THINK</span></section>
      <section className="stem-showcase section-wrap" aria-label="HSSC STEM in action">
        <div className="section-kicker">Curiosity in action</div><div className="stem-showcase-heading"><h2>Ideas for a<br/><em>brighter tomorrow.</em></h2><p>Science, technology, engineering and mathematics open new ways to understand our world.</p></div>
        <div className="stem-showcase-grid">
          <figure><img src="/images/hssc-stem-championship.png" alt="HSSC STEM Championship celebrating science, technology, engineering and mathematics" loading="lazy" decoding="async"/><figcaption><b>High School STEM Championship</b><span>Explore · Innovate · Solve · Lead</span></figcaption></figure>
          <figure><img src="/images/hssc-stem-lab.png" alt="Students collaborating on science experiments, robotics and engineering projects" loading="lazy" decoding="async"/><figcaption><b>Learning through discovery</b><span>Building skills for a brighter future</span></figcaption></figure>
        </div>
      </section>
      <section className="logo-feature section-wrap" aria-label="Meaning of the HSSC logo">
        <div className="logo-feature-animation"><video autoPlay muted loop playsInline preload="metadata" poster="/images/hssc-logo.png" aria-label="Animated HSSC STEM Championship logo"><source src="/assets/hssc-logo-animation.mp4" type="video/mp4"/></video></div>
        <div className="logo-feature-copy"><div className="section-kicker">The HSSC mark</div><h2>Four disciplines.<br/><em>One bold future.</em></h2><p>The HSSC logo brings science, technology, engineering and mathematics together in one championship. Its moving STEM motifs celebrate curiosity, creative problem-solving and the ideas students can build when they learn together.</p></div>
      </section>
      <section className="intro section-wrap"><div className="section-kicker">A different kind of competition</div><div className="intro-grid"><h2>Not just what you know.<br/><em>What you can imagine.</em></h2><div><p>HSSC brings secondary school students together to explore the ideas shaping our world. Across six STEM disciplines, students put their knowledge to work, think boldly and discover what they are capable of.</p><Link className="text-link" to="/policy">How the championship works <ArrowRight size={16}/></Link></div></div>
        <div className="stat-strip"><div><strong>06</strong><span>STEM disciplines</span></div><div><strong>20</strong><span>Educational zones</span></div><div><strong>01</strong><span>Curious community</span></div><div><strong>∞</strong><span>Ways to make a difference</span></div></div></section>
      <section className="subjects-section"><div className="section-wrap"><div className="section-kicker">Explore the disciplines</div><div className="section-heading"><h2>Six ways to see<br/><em>the world differently.</em></h2><p>Every great discovery starts with the right question. Where will yours take you?</p></div><div className="subject-grid">{subjects.map((subject, i) => { const Icon = subject.icon; return <article className="subject-card" key={subject.name}><span className="subject-number">0{i + 1}</span><Icon size={24}/><h3>{subject.name}</h3><span className="subject-arrow"><ArrowRight size={17}/></span></article> })}</div></div></section>
      <section className="journey section-wrap"><div className="section-kicker">The championship journey</div><div className="journey-grid"><div><h2>One journey.<br/><em>Room to grow.</em></h2><p>The championship begins with an online objective examination, followed by further stages. Detailed arrangements for later stages will be shared when confirmed.</p><Link className="text-link" to="/policy">Read competition policy <ArrowRight size={16}/></Link></div><div className="stage-list"><article><span>01</span><div><h3>Online objective examination</h3><p>Put your STEM knowledge to the test in a timed, online competition.</p></div><ChevronRight/></article><article className="stage-future"><span>02</span><div><h3>Physical / proving stage</h3><p>Further information will be shared by the organizers.</p></div><span className="coming-soon">DETAILS TO FOLLOW</span></article><article className="stage-future"><span>03</span><div><h3>Practical STEM stage</h3><p>Further information will be shared by the organizers.</p></div><span className="coming-soon">DETAILS TO FOLLOW</span></article><article className="stage-future"><span>04</span><div><h3>Final stage</h3><p>Further information will be shared by the organizers.</p></div><span className="coming-soon">DETAILS TO FOLLOW</span></article></div></div></section>
      <section className="sponsor-band"><div className="section-wrap sponsor-content"><div><div className="section-kicker">Made possible together</div><h2>Backed by people<br/>who believe in <em>possibility.</em></h2><Link className="text-link" to="/sponsors">Meet our sponsors <ArrowRight size={16}/></Link></div><div className="sponsor-logos">{sponsorState.error ? <span className="sponsor-error">Sponsor information is temporarily unavailable.</span> : sponsorState.data?.map(s => { const logo = sponsorLogo(s); return <div className="sponsor-logo" key={s.id}>{logo ? <img src={logo} alt={`${s.name} logo`} loading="lazy" decoding="async"/> : <span>{s.name.slice(0,1).toUpperCase()}</span>}<b>{s.name}</b></div> })}</div></div></section>
      <section className="intro section-wrap about-preview"><div className="section-kicker">Behind the championship</div><div className="intro-grid"><h2>Built around<br/><em>possibility.</em></h2><div><p>HSSC is connected with Shamaden Imole Charity Foundation, Lagos State Interpublic School Championship, and StackThread, the technical/project team behind the broader setup.</p><Link className="text-link" to="/about">Meet the people and partners <ArrowRight size={16}/></Link></div></div></section>
      <section className="cta section-wrap"><div className="cta-inner"><div className="cta-orbit">✳</div><div className="section-kicker">Your next chapter starts here</div><h2>Have a school full<br/>of <em>bright ideas?</em></h2><p>Join the HSSC community and give your students a new way to shine.</p><Link className="button button-light" to="/register-school">Register your school <ArrowRight size={17}/></Link></div></section>
    </main>
  </>
}

function AboutPage() {
  return <main className="content-page about-page">
    <div className="page-hero"><div className="section-kicker">The people and organizations behind HSSC</div><h1>About <em>us.</em></h1><p>HSSC brings together schools, partners, and a technical team around a STEM competition for secondary-school students.</p></div>
    <section className="about-grid" aria-label="HSSC partners and team">
      <article className="about-card about-card-foundation"><div className="about-card-logo about-card-logo-foundation"><img src="/images/shamaden-imole-foundation.jfif" alt="Shamaden Imole logo" loading="lazy" decoding="async"/></div><span className="section-kicker">FOUNDATION</span><h2>Shamaden Imole Charity Foundation</h2><p>Organization associated with the championship.</p></article>
      <article className="about-card"><div className="about-card-logo about-card-logo-lagos"><img src="/images/lagos-interpublic-championship.png" alt="Lagos State Inter-Public Schools Championship logo" loading="lazy" decoding="async"/></div><span className="section-kicker">SCHOOL CHAMPIONSHIP</span><h2>Lagos State Inter-Public Schools Championship</h2><p>Partner associated with the HSSC competition.</p></article>
      <article className="about-card about-card-stackthread"><div className="about-card-logo about-card-logo-stackthread"><img src="/images/stackthread-labs-technology.png" alt="StackThread Labs & Technology logo" loading="lazy" decoding="async"/></div><span className="section-kicker">TECHNICAL / PROJECT TEAM</span><h2>StackThread Labs &amp; Technology</h2><p>Involved in the development and broader setup of the project, including the online platform and the physical/proving stages leading toward the practical competition.</p><div className="about-team-member"><span>TEAM MEMBER</span><b>Godwin Dickson</b><b>Olaniran Feranmi</b><b>Adepoju Ridwan</b><b>Gbadamosi Adekunle</b></div></article>
    </section>
  </main>
}

function SponsorsPage() {
  const { data, error } = useSponsors()
  return <main className="content-page"><div className="page-hero"><div className="section-kicker">Our partners</div><h1>Better, <em>together.</em></h1><p>Meet the supporters helping young minds explore what's possible.</p></div>{error ? <PageState title="Sponsor information unavailable" message={error}/> : !data ? <div className="page-message"><span className="loader"/><p>Loading sponsor information…</p></div> : data.length ? <div className="sponsor-page-grid">{data.map(s => { const logo = sponsorLogo(s); return <article className="sponsor-large" key={s.id}>{logo ? <div className="sponsor-large-logo"><img src={logo} alt={`${s.name} logo`} loading="lazy" decoding="async"/></div> : <span className="sponsor-initial">{s.name.slice(0,1).toUpperCase()}</span>}<span className="section-kicker">HSSC PARTNER</span><h2>{s.name}</h2>{s.organization && <p>{s.organization}</p>}{s.description && <p>{s.description}</p>}{s.websiteUrl && <a href={s.websiteUrl} target="_blank" rel="noreferrer">Visit partner website <ArrowRight size={15}/></a>}</article> })}</div> : <div className="page-message"><h2>No sponsors have been added yet.</h2></div>}</main>
}

function PolicyPage() {
  const [sections, setSections] = useState<PolicySection[] | null>(null)
  const [error, setError] = useState('')
  useEffect(() => { api<PolicySection[]>('/policy').then(setSections).catch(e => setError(e.message)) }, [])
  return <main className="content-page"><div className="page-hero"><div className="section-kicker">A fair field for every curious mind</div><h1>Rules of the <em>road.</em></h1><p>Clear guidelines help everyone focus on what matters: learning, thinking and competing fairly.</p></div>{error ? <PageState title="Competition policy unavailable" message={error}/> : !sections ? <div className="page-message"><span className="loader"/><p>Loading the current competition policy…</p></div> : <div className="policy-layout"><aside><span>IN THIS POLICY</span>{sections.map(section => <a key={section.id} href={`#policy-${section.id}`}>{section.section} &nbsp; {section.title}</a>)}</aside><div className="policy-sections">{sections.map(section => <article id={`policy-${section.id}`} key={section.id}><span>{section.section}</span><div><h2>{section.title}</h2><p>{section.content}</p></div></article>)}<p className="policy-note"><CircleHelp size={18}/> Final competition instructions and updated policy are communicated through official HSSC channels.</p></div></div>}</main>
}

function SignInPage() {
  const { signIn, user } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : user.role === 'SCHOOL' ? '/school/dashboard' : '/dashboard'} replace/>
  async function submit(e: FormEvent) {
    e.preventDefault(); setError(''); setBusy(true)
    try { const signedInUser = await signIn(email, password); navigate(signedInUser.role === 'ADMIN' ? '/admin' : signedInUser.role === 'SCHOOL' ? '/school/dashboard' : '/dashboard') }
    catch (err) { setError(err instanceof Error ? err.message : 'Unable to sign in.') }
    finally { setBusy(false) }
  }
  return <main className="auth-page"><div className="auth-aside"><div className="section-kicker">Welcome back</div><h1>Big ideas<br/>start with <em>you.</em></h1><p>Your next step in the HSSC journey is waiting.</p><div className="auth-aside-stamp"><Atom/><span>STAY<br/>CURIOUS</span></div></div><form className="form-card" onSubmit={submit}><div className="section-kicker">HSSC participant portal</div><h2>Sign in</h2><p>Use your school, student, or administrator account to continue.</p>{error && <div className="form-error" role="alert">{error}</div>}<label>Email address<input type="email" value={email} onChange={e => setEmail(e.target.value)} autoComplete="username" required/></label><label>Password<input type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password" required/></label><button className="button button-dark form-submit" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'} <ArrowRight size={16}/></button><div className="form-foot">Registering a school? <Link to="/register-school">Get started <ArrowRight size={14}/></Link></div></form></main>
}

function AdminSignInPage() {
  const { signIn, user } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/sign-in'} replace/>
  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      const account = await signIn(email, password)
      if (account.role !== 'ADMIN') {
        setError('This sign-in page is for authorized administrators only.')
        return
      }
      navigate('/admin', { replace: true })
    } catch (e) { setError(e instanceof Error ? e.message : 'Administrator sign-in failed.') }
    finally { setBusy(false) }
  }
  return <main className="admin-login-page"><section className="admin-login-brand"><Brand admin/><div><span className="section-kicker">HSSC · RESTRICTED ACCESS</span><h1>Competition<br/><em>administration.</em></h1><p>Authorized competition staff only. Administrative actions are protected and recorded.</p></div></section><form className="admin-login-card" onSubmit={submit}><div className="section-kicker">SECURE ADMINISTRATOR PORTAL</div><h2>Administrator sign in</h2><p>Use the administrator account configured for this HSSC system.</p>{error && <div className="form-error" role="alert">{error}</div>}<label>Email address<input required type="email" value={email} onChange={e => setEmail(e.target.value)} autoComplete="username"/></label><label>Password<input required type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password"/></label><button className="button button-dark form-submit" disabled={busy}>{busy ? 'Verifying access…' : 'Sign in securely'} <ArrowRight size={16}/></button><Link className="admin-login-back" to="/">Return to public website</Link></form></main>
}

function SchoolRegistrationPage() {
  const [districts, setDistricts] = useState<District[]>([])
  const [districtId, setDistrictId] = useState('')
  const [fields, setFields] = useState({ zoneId: '', schoolName: '', principalName: '', principalEmail: '', principalPhone: '', teacherInChargeName: '', teacherInChargePhone: '', address: '', password: '', confirmPassword: '', principalSignature: null as File | null })
  const [participants, setParticipants] = useState<{ key: string; fullName: string; gender: string; age: string; guardianName: string; guardianPhone: string; homeAddress: string; photo: File | null; uniformConfirmed: boolean }[]>([
    { key: crypto.randomUUID(), fullName: '', gender: '', age: '', guardianName: '', guardianPhone: '', homeAddress: '', photo: null, uniformConfirmed: false },
  ])
  const [error, setError] = useState('')
  const [success, setSuccess] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  useEffect(() => { api<District[]>('/districts').then(setDistricts).catch(e => setError(e.message)) }, [])
  const zones = districts.find(d => String(d.id) === districtId)?.zones || []
  const change = (key: keyof typeof fields) => (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setFields({ ...fields, [key]: e.target.value })
  async function submit(e: FormEvent) {
    e.preventDefault(); setError('')
    if (fields.password !== fields.confirmPassword) { setError('The passwords do not match.'); return }
    if (!participants.length || participants.some(student => !student.fullName.trim() || !student.gender || !student.age || !student.guardianName.trim() || !student.guardianPhone.trim() || !student.homeAddress.trim() || !student.photo || !student.uniformConfirmed)) {
      setError('Complete each participating student’s details, parent or guardian name and phone number, home address, uniform photograph, and photo confirmation before submitting.')
      return
    }
    setBusy(true)
    try {
      if (!fields.principalSignature) {
        setError('Upload the principal signature image before submitting this registration.')
        return
      }
      if (participants.length > 2) {
        setError('Only two students can be registered per school in this round.')
        return
      }
      const data = new FormData()
      data.append('registration', new Blob([JSON.stringify({
        zoneId: fields.zoneId, name: fields.schoolName, email: fields.principalEmail,
        principalName: fields.principalName, principalEmail: fields.principalEmail,
        principalPhone: fields.principalPhone, teacherInChargeName: fields.teacherInChargeName,
        teacherInChargePhone: fields.teacherInChargePhone, address: fields.address, password: fields.password,
        confirmPassword: fields.confirmPassword,
      })], { type: 'application/json' }))
      data.append('document', fields.principalSignature)
      data.append('participants', new Blob([JSON.stringify(participants.map(({ fullName, gender, age, guardianName, guardianPhone, homeAddress }) => ({
        fullName: fullName.trim(), gender, age: Number(age), guardianName: guardianName.trim(), guardianPhone: guardianPhone.trim(), homeAddress: homeAddress.trim(),
      })))], { type: 'application/json' }))
      participants.forEach(student => { if (student.photo) data.append('studentPhotos', student.photo) })
      const result = await api<{ status: string; message: string }>('/schools/register/with-students', { method: 'POST', body: data })
      if (result.status !== 'PENDING') {
        setError(result.message || 'Registration could not be completed. Please check the details and try again.')
        return
      }
      setSuccess(result.message || 'Registration successful. Please hold on. Your account will be reviewed and approved by the administrator before you can sign in.')
    } catch (err) { setError(err instanceof Error ? err.message : 'Registration could not be completed.') }
    finally { setBusy(false) }
  }
  function updateParticipant(key: string, update: Partial<typeof participants[number]>) {
    setParticipants(current => current.map(student => student.key === key ? { ...student, ...update } : student))
  }
  function addParticipant() {
    setParticipants(current => [...current, { key: crypto.randomUUID(), fullName: '', gender: '', age: '', guardianName: '', guardianPhone: '', homeAddress: '', photo: null, uniformConfirmed: false }])
  }
  function removeParticipant(key: string) {
    setParticipants(current => current.length > 1 ? current.filter(student => student.key !== key) : current)
  }
  return <main className="form-page registration-page">
    <div className="form-page-heading"><div className="section-kicker">Join the championship</div><h1>Register your <em>school.</em></h1><p>Register your school and the students who will represent it in HSSC.</p><div className="form-step"><span>01</span> SCHOOL DETAILS <i/> <span>02</span> PARTICIPATING STUDENTS <i/> <span>03</span> ACCOUNT SETUP</div><div className="registration-assurance"><ShieldCheck size={16}/> Add a current parent or guardian phone number for each student.</div></div>
    {success ? <div className="success-card" role="status"><span className="success-check"><Check/></span><div className="section-kicker">Registration received</div><h2>Registration successful.</h2><p>{success}</p><p>Your participating student records have been securely linked to this school and are available to the HSSC administrator during review.</p></div> :
      <form className="registration-form" onSubmit={submit}>
        {error && <div className="form-error" role="alert">{error}</div>}
        <div className="form-section-title"><span>01</span><div><h2>School location</h2><p>Choose your district first to see its zones.</p></div></div>
        <label>Educational district<select required value={districtId} onChange={e => { setDistrictId(e.target.value); setFields({ ...fields, zoneId: '' }) }}><option value="">Select a district</option>{districts.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}</select></label>
        <label>Zone<select required disabled={!districtId} value={fields.zoneId} onChange={change('zoneId')}><option value="">Select a zone</option>{zones.map(z => <option key={z.id} value={z.id}>{z.name}</option>)}</select></label>
        <div className="form-section-title"><span>02</span><div><h2>School details</h2><p>Use the official details for your school.</p></div></div>
        <label>School name<input required value={fields.schoolName} onChange={change('schoolName')} autoComplete="organization"/></label>
        <label>School address<input required value={fields.address} onChange={change('address')} autoComplete="street-address"/></label>
        <div className="form-two"><label>Principal's full name<input required value={fields.principalName} onChange={change('principalName')}/></label><label>Principal's phone<input required type="tel" value={fields.principalPhone} onChange={change('principalPhone')} autoComplete="tel"/></label></div>
        <label>Principal's email<input required type="email" value={fields.principalEmail} onChange={change('principalEmail')} autoComplete="email"/></label>
        <div className="form-two"><label>Teacher in charge full name<input required value={fields.teacherInChargeName} onChange={change('teacherInChargeName')}/></label><label>Teacher in charge phone<input required type="tel" value={fields.teacherInChargePhone} onChange={change('teacherInChargePhone')} autoComplete="tel"/></label></div>
        <label>Principal signature image<input required type="file" accept="image/jpeg,image/png" onChange={e => setFields({ ...fields, principalSignature: e.target.files?.[0] || null })}/><small>Upload a clear JPG or PNG image of the principal's signature for official school registration capture.</small></label>
        <div className="school-participant-heading"><div className="form-section-title"><span>03</span><div><h2>Participating students</h2><p>Only two students may register per school for this round. Add the two students who will represent your school.</p></div></div><button type="button" className="button button-outline" onClick={addParticipant} disabled={participants.length >= 2}>Add student <ArrowRight size={15}/></button></div>
        <div className="participant-draft-list">{participants.map((student, index) => <fieldset className="participant-draft" key={student.key}>
          <legend>Student {String(index + 1).padStart(2, '0')}</legend>
          {participants.length > 1 && <button type="button" className="participant-remove" onClick={() => removeParticipant(student.key)} aria-label={`Remove student ${index + 1}`}><X size={16}/></button>}
          <div className="participant-draft-grid"><StudentPhotoPreview file={student.photo}/>
            <div className="participant-draft-fields">
              <label>Student name<input required maxLength={200} value={student.fullName} onChange={e => updateParticipant(student.key, { fullName: e.target.value })} autoComplete="name"/></label>
              <label>Parent or guardian full name<input required maxLength={200} value={student.guardianName} onChange={e => updateParticipant(student.key, { guardianName: e.target.value })} autoComplete="name"/></label>
              <label>Parent or guardian phone number<input required type="tel" maxLength={40}  value={student.guardianPhone} onChange={e => { validatePhoneNumberInput(e); updateParticipant(student.key, { guardianPhone: e.target.value }) }} autoComplete="tel" placeholder="+234 800 000 0000"/><small>Use a number the school can reach about the student.</small></label>
              <label>Student home address<textarea required maxLength={500} rows={2} value={student.homeAddress} onChange={e => updateParticipant(student.key, { homeAddress: e.target.value })} autoComplete="street-address"/></label>
              <div className="form-two"><label>Age<input required type="number" min="1" max="120" value={student.age} onChange={e => updateParticipant(student.key, { age: e.target.value })}/></label><label>Gender<select required value={student.gender} onChange={e => updateParticipant(student.key, { gender: e.target.value })}><option value="">Select gender</option><option>Female</option><option>Male</option><option>Prefer not to say</option></select></label></div>
              <label>Student photograph in school uniform<input required type="file" accept="image/jpeg,image/png" onChange={e => updateParticipant(student.key, { photo: e.target.files?.[0] || null })}/><small>Upload a clear JPEG or PNG photograph of this student wearing their school uniform (maximum 5 MB per photo).</small></label>
              <label className="participant-uniform-confirmation"><input required type="checkbox" checked={student.uniformConfirmed} onChange={e => updateParticipant(student.key, { uniformConfirmed: e.target.checked })}/> I confirm this photograph shows the student wearing their school uniform.</label>
            </div>
          </div>
          
        </fieldset>)}</div>
        <div className="form-section-title"><span>04</span><div><h2>Create school access</h2><p>School sign-in is available after administrator approval.</p></div></div>
        <div className="form-two"><label>Password<input required type="password" minLength={12} value={fields.password} onChange={change('password')} autoComplete="new-password"/></label><label>Confirm password<input required type="password" minLength={12} value={fields.confirmPassword} onChange={change('confirmPassword')} autoComplete="new-password"/></label></div>
        <p className="form-terms"><strong>Important:</strong> only two students can be registered for each school in this registration round.</p>
        <button className="button button-dark form-submit" disabled={busy}>{busy ? 'Submitting school and students…' : 'Submit school registration'} <ArrowRight size={16}/></button>
        <p className="form-terms"><LockKeyhole size={14}/> The school account and student roster are securely handled and visible only to authorized competition administrators.</p>
      </form>}
  </main>
}

function StudentPhotoPreview({ file }: { file: File | null }) {
  const [preview, setPreview] = useState('')
  useEffect(() => {
    if (!file) { setPreview(''); return }
    const url = URL.createObjectURL(file)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [file])
  return <div className="participant-photo-preview">{preview ? <img src={preview} alt="Selected student photograph preview"/> : <div><Microscope size={25}/><span>PHOTO PREVIEW</span></div>}</div>
}

function validatePhoneNumberInput(event: ChangeEvent<HTMLInputElement>) {
  const value = event.currentTarget.value
  const valid = !value || /^(?=(?:\D*\d){7,15}\D*$)[+()0-9 .-]+$/.test(value)
  event.currentTarget.setCustomValidity(valid ? '' : 'Enter a phone number with 7 to 15 digits.')
}

function RegisterStudentPage() {
  const [districts, setDistricts] = useState<District[]>([])
  const [schools, setSchools] = useState<{id:string;name:string;zoneId:string;districtId:string}[]>([])
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)
  const [busy, setBusy] = useState(false)
  const [schoolsLoaded, setSchoolsLoaded] = useState(false)
  const [file, setFile] = useState<File | null>(null)
  const [form, setForm] = useState({ schoolId: '', studentNumber: '', fullName: '', age: '', gender: '', parentGuardianName: '', guardianPhone: '', homeAddress: '', email: '', phone: '', password: '', confirmPassword: '' })
  useEffect(() => {
    api<District[]>('/districts').then(setDistricts).catch(e => setError(e.message))
    api<typeof schools>('/schools').then(setSchools).catch(e => setError(e.message)).finally(() => setSchoolsLoaded(true))
  }, [])
  const selectedSchool = schools.find(s => String(s.id) === form.schoolId)
  const location = selectedSchool ? districts.flatMap(d => d.zones.map(z => ({ ...z, district: d.name }))).find(z => z.id === selectedSchool.zoneId) : null
  const change = (key: keyof typeof form) => (e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => setForm({ ...form, [key]: e.target.value })
  async function submit(e: FormEvent) {
    e.preventDefault(); setError('')
    if (form.password !== form.confirmPassword) { setError('The passwords do not match.'); return }
    if (!file) { setError('Please upload a student photograph without school uniform.'); return }
    const data = new FormData()
    Object.entries(form).forEach(([key, value]) => data.append(key, value))
    data.append('photo', file)
    setBusy(true)
    try { await api('/students/register', { method: 'POST', body: data }); setDone(true) }
    catch (err) { setError(err instanceof Error ? err.message : 'Registration could not be completed.') }
    finally { setBusy(false) }
  }
  return <main className="form-page registration-page"><div className="form-page-heading"><div className="section-kicker">Student participation</div><h1>Bring your <em>curiosity.</em></h1><p>Student accounts are linked to a participating school.</p><div className="registration-assurance"><ShieldCheck size={16}/> Guardian contact details help the school reach the right family.</div></div>{done ? <div className="success-card"><span className="success-check"><Check/></span><h2>Registration received.</h2><p>Your student account is awaiting school review. The school will activate access after verifying your registration.</p></div> : <form className="registration-form" onSubmit={submit}>{error && <div className="form-error" role="alert">{error}</div>}<label>Participating school<select required value={form.schoolId} onChange={change('schoolId')}><option value="">Select your school</option>{schools.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label>{schoolsLoaded && schools.length === 0 && <div className="empty-inline">{error ? 'Participating schools are unavailable.' : 'No participating schools are available for student registration yet.'}</div>}{!schoolsLoaded && <div className="empty-inline">Loading participating schools…</div>}{selectedSchool && <div className="location-summary"><span>{selectedSchool.name}</span><span>{location?.district} · {location?.name}</span></div>}<label>Student number<input required value={form.studentNumber} onChange={change('studentNumber')} autoComplete="off"/></label><label>Student full name<input required value={form.fullName} onChange={change('fullName')} autoComplete="name"/></label><div className="form-two"><label>Age<input required type="number" min="1" max="120" value={form.age} onChange={change('age')}/></label><label>Gender<select required value={form.gender} onChange={change('gender')}><option value="">Select</option><option>Female</option><option>Male</option><option>Prefer not to say</option></select></label></div><div className="form-section-title"><span>✳</span><div><h2>Parent or guardian</h2><p>Please provide a phone number the school can use to contact the student's family.</p></div></div><label>Parent or guardian full name<input required maxLength={200} value={form.parentGuardianName} onChange={change('parentGuardianName')} autoComplete="name"/></label><label>Parent or guardian phone number<input required type="tel" maxLength={40}  value={form.guardianPhone} onChange={e => { validatePhoneNumberInput(e); change('guardianPhone')(e) }} autoComplete="tel" placeholder="+234 800 000 0000"/><small>Enter 7–15 digits; country code is recommended.</small></label><label>Student home address<textarea required maxLength={500} rows={2} value={form.homeAddress} onChange={change('homeAddress')} autoComplete="street-address"/></label><div className="form-section-title"><span>✳</span><div><h2>Student account</h2><p>Use an email address and password the student can access.</p></div></div><label>Email address<input required type="email" value={form.email} onChange={change('email')}/></label><label>Student contact phone (optional)<input type="tel" value={form.phone} onChange={e => { validatePhoneNumberInput(e); change('phone')(e) }} autoComplete="tel" /></label><label>Student photograph<input required type="file" accept="image/jpeg,image/png" onChange={e => setFile(e.target.files?.[0] || null)}/><small>Upload a clear photograph of the student without school uniform (JPEG or PNG).</small></label><label className="photo-confirm"><input required type="checkbox"/> I confirm this is a photograph of the student without school uniform.</label><div className="form-two"><label>Password<input required type="password" minLength={12} value={form.password} onChange={change('password')}/></label><label>Confirm password<input required type="password" minLength={12} value={form.confirmPassword} onChange={change('confirmPassword')}/></label></div><button className="button button-dark form-submit" disabled={busy || !schoolsLoaded || !schools.length}>{busy ? 'Creating account…' : 'Create student account'} <ArrowRight size={16}/></button><p className="form-terms"><LockKeyhole size={14}/> Your registration is reviewed by your school; family contact details are handled privately.</p></form>}</main>
}

function ProtectedRoute({ roles, children }: { roles: string[]; children: ReactNode }) {
  const location = useLocation()
  const { token, user, loading } = useAuth()
  if (loading) return <div className="page-message"><span className="loader"/><p>Loading your secure HSSC account…</p></div>
  if (!token || !user) return <Navigate to={roles.includes('ADMIN') ? '/admin/login' : '/sign-in'} state={{ from: location }} replace/>
  const studentAccess = user.role === 'STUDENT' || Boolean(user.studentId)
  if (!roles.includes(user.role) && !(roles.includes('STUDENT') && studentAccess)) return <Navigate to="/" replace/>
  return <>{children}</>
}

function Dashboard() {
  const { token, user } = useAuth()
  const [data, setData] = useState<Record<string, unknown> | null>(null)
  const [error, setError] = useState('')
  useEffect(() => { api<Record<string, unknown>>('/student/dashboard', {}, token).then(setData).catch(e => setError(e.message)) }, [token])
  if (error) return <PageState title="We couldn't load your dashboard" message={error} retry={() => { setError(''); api<Record<string, unknown>>('/student/dashboard', {}, token).then(setData).catch(e => setError(e.message)) }}/>
  if (!data) return <div className="page-message"><span className="loader"/><p>Preparing your dashboard…</p></div>
  const status = String(data.portalStatus || 'LOCKED')
  const isOpen = status === 'OPEN'
  return <main className="dashboard-page"><div className="dashboard-top"><div><div className="section-kicker">Your HSSC space</div><h1>Hello, <em>{String(data.studentName || user?.name || 'thinker')}.</em></h1><p>Your journey is yours to shape. Here's where things stand.</p></div><span className={`status-pill ${isOpen ? 'status-open' : ''}`}><i/>{isOpen ? 'DISTRICT PORTAL OPEN' : `PORTAL ${status}`}</span></div><div className="dashboard-grid"><section className="dash-main"><div className="profile-banner"><div><span className="section-kicker">YOUR PARTICIPATION</span><h2>{String(data.schoolName || 'Your school')}</h2><p>{String(data.zoneName || 'Zone')} <span>·</span> {String(data.districtName || 'District')}</p></div><span className="profile-glyph">✳</span></div><div className={`exam-access ${isOpen ? 'exam-available' : ''}`}>{isOpen ? <><div className="access-icon"><BookOpen/></div><div><span className="section-kicker">ONLINE EXAMINATION</span><h2>Your exam is ready.</h2><p>Your district's examination portal is open. Find a quiet place and make sure your connection is steady.</p></div><Link className="button button-dark" to="/exam">Go to exam <ArrowRight size={16}/></Link></> : <><div className="access-icon locked"><LockKeyhole/></div><div><span className="section-kicker">ONLINE EXAMINATION</span><h2>Good things take preparation.</h2><p>The examination area unlocks when your district portal opens. Check back here for updates.</p><span className="lock-explanation"><LockKeyhole size={14}/> YOUR DISTRICT PORTAL IS {status}</span></div></>}</div><div className="dash-cards"><article><span className="section-kicker">EXAM STATUS</span><strong>{String(data.examStatus || 'NOT STARTED')}</strong><p>Your examination progress</p></article><article><span className="section-kicker">RESULT STATUS</span><strong>{String(data.resultStatus || 'NOT RELEASED')}</strong><p>Results are reviewed before release</p></article><article><span className="section-kicker">QUALIFICATION</span><strong>{String(data.qualificationStatus || 'PENDING')}</strong><p>Updates will appear here</p></article></div></section><aside className="dash-side"><div className="side-card"><span className="section-kicker">YOUR COMPETITION</span><h3>Six subjects.<br/>So many ways in.</h3><div className="mini-subjects">{subjects.map(s => <span key={s.name}>{s.name}</span>)}</div></div><div className="side-card announcement"><span className="section-kicker">ANNOUNCEMENTS</span><h3>Nothing new, for now.</h3><p>Important competition updates will show here.</p></div></aside></div><StudentBottomNav/></main>
}

type SchoolDashboardData = {
  id: string; name: string; email: string; status: string; address: string; registrationNumber: string | null
  principalName: string | null; principalEmail: string | null; principalPhone: string | null
  teacherInChargeName: string | null; teacherInChargePhone: string | null; principalSignatureDocumentId: string | null
  zoneName: string; districtName: string; participants: ParticipantReview[]
}

function SchoolDashboardPage({ focus = 'overview' }: { focus?: 'overview' | 'participants' | 'profile' }) {
  const { token } = useAuth()
  const [school, setSchool] = useState<SchoolDashboardData | null>(null)
  const [error, setError] = useState('')
  useEffect(() => {
    api<SchoolDashboardData>('/school/dashboard', {}, token).then(setSchool).catch(e => setError(e.message))
  }, [token])
  if (error) return <PageState title="School dashboard unavailable" message={error} retry={() => { setError(''); api<SchoolDashboardData>('/school/dashboard', {}, token).then(setSchool).catch(e => setError(e.message)) }}/>
  if (!school) return <div className="page-message"><span className="loader"/><p>Loading your school portal…</p></div>
  return <main className="school-dashboard-page">
    <aside className="school-dashboard-rail">
      <span className="section-kicker">SCHOOL PORTAL</span>
      <h2>{school.name}</h2>
      <nav aria-label="School dashboard">
        <Link className={focus === 'overview' ? 'active' : ''} to="/school/dashboard"><Monitor size={16}/> Overview</Link>
        <Link className={focus === 'participants' ? 'active' : ''} to="/school/students"><Microscope size={16}/> Participating students</Link>
        <Link className={focus === 'profile' ? 'active' : ''} to="/school/profile"><ShieldCheck size={16}/> School registration</Link>
        <Link className="desktop-dashboard-link" to="/exam"><BookOpen size={16}/> Exam portal</Link>
        <Link className="desktop-dashboard-link" to="/results"><Check size={16}/> Results</Link>
        <Link className="desktop-dashboard-link" to="/leaderboard"><Trophy size={16}/> Leaderboard</Link>
      </nav>
      <div className="school-dashboard-rail-note"><LockKeyhole size={15}/><span>Only authorized school representatives can access this information.</span></div>
    </aside>
    <section className="school-dashboard-main">
      <div className="school-dashboard-heading"><div><span className="section-kicker">HSSC · SCHOOL CONSOLE</span><h1>{focus === 'participants' ? <>Participating <em>students.</em></> : focus === 'profile' ? <>School <em>registration.</em></> : <>Your school, <em>in focus.</em></>}</h1><p>Review your school information, monitor your competition roster, and access the exam, results, and leaderboard from one place.</p></div><span className={`status-pill ${school.status === 'ACTIVE' ? 'status-open' : ''}`}>{school.status}</span></div>
      <div className="school-dashboard-identity"><div className="school-identity-mark"><Atom size={27}/></div><div><span className="section-kicker">REGISTERED SCHOOL</span><h2>{school.name}</h2><p>{school.zoneName} <span>·</span> {school.districtName}</p></div><div className="school-identity-status"><span className="section-kicker">ACCOUNT STATUS</span><b>{school.status}</b></div></div>
      <section className="school-dashboard-panel" aria-labelledby="school-competition-brief-heading">
        <div className="school-dashboard-panel-heading"><div><span className="section-kicker">COMPETITION TRACKER</span><h2 id="school-competition-brief-heading">Competition access</h2></div><span>01 / PORTAL</span></div>
        <div className="school-action-grid">
          <Link className="school-action-card" to="/exam"><div className="school-action-icon"><BookOpen size={18}/></div><div><span className="section-kicker">EXAM PORTAL</span><h3>Assessment access</h3><p>Open the current exam and continue from the school dashboard.</p></div><ChevronRight size={16}/></Link>
          <Link className="school-action-card" to="/results"><div className="school-action-icon"><Check size={18}/></div><div><span className="section-kicker">RESULTS</span><h3>Performance review</h3><p>Check released results and review completed exam outcomes.</p></div><ChevronRight size={16}/></Link>
          <Link className="school-action-card" to="/leaderboard"><div className="school-action-icon"><Trophy size={18}/></div><div><span className="section-kicker">LEADERBOARD</span><h3>School standings</h3><p>See published rankings and school performance across the competition.</p></div><ChevronRight size={16}/></Link>
        </div>
      </section>
      <section className={`school-dashboard-panel ${focus === 'profile' ? 'school-panel-focused' : ''}`} aria-labelledby="school-information-heading">
        <div className="school-dashboard-panel-heading"><div><span className="section-kicker">REGISTRATION RECORD</span><h2 id="school-information-heading">School information</h2></div><span>02 / PROFILE</span></div>
        <dl className="school-dashboard-details"><div><dt>School account</dt><dd>{school.email}</dd></div><div><dt>School address</dt><dd>{school.address}</dd></div><div><dt>Principal</dt><dd>{school.principalName || 'Not provided'}</dd></div><div><dt>Principal email</dt><dd>{school.principalEmail || 'Not provided'}</dd></div><div><dt>Principal phone</dt><dd>{school.principalPhone || 'Not provided'}</dd></div><div><dt>Teacher in charge</dt><dd>{school.teacherInChargeName || 'Not provided'}</dd></div><div><dt>Teacher in charge phone</dt><dd>{school.teacherInChargePhone || 'Not provided'}</dd></div><div><dt>Principal signature</dt><dd>{school.principalSignatureDocumentId ? <PrincipalSignature documentId={school.principalSignatureDocumentId} token={token || ''}/> : 'Not provided'}</dd></div><div><dt>Registration number</dt><dd>{school.registrationNumber || 'Not provided'}</dd></div></dl>
      </section>
      <section id="participants" className={`school-dashboard-panel ${focus === 'participants' ? 'school-panel-focused' : ''}`} aria-labelledby="school-participants-heading">
        <div className="school-dashboard-panel-heading"><div><span className="section-kicker">COMPETITION ROSTER · {school.participants.length} STUDENTS</span><h2 id="school-participants-heading">Participating students</h2></div><span>03 / ROSTER</span></div>
        {school.participants.length ? <div className="school-participant-list">{school.participants.map(participant => <article className="school-participant-row" key={participant.id}><AdminParticipantPhoto documentId={participant.photoDocumentId} token={token || ''}/><div><span className="section-kicker">HSSC PARTICIPANT</span><h3>{participant.fullName}</h3><p>{participant.gender} <span>·</span> Age {participant.age}</p><small>Guardian: {participant.guardianName} · {participant.guardianPhone}</small></div><span className="school-roster-number">ROSTERED</span></article>)}</div> : <div className="empty-inline">No participating students have been submitted with this school registration.</div>}
      </section>
    </section>
  </main>
}

function PrincipalSignature({ documentId, token }: { documentId: string; token: string }) {
  const [url, setUrl] = useState('')
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    let objectUrl = ''
    downloadDocument(documentId, token).then(blob => {
      if (!blob.type.startsWith('image/')) throw new Error('The stored signature is not an image.')
      if (!active) return
      objectUrl = URL.createObjectURL(blob)
      setUrl(objectUrl)
    }).catch(e => { if (active) setError(e instanceof Error ? e.message : 'Signature unavailable.') })
    return () => { active = false; if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }, [documentId, token])
  if (error) return <span title={error}>Signature unavailable</span>
  return url ? <a href={url} target="_blank" rel="noreferrer">View signature</a> : <span>Loading signature…</span>
}

function PageState({ title, message, retry }: { title: string; message: string; retry?: () => void }) {
  return <div className="page-message"><span className="message-icon"><CircleHelp/></span><h2>{title}</h2><p>{message}</p>{retry && <button className="button button-outline" onClick={retry}>Try again</button>}</div>
}

type StudentResult = {
  candidateName: string
  candidateId: string | null
  examTitle: string
  status: string
  score: number | null
  possiblePoints: number | null
  submittedAt: string | null
  released: boolean
  correctAnswers?: number | null
  incorrectAnswers?: number | null
  marksPerCorrectAnswer?: number | null
  attemptedQuestions?: number | null
  unansweredQuestions?: number | null
  percentage?: number | null
  startedAt?: string | null
  timeSpentSeconds?: number | null
  submissionType?: string | null
}

function StudentBottomNav() {
  const { user } = useAuth()
  const overviewPath = user?.role === 'SCHOOL' ? '/school/dashboard' : '/dashboard'
  return <nav className="student-bottom-nav" aria-label="Student navigation">
    <NavLink to="/exam">Exam Portal</NavLink><NavLink to="/results">View Results</NavLink><NavLink to="/leaderboard">Leaderboard</NavLink><NavLink to={overviewPath}>Overview</NavLink>
  </nav>
}

function ResultsPage() {
  const { token, user } = useAuth()
  const hasStudentAccess = user?.role === 'SCHOOL' || user?.role === 'STUDENT' || Boolean(user?.studentId)
  const [results, setResults] = useState<StudentResult[] | null>(null)
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    if (!hasStudentAccess) { setLoaded(true); return }
    let active = true
    const refresh = () => api<StudentResult[]>('/results/me', {}, token).then(rows => { if (active) { setResults(rows); setError('') } })
      .catch(e => { if (active && e.status !== 404 && e.status !== 403) setError(e.message) })
      .finally(() => { if (active) setLoaded(true) })
    void refresh()
    const interval = window.setInterval(refresh, 15000)
    return () => { active = false; window.clearInterval(interval) }
  }, [token, hasStudentAccess])
  const released = results?.filter(result => result.released) ?? []
  return <main className="content-page"><div className="page-hero"><div className="section-kicker">Your hard work, recognized</div><h1>Results &amp; <em>recognition.</em></h1><p>{user?.role === 'SCHOOL' ? 'Released results for students registered with your school.' : 'Results are reviewed carefully before they are released to students.'}</p></div>{error ? <PageState title="Results are unavailable" message={error}/> : !loaded ? <div className="page-message"><span className="loader"/><p>Checking your result…</p></div> : !hasStudentAccess ? <div className="results-locked"><LockKeyhole/><h2>Sign in to view your result.</h2><p>Each student can access their own result when it is released.</p><Link className="button button-dark" to="/sign-in">Sign in <ArrowRight size={16}/></Link></div> : !released.length ? <div className="results-locked"><div className="result-asterisk">✳</div><span className="section-kicker">STUDENT RESULTS</span><h2>Results not yet available.</h2><p>Your examination result is not available yet. The administration will release your result when it is ready.</p></div> : released.map((result, i) => <article className="result-detail" key={`${result.candidateId}-${result.examTitle}-${i}`}><div className="section-kicker">{user?.role === 'SCHOOL' ? `${result.candidateName} · ` : 'YOUR '}RELEASED RESULT · {result.status}</div><h2>{result.score ?? '—'} <small>/ {result.possiblePoints ?? '—'}</small></h2><p>{result.examTitle} · {result.submittedAt ? new Date(result.submittedAt).toLocaleString() : 'Submission time unavailable'}</p><div className="result-breakdown"><div><span>Percentage</span><strong>{result.percentage == null ? '—' : `${result.percentage}%`}</strong></div><div><span>Correct answers</span><strong>{result.correctAnswers ?? '—'}</strong></div><div><span>Incorrect answers</span><strong>{result.incorrectAnswers ?? '—'}</strong></div><div><span>Attempted questions</span><strong>{result.attemptedQuestions ?? '—'}</strong></div><div><span>Unanswered questions</span><strong>{result.unansweredQuestions ?? '—'}</strong></div><div><span>Time spent</span><strong>{result.timeSpentSeconds == null ? '—' : `${Math.floor(result.timeSpentSeconds / 60)}m ${result.timeSpentSeconds % 60}s`}</strong></div><div><span>Started</span><strong>{result.startedAt ? new Date(result.startedAt).toLocaleString() : '—'}</strong></div><div><span>Submission type</span><strong>{result.submissionType || '—'}</strong></div></div></article>)}<StudentBottomNav/></main>
}

function LeaderboardPage() {
  const { token } = useAuth()
  const [board, setBoard] = useState<{published:boolean;qualificationDecisionPending:boolean; entries:{studentName:string;schoolName:string;zoneName:string;districtName:string;score:number;rank:number|null;qualified:boolean|null}[]} | null>(null)
  const [error, setError] = useState('')
  useEffect(() => { api<NonNullable<typeof board>>('/leaderboard', {}, token).then(setBoard).catch(e => setError(e.message)) }, [token])
  return <main className="content-page"><div className="page-hero"><div className="section-kicker">Celebrating achievement</div><h1>The next <em>bright minds.</em></h1><p>Recognizing young STEM thinkers across all 20 educational zones.</p></div>{error ? <PageState title="Leaderboard unavailable" message={error}/> : !board ? <div className="page-message"><span className="loader"/><p>Loading leaderboard…</p></div> : !board.published ? <div className="results-locked"><Trophy/><span className="section-kicker">HSSC LEADERBOARD</span><h2>Leaderboard not yet published.</h2><p>Qualifier selections are reviewed by the competition team before the leaderboard is shared.</p></div> : <div className="leaderboard-table"><div className="leaderboard-row leaderboard-head"><span>STUDENT</span><span>SCHOOL</span><span>ZONE</span><span>DISTRICT</span><span>SCORE</span></div>{board.entries.map((entry, i) => <div className="leaderboard-row" key={`${entry.studentName}-${i}`}><span><b className="rank">{entry.rank ?? i + 1}</b>{entry.studentName}</span><span>{entry.schoolName}</span><span>{entry.zoneName}</span><span>{entry.districtName}</span><strong>{entry.score}</strong></div>)}</div>}<StudentBottomNav/></main>
}

type ExamPortalStatus = {
  portalOpen: boolean
  examId: string | null
  examTitle: string | null
  questionCount: number
  durationMinutes: number
  marksPerCorrectAnswer: number
  attemptStatus: 'NOT_STARTED' | 'IN_PROGRESS' | 'SUBMITTED' | 'TIMED_OUT' | null
  opensAt: string | null
  closesAt: string | null
  scheduleStatus: 'NOT_SCHEDULED' | 'SCHEDULED' | 'OPEN' | 'CLOSED' | 'DISABLED'
  participants: { participantId: string; participantName: string; attemptStatus: 'NOT_STARTED' | 'IN_PROGRESS' | 'SUBMITTED' | 'TIMED_OUT' }[]
}
type ExamQuestion = {
  id: string
  order: number
  questionText: string
  options: Record<string, string>
  selectedOption: string | null
}
type ExamAttempt = {
  id: string
  participantId: string | null
  status: 'IN_PROGRESS' | 'SUBMITTED' | 'TIMED_OUT'
  startedAt: string
  deadline: string
  remainingSeconds: number
  attemptCode: string
  questions: ExamQuestion[]
}

function ExamPage() {
  const { token, user } = useAuth()
  const [portal, setPortal] = useState<ExamPortalStatus | null>(null)
  const [attempt, setAttempt] = useState<ExamAttempt | null>(null)
  const [participantId, setParticipantId] = useState('')
  const [completedStatus, setCompletedStatus] = useState<'SUBMITTED' | 'TIMED_OUT' | null>(null)
  const [error, setError] = useState('')
  const [ready, setReady] = useState(false)
  const [starting, setStarting] = useState(false)
  const attemptRef = useRef<ExamAttempt | null>(null)
  const loadingAttemptFor = useRef<string | null>(null)
  const selectedParticipantRef = useRef('')
  attemptRef.current = attempt
  useEffect(() => {
    let active = true
    async function refreshStatus() {
      try {
        const next = await api<ExamPortalStatus>('/exams/student/status', {}, token)
        if (!active) return
        setPortal(next)
        setError('')
        const candidate = next.participants.find(row => row.participantId === selectedParticipantRef.current)
          || next.participants.find(row => row.attemptStatus === 'IN_PROGRESS')
          || next.participants.find(row => row.attemptStatus !== 'NOT_STARTED')
          || next.participants[0]
        const selectedId = candidate?.participantId || ''
        selectedParticipantRef.current = selectedId
        setParticipantId(selectedId)
        if (candidate?.attemptStatus === 'IN_PROGRESS' && next.examId && !attemptRef.current
            && loadingAttemptFor.current !== `${next.examId}:${selectedId}`) {
          loadingAttemptFor.current = `${next.examId}:${selectedId}`
          try {
            const restored = await api<ExamAttempt>(`/exams/${next.examId}/attempt?participantId=${encodeURIComponent(selectedId)}`, {}, token)
            if (active) { attemptRef.current = restored; setAttempt(restored); setCompletedStatus(null) }
          } catch (e) {
            if (active) setError(e instanceof Error ? e.message : 'Your examination session could not be restored.')
          } finally { loadingAttemptFor.current = null }
        } else if (candidate?.attemptStatus === 'SUBMITTED' || candidate?.attemptStatus === 'TIMED_OUT') {
          attemptRef.current = null
          setAttempt(null)
          setCompletedStatus(candidate.attemptStatus)
        } else if (candidate?.attemptStatus === 'NOT_STARTED') {
          setCompletedStatus(null)
        }
      } catch (e) {
        if (active) setError(e instanceof Error ? e.message : 'Examination status could not be checked.')
      }
    }
    void refreshStatus()
    const poll = window.setInterval(() => { void refreshStatus() }, 10000)
    return () => { active = false; window.clearInterval(poll) }
  }, [token])
  async function start() {
    if (!portal?.examId) return
    if (user?.role === 'SCHOOL' && portal.participants.length !== 2) {
      setError('Your school must have exactly two participating students on its roster before it can begin the shared examination.')
      return
    }
    setStarting(true); setError('')
    try {
      const started = await api<ExamAttempt>(`/exams/${portal.examId}/start`, {
        method: 'POST', body: JSON.stringify(user?.role === 'SCHOOL' ? {} : { participantId }),
      }, token)
      attemptRef.current = started
      setAttempt(started)
      setCompletedStatus(null)
      setReady(false)
    } catch (e) { setError(e instanceof Error ? e.message : 'Exam could not be started.') }
    finally { setStarting(false) }
  }
  if (error && !portal && !attempt) return <PageState title="The exam is not available" message={error} retry={() => { setError(''); setPortal(null) }}/>
  if (!portal) return <div className="page-message"><BrandLoader/><p>Checking examination access…</p></div>
  if (attempt) return <ExamRunner exam={attempt} token={token} onSubmitted={status => { attemptRef.current = null; setAttempt(null); setCompletedStatus(status) }}/>
  const selectedCandidate = portal.participants.find(candidate => candidate.participantId === participantId)
  const selectedStatus = completedStatus || selectedCandidate?.attemptStatus
  const dashboardPath = user?.role === 'SCHOOL' ? '/school/dashboard' : '/dashboard'
  if (!portal.examId) {
    return <main className="exam-portal-state"><div className="results-locked"><BookOpen/><span className="section-kicker">EXAMINATION PORTAL</span><h2>Your exam has not been scheduled yet.</h2><p>Your school district does not have a published examination schedule yet.</p></div><StudentBottomNav/></main>
  }
  if (selectedStatus === 'SUBMITTED' || selectedStatus === 'TIMED_OUT') {
    const timedOut = selectedStatus === 'TIMED_OUT'
    return <><div className="submission-page"><div className="submission-check"><Check/></div><div className="section-kicker">EXAMINATION COMPLETE{user?.role === 'SCHOOL' && portal.participants.length === 2 ? ` · ${portal.participants.map(candidate => candidate.participantName).join(' & ')}` : ''}</div><h1>{timedOut ? <>Time is<br/><em>up.</em></> : <>Thank you for<br/><em>submitting.</em></>}</h1><p>{timedOut ? 'Your time expired and the server has recorded this examination as submitted.' : 'Your examination has been submitted successfully. The result will be reviewed and you will be notified when it is released.'}</p><Link className="button button-dark" to={dashboardPath}>Return to overview <ArrowRight size={16}/></Link></div><StudentBottomNav/></>
  }
  if (!portal.portalOpen) {
    const openDate = portal.opensAt ? new Date(portal.opensAt).toLocaleString() : ''
    const closeDate = portal.closesAt ? new Date(portal.closesAt).toLocaleString() : ''
    const scheduled = portal.scheduleStatus === 'SCHEDULED'
    const disabled = portal.scheduleStatus === 'DISABLED'
    return <main className="exam-portal-state"><div className="results-locked"><LockKeyhole/><span className="section-kicker">EXAMINATION PORTAL · {scheduled ? 'SCHEDULED' : disabled ? 'DISABLED' : 'CLOSED'}</span><h2>{scheduled ? 'Your examination is scheduled but not yet open.' : disabled ? 'Exam access is currently disabled for your district.' : 'The exam portal is currently closed.'}</h2><p>{scheduled ? `The examination opens ${openDate} and closes ${closeDate}.` : disabled ? 'The competition administration has not enabled examination access for your district.' : `The examination period ended ${closeDate}. New attempts are no longer available.`}</p><Link className="button button-outline" to={dashboardPath}>Back to overview</Link></div><StudentBottomNav/></main>
  }
  const questionCount = Number(portal.questionCount || 0)
  const durationMinutes = Number(portal.durationMinutes || 0)
  if (!portal.examId || !questionCount || !durationMinutes) return <><PageState title="Exam configuration unavailable" message="The examination settings are incomplete. Please contact the competition administrator."/><StudentBottomNav/></>
  const marksPerCorrect = Number(portal.marksPerCorrectAnswer || 2.5)
  return <><main className="exam-intro"><div className="section-kicker">HSSC · ONLINE EXAMINATION · PORTAL OPEN</div><span className="exam-spark">✳</span><h1>{portal.examTitle ? <>{portal.examTitle}<br/><em>Are you ready?</em></> : <>The exam portal is<br/><em>open.</em></>}</h1><p className="exam-intro-lede">Take a breath. Trust what you know. Give every question your best thinking.</p>{user?.role === 'SCHOOL' && <div className="exam-team-notice"><b>One shared team examination</b><span>{portal.participants.map(candidate => candidate.participantName).join(' & ') || 'Your school’s two registered students'} will work together on one attempt, and the shared result will be credited to both students.</span>{portal.participants.length !== 2 && <strong>Exactly two participating students must be registered before your school can begin.</strong>}</div>}<div className="exam-facts"><div><strong>{questionCount}</strong><span>QUESTIONS</span></div><div><strong>{durationMinutes}</strong><span>MINUTES</span></div><div><strong>{marksPerCorrect}</strong><span>MARKS EACH</span></div><div><strong>{(questionCount * marksPerCorrect).toFixed(2).replace(/\\.00$/, '')}</strong><span>MAX MARKS</span></div></div><ul className="exam-warnings"><li><Check/> Do not refresh, close the examination unnecessarily, or attempt to manipulate your session.</li><li><Check/> The {durationMinutes}-minute timer starts only when you begin and is controlled by the server.</li><li><Check/> Your answers are saved to the server as you go. Review them before manual submission, where time permits.</li><li><Check/> The examination is submitted automatically when the time expires.</li></ul><button className="button button-dark" onClick={() => setReady(true)} disabled={user?.role === 'SCHOOL' && portal.participants.length !== 2}>Begin examination <ArrowRight size={17}/></button>{error && <div className="form-error" role="alert">{error}</div>}{ready && <div className="modal-backdrop" role="presentation" onClick={e => { if (e.target === e.currentTarget) setReady(false) }}><div className="confirm-modal" role="dialog" aria-modal="true" aria-labelledby="exam-modal-title"><button className="modal-close" aria-label="Close" onClick={() => setReady(false)}><X/></button><div className="section-kicker">FINAL CHECK</div><h2 id="exam-modal-title">Ready to begin?</h2><p>This starts your official examination. Your {durationMinutes}-minute timer begins immediately and cannot be reset.</p><div className="modal-stats"><span><b>{questionCount}</b> questions</span><span><b>{durationMinutes} min</b> time limit</span></div><div className="modal-actions"><button className="button button-outline" onClick={() => setReady(false)}>Not yet</button><button className="button button-dark" onClick={() => void start()} disabled={starting}>{starting ? 'Starting…' : 'Start examination'} <ArrowRight size={16}/></button></div></div></div>}</main><StudentBottomNav/></>
}

function ExamRunner({ exam, token, onSubmitted }: { exam: ExamAttempt; token: string | null; onSubmitted: (status: 'SUBMITTED' | 'TIMED_OUT') => void }) {
  const questions = [...exam.questions].sort((a, b) => a.order - b.order)
  const [current, setCurrent] = useState(0)
  const [answers, setAnswers] = useState<Record<string,string>>(() => Object.fromEntries(questions.filter(q => q.selectedOption).map(q => [q.id, q.selectedOption as string])))
  const [remaining, setRemaining] = useState(Number(exam.remainingSeconds))
  const [saving, setSaving] = useState<Record<string,boolean>>({})
  const [error, setError] = useState('')
  const [confirm, setConfirm] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const submittedForTimeout = useRef(false)
  const answerQueues = useRef<Record<string,Promise<void>>>({})
  useEffect(() => {
    const candidateQuery = exam.participantId ? `?participantId=${encodeURIComponent(exam.participantId)}` : ''
    const refresh = () => api<ExamAttempt>(`/exams/${exam.id}/attempt${candidateQuery}`, {}, token).then(r => {
      setRemaining(r.remainingSeconds)
      if (r.status === 'SUBMITTED' || r.status === 'TIMED_OUT') {
        submittedForTimeout.current = true
        onSubmitted(r.status)
      }
    }).catch(e => setError(e.message))
    const syncTimer = window.setInterval(refresh, 10000)
    const countdown = window.setInterval(() => setRemaining(value => Math.max(value - 1, 0)), 1000)
    return () => { window.clearInterval(syncTimer); window.clearInterval(countdown) }
  }, [exam.id, token, onSubmitted])
  useEffect(() => {
    if (remaining !== 0 || submittedForTimeout.current) return
    submittedForTimeout.current = true
    void submit(true)
  }, [remaining])
  async function saveAnswer(qid: string, answer: string) {
    setAnswers(prev => ({ ...prev, [qid]: answer }))
    setSaving(prev => ({ ...prev, [qid]: true }))
    const previous = answerQueues.current[qid] || Promise.resolve()
    const request = previous.catch(() => undefined).then(() => api<void>(`/exams/${exam.id}/answers/${qid}`, {
      method: 'PUT', body: JSON.stringify({ selectedOption: answer, attemptCode: exam.attemptCode }),
    }, token))
    answerQueues.current[qid] = request
    try { await request; setError('') }
    catch (e) { setError(e instanceof Error ? e.message : 'Answer could not be saved.') }
    finally {
      if (answerQueues.current[qid] === request) {
        delete answerQueues.current[qid]
        setSaving(prev => ({ ...prev, [qid]: false }))
      }
    }
  }
  async function submit(timedOut = false) {
    if (submitting) return
    setSubmitting(true)
    try {
      await Promise.all(Object.values(answerQueues.current))
      await api(`/exams/${exam.id}/submit`, { method: 'POST', body: JSON.stringify({ attemptCode: exam.attemptCode }) }, token)
      submittedForTimeout.current = true
      onSubmitted(timedOut ? 'TIMED_OUT' : 'SUBMITTED')
    }
    catch (e) { setError(e instanceof Error ? e.message : 'Submission could not be completed.') }
    finally { setSubmitting(false) }
  }
  if (!questions.length) return <PageState title="Exam questions unavailable" message="The active exam response did not contain questions. Please contact the competition administrator."/>
  const question = questions[current]
  const seconds = Math.max(0, remaining)
  const answeredCount = Object.keys(answers).length
  return <main className="exam-runner"><div className="runner-header"><Brand/><div className={`runner-timer ${seconds < 300 ? 'timer-low' : ''}`} aria-live="polite"><span>SERVER TIME REMAINING</span><strong>{String(Math.floor(seconds / 60)).padStart(2,'0')}:{String(seconds % 60).padStart(2,'0')}</strong></div><button className="button button-dark runner-submit" onClick={() => setConfirm(true)}>Submit exam</button></div>{error && <div className="form-error runner-error" role="alert">{error}</div>}<div className="runner-layout"><section className="question-panel"><div className="question-meta"><span>QUESTION {current + 1} OF {questions.length}</span></div><p className="exam-runner-counts" aria-live="polite">{answeredCount} answered · {questions.length - answeredCount} unanswered</p><h1>{question.questionText}</h1><div className="option-list">{Object.entries(question.options).map(([key, text]) => <button key={key} className={`answer-option ${answers[question.id] === key ? 'option-selected' : ''}`} onClick={() => void saveAnswer(question.id, key)}><span>{key}</span>{text}</button>)}</div><div className="question-answer-status" role="status">{saving[question.id] ? 'Saving answer…' : answers[question.id] ? 'Answer saved to your exam' : 'No answer selected'}</div><div className="question-controls"><button className="button button-outline" disabled={current === 0} onClick={() => setCurrent(current - 1)}>Previous</button><button className="button button-dark" disabled={current === questions.length - 1} onClick={() => setCurrent(current + 1)}>Next question <ArrowRight size={16}/></button></div></section><aside className="question-sidebar"><div className="section-kicker">QUESTION NAVIGATOR</div><p>Jump to any question. Green numbers have saved answers.</p><div className="question-grid">{questions.map((q, i) => <button aria-label={`Question ${i + 1}${answers[q.id] ? ', answered' : ', unanswered'}`} className={`${current === i ? 'navigator-active' : ''} ${answers[q.id] ? 'navigator-answered' : ''}`} key={q.id} onClick={() => setCurrent(i)}>{i + 1}</button>)}</div><div className="navigator-legend"><span><i/> Unanswered</span><span><i/> Answered</span><span><i/> Current</span></div></aside></div>{confirm && <div className="modal-backdrop"><div className="confirm-modal" role="dialog" aria-modal="true"><div className="section-kicker">SUBMIT EXAMINATION</div><h2>Finish and submit?</h2><p>You have answered {answeredCount} of {questions.length} questions. Once submitted, you cannot make changes.</p><div className="modal-actions"><button className="button button-outline" disabled={submitting} onClick={() => setConfirm(false)}>Keep working</button><button className="button button-dark" disabled={submitting} onClick={() => void submit()}>{submitting ? 'Submitting…' : 'Submit examination'}</button></div></div></div>}</main>
}

function AdminSidebar({ active }: { active: string }) {
  const links = [
    ['Overview', '/admin'],
    ['Registrations', '/admin/registrations'],
    ['Review results', '/admin/results'],
    ['Leaderboard', '/admin/leaderboard'],
    ['Question banks', '/admin/question-banks'],
    ['Notifications', '/admin/notifications'],
    ['Settings', '/admin/settings'],
  ]
  return <aside className="admin-sidebar"><Brand admin/><span className="admin-nav-label">MANAGE</span>{links.map(([label, href]) =>
    <Link key={href} to={href} className={label === active ? 'admin-nav-active' : ''}>{label}</Link>)}</aside>
}

function AdminTopbar({ title }: { title: string }) {
  const { user, signOut } = useAuth()
  const initials = user?.email.split('@')[0].slice(0, 2).toUpperCase() || 'AD'
  return <header className="admin-topbar"><span>{title}</span><div className="admin-topbar-actions">
    <Link className="admin-notification-link" to="/admin/notifications" aria-label="Open notifications"><Bell size={16}/></Link>
    <details className="admin-profile"><summary><span className="admin-avatar">{initials}</span><span className="admin-profile-label"><b>{user?.email}</b><small>Administrator</small></span></summary>
      <div className="admin-profile-menu"><span>{user?.email}</span><small>{user?.role || 'ADMIN'}</small><button onClick={signOut}>Sign out</button></div>
    </details>
  </div></header>
}

type AdminNotification = { id: string; type: string; message: string; read: boolean; createdAt: string; actionUrl: string | null }

function AdminNotificationsPage() {
  const { token } = useAuth()
  const navigate = useNavigate()
  const [notifications, setNotifications] = useState<AdminNotification[] | null>(null)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState('')
  useEffect(() => {
    api<AdminNotification[]>('/notifications', {}, token)
      .then(setNotifications)
      .catch(e => setError(e instanceof Error ? e.message : 'Unable to load notifications.'))
  }, [token])
  async function openNotification(notification: AdminNotification) {
    setError('')
    if (!notification.read) {
      setBusyId(notification.id)
      try {
        await api(`/notifications/${notification.id}/read`, { method: 'POST' }, token)
        setNotifications(items => items?.map(item => item.id === notification.id ? { ...item, read: true } : item) ?? [])
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Notification could not be marked as read.')
        setBusyId('')
        return
      }
    }
    setBusyId('')
    navigate(notification.actionUrl || '/admin')
  }
  return <main className="admin-page"><AdminSidebar active="Notifications"/><section className="admin-main"><AdminTopbar title="Notifications"/><div className="admin-content">
    <div className="section-kicker">ADMINISTRATION</div><h1>Notifications</h1><p>Updates about school registrations and competition activity.</p>
    {error && <div className="form-error" role="alert">{error}</div>}
    {!notifications ? <div className="page-message"><span className="loader"/><p>Loading notifications…</p></div> : notifications.length === 0 ? <div className="empty-inline">You’re all caught up. New competition updates will appear here.</div> :
      <div className="admin-notification-list">{notifications.map(item => <article className={`admin-notification ${item.read ? '' : 'notification-unread'}`} key={item.id}>
        <span className="notification-indicator" aria-hidden="true"/><div><span className="section-kicker">{item.type.replaceAll('_', ' ')}</span><p>{item.message}</p><time dateTime={item.createdAt}>{new Date(item.createdAt).toLocaleString()}</time></div>
        <button className="button button-outline" disabled={busyId === item.id} onClick={() => void openNotification(item)}>{busyId === item.id ? 'Opening…' : item.actionUrl ? 'Open' : item.read ? 'View' : 'Mark read'}</button>
      </article>)}</div>}
  </div></section></main>
}

function StudentPhoto({ documentId, token }: { documentId: string; token: string }) {
  const [url, setUrl] = useState('')
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    let objectUrl = ''
    downloadDocument(documentId, token).then(blob => {
      if (!blob.type.startsWith('image/')) return
      objectUrl = URL.createObjectURL(blob)
      if (active) setUrl(objectUrl)
    }).catch(e => { if (active) setError(e.message) })
    return () => { active = false; if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }, [documentId, token])
  if (error) return <span className="admin-photo-error">Photo unavailable</span>
  return url ? <img className="admin-student-photo" src={url} alt="Student registration photograph"/> : <span className="admin-photo-placeholder">Loading photo…</span>
}

function AdminParticipantPhoto({ documentId, token }: { documentId: string; token: string }) {
  const [url, setUrl] = useState('')
  const [error, setError] = useState(false)
  useEffect(() => {
    let active = true
    let objectUrl = ''
    downloadDocument(documentId, token).then(blob => {
      if (!blob.type.startsWith('image/')) { setError(true); return }
      objectUrl = URL.createObjectURL(blob)
      if (active) setUrl(objectUrl)
    }).catch(() => { if (active) setError(true) })
    return () => { active = false; if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }, [documentId, token])
  if (error) return <div className="participant-review-photo participant-review-photo-error">Photo unavailable</div>
  return url ? <img className="participant-review-photo" src={url} alt="Participating student wearing school uniform"/> :
    <div className="participant-review-photo participant-review-photo-loading">Loading photo…</div>
}

type SchoolReview = {
  id: string; accountId: string; name: string; registrationNumber: string | null; address: string; status: string; loginEmail: string
  principalName: string | null; principalEmail: string | null; principalPhone: string | null
  teacherInChargeName: string | null; teacherInChargePhone: string | null; principalSignatureDocumentId: string | null; zoneId: string
  zoneName: string; districtId: string; districtName: string; studentCount: number
}
type AdminStudentReview = {
  id: string; email: string; fullName: string; studentNumber: string; age: number | null; gender: string; phone: string | null
  parentGuardianName: string; guardianPhone: string; homeAddress: string; status: string
  documents: { id: string; originalName: string; contentType: string }[]
}
type ParticipantReview = { id: string; fullName: string; gender: string; age: number; guardianName: string; guardianPhone: string; homeAddress: string; photoDocumentId: string }

function AdminRegistrationsPage() {
  const { token } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const zoneRequest = useRef(0)
  const schoolRequest = useRef(0)
  const [districts, setDistricts] = useState<District[]>([])
  const [zones, setZones] = useState<{ id: string; name: string }[]>([])
  const [districtId, setDistrictId] = useState('')
  const [zoneId, setZoneId] = useState('')
  const [schools, setSchools] = useState<SchoolReview[]>([])
  const [selectedSchool, setSelectedSchool] = useState<SchoolReview | null>(null)
  const [students, setStudents] = useState<AdminStudentReview[]>([])
  const [participants, setParticipants] = useState<ParticipantReview[]>([])
  const [loading, setLoading] = useState(true)
  const [zonesLoading, setZonesLoading] = useState(false)
  const [schoolsLoading, setSchoolsLoading] = useState(false)
  const [schoolLoading, setSchoolLoading] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [success, setSuccess] = useState('')
  useEffect(() => {
    api<District[]>('/admin/districts', {}, token).then(setDistricts)
      .catch(e => setError(e.message)).finally(() => setLoading(false))
  }, [token])
  const requestedDistrictId = searchParams.get('district')
  useEffect(() => {
    if (!requestedDistrictId && districtId) {
      zoneRequest.current += 1
      schoolRequest.current += 1
      setDistrictId('')
      setZoneId('')
      setSchools([])
      setSelectedSchool(null)
      setStudents([])
      setParticipants([])
      setSchoolsLoading(false)
      setSchoolLoading(false)
    } else if (requestedDistrictId && districts.some(district => district.id === requestedDistrictId) && districtId !== requestedDistrictId) {
      zoneRequest.current += 1
      schoolRequest.current += 1
      setDistrictId(requestedDistrictId)
      setZoneId('')
      setSchools([])
      setSelectedSchool(null)
      setStudents([])
      setParticipants([])
      setSchoolsLoading(false)
      setSchoolLoading(false)
    }
  }, [districts, districtId, requestedDistrictId])
  useEffect(() => {
    if (!districtId) { setZones([]); return }
    let active = true
    setZonesLoading(true)
    api<{ id: string; name: string }[]>(`/admin/districts/${districtId}/zones`, {}, token)
      .then(rows => { if (active) setZones(rows) })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setZonesLoading(false) })
    return () => { active = false }
  }, [districtId, token])
  function selectDistrict(id: string) {
    zoneRequest.current += 1
    schoolRequest.current += 1
    setDistrictId(id)
    setSearchParams({ district: id })
    setZoneId('')
    setSchools([])
    setSelectedSchool(null)
    setStudents([])
    setParticipants([])
    setSchoolsLoading(false)
    setSchoolLoading(false)
    setError('')
  }
  async function selectZone(id: string) {
    const requestId = ++zoneRequest.current
    schoolRequest.current += 1
    setZoneId(id)
    setSelectedSchool(null)
    setStudents([])
    setParticipants([])
    setSchoolsLoading(false)
    setSchoolLoading(false)
    setError('')
    setSchoolsLoading(true)
    try {
      const records = await api<SchoolReview[]>(`/admin/zones/${id}/schools`, {}, token)
      if (requestId === zoneRequest.current) setSchools(records)
    } catch (e) {
      if (requestId === zoneRequest.current) setError(e instanceof Error ? e.message : 'Unable to load schools in this zone.')
    } finally {
      if (requestId === zoneRequest.current) setSchoolsLoading(false)
    }
  }
  async function openSchool(school: SchoolReview) {
    const requestId = ++schoolRequest.current
    setSelectedSchool(school)
    setSchoolLoading(true)
    setStudents([])
    setParticipants([])
    setError('')
    try {
      const [detail, enrolled, roster] = await Promise.all([
        api<SchoolReview>(`/admin/schools/${school.id}`, {}, token),
        api<AdminStudentReview[]>(`/admin/schools/${school.id}/students`, {}, token),
        api<ParticipantReview[]>(`/admin/schools/${school.id}/participants`, {}, token),
      ])
      if (requestId === schoolRequest.current) {
        setSelectedSchool(detail)
        setStudents(enrolled)
        setParticipants(roster)
      }
    } catch (e) {
      if (requestId === schoolRequest.current) setError(e instanceof Error ? e.message : 'Unable to load school details.')
    } finally {
      if (requestId === schoolRequest.current) setSchoolLoading(false)
    }
  }
  async function approveSchool() {
    if (!selectedSchool || selectedSchool.status !== 'PENDING') return
    setBusy(true)
    setError('')
    setSuccess('')
    try {
      await api(`/admin/accounts/${selectedSchool.accountId}/status?status=ACTIVE`, { method: 'POST' }, token)
      setSuccess(`${selectedSchool.name} has been approved and can now sign in.`)
      setSchools(rows => rows.map(row => row.id === selectedSchool.id ? { ...row, status: 'ACTIVE' } : row))
      setSelectedSchool(current => current ? { ...current, status: 'ACTIVE' } : current)
    } catch (e) { setError(e instanceof Error ? e.message : 'School approval could not be completed.') }
    finally { setBusy(false) }
  }
  function backToDistricts() {
    zoneRequest.current += 1
    schoolRequest.current += 1
    setSearchParams({})
    setDistrictId('')
    setZoneId('')
    setZones([])
    setSchools([])
    setSelectedSchool(null)
    setStudents([])
    setParticipants([])
    setSchoolsLoading(false)
    setSchoolLoading(false)
  }
  function backToZones() {
    zoneRequest.current += 1
    schoolRequest.current += 1
    setZoneId('')
    setSchools([])
    setSelectedSchool(null)
    setStudents([])
    setParticipants([])
    setSchoolsLoading(false)
    setSchoolLoading(false)
  }
  function backToSchools() {
    schoolRequest.current += 1
    setSchoolLoading(false)
    setSelectedSchool(null)
    setStudents([])
    setParticipants([])
  }
  async function archiveSchool() {
    if (!selectedSchool || !window.confirm(`Archive ${selectedSchool.name} and disable its associated student accounts? Existing competition history will be retained.`)) return
    setBusy(true)
    setError('')
    setSuccess('')
    try {
      await api(`/admin/schools/${selectedSchool.id}`, { method: 'DELETE' }, token)
      setSuccess(`${selectedSchool.name} was archived.`)
      setSelectedSchool(null)
      setStudents([])
      setParticipants([])
      await selectZone(zoneId)
    } catch (e) { setError(e instanceof Error ? e.message : 'School could not be archived.') }
    finally { setBusy(false) }
  }
  return <main className="admin-page"><AdminSidebar active="Registrations"/><section className="admin-main"><AdminTopbar title="Registration management"/><div className="admin-content">
    <div className="section-kicker">REGISTRATION PROCESS</div><h1>Schools by district</h1><p>Follow the database-backed district, zone, and school records to review registrations and enrolled students.</p>
    <nav className="admin-breadcrumb" aria-label="Registration hierarchy">
      <button onClick={backToDistricts} aria-current={!districtId ? 'step' : undefined}>Districts</button>
      {districtId && <><ChevronRight size={14}/><button onClick={backToZones} aria-current={!zoneId ? 'step' : undefined}>{districts.find(d => d.id === districtId)?.name || 'District'}</button></>}
      {zoneId && <><ChevronRight size={14}/><button onClick={backToSchools} aria-current={!selectedSchool ? 'step' : undefined}>{zones.find(z => z.id === zoneId)?.name || 'Zone'}</button></>}
      {selectedSchool && <><ChevronRight size={14}/><span aria-current="page">{selectedSchool.name}</span></>}
    </nav>
    {error && <div className="form-error" role="alert">{error}</div>}{success && <div className="success-inline" role="status">{success}</div>}
    {loading ? <div className="page-message"><span className="loader"/><p>Loading districts…</p></div> : <div className="admin-hierarchy">
      {!districtId && <section className="admin-hierarchy-step"><h2>01 · Select a district</h2><div className="admin-choice-grid">{districts.map(d => <button key={d.id} className="admin-choice" onClick={() => selectDistrict(d.id)}>{d.name}<ChevronRight size={15}/></button>)}</div></section>}
      {districtId && <section className="admin-hierarchy-step"><h2>02 · {districts.find(d => d.id === districtId)?.name} zones</h2>{zonesLoading ? <div className="page-message"><span className="loader"/><p>Loading zones…</p></div> : zones.length ? <div className="admin-choice-grid">{zones.map(z => <button key={z.id} className={zoneId === z.id ? 'admin-choice active' : 'admin-choice'} onClick={() => void selectZone(z.id)}>{z.name}<ChevronRight size={15}/></button>)}</div> : <div className="empty-inline">No zones are configured for this district.</div>}</section>}
      {zoneId && <section className="admin-hierarchy-step"><h2>03 · Registered schools in {zones.find(z => z.id === zoneId)?.name}</h2>{schoolsLoading ? <div className="page-message"><span className="loader"/><p>Loading registered schools…</p></div> : schools.length ? <div className="admin-school-list">{schools.map(school => <button key={school.id} className={selectedSchool?.id === school.id ? 'admin-school-row active' : 'admin-school-row'} onClick={() => void openSchool(school)}><span><b>{school.name}</b><small>{school.principalEmail || school.loginEmail}</small></span><span className={`status-pill ${school.status === 'ACTIVE' ? 'status-open' : ''}`}>{school.status}</span><ChevronRight size={15}/></button>)}</div> : <div className="empty-inline">No schools are registered in this zone yet.</div>}</section>}
      {selectedSchool && <section className="admin-hierarchy-step school-review-detail"><div className="admin-section-heading"><div><h2>{selectedSchool.name}</h2><span className="status-pill">{selectedSchool.status}</span></div><div className="school-review-actions">{selectedSchool.status === 'PENDING' && <button className="button button-dark" disabled={busy || schoolLoading} onClick={() => void approveSchool()}>{busy ? 'Approving…' : 'Approve school'}</button>}<button className="button button-outline" disabled={busy || schoolLoading} onClick={() => void archiveSchool()}>{busy ? 'Updating…' : 'Archive school'}</button></div></div>
        {schoolLoading ? <div className="page-message"><span className="loader"/><p>Loading school details and student registrations…</p></div> : <>
        <dl className="school-review-fields"><div><dt>District</dt><dd>{selectedSchool.districtName}</dd></div><div><dt>Zone</dt><dd>{selectedSchool.zoneName}</dd></div><div><dt>School address</dt><dd>{selectedSchool.address}</dd></div><div><dt>School account email</dt><dd>{selectedSchool.loginEmail}</dd></div><div><dt>Principal</dt><dd>{selectedSchool.principalName || 'Not provided'}</dd></div><div><dt>Principal email</dt><dd>{selectedSchool.principalEmail || 'Not provided'}</dd></div><div><dt>Principal phone</dt><dd>{selectedSchool.principalPhone || 'Not provided'}</dd></div><div><dt>Teacher in charge</dt><dd>{selectedSchool.teacherInChargeName || 'Not provided'}</dd></div><div><dt>Teacher in charge phone</dt><dd>{selectedSchool.teacherInChargePhone || 'Not provided'}</dd></div><div><dt>Principal signature</dt><dd>{selectedSchool.principalSignatureDocumentId ? <PrincipalSignature documentId={selectedSchool.principalSignatureDocumentId} token={token || ''}/> : 'Not provided'}</dd></div><div><dt>School registration number</dt><dd>{selectedSchool.registrationNumber || 'Not provided'}</dd></div></dl>
        <h3>Participating students submitted with school registration ({participants.length})</h3>{participants.length ? <div className="participant-review-grid">{participants.map(student => <article className="participant-review-card" key={student.id}><AdminParticipantPhoto documentId={student.photoDocumentId} token={token || ''}/><div className="participant-review-info"><span className="section-kicker">PARTICIPANT · {selectedSchool.name}</span><h4>{student.fullName}</h4><p>{student.gender} <span>·</span> Age {student.age}</p><p>Parent / guardian: {student.guardianName}</p><p>Guardian phone: {student.guardianPhone || 'Not provided'}</p><p>Home address: {student.homeAddress}</p></div></article>)}</div> : <div className="empty-inline">No participating student roster was submitted with this school registration.</div>}
        <h3 className="admin-registered-students-heading">Student portal accounts ({students.length})</h3>{students.length ? <div className="admin-student-list">{students.map(student => <article className="admin-student-row" key={student.id}><div className="admin-student-photo-frame">{student.documents.filter(doc => doc.contentType.startsWith('image/')).slice(0,1).map(doc => <StudentPhoto key={doc.id} documentId={doc.id} token={token || ''}/>)}</div><div><b>{student.fullName}</b><span>{student.studentNumber} · {student.age ?? 'Age not provided'} · {student.gender}{student.phone ? ` · ${student.phone}` : ''}</span><small>{student.email}</small><small>Parent / guardian: {student.parentGuardianName} · {student.guardianPhone ? `Guardian phone: ${student.guardianPhone} · ` : ''}Home address: {student.homeAddress}</small></div><span className="status-pill">{student.status}</span></article>)}</div> : <div className="empty-inline">No separately registered student portal accounts are linked to this school yet.</div>}
        </>}
      </section>}
    </div>}
  </div></section></main>
}

function AdminPage() {
  const { token } = useAuth()
  const [data, setData] = useState<{pendingAccounts:number;activeSchools:number;activeStudents:number;exams:number;attempts:number} | null>(null)
  const [districts, setDistricts] = useState<{id:string;code:string;name:string;zoneCount:number;schoolCount:number;studentCount:number;examAccessOpen:boolean}[]>([])
  const [error, setError] = useState('')
  const [busyDistrict, setBusyDistrict] = useState('')
  async function load() {
    setError('')
    try {
      const [summary, districtRows] = await Promise.all([
        api<typeof data>('/admin/dashboard', {}, token),
        api<typeof districts>('/admin/districts/overview', {}, token),
      ])
      setData(summary)
      setDistricts(districtRows)
    } catch (e) { setError(e instanceof Error ? e.message : 'Unable to load administration data.') }
  }
  useEffect(() => { void load() }, [token])
  async function toggleDistrict(district: typeof districts[number]) {
    const action = district.examAccessOpen ? 'disable' : 'enable'
    if (!window.confirm(`${action === 'enable' ? 'Enable' : 'Disable'} exam access for ${district.name}? This does not create or publish an examination schedule. Closing access will not cancel active attempts.`)) return
    setBusyDistrict(district.id)
    setError('')
    try {
      await api(`/admin/districts/${district.id}/portal/exam_access`, {
        method: 'PUT', body: JSON.stringify({ enabled: !district.examAccessOpen }),
      }, token)
      setDistricts(rows => rows.map(row => row.id === district.id ? { ...row, examAccessOpen: !row.examAccessOpen } : row))
    } catch (e) { setError(e instanceof Error ? e.message : 'District exam portal could not be updated.') }
    finally { setBusyDistrict('') }
  }
  const statistics = data ? [
    ['Pending reviews', data.pendingAccounts],
    ['Active schools', data.activeSchools],
    ['Active students', data.activeStudents],
    ['Exams', data.exams],
    ['Submitted attempts', data.attempts],
  ] as const : []
  return <main className="admin-page"><AdminSidebar active="Overview"/><section className="admin-main"><AdminTopbar title="Competition management"/><div className="admin-content"><div className="section-kicker">HSSC ADMINISTRATION</div><h1>Competition overview</h1><p>Exam access is a district-level permission only. Examination schedules are created and published separately for a selected district in Question banks &amp; exam setup.</p>{error && <div className="form-error" role="alert">{error}</div>}{!data ? <div className="page-message"><span className="loader"/><p>Loading competition data…</p></div> : <><div className="admin-stats">{statistics.map(([label,value]) => <article key={label}><span>{label.toUpperCase()}</span><strong>{value}</strong></article>)}</div>  <div className="admin-section-heading"><div><div className="section-kicker">DISTRICT EXAM ACCESS</div><h2>District access controls</h2></div><Link className="text-link" to="/admin/question-banks">Prepare or review examination schedules <ArrowRight size={15}/></Link></div>{districts.length ? <div className="admin-district-grid">{districts.map(d => <article key={d.id} className="admin-district"><Link className="admin-district-open" to={`/admin/registrations?district=${encodeURIComponent(d.id)}`} aria-label={`Review schools in ${d.name}`}><span className="section-kicker">{d.name}</span><span className="admin-district-numbers"><span><strong>{d.schoolCount}</strong><span>schools</span></span><span><strong>{d.studentCount}</strong><span>students</span></span></span><span className="admin-district-footer"><span>{d.zoneCount} zones</span><ChevronRight size={16}/></span></Link><div className="admin-district-footer admin-district-portal"><span className={`status-pill ${d.examAccessOpen ? 'status-open' : ''}`}>{d.examAccessOpen ? 'ACCESS ENABLED' : 'ACCESS DISABLED'}</span><button className="button button-outline district-portal-toggle" disabled={busyDistrict === d.id} onClick={() => void toggleDistrict(d)}>{busyDistrict === d.id ? 'Updating…' : d.examAccessOpen ? 'Disable exam access' : 'Enable exam access'}</button></div></article>)}</div> : <div className="empty-inline">No districts are configured. Check the district seed settings and restart the backend.</div>}<div className="admin-section-heading admin-results-heading"><div><div className="section-kicker">REVIEW RESULTS</div><h2>Submissions and result review</h2></div><Link className="text-link" to="/admin/results">Open review results <ArrowRight size={15}/></Link></div><p className="admin-section-description">Review submitted attempts and release results by district after verification.</p></>}</div></section></main>
}

function AdminFeature({ title, endpoint }: { title: string; endpoint: string }) {
  const { token } = useAuth()
  const [data, setData] = useState<unknown>(null)
  const [error, setError] = useState('')
  useEffect(() => { api(endpoint, {}, token).then(setData).catch(e => setError(e.message)) }, [endpoint, token])
  return <main className="admin-page"><AdminSidebar active={title === 'Review results' ? 'Review results' : title}/><section className="admin-main"><AdminTopbar title={title}/><div className="admin-content"><div className="section-kicker">ADMINISTRATION</div><h1>{title}</h1>{error ? <PageState title="Unable to load this area" message={error}/> : !data ? <div className="page-message"><span className="loader"/><p>Loading secure admin data…</p></div> : <pre className="admin-json">{JSON.stringify(data, null, 2)}</pre>}</div></section></main>
}

type AdminExamResult = {
  examId: string; examTitle: string; attemptId: string; attemptCode: string; studentId: string | null
  studentName: string; studentNumber: string | null; schoolName: string | null; districtName: string | null
  status: string; score: number; possiblePoints: number; startedAt: string; submittedAt: string | null
  timeSpentSeconds: number; submissionType: string; totalQuestions: number; attemptedQuestions: number
  answeredQuestions: number; unansweredQuestions: number; correctAnswers: number; incorrectAnswers: number
  anonymousBlankCredits: number; marksPerCorrectAnswer: number; percentage: number; released: boolean
}

function AdminResultsPage() {
  const { token } = useAuth()
  const [results, setResults] = useState<AdminExamResult[] | null>(null)
  const [exams, setExams] = useState<{id:string;title:string;status:string;closesAt:string}[]>([])
  const [filter, setFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [minimumScore, setMinimumScore] = useState('')
  const [examId, setExamId] = useState('')
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  async function load() {
    setError('')
    try {
      const [examRows, resultRows] = await Promise.all([
        api<typeof exams>('/admin/exams', {}, token),
        api<AdminExamResult[]>(`/admin/results${examId ? `?examId=${encodeURIComponent(examId)}` : ''}`, {}, token),
      ])
      setExams(examRows)
      setResults(resultRows)
    } catch (e) { setError(e instanceof Error ? e.message : 'Examination results could not be loaded.') }
  }
  useEffect(() => { void load() }, [token, examId])
  const visible = (results || []).filter(row =>
    `${row.studentName} ${row.studentNumber || ''} ${row.schoolName || ''} ${row.status}`.toLowerCase().includes(filter.toLowerCase())
    && (!statusFilter || row.status === statusFilter)
    && (!minimumScore || row.score >= Number(minimumScore)))
  async function release(row: AdminExamResult) {
    if (!window.confirm(`Release ${row.studentName}'s result to their school account?`)) return
    setBusy(row.attemptId); setError(''); setSuccess('')
    try {
      await api(`/admin/exams/${row.examId}/attempts/${row.attemptId}/release`, { method: 'POST' }, token)
      setSuccess(`Result released for ${row.studentName}.`)
      await load()
    } catch (e) { setError(e instanceof Error ? e.message : 'Result could not be released.') }
    finally { setBusy('') }
  }
  async function releaseAll() {
    const selectedExam = exams.find(exam => exam.id === examId)
    if (!selectedExam || !window.confirm(`Release all finalized results for ${selectedExam.title}?`)) return
    setBusy('all'); setError(''); setSuccess('')
    try {
      await api(`/admin/exams/${selectedExam.id}/release-results`, { method: 'POST' }, token)
      setSuccess(`All finalized results for ${selectedExam.title} have been released.`)
      await load()
    } catch (e) { setError(e instanceof Error ? e.message : 'Results could not be released.') }
    finally { setBusy('') }
  }
  return <main className="admin-page"><AdminSidebar active="Review results"/><section className="admin-main"><AdminTopbar title="Examination results"/><div className="admin-content">
    <div className="section-kicker">EXAMINATION MONITORING</div><h1>Review results</h1><p>Review server-marked attempts and release individual results or all results for a completed examination.</p>
    {error && <div className="form-error" role="alert">{error}</div>}{success && <div className="success-inline" role="status">{success}</div>}
    <div className="admin-result-filters"><label>Examination<select value={examId} onChange={event => setExamId(event.target.value)}><option value="">All examinations</option>{exams.map(exam => <option key={exam.id} value={exam.id}>{exam.title} · {exam.status}</option>)}</select></label><label>Search student or school<input value={filter} onChange={event => setFilter(event.target.value)} placeholder="Search results"/></label><label>Attempt status<select value={statusFilter} onChange={event => setStatusFilter(event.target.value)}><option value="">All statuses</option><option value="IN_PROGRESS">In progress</option><option value="SUBMITTED">Submitted</option><option value="TIMED_OUT">Timed out</option></select></label><label>Minimum score<input type="number" min="0" value={minimumScore} onChange={event => setMinimumScore(event.target.value)} placeholder="Any score"/></label>{examId && <button className="button button-dark" disabled={busy !== ''} onClick={() => void releaseAll()}>{busy === 'all' ? 'Releasing…' : 'Release all finalized results'}</button>}</div>
    {!results ? <div className="page-message"><span className="loader"/><p>Loading examination results…</p></div> : visible.length === 0 ? <div className="empty-inline">No examination attempts match this selection.</div> : <div className="admin-result-list">{visible.map(row => <article className="admin-result-card" key={row.attemptId}>
      <div className="admin-result-heading"><div><span className="section-kicker">{row.examTitle} · {row.status.replaceAll('_',' ')}</span><h2>{row.studentName}</h2><p>{row.studentNumber || 'No student number'} · {row.schoolName || 'Anonymous'} · {row.districtName || '—'}</p></div><span className={`status-pill ${row.released ? 'status-open' : ''}`}>{row.released ? 'RESULT RELEASED' : 'AWAITING RELEASE'}</span></div>
      <div className="admin-result-metrics"><div><span>Score</span><strong>{row.score} / {row.possiblePoints}</strong><small>{row.correctAnswers} × {row.marksPerCorrectAnswer} marks</small></div><div><span>Correct / incorrect</span><strong>{row.correctAnswers} / {row.incorrectAnswers}</strong></div><div><span>Attempted / unanswered</span><strong>{row.attemptedQuestions} / {row.unansweredQuestions}</strong></div><div><span>Percentage</span><strong>{row.percentage}%</strong></div><div><span>Time spent</span><strong>{Math.floor(row.timeSpentSeconds / 60)}m {row.timeSpentSeconds % 60}s</strong></div><div><span>Submission</span><strong>{row.submissionType} · {row.status}</strong></div></div>
      <small className="admin-help-text">{row.anonymousBlankCredits} unattempted anonymous questions credited at {row.marksPerCorrectAnswer} marks each.</small>
      <div className="admin-result-dates"><span>Started {new Date(row.startedAt).toLocaleString()}</span><span>{row.submittedAt ? `Submitted ${new Date(row.submittedAt).toLocaleString()}` : 'Not yet submitted'}</span></div>
      {!row.released && row.status !== 'IN_PROGRESS' && <button className="button button-outline" disabled={busy !== ''} onClick={() => void release(row)}>{busy === row.attemptId ? 'Releasing…' : 'Release this result'}</button>}
    </article>)}</div>}
  </div></section></main>
}

type AdminLeaderboard = {
  exams: {id:string;title:string;status:string;closesAt:string}[]; examId:string|null
  candidates: {id:string;name:string;zoneName:string;districtName:string;topScore:number;finalizedResults:number}[]
  selectedSchoolIds:string[]; published:boolean
}

function AdminLeaderboardPage() {
  const { token } = useAuth()
  const [data, setData] = useState<AdminLeaderboard | null>(null)
  const [examId, setExamId] = useState('')
  const [selected, setSelected] = useState<string[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  async function load(id = examId) {
    setError('')
    try {
      const next = await api<AdminLeaderboard>(`/admin/leaderboard${id ? `?examId=${encodeURIComponent(id)}` : ''}`, {}, token)
      setData(next)
      setExamId(next.examId || '')
      setSelected(next.selectedSchoolIds || [])
    } catch (e) { setError(e instanceof Error ? e.message : 'Leaderboard candidates could not be loaded.') }
  }
  useEffect(() => { void load('') }, [token])
  function toggle(id: string) {
    setSelected(current => current.includes(id) ? current.filter(value => value !== id) : current.length < 20 ? [...current, id] : current)
  }
  async function publish() {
    if (!examId || !window.confirm(`Publish the leaderboard with ${selected.length} selected schools?`)) return
    setBusy(true); setError(''); setSuccess('')
    try {
      await api('/admin/leaderboard/publish', { method:'POST', body:JSON.stringify({examId,schoolIds:selected}) }, token)
      setSuccess('The selected school leaderboard has been published.')
      await load(examId)
    } catch (e) { setError(e instanceof Error ? e.message : 'Leaderboard could not be published.') }
    finally { setBusy(false) }
  }
  return <main className="admin-page"><AdminSidebar active="Leaderboard"/><section className="admin-main"><AdminTopbar title="Leaderboard & qualification"/><div className="admin-content">
    <div className="section-kicker">QUALIFYING SCHOOLS</div><h1>Leaderboard</h1><p>Select up to 20 schools from finalized, released results. Students are ranked by individual result score; equal scores share the same rank.</p>
    {error && <div className="form-error" role="alert">{error}</div>}{success && <div className="success-inline" role="status">{success}</div>}
    {!data ? <div className="page-message"><span className="loader"/><p>Loading eligible schools…</p></div> : <>
      <div className="admin-result-filters"><label>Examination<select value={examId} onChange={event => { setExamId(event.target.value); void load(event.target.value) }}><option value="">Select examination</option>{data.exams.map(exam => <option key={exam.id} value={exam.id}>{exam.title} · {exam.status}</option>)}</select></label><span className={`status-pill ${data.published ? 'status-open' : ''}`}>{data.published ? 'PUBLISHED' : 'DRAFT'}</span><span>{selected.length} / 20 schools selected</span></div>
      {!data.candidates.length ? <div className="empty-inline">No finalized, released student results are available to qualify schools yet.</div> : <div className="admin-candidate-list">{data.candidates.map(school => <label className="admin-candidate" key={school.id}><input type="checkbox" checked={selected.includes(school.id)} disabled={!selected.includes(school.id) && selected.length >= 20} onChange={() => toggle(school.id)}/><span><b>{school.name}</b><small>{school.zoneName} · {school.districtName}</small></span><span><b>{school.topScore}</b><small>best student score</small></span><span>{school.finalizedResults} released result{school.finalizedResults === 1 ? '' : 's'}</span></label>)}</div>}
      <button className="button button-dark" disabled={busy || !examId || selected.length === 0} onClick={() => void publish()}>{busy ? 'Publishing…' : data.published ? 'Update published leaderboard' : 'Publish leaderboard'} <ArrowRight size={16}/></button>
    </>}
  </div></section></main>
}

type QuestionBankRecord = {id:string;name:string;active:boolean}
type AdminExamRecord = {id:string;title:string;opensAt:string;closesAt:string;durationMinutes:number;questionCount:number;status:string;districtId:string|null;districtName:string|null;bankId:string;bankName:string}
type QuestionSetLoadResponse = {id:string;name:string;active:boolean;questionCount:number;keyedQuestionCount:number;anonymousQuestionCount:number}

function AdminQuestionBanksPage() {
  const { token } = useAuth()
  const [banks, setBanks] = useState<QuestionBankRecord[] | null>(null)
  const [exams, setExams] = useState<AdminExamRecord[] | null>(null)
  const [districts, setDistricts] = useState<District[] | null>(null)
  const [bankId, setBankId] = useState('')
  const [districtId, setDistrictId] = useState('')
  const [examTitle, setExamTitle] = useState('HSSC STEM examination')
  const localDateTime = (minutesAhead: number) => {
    const date = new Date(Date.now() + minutesAhead * 60000)
    date.setSeconds(0, 0)
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
  }
  const [opensAt, setOpensAt] = useState(() => localDateTime(10))
  const [closesAt, setClosesAt] = useState(() => localDateTime(250))
  const questionCount = '200'
  const [questionFile, setQuestionFile] = useState<File | null>(null)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  async function load() {
    setError('')
    try {
      const [bankRows, examRows, districtRows] = await Promise.all([
        api<QuestionBankRecord[]>('/admin/question-banks', {}, token),
        api<AdminExamRecord[]>('/admin/exams', {}, token),
        api<District[]>('/districts'),
      ])
      setBanks(bankRows); setExams(examRows); setDistricts(districtRows)
    } catch (e) { setError(e instanceof Error ? e.message : 'Exam configuration could not be loaded.') }
  }
  useEffect(() => { void load() }, [token])
  async function createPlaceholders() {
    if (!window.confirm('Create a clearly labeled testing bank with 200 placeholder questions? These are not official competition questions.')) return
    setBusy('placeholders'); setError(''); setSuccess('')
    try {
      const bank = await api<QuestionBankRecord>('/admin/question-banks/placeholders', { method: 'POST' }, token)
      setSuccess('The 200-question placeholder testing bank is ready.')
      await load()
      setBankId(bank.id)
    } catch (e) { setError(e instanceof Error ? e.message : 'Placeholder questions could not be created.') }
    finally { setBusy('') }
  }
  async function loadQuestionSet(setCode: 'A' | 'B') {
    setBankId('')
    setBusy(`set-${setCode}`); setError(''); setSuccess('')
    try {
      const result = await api<QuestionSetLoadResponse>(`/admin/question-banks/official-sets/${setCode}`, { method: 'POST' }, token)
      await load()
      setBankId(result.id)
      setSuccess(`${result.name} selected: ${result.questionCount} questions, ${result.keyedQuestionCount} keyed, ${result.anonymousQuestionCount} marked anonymous.`)
    } catch (e) { setError(e instanceof Error ? e.message : `Set ${setCode} could not be loaded.`) }
    finally { setBusy('') }
  }
  function selectBank(value: string) {
    if (value === 'official:A' || value === 'official:B') {
      void loadQuestionSet(value.slice(-1) as 'A' | 'B')
      return
    }
    setBankId(value)
  }
  async function importQuestions() {
    if (!bankId || !questionFile) return
    setBusy('import'); setError(''); setSuccess('')
    try {
      const parsed: unknown = JSON.parse(await questionFile.text())
      if (!Array.isArray(parsed)) throw new Error('The JSON file must contain an array of question objects.')
      await api(`/admin/question-banks/${bankId}/questions/import`, { method: 'POST', body: JSON.stringify(parsed) }, token)
      setSuccess(`Imported ${parsed.length} questions into the selected bank.`)
      setQuestionFile(null)
    } catch (e) { setError(e instanceof Error ? e.message : 'Question import failed.') }
    finally { setBusy('') }
  }
  async function createExam(event: FormEvent) {
    event.preventDefault()
    if (!bankId) { setError('Create or select a question bank first.'); return }
    if (placeholderBankSelected) {
      setError('The placeholder testing bank cannot generate an official examination. Select Set A, Set B, or a complete question bank.')
      return
    }
    if (!districtId) { setError('Select the educational district for this examination.'); return }
    if (!window.confirm(`Create the ${questionCount}-question exam "${examTitle}" as a draft: 180 random questions from the selected set plus 20 anonymous questions?`)) return
    setBusy('create-exam'); setError(''); setSuccess('')
    try {
      const draft = await api<AdminExamRecord>('/admin/exams', { method: 'POST', body: JSON.stringify({
        title: examTitle, bankId, districtId, opensAt: new Date(opensAt).toISOString(), closesAt: new Date(closesAt).toISOString(),
        questionCount: Number(questionCount), anonymousAllowed: false,
      }) }, token)
      setSuccess(`Created ${draft.title} for ${draft.districtName}. Publish it to make the district schedule available to schools.`)
      await load()
    } catch (e) { setError(e instanceof Error ? e.message : 'Exam draft could not be created.') }
    finally { setBusy('') }
  }
  async function publishExam(exam: AdminExamRecord) {
    if (!window.confirm(`Publish ${exam.title}? Students may start once their district portal is open and its scheduled opening time has arrived.`)) return
    setBusy(exam.id); setError(''); setSuccess('')
    try {
      await api(`/admin/exams/${exam.id}/publish`, { method: 'POST' }, token)
      setSuccess(`${exam.title} is published.`)
      await load()
    } catch (e) { setError(e instanceof Error ? e.message : 'Exam could not be published.') }
    finally { setBusy('') }
  }
  const selectedBank = banks?.find(bank => bank.id === bankId)
  const placeholderBankSelected = selectedBank?.name === 'HSSC placeholder testing questions'
  return <main className="admin-page"><AdminSidebar active="Question banks"/><section className="admin-main"><AdminTopbar title="Question banks & exam setup"/><div className="admin-content">
    <div className="section-kicker">EXAM CONFIGURATION</div><h1>Questions & examinations</h1><p>Load the official Set A and Set B questions, import a question bank, or create a clearly identified placeholder bank for testing.</p>
    {error && <div className="form-error" role="alert">{error}</div>}{success && <div className="success-inline" role="status">{success}</div>}
    {placeholderBankSelected && <div className="empty-inline" role="status">The placeholder testing bank is selected. It cannot generate an official examination; select Set A, Set B, or a complete question bank instead.</div>}
    {!banks || !exams || !districts ? <div className="page-message"><span className="loader"/><p>Loading exam configuration…</p></div> : <>
      <section className="admin-exam-panel"><div className="section-kicker">OFFICIAL QUESTION SETS</div><h2>Bundled examination sets</h2><p>Select Set A or Set B directly in the question bank selector below; the matching bundled data is loaded into its own bank automatically. No bank is preselected. Examinations are not created unless the selected set has sufficient answer keys and 20 anonymous questions.</p></section>
      <section className="admin-exam-panel"><div className="admin-section-heading"><div><div className="section-kicker">TEST DATA</div><h2>Placeholder question bank</h2></div><button className="button button-outline" disabled={busy !== ''} onClick={() => void createPlaceholders()}>{busy === 'placeholders' ? 'Creating…' : 'Create 200 placeholder questions'}</button></div><p>This creates persistent non-official test questions for checking the exam workflow.</p></section>
      <section className="admin-exam-panel"><div className="section-kicker">QUESTION IMPORT</div><h2>Import questions</h2><div className="admin-result-filters"><label>Question bank<select value={bankId} onChange={event => selectBank(event.target.value)}><option value="">Select a bank</option><option value="official:A">Set A · bundled bank</option><option value="official:B">Set B · bundled bank</option>{banks.map(bank => <option key={bank.id} value={bank.id}>{bank.name}{bank.active ? '' : ' · inactive'}</option>)}</select></label><label>JSON question file<input type="file" accept="application/json,.json" onChange={event => setQuestionFile(event.target.files?.[0] || null)}/></label><button className="button button-dark" disabled={busy !== '' || !bankId || !questionFile} onClick={() => void importQuestions()}>{busy === 'import' ? 'Importing…' : 'Import questions'}</button></div><small className="admin-help-text">Expected JSON: an array of question objects with prompt, subject, points, options, correctOption when keyed, and anonymous:true for an anonymous item without a key.</small></section>
      <section className="admin-exam-panel"><div className="section-kicker">CREATE EXAM</div><h2>Prepare an examination</h2><form className="admin-exam-form" onSubmit={event => void createExam(event)}><label>Examination title<input required maxLength={160} value={examTitle} onChange={event => setExamTitle(event.target.value)}/></label><label>Question bank<select required value={bankId} onChange={event => selectBank(event.target.value)}><option value="">Select a question bank</option><option value="official:A">Set A · bundled bank</option><option value="official:B">Set B · bundled bank</option>{banks.filter(bank => bank.active).map(bank => <option key={bank.id} value={bank.id}>{bank.name}</option>)}</select></label><label>Educational district<select required value={districtId} onChange={event => setDistrictId(event.target.value)}><option value="">Select a district</option>{districts.map(district => <option key={district.id} value={district.id}>{district.name}</option>)}</select></label><label>Question count<input type="number" value={questionCount} readOnly aria-readonly="true"/></label><label>Opens at<input type="datetime-local" required value={opensAt} onChange={event => setOpensAt(event.target.value)}/></label><label>Closes at<input type="datetime-local" required value={closesAt} onChange={event => setClosesAt(event.target.value)}/></label><button className="button button-dark" disabled={busy !== '' || !districtId || !bankId}>{busy === 'create-exam' ? 'Creating…' : 'Create draft examination'}</button></form><p className="admin-help-text">Each candidate receives 180 random, answer-keyed questions from the selected set plus 20 marked anonymous questions, for 200 total. The district and opening/closing times are stored on the examination. Publish the draft to make its schedule visible to schools in that district.</p></section>
      <section className="admin-exam-panel"><div className="section-kicker">SAVED EXAMS</div><h2>Examination schedule</h2>{exams.length ? <div className="admin-exam-list">{exams.map(exam => {
        const placeholder = exam.bankName === 'HSSC placeholder testing questions'
        return <article key={exam.id}><div><b>{exam.title}</b><span>{exam.bankName} · {exam.questionCount} questions · {exam.durationMinutes} minutes · {exam.districtName || 'District not assigned'} · opens {new Date(exam.opensAt).toLocaleString()} · closes {new Date(exam.closesAt).toLocaleString()}</span>{placeholder && <span className="form-error">NON-OFFICIAL PLACEHOLDER: this exam is not visible or available to schools. Create and publish a schedule using Set A or Set B.</span>}</div><span className={`status-pill ${exam.status === 'OPEN' && !placeholder ? 'status-open' : ''}`}>{placeholder ? 'NOT AVAILABLE TO SCHOOLS' : exam.status}</span>{exam.status === 'DRAFT' && !placeholder && <button className="button button-outline" disabled={busy !== ''} onClick={() => void publishExam(exam)}>{busy === exam.id ? 'Publishing…' : 'Publish exam'}</button>}</article>
      })}</div> : <div className="empty-inline">No examination has been configured yet.</div>}</section>
    </>}
  </div></section></main>
}

function App() {
  const location = useLocation()
  const standalone = location.pathname === '/exam' || location.pathname.startsWith('/admin')
  return <>{!standalone && <Header/>}<Routes>
    <Route path="/" element={<Landing/>}/><Route path="/about" element={<AboutPage/>}/><Route path="/policy" element={<PolicyPage/>}/><Route path="/sponsors" element={<SponsorsPage/>}/>
    <Route path="/sign-in" element={<SignInPage/>}/><Route path="/register-school" element={<SchoolRegistrationPage/>}/><Route path="/register-student" element={<RegisterStudentPage/>}/>
    <Route path="/admin/login" element={<AdminSignInPage/>}/>
    <Route path="/dashboard" element={<ProtectedRoute roles={['STUDENT']}><Dashboard/></ProtectedRoute>}/>
    <Route path="/school/dashboard" element={<ProtectedRoute roles={['SCHOOL']}><SchoolDashboardPage/></ProtectedRoute>}/>
    <Route path="/school/students" element={<ProtectedRoute roles={['SCHOOL']}><SchoolDashboardPage focus="participants"/></ProtectedRoute>}/>
    <Route path="/school/profile" element={<ProtectedRoute roles={['SCHOOL']}><SchoolDashboardPage focus="profile"/></ProtectedRoute>}/>
    <Route path="/exam" element={<ProtectedRoute roles={['SCHOOL','STUDENT']}><ExamPage/></ProtectedRoute>}/>
    <Route path="/results" element={<ProtectedRoute roles={['SCHOOL','STUDENT']}><ResultsPage/></ProtectedRoute>}/>
    <Route path="/leaderboard" element={<ProtectedRoute roles={['SCHOOL','STUDENT']}><LeaderboardPage/></ProtectedRoute>}/>
    <Route path="/admin" element={<ProtectedRoute roles={['ADMIN']}><AdminPage/></ProtectedRoute>}/>
    <Route path="/admin/registrations/*" element={<ProtectedRoute roles={['ADMIN']}><AdminRegistrationsPage/></ProtectedRoute>}/>
    <Route path="/admin/results/*" element={<ProtectedRoute roles={['ADMIN']}><AdminResultsPage/></ProtectedRoute>}/>
    <Route path="/admin/leaderboard" element={<ProtectedRoute roles={['ADMIN']}><AdminLeaderboardPage/></ProtectedRoute>}/>
    <Route path="/admin/question-banks" element={<ProtectedRoute roles={['ADMIN']}><AdminQuestionBanksPage/></ProtectedRoute>}/>
    <Route path="/admin/notifications" element={<ProtectedRoute roles={['ADMIN']}><AdminNotificationsPage/></ProtectedRoute>}/>
    <Route path="/admin/settings" element={<ProtectedRoute roles={['ADMIN']}><AdminFeature title="Competition settings" endpoint="/admin/settings"/></ProtectedRoute>}/>
    <Route path="*" element={<PageState title="Page not found" message="That page doesn't exist. Head back to the HSSC home page."/>}/>
  </Routes>{!standalone && <Footer/>}<SponsorTicker/>{['/', '/results', '/leaderboard'].includes(location.pathname) && <PartnerAdvertisement key={location.pathname}/>}</>
}

export default App
