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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith



@LargeTest
@RunWith(AndroidJUnit4::class)
class ReceivedCoinsTest {

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
        val testCoin = Coin("8e25-4c82-ff7d-e6b6-1eca-c435testCoin", 5.0, "SHIL", LatLng(0.0,0.0))
        FirebaseFirestore.getInstance()
                .collection(User.USERS_COLLECTION_KEY)
                .document(FirebaseAuth.getInstance().uid!!).collection(User.RECEIVED_COLLECTION_KEY)
                .document(testCoin.id).set(testCoin)
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    //user tries to pay in coin gifted by someone even though he achieved the limit of coins for today, but banking still succeeds
    @Test
    fun receiveCoinlTest() {

        mActivityTestRule.launchActivity(null)

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //set the number of coins paid in to 26
        User.addPaidInCoins(FirebaseFirestore.getInstance(), 25)

        //open navigation drawer
        onView(withContentDescription("Navigate up")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //check that user's gold is 0
        onView(withId(R.id.header_gold)).check(matches(withText("0")))

        //open received coins screen
        onView(withText(mActivityTestRule.activity.getString(R.string.received_coins))).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //select the coin to bank in
        onView(withId(R.id.item_checkBox)).perform(click())

        onView(withId(R.id.item_checkBox)).check(matches(isDisplayed()))

        //check if the user exhausted the limit
        assertEquals(User.getNPaidInCoins(),25)

        //pay the coin in
        onView(withId(R.id.pay_in_button)).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //go back to main activity
        onView(withContentDescription("Navigate up")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //open navigation drawer
        onView(withContentDescription("Navigate up")).perform(click())

        //check the amount of gold
        onView(withId(R.id.header_gold)).check(matches(withText("225")))

    }

    @After
    fun clearTestMap() {
        //delete the fake fields from shared prefs to allow download of current map in the gameplay
        Utils.saveMapToSharedPrefs(mActivityTestRule.activity, "wrong date", "")
    }


}
