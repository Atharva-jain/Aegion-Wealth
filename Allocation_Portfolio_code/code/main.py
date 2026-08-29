import pandas as pd
import yfinance as yf
import warnings
import concurrent.futures 

# Suppress warnings to keep the terminal output clean
warnings.filterwarnings('ignore')

from regime_detector import MarketRegimeDetector
from predictive_engine import LSTMPredictiveEngine  
from allocation_engine import AdvancedAllocationEngine
from portfolio_xray import PortfolioXRay
from risk_engine import RiskEngine
from portfolio_grader import PortfolioGrader

def train_and_predict(ticker, raw_data):
    """
    Multithreading worker function to train the Ensemble ML model and predict returns per stock.
    """
    try:
        # Safely extract data depending on pandas/yfinance version mapping
        if isinstance(raw_data.columns, pd.MultiIndex):
            stock_data = pd.DataFrame({
                'Close': raw_data['Close'][ticker],
                'Volume': raw_data['Volume'][ticker]
            }).ffill().dropna()
        else:
            stock_data = pd.DataFrame({
                'Close': raw_data['Close'],
                'Volume': raw_data['Volume']
            }).ffill().dropna()
        
        # 🚀 FIX: Adjusted minimum rows for the new Embargo logic.
        # We need enough data to generate features, purge the last 30 days, and train.
        if len(stock_data) < 150:
             print(f"     ⚠️ {ticker:<15} | Bypassing AI (Insufficient History for Embargo)")
             return ticker, 0.0
             
        # 🚀 FIX: Sequence length synced with the new point-in-time engine
        lstm_engine = LSTMPredictiveEngine(sequence_length=5, horizon=30)
        lstm_engine.train(stock_data)
        prediction = lstm_engine.predict_expected_return(stock_data)
        
        expected_return_decimal = prediction['expected_return_percentage'] / 100.0
        
        # Annualize the 30-day smoothed prediction
        annualized_return = ((1 + expected_return_decimal) ** (252 / 30)) - 1
        
        print(f"     ✅ {ticker.replace('.NS', ''):<15} | Predicted 30-Day: {prediction['expected_return_percentage']:>6.2f}% | Annualized: {annualized_return*100:>6.2f}%")
        return ticker, annualized_return
        
    except ValueError as ve:
        # Catches strict data embargo errors gracefully without crashing the thread
        print(f"     ⚠️ {ticker:<15} | Data Warning: {ve}")
        return ticker, 0.0
    except Exception as e:
        print(f"     ❌ {ticker:<15} | Error: {e}")
        return ticker, 0.0

def main():
    print("\n" + "="*80)
    print("🚀 INITIALIZING AEGION WEALTH AI (INSTITUTIONAL GRADE)")
    print("="*80)
    
    user_risk_profile = 'moderate' 
    initial_investment_amount = 600.0
    time_horizon_years = 5
    
    # Diversified Equity Selection
    #tickers = ['MARUTI.NS', 'TCS.NS', 'ITC.NS', 'RELIANCE.NS', 'SUNPHARMA.NS', 
    #           'HDFCBANK.NS', 'ULTRACEMCO.NS', 'NTPC.NS', 'BHARTIARTL.NS', 'TATASTEEL.NS']
    #tickers = ['INFY.NS', 'TCS.NS', 'GOLDIAM.NS', 'ADANIENT.NS']
    tickers = ['INFY.NS', 'TCS.NS', 'WIPRO.NS']


    print(f"👤 Requested Risk:      {user_risk_profile.upper()}")
    print(f"💰 Initial Investment:  ₹{initial_investment_amount:,.2f}")
    print(f"⏳ Time Horizon:        {time_horizon_years} Years")
    print(f"📈 Selected Tickers:    {', '.join([t.replace('.NS', '') for t in tickers])}")

    horizon_warning = None
    if time_horizon_years < 3 and user_risk_profile == 'aggressive':
        user_risk_profile = 'moderate' 
        horizon_warning = "⚠️ TIME HORIZON OVERRIDE: Downgraded to Moderate to protect capital."
        print(f"\n{horizon_warning}")

    print("\n[1/6] Running Institutional Macro Regime Detector (NIFTY 50 & VIX)...")
    regime_detector = MarketRegimeDetector(index_ticker='^NSEI', vix_ticker='^INDIAVIX')
    regime_data = regime_detector.detect_regime()
    final_applied_risk, risk_warning = regime_detector.adjust_risk_profile(user_risk_profile, regime_data)
    
    print(f"  -> Detected Regime: {regime_data['regime']}")
    print(f"  -> VIX Level:       {regime_data['current_vix']}")
    print(f"  -> Status:          {regime_data['status']}")
    if risk_warning:
         print(f"  -> {risk_warning}")

    print("\n[1.5/6] Fetching 2-Year Market Data & Sector Mappings...")
    try:
        raw_data = yf.download(tickers, period="2y", progress=False)
        if isinstance(raw_data.columns, pd.MultiIndex):
            historical_prices = raw_data['Close'].ffill().dropna()
        else:
            historical_prices = raw_data['Close'].ffill().dropna()
    except Exception as e:
        print(f"Error fetching data: {e}")
        return

    xray = PortfolioXRay(sector_warning_threshold=40.0, single_stock_warning=25.0)
    sector_mapping = xray.get_sector_mapping(tickers) 

    print("\n[2/6] Running Ensembled Machine Learning Engine (Point-in-Time)...")
    ai_predictions = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(tickers)) as executor:
        futures = {executor.submit(train_and_predict, ticker, raw_data): ticker for ticker in tickers}
        for future in concurrent.futures.as_completed(futures):
            ticker, expected_return = future.result()
            ai_predictions[ticker] = expected_return

    print("\n[3/6] Running Convex Optimizer (EWM Covariance & L2 Regularization)...")
    engine = AdvancedAllocationEngine(risk_profile=final_applied_risk, transaction_cost_penalty=0.002, risk_free_rate=0.07)
    
    results = engine.generate_allocation(
        historical_prices, 
        ai_predictions, 
        sector_mapping=sector_mapping
    )
    
    metrics = results['metrics']
    optimized_weights = results['weights']
    
    print(f"  -> Expected Annual Return: {metrics['expected_return']*100:.2f}%")
    print(f"  -> Portfolio Volatility:   {metrics['portfolio_vol']*100:.2f}%")

    print("\n[4/6] Running Portfolio X-Ray (Hidden Correlation Checks)...")
    xray_results = xray.analyze_portfolio(optimized_weights, historical_prices_df=historical_prices)
    actual_warnings = xray_results['warnings']
    
    print("  -> Sector Breakdown:")
    for sector, wt in xray_results['sector_allocation'].items():
         print(f"       - {sector}: {wt}%")

    print("\n[5/6] Running Risk Engine (Cornish-Fisher VaR & Macro Stress Tests)...")
    risk_engine = RiskEngine(initial_investment=initial_investment_amount, time_horizon_days=21) 
    risk_results = risk_engine.assess_portfolio_risk(historical_prices, optimized_weights)
    risk_metrics = risk_results['metrics']
    
    var_key = "21_day_modified_var" if "21_day_modified_var" in risk_metrics else "21_day_historical_var"
    
    print(f"  -> Max Loss (Normal Markets - CF VaR):             -₹{risk_metrics[var_key]:,.2f}")
    print(f"  -> Average Loss in a Crash (Expected Shortfall):   -₹{risk_metrics['21_day_expected_shortfall']:,.2f}")
    print(f"  -> 🚨 Black Swan Stress Test Exposure:             -₹{risk_metrics.get('black_swan_stress_test_loss', 0.0):,.2f}")

    print("\n[6/6] Generating Final AI Health Score...")
    grader = PortfolioGrader(risk_free_rate=0.07)
    grading_results = grader.evaluate_portfolio(
        expected_return=metrics['expected_return'],
        volatility=metrics['portfolio_vol'],
        xray_warnings=actual_warnings,
        overall_sentiment=regime_data['regime'],
        stress_test_loss=risk_metrics.get('black_swan_stress_test_loss', 0.0),
        investment_base=initial_investment_amount
    )

   # ==========================================
    # FINAL OUTPUT / DEPLOYMENT LOG
    # ==========================================
    print("\n" + "="*80)
    print("🎯 OPTIMIZED PORTFOLIO DEPLOYMENT PLAN")
    print("="*80)
    
    total_invested_pct = 0.0
    for ticker, weight in sorted(optimized_weights.items(), key=lambda x: x[1], reverse=True):
        if weight > 0:
            rupee_value = (weight/100) * initial_investment_amount
            total_invested_pct += weight
            print(f"{ticker.replace('.NS', ''):<15} | Weight: {weight:>6.2f}% | Amount to Deploy: ₹{rupee_value:>10,.2f}")

    # Display Cash Reserve if the algorithm decided to hold back capital
    if total_invested_pct < 99.0:
        cash_weight = 100.0 - total_invested_pct
        cash_value = (cash_weight/100) * initial_investment_amount
        print(f"{'CASH RESERVE':<15} | Weight: {cash_weight:>6.2f}% | Amount to Deploy: ₹{cash_value:>10,.2f}")

    print("\n" + "="*80)
    print("🩺 FINAL PORTFOLIO GRADE & VERDICT")
    print("="*80)
    print(f"Health Score:  {grading_results['health_score']} / 100")
    print(f"Verdict:       {grading_results['color']} {grading_results['verdict']}")
    print(f"Sharpe Ratio:  {grading_results['sharpe_ratio']}")
    
    if actual_warnings:
        print("\n⚠️  System Warnings:")
        for warning in actual_warnings:
            print(f"    - {warning}")
            
    print(f"\n💡 AI Advice:   {grading_results['advice']}")
    print("="*80 + "\n")
    
if __name__ == "__main__":
    main()