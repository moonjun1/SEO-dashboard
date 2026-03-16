export function getScoreColor(score) {
  if (score == null) return '#64748b';
  if (score <= 40) return '#ef4444';
  if (score <= 70) return '#eab308';
  return '#22c55e';
}

export default function ScoreCard({ label, value, icon, subtitle }) {
  return (
    <div className="stat-card">
      <div className="stat-card-header">
        <span className="stat-card-icon">{icon}</span>
        <span className="stat-card-label">{label}</span>
      </div>
      <div className="stat-card-value">{value ?? '-'}</div>
      {subtitle && <div className="stat-card-subtitle">{subtitle}</div>}
    </div>
  );
}

export function ScoreGauge({ score, size = 120 }) {
  const color = getScoreColor(score);
  const radius = (size - 12) / 2;
  const circumference = 2 * Math.PI * radius;
  const progress = score != null ? (score / 100) * circumference : 0;
  const center = size / 2;

  return (
    <div className="score-gauge" style={{ width: size, height: size }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          stroke="#334155"
          strokeWidth="8"
        />
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          stroke={color}
          strokeWidth="8"
          strokeDasharray={circumference}
          strokeDashoffset={circumference - progress}
          strokeLinecap="round"
          transform={`rotate(-90 ${center} ${center})`}
          style={{ transition: 'stroke-dashoffset 0.6s ease' }}
        />
      </svg>
      <div className="score-gauge-text" style={{ color }}>
        {score != null ? score : '-'}
      </div>
    </div>
  );
}
