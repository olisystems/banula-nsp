export type SmartLocation = Location & {
  malo: string;
  smart_meter_id: string;
  message_queue_url: string;
  market_location_id: string;
  metering_location_id: string;
  dso_market_partner_id: string;
  tso_market_partner_id: string;
  mpo_market_partner_id: string;
  metering_data_source: 'MSCONS' | 'SMART_METER' | 'COUNTROL_BACKEND';
  default_supplier: {
    supplier_market_partner_id: string;
    bkv_id: string;
    balancing_group_eic_id: string;
  };
};

export type Location = {
  country_code?: string;
  party_id?: string;
  id?: string;
  publish?: boolean;
  publish_allowed_to?: PublishTokenType[];
  name?: string;
  address?: string;
  city?: string;
  postal_code?: string;
  state?: string;
  country?: string;
  coordinates?: GeoLocation;
  related_locations?: AdditionalGeoLocation[];
  parking_type?: ParkingType;
  evses?: EVSE[];
  directions?: DisplayText[];
  operator?: BusinessDetails;
  suboperator?: BusinessDetails;
  owner?: BusinessDetails;
  facilities?: Facility[];
  timezone?: string;
  opening_times?: Hours;
  charging_when_closed?: boolean;
  images?: Image[];
  energy_mix?: EnergyMix;
  last_updated?: string;
};

export type PublishTokenType = {
  uid?: string;
  type?: TokenType;
  visual_number?: string;
  issuer?: string;
  group_id?: string;
};

export type GeoLocation = {
  type?: string;
  coordinates?: number[];
};

export type AdditionalGeoLocation = {
  latitude?: string;
  longitude?: string;
  name?: DisplayText;
};

export enum ParkingType {
  ALONG_MOTORWAY = "ALONG_MOTORWAY",
  PARKING_GARAGE = "PARKING_GARAGE",
  PARKING_LOT = "PARKING_LOT",
  ON_DRIVEWAY = "ON_DRIVEWAY",
  ON_STREET = "ON_STREET",
  UNDERGROUND_GARAGE = "UNDERGROUND_GARAGE",
}

export type EVSE = {
  uid?: string;
  evse_id?: string;
  status?: Status;
  status_schedule?: StatusSchedule[];
  capabilities?: Capability[];
  connectors?: Connector[];
  floor_level?: string;
  coordinates?: GeoLocation;
  physical_reference?: string;
  directions?: DisplayText[];
  parking_restrictions?: ParkingRestriction[];
  images?: Image[];
  last_updated?: string;
};

export type StatusSchedule = {
  period_begin?: string;
  period_end?: string;
  status?: Status;
};

export enum Status {
  AVAILABLE,
  BLOCKED,
  CHARGING,
  INOPERATIVE,
  OUTOFORDER,
  PLANNED,
  REMOVED,
  RESERVED,
  UNKNOWN,
}

export enum Capability {
  CHARGING_PROFILE_CAPABLE = "CHARGING_PROFILE_CAPABLE",
  CHARGING_PREFERENCES_CAPABLE = "CHARGING_PREFERENCES_CAPABLE",
  CHIP_CARD_SUPPORT = "CHIP_CARD_SUPPORT",
  CONTACTLESS_CARD_SUPPORT = "CONTACTLESS_CARD_SUPPORT",
  CREDIT_CARD_PAYABLE = "CREDIT_CARD_PAYABLE",
  DEBIT_CARD_PAYABLE = "DEBIT_CARD_PAYABLE",
  PED_TERMINAL = "PED_TERMINAL",
  REMOTE_START_STOP_CAPABLE = "REMOTE_START_STOP_CAPABLE",
  RESERVABLE = "RESERVABLE",
  RFID_READER = "RFID_READER",
  START_SESSION_CONNECTOR_REQUIRED = "START_SESSION_CONNECTOR_REQUIRED",
  TOKEN_GROUP_CAPABLE = "TOKEN_GROUP_CAPABLE",
  UNLOCK_CAPABLE = "UNLOCK_CAPABLE",
}

export type Connector = {
  id?: string;
  standard?: ConnectorType;
  format?: ConnectorFormat;
  power_type?: PowerType;
  max_voltage?: number;
  max_amperage?: number;
  max_electric_power?: number;
  tariff_ids?: string[];
  terms_and_conditions?: string;
  last_updated?: string;
};

export enum PowerType {
  AC_1_PHASE = "AC_1_PHASE",
  AC_2_PHASE = "AC_2_PHASE",
  AC_2_PHASE_SPLIT = "AC_2_PHASE_SPLIT",
  AC_3_PHASE = "AC_3_PHASE",
  DC = "DC",
}

export enum ConnectorFormat {
  SOCKET = "SOCKET",
  CABLE = "CABLE",
}

export enum ConnectorType {
  CHADEMO = "CHADEMO",
  CHAOJI = "CHAOJI",
  DOMESTIC_A = "DOMESTIC_A",
  DOMESTIC_B = "DOMESTIC_B",
  DOMESTIC_C = "DOMESTIC_C",
  DOMESTIC_D = "DOMESTIC_D",
  DOMESTIC_E = "DOMESTIC_E",
  DOMESTIC_F = "DOMESTIC_F",
  DOMESTIC_G = "DOMESTIC_G",
  DOMESTIC_H = "DOMESTIC_H",
  DOMESTIC_I = "DOMESTIC_I",
  DOMESTIC_J = "DOMESTIC_J",
  DOMESTIC_K = "DOMESTIC_K",
  DOMESTIC_L = "DOMESTIC_L",
  DOMESTIC_M = "DOMESTIC_M",
  DOMESTIC_N = "DOMESTIC_N",
  DOMESTIC_O = "DOMESTIC_O",
  GBT_AC = "GBT_AC",
  GBT_DC = "GBT_DC",
  IEC_60309_2_SINGLE_16 = "IEC_60309_2_single_16",
  IEC_60309_2_THREE_16 = "IEC_60309_2_three_16",
  IEC_60309_2_THREE_32 = "IEC_60309_2_three_32",
  IEC_60309_2_THREE_64 = "IEC_60309_2_three_64",
  IEC_62196_T1 = "IEC_62196_T1",
  IEC_62196_T1_COMBO = "IEC_62196_T1_COMBO",
  IEC_62196_T2 = "IEC_62196_T2",
  IEC_62196_T2_COMBO = "IEC_62196_T2_COMBO",
  IEC_62196_T3A = "IEC_62196_T3A",
  IEC_62196_T3C = "IEC_62196_T3C",
  NEMA_5_20 = "NEMA_5_20",
  NEMA_6_30 = "NEMA_6_30",
  NEMA_6_50 = "NEMA_6_50",
  NEMA_10_30 = "NEMA_10_30",
  NEMA_10_50 = "NEMA_10_50",
  NEMA_14_30 = "NEMA_14_30",
  NEMA_14_50 = "NEMA_14_50",
  PANTOGRAPH_BOTTOM_UP = "PANTOGRAPH_BOTTOM_UP",
  PANTOGRAPH_TOP_DOWN = "PANTOGRAPH_TOP_DOWN",
  TESLA_R = "TESLA_R",
  TESLA_S = "TESLA_S",
}

export type DisplayText = {
  language?: string;
  text?: string;
};

export enum ParkingRestriction {
  EV_ONLY = "EV_ONLY",
  PLUGGED = "PLUGGED",
  DISABLED = "DISABLED",
  CUSTOMERS = "CUSTOMERS",
  MOTORCYCLES = "MOTORCYCLES",
}

export type Image = {
  url?: string;
  thumbnail?: string;
  category?: ImageCategory;
  type?: string;
  width?: string;
  height?: string;
};

export enum ImageCategory {
  CHARGER = "CHARGER",
  ENTRANCE = "ENTRANCE",
  LOCATION = "LOCATION",
  NETWORK = "NETWORK",
  OPERATOR = "OPERATOR",
  OTHER = "OTHER",
  OWNER = "OWNER",
}

export type BusinessDetails = {
  name?: string;
  website?: string;
  logo?: Image;
};

export enum Facility {
  HOTEL = "HOTEL",
  RESTAURANT = "RESTAURANT",
  CAFE = "CAFE",
  MALL = "MALL",
  SUPERMARKET = "SUPERMARKET",
  SPORT = "SPORT",
  RECREATION_AREA = "RECREATION_AREA",
  NATURE = "NATURE",
  MUSEUM = "MUSEUM",
  BIKE_SHARING = "BIKE_SHARING",
  BUS_STOP = "BUS_STOP",
  TAXI_STAND = "TAXI_STAND",
  TRAM_STOP = "TRAM_STOP",
  METRO_STATION = "METRO_STATION",
  TRAIN_STATION = "TRAIN_STATION",
  AIRPORT = "AIRPORT",
  PARKING_LOT = "PARKING_LOT",
  CARPOOL_PARKING = "CARPOOL_PARKING",
  FUEL_STATION = "FUEL_STATION",
  WIFI = "WIFI",
}

export type EnergyMix = {
  green_energy?: boolean;
  energy_sources?: EnergySource[];
  environ_impact?: EnvironmentalImpact[];
  supplier_name?: string;
  energy_product_name?: string;
};

export type EnergySource = {
  source?: EnergySourceCategory;
  percentage?: number;
};

export enum EnergySourceCategory {
  NUCLEAR = "NUCLEAR",
  SOLAR = "SOLAR",
  WIND = "WIND",
  HYDRO = "HYDRO",
  BIOMASS = "BIOMASS",
  COAL = "COAL",
  GAS = "GAS",
  OIL = "OIL",
  OTHER = "OTHER",
}

export type EnvironmentalImpact = {
  category?: EnvironmentalImpactCategory;
  amount?: number;
};

export enum EnvironmentalImpactCategory {
  NUCLEAR_WASTE = "NUCLEAR_WASTE",
  CARBON_DIOXIDE = "CARBON_DIOXIDE",
}

export type Hours = {
  twenty_four_seven?: boolean;
  regular_hours?: RegularHours[];
  exceptional_openings?: ExceptionalPeriod[];
  exceptional_closings?: ExceptionalPeriod[];
};

export type RegularHours = {
  weekday?: number;
  period_begin?: string;
  period_end?: string;
};

export type ExceptionalPeriod = {
  period_begin?: string;
  period_end?: string;
};

export enum TokenType {
  AD_HOC_USER = "AD_HOC_USER",
  APP_USER = "APP_USER",
  OTHER = "OTHER",
  RFID = "RFID",
}
