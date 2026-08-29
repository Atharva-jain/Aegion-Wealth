class PortfolioGrader:
    def __init__(self, risk_free_rate=0.07):
        self.risk_free_rate = risk_free_rate

    def evaluate_portfolio(self, expected_return, volatility, xray_warnings, overall_sentiment, stress_test_loss, investment_base):
        """
        Calculates a Health Score (0-100) using continuous scaling and stress-test limits.
        """
        if expected_return > 1.0 or expected_return < -1.0:
            expected_return /= 100.0
        if volatility > 1.0:
            volatility /= 100.0

        score_breakdown = {"base_score": 50.0}
        score = 50.0  

        # 1. Reward/Risk Ratio (Sharpe Ratio)
        if volatility > 1e-6:
            sharpe_ratio = (expected_return - self.risk_free_rate) / volatility
        else:
            sharpe_ratio = 0.0

        # Continuous Scoring: Map Sharpe ratio smoothly
        sharpe_points = max(-25.0, min(35.0, sharpe_ratio * 15.0))
        score += sharpe_points
        score_breakdown["risk_adjusted_points"] = round(sharpe_points, 1)

        # 2. Diversification & Correlation Penalty 
        warning_penalty = 0.0
        for i, warning in enumerate(xray_warnings):
            if "ILLUSION OF DIVERSIFICATION" in warning:
                warning_penalty -= 12.0 # Heavy penalty for hidden correlations
            elif i == 0:
                warning_penalty -= 15.0
            elif i == 1:
                warning_penalty -= 10.0
            else:
                warning_penalty -= 5.0  
                
        score += warning_penalty
        score_breakdown["concentration_penalty"] = warning_penalty

        # 3. Sentiment Adjustment
        sentiment = str(overall_sentiment).upper().strip()
        sentiment_points = {"STRONGLY BULLISH": 15.0, "BULLISH": 10.0, "BEARISH": -10.0, "STRONGLY BEARISH": -20.0}.get(sentiment, 0.0)
        score += sentiment_points
        score_breakdown["sentiment_impact"] = sentiment_points

        # 4. 🚀 INNOVATION 1: Macro Stress Test Limit
        stress_pct = stress_test_loss / investment_base
        if stress_pct > 0.35: # If a Black Swan event would wipe out more than 35% of the portfolio
            score -= 25.0
            score_breakdown["stress_test_penalty"] = -25.0
            xray_warnings.append("🚨 CRITICAL: Portfolio fails Black Swan stress test. Excessive downside risk.")

        final_score = max(0.0, min(100.0, score))

        # 5. Generate Verdict
        if final_score >= 80:
            verdict, color_code, advice = "EXCELLENT", "🟢 GREEN", "Highly efficient, stress-tested portfolio with strong AI tailwinds."
        elif final_score >= 60:
            verdict, color_code, advice = "GOOD", "🟡 YELLOW", "Solid portfolio, but may have slight concentration risks or moderate expected returns."
        elif final_score >= 40:
            verdict, color_code, advice = "SUBOPTIMAL", "🟠 ORANGE", "Notable flaws. The risk taken is not adequately rewarded, or it failed the stress test."
        else:
            verdict, color_code, advice = "DANGEROUS", "🔴 RED", "Avoid. This portfolio is either highly concentrated, secretly correlated, or mathematically projected to fail."

        return {
            "health_score": round(final_score, 1),
            "verdict": verdict,
            "color": color_code,
            "advice": advice,
            "sharpe_ratio": round(sharpe_ratio, 2),
            "score_breakdown": score_breakdown 
        }