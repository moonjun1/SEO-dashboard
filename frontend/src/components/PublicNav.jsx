import { Link, useLocation } from 'react-router-dom';

export default function PublicNav() {
  const location = useLocation();

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="public-nav" role="navigation" aria-label="공개 페이지 내비게이션">
      <div className="public-nav-inner">
        <Link to="/" className="public-nav-brand" aria-label="홈으로 이동">
          <div className="sidebar-logo" style={{ width: 32, height: 32, fontSize: 16 }}>S</div>
          <span className="public-nav-title">SEO 진단 검사기</span>
        </Link>
        <div className="public-nav-links">
          <Link
            to="/"
            className={`public-nav-link ${isActive('/') ? 'active' : ''}`}
          >
            홈
          </Link>
          <Link
            to="/ranking"
            className={`public-nav-link ${isActive('/ranking') ? 'active' : ''}`}
          >
            순위
          </Link>
          <Link to="/login" className="btn btn-sm btn-primary">
            로그인
          </Link>
        </div>
      </div>
    </nav>
  );
}
