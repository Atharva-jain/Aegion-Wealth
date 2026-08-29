import numpy as np
import pandas as pd
import yfinance as yf
import pandas_datareader.data as web
from datetime import datetime, timedelta
from sklearn.preprocessing import RobustScaler
from sklearn.ensemble import HistGradientBoostingRegressor
import warnings

warnings.filterwarnings('ignore')

class MacroBondPredictor:
    def __init__(self, horizon_days=30):
        self.horizon = horizon_days
        self.scaler = RobustScaler()
        
        # 1. TUNED MACRO MODEL:
        # High regularization because macro data is incredibly noisy and low-frequency.
        self.model = HistGradientBoostingRegressor(
            max_iter=100, 
            learning_rate=0.02, 
            max_depth=3, 
            min_samples_leaf=10,
            l2_regularization=2.0, 
            random_state=42
        )
        
        # 2. 🚀 INNOVATION: Convexity and Yield Curve Beta added
        # Curve Beta dictates how much the specific bond's yield moves relative to the 10Y benchmark.
        self.bond_database = {
            # --- LIQUID / OVERNIGHT (Cash Proxies) ---
            'LIQUIDBEES.NS': {'type': 'BOND', 'name': 'Nippon Liquid ETF',  'duration': 0.05, 'convexity': 0.01, 'curve_beta': 0.10, 'base_yield': 0.065},

            # --- GOVERNMENT SECURITIES (G-SECS) ---
            'LICNETFGSC.NS': {'type': 'BOND', 'name': 'LIC Long G-Sec ETF', 'duration': 7.00, 'convexity': 55.0, 'curve_beta': 1.00, 'base_yield': 0.071},

            # --- BHARAT BOND ETFS (AAA PSU Corporate Bonds) ---
            'EBBETF0425.NS': {'type': 'BOND', 'name': 'Bharat Bond 2025',   'duration': 0.90, 'convexity': 1.20, 'curve_beta': 0.35, 'base_yield': 0.073},
            'EBBETF0430.NS': {'type': 'BOND', 'name': 'Bharat Bond 2030',   'duration': 4.50, 'convexity': 24.5, 'curve_beta': 0.85, 'base_yield': 0.074},

            # --- NON-BOND HEDGES (Driven by separate macro factors, not duration) ---
            'GOLDBEES.NS':   {'type': 'HEDGE', 'name': 'Nippon Gold ETF',   'base_yield': 0.00},
            'MON100.NS':     {'type': 'HEDGE', 'name': 'Nasdaq 100 ETF',    'base_yield': 0.00}
        }

    def fetch_macro_data(self, period=3):
        print(f"🌍 Fetching Macro-Economic Indicators via FRED & Yahoo Finance...")
        
        end_date = datetime.today()
        start_date = end_date - timedelta(days=period*365)
        
        try:
            fred_data = web.DataReader('INDIRLTLT01STM', 'fred', start_date, end_date)
            fred_data.columns = ['Yield_10Y']
            indian_yield_df = fred_data.resample('D').ffill()
            print("     ✅ Successfully fetched Indian Yields from FRED.")
        except Exception as e:
            print(f"     ⚠️ FRED Error: {e}. Falling back to US 10Y proxy.")
            fallback = yf.download('^TNX', start=start_date, end=end_date, progress=False)['Close']
            indian_yield_df = pd.DataFrame(fallback).rename(columns={fallback.columns[0] if isinstance(fallback, pd.DataFrame) else '^TNX': 'Yield_10Y'})

        yf_tickers = {'USD_INR': 'INR=X', 'Nifty_50': '^NSEI'}
        raw_yf_data = yf.download(list(yf_tickers.values()), start=start_date, end=end_date, progress=False)['Close']
        
        if isinstance(raw_yf_data.columns, pd.MultiIndex):
             raw_yf_data = raw_yf_data[list(yf_tickers.values())]
        raw_yf_data.columns = list(yf_tickers.keys())

        indian_yield_df.index = indian_yield_df.index.tz_localize(None).normalize()
        raw_yf_data.index = raw_yf_data.index.tz_localize(None).normalize()
        
        merged_data = pd.merge(indian_yield_df, raw_yf_data, left_index=True, right_index=True, how='inner')
        return merged_data.ffill().bfill().dropna()

    def _engineer_macro_features(self, df, is_training=True):
        data = df.copy()
        
        # Absolute changes for yields (bps equivalents), percentage changes for prices
        data['Yield_Change_1W'] = data['Yield_10Y'].diff(5)
        data['Yield_Change_1M'] = data['Yield_10Y'].diff(20)
        data['Currency_Stress'] = data['USD_INR'].pct_change(20)
        data['Equity_Momentum'] = data['Nifty_50'].pct_change(20)
        
        if is_training:
            # 🚀 FIX: Prevent Look-ahead bias by dropping NaN horizons
            data['Target_Yield_Change'] = data['Yield_10Y'].shift(-self.horizon) - data['Yield_10Y']
            
        return data.dropna()

    def train(self, macro_data):
        data = self._engineer_macro_features(macro_data, is_training=True)
        features = ['Yield_10Y', 'Yield_Change_1W', 'Yield_Change_1M', 'Currency_Stress', 'Equity_Momentum']
        
        # Guardrail against edge case where data is too short
        if len(data) < 50:
            print("⚠️ Insufficient macro data for robust training. Model may underperform.")
            
        X = self.scaler.fit_transform(data[features])
        y = data['Target_Yield_Change'].values
        self.model.fit(X, y)

    def predict_bond_returns(self, macro_data, searched_bond_tickers):
        data = self._engineer_macro_features(macro_data, is_training=False)
        features = ['Yield_10Y', 'Yield_Change_1W', 'Yield_Change_1M', 'Currency_Stress', 'Equity_Momentum']
        
        # Get latest features for prediction
        latest_features = self.scaler.transform(data[features].tail(1))
        current_currency_stress = float(data['Currency_Stress'].iloc[-1])
        current_equity_momentum = float(data['Equity_Momentum'].iloc[-1])
        
        # Predict the absolute shift in the 10Y Yield (e.g., 0.15 means +15 bps)
        predicted_10Y_shift = float(self.model.predict(latest_features)[0])
        
        # Cap extreme macro predictions (+/- 75 bps max in 30 days)
        predicted_10Y_shift = np.clip(predicted_10Y_shift, -0.75, 0.75)
        
        results = {}
        for ticker in searched_bond_tickers:
            profile = self.bond_database.get(ticker, {'type': 'UNKNOWN', 'name': 'Generic Asset', 'duration': 4.0, 'convexity': 16.0, 'curve_beta': 1.0, 'base_yield': 0.065})
            
            if profile['type'] == 'BOND':
                # 🚀 1. Apply Curve Beta: Adjust 10Y shift for this specific maturity
                local_yield_shift = predicted_10Y_shift * profile['curve_beta']
                
                # Convert absolute shift to decimal for formula (e.g., 0.15 -> 0.0015)
                dy = local_yield_shift / 100.0 
                
                # 🚀 2. Apply Convexity Pricing Formula: (-D * dy) + (0.5 * C * dy^2)
                duration_effect = -profile['duration'] * dy
                convexity_effect = 0.5 * profile['convexity'] * (dy ** 2)
                capital_gains = duration_effect + convexity_effect
                
                # Calculate Base Yield Accrual for the horizon
                horizon_yield_return = profile['base_yield'] * (self.horizon / 365.0)
                
                total_return = horizon_yield_return + capital_gains
                
            elif profile['type'] == 'HEDGE':
                # 🚀 3. Macro Hedges (Gold/FX) are not priced by duration. 
                # We model them based on structural flight-to-safety rules.
                if ticker == 'GOLDBEES.NS':
                    # Gold rises when currency weakens or equities crash
                    total_return = (current_currency_stress * 0.5) - (current_equity_momentum * 0.2)
                elif ticker == 'MON100.NS':
                    # Nasdaq ETF is unhedged, so it benefits heavily from INR depreciation
                    total_return = (current_equity_momentum * 0.8) + (current_currency_stress * 1.0)
                else:
                    total_return = 0.0
                    
            else:
                total_return = 0.0 # Default fallback
            
            # Bound the returns mathematically to avoid optimizer blowups
            total_return = np.clip(total_return, -0.10, 0.10)
            annualized_return = ((1 + total_return) ** (365 / self.horizon)) - 1
            
            results[ticker] = annualized_return
            
            if profile['type'] == 'BOND':
                print(f"     🛡️ [BOND] {profile['name']:<20} | Pred 10Y Shift: {predicted_10Y_shift:>5.2f} bps | Ann. Return: {annualized_return*100:>5.2f}%")
            else:
                print(f"     🦔 [HEDGE] {profile['name']:<19} | Macro Implied Return       | Ann. Return: {annualized_return*100:>5.2f}%")
                
        return results