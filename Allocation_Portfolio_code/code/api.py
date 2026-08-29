from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any
import pandas as pd
import yfinance as yf
import concurrent.futures
from cachetools import TTLCache, cached
from datetime import timedelta

# Import Aegion Wealth Local Modules
from regime_detector import MarketRegimeDetector
from predictive_engine import LSTMPredictiveEngine  
from allocation_engine import AdvancedAllocationEngine
from multi_asset_allocation import MultiAssetAllocationEngine
from macro_bond_predictor import MacroBondPredictor
from multi_portfolio_xray import PortfolioXRay
from risk_engine import RiskEngine
from portfolio_grader import PortfolioGrader

app = FastAPI(title="Aegion Wealth AI API", version="2.0")

# --- CACHE CONFIGURATION ---
# Optimized TTL to reduce compute costs and API timeouts
ml_prediction_cache = TTLCache(maxsize=500, ttl=timedelta(hours=6).total_seconds())
regime_cache = TTLCache(maxsize=1, ttl=timedelta(hours=6).total_seconds())
macro_prediction_cache = TTLCache(maxsize=10, ttl=timedelta(hours=12).total_seconds())

# --- REQUEST MODELS ---
class StockRequest(BaseModel):
    initial_investment_amount: float
    user_risk_profile: str
    time_horizon_years: int
    stock_tickers: List[str]

class MultiAssetRequest(BaseModel):
    initial_investment_amount: float
    user_risk_profile: str
    time_horizon_years: int
    stock_tickers: List[str]
    bond_tickers: List[str]

@app.get("/")
def health_check():
    return {
        "status": "online", 
        "message": "Aegion Wealth AI Backend is running. Use /docs to test endpoints."
    }

# --- CACHED UTILITIES ---
@cached(cache=ml_prediction_cache)
def cached_stock_ai(ticker: str):
    """Trains Ensemble AI and returns annualized predicted return."""
    try:
        raw_data = yf.download(ticker, period="2y", progress=False)
        if raw_data.empty: return ticker, 0.0
        
        if isinstance(raw_data.columns, pd.MultiIndex):
            stock_data = pd.DataFrame({'Close': raw_data['Close'][ticker], 'Volume': raw_data['Volume'][ticker]})
        else:
            stock_data = pd.DataFrame({'Close': raw_data['Close'], 'Volume': raw_data['Volume']})
        
        stock_data = stock_data.ffill().dropna()
        # Ensure enough data exists for the strict Embargo rules
        if len(stock_data) < 150: return ticker, 0.0
            
        # 🚀 FIX APPLIED: sequence_length=5 to match the cross-sectional engine
        lstm_engine = LSTMPredictiveEngine(sequence_length=5, horizon=30)
        lstm_engine.train(stock_data)
        prediction = lstm_engine.predict_expected_return(stock_data)
        
        # Annualize 30-day predicted return
        expected_return_decimal = prediction['expected_return_percentage'] / 100.0
        annualized_return = ((1 + expected_return_decimal) ** (252 / 30)) - 1
        return ticker, annualized_return
    except ValueError as ve:
        print(f"Data Warning for {ticker}: {ve}")
        return ticker, 0.0
    except Exception as e:
        print(f"Error for {ticker}: {e}")
        return ticker, 0.0

@cached(cache=regime_cache)
def cached_regime_detector():
    """Detects institutional macro-market regime."""
    detector = MarketRegimeDetector(index_ticker='^NSEI', long_ma_window=200)
    return detector.detect_regime()

@cached(cache=macro_prediction_cache)
def cached_macro_ai(bond_tickers_tuple):
    """Caches FRED API calls and Macro ML training to prevent endpoint timeouts."""
    try:
        macro_engine = MacroBondPredictor(horizon_days=30)
        macro_data = macro_engine.fetch_macro_data(period=3)
        macro_engine.train(macro_data)
        # Convert tuple back to list for the engine
        return macro_engine.predict_bond_returns(macro_data, list(bond_tickers_tuple))
    except Exception as e:
        print(f"Macro Engine Error: {e}")
        return {ticker: 0.0 for ticker in bond_tickers_tuple}

# --- ENDPOINT 1: STOCK-ONLY OPTIMIZATION ---
@app.post("/optimize/stocks")
def optimize_stocks(request: StockRequest):
    try:
        system_overrides = []
        
        # 1. Macro Regime Check
        regime_data = cached_regime_detector()
        regime_detector = MarketRegimeDetector()
        
        # Risk Profile Logic
        temp_risk = request.user_risk_profile.lower()
        if request.time_horizon_years < 3 and temp_risk == 'aggressive':
            temp_risk = 'moderate'
            system_overrides.append("⚠️ TIME HORIZON OVERRIDE: Downgraded to Moderate to protect capital.")
            
        final_risk, risk_warning = regime_detector.adjust_risk_profile(temp_risk, regime_data)
        if risk_warning:
            system_overrides.append(risk_warning)

        # 2. Multithreaded AI Predictions
        ai_predictions = {}
        with concurrent.futures.ThreadPoolExecutor(max_workers=min(10, len(request.stock_tickers))) as executor:
            futures = {executor.submit(cached_stock_ai, ticker): ticker for ticker in request.stock_tickers}
            for future in concurrent.futures.as_completed(futures):
                ticker, expected_return = future.result()
                ai_predictions[ticker] = expected_return

        # 3. Fetch Batch Data (With Robust Extraction to prevent Matrix Collapses)
        raw_data = yf.download(request.stock_tickers, period="2y", progress=False)
        
        if isinstance(raw_data.columns, pd.MultiIndex):
            historical_prices = raw_data['Close']
        else:
            historical_prices = pd.DataFrame(raw_data['Close'])
            
        historical_prices = historical_prices.ffill().bfill()
        historical_prices = historical_prices.dropna(axis=1, how='all').dropna()
        
        if historical_prices.empty:
            raise ValueError("Critical Error: Asset price matrix collapsed during formatting.")

        # Align AI predictions to surviving tickers
        valid_tickers = historical_prices.columns.tolist()
        aligned_preds = {t: ai_predictions[t] for t in valid_tickers if t in ai_predictions}

        # 4. Advanced Allocation Engine
        xray_pre_check = PortfolioXRay()
        sector_mapping = xray_pre_check.get_sector_mapping(valid_tickers)
        
        allocator = AdvancedAllocationEngine(risk_profile=final_risk, risk_free_rate=0.07)
        alloc_results = allocator.generate_allocation(
            historical_prices, 
            aligned_preds, 
            sector_mapping=sector_mapping
        )
        optimized_weights = alloc_results['weights']
        metrics = alloc_results['metrics']

        # 🚀 ACCURACY UPGRADE: Explicit Cash Reserve Calculation
        total_invested = sum(optimized_weights.values())
        cash_reserve_pct = round(max(0.0, 100.0 - total_invested), 2)

        # 5. Institutional Risk Engine & X-Ray
        xray = PortfolioXRay(sector_warning_threshold=40.0, single_stock_warning=25.0)
        xray_results = xray.analyze_portfolio(optimized_weights, historical_prices_df=historical_prices)
        
        risk_engine = RiskEngine(initial_investment=request.initial_investment_amount, time_horizon_days=21)
        risk_results = risk_engine.assess_portfolio_risk(historical_prices, optimized_weights)

        # 6. Portfolio Health Grading
        grader = PortfolioGrader(risk_free_rate=0.07)
        health_grade = grader.evaluate_portfolio(
            expected_return=metrics['expected_return'],
            volatility=metrics['portfolio_vol'],
            xray_warnings=xray_results['warnings'],
            overall_sentiment=regime_data['regime'],
            stress_test_loss=risk_results['metrics'].get('black_swan_stress_test_loss', 0.0),
            investment_base=request.initial_investment_amount
        )

        # 🚀 ACCURACY UPGRADE: Add Cash Reserve directly to the weights payload for the frontend UI
        if cash_reserve_pct > 0:
            optimized_weights["CASH_RESERVE"] = cash_reserve_pct

        return {
            "system_overrides": system_overrides,
            "market_regime": {"regime": regime_data['regime'], "status": regime_data['status']},
            "asset_class_breakdown": {
                "Stocks": round(total_invested, 2),
                "Cash_Reserve": cash_reserve_pct
            },
            "allocation": {
                "weights": optimized_weights,
                "expected_annual_return_pct": round(metrics['expected_return'] * 100, 2),
                "portfolio_volatility_pct": round(metrics['portfolio_vol'] * 100, 2)
            },
            "risk_analytics": {
                "cornish_fisher_var_21d": risk_results['metrics'].get('21_day_modified_var', risk_results['metrics'].get('21_day_historical_var')),
                "expected_shortfall_cvar": risk_results['metrics'].get('21_day_expected_shortfall', 0.0),
                "stress_test_loss_exposure": risk_results['metrics'].get('black_swan_stress_test_loss', 0.0)
            },
            "portfolio_xray": xray_results['sector_allocation'],
            "system_alerts": xray_results['warnings'],
            "portfolio_grade": {
                "health_score": health_grade['health_score'],
                "verdict": f"{health_grade['color']} {health_grade['verdict']}",
                "sharpe_ratio": health_grade['sharpe_ratio'],
                "advice": health_grade['advice'],
                "score_breakdown": health_grade.get('score_breakdown', {})
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
# --- ENDPOINT 2: MULTI-ASSET OPTIMIZATION ---
@app.post("/optimize/multi-asset")
def optimize_multi_asset(request: MultiAssetRequest):
    try:
        system_overrides = []
        
        # 1. Macro Regime Check (Cached)
        regime_data = cached_regime_detector()
        regime_detector = MarketRegimeDetector()
        
        # Risk Profile Logic & System Overrides
        temp_risk = request.user_risk_profile.lower()
        if request.time_horizon_years < 3 and temp_risk == 'aggressive':
            temp_risk = 'moderate'
            system_overrides.append("⚠️ TIME HORIZON OVERRIDE: Downgraded to Moderate to protect capital.")
            
        final_risk, risk_warning = regime_detector.adjust_risk_profile(temp_risk, regime_data)
        if risk_warning:
            system_overrides.append(risk_warning)

        # 2. Dual-Engine Intelligence Predictions
        ai_predictions = {}
        # A. Equity Momentum AI (Multithreaded)
        with concurrent.futures.ThreadPoolExecutor(max_workers=len(request.stock_tickers)) as ex:
            futures = {ex.submit(cached_stock_ai, t): t for t in request.stock_tickers}
            for f in concurrent.futures.as_completed(futures):
                ticker, ret = f.result()
                ai_predictions[ticker] = ret

        # 🚀 FIX APPLIED: Macro-Economic Bond AI is now cached safely
        bond_preds = cached_macro_ai(tuple(request.bond_tickers))
        ai_predictions.update(bond_preds)

        # 🚀 FIX APPLIED: Robust Matrix Extraction
        all_tickers = request.stock_tickers + request.bond_tickers
        raw_data = yf.download(all_tickers, period="2y", progress=False)
        
        if isinstance(raw_data.columns, pd.MultiIndex):
            historical_prices = raw_data['Close']
        else:
            historical_prices = pd.DataFrame(raw_data['Close'])
            
        # Backfill first, then drop purely empty columns, then drop remaining NaNs
        historical_prices = historical_prices.ffill().bfill()
        historical_prices = historical_prices.dropna(axis=1, how='all').dropna()
        
        if historical_prices.empty:
            raise ValueError("Critical Error: Asset price matrix collapsed during formatting.")
        
        # Align AI predictions to surviving tickers
        valid_tickers = historical_prices.columns.tolist()
        aligned_preds = {t: ai_predictions[t] for t in valid_tickers if t in ai_predictions}
        
        # 4. Multi-Asset Optimization
        allocator = MultiAssetAllocationEngine(risk_profile=final_risk, risk_free_rate=0.07)
        results = allocator.generate_multi_asset_allocation(
            historical_prices, 
            aligned_preds, 
            request.bond_tickers, 
            regime=regime_data['regime']
        )
        optimized_weights = results['weights']
        metrics = results['metrics']

        # 5. Institutional Risk Engine & X-Ray
        xray = PortfolioXRay(sector_warning_threshold=50.0, single_stock_warning=41.0)
        xray_results = xray.analyze_portfolio(optimized_weights, historical_prices_df=historical_prices)
        
        risk_engine = RiskEngine(initial_investment=request.initial_investment_amount, time_horizon_days=21)
        risk_results = risk_engine.assess_portfolio_risk(historical_prices, optimized_weights)

        # 6. Portfolio Health Grading
        grader = PortfolioGrader(risk_free_rate=0.07)
        health_grade = grader.evaluate_portfolio(
            expected_return=metrics['expected_return'],
            volatility=metrics['portfolio_vol'],
            xray_warnings=xray_results['warnings'],
            overall_sentiment=regime_data['regime'],
            stress_test_loss=risk_results['metrics'].get('black_swan_stress_test_loss', 0.0),
            investment_base=request.initial_investment_amount
        )

        # Final Institutional JSON Payload
        return {
            "system_overrides": system_overrides,
            "market_regime": {"regime": regime_data['regime'], "status": regime_data['status']},
            "asset_class_breakdown": results.get('asset_class_breakdown', {}),
            "allocation": {
                "weights": optimized_weights,
                "expected_annual_return_pct": round(metrics['expected_return'] * 100, 2),
                "portfolio_volatility_pct": round(metrics['portfolio_vol'] * 100, 2)
            },
            "risk_analytics": {
                "cornish_fisher_var_21d": risk_results['metrics'].get('21_day_modified_var', risk_results['metrics'].get('21_day_historical_var')),
                "expected_shortfall_cvar": risk_results['metrics'].get('21_day_expected_shortfall', 0.0),
                "stress_test_loss_exposure": risk_results['metrics'].get('black_swan_stress_test_loss', 0.0)
            },
            "portfolio_xray": xray_results['sector_allocation'],
            "system_alerts": xray_results['warnings'],
            "portfolio_grade": {
                "health_score": health_grade['health_score'],
                "verdict": f"{health_grade['color']} {health_grade['verdict']}",
                "sharpe_ratio": health_grade['sharpe_ratio'],
                "advice": health_grade['advice'],
                "score_breakdown": health_grade.get('score_breakdown', {})
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))