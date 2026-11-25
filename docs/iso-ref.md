# **ISO 8583 — Developer Reference & Implementation Guide**

This guide goes beyond the standard overview and focuses on how to **read, decode, construct, and process ISO 8583 messages in a payment switch, banking integration, POS, ATM, or mobile money/payment system**.

Practical reference • Message parsing • Bitmaps • Transaction flows • Field decoding

## **1. Anatomy of an ISO 8583 message**

A typical ISO 8583 message is composed of three main parts:

**MTI — Message Type Indicator**

4 digits

**Bitmap — Field Presence Indicator**

16 hexadecimal characters (primary bitmap)

**Data Elements — Transaction Payload**

Card, amount, merchant, terminal, transaction data

### **Structure**

```
+-----------------------------------------------------------+
| MTI | BITMAP | DATA ELEMENTS                              |
+-----------------------------------------------------------+
| 4   | 8 bytes| Variable / fixed fields                    |
+-----------------------------------------------------------+
```

The message may additionally be preceded by a transport header or length prefix, depending on the switch or processor.

## **2. Understanding the MTI**

The MTI is a four-digit value that determines how the message should be interpreted.

```
0200
│││└── Origin
││└─── Function
│└──── Class
└───── Version
```

| Position | Meaning          | Example |
| -------- | ---------------- | ------- |
| 1        | Version          | `0`     |
| 2        | Message class    | `2`     |
| 3        | Message function | `0`     |
| 4        | Message origin   | `0`     |

### **MTI breakdown**

For example, `0200`:

| Digit | Value | Interpretation        |
| ----- | ----- | --------------------- |
| 1     | 0     | ISO 8583:1987 version |
| 2     | 2     | Financial transaction |
| 3     | 0     | Request               |
| 4     | 0     | Acquirer-originated   |

Therefore:

**0200 = Acquirer-originated financial transaction request**

### **Common MTIs**

| MTI    | Meaning                        |
| ------ | ------------------------------ |
| `0100` | Authorization request          |
| `0110` | Authorization response         |
| `0120` | Authorization advice           |
| `0130` | Authorization advice response  |
| `0200` | Financial transaction request  |
| `0210` | Financial transaction response |
| `0220` | Financial advice               |
| `0230` | Financial advice response      |
| `0400` | Reversal request               |
| `0410` | Reversal response              |
| `0420` | Reversal advice                |
| `0430` | Reversal advice response       |
| `0800` | Network management request     |
| `0810` | Network management response    |

## **3. Transaction types and message flows**

### **3.1 Authorization transaction**

An authorization transaction checks whether a payment can be approved.

## **4. Bitmap decoding**

The bitmap tells the receiver which data elements exist in the message.

Each bit represents a field number.

### **Primary bitmap**

- 64 bits
- 8 bytes
- 16 hexadecimal characters
- Represents DE 1–64

### **Secondary bitmap**

- 64 additional bits
- Represents DE 65–128
- Present when bit 1 of the primary bitmap is set

### **Bitmap positioning**

## **Bitmap bit positions**

Bit 1 → DE 1

Bit 2 → DE 2

Bit 3 → DE 3

Bit 4 → DE 4

Bit 5 → DE 5

Bit 6 → DE 6

Bit 7 → DE 7

Bit 8 → DE 8

Bit 9 → DE 9

Bit 10 → DE 10

Bit 11 → DE 11

Bit 12 → DE 12

...

Bit 64 → DE 64

### **Bitmap decoding example**

Bitmap:

```
7010001102C048
```

Convert every hexadecimal character into 4 binary bits.

| Hex | Binary |
| --- | ------ |
| 7   | `0111` |
| 0   | `0000` |
| 1   | `0001` |
| 0   | `0000` |
| 0   | `0000` |
| 0   | `0000` |
| 1   | `0001` |
| 1   | `0001` |
| 0   | `0000` |
| 2   | `0010` |
| C   | `1100` |
| 0   | `0000` |
| 4   | `0100` |
| 8   | `1000` |

When converted into binary, every set bit indicates a present field.

Note: The example bitmap above is illustrative. Always validate the bitmap length, secondary bitmap indicator, and field mapping against the message specification you're implementing.

### **Bitmap decoding algorithm**

```
1. Read the primary bitmap.
2. Convert hexadecimal bitmap into binary.
3. Check bit 1.
4. If bit 1 is set, read the secondary bitmap.
5. Enumerate set bits.
6. Map each set bit to its corresponding data element.
7. Parse fields in ascending field-number order.
```

### **Bitmap position mapping**

For the primary bitmap:

```
Bitmap byte 1, bit 1 → DE 1
Bitmap byte 1, bit 2 → DE 2
Bitmap byte 1, bit 3 → DE 3
...
Bitmap byte 8, bit 8 → DE 64
```

The first bit of the primary bitmap is a special indicator for the presence of the secondary bitmap.

## **5. Data elements — field-by-field reference**

ISO 8583 fields are usually referred to as Data Elements (DE).

Field definitions and encoding can differ by ISO version, bank, processor, and payment network.

### **Core transaction fields**

| DE | Name                               | Typical format | Description                             |
| -- | ---------------------------------- | -------------- | --------------------------------------- |
| 2  | PAN                                | LLVAR, n..19   | Primary Account Number                  |
| 3  | Processing Code                    | Fixed, n6      | Transaction type and account processing |
| 4  | Transaction Amount                 | Fixed, n12     | Transaction amount                      |
| 5  | Settlement Amount                  | Fixed, n12     | Settlement amount                       |
| 6  | Cardholder Billing Amount          | Fixed, n12     | Amount billed to cardholder             |
| 7  | Transmission Date/Time             | Fixed, n10     | MMDDhhmmss                              |
| 9  | Settlement Conversion Rate         | Fixed          | Conversion rate                         |
| 10 | Cardholder Billing Conversion Rate | Fixed          | Billing conversion rate                 |
| 11 | STAN                               | Fixed, n6      | System Trace Audit Number               |
| 12 | Local Transaction Time             | Fixed, n6      | HHMMSS                                  |
| 13 | Local Transaction Date             | Fixed, n4      | MMDD                                    |
| 14 | Expiration Date                    | Fixed, n4      | YYMM                                    |
| 15 | Settlement Date                    | Fixed, n4      | MMDD                                    |
| 18 | MCC                                | Fixed, n4      | Merchant Category Code                  |
| 22 | POS Entry Mode                     | Fixed, n3      | Card capture method                     |
| 23 | Card Sequence Number               | Fixed, n3      | Card sequence number                    |
| 25 | POS Condition Code                 | Fixed, n2      | Terminal transaction condition          |
| 26 | PIN Capture Code                   | Fixed, n2      | PIN capture capability                  |

### **Institution, merchant and terminal fields**

| DE | Name                       | Typical format | Description                    |
| -- | -------------------------- | -------------- | ------------------------------ |
| 32 | Acquiring Institution ID   | LLVAR          | Acquirer identifier            |
| 33 | Forwarding Institution ID  | LLVAR          | Forwarding institution         |
| 35 | Track 2 Data               | LLVAR          | Card track data                |
| 37 | Retrieval Reference Number | Fixed, an12    | Transaction reference          |
| 38 | Authorization ID           | Fixed, an6     | Approval reference             |
| 39 | Response Code              | Fixed, an2     | Transaction result             |
| 41 | Terminal ID                | Fixed, ans8    | POS/ATM terminal identifier    |
| 42 | Merchant ID                | Fixed, ans15   | Merchant identifier            |
| 43 | Merchant Name/Location     | Fixed, ans40   | Merchant descriptor            |
| 49 | Currency Code              | Fixed, n3      | ISO 4217 currency              |
| 52 | PIN Data                   | Binary         | Encrypted PIN block            |
| 53 | Security Control Info      | Fixed          | Security-related information   |
| 54 | Additional Amounts         | LLLVAR         | Additional transaction amounts |
| 55 | ICC Data                   | LLLVAR         | EMV chip data                  |
| 57 | National Data              | Variable       | Network-specific data          |
| 60 | Reserved                   | Variable       | Network-specific               |
| 61 | Reserved                   | Variable       | Network-specific               |
| 62 | Reserved                   | Variable       | Network-specific               |
| 63 | Reserved                   | Variable       | Network-specific               |
| 64 | MAC                        | Binary         | Message authentication code    |

### **Additional fields**

| DE  | Name                                |
| --- | ----------------------------------- |
| 70  | Network Management Information Code |
| 90  | Original Data Elements              |
| 95  | Replacement Amounts                 |
| 100 | Receiving Institution ID            |
| 102 | Account ID 1                        |
| 103 | Account ID 2                        |
| 104 | Transaction Description             |
| 128 | Secondary MAC                       |

Security note: PAN, track data, PIN blocks, cryptographic keys, and sensitive EMV data must be handled according to applicable PCI and network security requirements. Avoid logging raw sensitive payment data.

## **6. Understanding field encoding**

ISO 8583 fields are not all encoded in the same way.

### **Fixed-length fields**

A fixed-length field has a predefined length.

Example:

```
DE 3 — Processing Code
Length: 6 digits
```

Value:

```
000000
```

The parser knows it must read exactly 6 digits.

### **LLVAR — Two-digit length prefix**

LLVAR fields contain a two-digit length prefix followed by the field value.

Example: DE 2 — PAN

```
16 1234567890123456
```

Interpretation:

| Segment       | Value              |
| ------------- | ------------------ |
| Length prefix | `16`               |
| Data          | `1234567890123456` |
| Total         | 18 characters      |

### **LLLVAR — Three-digit length prefix**

LLLVAR fields use three digits to indicate the data length.

Example:

```
025 + 25 characters of data
```

Interpretation:

```
025 = Data length
Next 25 characters = Field value
```

### **Length prefix summary**

| Type   | Prefix length | Maximum commonly indicated |
| ------ | ------------- | -------------------------- |
| Fixed  | 0             | Predefined                 |
| LVAR   | 1             | 9                          |
| LLVAR  | 2             | 99                         |
| LLLVAR | 3             | 999                        |

The maximum data length and actual encoding rules are determined by the field specification.

## **7. Financial transaction field breakdown**

Consider a typical financial transaction request.

| Field | Example          | Description            |
| ----- | ---------------- | ---------------------- |
| MTI   | `0200`           | Financial request      |
| DE 3  | `000000`         | Processing code        |
| DE 4  | `000000005000`   | Amount                 |
| DE 7  | `1125183045`     | Transmission date/time |
| DE 11 | `123456`         | STAN                   |
| DE 22 | `051`            | POS entry mode         |
| DE 41 | `TERM0001`       | Terminal ID            |
| DE 42 | `MERCHANT000001` | Merchant ID            |
| DE 49 | `800`            | UGX currency code      |

These are illustrative values, not a universal profile. For example, field lengths and POS entry mode meanings must be checked against the implementation specification.

### **DE 4 — Transaction amount**

DE 4 commonly contains a 12-digit amount without a decimal point.

For example:

```
000000005000
```

Depending on currency exponent and network conventions, this might represent:

```
UGX 5,000
```

For a currency with two decimal places, the same numeric field could represent a value with minor-unit scaling.

| Amount      | DE 4           |
| ----------- | -------------- |
| UGX 500     | `000000000500` |
| UGX 5,000   | `000000005000` |
| UGX 25,000  | `000000025000` |
| UGX 100,000 | `000000100000` |

Do not assume that every ISO 8583 implementation uses the same decimal scaling for every currency.

## **8. Processing Code — DE 3**

DE 3 is a six-digit field that defines what kind of transaction is being processed.

```
DE 3 = TT AA SS
```

| Positions | Meaning           |
| --------- | ----------------- |
| 1–2       | Transaction type  |
| 3–4       | From account type |
| 5–6       | To account type   |

Example:

```
000000
```

The meaning of each component depends on the payment network or switch configuration.

### **Common processing scenarios**

| Scenario        | Example processing code | Typical description  |
| --------------- | ----------------------- | -------------------- |
| Purchase        | `000000`                | Purchase transaction |
| Cash withdrawal | `010000`                | Withdrawal           |
| Balance inquiry | `310000`                | Balance inquiry      |
| Transfer        | `400000`                | Account transfer     |
| Refund          | Network-specific        | Refund / credit      |
| Cash advance    | Network-specific        | Cash advance         |

These processing codes are examples, not universal definitions. Never hardcode their meaning without checking the switch's processing-code specification.

## **9. Response codes — DE 39**

DE 39 tells the requesting system how the transaction was handled.

It is normally two characters.

### **Common response codes**

| Code | Description                             | Classification   |
| ---- | --------------------------------------- | ---------------- |
| `00` | Approved                                | Success          |
| `01` | Refer to issuer                         | Referral         |
| `02` | Refer to issuer, special condition      | Referral         |
| `03` | Invalid merchant                        | Error            |
| `04` | Pick up card                            | Card retention   |
| `05` | Do not honor                            | Decline          |
| `06` | Error                                   | Processing error |
| `07` | Pick up card, special condition         | Card retention   |
| `12` | Invalid transaction                     | Error            |
| `13` | Invalid amount                          | Error            |
| `14` | Invalid card number                     | Error            |
| `15` | No such issuer                          | Routing error    |
| `19` | Re-enter transaction                    | Retry            |
| `30` | Format error                            | Error            |
| `31` | Bank not supported                      | Routing error    |
| `41` | Lost card                               | Hard decline     |
| `43` | Stolen card                             | Hard decline     |
| `51` | Insufficient funds                      | Decline          |
| `54` | Expired card                            | Hard decline     |
| `55` | Incorrect PIN                           | Decline          |
| `57` | Transaction not permitted to cardholder | Decline          |
| `58` | Transaction not permitted at terminal   | Decline          |
| `61` | Exceeds withdrawal limit                | Decline          |
| `65` | Exceeds frequency limit                 | Decline          |
| `68` | Response received too late              | Timeout          |
| `91` | Issuer or switch unavailable            | System error     |
| `96` | System malfunction                      | System error     |

### **Handling response codes in your application**

Instead of only checking whether a response code is `00`, classify responses into business categories.

```
APPROVED
DECLINED
RETRYABLE
TIMEOUT
SYSTEM_ERROR
INVALID_MESSAGE
```

Example logic:

```
function classifyResponseCode(code: string) {
  if (code === "00") {
    return "APPROVED";
  }

  if (["91", "96", "68"].includes(code)) {
    return "SYSTEM_ERROR";
  }

  if (["05", "51", "55", "61", "65"].includes(code)) {
    return "DECLINED";
  }

  return "UNKNOWN";
}
```

In a production switch, response handling must be profile-specific. A timeout or unavailable issuer does not necessarily mean the transaction failed financially.

## **10. STAN and RRN**

These two fields are essential for transaction tracking and reconciliation.

### **DE 11 — System Trace Audit Number (STAN)**

STAN is typically a six-digit number used to identify and trace a transaction within a processing system.

Example:

```
123456
```

It is commonly used for:

- Transaction correlation
- Duplicate detection
- Troubleshooting
- Reconciliation
- Switch logging

STAN is not necessarily globally unique forever. Uniqueness is usually scoped by a combination of identifiers and time.

### **DE 37 — Retrieval Reference Number (RRN)**

RRN is a transaction reference used to retrieve or trace a transaction across systems.

Example:

```
612518123456
```

A transaction may be identified using a combination of:

```
STAN
RRN
Terminal ID
Merchant ID
Transaction Date
Acquirer ID
```

## **11. POS Entry Mode — DE 22**

DE 22 identifies how card credentials were captured.

Typical examples include:

| Method          | Description                       |
| --------------- | --------------------------------- |
| Manual entry    | Card number entered manually      |
| Magnetic stripe | Card read through magnetic stripe |
| Chip            | EMV chip inserted                 |
| Contactless     | Tap-to-pay                        |
| E-commerce      | Card details captured online      |
| Mobile          | Mobile payment credential         |

The field is often three digits, but the exact mapping of digits to capture method, PIN capability, or other attributes depends on the implementation profile.

## **12. Network management — 0800 / 0810**

Network management messages are used to maintain communication between systems.

They may be used for:

- Sign-on
- Sign-off
- Echo tests
- Network status
- Key exchange
- Communication initialization

### **Example: Echo test**

```
Switch A                       Switch B
   |                              |
   |---------- 0800 -------------->|
   |                              |
   |<--------- 0810 ---------------|
   |                              |
   |       Connection OK           |
```

### **Common Network Management Information Code — DE 70**

| Code  | Description        |
| ----- | ------------------ |
| `001` | Sign-on            |
| `002` | Sign-off           |
| `003` | Transaction status |
| `004` | Echo test          |
| `005` | Cutover            |
| `006` | Unsolicited status |

Exact DE 70 values may vary by network.

## **13. Example messages**

These examples illustrate message contents at the data-element level. They are not guaranteed to be directly wire-compatible with every switch because bitmap encoding, field lengths, headers, and network profiles vary.

### **Example A — 0200 Purchase Request**

```
MTI: 0200

DE 3: 000000
DE 4: 000000005000
DE 7: 1125183045
DE 11: 123456
DE 22: 051
DE 41: TERM0001
DE 42: MERCHANT000001
DE 49: 800
```

| Field | Value            | Meaning                        |
| ----- | ---------------- | ------------------------------ |
| MTI   | `0200`           | Financial request              |
| DE 3  | `000000`         | Processing code                |
| DE 4  | `000000005000`   | Transaction amount             |
| DE 7  | `1125183045`     | Transmission timestamp         |
| DE 11 | `123456`         | STAN                           |
| DE 22 | `051`            | Entry mode (profile-dependent) |
| DE 41 | `TERM0001`       | Terminal                       |
| DE 42 | `MERCHANT000001` | Merchant                       |
| DE 49 | `800`            | Currency — UGX                 |

### **Example B — 0210 Approved Response**

```
MTI: 0210

DE 3: 000000
DE 4: 000000005000
DE  11: 123456
DE  37: 612518123456
DE  38: A12345
DE  39: 00
DE  41: TERM0001
DE  49: 800
```

| Field | Value          | Meaning             |
| ----- | -------------- | ------------------- |
| MTI   | `0210`         | Financial response  |
| DE 11 | `123456`       | Original STAN       |
| DE 37 | `612518123456` | Retrieval reference |
| DE 38 | `A12345`       | Authorization code  |
| DE 39 | `00`           | Approved            |

### **Example C — 0210 Declined Response**

```
MTI: 0210

DE 11: 123456
DE 39: 51
```

Interpretation:

```
Transaction declined
Reason: Insufficient funds
```

### **Example D — 0400 Reversal Request**

```
MTI: 0400

DE  3: 000000
DE  4: 000000005000
DE  7: 1125183045
DE 11: 123456
DE  37: 612518123456
DE  41: TERM0001
DE  42: MERCHANT000001
DE  90: Original Data Elements
```

The reversal should reference the original transaction so that the receiving system can identify what must be reversed.

### **DE 90 — Original Data Elements**

DE 90 typically contains information identifying the original transaction.

A commonly used structure is:

| Component                          | Length |
| ---------------------------------- | ------ |
| Original MTI                       | 4      |
| Original STAN                      | 6      |
| Original transmission date/time    | 10     |
| Original acquiring institution ID  | 11     |
| Original forwarding institution ID | 11     |
| **Total**                          | **42** |

Example:

```
0200123456112518304500000000000
```

The exact field structure and padding must be validated against your ISO 8583 profile.

## **14. How to decode a raw ISO 8583 message**

A parser should process a message in a predictable sequence.

## **ISO 8583 decoding pipeline**

1\. Receive raw bytes

2\. Parse transport header / message length

3\. Extract MTI

4\. Read primary bitmap

5\. Read secondary / tertiary bitmap if present

6\. Identify present fields

7\. Parse fields in ascending order

8\. Validate field lengths and values

9\. Build transaction model

### **Pseudocode**

```
function decode(message, profile):

    message = parse_transport_header(message)

    mti = read(4)

    primary_bitmap = read_bitmap(64)

    fields = {}

    if primary_bitmap.bit(1) == 1:
        secondary_bitmap = read_bitmap(64)

    if secondary_bitmap.bit(1) == 1:
        tertiary_bitmap = read_bitmap(64)

    present_fields = get_set_bits(primary_bitmap,
                                 secondary_bitmap,
                                 tertiary_bitmap)

    for field_number in present_fields.sorted():

        definition = profile.get_field(field_number)

        if definition == null:
            throw UnsupportedField(field_number)

        if definition.is_fixed:
            length = definition.fixed_length

        else:
            length = read_length_prefix(
                definition.length_prefix_size
            )

        value = read_field(length, definition.encoding)

        fields[field_number] = value

    validate(mti, fields, profile)

    return {
        mti: mti,
        fields: fields
    }
```

The above is pseudocode. A production parser must account for binary bitmaps, packed numeric fields, BCD, ASCII, EBCDIC, field-specific padding, and profile-defined length prefixes.

## **15. Building an ISO 8583 message**

Constructing a message follows the reverse process of decoding.

```
1. Determine the MTI.
2. Collect the transaction data elements.
3. Determine which fields are present.
4. Generate the bitmap.
5. Serialize fields in ascending order.
6. Apply field-specific encoding.
7. Calculate MAC if required.
8. Add transport header or length prefix.
9. Send the message.
```

### **Example message-building structure**

```
{
  "mti": "0200",
  "fields": {
    "3": "000000",
    "4": "000000005000",
    "7": "1125183045",
    "11": "123456",
    "22": "051",
    "41": "TERM0001",
    "42": "MERCHANT000001",
    "49": "800"
  }
}
```

The JSON above is a logical representation of the transaction, not the ISO 8583 wire format.

## **16. Transaction matching and duplicate detection**

A payment switch should not depend on the STAN alone to match transactions.

A practical transaction correlation key may include:

```
Acquirer ID
Terminal ID
Merchant ID
STAN
RRN
Transaction Date
Transaction Amount
PAN Token / Account Reference
```

### **Why duplicate detection matters**

Duplicate requests may occur because of:

- Terminal retries
- Network timeouts
- Switch retries
- Processor retransmissions
- Lost response messages
- Client reconnects

Your switch should maintain a transaction state machine and prevent the same transaction from being financially processed multiple times.

### **Recommended components**

| Component              | Responsibility                                      |
| ---------------------- | --------------------------------------------------- |
| TCP Listener           | Accept network connections                          |
| Transport Decoder      | Parse headers and message boundaries                |
| ISO Parser             | Decode MTI, bitmap and data elements                |
| Validator              | Validate message structure and field constraints    |
| Transaction Router     | Route transactions based on processing code         |
| Institution Connector  | Communicate with external institutions              |
| Transaction Manager    | Handle lifecycle and idempotency                    |
| Response Builder       | Construct response MTIs and fields                  |
| MAC Service            | Message authentication and cryptographic operations |
| Logger                 | Audit events without exposing sensitive data        |
| Reconciliation Service | Compare transaction records across systems          |

## **18. ISO 8583 security considerations**

ISO 8583 is a message format; it does not, by itself, provide complete transport or application security.

Implementations may require:

- TLS or secure private network connectivity
- Message Authentication Code (MAC) validation
- HSM-based key management
- PIN block encryption
- PCI DSS compliance
- Sensitive data masking
- Replay protection
- Duplicate transaction detection
- Secure key rotation
- Audit logging
- Access control
- Secure reversal processing

### **Never expose sensitive data in logs**

Avoid logging:

- Full PAN
- PIN blocks
- Track 1 or Track 2 data
- Cryptographic keys
- Sensitive EMV data
- Authentication credentials

Prefer masked or tokenized values.

Example:

```
PAN: 476173******0010
STAN: 123456
RRN: 612518123456
MTI: 0200
DE39: 00
Amount: 5000
```

## **19. Implementation checklist**

## **ISO 8583 switch readiness**

Transport protocol implemented

Message framing / length prefix handling

MTI parser

Primary bitmap decoder

Secondary bitmap decoder

Field definitions and profiles

Fixed-length field parser

LLVAR / LLLVAR parser

ASCII / BCD / binary encoding

Request / response mapping

Response code handling

Transaction correlation

Duplicate detection / idempotency

Timeout handling

Reversal processing

MAC verification

Key management / HSM integration

Audit logging and masking

Network management / sign-on

Reconciliation

Integration test cases

## **20. Recommended learning sequence**

## **Level 1**

**Fundamentals**

MTI, bitmaps, data elements, field types

## **Level 2**

**Message parsing**

Decode raw messages, understand encoding, build a parser

## **Level 3**

**Transactions**

0200, 0210, authorization, reversal, advice

## **Level 4**

**Switch development**

Routing, idempotency, concurrency, retries, timeouts

## **Level 5**

**Security**

MAC, PIN blocks, HSM, PCI, cryptography

## **Level 6**

**Production**

Reconciliation, monitoring, certification, failover
