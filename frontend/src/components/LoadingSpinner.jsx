export default function LoadingSpinner({ size = 40, text = '' }) {
  return (
    <div className="loading-container">
      <div
        className="spinner"
        style={{ width: size, height: size }}
      />
      {text && <p className="loading-text">{text}</p>}
    </div>
  );
}
