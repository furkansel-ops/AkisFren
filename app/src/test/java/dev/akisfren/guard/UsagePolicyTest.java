package dev.akisfren.guard;

import org.junit.Test;
import static org.junit.Assert.*;

public class UsagePolicyTest {
    @Test public void escalatingPenaltyCappedAtThirtyMinutes() {
        assertEquals(300, UsagePolicy.penaltySeconds(300, 0, true));
        assertEquals(600, UsagePolicy.penaltySeconds(300, 1, true));
        assertEquals(900, UsagePolicy.penaltySeconds(300, 4, true));
        assertEquals(1800, UsagePolicy.penaltySeconds(1200, 9, true));
        assertEquals(300, UsagePolicy.penaltySeconds(300, 9, false));
    }
    @Test public void budgetDisabledNeverBlocks() {
        assertFalse(UsagePolicy.budgetExceeded(4000, 15, false));
        assertFalse(UsagePolicy.budgetExceeded(899, 15, true));
        assertTrue(UsagePolicy.budgetExceeded(900, 15, true));
    }
    @Test public void eachFeedHasAnIndependentOptInDailyLimit() {
        assertFalse(UsagePolicy.targetBudgetExceeded(1800, 15, false));
        assertFalse(UsagePolicy.targetBudgetExceeded(899, 15, true));
        assertTrue(UsagePolicy.targetBudgetExceeded(900, 15, true));
        assertTrue(UsagePolicy.targetBudgetExceeded(1200, 15, true));
    }
    @Test public void eightyPercentWarningOnlyFiresBeforeTheLimit() {
        assertFalse(UsagePolicy.shouldWarnSession(7999, 10, true));
        assertTrue(UsagePolicy.shouldWarnSession(8000, 10, true));
        assertTrue(UsagePolicy.shouldWarnSession(9999, 10, true));
        assertFalse(UsagePolicy.shouldWarnSession(10000, 10, true));
        assertFalse(UsagePolicy.shouldWarnSession(8000, 10, false));
        assertFalse(UsagePolicy.shouldWarnSession(0, 0, true));
    }
    @Test public void sleepModeCrossesMidnight() {
        assertEquals("uyku", UsagePolicy.activeMode(true, 1, 23, 9, "serbest"));
        assertEquals("uyku", UsagePolicy.activeMode(true, 23, 23, 9, "serbest"));
        assertEquals("ders", UsagePolicy.activeMode(true, 10, 23, 9, "serbest"));
        assertEquals("serbest", UsagePolicy.activeMode(true, 17, 23, 9, "serbest"));
    }
    @Test public void midnightStartDoesNotSleepAllDay() {
        assertEquals("uyku", UsagePolicy.activeMode(true, 4, 0, 9, "serbest"));
        assertEquals("serbest", UsagePolicy.activeMode(true, 14, 0, 9, "serbest"));
    }
    @Test public void modeLimits() {
        assertEquals(0, UsagePolicy.limitSeconds(120, "uyku"));
        assertEquals(20, UsagePolicy.limitSeconds(120, "ders"));
        assertEquals(60, UsagePolicy.limitSeconds(120, "is"));
        assertEquals(120, UsagePolicy.limitSeconds(120, "serbest"));
    }
}
