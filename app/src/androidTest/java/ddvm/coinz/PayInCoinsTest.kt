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

        onView(withContentDescription("Navigate up")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withId(R.id.header_gold)).check(matches(withText("0")))

        onView(withText("Wallet")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withId(R.id.item_checkBox)).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withId(R.id.item_checkBox)).check(matches(isDisplayed()))

        onView(withId(R.id.pay_in_button)).perform(click())

        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withId(R.id.coin_icon)).check(matches(withText("5")))

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

        onView(withContentDescription("Navigate up")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withId(R.id.header_gold)).check(matches(withText("0")))

        //set the location at the bank to allow paying to the bank
        mActivityTestRule.activity.setUserLatLng(Bank.coordinates)

        onView(withText("Wallet")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withId(R.id.item_checkBox)).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withId(R.id.item_checkBox)).check(matches(isDisplayed()))

        onView(withId(R.id.pay_in_button)).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withContentDescription("Navigate up")).perform(click())

        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        onView(withContentDescription("Navigate up")).perform(click())

        //check if the amount of coins is 0 and gold increased
        onView(withId(R.id.header_wallet_capacity)).check(matches(withText("0/10")))
        onView(withId(R.id.header_gold)).check(matches(withText("225")))

        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }



}
