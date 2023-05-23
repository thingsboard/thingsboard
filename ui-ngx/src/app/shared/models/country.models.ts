///
/// Copyright © 2016-2023 The Thingsboard Authors
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

export enum CountryISO {
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
  'Türkiye' = 'TR',
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
  public allCountries: Array<Country> = [
    {name: 'Afghanistan', iso2: CountryISO.Afghanistan, dialCode: '93', flag: '🇦🇫'},
    {name: 'Albania', iso2: CountryISO.Albania, dialCode: '355', flag: '🇦🇱'},
    {name: 'Algeria', iso2: CountryISO.Algeria, dialCode: '213', flag: '🇩🇿'},
    {name: 'American Samoa', iso2: CountryISO.AmericanSamoa, dialCode: '1', flag: '🇦🇸'},
    {name: 'Andorra', iso2: CountryISO.Andorra, dialCode: '376', flag: '🇦🇩'},
    {name: 'Angola', iso2: CountryISO.Angola, dialCode: '244', flag: '🇦🇴'},
    {name: 'Anguilla', iso2: CountryISO.Anguilla, dialCode: '1', flag: '🇦🇮'},
    {name: 'Antigua and Barbuda', iso2: CountryISO.AntiguaAndBarbuda, dialCode: '1', flag: '🇦🇬'},
    {name: 'Argentina', iso2: CountryISO.Argentina, dialCode: '54', flag: '🇦🇷'},
    {name: 'Armenia', iso2: CountryISO.Armenia, dialCode: '374', flag: '🇦🇲'},
    {name: 'Aruba', iso2: CountryISO.Aruba, dialCode: '297', flag: '🇦🇼'},
    {name: 'Australia', iso2: CountryISO.Australia, dialCode: '61', flag: '🇦🇺'},
    {name: 'Austria', iso2: CountryISO.Austria, dialCode: '43', flag: '🇦🇹'},
    {name: 'Azerbaijan', iso2: CountryISO.Azerbaijan, dialCode: '994', flag: '🇦🇿'},
    {name: 'Bahamas', iso2: CountryISO.Bahamas, dialCode: '1', flag: '🇧🇸'},
    {name: 'Bahrain', iso2: CountryISO.Bahrain, dialCode: '973', flag: '🇧🇭'},
    {name: 'Bangladesh', iso2: CountryISO.Bangladesh, dialCode: '880', flag: '🇧🇩'},
    {name: 'Barbados', iso2: CountryISO.Barbados, dialCode: '1', flag: '🇧🇧'},
    {name: 'Belarus', iso2: CountryISO.Belarus, dialCode: '375', flag: '🇧🇾'},
    {name: 'Belgium', iso2: CountryISO.Belgium, dialCode: '32', flag: '🇧🇪'},
    {name: 'Belize', iso2: CountryISO.Belize, dialCode: '501', flag: '🇧🇿'},
    {name: 'Benin', iso2: CountryISO.Benin, dialCode: '229', flag: '🇧🇯'},
    {name: 'Bermuda', iso2: CountryISO.Bermuda, dialCode: '1', flag: '🇧🇲'},
    {name: 'Bhutan', iso2: CountryISO.Bhutan, dialCode: '975', flag: '🇧🇹'},
    {name: 'Bolivia', iso2: CountryISO.Bolivia, dialCode: '591', flag: '🇧🇴'},
    {name: 'Bosnia and Herzegovina', iso2: CountryISO.BosniaAndHerzegovina, dialCode: '387', flag: '🇧🇦'},
    {name: 'Botswana', iso2: CountryISO.Botswana, dialCode: '267', flag: '🇧🇼'},
    {name: 'Brazil', iso2: CountryISO.Brazil, dialCode: '55', flag: '🇧🇷'},
    {name: 'British Indian Ocean Territory', iso2: CountryISO.BritishIndianOceanTerritory, dialCode: '246', flag: '🇮🇴'},
    {name: 'British Virgin Islands', iso2: CountryISO.BritishVirginIslands, dialCode: '1', flag: '🇻🇬'},
    {name: 'Brunei', iso2: CountryISO.Brunei, dialCode: '673', flag: '🇧🇳'},
    {name: 'Bulgaria', iso2: CountryISO.Bulgaria, dialCode: '359', flag: '🇧🇬'},
    {name: 'Burkina Faso', iso2: CountryISO.BurkinaFaso, dialCode: '226', flag: '🇧🇫'},
    {name: 'Burundi', iso2: CountryISO.Burundi, dialCode: '257', flag: '🇧🇮'},
    {name: 'Cambodia', iso2: CountryISO.Cambodia, dialCode: '855', flag: '🇰🇭'},
    {name: 'Cameroon', iso2: CountryISO.Cameroon, dialCode: '237', flag: '🇨🇲'},
    {name: 'Canada', iso2: CountryISO.Canada, dialCode: '1', flag: '🇨🇦'},
    {name: 'Cape Verde', iso2: CountryISO.CapeVerde, dialCode: '238', flag: '🇨🇻'},
    {name: 'Caribbean Netherlands', iso2: CountryISO.CaribbeanNetherlands, dialCode: '599', flag: '🇧🇶'},
    {name: 'Cayman Islands', iso2: CountryISO.CaymanIslands, dialCode: '1', flag: '🇰🇾'},
    {name: 'Central African Republic', iso2: CountryISO.CentralAfricanRepublic, dialCode: '236', flag: '🇨🇫'},
    {name: 'Chad', iso2: CountryISO.Chad, dialCode: '235', flag: '🇹🇩'},
    {name: 'Chile', iso2: CountryISO.Chile, dialCode: '56', flag: '🇨🇱'},
    {name: 'China', iso2: CountryISO.China, dialCode: '86', flag: '🇨🇳'},
    {name: 'Christmas Island', iso2: CountryISO.ChristmasIsland, dialCode: '61', flag: '🇨🇽'},
    {name: 'Cocos Islands', iso2: CountryISO.Cocos, dialCode: '61', flag: '🇨🇨'},
    {name: 'Colombia', iso2: CountryISO.Colombia, dialCode: '57', flag: '🇨🇨'},
    {name: 'Comoros', iso2: CountryISO.Comoros, dialCode: '269', flag: '🇰🇲'},
    {name: 'Congo-Kinshasa', iso2: CountryISO.CongoDRCJamhuriYaKidemokrasiaYaKongo, dialCode: '243', flag: '🇨🇩'},
    {name: 'Congo-Brazzaville', iso2: CountryISO.CongoRepublicCongoBrazzaville, dialCode: '242', flag: '🇨🇬'},
    {name: 'Cook Islands', iso2: CountryISO.CookIslands, dialCode: '682', flag: '🇨🇰'},
    {name: 'Costa Rica', iso2: CountryISO.CostaRica, dialCode: '506', flag: '🇨🇷'},
    {name: 'Côte d’Ivoire', iso2: CountryISO.CôteDIvoire, dialCode: '225', flag: '🇨🇮'},
    {name: 'Croatia', iso2: CountryISO.Croatia, dialCode: '385', flag: '🇭🇷'},
    {name: 'Cuba', iso2: CountryISO.Cuba, dialCode: '53', flag: '🇨🇺'},
    {name: 'Curaçao', iso2: CountryISO.Curaçao, dialCode: '599', flag: '🇨🇼'},
    {name: 'Cyprus', iso2: CountryISO.Cyprus, dialCode: '357', flag: '🇨🇾'},
    {name: 'Czech Republic', iso2: CountryISO.CzechRepublic, dialCode: '420', flag: '🇨🇿'},
    {name: 'Denmark', iso2: CountryISO.Denmark, dialCode: '45', flag: '🇩🇰'},
    {name: 'Djibouti', iso2: CountryISO.Djibouti, dialCode: '253', flag: '🇩🇯'},
    {name: 'Dominica', iso2: CountryISO.Dominica, dialCode: '1767', flag: '🇩🇲'},
    {name: 'Dominican Republic', iso2: CountryISO.DominicanRepublic, dialCode: '1', flag: '🇩🇴'},
    {name: 'Ecuador', iso2: CountryISO.Ecuador, dialCode: '593', flag: '🇪🇨'},
    {name: 'Egypt', iso2: CountryISO.Egypt, dialCode: '20', flag: '🇪🇬'},
    {name: 'El Salvador', iso2: CountryISO.ElSalvador, dialCode: '503', flag: '🇸🇻'},
    {name: 'Equatorial Guinea', iso2: CountryISO.EquatorialGuinea, dialCode: '240', flag: '🇬🇶'},
    {name: 'Eritrea', iso2: CountryISO.Eritrea, dialCode: '291', flag: '🇪🇷'},
    {name: 'Estonia', iso2: CountryISO.Estonia, dialCode: '372', flag: '🇪🇪'},
    {name: 'Ethiopia', iso2: CountryISO.Ethiopia, dialCode: '251', flag: '🇪🇹'},
    {name: 'Falkland Islands', iso2: CountryISO.FalklandIslands, dialCode: '500', flag: '🇫🇰'},
    {name: 'Faroe Islands', iso2: CountryISO.FaroeIslands, dialCode: '298', flag: '🇫🇴'},
    {name: 'Fiji', iso2: CountryISO.Fiji, dialCode: '679', flag: '🇫🇯'},
    {name: 'Finland', iso2: CountryISO.Finland, dialCode: '358', flag: '🇫🇮'},
    {name: 'France', iso2: CountryISO.France, dialCode: '33', flag: '🇫🇷'},
    {name: 'French Guiana', iso2: CountryISO.FrenchGuiana, dialCode: '594', flag: '🇬🇫'},
    {name: 'French Polynesia', iso2: CountryISO.FrenchPolynesia, dialCode: '689', flag: '🇵🇫'},
    {name: 'Gabon', iso2: CountryISO.Gabon, dialCode: '241', flag: '🇬🇦'},
    {name: 'Gambia', iso2: CountryISO.Gambia, dialCode: '220', flag: '🇬🇲'},
    {name: 'Georgia', iso2: CountryISO.Georgia, dialCode: '995', flag: '🇬🇪'},
    {name: 'Germany', iso2: CountryISO.Germany, dialCode: '49', flag: '🇩🇪'},
    {name: 'Ghana', iso2: CountryISO.Ghana, dialCode: '233', flag: '🇬🇭'},
    {name: 'Gibraltar', iso2: CountryISO.Gibraltar, dialCode: '350', flag: '🇬🇮'},
    {name: 'Greece', iso2: CountryISO.Greece, dialCode: '30', flag: '🇬🇷'},
    {name: 'Greenland', iso2: CountryISO.Greenland, dialCode: '299', flag: '🇬🇱'},
    {name: 'Grenada', iso2: CountryISO.Grenada, dialCode: '1', flag: '🇬🇩'},
    {name: 'Guadeloupe', iso2: CountryISO.Guadeloupe, dialCode: '590', flag: '🇬🇵'},
    {name: 'Guam', iso2: CountryISO.Guam, dialCode: '1', flag: '🇬🇺'},
    {name: 'Guatemala', iso2: CountryISO.Guatemala, dialCode: '502', flag: '🇬🇹'},
    {name: 'Guernsey', iso2: CountryISO.Guernsey, dialCode: '44', flag: '🇬🇬'},
    {name: 'Guinea', iso2: CountryISO.Guinea, dialCode: '224', flag: '🇬🇳'},
    {name: 'Guinea-Bissau', iso2: CountryISO.GuineaBissau, dialCode: '245', flag: '🇬🇼'},
    {name: 'Guyana', iso2: CountryISO.Guyana, dialCode: '592', flag: '🇬🇾'},
    {name: 'Haiti', iso2: CountryISO.Haiti, dialCode: '509', flag: '🇭🇹'},
    {name: 'Honduras', iso2: CountryISO.Honduras, dialCode: '504', flag: '🇭🇳'},
    {name: 'Hong Kong', iso2: CountryISO.HongKong, dialCode: '852', flag: '🇭🇰'},
    {name: 'Hungary', iso2: CountryISO.Hungary, dialCode: '36', flag: '🇭🇺'},
    {name: 'Iceland', iso2: CountryISO.Iceland, dialCode: '354', flag: '🇮🇸'},
    {name: 'India', iso2: CountryISO.India, dialCode: '91', flag: '🇮🇳'},
    {name: 'Indonesia', iso2: CountryISO.Indonesia, dialCode: '62', flag: '🇮🇩'},
    {name: 'Iran', iso2: CountryISO.Iran, dialCode: '98', flag: '🇮🇷'},
    {name: 'Iraq', iso2: CountryISO.Iraq, dialCode: '964', flag: '🇮🇶'},
    {name: 'Ireland', iso2: CountryISO.Ireland, dialCode: '353', flag: '🇮🇪'},
    {name: 'Isle of Man', iso2: CountryISO.IsleOfMan, dialCode: '44', flag: '🇮🇲'},
    {name: 'Israel', iso2: CountryISO.Israel, dialCode: '972', flag: '🇮🇱'},
    {name: 'Italy', iso2: CountryISO.Italy, dialCode: '39', flag: '🇮🇹'},
    {name: 'Jamaica', iso2: CountryISO.Jamaica, dialCode: '1', flag: '🇯🇲'},
    {name: 'Japan', iso2: CountryISO.Japan, dialCode: '81', flag: '🇯🇵'},
    {name: 'Jersey', iso2: CountryISO.Jersey, dialCode: '44', flag: '🇯🇪'},
    {name: 'Jordan', iso2: CountryISO.Jordan, dialCode: '962', flag: '🇯🇴'},
    {name: 'Kazakhstan', iso2: CountryISO.Kazakhstan, dialCode: '7', flag: '🇰🇿'},
    {name: 'Kenya', iso2: CountryISO.Kenya, dialCode: '254', flag: '🇰🇪'},
    {name: 'Kiribati', iso2: CountryISO.Kiribati, dialCode: '686', flag: '🇰🇮'},
    {name: 'Kosovo', iso2: CountryISO.Kosovo, dialCode: '383', flag: '🇽🇰'},
    {name: 'Kuwait', iso2: CountryISO.Kuwait, dialCode: '965', flag: '🇰🇼'},
    {name: 'Kyrgyzstan', iso2: CountryISO.Kyrgyzstan, dialCode: '996', flag: '🇰🇬'},
    {name: 'Laos', iso2: CountryISO.Laos, dialCode: '856', flag: '🇱🇦'},
    {name: 'Latvia', iso2: CountryISO.Latvia, dialCode: '371', flag: '🇱🇻'},
    {name: 'Lebanon', iso2: CountryISO.Lebanon, dialCode: '961', flag: '🇱🇧'},
    {name: 'Lesotho', iso2: CountryISO.Lesotho, dialCode: '266', flag: '🇱🇸'},
    {name: 'Liberia', iso2: CountryISO.Liberia, dialCode: '231', flag: '🇱🇷'},
    {name: 'Libya', iso2: CountryISO.Libya, dialCode: '218', flag: '🇱🇾'},
    {name: 'Liechtenstein', iso2: CountryISO.Liechtenstein, dialCode: '423', flag: '🇱🇮'},
    {name: 'Lithuania', iso2: CountryISO.Lithuania, dialCode: '370', flag: '🇱🇹'},
    {name: 'Luxembourg', iso2: CountryISO.Luxembourg, dialCode: '352', flag: '🇱🇺'},
    {name: 'Macau', iso2: CountryISO.Macau, dialCode: '853', flag: '🇲🇴'},
    {name: 'Macedonia', iso2: CountryISO.Macedonia, dialCode: '389', flag: '🇲🇰'},
    {name: 'Madagascar', iso2: CountryISO.Madagascar, dialCode: '261', flag: '🇲🇬'},
    {name: 'Malawi', iso2: CountryISO.Malawi, dialCode: '265', flag: '🇲🇼'},
    {name: 'Malaysia', iso2: CountryISO.Malaysia, dialCode: '60', flag: '🇲🇾'},
    {name: 'Maldives', iso2: CountryISO.Maldives, dialCode: '960', flag: '🇲🇻'},
    {name: 'Mali', iso2: CountryISO.Mali, dialCode: '223', flag: '🇲🇱'},
    {name: 'Malta', iso2: CountryISO.Malta, dialCode: '356', flag: '🇲🇹'},
    {name: 'Marshall Islands', iso2: CountryISO.MarshallIslands, dialCode: '692', flag: '🇲🇭'},
    {name: 'Martinique', iso2: CountryISO.Martinique, dialCode: '596', flag: '🇲🇶'},
    {name: 'Mauritania', iso2: CountryISO.Mauritania, dialCode: '222', flag: '🇲🇷'},
    {name: 'Mauritius', iso2: CountryISO.Mauritius, dialCode: '230', flag: '🇲🇺'},
    {name: 'Mayotte', iso2: CountryISO.Mayotte, dialCode: '262', flag: '🇾🇹'},
    {name: 'Mexico', iso2: CountryISO.Mexico, dialCode: '52', flag: '🇲🇽'},
    {name: 'Micronesia', iso2: CountryISO.Micronesia, dialCode: '691', flag: '🇫🇲'},
    {name: 'Moldova', iso2: CountryISO.Moldova, dialCode: '373', flag: '🇲🇩'},
    {name: 'Monaco', iso2: CountryISO.Monaco, dialCode: '377', flag: '🇲🇨'},
    {name: 'Mongolia', iso2: CountryISO.Mongolia, dialCode: '976', flag: '🇲🇳'},
    {name: 'Montenegro', iso2: CountryISO.Montenegro, dialCode: '382', flag: '🇲🇪'},
    {name: 'Montserrat', iso2: CountryISO.Montserrat, dialCode: '1', flag: '🇲🇸'},
    {name: 'Morocco', iso2: CountryISO.Morocco, dialCode: '212', flag: '🇲🇦'},
    {name: 'Mozambique', iso2: CountryISO.Mozambique, dialCode: '258', flag: '🇲🇿'},
    {name: 'Myanmar', iso2: CountryISO.Myanmar, dialCode: '95', flag: '🇲🇲'},
    {name: 'Namibia', iso2: CountryISO.Namibia, dialCode: '264', flag: '🇳🇦'},
    {name: 'Nauru', iso2: CountryISO.Nauru, dialCode: '674', flag: '🇳🇷'},
    {name: 'Nepal', iso2: CountryISO.Nepal, dialCode: '977', flag: '🇳🇵'},
    {name: 'Netherlands', iso2: CountryISO.Netherlands, dialCode: '31', flag: '🇳🇱'},
    {name: 'New Caledonia', iso2: CountryISO.NewCaledonia, dialCode: '687', flag: '🇳🇨'},
    {name: 'New Zealand', iso2: CountryISO.NewZealand, dialCode: '64', flag: '🇳🇿'},
    {name: 'Nicaragua', iso2: CountryISO.Nicaragua, dialCode: '505', flag: '🇳🇮'},
    {name: 'Niger', iso2: CountryISO.Niger, dialCode: '227', flag: '🇳🇪'},
    {name: 'Nigeria', iso2: CountryISO.Nigeria, dialCode: '234', flag: '🇳🇬'},
    {name: 'Niue', iso2: CountryISO.Niue, dialCode: '683', flag: '🇳🇺'},
    {name: 'Norfolk Island', iso2: CountryISO.NorfolkIsland, dialCode: '672', flag: '🇳🇫'},
    {name: 'North Korea', iso2: CountryISO.NorthKorea, dialCode: '850', flag: '🇰🇵'},
    {name: 'Northern Mariana Islands', iso2: CountryISO.NorthernMarianaIslands, dialCode: '1', flag: '🇲🇵'},
    {name: 'Norway', iso2: CountryISO.Norway, dialCode: '47', flag: '🇳🇴'},
    {name: 'Oman', iso2: CountryISO.Oman, dialCode: '968', flag: '🇴🇲'},
    {name: 'Pakistan', iso2: CountryISO.Pakistan, dialCode: '92', flag: '🇵🇰'},
    {name: 'Palau', iso2: CountryISO.Palau, dialCode: '680', flag: '🇵🇼'},
    {name: 'Palestine', iso2: CountryISO.Palestine, dialCode: '970', flag: '🇵🇸'},
    {name: 'Panama', iso2: CountryISO.Panama, dialCode: '507', flag: '🇵🇦'},
    {name: 'Papua New Guinea', iso2: CountryISO.PapuaNewGuinea, dialCode: '675', flag: '🇵🇬'},
    {name: 'Paraguay', iso2: CountryISO.Paraguay, dialCode: '595', flag: '🇵🇾'},
    {name: 'Peru', iso2: CountryISO.Peru, dialCode: '51', flag: '🇵🇪'},
    {name: 'Philippines', iso2: CountryISO.Philippines, dialCode: '63', flag: '🇵🇭'},
    {name: 'Poland', iso2: CountryISO.Poland, dialCode: '48', flag: '🇵🇱'},
    {name: 'Portugal', iso2: CountryISO.Portugal, dialCode: '351', flag: '🇵🇹'},
    {name: 'Puerto Rico', iso2: CountryISO.PuertoRico, dialCode: '1', flag: '🇵🇷'},
    {name: 'Qatar', iso2: CountryISO.Qatar, dialCode: '974', flag: '🇶🇦'},
    {name: 'Réunion', iso2: CountryISO.Réunion, dialCode: '262', flag: '🇷🇪'},
    {name: 'Romania', iso2: CountryISO.Romania, dialCode: '40', flag: '🇷🇴'},
    {name: 'Russia', iso2: CountryISO.Russia, dialCode: '7', flag: '🇷🇺'},
    {name: 'Rwanda', iso2: CountryISO.Rwanda, dialCode: '250', flag: '🇷🇼'},
    {name: 'Saint Barthélemy', iso2: CountryISO.SaintBarthélemy, dialCode: '590', flag: '🇧🇱'},
    {name: 'Saint Helena', iso2: CountryISO.SaintHelena, dialCode: '290', flag: '🇸🇭'},
    {name: 'Saint Kitts and Nevis', iso2: CountryISO.SaintKittsAndNevis, dialCode: '1', flag: '🇰🇳'},
    {name: 'Saint Lucia', iso2: CountryISO.SaintLucia, dialCode: '1', flag: '🇱🇨'},
    {name: 'Saint Martin', iso2: CountryISO.SaintMartin, dialCode: '590', flag: '🇲🇫'},
    {name: 'Saint Pierre and Miquelon', iso2: CountryISO.SaintPierreAndMiquelon, dialCode: '508', flag: '🇵🇲'},
    {name: 'Saint Vincent and the Grenadines', iso2: CountryISO.SaintVincentAndTheGrenadines, dialCode: '1', flag: '🇻🇨'},
    {name: 'Samoa', iso2: CountryISO.Samoa, dialCode: '685', flag: '🇼🇸'},
    {name: 'San Marino', iso2: CountryISO.SanMarino, dialCode: '378', flag: '🇸🇲'},
    {name: 'São Tomé and Príncipe', iso2: CountryISO.SãoToméAndPríncipe, dialCode: '239', flag: '🇸🇹'},
    {name: 'Saudi Arabia', iso2: CountryISO.SaudiArabia, dialCode: '966', flag: '🇸🇦'},
    {name: 'Senegal', iso2: CountryISO.Senegal, dialCode: '221', flag: '🇸🇳'},
    {name: 'Serbia', iso2: CountryISO.Serbia, dialCode: '381', flag: '🇷🇸'},
    {name: 'Seychelles', iso2: CountryISO.Seychelles, dialCode: '248', flag: '🇸🇨'},
    {name: 'Sierra Leone', iso2: CountryISO.SierraLeone, dialCode: '232', flag: '🇸🇱'},
    {name: 'Singapore', iso2: CountryISO.Singapore, dialCode: '65', flag: '🇸🇬'},
    {name: 'Sint Maarten', iso2: CountryISO.SintMaarten, dialCode: '1', flag: '🇸🇽'},
    {name: 'Slovakia', iso2: CountryISO.Slovakia, dialCode: '421', flag: '🇸🇰'},
    {name: 'Slovenia', iso2: CountryISO.Slovenia, dialCode: '386', flag: '🇸🇮'},
    {name: 'Solomon Islands', iso2: CountryISO.SolomonIslands, dialCode: '677', flag: '🇸🇧'},
    {name: 'Somalia', iso2: CountryISO.Somalia, dialCode: '252', flag: '🇸🇴'},
    {name: 'South Africa', iso2: CountryISO.SouthAfrica, dialCode: '27', flag: '🇿🇦'},
    {name: 'South Korea', iso2: CountryISO.SouthKorea, dialCode: '82', flag: '🇰🇷'},
    {name: 'South Sudan', iso2: CountryISO.SouthSudan, dialCode: '211', flag: '🇸🇸'},
    {name: 'Spain', iso2: CountryISO.Spain, dialCode: '34', flag: '🇪🇸'},
    {name: 'Sri Lanka', iso2: CountryISO.SriLanka, dialCode: '94', flag: '🇱🇰'},
    {name: 'Sudan', iso2: CountryISO.Sudan, dialCode: '249', flag: '🇸🇩'},
    {name: 'Suriname: ', iso2: CountryISO.Suriname, dialCode: '597', flag: '🇸🇷'},
    {name: 'Svalbard and Jan Mayen', iso2: CountryISO.SvalbardAndJanMayen, dialCode: '47', flag: '🇸🇯'},
    {name: 'Swaziland', iso2: CountryISO.Swaziland, dialCode: '268', flag: '🇸🇿'},
    {name: 'Sweden', iso2: CountryISO.Sweden, dialCode: '46', flag: '🇸🇪'},
    {name: 'Switzerland', iso2: CountryISO.Switzerland, dialCode: '41', flag: '🇨🇭'},
    {name: 'Syria', iso2: CountryISO.Syria, dialCode: '963', flag: '🇸🇾'},
    {name: 'Taiwan', iso2: CountryISO.Taiwan, dialCode: '886', flag: '🇹🇼'},
    {name: 'Tajikistan', iso2: CountryISO.Tajikistan, dialCode: '992', flag: '🇹🇯'},
    {name: 'Tanzania', iso2: CountryISO.Tanzania, dialCode: '255', flag: '🇹🇿'},
    {name: 'Thailand', iso2: CountryISO.Thailand, dialCode: '66', flag: '🇹🇭'},
    {name: 'Timor-Leste', iso2: CountryISO.TimorLeste, dialCode: '670', flag: '🇹🇱'},
    {name: 'Togo', iso2: CountryISO.Togo, dialCode: '228', flag: '🇹🇬'},
    {name: 'Tokelau', iso2: CountryISO.Tokelau, dialCode: '690', flag: '🇹🇰'},
    {name: 'Tonga', iso2: CountryISO.Tonga, dialCode: '676', flag: '🇹🇴'},
    {name: 'Trinidad and Tobago', iso2: CountryISO.TrinidadAndTobago, dialCode: '1', flag: '🇹🇹'},
    {name: 'Tunisia', iso2: CountryISO.Tunisia, dialCode: '216', flag: '🇹🇳'},
    {name: 'Türkiye', iso2: CountryISO['Türkiye'], dialCode: '90', flag: '🇹🇷'},
    {name: 'Turkmenistan', iso2: CountryISO.Turkmenistan, dialCode: '993', flag: '🇹🇲'},
    {name: 'Turks and Caicos Islands', iso2: CountryISO.TurksAndCaicosIslands, dialCode: '1', flag: '🇹🇨'},
    {name: 'Tuvalu', iso2: CountryISO.Tuvalu, dialCode: '688', flag: '🇹🇻'},
    {name: 'U.S. Virgin Islands', iso2: CountryISO.USVirginIslands, dialCode: '1', flag: '🇻🇮'},
    {name: 'Uganda', iso2: CountryISO.Uganda, dialCode: '256', flag: '🇺🇬'},
    {name: 'Ukraine', iso2: CountryISO.Ukraine, dialCode: '380', flag: '🇺🇦'},
    {name: 'United Arab Emirates', iso2: CountryISO.UnitedArabEmirates, dialCode: '971', flag: '🇦🇪'},
    {name: 'United Kingdom', iso2: CountryISO.UnitedKingdom, dialCode: '44', flag: '🇬🇧'},
    {name: 'United States', iso2: CountryISO.UnitedStates, dialCode: '1', flag: '🇺🇸'},
    {name: 'Uruguay', iso2: CountryISO.Uruguay, dialCode: '598', flag: '🇺🇾'},
    {name: 'Uzbekistan', iso2: CountryISO.Uzbekistan, dialCode: '998', flag: '🇺🇿'},
    {name: 'Vanuatu', iso2: CountryISO.Vanuatu, dialCode: '678', flag: '🇻🇺'},
    {name: 'Vatican City', iso2: CountryISO.VaticanCity, dialCode: '39', flag: '🇻🇦'},
    {name: 'Venezuela', iso2: CountryISO.Venezuela, dialCode: '58', flag: '🇻🇪'},
    {name: 'Vietnam', iso2: CountryISO.Vietnam, dialCode: '84', flag: '🇻🇳'},
    {name: 'Wallis and Futuna', iso2: CountryISO.WallisAndFutuna, dialCode: '681', flag: '🇼🇫'},
    {name: 'Western Sahara', iso2: CountryISO.WesternSahara, dialCode: '212', flag: '🇪🇭'},
    {name: 'Yemen', iso2: CountryISO.Yemen, dialCode: '967', flag: '🇾🇪'},
    {name: 'Zambia', iso2: CountryISO.Zambia, dialCode: '260', flag: '🇿🇲'},
    {name: 'Zimbabwe', iso2: CountryISO.Zimbabwe, dialCode: '263', flag: '🇿🇼'},
    {name: 'Åland Islands', iso2: CountryISO.ÅlandIslands, dialCode: '358', flag: '🇦🇽'}
  ];
}
