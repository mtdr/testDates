package com.company;

import java.util.Calendar;
import java.util.Date;

public class MyUtils {
    static long day = 24 * 60 * 60 * 1000;
    static double hoursInDay = 8.5;

    public static Date dayPlusWorkHours(Date date, Double workHours) {
        if (workHours == 0 || date == null || date == new Date(0)) {
            return null;
        } else {
            // копируем, чтобы не затереть входные параметры
            Date copyDate = new Date(date.getTime());
            Date foundDate = new Date(copyDate.getTime());
            // проверяем и назначаем корректное время входной даты
            checkAndSetDateToWorkTimeSoft(copyDate);
            // считаем полные рабочие дни
            int fullWorkDays = (int) (workHours / hoursInDay);
            // считаем остаток
            Double remainderHours = workHours % hoursInDay;

            long msInHour = 60 * 60 * 1000;

            Calendar tempCal = Calendar.getInstance();
            while (fullWorkDays > 0) {
                copyDate.setTime(copyDate.getTime() + day);
                tempCal.setTime(new Date(copyDate.getTime()));
                if (tempCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY
                        && tempCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    fullWorkDays--;
                    foundDate.setTime(tempCal.getTimeInMillis());
                }
            }
            foundDate = getNearestWorkDay(foundDate);
            // берем последний добавленный день, к нему нужно добавить остаток от деления
            Date tempRes = new Date(foundDate.getTime() + (long) (remainderHours * msInHour));
            // приводим результат в рабочий график
            checkAndSetDateToWorkTimeSoft(tempRes);
            // TODO TEST!
            return tempRes;
        }
    }

    /**
     * Проверяет дату на вхождение в "рабочее время" (9:00 - 17:30)
     * При несоответствии не обрезает время как
     * @see checkAndSetToWorkTime, а берет следующий день
     *
     * @param date исходная дата
     */
    private static void checkAndSetDateToWorkTimeSoft(Date date) {
        date = getNearestWorkDay(date);
        if (date.getHours() < 9) {
            setTimeToStartOfWorkDay(date);
        } else {
            Date endOfThatDay = new Date(date.getYear(), date.getMonth(), date.getDate(), 17, 30);
            long diff = date.getTime() - endOfThatDay.getTime();
            if (diff > 0) {
                // get next workDay and add diff
                date.setTime(date.getTime() + day);
                setTimeToStartOfWorkDay(date);
                date.setTime(date.getTime() + diff);
            }
        }
    }

    private static Date getNearestWorkDay(Date date) {
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(date);
        int dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK);

        // если date выходной, то нужно найти ближайший рабочий день
        while (dayOfWeek == Calendar.SATURDAY
                || dayOfWeek == Calendar.SUNDAY) {
            setTimeToStartOfWorkDay(date);
            date.setTime(date.getTime() + day);
            tempCal.setTime(date);
            dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK);
        }

        return date;
    }

    /**
     * Выставление начала рабочего дня
     * 09:00
     *
     * @param date
     */
    private static void setTimeToStartOfWorkDay(Date date) {
        date.setHours(9);
        date.setMinutes(0);
        date.setSeconds(0);
    }

    /**
     * Выставление конца рабочего дня
     * 17:30
     *
     * @param date
     */
    private static void setTimeToEndOfWorkDay(Date date) {
        date.setHours(17);
        date.setMinutes(30);
        date.setSeconds(0);
    }

    /**
     * Выдает разницу между 2-ым параметром и 1-ым в миллисекундах
     *
     * @param from 1-ая дата
     * @param to   2-ая
     * @return разница в миллисекундах
     */
    private static long getTimeTo(Date from, Date to) {
        if (to.getTime() <= from.getTime()) {
            return 0;
        }
        return to.getTime() - from.getTime();
    }
}
