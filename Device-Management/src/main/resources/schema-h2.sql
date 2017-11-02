CREATE TABLE IF NOT EXISTS DEVICE_SHADOW (
  deviceName varchar,
  availableTags varchar,
  reportedTags varchar,
  desiredTags varchar,
  CONSTRAINT PK_DEVICE_SHADOW PRIMARY KEY (deviceName)
 );