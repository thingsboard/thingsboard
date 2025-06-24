# Modbus Functions

The Modbus connector supports the following Modbus functions:

| Modbus Function Code | Description                      |
|----------------------|----------------------------------|
| 1                    | Read Coils                        |
| 2                    | Read Discrete Inputs              |
| 3                    | Read Multiple Holding Registers   |
| 4                    | Read Input Registers              |
| 5                    | Write Coil                        |
| 6                    | Write Register                    |
| 15                   | Write Coils                       |
| 16                   | Write Registers                   |

## Data Types

A list and description of the supported data types for reading/writing data:

| Type     | Function Code | Objects Count | Note                                               |
|----------|---------------|---------------|----------------------------------------------------|
| string   | 3-4           | 1-…           | Read bytes from registers and decode it (‘UTF-8’ coding). |
| bytes    | 3-4           | 1-…           | Read bytes from registers.                        |
| bits     | 1-4           | 1-…           | Read coils. If the objects count is 1, result will be interpreted as a boolean. Otherwise, the result will be an array with bits. |
| 16int    | 3-4           | 1             | Integer 16 bit.                                   |
| 16uint   | 3-4           | 1             | Unsigned integer 16 bit.                          |
| 16float  | 3-4           | 1             | Float 16 bit.                                     |
| 32int    | 3-4           | 2             | Integer 32 bit.                                   |
| 32uint   | 3-4           | 2             | Unsigned integer 32 bit.                          |
| 32float  | 3-4           | 2             | Float 32 bit.                                     |
| 64int    | 3-4           | 4             | Integer 64 bit.                                   |
| 64uint   | 3-4           | 4             | Unsigned integer 64 bit.                          |
| 64float  | 3-4           | 4             | Float 64 bit.                                     |
