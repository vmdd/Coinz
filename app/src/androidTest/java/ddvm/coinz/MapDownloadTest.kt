package ddvm.coinz


import android.Manifest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.filters.LargeTest

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.assertTrue

@LargeTest
@RunWith(AndroidJUnit4::class)
class MapDownloadTest {

    @Rule @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Rule @JvmField
    var locationPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @Before
    fun setUp() {
        TestUtils.setUp()
    }

    //download the map and check if the number of coins in the list is 50
    @Test
    fun mapDownloadTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(10000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        mActivityTestRule.launchActivity(null)

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }


        val numberOfCoins = mActivityTestRule.getActivity().checkCoinsNumber()

        //check if number of coins in the list is 50
        assertTrue("number of coins is 50", numberOfCoins == 50)

    }


}
