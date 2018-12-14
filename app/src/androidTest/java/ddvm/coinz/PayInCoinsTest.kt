package ddvm.coinz

import android.Manifest
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.mapboxsdk.geometry.LatLng
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith



@LargeTest
@RunWith(AndroidJUnit4::class)
class PayInCoinsTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Rule
    @JvmField
    var locationPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @Before
    fun setUp() {
        TestUtils.setUp()
        try {
            Thread.sleep(10000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        val testCoin = Coin("testCoin", 5.0, "SHIL", LatLng(0.0,0.0))
        FirebaseFirestore.getInstance()
                .collection(User.USERS_COLLECTION_KEY)
                .document(FirebaseAuth.getInstance().uid!!).collection(User.WALLET_COLLECTION_KEY)
                .document(testCoin.id).set(testCoin)
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    //user tries to pay in the coin while not being at the bank, the test will fail
    @Test
    fun payInFailTest() {

        mActivityTestRule.launchActivity(null)

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //open nav drawer
        onView(withContentDescription("Navigate up")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //check the gold is 0
        onView(withId(R.id.header_gold)).check(matches(withText("0")))

        //check that one coin is in the wallet
        onView(withId(R.id.header_wallet_capacity)).check(matches(withText("1/10")))

        //open the wallet
        onView(withText("Wallet")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //check the coin is there
        onView(withId(R.id.coin_icon)).check(matches(withText("5")))

        //select the coin to send
        onView(withId(R.id.item_checkBox)).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withId(R.id.item_checkBox)).check(matches(isDisplayed()))

        //pay the coin in
        onView(withId(R.id.pay_in_button)).perform(click())

        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //the coin still in the wallet
        onView(withId(R.id.coin_icon)).check(matches(withText("5")))

        //go back to main activity
        onView(withContentDescription("Navigate up")).perform(click())

        //open nav drawer
        onView(withContentDescription("Navigate up")).perform(click())

        //check that one coin is in the wallet
        onView(withId(R.id.header_wallet_capacity)).check(matches(withText("1/10")))

    }

    //user tries to pay in the coin while being at the bank, test will pass
    @Test
    fun payInSuccessTest() {
        mActivityTestRule.launchActivity(null)

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //open nav drawer
        onView(withContentDescription("Navigate up")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //check that gold is 0
        onView(withId(R.id.header_gold)).check(matches(withText("0")))

        //set the location at the bank to allow paying to the bank
        mActivityTestRule.activity.setUserLatLng(Bank.coordinates)

        //open the wallet
        onView(withText("Wallet")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //select the coin to pay in
        onView(withId(R.id.item_checkBox)).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withId(R.id.item_checkBox)).check(matches(isDisplayed()))

        //check the coin there has gold value 225
        onView(withId(R.id.gold_value)).check(matches(withText("225")))

        //pay the coin in
        onView(withId(R.id.pay_in_button)).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //go back to main menu
        onView(withContentDescription("Navigate up")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //open nav drawer
        onView(withContentDescription("Navigate up")).perform(click())

        //check if the amount of coins is 0 and gold increased to exactly 225
        onView(withId(R.id.header_wallet_capacity)).check(matches(withText("0/10")))
        onView(withId(R.id.header_gold)).check(matches(withText("225")))

        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    //user tries to pay in the coin while being at the bank
    //already paid in 20 coins today and tries to pay 6 more, but fails because of the limit
    @Test
    fun payInOverLimitTest() {

        //we need to add 5 more coins to have 6 in the wallet
        for(i in 0..4) {
            val testCoin = Coin("testCoin$i", 5.0 + i, "SHIL", LatLng(0.0, 0.0))
            FirebaseFirestore.getInstance()
                    .collection(User.USERS_COLLECTION_KEY)
                    .document(FirebaseAuth.getInstance().uid!!).collection(User.WALLET_COLLECTION_KEY)
                    .document(testCoin.id).set(testCoin)
        }

        //time for firebase to store stuff
        try {
            Thread.sleep(10000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //launch the activity
        mActivityTestRule.launchActivity(null)
        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        //set number of coins paid in to 20
        User.addPaidInCoins(FirebaseFirestore.getInstance(), 20)

        //open nav drawer
        onView(withContentDescription("Navigate up")).perform(click())

        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //check that gold is 0
        onView(withId(R.id.header_gold)).check(matches(withText("0")))

        //check there are 6 coins in the wallet
        onView(withId(R.id.header_wallet_capacity)).check(matches(withText("6/10")))

        //set the location at the bank to allow paying to the bank
        mActivityTestRule.activity.setUserLatLng(Bank.coordinates)

        //open the wallet
        onView(withText("Wallet")).perform(click())

        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //check all coins to pay in
        onView(withId(R.id.check_all)).perform(click())

        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //pay the coin in
        onView(withId(R.id.pay_in_button)).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //go back to main menu
        onView(withContentDescription("Navigate up")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //open nav drawer
        onView(withContentDescription("Navigate up")).perform(click())

        //check if the amount of coins is still 6 and gold 0
        onView(withId(R.id.header_wallet_capacity)).check(matches(withText("6/10")))
        onView(withId(R.id.header_gold)).check(matches(withText("0")))

        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    @After
    fun clearTestMap() {
        //delete the fake fields from shared prefs to allow download of current map in the gameplay
        Utils.saveMapToSharedPrefs(mActivityTestRule.activity, "wrong date", "")
    }


}
