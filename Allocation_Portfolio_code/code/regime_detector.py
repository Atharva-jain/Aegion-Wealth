import yfinance as yf
import pandas as pd
import numpy as np
import warnings

warnings.filterwarnings('ignore')

class MarketRegimeDetector:
    def __init__(self, index_ticker='^NSEI', vix_ticker='^INDIAVIX', long_ma_window=200, short_ma_window=50):
        self.index_ticker = index_ticker
        self.vix_ticker = vix_ticker
        self.long_ma = long_ma_window
        self.short_ma = short_ma_window
        
        # 🚀 INNOVATION: Hysteresis Thresholds
        self.vix_panic_trigger = 22.0
        self.vix_relax_trigger = 18.0 

    def _calculate_atr_buffer(self, data, window=14):
        high_low = data['High'] - data['Low']
        high_close = np.abs(data['High'] - data['Close'].shift())
        low_close = np.abs(data['Low'] - data['Close'].shift())
        ranges = pd.concat([high_low, high_close, low_close], axis=1)
        true_range = np.max(ranges, axis=1)
        atr = true_range.rolling(window).mean()
        return (atr.iloc[-1] / data['Close'].iloc[-1])

    def detect_regime(self):
        print(f"Engine: Analyzing Macro Market Regime & Volatility Matrix...")
        
        try:
            index_data = yf.download(self.index_ticker, period="2y", progress=False)
            if index_data.empty:
                raise ValueError("No data found for the primary index.")
                
            if isinstance(index_data.columns, pd.MultiIndex):
                close_series = index_data['Close', self.index_ticker]
                high_series = index_data['High', self.index_ticker]
                low_series = index_data['Low', self.index_ticker]
            else:
                close_series = index_data['Close']
                high_series = index_data['High']
                low_series = index_data['Low']
                
            df = pd.DataFrame({'Close': close_series, 'High': high_series, 'Low': low_series}).ffill().dropna()

            # 🚀 FIX: Fetching a longer window of VIX to check for recent panic
            try:
                vix_data = yf.download(self.vix_ticker, period="1mo", progress=False)['Close']
                if isinstance(vix_data, pd.DataFrame):
                    vix_data = vix_data.iloc[:, 0]
                
                # Check the last 10 days for Hysteresis logic
                recent_vix_history = vix_data.tail(10)
                current_vix = float(recent_vix_history.iloc[-1])
                max_recent_vix = float(recent_vix_history.max())
            except Exception:
                current_vix = 15.0 
                max_recent_vix = 15.0

            sma_200 = df['Close'].rolling(window=self.long_ma).mean()
            sma_50 = df['Close'].rolling(window=self.short_ma).mean()
            
            current_price = float(df['Close'].iloc[-1])
            current_200_dma = float(sma_200.iloc[-1])
            current_50_dma = float(sma_50.iloc[-1])
            
            dynamic_buffer = self._calculate_atr_buffer(df)
            distance_pct = (current_price - current_200_dma) / current_200_dma
            
            regime = "NEUTRAL"
            status = "Transition Market."
            
            # 🚀 INNOVATION: Hysteresis Logic applied
            is_in_panic = current_vix >= self.vix_panic_trigger
            is_cooling_off = (max_recent_vix >= self.vix_panic_trigger) and (current_vix > self.vix_relax_trigger)

            if is_in_panic or is_cooling_off:
                regime = "HIGH_STRESS"
                status = f"🚨 PANIC REGIME: VIX at {round(current_vix, 1)} (Cooling down to {self.vix_relax_trigger} required to exit panic state)."
            
            elif current_50_dma < current_200_dma and distance_pct < -dynamic_buffer:
                regime = "BEAR"
                status = f"🐻 STRUCTURAL BEAR: Death Cross active. Price is {round(abs(distance_pct) * 100, 2)}% below 200-DMA."
                
            elif current_50_dma > current_200_dma and distance_pct > dynamic_buffer:
                regime = "BULL"
                status = f"📈 CONFIRMED BULL: Golden Cross active. Price safely above 200-DMA."
                
            elif distance_pct < -dynamic_buffer:
                regime = "CORRECTION"
                status = f"📉 CORRECTION: Price dipped below 200-DMA, but long-term trend hasn't collapsed."
                
            else:
                regime = "NEUTRAL"
                status = f"⚖️ CHOP REGIME: Price hovering within the {round(dynamic_buffer*100, 2)}% noise buffer."

            return {
                "regime": regime,
                "current_index_price": round(current_price, 2),
                "current_vix": round(current_vix, 2),
                "status": status,
                "dynamic_buffer_applied_pct": round(dynamic_buffer * 100, 2)
            }
            
        except Exception as e:
            return {"regime": "NEUTRAL", "status": f"Error: {e}", "current_vix": 15.0}

    def adjust_risk_profile(self, user_requested_risk, regime_data):
        original_risk = user_requested_risk.lower()
        regime = regime_data.get('regime', 'NEUTRAL')

        if regime == "HIGH_STRESS":
            return "conservative", "🚨 SYSTEM OVERRIDE: High Volatility (VIX Panic) detected. Locked to Conservative."
        elif regime == "BEAR" and original_risk in ["aggressive", "moderate"]:
            return "conservative", "🚨 SYSTEM OVERRIDE: Structural Bear Market. Downgrading to Conservative."
        elif regime in ["CORRECTION", "NEUTRAL"] and original_risk == "aggressive":
            return "moderate", "⚠️ SYSTEM CAUTION: Market trend uncertain. Downgrading Aggressive to Moderate."
                
        return original_risk, None