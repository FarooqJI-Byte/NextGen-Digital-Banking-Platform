import React, { useState } from 'react';
import './AskAIBar.css';

/**
 * AskAIBar Signature Component
 * Floating glassmorphic pill bar connected to the AI Loan Intelligence Service.
 * Formatted to answer risk questions for the Loan module and display coming-soon on others.
 */
export const AskAIBar = ({
  activeModule = 'loan', // 'loan' | 'general'
  riskData = {
    riskScore: 680,
    eligibilityTier: 'MEDIUM_RISK',
    repaymentProbability: 0.842,
    keyFactors: ['Debt-to-Income ratio at 42%', '3 existing active credit accounts', 'Clean 12-month payment history']
  }
}) => {
  const [query, setQuery] = useState('');
  const [response, setResponse] = useState(null);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!query.trim()) return;

    if (activeModule === 'loan') {
      const lower = query.toLowerCase();
      if (lower.includes('risk') || lower.includes('why') || lower.includes('tier') || lower.includes('score')) {
        setResponse(
          `AI Evaluation Summary: You were categorized as ${riskData.eligibilityTier} with a risk score of ${riskData.riskScore}/900 and estimated repayment probability of ${(riskData.repaymentProbability * 100).toFixed(1)}%. Primary factors: ${riskData.keyFactors.join('; ')}.`
        );
      } else {
        setResponse(`NextGen AI Loan Assistant: Processed query "${query}". Your current credit score is ${riskData.riskScore}. Recommended max tenure is 36 months.`);
      }
    } else {
      setResponse('NextGen AI Assistant: Module integration coming soon.');
    }
  };

  const isDisabled = activeModule !== 'loan';

  return (
    <div className="ask-ai-bar">
      <form onSubmit={handleSubmit} className="ask-ai-bar__form">
        <div className="ask-ai-bar__icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
          </svg>
        </div>

        <input
          type="text"
          className="ask-ai-bar__input"
          placeholder={isDisabled ? "Ask NextGen AI (Coming soon for this view)..." : "Ask NextGen AI about loan eligibility and risk factors..."}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          disabled={isDisabled}
        />

        <button type="submit" className="ask-ai-bar__submit" disabled={isDisabled || !query.trim()}>
          Ask AI
        </button>
      </form>

      {response && (
        <div className="ask-ai-bar__response">
          {response}
        </div>
      )}
    </div>
  );
};

export default AskAIBar;
