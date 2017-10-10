CREATE TABLE IF NOT EXISTS DEVICE_SHADOW (
  deviceToken varchar,
  tagName varchar,
  reported boolean,
  desired boolean,
  CONSTRAINT PK_DEVICE_SHADOW PRIMARY KEY (deviceToken, tagName)
 );