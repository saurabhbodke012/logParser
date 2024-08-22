import java.io.*;
import java.util.*;

public class logParser {

    private static final String COMMA_DELIMITER = ",";
    private static final String WHITESPACE_DELIMITER = "\\s+";

    private static final String LOOKUP_FILE_PATH = "lookup.csv";
    private static final String FLOW_LOG_FILE_PATH = "log.txt";
    private static final String OUTPUT_FILE_PATH = "output.txt";

    // Maps to store tag counts and port/protocol combination counts
    private Map<String, Integer> tagCount = new HashMap<>();
    private Map<String, Integer> portProtocolCount = new HashMap<>();
    private Map<String, Set<String>> lookupTable = new HashMap<>();

    // Protocol mapping
    private static final Map<String, String> PROTOCOL_MAP = Map.of(
            "6", "tcp",
            "17", "udp",
            "1", "icmp"
    );

    // Count for untagged flows (flows with no matching entry in the lookup table)
    private int untagged = 0;

    public static void main(String[] args) {
        logParser parser = new logParser();
        try {

            String lookupFilePath = LOOKUP_FILE_PATH;
            String flowLogFilePath = FLOW_LOG_FILE_PATH;
            String outputFilePath = OUTPUT_FILE_PATH;

            // Load lookup table and process the logs
            parser.loadLookupTable(lookupFilePath);
            parser.processFlowLog(flowLogFilePath);
            parser.writeResults(outputFilePath);
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to load the lookup table from a CSV file
    public void loadLookupTable(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Lookup file does not exist: " + filePath);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean hasValidEntries = false;

            while ((line = br.readLine()) != null) {
                String[] columns = line.split(COMMA_DELIMITER);

                // Check for missing columns
                if (columns.length != 3) {
                    System.err.println("Invalid entry in lookup table: " + line);
                    continue; // Skip invalid entries
                }

                String dstPort = columns[0].toLowerCase().trim();
                String protocol = columns[1].toLowerCase().trim();
                String tag = columns[2].toLowerCase().trim();  // Convert tag to lowercase for case-insensitive matching

                // Validate that dstPort is a valid integer
                if (!isValidInteger(dstPort)) {
                    System.err.println("Invalid port in lookup table: " + line);
                    continue; // Skip invalid entries
                }

                // Validate that protocol is valid (check numeric or string protocol)
                protocol = normalizeProtocol(protocol);
                if (protocol == null) {
                    System.err.println("Invalid protocol in the lookup table: " + line);
                    continue; // Skip invalid entries
                }

                // Add tags for the same port/protocol combination
                String key = dstPort + COMMA_DELIMITER + protocol;
                lookupTable.computeIfAbsent(key, k -> new HashSet<>()).add(tag);
                hasValidEntries = true;
            }

            if (!hasValidEntries) {
                throw new IOException("No valid entries in the lookup table.");
            }
        }
    }

    // Method to process the flow log file
    public void processFlowLog(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Flow log file missing. " + filePath);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean hasValidEntries = false;

            while ((line = br.readLine()) != null) {
                String[] columns = line.split(WHITESPACE_DELIMITER);

                // Ensure that the log entry has 11 fields (based on AWS flow log structure)
                if (columns.length != 11) {
                    System.err.println("Invalid flow log entry: " + line);
                    continue; // Skip invalid entries
                }

                String dstPort = columns[6].toLowerCase().trim();
                String protocol = columns[7].toLowerCase().trim();

                // Validate that dstPort is a valid integer
                if (!isValidInteger(dstPort)) {
                    System.err.println("Invalid port in flow log: " + line);
                    continue; // Skip invalid entries
                }

                // Validate that protocol is valid (check numeric or string protocol)
                protocol = normalizeProtocol(protocol);
                if (protocol == null) {
                    System.err.println("Invalid protocol in flow log: " + line);
                    continue; // Skip invalid entries
                }

                // Create key for lookup
                String key = dstPort + COMMA_DELIMITER + protocol;

                // Check if the combination exists in the lookup table
                if (lookupTable.containsKey(key)) {
                    Set<String> tags = lookupTable.get(key);
                    for (String tag : tags) {
                        tagCount.put(tag, tagCount.getOrDefault(tag, 0) + 1);
                    }
                } else {
                    untagged++;
                }

                portProtocolCount.put(key, portProtocolCount.getOrDefault(key, 0) + 1);
                hasValidEntries = true;
            }

            if (!hasValidEntries) {
                throw new IOException("No valid entries found in the flow log.");
            }
        }
    }

    // Method to normalize the protocol to its string representation
    private String normalizeProtocol(String protocol) {
        // Check if the protocol is a known numeric value
        if (PROTOCOL_MAP.containsKey(protocol)) {
            return PROTOCOL_MAP.get(protocol); // Return the string representation (e.g., "tcp", "udp")
        }

        // Check if it's already a valid string protocol
        if (protocol.equals("tcp") || protocol.equals("udp") || protocol.equals("icmp")) {
            return protocol;
        }

        // If it's neither, return null (invalid protocol)
        return null;
    }

    // Method to write results to an output file
    public void writeResults(String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

            writer.write("Tag Counts:\n");
            writer.write("Tag,Count\n");  // Header for the CSV-style format

            // TagCounts
            for (Map.Entry<String, Integer> entry : tagCount.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }

            // Write the count for unmatched flows as "Untagged"
            writer.write("Untagged," + untagged + "\n");

            writer.write("\nPort/Protocol Combination Counts:\n");
            writer.write("Port,Protocol,Count\n");  // Header for Port/Protocol counts

            // Write port/protocol counts in the format: Port,Protocol,Count
            for (Map.Entry<String, Integer> entry : portProtocolCount.entrySet()) {
                String[] keyParts = entry.getKey().split(COMMA_DELIMITER);
                writer.write(keyParts[0] + "," + keyParts[1] + "," + entry.getValue() + "\n");
            }
        }
    }

    //  if the string is a valid integer
    private boolean isValidInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
