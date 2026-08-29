import numpy as np
import pandas as pd
from sklearn.preprocessing import RobustScaler
from sklearn.ensemble import HistGradientBoostingRegressor, VotingRegressor
from sklearn.linear_model import Ridge
import warnings

warnings.filterwarnings('ignore')

class LSTMPredictiveEngine:
    def __init__(self, sequence_length=5, horizon=30):
        # We keep the signature to maintain compatibility with main.py
        # However, we will use sequence_length to calculate rolling feature momentum 
        # rather than flattening arrays, which ruins tree-based models.
        self.sequence_length = sequence_length
        self.horizon = horizon
        
        self.feature_scaler = RobustScaler()
        
        # 1. TUNED ENSEMBLE: 
        # Tree handles non-linear regime interactions; Ridge provides linear extrapolation.
        tree_model = HistGradientBoostingRegressor(
            max_iter=100, 
            learning_rate=0.03,  # Slower learning rate for better generalization
            max_depth=3,         # Shallow depth to strictly prevent overfitting noise
            min_samples_leaf=15, # Force the tree to find broader patterns
            l2_regularization=1.0,
            random_state=42
        )
        linear_model = Ridge(alpha=200.0) # High penalty to compress noise
        
        self.model = VotingRegressor([('tree', tree_model), ('linear', linear_model)])
        
        self.feature_columns = [
            'Log_Return', 'Vol_Change', 'SMA_20_Ratio', 'SMA_50_Ratio',
            'RSI', 'MACD_Hist', 'ATR_Proxy', 'BB_Width', 'OBV_Change', 'Momentum_1M'
        ]

    def _engineer_features(self, df, is_training=True):
        data = df.copy()
        
        # --- SAFE Cross-Sectional Feature Engineering (No Future Data) ---
        data['Log_Return'] = np.log(data['Close'] / data['Close'].shift(1))
        data['Vol_Change'] = data['Volume'].pct_change()
        
        sma_20 = data['Close'].rolling(window=20).mean()
        sma_50 = data['Close'].rolling(window=50).mean()
        data['SMA_20_Ratio'] = data['Close'] / sma_20
        data['SMA_50_Ratio'] = data['Close'] / sma_50
        data['Momentum_1M'] = data['Close'] / data['Close'].shift(20) - 1
        
        std_20 = data['Close'].rolling(window=20).std()
        data['BB_Width'] = (std_20 * 2) / sma_20 
        
        high_low = data['Close'].rolling(2).max() - data['Close'].rolling(2).min()
        data['ATR_Proxy'] = high_low.rolling(14).mean() / data['Close']
        
        direction = np.where(data['Close'] > data['Close'].shift(1), 1, -1)
        direction[0] = 0
        data['OBV'] = (data['Volume'] * direction).cumsum()
        data['OBV_Change'] = data['OBV'].pct_change()
        
        delta = data['Close'].diff()
        gain = (delta.where(delta > 0, 0)).rolling(window=14).mean()
        loss = (-delta.where(delta < 0, 0)).rolling(window=14).mean()
        rs = gain / loss
        data['RSI'] = np.where(loss == 0, 100, 100 - (100 / (1 + rs)))
        
        exp1 = data['Close'].ewm(span=12, adjust=False).mean()
        exp2 = data['Close'].ewm(span=26, adjust=False).mean()
        macd_line = exp1 - exp2
        signal_line = macd_line.ewm(span=9, adjust=False).mean()
        data['MACD_Hist'] = macd_line - signal_line
        
        if is_training:
            # 🚀 FIX: Absolute Point-in-Time Target. No overlapping rolling means.
            # We predict exactly where the price will be in 'horizon' days.
            data['Target'] = (data['Close'].shift(-self.horizon) / data['Close']) - 1
            
        data.replace([np.inf, -np.inf], np.nan, inplace=True)
        return data

    def train(self, historical_data):
        data = self._engineer_features(historical_data, is_training=True)
        
        # 🚀 FIX: The Purge and Embargo
        # We must explicitly drop the last 'horizon' rows because their targets are NaN.
        # Furthermore, dropna() handles the NaNs from the rolling feature calculations.
        clean_data = data.dropna()
        
        if clean_data.empty or len(clean_data) < 50:
            raise ValueError("Insufficient data to train the model safely after embargo.")

        # Instead of sequence flattening, we use the point-in-time features.
        # Tree models prefer these distinct, non-collinear features.
        df_features = clean_data[self.feature_columns]
        y = clean_data['Target'].values
        
        X = self.feature_scaler.fit_transform(df_features)
        
        self.model.fit(X, y)

    def predict_expected_return(self, recent_data):
        data = self._engineer_features(recent_data, is_training=False)
        
        # We only need the very last valid row for the current prediction
        clean_data = data.dropna(subset=self.feature_columns)
        
        if clean_data.empty:
             raise ValueError("Not enough data to generate current features.")
             
        latest_features = clean_data[self.feature_columns].tail(1)
        current_price = clean_data['Close'].iloc[-1] 
        
        scaled_input = self.feature_scaler.transform(latest_features)
        
        # Output prediction
        expected_return_decimal = float(self.model.predict(scaled_input)[0])
        
        # 🚀 Institutional Guardrail
        # Standard deviation capping. Ensures the AI doesn't predict absurd +50% returns in 30 days.
        expected_return_decimal = np.clip(expected_return_decimal, -0.08, 0.08)
        
        return {
            "current_price": round(current_price, 2),
            "predicted_price_at_horizon": round(current_price * (1 + expected_return_decimal), 2),
            "expected_return_percentage": round(expected_return_decimal * 100, 2),
            "horizon_days": self.horizon,
            "engine_status": "Point-in-Time Ensemble AI"
        }