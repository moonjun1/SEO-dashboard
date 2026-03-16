import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { publicGet } from '../api/client';
import { ScoreGauge, getScoreColor } from '../components/ScoreCard';
import PublicNav from '../components/PublicNav';
import LoadingSpinner from '../components/LoadingSpinner';

function ScoreMiniCard({ label, score }) {
  const color = getScoreColor(score);
  return (
    <div className="score-mini-card">
      <div className="score-mini-value" style={{ color }}>{score ?? '-'}</div>
      <div className="score-mini-bar">
        <div
          className="score-mini-bar-fill"
          style={{ width: `${Math.min(score ?? 0, 100)}%`, background: color }}
        />
      </div>
      <div className="score-mini-label">{label}</div>
    </div>
  );
}

function CheckItem({ label, passed }) {
  return (
    <div className="check-item">
      <span className={`check-icon ${passed ? 'check-pass' : 'check-fail'}`}>
        {passed ? '\u2705' : '\u274C'}
      </span>
      <span className="check-label">{label}</span>
    </div>
  );
}

function MetaField({ label, value, length }) {
  return (
    <div className="meta-field">
      <label>{label}</label>
      <div className="meta-code">{value || '-'}</div>
      {length != null && (
        <span className="meta-length">{length}자</span>
      )}
    </div>
  );
}

export default function PublicResultPage() {
  const { id } = useParams();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    publicGet(`/public/analyze/${id}`)
      .then((res) => setData(res.data || res))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="landing-page">
        <PublicNav />
        <div style={{ paddingTop: 120 }}>
          <LoadingSpinner size={48} text="분석 결과를 불러오는 중..." />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="landing-page">
        <PublicNav />
        <div className="result-page">
          <div className="alert alert-error">{error}</div>
          <Link to="/" className="btn btn-primary" style={{ marginTop: 16 }}>
            돌아가기
          </Link>
        </div>
      </div>
    );
  }

  if (!data) return null;

  const totalScore = data.totalScore ?? data.score ?? data.seoScore ?? 0;
  const scores = data.scores || {};
  const meta = data.meta || {};
  const checklist = data.checklist || {};
  const headings = data.headings || {};
  const links = data.links || {};
  const performance = data.performance || {};
  const issues = data.issues || [];
  const metaTags = data.metaTags || data.meta_tags || [];
  const analyzedAt = data.analyzedAt || data.created_at || data.createdAt;

  const scoreItems = [
    { label: '타이틀', score: scores.title ?? scores.titleScore },
    { label: '메타 디스크립션', score: scores.description ?? scores.descriptionScore ?? scores.metaDescription },
    { label: '헤딩', score: scores.heading ?? scores.headingScore ?? scores.headings },
    { label: '이미지', score: scores.image ?? scores.imageScore ?? scores.images },
    { label: '링크', score: scores.link ?? scores.linkScore ?? scores.links },
    { label: '성능', score: scores.performance ?? scores.performanceScore ?? scores.speed },
  ];

  const headingEntries = [
    { tag: 'H1', count: headings.h1 ?? 0 },
    { tag: 'H2', count: headings.h2 ?? 0 },
    { tag: 'H3', count: headings.h3 ?? 0 },
    { tag: 'H4', count: headings.h4 ?? 0 },
    { tag: 'H5', count: headings.h5 ?? 0 },
    { tag: 'H6', count: headings.h6 ?? 0 },
  ];
  const maxHeading = Math.max(...headingEntries.map((h) => h.count), 1);

  const linkList = links.list || links.items || [];
  const internalCount = links.internal ?? links.internalCount ?? 0;
  const externalCount = links.external ?? links.externalCount ?? 0;
  const brokenCount = links.broken ?? links.brokenCount ?? 0;

  return (
    <div className="landing-page">
      <PublicNav />

      <div className="result-page">
        <div className="result-header card">
          <div className="result-header-info">
            <h1 className="result-url">{data.url || data.domain || '-'}</h1>
            {analyzedAt && (
              <p className="text-muted">
                분석 일시: {new Date(analyzedAt).toLocaleString('ko-KR')}
              </p>
            )}
          </div>
          <div className="result-header-gauge">
            <ScoreGauge score={totalScore} size={140} />
            <span className="result-total-label">종합 점수</span>
          </div>
        </div>

        {/* Score cards */}
        <div className="score-grid">
          {scoreItems.map((s, i) => (
            <ScoreMiniCard key={i} label={s.label} score={s.score} />
          ))}
        </div>

        {/* Meta information */}
        <section className="card result-section">
          <h2 className="section-title">메타 정보</h2>
          <div className="meta-result">
            <MetaField
              label="페이지 타이틀"
              value={meta.title || data.title}
              length={meta.titleLength ?? (meta.title || data.title || '').length}
            />
            <MetaField
              label="메타 디스크립션"
              value={meta.description || data.description}
              length={meta.descriptionLength ?? (meta.description || data.description || '').length}
            />
            <MetaField
              label="Canonical URL"
              value={meta.canonical || data.canonical}
            />
          </div>

          {metaTags.length > 0 && (
            <>
              <h3 style={{ fontSize: 14, color: 'var(--text-secondary)', marginTop: 20, marginBottom: 8 }}>
                모든 메타태그
              </h3>
              <div style={{ overflowX: 'auto' }}>
                <table className="table meta-tags-table">
                  <thead>
                    <tr>
                      <th>Name / Property</th>
                      <th>Content</th>
                    </tr>
                  </thead>
                  <tbody>
                    {metaTags.map((tag, i) => (
                      <tr key={i}>
                        <td style={{ fontFamily: 'monospace', fontSize: 13 }}>
                          {tag.name || tag.property || tag.httpEquiv || '-'}
                        </td>
                        <td style={{ wordBreak: 'break-all', fontSize: 13 }}>
                          {tag.content || '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </section>

        {/* Checklist */}
        <section className="card result-section">
          <h2 className="section-title">체크리스트</h2>
          <div className="checklist-grid">
            <CheckItem label="HTTPS" passed={checklist.https ?? checklist.isHttps} />
            <CheckItem label="OG 태그" passed={checklist.ogTags ?? checklist.og ?? checklist.openGraph} />
            <CheckItem label="Twitter Card" passed={checklist.twitterCard ?? checklist.twitter} />
            <CheckItem label="Viewport (모바일)" passed={checklist.viewport ?? checklist.mobileViewport} />
            <CheckItem label="Favicon" passed={checklist.favicon} />
            <CheckItem label="Robots.txt" passed={checklist.robotsTxt ?? checklist.robots} />
            <CheckItem label="Sitemap.xml" passed={checklist.sitemap ?? checklist.sitemapXml} />
          </div>
        </section>

        {/* Headings */}
        <section className="card result-section">
          <h2 className="section-title">헤딩 구조</h2>
          <div className="heading-chart">
            {headingEntries.map((h) => (
              <div className="heading-row" key={h.tag}>
                <span className="heading-tag">{h.tag}</span>
                <div className="heading-bar-bg">
                  <div
                    className="heading-bar-fill"
                    style={{ width: `${(h.count / maxHeading) * 100}%` }}
                  />
                </div>
                <span className="heading-count">{h.count}</span>
              </div>
            ))}
          </div>
        </section>

        {/* Links */}
        <section className="card result-section">
          <h2 className="section-title">링크 분석</h2>
          <div className="link-stats">
            <div className="link-stat-card">
              <div className="link-stat-value">{internalCount}</div>
              <div className="link-stat-label">내부 링크</div>
            </div>
            <div className="link-stat-card">
              <div className="link-stat-value">{externalCount}</div>
              <div className="link-stat-label">외부 링크</div>
            </div>
            <div className="link-stat-card" style={{ borderColor: brokenCount > 0 ? 'var(--danger)' : undefined }}>
              <div className="link-stat-value" style={{ color: brokenCount > 0 ? 'var(--danger)' : undefined }}>
                {brokenCount}
              </div>
              <div className="link-stat-label">깨진 링크</div>
            </div>
          </div>

          {linkList.length > 0 && (
            <div style={{ overflowX: 'auto', marginTop: 16 }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>URL</th>
                    <th>텍스트</th>
                    <th style={{ width: 80 }}>유형</th>
                  </tr>
                </thead>
                <tbody>
                  {linkList.slice(0, 50).map((link, i) => (
                    <tr key={i}>
                      <td className="url-cell" style={{ maxWidth: 300 }}>
                        {link.url || link.href || '-'}
                      </td>
                      <td>{link.text || link.anchorText || '-'}</td>
                      <td>
                        <span className={`status-badge ${link.type === 'internal' || link.internal ? 'status-completed' : 'status-pending'}`}>
                          {link.type === 'internal' || link.internal ? '내부' : '외부'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* Performance */}
        <section className="card result-section">
          <h2 className="section-title">성능</h2>
          <div className="perf-stats">
            <div className="perf-stat">
              <span className="perf-stat-label">응답 시간</span>
              <span className="perf-stat-value">
                {performance.responseTime ?? performance.response_time ?? '-'} ms
              </span>
            </div>
            <div className="perf-stat">
              <span className="perf-stat-label">페이지 크기</span>
              <span className="perf-stat-value">
                {performance.pageSize ?? performance.page_size
                  ? `${Math.round((performance.pageSize ?? performance.page_size) / 1024)} KB`
                  : '-'}
              </span>
            </div>
          </div>
        </section>

        {/* Issues */}
        {issues.length > 0 && (
          <section className="card result-section">
            <h2 className="section-title">이슈 및 개선사항</h2>
            <ul className="issue-list">
              {issues.map((issue, i) => {
                const severity = (issue.severity || issue.level || 'info').toLowerCase();
                let severityClass = 'severity-info';
                let severityLabel = '정보';
                if (severity === 'error' || severity === 'critical' || severity === 'high') {
                  severityClass = 'severity-critical';
                  severityLabel = '오류';
                } else if (severity === 'warning' || severity === 'medium') {
                  severityClass = 'severity-warning';
                  severityLabel = '경고';
                }
                return (
                  <li key={i} className="issue-item">
                    <span className={`issue-severity ${severityClass}`}>{severityLabel}</span>
                    <span>{issue.message || issue.description || issue.text}</span>
                  </li>
                );
              })}
            </ul>
          </section>
        )}

        <div style={{ textAlign: 'center', marginTop: 32, marginBottom: 48 }}>
          <Link to="/" className="btn btn-primary">
            다른 사이트 분석하기
          </Link>
        </div>
      </div>
    </div>
  );
}
