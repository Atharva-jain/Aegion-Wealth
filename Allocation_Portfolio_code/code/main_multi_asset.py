import pandas as pd
import yfinance as yf
import warnings
import concurrent.futures 

# Suppress warnings to keep the terminal output clean
warnings.filterwarnings('ignore')

from regime_detector import MarketRegimeDetector
from predictive_engine import LSTMPredictiveEngine  
from multi_asset_allocation import MultiAssetAllocationEngine
from macro_bond_predictor import MacroBondPredictor
from multi_portfolio_xray import PortfolioXRay
from risk_engine import RiskEngine
from portfolio_grader import PortfolioGrader

def train_and_predict_stocks(ticker, raw_data):
    """Worker function to train the Equity Ensemble ML model and predict returns per stock."""
    try:
        if isinstance(raw_data.columns, pd.MultiIndex):
            stock_data = pd.DataFrame({'Close': raw_data['Close'][ticker], 'Volume': raw_data['Volume'][ticker]}).ffill().dropna()
        else:
            stock_data = pd.DataFrame({'Close': raw_data['Close'], 'Volume': raw_data['Volume']}).ffill().dropna()
        
        # 🚀 Syncing with strict Embargo rules
        if len(stock_data) < 150: 
            print(f"     ⚠️ [EQUITY] {ticker:<14} | Bypassing (Insufficient History)")
            return ticker, 0.0
             
        lstm_engine = LSTMPredictiveEngine(sequence_length=5, horizon=30)
        lstm_engine.train(stock_data)
        prediction = lstm_engine.predict_expected_return(stock_data)
        
        expected_return_decimal = prediction['expected_return_percentage'] / 100.0
        annualized_return = ((1 + expected_return_decimal) ** (252 / 30)) - 1
        
        print(f"     ✅ [EQUITY] {ticker:<14} | Predicted Annualized: {annualized_return*100:>6.2f}%")
        return ticker, annualized_return
    except ValueError as ve:
        # Captures strict Embargo purges gracefully
        print(f"     ⚠️ [EQUITY] {ticker:<14} | Data Warning: {ve}")
        return ticker, 0.0
    except Exception as e:
        print(f"     ❌ [EQUITY] {ticker:<14} | Error: {e}")
        return ticker, 0.0

def main():
    print("\n" + "="*85)
    print("🚀 INITIALIZING AEGION WEALTH: MULTI-ASSET QUANTITATIVE FUND")
    print("="*85)
    
    user_risk_profile = 'aggressive' 
    initial_investment_amount = 1000000.0
    
    stock_tickers = ['MARUTI.NS', 'TCS.NS', 'ITC.NS', 'RELIANCE.NS', 'SUNPHARMA.NS', 
               'HDFCBANK.NS', 'ULTRACEMCO.NS', 'NTPC.NS', 'BHARTIARTL.NS', 'TATASTEEL.NS']
    
    bond_tickers = [
        'LIQUIDBEES.NS', 
        'LICNETFGSC.NS', 
        'EBBETF0425.NS', 
        'EBBETF0430.NS',
        'GOLDBEES.NS',
        'MON100.NS'
    ]
    all_tickers = stock_tickers + bond_tickers

    print(f"💰 Initial Investment:   ₹{initial_investment_amount:,.2f}")
    print(f"📈 Equities (Risk-On):   {', '.join([t.replace('.NS', '') for t in stock_tickers])}")
    print(f"🛡️ Bonds (Defensive):    {', '.join([t.replace('.NS', '') for t in bond_tickers])}")

    print("\n[1/6] Running Macro Regime & VIX Stress Detector (With Hysteresis)...")
    regime_detector = MarketRegimeDetector(index_ticker='^NSEI', vix_ticker='^INDIAVIX')
    regime_data = regime_detector.detect_regime()
    final_applied_risk, risk_warning = regime_detector.adjust_risk_profile(user_risk_profile, regime_data)
    detected_regime = regime_data['regime']
    
    print(f"  -> Detected Regime: {detected_regime} Market")
    if risk_warning:
         print(f"  -> {risk_warning}")

    print("\n[2/6] Fetching Multi-Asset Market Data...")
    raw_data = yf.download(all_tickers, period="2y", progress=False)
    
    if isinstance(raw_data.columns, pd.MultiIndex):
        historical_prices = raw_data['Close']
    else:
        historical_prices = pd.DataFrame(raw_data['Close'])

    historical_prices = historical_prices.ffill().bfill()
    historical_prices = historical_prices.dropna(axis=1, how='all').dropna() 
    
    if historical_prices.empty:
        raise ValueError("CRITICAL ERROR: Yahoo Finance returned completely empty data for all tickers.")

    print("\n[3/6] Generating Dual-Engine Intelligence...")
    ai_predictions = {}
    
    print("  --- Running Equity Momentum AI (Cross-Sectional Ensemble) ---")
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(stock_tickers)) as executor:
        futures = {executor.submit(train_and_predict_stocks, ticker, raw_data): ticker for ticker in stock_tickers}
        for future in concurrent.futures.as_completed(futures):
            ticker, expected_return = future.result()
            ai_predictions[ticker] = expected_return

    print("\n  --- Running Macro-Economic Bond AI (Convexity Priced) ---")
    macro_engine = MacroBondPredictor(horizon_days=30)
    macro_data = macro_engine.fetch_macro_data(period=3)
    macro_engine.train(macro_data)
    bond_predictions = macro_engine.predict_bond_returns(macro_data, bond_tickers)
    
    ai_predictions.update(bond_predictions) 

    print(f"\n[4/6] Running Multi-Asset Convex Optimizer (Regime: {detected_regime})...")
    
    valid_price_tickers = historical_prices.columns.tolist()
    aligned_ai_predictions = {ticker: ai_predictions[ticker] for ticker in valid_price_tickers if ticker in ai_predictions}
    
    allocator = MultiAssetAllocationEngine(risk_profile=final_applied_risk, risk_free_rate=0.07)
    
    results = allocator.generate_multi_asset_allocation(
        historical_prices_df=historical_prices, 
        ai_expected_returns_dict=aligned_ai_predictions, 
        bond_tickers=bond_tickers,
        regime=detected_regime
    )
    
    metrics = results['metrics']
    optimized_weights = results['weights']
    asset_breakdown = results['asset_class_breakdown']

    print("\n[5/6] Running Risk Engine (Cornish-Fisher VaR & Stress Testing)...")
    
    xray = PortfolioXRay(sector_warning_threshold=50.0, single_stock_warning=41.0)
    xray_results = xray.analyze_portfolio(optimized_weights, historical_prices_df=historical_prices)
    
    risk_engine = RiskEngine(initial_investment=initial_investment_amount, time_horizon_days=21) 
    risk_results = risk_engine.assess_portfolio_risk(historical_prices, optimized_weights)
    risk_metrics = risk_results['metrics']

    print("\n[6/6] Generating Final AI Health Score...")
    grader = PortfolioGrader(risk_free_rate=0.07)
    
    grading_results = grader.evaluate_portfolio(
        expected_return=metrics['expected_return'],
        volatility=metrics['portfolio_vol'],
        xray_warnings=xray_results['warnings'],
        overall_sentiment=detected_regime, 
        stress_test_loss=risk_metrics.get('black_swan_stress_test_loss', 0.0),
        investment_base=initial_investment_amount
    )

    # ==========================================
    # FINAL OUTPUT
    # ==========================================
    print("\n" + "="*85)
    print("🎯 MULTI-ASSET DEPLOYMENT PLAN")
    print("="*85)
    print(f"📊 ASSET CLASS BREAKDOWN:")
    print(f"   - Equities (Risk-On): {asset_breakdown.get('Stocks', 0.0)}%")
    print(f"   - Bonds (Defensive):  {asset_breakdown.get('Fixed_Income_Bonds', 0.0)}%")
    if asset_breakdown.get('Cash_Reserve', 0.0) > 0:
        print(f"   - Cash Reserve:       {asset_breakdown.get('Cash_Reserve', 0.0)}%")
    print("-" * 85)
    
    total_invested_pct = 0.0
    for ticker, weight in sorted(optimized_weights.items(), key=lambda x: x[1], reverse=True):
        if weight > 0:
            rupee_value = (weight/100) * initial_investment_amount
            total_invested_pct += weight
            asset_type = "🛡️ BOND " if ticker in bond_tickers else "📈 STOCK"
            print(f"{asset_type} | {ticker.replace('.NS', ''):<22} | Weight: {weight:>6.2f}% | Amount: ₹{rupee_value:>10,.2f}")

    # 🚀 ACCURACY UPGRADE: Explicitly print the Cash Reserve allocation
    if total_invested_pct < 99.0:
        cash_weight = 100.0 - total_invested_pct
        cash_value = (cash_weight/100) * initial_investment_amount
        print(f"💵 CASH  | {'CASH RESERVE':<22} | Weight: {cash_weight:>6.2f}% | Amount: ₹{cash_value:>10,.2f}")

    print("\n" + "="*85)
    print("🩺 INSTITUTIONAL PORTFOLIO METRICS")
    print("="*85)
    print(f"Health Score:           {grading_results['health_score']} / 100 ({grading_results['color']} {grading_results['verdict']})")
    print(f"Expected Annual Return: {metrics['expected_return']*100:.2f}%")
    print(f"Portfolio Volatility:   {metrics['portfolio_vol']*100:.2f}%")
    print(f"Sharpe Ratio:           {grading_results['sharpe_ratio']}")
    print("-" * 85)
    
    var_key = "21_day_modified_var" if "21_day_modified_var" in risk_metrics else "21_day_historical_var"
    print(f"Max Normal Loss (VaR):  -₹{risk_metrics.get(var_key, 0.0):,.2f}")
    print(f"Black Swan Stress Risk: -₹{risk_metrics.get('black_swan_stress_test_loss', 0.0):,.2f}")
    
    if xray_results['warnings']:
        print("\n⚠️ System Warnings:")
        for w in xray_results['warnings']:
            print(f"   - {w}")
            
    print("="*85 + "\n")

if __name__ == "__main__":
    main()