///
/// Copyright © 2016-2022 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Injectable } from '@angular/core';

export interface Country {
  name: string;
  iso2: string;
  dialCode: string;
  areaCodes?: string[];
  flag: string;
}

export enum SearchCountryField {
  DialCode = 'dialCode',
  Iso2 = 'iso2',
  Name = 'name',
  All = 'all'
}


export enum CountryISO{
  Afghanistan = 'AF',
  Albania = 'AL',
  Algeria = 'DZ',
  AmericanSamoa = 'AS',
  Andorra = 'AD',
  Angola = 'AO',
  Anguilla = 'AI',
  AntiguaAndBarbuda = 'AG',
  Argentina = 'AR',
  Armenia = 'AM',
  Aruba = 'AW',
  Australia = 'AU',
  Austria = 'AT',
  Azerbaijan = 'AZ',
  Bahamas = 'BS',
  Bahrain = 'BH',
  Bangladesh = 'BD',
  Barbados = 'BB',
  Belarus = 'BY',
  Belgium = 'BE',
  Belize = 'BZ',
  Benin = 'BJ',
  Bermuda = 'BM',
  Bhutan = 'BT',
  Bolivia = 'BO',
  BosniaAndHerzegovina = 'BA',
  Botswana = 'BW',
  Brazil = 'BR',
  BritishIndianOceanTerritory = 'IO',
  BritishVirginIslands = 'VG',
  Brunei = 'BN',
  Bulgaria = 'BG',
  BurkinaFaso = 'BF',
  Burundi = 'BI',
  Cambodia = 'KH',
  Cameroon = 'CM',
  Canada = 'CA',
  CapeVerde = 'CV',
  CaribbeanNetherlands = 'BQ',
  CaymanIslands = 'KY',
  CentralAfricanRepublic = 'CF',
  Chad = 'TD',
  Chile = 'CL',
  China = 'CN',
  ChristmasIsland = 'CX',
  Cocos = 'CC',
  Colombia = 'CC',
  Comoros = 'KM',
  CongoDRCJamhuriYaKidemokrasiaYaKongo = 'CD',
  CongoRepublicCongoBrazzaville = 'CG',
  CookIslands = 'CK',
  CostaRica = 'CR',
  CôteDIvoire = 'CI',
  Croatia = 'HR',
  Cuba = 'CU',
  Curaçao = 'CW',
  Cyprus = 'CY',
  CzechRepublic = 'CZ',
  Denmark = 'DK',
  Djibouti = 'DJ',
  Dominica = 'DM',
  DominicanRepublic = 'DO',
  Ecuador = 'EC',
  Egypt = 'EG',
  ElSalvador = 'SV',
  EquatorialGuinea = 'GQ',
  Eritrea = 'ER',
  Estonia = 'EE',
  Ethiopia = 'ET',
  FalklandIslands = 'FK',
  FaroeIslands = 'FO',
  Fiji = 'FJ',
  Finland = 'FI',
  France = 'FR',
  FrenchGuiana = 'GF',
  FrenchPolynesia = 'PF',
  Gabon = 'GA',
  Gambia = 'GM',
  Georgia = 'GE',
  Germany = 'DE',
  Ghana = 'GH',
  Gibraltar = 'GI',
  Greece = 'GR',
  Greenland = 'GL',
  Grenada = 'GD',
  Guadeloupe = 'GP',
  Guam = 'GU',
  Guatemala = 'GT',
  Guernsey = 'GG',
  Guinea = 'GN',
  GuineaBissau = 'GW',
  Guyana = 'GY',
  Haiti = 'HT',
  Honduras = 'HN',
  HongKong = 'HK',
  Hungary = 'HU',
  Iceland = 'IS',
  India = 'IN',
  Indonesia = 'ID',
  Iran = 'IR',
  Iraq = 'IQ',
  Ireland = 'IE',
  IsleOfMan = 'IM',
  Israel = 'IL',
  Italy = 'IT',
  Jamaica = 'JM',
  Japan = 'JP',
  Jersey = 'JE',
  Jordan = 'JO',
  Kazakhstan = 'KZ',
  Kenya = 'KE',
  Kiribati = 'KI',
  Kosovo = 'XK',
  Kuwait = 'KW',
  Kyrgyzstan = 'KG',
  Laos = 'LA',
  Latvia = 'LV',
  Lebanon = 'LB',
  Lesotho = 'LS',
  Liberia = 'LR',
  Libya = 'LY',
  Liechtenstein = 'LI',
  Lithuania = 'LT',
  Luxembourg = 'LU',
  Macau = 'MO',
  Macedonia = 'MK',
  Madagascar = 'MG',
  Malawi = 'MW',
  Malaysia = 'MY',
  Maldives = 'MV',
  Mali = 'ML',
  Malta = 'MT',
  MarshallIslands = 'MH',
  Martinique = 'MQ',
  Mauritania = 'MR',
  Mauritius = 'MU',
  Mayotte = 'YT',
  Mexico = 'MX',
  Micronesia = 'FM',
  Moldova = 'MD',
  Monaco = 'MC',
  Mongolia = 'MN',
  Montenegro = 'ME',
  Montserrat = 'MS',
  Morocco = 'MA',
  Mozambique = 'MZ',
  Myanmar = 'MM',
  Namibia = 'NA',
  Nauru = 'NR',
  Nepal = 'NP',
  Netherlands = 'NL',
  NewCaledonia = 'NC',
  NewZealand = 'NZ',
  Nicaragua = 'NI',
  Niger = 'NE',
  Nigeria = 'NG',
  Niue = 'NU',
  NorfolkIsland = 'NF',
  NorthKorea = 'KP',
  NorthernMarianaIslands = 'MP',
  Norway = 'NO',
  Oman = 'OM',
  Pakistan = 'PK',
  Palau = 'PW',
  Palestine = 'PS',
  Panama = 'PA',
  PapuaNewGuinea = 'PG',
  Paraguay = 'PY',
  Peru = 'PE',
  Philippines = 'PH',
  Poland = 'PL',
  Portugal = 'PT',
  PuertoRico = 'PR',
  Qatar = 'QA',
  Réunion = 'RE',
  Romania = 'RO',
  Russia = 'RU',
  Rwanda = 'RW',
  SaintBarthélemy = 'BL',
  SaintHelena = 'SH',
  SaintKittsAndNevis = 'KN',
  SaintLucia = 'LC',
  SaintMartin = 'MF',
  SaintPierreAndMiquelon = 'PM',
  SaintVincentAndTheGrenadines = 'VC',
  Samoa = 'WS',
  SanMarino = 'SM',
  SãoToméAndPríncipe = 'ST',
  SaudiArabia = 'SA',
  Senegal = 'SN',
  Serbia = 'RS',
  Seychelles = 'SC',
  SierraLeone = 'SL',
  Singapore = 'SG',
  SintMaarten = 'SX',
  Slovakia = 'SK',
  Slovenia = 'SI',
  SolomonIslands = 'SB',
  Somalia = 'SO',
  SouthAfrica = 'ZA',
  SouthKorea = 'KR',
  SouthSudan = 'SS',
  Spain = 'ES',
  SriLanka = 'LK',
  Sudan = 'SD',
  Suriname = 'SR',
  SvalbardAndJanMayen = 'SJ',
  Swaziland = 'SZ',
  Sweden = 'SE',
  Switzerland = 'CH',
  Syria = 'SY',
  Taiwan = 'TW',
  Tajikistan = 'TJ',
  Tanzania = 'TZ',
  Thailand = 'TH',
  TimorLeste = 'TL',
  Togo = 'TG',
  Tokelau = 'TK',
  Tonga = 'TO',
  TrinidadAndTobago = 'TT',
  Tunisia = 'TN',
  Turkey = 'TR',
  Turkmenistan = 'TM',
  TurksAndCaicosIslands = 'TC',
  Tuvalu = 'TV',
  USVirginIslands = 'VI',
  Uganda = 'UG',
  Ukraine = 'UA',
  UnitedArabEmirates = 'AE',
  UnitedKingdom = 'GB',
  UnitedStates = 'US',
  Uruguay = 'UY',
  Uzbekistan = 'UZ',
  Vanuatu = 'VU',
  VaticanCity = 'VA',
  Venezuela = 'VE',
  Vietnam = 'VN',
  WallisAndFutuna = 'WF',
  WesternSahara = 'EH',
  Yemen = 'YE',
  Zambia = 'ZM',
  Zimbabwe = 'ZW',
  ÅlandIslands = 'AX',
}

@Injectable()
export class CountryData {
  public allCountries = [
    [
      'Afghanistan',
      CountryISO.Afghanistan,
      '93'
    ],
    [
      'Albania',
      CountryISO.Albania,
      '355'
    ],
    [
      'Algeria',
      CountryISO.Algeria,
      '213'
    ],
    [
      'American Samoa',
      CountryISO.AmericanSamoa,
      '1',
      1,
      [
        '684',
      ]
    ],
    [
      'Andorra',
      CountryISO.Andorra,
      '376'
    ],
    [
      'Angola',
      CountryISO.Angola,
      '244'
    ],
    [
      'Anguilla',
      CountryISO.Anguilla,
      '1',
      1,
      [
        '264',
      ]
    ],
    [
      'Antigua and Barbuda',
      CountryISO.AntiguaAndBarbuda,
      '1',
      1,
      [
        '268',
      ]
    ],
    [
      'Argentina',
      CountryISO.Argentina,
      '54'
    ],
    [
      'Armenia',
      CountryISO.Armenia,
      '374'
    ],
    [
      'Aruba',
      CountryISO.Aruba,
      '297'
    ],
    [
      'Australia',
      CountryISO.Australia,
      '61',
      0
    ],
    [
      'Austria',
      CountryISO.Austria,
      '43'
    ],
    [
      'Azerbaijan',
      CountryISO.Azerbaijan,
      '994'
    ],
    [
      'Bahamas',
      CountryISO.Bahamas,
      '1',
      1,
      [
        '242',
      ]
    ],
    [
      'Bahrain',
      CountryISO.Bahrain,
      '973'
    ],
    [
      'Bangladesh',
      CountryISO.Bangladesh,
      '880'
    ],
    [
      'Barbados',
      CountryISO.Barbados,
      '1',
      1,
      [
        '246',
      ]
    ],
    [
      'Belarus',
      CountryISO.Belarus,
      '375'
    ],
    [
      'Belgium',
      CountryISO.Belgium,
      '32'
    ],
    [
      'Belize',
      CountryISO.Belize,
      '501'
    ],
    [
      'Benin',
      CountryISO.Benin,
      '229'
    ],
    [
      'Bermuda',
      CountryISO.Bermuda,
      '1',
      1,
      [
        '441',
      ]
    ],
    [
      'Bhutan',
      CountryISO.Bhutan,
      '975'
    ],
    [
      'Bolivia',
      CountryISO.Bolivia,
      '591'
    ],
    [
      'Bosnia and Herzegovina',
      CountryISO.BosniaAndHerzegovina,
      '387'
    ],
    [
      'Botswana',
      CountryISO.Botswana,
      '267'
    ],
    [
      'Brazil',
      CountryISO.Brazil,
      '55'
    ],
    [
      'British Indian Ocean Territory',
      CountryISO.BritishIndianOceanTerritory,
      '246'
    ],
    [
      'British Virgin Islands',
      CountryISO.BritishVirginIslands,
      '1',
      1,
      [
        '284',
      ]
    ],
    [
      'Brunei',
      CountryISO.Brunei,
      '673'
    ],
    [
      'Bulgaria',
      CountryISO.Bulgaria,
      '359'
    ],
    [
      'Burkina Faso',
      CountryISO.BurkinaFaso,
      '226'
    ],
    [
      'Burundi',
      CountryISO.Burundi,
      '257'
    ],
    [
      'Cambodia',
      CountryISO.Cambodia,
      '855'
    ],
    [
      'Cameroon',
      CountryISO.Cameroon,
      '237'
    ],
    [
      'Canada',
      CountryISO.Canada,
      '1',
      1,
      [
        '204', '226', '236', '249', '250', '289', '306', '343', '365', '387', '403', '416',
        '418', '431', '437', '438', '450', '506', '514', '519', '548', '579', '581', '587',
        '604', '613', '639', '647', '672', '705', '709', '742', '778', '780', '782', '807',
        '819', '825', '867', '873', '902', '905'
      ]
    ],
    [
      'Cape Verde',
      CountryISO.CapeVerde,
      '238'
    ],
    [
      'Caribbean Netherlands',
      CountryISO.CaribbeanNetherlands,
      '599',
      1
    ],
    [
      'Cayman Islands',
      CountryISO.CaymanIslands,
      '1',
      1,
      [
        '345',
      ]
    ],
    [
      'Central African Republic',
      CountryISO.CentralAfricanRepublic,
      '236'
    ],
    [
      'Chad',
      CountryISO.Chad,
      '235'
    ],
    [
      'Chile',
      CountryISO.Chile,
      '56'
    ],
    [
      'China',
      CountryISO.China,
      '86'
    ],
    [
      'Christmas Island',
      CountryISO.ChristmasIsland,
      '61',
      2
    ],
    [
      'Cocos Islands',
      CountryISO.Cocos,
      '61',
      1
    ],
    [
      'Colombia',
      CountryISO.Colombia,
      '57'
    ],
    [
      'Comoros',
      CountryISO.Comoros,
      '269'
    ],
    [
      'Congo-Kinshasa',
      CountryISO.CongoDRCJamhuriYaKidemokrasiaYaKongo,
      '243'
    ],
    [
      'Congo-Brazzaville',
      CountryISO.CongoRepublicCongoBrazzaville,
      '242'
    ],
    [
      'Cook Islands',
      CountryISO.CookIslands,
      '682'
    ],
    [
      'Costa Rica',
      CountryISO.CostaRica,
      '506'
    ],
    [
      'Côte d’Ivoire',
      CountryISO.CôteDIvoire,
      '225'
    ],
    [
      'Croatia',
      CountryISO.Croatia,
      '385'
    ],
    [
      'Cuba',
      CountryISO.Cuba,
      '53'
    ],
    [
      'Curaçao',
      CountryISO.Curaçao,
      '599',
      0
    ],
    [
      'Cyprus',
      CountryISO.Cyprus,
      '357'
    ],
    [
      'Czech Republic',
      CountryISO.CzechRepublic,
      '420'
    ],
    [
      'Denmark',
      CountryISO.Denmark,
      '45'
    ],
    [
      'Djibouti',
      CountryISO.Djibouti,
      '253'
    ],
    [
      'Dominica',
      CountryISO.Dominica,
      '1767'
    ],
    [
      'Dominican Republic',
      CountryISO.DominicanRepublic,
      '1',
      2,
      ['809', '829', '849']
    ],
    [
      'Ecuador',
      CountryISO.Ecuador,
      '593'
    ],
    [
      'Egypt',
      CountryISO.Egypt,
      '20'
    ],
    [
      'El Salvador',
      CountryISO.ElSalvador,
      '503'
    ],
    [
      'Equatorial Guinea',
      CountryISO.EquatorialGuinea,
      '240'
    ],
    [
      'Eritrea',
      CountryISO.Eritrea,
      '291'
    ],
    [
      'Estonia',
      CountryISO.Estonia,
      '372'
    ],
    [
      'Ethiopia',
      CountryISO.Ethiopia,
      '251'
    ],
    [
      'Falkland Islands',
      CountryISO.FalklandIslands,
      '500'
    ],
    [
      'Faroe Islands',
      CountryISO.FaroeIslands,
      '298'
    ],
    [
      'Fiji',
      CountryISO.Fiji,
      '679'
    ],
    [
      'Finland',
      CountryISO.Finland,
      '358',
      0
    ],
    [
      'France',
      CountryISO.France,
      '33'
    ],
    [
      'French Guiana',
      CountryISO.FrenchGuiana,
      '594'
    ],
    [
      'French Polynesia',
      CountryISO.FrenchPolynesia,
      '689'
    ],
    [
      'Gabon',
      CountryISO.Gabon,
      '241'
    ],
    [
      'Gambia',
      CountryISO.Gambia,
      '220'
    ],
    [
      'Georgia',
      CountryISO.Georgia,
      '995'
    ],
    [
      'Germany',
      CountryISO.Germany,
      '49'
    ],
    [
      'Ghana',
      CountryISO.Ghana,
      '233'
    ],
    [
      'Gibraltar',
      CountryISO.Gibraltar,
      '350'
    ],
    [
      'Greece',
      CountryISO.Greece,
      '30'
    ],
    [
      'Greenland',
      CountryISO.Greenland,
      '299'
    ],
    [
      'Grenada',
      CountryISO.Grenada,
      '1473'
    ],
    [
      'Guadeloupe',
      CountryISO.Guadeloupe,
      '590',
      0
    ],
    [
      'Guam',
      CountryISO.Guam,
      '1',
      1,
      [
        '671',
      ]
    ],
    [
      'Guatemala',
      CountryISO.Guatemala,
      '502'
    ],
    [
      'Guernsey',
      CountryISO.Guernsey,
      '44',
      1,
      [1481]
    ],
    [
      'Guinea',
      CountryISO.Guinea,
      '224'
    ],
    [
      'Guinea-Bissau',
      CountryISO.GuineaBissau,
      '245'
    ],
    [
      'Guyana',
      CountryISO.Guyana,
      '592'
    ],
    [
      'Haiti',
      CountryISO.Haiti,
      '509'
    ],
    [
      'Honduras',
      CountryISO.Honduras,
      '504'
    ],
    [
      'Hong Kong',
      CountryISO.HongKong,
      '852'
    ],
    [
      'Hungary',
      CountryISO.Hungary,
      '36'
    ],
    [
      'Iceland',
      CountryISO.Iceland,
      '354'
    ],
    [
      'India',
      CountryISO.India,
      '91'
    ],
    [
      'Indonesia',
      CountryISO.Indonesia,
      '62'
    ],
    [
      'Iran',
      CountryISO.Iran,
      '98'
    ],
    [
      'Iraq',
      CountryISO.Iraq,
      '964'
    ],
    [
      'Ireland',
      CountryISO.Ireland,
      '353'
    ],
    [
      'Isle of Man',
      CountryISO.IsleOfMan,
      '44',
      2,
      [1624]
    ],
    [
      'Israel',
      CountryISO.Israel,
      '972'
    ],
    [
      'Italy',
      CountryISO.Italy,
      '39',
      0
    ],
    [
      'Jamaica',
      CountryISO.Jamaica,
      '1',
      1,
      [
        '876',
      ]
    ],
    [
      'Japan',
      CountryISO.Japan,
      '81'
    ],
    [
      'Jersey',
      CountryISO.Jersey,
      '44',
      3,
      [1534]
    ],
    [
      'Jordan',
      CountryISO.Jordan,
      '962'
    ],
    [
      'Kazakhstan',
      CountryISO.Kazakhstan,
      '7',
      1
    ],
    [
      'Kenya',
      CountryISO.Kenya,
      '254'
    ],
    [
      'Kiribati',
      CountryISO.Kiribati,
      '686'
    ],
    [
      'Kosovo',
      CountryISO.Kosovo,
      '383'
    ],
    [
      'Kuwait',
      CountryISO.Kuwait,
      '965'
    ],
    [
      'Kyrgyzstan',
      CountryISO.Kyrgyzstan,
      '996'
    ],
    [
      'Laos',
      CountryISO.Laos,
      '856'
    ],
    [
      'Latvia',
      CountryISO.Latvia,
      '371'
    ],
    [
      'Lebanon',
      CountryISO.Lebanon,
      '961'
    ],
    [
      'Lesotho',
      CountryISO.Lesotho,
      '266'
    ],
    [
      'Liberia',
      CountryISO.Liberia,
      '231'
    ],
    [
      'Libya',
      CountryISO.Libya,
      '218'
    ],
    [
      'Liechtenstein',
      CountryISO.Liechtenstein,
      '423'
    ],
    [
      'Lithuania',
      CountryISO.Lithuania,
      '370'
    ],
    [
      'Luxembourg',
      CountryISO.Luxembourg,
      '352'
    ],
    [
      'Macau',
      CountryISO.Macau,
      '853'
    ],
    [
      'Macedonia',
      CountryISO.Macedonia,
      '389'
    ],
    [
      'Madagascar',
      CountryISO.Madagascar,
      '261'
    ],
    [
      'Malawi',
      CountryISO.Malawi,
      '265'
    ],
    [
      'Malaysia',
      CountryISO.Malaysia,
      '60'
    ],
    [
      'Maldives',
      CountryISO.Maldives,
      '960'
    ],
    [
      'Mali',
      CountryISO.Mali,
      '223'
    ],
    [
      'Malta',
      CountryISO.Malta,
      '356'
    ],
    [
      'Marshall Islands',
      CountryISO.MarshallIslands,
      '692'
    ],
    [
      'Martinique',
      CountryISO.Martinique,
      '596'
    ],
    [
      'Mauritania',
      CountryISO.Mauritania,
      '222'
    ],
    [
      'Mauritius',
      CountryISO.Mauritius,
      '230'
    ],
    [
      'Mayotte',
      CountryISO.Mayotte,
      '262',
      1
    ],
    [
      'Mexico',
      CountryISO.Mexico,
      '52'
    ],
    [
      'Micronesia',
      CountryISO.Micronesia,
      '691'
    ],
    [
      'Moldova',
      CountryISO.Moldova,
      '373'
    ],
    [
      'Monaco',
      CountryISO.Monaco,
      '377'
    ],
    [
      'Mongolia',
      CountryISO.Mongolia,
      '976'
    ],
    [
      'Montenegro',
      CountryISO.Montenegro,
      '382'
    ],
    [
      'Montserrat',
      CountryISO.Montserrat,
      '1',
      1,
      [
        '664',
      ]
    ],
    [
      'Morocco',
      CountryISO.Morocco,
      '212',
      0
    ],
    [
      'Mozambique',
      CountryISO.Mozambique,
      '258'
    ],
    [
      'Myanmar',
      CountryISO.Myanmar,
      '95'
    ],
    [
      'Namibia',
      CountryISO.Namibia,
      '264'
    ],
    [
      'Nauru',
      CountryISO.Nauru,
      '674'
    ],
    [
      'Nepal',
      CountryISO.Nepal,
      '977'
    ],
    [
      'Netherlands',
      CountryISO.Netherlands,
      '31'
    ],
    [
      'New Caledonia',
      CountryISO.NewCaledonia,
      '687'
    ],
    [
      'New Zealand',
      CountryISO.NewZealand,
      '64'
    ],
    [
      'Nicaragua',
      CountryISO.Nicaragua,
      '505'
    ],
    [
      'Niger',
      CountryISO.Niger,
      '227'
    ],
    [
      'Nigeria',
      CountryISO.Nigeria,
      '234'
    ],
    [
      'Niue',
      CountryISO.Niue,
      '683'
    ],
    [
      'Norfolk Island',
      CountryISO.NorfolkIsland,
      '672'
    ],
    [
      'North Korea',
      CountryISO.NorthKorea,
      '850'
    ],
    [
      'Northern Mariana Islands',
      CountryISO.NorthernMarianaIslands,
      '1670'
    ],
    [
      'Norway',
      CountryISO.Norway,
      '47',
      0
    ],
    [
      'Oman',
      CountryISO.Oman,
      '968'
    ],
    [
      'Pakistan',
      CountryISO.Pakistan,
      '92'
    ],
    [
      'Palau',
      CountryISO.Palau,
      '680'
    ],
    [
      'Palestine',
      CountryISO.Palestine,
      '970'
    ],
    [
      'Panama',
      CountryISO.Panama,
      '507'
    ],
    [
      'Papua New Guinea',
      CountryISO.PapuaNewGuinea,
      '675'
    ],
    [
      'Paraguay',
      CountryISO.Paraguay,
      '595'
    ],
    [
      'Peru',
      CountryISO.Peru,
      '51'
    ],
    [
      'Philippines',
      CountryISO.Philippines,
      '63'
    ],
    [
      'Poland',
      CountryISO.Poland,
      '48'
    ],
    [
      'Portugal',
      CountryISO.Portugal,
      '351'
    ],
    [
      'Puerto Rico',
      CountryISO.PuertoRico,
      '1',
      3,
      ['787', '939']
    ],
    [
      'Qatar',
      CountryISO.Qatar,
      '974'
    ],
    [
      'Réunion',
      CountryISO.Réunion,
      '262',
      0
    ],
    [
      'Romania',
      CountryISO.Romania,
      '40'
    ],
    [
      'Russia',
      CountryISO.Russia,
      '7',
      0
    ],
    [
      'Rwanda',
      CountryISO.Rwanda,
      '250'
    ],
    [
      'Saint Barthélemy',
      CountryISO.SaintBarthélemy,
      '590',
      1
    ],
    [
      'Saint Helena',
      CountryISO.SaintHelena,
      '290'
    ],
    [
      'Saint Kitts and Nevis',
      CountryISO.SaintKittsAndNevis,
      '1869'
    ],
    [
      'Saint Lucia',
      CountryISO.SaintLucia,
      '1',
      1,
      [
        '758',
      ]
    ],
    [
      'Saint Martin',
      CountryISO.SaintMartin,
      '590',
      2
    ],
    [
      'Saint Pierre and Miquelon',
      CountryISO.SaintPierreAndMiquelon,
      '508'
    ],
    [
      'Saint Vincent and the Grenadines',
      CountryISO.SaintVincentAndTheGrenadines,
      '1',
      1,
      [
        '784',
      ]
    ],
    [
      'Samoa',
      CountryISO.Samoa,
      '685'
    ],
    [
      'San Marino',
      CountryISO.SanMarino,
      '378'
    ],
    [
      'São Tomé and Príncipe',
      CountryISO.SãoToméAndPríncipe,
      '239'
    ],
    [
      'Saudi Arabia',
      CountryISO.SaudiArabia,
      '966'
    ],
    [
      'Senegal',
      CountryISO.Senegal,
      '221'
    ],
    [
      'Serbia',
      CountryISO.Serbia,
      '381'
    ],
    [
      'Seychelles',
      CountryISO.Seychelles,
      '248'
    ],
    [
      'Sierra Leone',
      CountryISO.SierraLeone,
      '232'
    ],
    [
      'Singapore',
      CountryISO.Singapore,
      '65'
    ],
    [
      'Sint Maarten',
      CountryISO.SintMaarten,
      '1',
      1,
      [
        '721',
      ]
    ],
    [
      'Slovakia',
      CountryISO.Slovakia,
      '421'
    ],
    [
      'Slovenia',
      CountryISO.Slovenia,
      '386'
    ],
    [
      'Solomon Islands',
      CountryISO.SolomonIslands,
      '677'
    ],
    [
      'Somalia',
      CountryISO.Somalia,
      '252'
    ],
    [
      'South Africa',
      CountryISO.SouthAfrica,
      '27'
    ],
    [
      'South Korea',
      CountryISO.SouthKorea,
      '82'
    ],
    [
      'South Sudan',
      CountryISO.SouthSudan,
      '211'
    ],
    [
      'Spain',
      CountryISO.Spain,
      '34'
    ],
    [
      'Sri Lanka',
      CountryISO.SriLanka,
      '94'
    ],
    [
      'Sudan',
      CountryISO.Sudan,
      '249'
    ],
    [
      'Suriname',
      CountryISO.Suriname,
      '597'
    ],
    [
      'Svalbard and Jan Mayen',
      CountryISO.SvalbardAndJanMayen,
      '47',
      1
    ],
    [
      'Swaziland',
      CountryISO.Swaziland,
      '268'
    ],
    [
      'Sweden',
      CountryISO.Sweden,
      '46'
    ],
    [
      'Switzerland',
      CountryISO.Switzerland,
      '41'
    ],
    [
      'Syria',
      CountryISO.Syria,
      '963'
    ],
    [
      'Taiwan',
      CountryISO.Taiwan,
      '886'
    ],
    [
      'Tajikistan',
      CountryISO.Tajikistan,
      '992'
    ],
    [
      'Tanzania',
      CountryISO.Tanzania,
      '255'
    ],
    [
      'Thailand',
      CountryISO.Thailand,
      '66'
    ],
    [
      'Timor-Leste',
      CountryISO.TimorLeste,
      '670'
    ],
    [
      'Togo',
      CountryISO.Togo,
      '228'
    ],
    [
      'Tokelau',
      CountryISO.Tokelau,
      '690'
    ],
    [
      'Tonga',
      CountryISO.Tonga,
      '676'
    ],
    [
      'Trinidad and Tobago',
      CountryISO.TrinidadAndTobago,
      '1',
      1,
      [
        '868',
      ]
    ],
    [
      'Tunisia',
      CountryISO.Tunisia,
      '216'
    ],
    [
      'Turkey',
      CountryISO.Turkey,
      '90'
    ],
    [
      'Turkmenistan',
      CountryISO.Turkmenistan,
      '993'
    ],
    [
      'Turks and Caicos Islands',
      CountryISO.TurksAndCaicosIslands,
      '1649'
    ],
    [
      'Tuvalu',
      CountryISO.Tuvalu,
      '688'
    ],
    [
      'U.S. Virgin Islands',
      CountryISO.USVirginIslands,
      '1',
      1,
      [
        '340',
      ]
    ],
    [
      'Uganda',
      CountryISO.Uganda,
      '256'
    ],
    [
      'Ukraine',
      CountryISO.Ukraine,
      '380'
    ],
    [
      'United Arab Emirates',
      CountryISO.UnitedArabEmirates,
      '971'
    ],
    [
      'United Kingdom',
      CountryISO.UnitedKingdom,
      '44',
      0
    ],
    [
      'United States',
      CountryISO.UnitedStates,
      '1',
      0
    ],
    [
      'Uruguay',
      CountryISO.Uruguay,
      '598'
    ],
    [
      'Uzbekistan',
      CountryISO.Uzbekistan,
      '998'
    ],
    [
      'Vanuatu',
      CountryISO.Vanuatu,
      '678'
    ],
    [
      'Vatican City',
      CountryISO.VaticanCity,
      '39',
      1
    ],
    [
      'Venezuela',
      CountryISO.Venezuela,
      '58'
    ],
    [
      'Vietnam',
      CountryISO.Vietnam,
      '84'
    ],
    [
      'Wallis and Futuna',
      CountryISO.WallisAndFutuna,
      '681'
    ],
    [
      'Western Sahara',
      CountryISO.WesternSahara,
      '212',
      1
    ],
    [
      'Yemen',
      CountryISO.Yemen,
      '967'
    ],
    [
      'Zambia',
      CountryISO.Zambia,
      '260'
    ],
    [
      'Zimbabwe',
      CountryISO.Zimbabwe,
      '263'
    ],
    [
      'Åland Islands',
      CountryISO.ÅlandIslands,
      '358',
      1
    ]
  ];
}
