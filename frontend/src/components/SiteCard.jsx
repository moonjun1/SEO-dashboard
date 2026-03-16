import { useNavigate } from 'react-router-dom';
import { ScoreGauge } from './ScoreCard';

export default function SiteCard({ site }) {
  const navigate = useNavigate();

  return (
    <div className="site-card" onClick={() => navigate(`/sites/${site.id}`)}>
      <div className="site-card-header">
        <div>
          <h3 className="site-card-name">{site.name}</h3>
          <p className="site-card-url">{site.url}</p>
        </div>
        <ScoreGauge score={site.seoScore ?? site.seo_score} size={72} />
      </div>
      <div className="site-card-stats">
        <div className="site-card-stat">
          <span className="site-card-stat-label">Keywords</span>
          <span className="site-card-stat-value">{site.keywordCount ?? site.keyword_count ?? 0}</span>
        </div>
        <div className="site-card-stat">
          <span className="site-card-stat-label">Pages</span>
          <span className="site-card-stat-value">{site.pageCount ?? site.page_count ?? 0}</span>
        </div>
        <div className="site-card-stat">
          <span className="site-card-stat-label">Last Crawl</span>
          <span className="site-card-stat-value">
            {(site.lastCrawledAt || site.last_crawled_at)
              ? new Date(site.lastCrawledAt || site.last_crawled_at).toLocaleDateString('ko-KR')
              : '-'}
          </span>
        </div>
      </div>
    </div>
  );
}
