package ddvm.coinz

import android.Manifest
import android.location.Location
import android.location.LocationManager
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class CollectCoinsTest {

    @Rule @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Rule @JvmField
    var locationPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @Before
    fun setUp() {
        TestUtils.setUp()
    }

    @Test
    fun collectCoinsTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        //reload the map with test geo json map
        mActivityTestRule.launchActivity(null)

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        User.resetLocalData()
        mActivityTestRule.activity.autocollectionOn()

        //checks if user wallet is initially empty
        assertTrue("is wallet empty", User.getWallet().size == 0)
        val testLoc = Location(LocationManager.GPS_PROVIDER)
        testLoc.longitude = -3.18987
        testLoc.latitude = 55.9425
        mActivityTestRule.activity.runOnUiThread {
            mActivityTestRule.activity.onLocationChanged(testLoc)
        }

        try {
            Thread.sleep(10000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }


        val appCompatImageButton = onView(
                withContentDescription("Navigate up"))
        appCompatImageButton.perform(click())

        val textView = onView(
                (withId(R.id.header_wallet_capacity)))
        //check if 2 coins collected
        textView.check(matches(withText("2/10")))

        //delete the fake fields from shared prefs
        Utils.saveMapToSharedPrefs(mActivityTestRule.activity, "wrong date", "")
    }


}

