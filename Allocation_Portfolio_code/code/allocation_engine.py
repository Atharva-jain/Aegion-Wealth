import numpy as np
import pandas as pd
import yfinance as yf
import cvxpy as cp
from sklearn.covariance import LedoitWolf
import warnings

warnings.filterwarnings('ignore')

class AdvancedAllocationEngine:
    def __init__(self, risk_profile='moderate', transaction_cost_penalty=0.001, risk_free_rate=0.07):
        self.risk_profile = risk_profile.lower()
        self.tc_penalty = transaction_cost_penalty
        self.rf = risk_free_rate / 252 
        
        self.risk_mapping = {
            'conservative': 10.0, 
            'moderate': 5.0,
            'aggressive': 2.0
        }
        self.gamma = self.risk_mapping.get(self.risk_profile, 5.0)

    def _get_market_caps(self, tickers):
        market_caps = {}
        for ticker in tickers:
            try:
                info = yf.Ticker(ticker).info
                market_caps[ticker] = info.get('marketCap', 1e9) 
            except:
                market_caps[ticker] = 1e9
        caps_series = pd.Series(market_caps)
        return caps_series / caps_series.sum() 

    # 🚀 INNOVATION 1: Exponentially Weighted Covariance (Shape Fixed)
    def _calculate_ewm_covariance(self, returns_df, span=180):
        """Gives more weight to recent volatility and correlations."""
        
        # FIX: Correctly extract the NxN covariance matrix for the most recent date
        last_date = returns_df.index[-1]
        ewm_cov = returns_df.ewm(span=span).cov().xs(last_date, level=0)
        
        # Blend with Ledoit-Wolf shrinkage to ensure matrix stability (Positive Definite)
        lw = LedoitWolf().fit(returns_df)
        shrunk_cov = lw.covariance_
        
        blended_cov = (0.7 * ewm_cov.values) + (0.3 * shrunk_cov)
        return pd.DataFrame(blended_cov * 252, index=returns_df.columns, columns=returns_df.columns)

    def _apply_black_litterman(self, cov_matrix, market_weights, ai_predictions):
        tau = 0.05  # Base scalar for market uncertainty
        
        implied_returns = self.gamma * cov_matrix.dot(market_weights)
        P = np.eye(len(ai_predictions)) 
        Q = ai_predictions.values - (self.rf * 252) 
        
        # 🚀 INNOVATION 2: Dynamic Uncertainty (Smart Omega Matrix)
        # Scales uncertainty based on the individual asset's historical volatility.
        # If an asset is highly volatile, the engine relies more on the market implied return.
        asset_variances = np.diag(cov_matrix)
        dynamic_confidence = np.clip(1.0 / (asset_variances + 1e-6), 0.1, 5.0)
        omega = np.diag(np.diag(tau * cov_matrix) / dynamic_confidence)
        
        term1 = np.linalg.inv(np.linalg.inv(tau * cov_matrix) + P.T.dot(np.linalg.inv(omega)).dot(P))
        term2 = np.linalg.inv(tau * cov_matrix).dot(implied_returns) + P.T.dot(np.linalg.inv(omega)).dot(Q)
        
        bl_excess_returns = term1.dot(term2)
        return pd.Series(bl_excess_returns, index=ai_predictions.index)

    def _optimize_cvxpy(self, expected_returns, cov_matrix, current_weights, sector_mapping=None, max_sector_weight=0.40, max_turnover=2.0):
        n = len(expected_returns)
        w = cp.Variable(n) 
        mu = expected_returns.values
        Sigma = cov_matrix.values
        w_curr = current_weights.values
        
        base_weight = 1.0 / n if n > 0 else 1.0
        dynamic_max_weight = min(0.249, base_weight + 0.15) if base_weight < 0.249 else base_weight + 0.05
        
        ret = mu.T @ w 
        risk = cp.quad_form(w, Sigma) 
        turnover = cp.norm(w - w_curr, 1) 
        
        l2_penalty = 0.05 * cp.sum_squares(w)
        
        objective = cp.Maximize(ret - (self.gamma / 2) * risk - (self.tc_penalty * turnover) - l2_penalty)
        
        constraints = [
            cp.sum(w) <= 1.0,              # 🚀 FIX 1: Allow sum to be less than 1 (Creates a Cash Position)
            w >= 0,                        
            w <= dynamic_max_weight,       
            turnover <= max_turnover       
        ]

        # 🚀 FIX 2: Strict Zero-Weight constraint for crashing stocks
        # If the Black-Litterman implied return is mathematically negative, force weight to 0.
        for i, expected_ret in enumerate(mu):
            if expected_ret < 0:
                constraints.append(w[i] == 0)
        
        if sector_mapping:
            tickers = expected_returns.index.tolist()
            unique_sectors = set(sector_mapping.values())
            for sector in unique_sectors:
                if sector not in ['Unknown Sector', 'Unknown']:
                    sector_vector = np.array([1 if sector_mapping.get(t) == sector else 0 for t in tickers])
                    constraints.append(w.T @ sector_vector <= max_sector_weight)
        
        prob = cp.Problem(objective, constraints)
        try:
            prob.solve(solver=cp.ECOS) 
        except cp.error.SolverError:
            try:
                prob.solve(solver=cp.SCS) 
            except cp.error.SolverError:
                pass
        
        if w.value is None:
            print("⚠️ Constraints infeasible. Defaulting to fallback weights.")
            return current_weights 

        clean_weights = np.where(w.value < 0.005, 0, w.value)
        
        # 🚀 FIX 3: Removed the normalization `clean_weights / np.sum(clean_weights)` 
        # so the portfolio doesn't artificially inflate back to 100% after zeroing out bad stocks.
        
        return pd.Series(clean_weights, index=expected_returns.index)

    def generate_allocation(self, historical_prices_df, ai_expected_returns_dict, current_holdings_dict=None, sector_mapping=None):
        tickers = list(ai_expected_returns_dict.keys())
        
        if current_holdings_dict is None:
            current_holdings = pd.Series(1.0/len(tickers), index=tickers) 
        else:
            current_holdings = pd.Series(current_holdings_dict).reindex(tickers).fillna(0.0)
            
        ai_predictions = pd.Series(ai_expected_returns_dict)
        returns_df = historical_prices_df.pct_change().dropna()
        
        blended_cov_matrix = self._calculate_ewm_covariance(returns_df)
        market_weights = self._get_market_caps(tickers)
        
        bl_returns = self._apply_black_litterman(blended_cov_matrix, market_weights, ai_predictions)

        optimal_weights = self._optimize_cvxpy(bl_returns, blended_cov_matrix, current_holdings, sector_mapping=sector_mapping)
        
        # 🚀 ACCURACY UPGRADE: Calculate Cash Yield
        total_invested = np.sum(optimal_weights)
        cash_weight = max(0.0, 1.0 - total_invested)
        
        # Absolute returns = Excess Returns + Risk-Free Rate
        abs_stock_returns = bl_returns + (self.rf * 252)
        
        # Portfolio Expected Return = (Stock Returns * Stock Weights) + (Risk-Free Rate * Cash Weight)
        portfolio_expected_return = float(np.sum(abs_stock_returns * optimal_weights) + (cash_weight * (self.rf * 252)))
        
        return {
            "weights": (optimal_weights * 100).round(2).to_dict(),
            "exclusions": {}, 
            "metrics": {
                "expected_return": portfolio_expected_return,
                "portfolio_vol": float(np.sqrt(optimal_weights.T @ blended_cov_matrix @ optimal_weights))
            }
        }