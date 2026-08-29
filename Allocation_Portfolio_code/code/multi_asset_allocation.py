import numpy as np
import pandas as pd
import yfinance as yf
import cvxpy as cp
from sklearn.covariance import LedoitWolf
import warnings

warnings.filterwarnings('ignore')

class MultiAssetAllocationEngine:
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

    def _calculate_ewm_covariance(self, returns_df, span=180):
        # 🚀 THE FIX: Safety net to prevent silent matrix collapse
        if returns_df.empty:
            raise ValueError("Matrix Error: The historical price matrix is completely empty. A ticker may be invalid or yfinance failed to fetch data.")
            
        last_date = returns_df.index[-1]
        ewm_cov = returns_df.ewm(span=span).cov().xs(last_date, level=0)
        lw = LedoitWolf().fit(returns_df)
        shrunk_cov = lw.covariance_
        blended_cov = (0.7 * ewm_cov.values) + (0.3 * shrunk_cov)
        return pd.DataFrame(blended_cov * 252, index=returns_df.columns, columns=returns_df.columns)

    def _apply_black_litterman(self, cov_matrix, market_weights, ai_predictions):
        tau = 0.05 
        implied_returns = self.gamma * cov_matrix.dot(market_weights)
        P = np.eye(len(ai_predictions)) 
        Q = ai_predictions.values - (self.rf * 252) 
        
        asset_variances = np.diag(cov_matrix)
        dynamic_confidence = np.clip(1.0 / (asset_variances + 1e-6), 0.1, 5.0)
        omega = np.diag(np.diag(tau * cov_matrix) / dynamic_confidence)
        
        term1 = np.linalg.inv(np.linalg.inv(tau * cov_matrix) + P.T.dot(np.linalg.inv(omega)).dot(P))
        term2 = np.linalg.inv(tau * cov_matrix).dot(implied_returns) + P.T.dot(np.linalg.inv(omega)).dot(Q)
        
        bl_excess_returns = term1.dot(term2)
        return pd.Series(bl_excess_returns, index=ai_predictions.index)

    # 🚀 INNOVATION: Separating Asset Classes for Dynamic Constraints
    def _optimize_cvxpy(self, expected_returns, cov_matrix, current_weights, bond_tickers, regime="NEUTRAL", max_turnover=2.0):
        n = len(expected_returns)
        w = cp.Variable(n) 
        mu = expected_returns.values
        Sigma = cov_matrix.values
        w_curr = current_weights.values
        tickers = expected_returns.index.tolist()
        
        ret = mu.T @ w 
        risk = cp.quad_form(w, Sigma) 
        turnover = cp.norm(w - w_curr, 1) 
        
        l2_penalty = 0.02 * cp.sum_squares(w)
        objective = cp.Maximize(ret - (self.gamma / 2) * risk - (self.tc_penalty * turnover) - l2_penalty)
        
        # 🚀 ACCURACY UPGRADE: Base constraints
        constraints = [
            cp.sum(w) <= 1.0,               
            w >= 0,                        
            turnover <= max_turnover       
        ]
        
        bond_vector = np.array([1 if t in bond_tickers else 0 for t in tickers])
        stock_vector = 1 - bond_vector # Creates an inverse vector for Equities

        for i, expected_ret in enumerate(mu):
            if expected_ret < 0: 
                constraints.append(w[i] == 0)
        
        # 🚀 ACCURACY UPGRADE: Dynamic Regime Constraints
        if regime in ["HIGH_STRESS", "BEAR"]:
            # DEFENSIVE MODE: Bonds must be >= 40%, Stocks must be <= 40% (Forces at least 20% Cash if needed)
            constraints.append(w.T @ bond_vector >= 0.40)
            constraints.append(w.T @ stock_vector <= 0.40)
            constraints.append(w <= 0.20) # Cap any single asset at 20% to prevent concentration risk
        elif regime == "BULL":
            # AGGRESSIVE MODE: Let winners run
            constraints.append(w.T @ bond_vector <= 0.25)
            constraints.append(w.T @ bond_vector >= 0.05)
            constraints.append(w <= 0.35) # Max 35% per asset
        else: 
            # NEUTRAL MODE: Balanced
            constraints.append(w.T @ bond_vector >= 0.20)
            constraints.append(w.T @ bond_vector <= 0.50)
            constraints.append(w <= 0.25) # Max 25% per asset

        prob = cp.Problem(objective, constraints)
        try:
            prob.solve(solver=cp.SCS) 
        except cp.error.SolverError:
            try:
                prob.solve(solver=cp.ECOS) 
            except cp.error.SolverError:
                pass
        
        if w.value is None:
            return current_weights 

        clean_weights = np.where(w.value < 0.005, 0, w.value)
        return pd.Series(clean_weights, index=tickers)
    
    def generate_multi_asset_allocation(self, historical_prices_df, ai_expected_returns_dict, bond_tickers, regime="NEUTRAL"):
        tickers = list(ai_expected_returns_dict.keys())
        current_holdings = pd.Series(1.0/len(tickers), index=tickers) 
            
        ai_predictions = pd.Series(ai_expected_returns_dict)
        returns_df = historical_prices_df.pct_change().dropna()
        
        blended_cov_matrix = self._calculate_ewm_covariance(returns_df)
        market_weights = self._get_market_caps(tickers)
        
        bl_returns = self._apply_black_litterman(blended_cov_matrix, market_weights, ai_predictions)

        optimal_weights = self._optimize_cvxpy(bl_returns, blended_cov_matrix, current_holdings, bond_tickers=bond_tickers, regime=regime)
        
        # 🚀 ACCURACY UPGRADE: Calculate Cash Yield for Multi-Asset
        total_invested = np.sum(optimal_weights)
        cash_weight = max(0.0, 1.0 - total_invested)
        abs_stock_returns = bl_returns + (self.rf * 252)
        portfolio_expected_return = float(np.sum(abs_stock_returns * optimal_weights) + (cash_weight * (self.rf * 252)))
        
        final_weights = (optimal_weights * 100).round(2).to_dict()
        total_bond_weight = sum([wt for tk, wt in final_weights.items() if tk in bond_tickers])
        total_stock_weight = sum([wt for tk, wt in final_weights.items() if tk not in bond_tickers])
        
        return {
            "weights": final_weights,
            "asset_class_breakdown": {
                "Stocks": round(total_stock_weight, 2),
                "Fixed_Income_Bonds": round(total_bond_weight, 2),
                "Cash_Reserve": round(cash_weight * 100, 2)
            },
            "metrics": {
                "expected_return": portfolio_expected_return,
                "portfolio_vol": float(np.sqrt(optimal_weights.T @ blended_cov_matrix @ optimal_weights))
            }
        }