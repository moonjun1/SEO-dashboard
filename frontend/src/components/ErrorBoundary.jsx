import { Component } from 'react';

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  handleReload = () => {
    this.setState({ hasError: false, error: null });
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-boundary" role="alert">
          <div className="error-boundary-content">
            <h2>문제가 발생했습니다</h2>
            <p>페이지를 불러오는 중 오류가 발생했습니다. 다시 시도해주세요.</p>
            <button className="btn btn-primary" onClick={this.handleReload}>
              새로고침
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
