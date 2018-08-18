package com.company;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class MyUtilsTest {

    @Test
    public void testSameWorkday() {
        Date from = new Date(118, 7, 16, 15, 0);
        Date toSameWorkDay = new Date(118, 7, 16, 17, 30);
        assertEquals(MyUtils.dayPlusWorkHours(from, 2.5), toSameWorkDay);
    }

    @Test
    public void testNextWorkday() {
        Date from = new Date(118, 7, 16, 15, 0);
        Date toSameWorkDay = new Date(118, 7, 17, 17, 30);
        assertEquals(MyUtils.dayPlusWorkHours(from, 11.0), toSameWorkDay);
    }

    @Test
    public void testWorkdayAfterWeekEnd() {
        Date friday = new Date(118, 7, 17, 15, 0);
        Date monday = new Date(118, 7, 20, 17, 30);
        assertEquals(MyUtils.dayPlusWorkHours(friday, 11.0), monday);
    }

    @Test
    public void testDayAfterDay() {
        Date friday = new Date(118, 7, 20, 15, 0);
        Date monday = new Date(118, 7, 22, 9, 30);
        assertEquals(MyUtils.dayPlusWorkHours(friday, 11.5), monday);
    }


}