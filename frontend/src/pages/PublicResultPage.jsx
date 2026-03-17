import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { publicGet } from '../api/client';
import { ScoreGauge, getScoreColor } from '../components/ScoreCard';
import PublicNav from '../components/PublicNav';
import LoadingSpinner from '../components/LoadingSpinner';

function ScoreMiniCard({ label, score }) {
  const numScore = typeof score === 'number' ? Math.round(score) : null;
  const color = getScoreColor(numScore);
  return (
    <div className="score-mini-card">
      <div className="score-mini-value" style={{ color }}>{numScore ?? '-'}</div>
      <div className="score-mini-bar">
        <div className="score-mini-bar-fill"
          style={{ width: `${Math.min(numScore ?? 0, 100)}%`, background: color }} />
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
      {length != null && <span className="meta-length">{length}자</span>}
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
      <div className="landing-page"><PublicNav />
        <div style={{ paddingTop: 120 }}><LoadingSpinner size={48} text="분석 결과를 불러오는 중..." /></div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="landing-page"><PublicNav />
        <div className="result-page">
          <div className="alert alert-error" role="alert">{error || '데이터를 찾을 수 없습니다'}</div>
          <Link to="/" className="btn btn-primary" style={{ marginTop: 16 }}>돌아가기</Link>
        </div>
      </div>
    );
  }

  // 플랫 구조에서 직접 매핑
  const totalScore = Math.round(data.seoScore ?? 0);
  const issues = Array.isArray(data.issues) ? data.issues : [];
  const metaTags = Array.isArray(data.metaTags) ? data.metaTags : [];
  const linkList = Array.isArray(data.linkList) ? data.linkList : [];
  const headingStructure = Array.isArray(data.headingStructure) ? data.headingStructure : [];

  // 헤딩 카운트 계산
  const headingCounts = { h1: 0, h2: 0, h3: 0, h4: 0, h5: 0, h6: 0 };
  headingStructure.forEach((h) => {
    const level = (h.level || '').toLowerCase();
    if (headingCounts[level] !== undefined) headingCounts[level]++;
  });
  const headingEntries = [
    { tag: 'H1', count: headingCounts.h1 },
    { tag: 'H2', count: headingCounts.h2 },
    { tag: 'H3', count: headingCounts.h3 },
    { tag: 'H4', count: headingCounts.h4 },
    { tag: 'H5', count: headingCounts.h5 },
    { tag: 'H6', count: headingCounts.h6 },
  ];
  const maxHeading = Math.max(...headingEntries.map((h) => h.count), 1);

  return (
    <div className="landing-page">
      <PublicNav />
      <div className="result-page">

        {/* 헤더: URL + 종합 점수 */}
        <div className="result-header card">
          <div className="result-header-info">
            <h1 className="result-url">{data.url || data.domain}</h1>
            {data.createdAt && (
              <p className="text-muted">분석 일시: {new Date(data.createdAt).toLocaleString('ko-KR')}</p>
            )}
          </div>
          <div className="result-header-gauge">
            <ScoreGauge score={totalScore} size={140} />
            <span className="result-total-label">종합 점수</span>
          </div>
        </div>

        {/* 6개 카테고리별 점수 */}
        <div className="score-grid">
          <ScoreMiniCard label="타이틀" score={data.titleScore} />
          <ScoreMiniCard label="메타 디스크립션" score={data.metaDescriptionScore} />
          <ScoreMiniCard label="헤딩" score={data.headingScore} />
          <ScoreMiniCard label="이미지" score={data.imageScore} />
          <ScoreMiniCard label="링크" score={data.linkScore} />
          <ScoreMiniCard label="성능" score={data.performanceScore} />
        </div>

        {/* 메타 정보 */}
        <section className="card result-section">
          <h2 className="section-title">메타 정보</h2>
          <div className="meta-result">
            <MetaField label="페이지 타이틀" value={data.title} length={(data.title || '').length} />
            <MetaField label="메타 디스크립션" value={data.metaDescription} length={(data.metaDescription || '').length} />
            <MetaField label="Canonical URL" value={data.canonicalUrl} />
          </div>
          {metaTags.length > 0 && (
            <>
              <h3 style={{ fontSize: 14, color: 'var(--text-secondary)', marginTop: 20, marginBottom: 8 }}>모든 메타태그</h3>
              <div style={{ overflowX: 'auto' }}>
                <table className="table meta-tags-table">
                  <thead><tr><th>Name / Property</th><th>Content</th></tr></thead>
                  <tbody>
                    {metaTags.map((tag, i) => (
                      <tr key={i}>
                        <td style={{ fontFamily: 'monospace', fontSize: 13 }}>{tag.name || tag.property || tag.httpEquiv || '-'}</td>
                        <td style={{ wordBreak: 'break-all', fontSize: 13 }}>{tag.content || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </section>

        {/* 체크리스트 */}
        <section className="card result-section">
          <h2 className="section-title">체크리스트</h2>
          <div className="checklist-grid">
            <CheckItem label="HTTPS" passed={data.hasHttps} />
            <CheckItem label="OG 태그" passed={data.hasOgTags} />
            <CheckItem label="Twitter Card" passed={data.hasTwitterCards} />
            <CheckItem label="Viewport (모바일)" passed={data.hasViewport} />
            <CheckItem label="Favicon" passed={data.hasFavicon} />
            <CheckItem label="Robots.txt" passed={data.hasRobotsTxt} />
            <CheckItem label="Sitemap.xml" passed={data.hasSitemap} />
          </div>
        </section>

        {/* 헤딩 구조 */}
        <section className="card result-section">
          <h2 className="section-title">헤딩 구조 (총 {data.totalHeadings ?? 0}개)</h2>
          <div className="heading-chart">
            {headingEntries.map((h) => (
              <div className="heading-row" key={h.tag}>
                <span className="heading-tag">{h.tag}</span>
                <div className="heading-bar-bg">
                  <div className="heading-bar-fill" style={{ width: `${(h.count / maxHeading) * 100}%` }} />
                </div>
                <span className="heading-count">{h.count}</span>
              </div>
            ))}
          </div>
        </section>

        {/* 링크 분석 */}
        <section className="card result-section">
          <h2 className="section-title">링크 분석</h2>
          <div className="link-stats">
            <div className="link-stat-card">
              <div className="link-stat-value">{data.internalLinks ?? 0}</div>
              <div className="link-stat-label">내부 링크</div>
            </div>
            <div className="link-stat-card">
              <div className="link-stat-value">{data.externalLinks ?? 0}</div>
              <div className="link-stat-label">외부 링크</div>
            </div>
            <div className="link-stat-card" style={{ borderColor: (data.brokenLinks ?? 0) > 0 ? 'var(--danger)' : undefined }}>
              <div className="link-stat-value" style={{ color: (data.brokenLinks ?? 0) > 0 ? 'var(--danger)' : undefined }}>
                {data.brokenLinks ?? 0}
              </div>
              <div className="link-stat-label">깨진 링크</div>
            </div>
          </div>
          {linkList.length > 0 && (
            <div style={{ overflowX: 'auto', marginTop: 16 }}>
              <table className="table">
                <thead><tr><th>URL</th><th>텍스트</th><th style={{ width: 80 }}>유형</th></tr></thead>
                <tbody>
                  {linkList.slice(0, 50).map((link, i) => (
                    <tr key={i}>
                      <td className="url-cell" style={{ maxWidth: 300 }}>{link.href || '-'}</td>
                      <td>{link.text || '-'}</td>
                      <td>
                        <span className={`status-badge ${link.internal ? 'status-completed' : 'status-pending'}`}>
                          {link.internal ? '내부' : '외부'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* 성능 */}
        <section className="card result-section">
          <h2 className="section-title">성능</h2>
          <div className="perf-stats">
            <div className="perf-stat">
              <span className="perf-stat-label">응답 시간</span>
              <span className="perf-stat-value">{data.responseTimeMs ?? '-'} ms</span>
            </div>
            <div className="perf-stat">
              <span className="perf-stat-label">페이지 크기</span>
              <span className="perf-stat-value">
                {data.contentLength ? `${Math.round(data.contentLength / 1024)} KB` : '-'}
              </span>
            </div>
            <div className="perf-stat">
              <span className="perf-stat-label">이미지</span>
              <span className="perf-stat-value">
                {data.totalImages ?? 0}개 (alt 없음: {data.imagesWithoutAlt ?? 0}개)
              </span>
            </div>
          </div>
        </section>

        {/* 이슈 */}
        {issues.length > 0 && (
          <section className="card result-section">
            <h2 className="section-title">이슈 및 개선사항 ({issues.length}건)</h2>
            <ul className="issue-list">
              {issues.map((issue, i) => {
                const sev = (issue.severity || 'info').toLowerCase();
                let cls = 'severity-info', label = '정보';
                if (sev === 'error' || sev === 'critical') { cls = 'severity-critical'; label = '오류'; }
                else if (sev === 'warning') { cls = 'severity-warning'; label = '경고'; }
                return (
                  <li key={i} className="issue-item">
                    <span className={`issue-severity ${cls}`}>{label}</span>
                    <span>{issue.message}</span>
                  </li>
                );
              })}
            </ul>
          </section>
        )}

        <div style={{ textAlign: 'center', marginTop: 32, marginBottom: 48 }}>
          <Link to="/" className="btn btn-primary">다른 사이트 분석하기</Link>
        </div>
      </div>
    </div>
  );
}
