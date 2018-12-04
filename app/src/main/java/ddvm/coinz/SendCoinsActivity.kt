package ddvm.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class SendCoinsActivity : AppCompatActivity() {

    private val tag = "SendCoinsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_coins)
    }
}
