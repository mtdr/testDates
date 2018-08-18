package com.company;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MyUtils {
    static long day = 24 * 60 * 60 * 1000;

    public static Date dayPlusWorkHours(Date date, Double workHours) {
        if (workHours == 0 || date == null || date == new Date(0)) {
            return null;
        } else {
            // копируем, чтобы не затереть входные параметры
            Date copyDate = new Date(date.getTime());
            // проверяем и назначаем корректное время входной даты
            checkAndSetWorkTime(copyDate);
            // считаем полные рабочие дни
            int fullWorkDays = (int) (workHours % 8);
            // считаем остаток
            Double remainderHours = workHours % 8;

            long msInHour = 60 * 60 * 1000;

            Calendar tempCal = Calendar.getInstance();
            tempCal.setTime(copyDate);
            // будем вносить в список даты рабочих дней, пока не добавим нужное количество (fullWorkDays)
            ArrayList<Date> dateList = new ArrayList<>();
            while (fullWorkDays > 0) {
                if (tempCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && tempCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    fullWorkDays--;
                    dateList.add(tempCal.getTime());
                }
                copyDate.setTime(copyDate.getTime() + day);
                tempCal.setTime(new Date(copyDate.getTime()));
            }
            // берем последний добавленный день, к нему нужно добавить остаток от деления
            Date tempRes = new Date(dateList.get(dateList.size() - 1).getTime() + (long) (remainderHours * msInHour));
            // пробуем привести предварительный результат в рабочий график
            Date tempInWorkTime = new Date(tempRes.getTime());
            checkAndSetWorkTime(tempInWorkTime);
            // check if tempRes in the same workday
            long diff = getTimeTo(tempRes, tempInWorkTime);
            if (diff > 0) {
                // разница менее 8 часов, добавляем к старту следующего рабочего дня
                Date nextDay = new Date(tempRes.getTime());
                setTimeToStartOfWorkDay(nextDay);
                tempRes.setTime(nextDay.getTime() + diff * msInHour);
            }
            // TODO TEST!
            return tempRes;
        }
    }

    /**
     * Проверяет дату на вхождение в "рабочее время" (9:00 - 17:30)
     * При несоответствии промежутку заменяет на ближайшую к рабочему времени
     *
     * @param date исходная дата
     */
    private static void checkAndSetWorkTime(Date date) {
        date = getNearestWorkDay(date);
        if (date.getHours() < 9) {
            setTimeToStartOfWorkDay(date);
        } else {
            if (date.getHours() > 17 || (date.getHours() == 17 && date.getMinutes() > 30)) {
                setTimeToEndOfWorkDay(date);
            }
        }
    }

    private static Date getNearestWorkDay(Date date) {
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(date);
        int dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK);

        // если date выходной, то нужно найти ближайший рабочий день
        if (dayOfWeek == Calendar.SATURDAY
                || dayOfWeek == Calendar.SUNDAY) {
            // берем начало рабочего дня
            setTimeToStartOfWorkDay(date);
            while (dayOfWeek != Calendar.SATURDAY
                    && dayOfWeek != Calendar.SUNDAY) {
                date.setTime(date.getTime() + day);
            }
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
