import yfinance as yf
import pandas as pd
import numpy as np

class PortfolioXRay:
    def __init__(self, sector_warning_threshold=40.0, single_stock_warning=20.0):
        self.sector_threshold = sector_warning_threshold
        self.stock_threshold = single_stock_warning
        self._metadata_cache = {}
        
        # 🚀 THE FIX: A hardcoded map for Indian ETFs and Bonds that Yahoo Finance cannot classify
       # Updated master map for Indian ETFs and Bonds
        self.indian_etf_map = {
            # --- LIQUID ---
            'LIQUIDBEES.NS': {'sector': 'Cash & Equivalents', 'industry': 'Liquid ETF', 'cap_category': 'Large Cap'},

            # --- GOVERNMENT SECURITIES ---
            'LICNETFGSC.NS': {'sector': 'Government Bonds', 'industry': 'G-Sec ETF', 'cap_category': 'Large Cap'},

            # --- CORPORATE / PSU TARGET MATURITY ---
            'EBBETF0425.NS': {'sector': 'Corporate Bonds',    'industry': 'Bond ETF',  'cap_category': 'Large Cap'},
            'EBBETF0430.NS': {'sector': 'Corporate Bonds',    'industry': 'Bond ETF',  'cap_category': 'Large Cap'},

            # --- GOLD (Hedge Assets) ---
            'GOLDBEES.NS':   {'sector': 'Commodities',        'industry': 'Gold ETF',  'cap_category': 'N/A'},
            'SETFGOLD.NS':   {'sector': 'Commodities',        'industry': 'Gold ETF',  'cap_category': 'N/A'},
            'KOTAKGOLD.NS':  {'sector': 'Commodities',        'industry': 'Gold ETF',  'cap_category': 'N/A'},

            # --- GLOBAL ---
            'MON100.NS':     {'sector': 'International Equity','industry': 'US Tech',   'cap_category': 'Large Cap'}
        }

    def _fetch_metadata(self, ticker_symbol):
        # 1. Check Cache
        if ticker_symbol in self._metadata_cache:
            return self._metadata_cache[ticker_symbol]
            
        # 2. Check Indian ETF Master Map (Bypasses Yahoo Finance limits)
        if ticker_symbol in self.indian_etf_map:
            metadata = self.indian_etf_map[ticker_symbol]
            self._metadata_cache[ticker_symbol] = metadata
            return metadata

        # 3. Fallback to Yahoo Finance for normal stocks (like RELIANCE or TCS)
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
   # 🚀 INNOVATION 2: Added historical_prices_df to check for hidden correlations
    def analyze_portfolio(self, portfolio_weights, historical_prices_df=None):
        total_weight = sum(portfolio_weights.values())
        multiplier = 100.0 if total_weight <= 1.5 else 1.0

        sector_allocation = {}
        industry_allocation = {}
        # We don't need to hardcode the keys anymore; we will build them dynamically
        cap_allocation = {} 
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
            
            # 🚀 THE FIX: Use .get() to safely add weights even if the key ('N/A') didn't exist yet
            sector_allocation[sector] = sector_allocation.get(sector, 0.0) + weight_pct
            industry_allocation[industry] = industry_allocation.get(industry, 0.0) + weight_pct
            cap_allocation[cap] = cap_allocation.get(cap, 0.0) + weight_pct

        sector_allocation = dict(sorted(sector_allocation.items(), key=lambda item: item[1], reverse=True))
        
        for sector, total_weight in sector_allocation.items():
            if total_weight >= self.sector_threshold:
                warnings.append(f"⚠️ SECTOR CONCENTRATION: {round(total_weight, 1)}% heavily exposed to {sector}.")
                
        if cap_allocation.get("Small Cap", 0) >= 30.0:
             warnings.append(f"⚠️ VOLATILITY RISK: Over 30% of portfolio is in Small Caps.")

        # Hidden Correlation Check
        if historical_prices_df is not None and len(active_tickers) > 1:
            try:
                returns_df = historical_prices_df[active_tickers].pct_change().dropna()
                corr_matrix = returns_df.corr()
                
                upper_tri = corr_matrix.where(np.triu(np.ones(corr_matrix.shape), k=1).astype(bool))
                high_corr_pairs = [(r, c) for r in upper_tri.index for c in upper_tri.columns if upper_tri.loc[r, c] > 0.8]
                
                for pair in high_corr_pairs:
                    warnings.append(f"⚠️ ILLUSION OF DIVERSIFICATION: {pair[0].replace('.NS', '')} and {pair[1].replace('.NS', '')} are highly correlated.")
            except Exception:
                pass

        return {
            "sector_allocation": {k: round(v, 2) for k, v in sector_allocation.items()},
            "cap_allocation": {k: round(v, 2) for k, v in cap_allocation.items() if v > 0},
            "warnings": warnings
        }