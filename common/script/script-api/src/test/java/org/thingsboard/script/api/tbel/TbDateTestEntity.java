/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
