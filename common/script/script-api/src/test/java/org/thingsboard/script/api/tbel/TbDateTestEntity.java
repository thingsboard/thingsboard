// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import lombok.Data;

import java.time.chrono.IsoChronology;

@Data
public class TbDateTestEntity {
    private int year;
    private int month;
    private int date;
    private int hours;
    public TbDateTestEntity(int year, int month, int date, int hours) {
        this.year = year;
        this.month = month;
        this.date = date;
        this.hours = hours;
        if (hours > 23) {
            if (date == 31) {
                this.year++;
                this.month = 1;
                this.date = 1;
            } else {
                this.date++;
            }
            this.hours = hours - 24;
        } else if (hours < 0) {
            if (month== 1 && date == 1) {
                this.year--;
                this.month = 12;
                this.date = 31;
            } else {
                this.date--;
            }
            this.hours = hours + 24;
        }

        if (this.date > 28) {
            int dom = 31;
            switch (month) {
                case 2:
                    dom = IsoChronology.INSTANCE.isLeapYear((long) year) ? 29 : 28;
                case 3:
                case 5:
                case 7:
                case 8:
                case 10:
                default:
                    break;
                case 4:
                case 6:
                case 9:
                case 11:
                    dom = 30;
            }
            if (this.date > dom) {
                this.date = this.date - dom;
                this.month++;
            }
        }
    }
    public int getYear(){
        return year < 70 ? 2000 + year : year <= 99 ? 1900 + year : year;
    }

    public String geMonthStr(){
        return String.format("%02d", month);
    }

    public String geDateStr(){
        return String.format("%02d", date);
    }

    public String geHoursStr(){
        return String.format("%02d", hours);
    }
}
