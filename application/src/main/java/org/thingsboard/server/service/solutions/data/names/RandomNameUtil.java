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
package org.thingsboard.server.service.solutions.data.names;

import org.thingsboard.server.common.data.StringUtils;

import java.util.Random;

public class RandomNameUtil {

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private static String[] FIRST_NAMES = new String[]{
            "Nadia", "Lynne", "Lera", "Holly", "Zina", "Mandi", "Kasie", "Josef", "Liane", "Man",
            "Karan", "Nga", "Tesha", "Elva", "Bree", "Ida", "Mimi", "Altha", "Omar", "Ollie",
            "Nicol", "Ned", "Teri", "Sabra", "Kari", "Eva", "Penni", "Simon", "Kaci", "Jess",
            "Bea", "Ron", "Melba", "Vella", "Nada", "Cyndi", "Audry", "Helga", "Chana", "Lola",
            "Terry", "Tami", "Dedra", "Erwin", "Anne", "Zoila", "Nelia", "Hyman", "Deane", "Erica",
            "Amos", "Maura", "Gwenn", "Evan", "Lelia", "Grant", "Abe", "Fanny", "Cindi", "Pilar",
            "Darcy", "Len", "Casie", "Jose", "Kylie", "Cami", "Casey", "Kerri", "Bruno", "Theda",
            "Ardis", "Carin", "Belia", "Jeff", "Yetta", "Ola", "Lyla", "Megan", "Zita", "Rocky",
            "Darci", "Dale", "Mirta", "Tanja", "Stacy", "Julie", "Aisha", "Avis", "Hugh", "Anh",
            "Bud", "Earle", "Ossie", "Odell", "Dovie", "Afton", "Joel", "Aida", "Vina", "Emiko",
            "Tori", "Keena", "Addie", "Nancy", "Tonda", "Josue", "Dina", "Mitch", "Thea", "Cole",
            "Lyman", "Donna", "Roma", "Deena", "Lue", "Bette", "Ilene", "Vera", "Kirby", "Lenna",
            "Pat", "Grace", "Gus", "Frank", "Lena", "Adele", "Kerry", "Santo", "Wade", "Trudi",
            "Bruce", "Raul", "Katy", "Heidi", "Marna", "Faith", "Ronna", "Kylee", "Emma", "Luna",
            "Ilda", "Mose", "Juan", "Cori", "Gayla", "Otha", "Jung", "Bambi", "Joi", "Sibyl",
            "Reid", "Bonny", "Roni", "Joy", "Farah", "Jeane", "Jill", "Tisha", "Leon", "Leigh",
            "Inge", "Zella", "Rubin", "Carri", "Nana", "Sofia", "Lucio", "Eboni", "Adam", "Lino",
            "Elvis", "Marci", "Adina", "Nanci", "Joyce", "James", "Louis", "Aurea", "Chi", "Kathy",
            "Nina", "Lise", "Reda", "Candi", "Ralph", "Velva", "Mari", "Mavis", "Kory", "Tammi",
            "Lucy", "Lavon", "Ana", "Lin", "Alena", "Haley", "Myra", "Amy", "Marlo", "Ami"
    };

    private static String[] LAST_NAMES = new String[]{
            "Vinson", "Rogers", "Burkett", "Gamboa", "Gross", "Toledo", "Kiser", "Harman", "Pierce", "Lovett",
            "Sexton", "Coates", "Seymour", "Holland", "Finley", "Hagen", "Schmitt", "Beach", "Rea", "Peck",
            "Noel", "Archer", "Zamora", "Wood", "Rivera", "Lentz", "Alonso", "Rider", "Story", "Schwab",
            "Mercado", "Caruso", "Bynum", "Spears", "Bingham", "Tyler", "Ponce", "Skaggs", "Matos", "Stinson",
            "Burgos", "Coles", "Helton", "Joyce", "Galvan", "Grant", "Stahl", "Daigle", "Hayden", "Quiroz",
            "Horne", "Shaw", "Addison", "Harvey", "Watkins", "Romano", "Ahmed", "Lind", "Swenson", "Kemp",
            "Barnes", "Horton", "Dugan", "Farr", "Swanson", "Hale", "Hidalgo", "Schulte", "Ervin", "Osorio",
            "Lincoln", "Zhang", "Oakes", "Stiles", "Lu", "Culver", "Singer", "Knott", "Bullard", "Butts",
            "Coley", "Roth", "Cobb", "Darnell", "Vaughan", "Conrad", "Austin", "Rico", "Hobson", "Brunner",
            "Mccray", "Mattson", "Barber", "Clement", "Ly", "Dejesus", "Painter", "Macias", "Healy", "Duncan",
            "Forrest", "Hurley", "Torres", "Blue", "Rucker", "Ferrell", "Maddox", "Osborne", "Holt", "Briggs",
            "Irvin", "Bassett", "Urban", "Overton", "Sloan", "Guzman", "Estrada", "Redding", "Gold", "Paz",
            "Lucero", "Reyes", "Conner", "Keith", "Hays", "Hutton", "Moore", "Tuttle", "Powers", "William",
            "Reeder", "Castro", "Vela", "Baca", "Frazier", "Kline", "Huggins", "Zepeda", "Pate", "Marion",
            "Hollis", "Ybarra", "Cuellar", "Gallo", "Gomes", "Louis", "Bolden", "Liu", "Hayes", "Simpson",
            "Otero", "Conway", "Peoples", "Mohr", "Loomis", "Engel", "Hughes", "Chapman", "Bower", "Schmidt",
            "Mcmahon", "Booth", "Ward", "Drake", "Cutler", "Gordon", "Black", "Clifton", "Curran", "Pierre",
            "Workman", "Tilley", "Moody", "Kuhn", "Eastman", "Scott", "Vang", "Tillman", "Cleary", "Yi",
            "Griffin", "Mendoza", "Mason", "Pittman", "Landis", "Gay", "Rowland", "Jordan", "Lo", "Kumar",
            "Vernon", "Boyd", "Rocha", "Mcqueen", "Cornett", "Deaton", "Borden", "Lacey", "Gaston", "Miles"
    };

    public static String nextFirstName() {
        return randomElement(FIRST_NAMES);
    }

    public static String nextLastName() {
        return randomElement(LAST_NAMES);
    }

    private static String toEmail(String firstName, String lastName) {
        return firstName.toLowerCase() + "." + lastName.toLowerCase() + "@thingsboard.io";
    }

    public static RandomNameData next() {
        var firstName = nextFirstName();
        var lastName = nextLastName();
        return new RandomNameData(firstName, lastName, toEmail(firstName, lastName));
    }

    public static RandomNameData nextSuperRandom() {
        var firstName = nextFirstName() + StringUtils.randomAlphanumeric(10).toLowerCase();
        var lastName = nextLastName() + StringUtils.randomAlphanumeric(10).toLowerCase();
        return new RandomNameData(firstName, lastName, toEmail(firstName, lastName));
    }

    private static String randomElement(String[] array) {
        return array[RANDOM.nextInt(array.length)];
    }

}
