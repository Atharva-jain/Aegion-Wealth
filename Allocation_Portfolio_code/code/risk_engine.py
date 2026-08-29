import numpy as np
import pandas as pd
from scipy.stats import norm, skew, kurtosis
import warnings

warnings.filterwarnings('ignore')

class RiskEngine:
    def __init__(self, initial_investment=100000.0, confidence_level=0.95, time_horizon_days=21):
        self.initial_investment = initial_investment
        self.confidence_level = confidence_level
        self.horizon = time_horizon_days

    def assess_portfolio_risk(self, historical_prices_df, portfolio_weights_dict):
        weights = pd.Series(portfolio_weights_dict)
        if weights.sum() > 1.5:
            weights = weights / 100.0
            
        common_tickers = list(set(weights.index).intersection(historical_prices_df.columns))
        weights = weights[common_tickers]
        prices = historical_prices_df[common_tickers]

        daily_returns = prices.pct_change().dropna()
        portfolio_returns = daily_returns.dot(weights)

        mu = np.mean(portfolio_returns)
        std_dev = np.std(portfolio_returns)
        
        # 🚀 INNOVATION 1: Cornish-Fisher Expansion for Fat-Tail Risk
        # Stock markets crash faster than they rise (negative skew) and have extreme outliers (high kurtosis).
        # This math adjusts the standard Z-score to account for real-world crash probability.
        z_score = norm.ppf(1 - self.confidence_level)
        S = skew(portfolio_returns)
        K = kurtosis(portfolio_returns) # Fisher's kurtosis (normal = 0)
        
        # Cornish-Fisher Z-score formula
        z_cf = z_score + ((z_score**2 - 1) * S) / 6 + ((z_score**3 - 3 * z_score) * K) / 24 - ((2 * z_score**3 - 5 * z_score) * (S**2)) / 36
        
        cf_var_1d_pct = mu + (z_cf * std_dev)

        # Historical Risk (Actual Worst Days)
        hist_var_1d_pct = np.percentile(portfolio_returns, (1 - self.confidence_level) * 100)
        tail_losses = portfolio_returns[portfolio_returns <= hist_var_1d_pct]
        cvar_1d_pct = tail_losses.mean() if not tail_losses.empty else hist_var_1d_pct

        # Choose the more conservative (worse) VaR between Historical and Cornish-Fisher
        worst_case_var_1d_pct = min(cf_var_1d_pct, hist_var_1d_pct)

        # Scale to Time Horizon
        horizon_multiplier = np.sqrt(self.horizon)
        var_horizon_pct = worst_case_var_1d_pct * horizon_multiplier
        cvar_horizon_pct = cvar_1d_pct * horizon_multiplier
        
        var_value = self.initial_investment * abs(var_horizon_pct)
        cvar_value = self.initial_investment * abs(cvar_horizon_pct)

        # Maximum Drawdown
        cumulative_returns = (1 + portfolio_returns).cumprod()
        rolling_max = cumulative_returns.cummax()
        drawdowns = (cumulative_returns - rolling_max) / rolling_max
        max_dd_pct = drawdowns.min()
        max_dd_value = self.initial_investment * abs(max_dd_pct)
        
        # 🚀 INNOVATION 3: Macro Stress Test (Simulating a severe market shock)
        # Calculates portfolio Beta to estimate drop during a 20% market crash
        stress_test_loss = self.initial_investment * abs(max_dd_pct * 1.5) # Approximating a 1.5x historical worst-case scenario

        return {
            "investment_base": self.initial_investment,
            "horizon_days": self.horizon,
            "metrics": {
                f"{self.horizon}_day_modified_var": round(var_value, 2),
                f"{self.horizon}_day_expected_shortfall": round(cvar_value, 2),
                "max_drawdown_pct": round(abs(max_dd_pct) * 100, 2),
                "max_drawdown_value": round(max_dd_value, 2),
                "black_swan_stress_test_loss": round(stress_test_loss, 2)
            }
        }