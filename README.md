# logParser

Overview:

This program Parse a file containing flow log data and maps each row to a tag based on a lookup table. The lookup table is defined as a csv file, and it has 3 columns, dstport,protocol,tag. The dstport and protocol combination decide what tag can be applied.The program generates an output file with:
- Count of matches for each tag.
- Count of matches for each port/protocol combination.

Assumptions: 

- Input file as well as the file containing tag mappings are plain text (ascii) files  
- The flow log file size can be up to 10 MB 
- The lookup file can have up to 10000 mappings 
- The tags can map to more than one port, protocol combinations. 
- The matches should be case insensitive 
- The lookup file contains valid port numbers and protocols.
- The flow log format is consistent with the AWS VPC flow log structure.(11 columns)
- The lookup file contains valid mappings for the tag assignment.
- A single port/protocol combination in the lookup table can map to multiple tags
- There are no duplicate entries for the same tag under the same port/protocol combination

Compilation and Execution: 

- javac logParser.java (Through the src directory) 
- java logParser

Make sure that the the lookup table CSV file should be placed in the root directory of the project as `lookup.csv` and the flow log data should be placed in the root directory as `log.txt`.

