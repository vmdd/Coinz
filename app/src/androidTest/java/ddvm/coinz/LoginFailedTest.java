package ddvm.coinz;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

//types in incorrect data and check if the login failed
@LargeTest
@RunWith(AndroidJUnit4.class)
public class LoginFailedTest {

    @Rule
    public ActivityTestRule<LoginActivity> mActivityTestRule = new ActivityTestRule<>(LoginActivity.class);

    @Test
    public void loginFailedTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.fieldEmail),
                        isDisplayed()));
        appCompatEditText.perform(replaceText("abc"), closeSoftKeyboard());

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.email_sign_in_button), withText("Sign in"),
                        isDisplayed()));
        appCompatButton.perform(click());

        ViewInteraction editText = onView(
                allOf(withId(R.id.fieldEmail), withText("abc"),
                        isDisplayed()));
        editText.check(matches(withText("abc")));

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.fieldPassword),
                        isDisplayed()));
        appCompatEditText2.perform(replaceText("123456"), closeSoftKeyboard());

        ViewInteraction appCompatEditText3 = onView(
                allOf(withId(R.id.fieldPassword), withText("123456"),
                        isDisplayed()));
        appCompatEditText3.perform(pressImeActionButton());

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.fieldEmail), withText("abc"),
                        isDisplayed()));
        editText2.check(matches(withText("abc")));

        ViewInteraction appCompatEditText4 = onView(
                allOf(withId(R.id.fieldEmail), withText("abc"),
                        isDisplayed()));
        appCompatEditText4.perform(replaceText("abc@gmail.com"));

        ViewInteraction appCompatEditText5 = onView(
                allOf(withId(R.id.fieldEmail), withText("abc@gmail.com"),
                        isDisplayed()));
        appCompatEditText5.perform(closeSoftKeyboard());

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.email_sign_in_button), withText("Sign in"),
                        isDisplayed()));
        appCompatButton2.perform(click());

        ViewInteraction editText3 = onView(
                allOf(withId(R.id.fieldEmail), withText("abc@gmail.com"),
                        isDisplayed()));
        editText3.check(matches(withText("abc@gmail.com")));

    }
}
