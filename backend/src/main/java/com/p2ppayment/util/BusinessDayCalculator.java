package com.p2ppayment.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class BusinessDayCalculator {

    /**
     * Adds the specified number of business days (weekdays only) to the given date.
     * Saturday and Sunday are not counted as business days.
     *
     * @param startDate the starting date
     * @param daysToAdd number of business days to add (must be >= 0)
     * @return the resulting date after adding business days
     */
    public static LocalDate addBusinessDays(LocalDate startDate, int daysToAdd) {
        if (daysToAdd < 0) {
            throw new IllegalArgumentException("daysToAdd must be >= 0");
        }

        if (daysToAdd == 0) {
            return startDate;
        }

        LocalDate resultDate = startDate;
        int businessDaysAdded = 0;

        while (businessDaysAdded < daysToAdd) {
            resultDate = resultDate.plusDays(1);

            // Check if the day is a weekday (Monday-Friday)
            DayOfWeek dayOfWeek = resultDate.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                businessDaysAdded++;
            }
        }

        return resultDate;
    }
}