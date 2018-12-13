package ddvm.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_bank.*

class BankActivity : AppCompatActivity() {

    private val exchangeRates = mutableMapOf<String, Double>()              //exchange rates of coins

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)

        //get the exchange rates from shared prefs
        exchangeRates.putAll(Utils.getExchangeRates(this))
        peny_rate.text = String.format("%.3f", exchangeRates["PENY"])
        dolr_rate.text = String.format("%.3f", exchangeRates["DOLR"])
        quid_rate.text = String.format("%.3f", exchangeRates["QUID"])
        shil_rate.text = String.format("%.3f", exchangeRates["SHIL"])

        //number of coins the user paid in slash the limit for today
        n_coins_paid_in.text = String.format("%d/%d", User.getNPaidInCoins(), WalletActivity.dailyLimit)
    }
}
