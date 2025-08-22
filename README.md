# Algorithmic Trading Backtester

A SpringBatch-based application for backtesting algorithmic trading strategies using real market data stored in Google BigQuery.

## 🚀 Features

- **SpringBatch Processing**: Efficiently process large volumes of historical market data
- **BigQuery Integration**: Store and query market data using Google Cloud BigQuery
- **Trading Strategies**: Implement and test various algorithmic trading strategies
- **Performance Metrics**: Calculate key trading performance indicators (returns, Sharpe ratio, etc.)
- **RESTful API**: Expose backtesting functionality via REST endpoints

## 🛠 Technology Stack

- **Java 11** - Core programming language
- **Spring Boot 2.7.14** - Application framework
- **Spring Batch** - Batch processing framework
- **Google Cloud BigQuery** - Data warehouse and analytics
- **Maven** - Dependency management
- **H2 Database** - In-memory database for Spring Batch metadata

## 📊 Current Implementation (Phase 1)

### Supported Features
- Market data ingestion for AAPL and TSLA
- Simple Moving Average (SMA) crossover strategy
- Basic performance metrics (total return, win rate)
- SpringBatch job for automated processing

### Trading Strategies
1. **Simple Moving Average Crossover**
   - 20-day SMA crosses above/below 50-day SMA
   - Generate BUY signal on golden cross
   - Generate SELL signal on death cross

## 🏗 Project Structure

```
src/
├── main/java/com/tradingbacktester/
│   ├── TradingBacktesterApplication.java     # Main application class
│   ├── config/                               # Configuration classes
│   ├── model/                               # Data models (MarketData, TradingSignal)
│   ├── batch/                               # SpringBatch components
│   ├── strategy/                            # Trading strategy implementations
│   └── service/                             # Business logic services
└── resources/
    ├── application.yml                      # Application configuration
    └── schema-bigquery.sql                 # BigQuery table schemas
```

## 🚀 Getting Started

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- Google Cloud account with BigQuery enabled
- Alpha Vantage API key (free tier available)

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/trading-backtester.git
   cd trading-backtester
   ```

2. **Configure environment variables**
   ```bash
   export GCP_PROJECT_ID=your-project-id
   export BIGQUERY_DATASET=trading_data
   export ALPHA_VANTAGE_API_KEY=your-api-key
   export GOOGLE_APPLICATION_CREDENTIALS=path/to/service-account-key.json
   ```

3. **Set up BigQuery**
   - Create a BigQuery dataset named `trading_data`
   - Ensure your service account has BigQuery admin permissions

4. **Build and run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Access H2 Console (for debugging)**
   - URL: http://localhost:8080/h2-console
   - JDBC URL: jdbc:h2:mem:backtestdb
   - Username: sa
   - Password: (empty)

## 🔮 Roadmap

### Phase 2 (Advanced Features)
- [ ] Multiple technical indicators (RSI, MACD, Bollinger Bands)
- [ ] Portfolio backtesting with multiple stocks
- [ ] Advanced performance metrics (Sharpe ratio, maximum drawdown)
- [ ] BigQuery optimization for large datasets

### Phase 3 (Professional Features)
- [ ] Custom strategy builder interface
- [ ] Risk management (stop-loss, position sizing)
- [ ] Parallel processing for multiple strategies
- [ ] Real-time results visualization

## 📈 Sample Results

*Coming soon - after Phase 1 implementation*

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-strategy`)
3. Commit your changes (`git commit -am 'Add new trading strategy'`)
4. Push to the branch (`git push origin feature/new-strategy`)
5. Create a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Contact

**Pal Neema**
- Email: pneemaa27@gmail.com
- LinkedIn: [/in/pal39](https://linkedin.com/in/pal39)
- GitHub: [/pal-3](https://github.com/pal-3)

---

## 🚧 Development Status

**Current Phase**: Phase 1 MVP Development
**Last Updated**: August 2025
**Status**: 🟡 In Development