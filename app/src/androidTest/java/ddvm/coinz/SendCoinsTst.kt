package ddvm.coinz

import android.Manifest
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.support.v7.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.android.synthetic.main.activity_send_coins.*
import org.junit.*
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SendCoinsTst {
    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(SendCoinsActivity::class.java, true, false)

    @Rule
    @JvmField
    var locationPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @Before
    fun setUp() {
        //clean setup of firebase and User data
        TestUtils.setUp()
        try {
            Thread.sleep(7000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //create a test coin and add it to firestore
        val testCoin = Coin("testCoin", 5.0, "SHIL", LatLng(0.0,0.0))
        FirebaseFirestore.getInstance()
                .collection(User.USERS_COLLECTION_KEY)
                .document(FirebaseAuth.getInstance().uid!!).collection(User.WALLET_COLLECTION_KEY)
                .document(testCoin.id).set(testCoin)
        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //download user data, because MainActivity not launched
        User.downloadUserData(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()) {}

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    //user tries send the coin, but the limit for paying in not achieved (no spare change)
    @Test
    fun sendCoinFailedTest() {

        mActivityTestRule.launchActivity(null)

        //set user location to bank to allow sending coins
        mActivityTestRule.activity.setUserLastLocation(Bank.coordinates)

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //check if coin is in the list
        Espresso.onView(ViewMatchers.withId(R.id.coin_icon)).check(ViewAssertions.matches(ViewMatchers.withText("5")))

        //type recipient
        Espresso.onView(ViewMatchers.withId(R.id.field_recipient)).perform(ViewActions.replaceText("ddvm8"), ViewActions.closeSoftKeyboard())

        //select the coin to send
        Espresso.onView(ViewMatchers.withId(R.id.item_checkBox)).perform(ViewActions.click())

        //check if checkbox exists
        Espresso.onView(ViewMatchers.withId(R.id.item_checkBox)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        //click send coins button
        Espresso.onView(ViewMatchers.withId(R.id.send_coins_button)).perform(ViewActions.click())

        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //check if the coin is still there
        Espresso.onView(ViewMatchers.withId(R.id.coin_icon)).check(ViewAssertions.matches(ViewMatchers.withText("5")))

    }

    //user tries to pay in the coin while being at the bank and already paid 25 coins, send should succeed
    @Test
    fun sendCoinsSuccessTest() {
        mActivityTestRule.launchActivity(null)

        mActivityTestRule.activity.setUserLastLocation(Bank.coordinates)

        User.addPaidInCoins(FirebaseFirestore.getInstance(), 25)

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //select the coin to send
        Espresso.onView(ViewMatchers.withId(R.id.item_checkBox)).perform(ViewActions.click())
        //enter recipient's name
        Espresso.onView(ViewMatchers.withId(R.id.field_recipient)).perform(ViewActions.replaceText("ddvm8"), ViewActions.closeSoftKeyboard())

        //send coin
        Espresso.onView(ViewMatchers.withId(R.id.send_coins_button)).perform(ViewActions.click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //check if recycler view is empty
        val recyclerView: RecyclerView = mActivityTestRule.activity.coins_recycler_view
        val itemsInRecyclerView = recyclerView.adapter.itemCount
        Assert.assertEquals(itemsInRecyclerView, 0)
    }

    @After
    fun clearTestMap() {
        //delete the fake fields from shared prefs to allow download of current map in the gameplay
        Utils.saveMapToSharedPrefs(mActivityTestRule.activity, "wrong date", "")
    }


}