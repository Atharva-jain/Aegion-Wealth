import yfinance as yf
import pandas as pd
import numpy as np

class PortfolioXRay:
    def __init__(self, sector_warning_threshold=40.0, single_stock_warning=20.0):
        self.sector_threshold = sector_warning_threshold
        self.stock_threshold = single_stock_warning
        self._metadata_cache = {}

    def _fetch_metadata(self, ticker_symbol):
        # (Keep the existing _fetch_metadata logic here exactly as you had it)
        if ticker_symbol in self._metadata_cache:
            return self._metadata_cache[ticker_symbol]
        try:
            stock = yf.Ticker(ticker_symbol)
            info = stock.info
            sector = info.get('sector', 'Unknown Sector')
            industry = info.get('industry', 'Unknown Industry')
            market_cap = info.get('marketCap', 0)
            if market_cap >= 800_000_000_000:       
                cap_category = "Large Cap"
            elif market_cap >= 250_000_000_000:     
                cap_category = "Mid Cap"
            elif market_cap > 0:                    
                cap_category = "Small Cap"
            else:
                cap_category = "Unknown Cap"
            metadata = {'sector': sector, 'industry': industry, 'cap_category': cap_category}
            self._metadata_cache[ticker_symbol] = metadata
            return metadata
        except Exception:
            return {'sector': 'Unknown Sector', 'industry': 'Unknown', 'cap_category': 'Unknown Cap'}

    def get_sector_mapping(self, tickers):
        return {ticker: self._fetch_metadata(ticker)['sector'] for ticker in tickers}

    # 🚀 INNOVATION 2: Added historical_prices_df to check for hidden correlations
    def analyze_portfolio(self, portfolio_weights, historical_prices_df=None):
        total_weight = sum(portfolio_weights.values())
        multiplier = 100.0 if total_weight <= 1.5 else 1.0

        sector_allocation = {}
        industry_allocation = {}
        cap_allocation = {"Large Cap": 0.0, "Mid Cap": 0.0, "Small Cap": 0.0, "Unknown Cap": 0.0}
        warnings = []

        active_tickers = []

        for ticker, raw_weight in portfolio_weights.items():
            weight_pct = raw_weight * multiplier
            if weight_pct <= 0.5: 
                continue 
            
            active_tickers.append(ticker)
                
            if weight_pct >= self.stock_threshold:
                warnings.append(f"⚠️ ASSET CONCENTRATION: {round(weight_pct, 1)}% in a single stock ({ticker.replace('.NS', '')}).")

            metadata = self._fetch_metadata(ticker)
            sector = metadata['sector']
            industry = metadata['industry']
            cap = metadata['cap_category']
            
            sector_allocation[sector] = sector_allocation.get(sector, 0.0) + weight_pct
            industry_allocation[industry] = industry_allocation.get(industry, 0.0) + weight_pct
            cap_allocation[cap] += weight_pct

        sector_allocation = dict(sorted(sector_allocation.items(), key=lambda item: item[1], reverse=True))
        
        for sector, total_weight in sector_allocation.items():
            if total_weight >= self.sector_threshold:
                warnings.append(f"⚠️ SECTOR CONCENTRATION: {round(total_weight, 1)}% heavily exposed to {sector}.")
                
        if cap_allocation.get("Small Cap", 0) >= 30.0:
             warnings.append(f"⚠️ VOLATILITY RISK: Over 30% of portfolio is in Small Caps.")

        # 🚀 INNOVATION 2: Hidden Correlation Check
        if historical_prices_df is not None and len(active_tickers) > 1:
            try:
                returns_df = historical_prices_df[active_tickers].pct_change().dropna()
                corr_matrix = returns_df.corr()
                
                # Check upper triangle of correlation matrix for values > 0.8
                upper_tri = corr_matrix.where(np.triu(np.ones(corr_matrix.shape), k=1).astype(bool))
                high_corr_pairs = [(r, c) for r in upper_tri.index for c in upper_tri.columns if upper_tri.loc[r, c] > 0.8]
                
                for pair in high_corr_pairs:
                    warnings.append(f"⚠️ ILLUSION OF DIVERSIFICATION: {pair[0].replace('.NS', '')} and {pair[1].replace('.NS', '')} are highly correlated. If one crashes, the other likely will too.")
            except Exception as e:
                pass

        return {
            "sector_allocation": {k: round(v, 2) for k, v in sector_allocation.items()},
            "cap_allocation": {k: round(v, 2) for k, v in cap_allocation.items() if v > 0},
            "warnings": warnings
        }