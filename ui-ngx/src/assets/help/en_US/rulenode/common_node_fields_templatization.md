Fields templatization feature allows you to process the incoming messages with dynamic configuration by substitution templates specified in the configuration fields with values from message or message metadata.

There are two types of rule node configuration templates defined.

*$[messageKey]* - templates with square brackets used to extract value from the message.

*${metadataKey}* - templates with curly brackets used to extract value from the message metadata.

Note: *messageKey* and *metadataKey* are just samples of key names that might exist in the message or metadata.
