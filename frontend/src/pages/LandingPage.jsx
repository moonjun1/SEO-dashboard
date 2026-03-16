import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { publicGet, publicPost } from '../api/client';
import { getScoreColor } from '../components/ScoreCard';
import PublicNav from '../components/PublicNav';

export default function LandingPage() {
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [ranking, setRanking] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    publicGet('/public/ranking')
      .then((res) => {
        const list = res.data?.content || res.data || [];
        setRanking(list.slice(0, 20));
      })
      .catch(() => {});
  }, []);

  const normalizeUrl = (input) => {
    let trimmed = input.trim();
    if (!trimmed) return '';
    if (!/^https?:\/\//i.test(trimmed)) {
      trimmed = 'https://' + trimmed;
    }
    return trimmed;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const normalized = normalizeUrl(url);
    if (!normalized) return;
    setError('');
    setLoading(true);
    try {
      const res = await publicPost('/public/analyze', { url: normalized });
      const id = res.data?.id || res.data?.analysisId || res.id;
      navigate(`/result/${id}`);
    } catch (err) {
      setError(err.message || '분석 중 오류가 발생했습니다.');
      setLoading(false);
    }
  };

  const features = [
    {
      icon: (
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>
        </svg>
      ),
      title: '콘텐츠 분석',
      desc: '타이틀, 메타태그, 헤딩 구조를 분석합니다.',
    },
    {
      icon: (
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#22c55e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/>
        </svg>
      ),
      title: '메타 태그',
      desc: 'OG, Twitter Card, viewport 등을 검사합니다.',
    },
    {
      icon: (
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#eab308" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
        </svg>
      ),
      title: '링크 분석',
      desc: '내부/외부 링크, 깨진 링크를 확인합니다.',
    },
    {
      icon: (
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#a855f7" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
        </svg>
      ),
      title: '속도 테스트',
      desc: '응답 시간, 페이지 크기를 측정합니다.',
    },
    {
      icon: (
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#ef4444" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-4"/>
        </svg>
      ),
      title: 'SEO 점수',
      desc: '6가지 항목을 종합하여 점수를 산출합니다.',
    },
    {
      icon: (
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#06b6d4" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>
        </svg>
      ),
      title: '개선 제안',
      desc: '구체적인 SEO 개선 방안을 제공합니다.',
    },
  ];

  if (loading) {
    return (
      <div className="landing-page">
        <PublicNav />
        <div className="analyzing-overlay">
          <div className="analyzing-content">
            <div className="analyzing-spinner">
              <div className="spinner" style={{ width: 56, height: 56 }} />
            </div>
            <h2 className="analyzing-title">SEO 분석 중...</h2>
            <p className="analyzing-desc">SEO 분석 중입니다. 잠시만 기다려주세요...</p>
            <p className="analyzing-url">{normalizeUrl(url)}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="landing-page">
      <PublicNav />

      <section className="hero-section">
        <h1 className="hero-title">SEO 진단 검사기</h1>
        <p className="hero-subtitle">
          URL을 입력하면 웹사이트의 SEO 상태를 즉시 분석합니다
        </p>
        <form className="url-input-form" onSubmit={handleSubmit}>
          <div className="url-input-wrapper">
            <svg className="url-input-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10"/><path d="M2 12h20"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
            </svg>
            <input
              type="text"
              className="url-input"
              placeholder="https://example.com"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              required
              aria-label="분석할 웹사이트 URL"
            />
            <button type="submit" className="btn btn-primary url-submit-btn">
              분석하기
            </button>
          </div>
        </form>
        {error && <div className="form-error" style={{ marginTop: 12, maxWidth: 640, margin: '12px auto 0' }}>{error}</div>}
      </section>

      <section className="features-section">
        <div className="feature-grid">
          {features.map((f, i) => (
            <div className="feature-card" key={i}>
              <div className="feature-icon">{f.icon}</div>
              <h3 className="feature-title">{f.title}</h3>
              <p className="feature-desc">{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {ranking.length > 0 && (
        <section className="ranking-section">
          <h2 className="section-title" style={{ textAlign: 'center', marginBottom: 24 }}>
            최근 분석 순위
          </h2>
          <div className="ranking-table-wrapper">
            <table className="table ranking-table">
              <thead>
                <tr>
                  <th style={{ width: 60 }}>순위</th>
                  <th>도메인</th>
                  <th style={{ width: 200 }}>점수</th>
                  <th style={{ width: 80, textAlign: 'center' }}>점수</th>
                  <th style={{ width: 80 }}></th>
                </tr>
              </thead>
              <tbody>
                {ranking.map((item, i) => {
                  const score = item.score ?? item.totalScore ?? item.seoScore ?? 0;
                  const domain = item.domain || item.url || '';
                  const id = item.id || item.analysisId;
                  return (
                    <tr key={id || i}>
                      <td style={{ fontWeight: 700, color: i < 3 ? '#3b82f6' : 'var(--text-secondary)' }}>
                        {i + 1}
                      </td>
                      <td>
                        <span className="ranking-domain">{domain}</span>
                      </td>
                      <td>
                        <div className="ranking-bar-bg">
                          <div
                            className="ranking-bar-fill"
                            style={{
                              width: `${Math.min(score, 100)}%`,
                              background: getScoreColor(score),
                            }}
                          />
                        </div>
                      </td>
                      <td style={{ textAlign: 'center', fontWeight: 700, color: getScoreColor(score) }}>
                        {score}
                      </td>
                      <td>
                        {id && (
                          <Link to={`/result/${id}`} className="btn btn-sm btn-secondary">
                            보기
                          </Link>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </section>
      )}

      <footer className="landing-footer">
        <p>SEO 진단 검사기 - 무료 웹사이트 SEO 분석 도구</p>
      </footer>
    </div>
  );
}
