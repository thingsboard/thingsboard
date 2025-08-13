### Expressions
#### Bytes converter:

For bytes converter, expression fields can use slices format only. A slice specifies how to slice a sequence, determining the start point, and the endpoint. Here's a basic overview of slice components:

- `start`: The starting index of the slice. It is included in the slice. If omitted, slicing starts at the beginning of the sequence. Indexing starts at 0, so the first element of the sequence is at index 0.

- `stop`: The ending index of the slice. It is excluded from the slice, meaning the slice will end just before this index. If omitted, slicing goes through the end of the sequence.

##### Bytes parsing examples:


| Message body           |  Slice          | Output data              | Description                  |
|:-----------------------|-----------------|--------------------------|------------------------------|
|   AM123,mytype,12.2,45 |  [:5]           |  AM123                   | Extracting device name       |
|   AM123,mytype,12.2,45 |  [:]            |  AM123,mytype,12.2,45    | Extracting all data          |
|   AM123,mytype,12.2,45 |  [18:]          |  45                      | Extracting humidity value    |
|   AM123,mytype,12.2,45 |  [13:17]        |  12.2                    | Extracting temperature value |
