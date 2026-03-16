import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { publicGet } from '../api/client';
import { getScoreColor } from '../components/ScoreCard';
import PublicNav from '../components/PublicNav';
import LoadingSpinner from '../components/LoadingSpinner';

export default function RankingPage() {
  const [ranking, setRanking] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    publicGet('/public/ranking')
      .then((res) => {
        const list = res.data?.content || res.data || [];
        setRanking(list.slice(0, 50));
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="landing-page">
      <PublicNav />
      <div className="result-page">
        <h1 className="page-title" style={{ textAlign: 'center', marginBottom: 32 }}>
          SEO 점수 순위표
        </h1>

        {error && <div className="alert alert-error">{error}</div>}

        {loading ? (
          <LoadingSpinner size={48} text="순위 데이터를 불러오는 중..." />
        ) : ranking.length === 0 ? (
          <div className="empty-state">
            <p>아직 분석된 사이트가 없습니다.</p>
          </div>
        ) : (
          <div className="ranking-table-wrapper">
            <table className="table ranking-table">
              <thead>
                <tr>
                  <th style={{ width: 60 }}>순위</th>
                  <th>도메인</th>
                  <th style={{ width: 200 }}>점수</th>
                  <th style={{ width: 80, textAlign: 'center' }}>점수</th>
                  <th style={{ width: 140 }}>분석일</th>
                  <th style={{ width: 100 }}></th>
                </tr>
              </thead>
              <tbody>
                {ranking.map((item, i) => {
                  const score = item.score ?? item.totalScore ?? item.seoScore ?? 0;
                  const domain = item.domain || item.url || '';
                  const id = item.id || item.analysisId;
                  const date = item.analyzedAt || item.created_at || item.createdAt;
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
                      <td style={{ fontSize: 13, color: 'var(--text-muted)' }}>
                        {date ? new Date(date).toLocaleDateString('ko-KR') : '-'}
                      </td>
                      <td>
                        {id && (
                          <Link to={`/result/${id}`} className="btn btn-sm btn-secondary">
                            상세 보기
                          </Link>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        <div style={{ textAlign: 'center', marginTop: 32, marginBottom: 48 }}>
          <Link to="/" className="btn btn-primary">
            사이트 분석하기
          </Link>
        </div>
      </div>
    </div>
  );
}
