# ISO 8583 — Financial Transaction Messaging Standard

## 1. Introduction

ISO 8583 is an international standard that defines the format of messages exchanged between financial institutions and payment systems for electronic transactions initiated using payment cards.

It defines a common message structure and communication flow for exchanging transaction requests, responses, and other financial messages.

It is commonly used in:
- ATM transactions
- Point-of-sale (POS) payments
- Card authorization
- Financial transfers
- Reversals and refunds
- Settlement and reconciliation
- Network management

Payment networks and financial institutions may implement their own variations of the standard.

### Parts of ISO 8583

The standard is divided into three parts:

1. **Part 1:** Messages, data elements, and code values.
2. **Part 2:** Application and registration procedures for Institution Identification Codes (IIC).
3. **Part 3:** Maintenance procedures for messages, data elements, and code values.

---

## 2. Message Format

An ISO 8583 message generally consists of:

1. Message Type Indicator (MTI)
2. Bitmap(s)
3. Data Elements

```text
+----------------------+----------------------+---------------------------+
| MTI                | Bitmap               | Data Elements            |
+----------------------+----------------------+---------------------------+
| 4 digits            | 8 or 16 bytes       | Transaction information  |
+----------------------+----------------------+---------------------------+
```

### Transaction Flow

A typical card transaction flows through the following systems:

```text
Cardholder
    |
    v
POS / ATM
    |
    v
Acquirer
    |
    v
Payment Network
    |
    v
Issuer
    |
    v
Authorization / Decline
    |
    v
Response returned to terminal
```

The message may contain information about:

- Cardholder account
- Card number
- Transaction amount
- Merchant
- Terminal
- Transaction date and time
- Transaction type
- Currency
- Security information

---

## 3. Message Type Indicator (MTI)

The MTI is a four-digit numeric field that identifies the type and purpose of an ISO 8583 message.

```text
MTI = V C F O
```

| Position | Meaning |
|---|---|
| 1 | ISO 8583 version |
| 2 | Message class |
| 3 | Message function |
| 4 | Message origin |

### 3.1 ISO 8583 Version

The first digit identifies the version of ISO 8583.

| Code | Version |
|---|---|
| 0xxx | ISO 8583:1987 |
| 1xxx | ISO 8583:1993 |
| 2xxx | ISO 8583:2003 |
| 3xxx | Reserved by ISO |
| 8xxx | National use |
| 9xxx | Private use |

### 3.2 Message Class

The second digit identifies the message class.

| Code | Message Class | Description |
|---|---|---|
| x0xx | Reserved | Reserved for ISO |
| x1xx | Authorization | Authorization processing |
| x2xx | Financial | Financial transaction processing |
| x3xx | File Actions | File and card-related exchanges |
| x4xx | Reversal / Chargeback | Reversals and chargeback processing |
| x5xx | Reconciliation | Settlement and reconciliation |
| x6xx | Administrative | Administrative messages |
| x7xx | Fee Collection | Fee-related messages |
| x8xx | Network Management | Network operations |
| x9xx | Reserved | Reserved for ISO |

### 3.3 Message Function

The third digit describes the message function.

| Code | Meaning |
|---|---|
| xx0x | Request |
| xx1x | Request Response |
| xx2x | Advice |
| xx3x | Advice Response |
| xx4x | Notification |
| xx5x | Notification Acknowledgement |
| xx6x | Instruction |
| xx7x | Instruction Acknowledgement |
| xx8x | Reserved |
| xx9x | Negative Acknowledgement in some implementations |

### 3.4 Message Origin

The fourth digit identifies the source or origin of the message.

| Code | Meaning |
|---|---|
| xxx0 | Acquirer |
| xxx1 | Acquirer Repeat |
| xxx2 | Issuer |
| xxx3 | Issuer Repeat |
| xxx4 | Other |
| xxx5 | Other Repeat |
| xxx6–xxx9 | Reserved / implementation-specific |

---

## 4. Common MTI Examples

| MTI | Name | Description |
|---|---|---|
| 0100 | Authorization Request | Requests authorization for a transaction |
| 0110 | Authorization Response | Response to an authorization request |
| 0120 | Authorization Advice | Advice relating to an authorization transaction |
| 0121 | Authorization Advice Repeat | Retransmission of authorization advice |
| 0130 | Authorization Advice Response | Acknowledgement of authorization advice |
| 0200 | Financial Request | Requests financial transaction processing |
| 0210 | Financial Response | Response to a financial request |
| 0220 | Financial Advice | Financial transaction advice |
| 0221 | Financial Advice Repeat | Repeated financial advice |
| 0230 | Financial Advice Response | Response to financial advice |
| 0320 | Batch Upload | Batch transaction upload |
| 0330 | Batch Upload Response | Response to batch upload |
| 0400 | Reversal Request | Requests reversal of a previous transaction |
| 0420 | Reversal Advice | Advice that a reversal has occurred |
| 0430 | Reversal Advice Response | Response to reversal advice |
| 0510 | Settlement Response | Settlement/reconciliation response |
| 0800 | Network Management Request | Logon, echo test, initialization, etc. |
| 0810 | Network Management Response | Response to network management request |
| 0820 | Network Management Advice | Network advice, such as key changes |

---

## 5. Bitmaps

A bitmap indicates which data elements are present in a message.

ISO 8583 uses bitmaps to avoid transmitting empty fields.

### Primary Bitmap

The primary bitmap contains 64 bits and indicates whether Data Elements 1–64 are present.

### Secondary Bitmap

The secondary bitmap contains another 64 bits and indicates whether Data Elements 65–128 are present.

The first bit of the primary bitmap indicates whether a secondary bitmap exists.

### Tertiary Bitmap

Some implementations support a tertiary bitmap to represent Data Elements 129–192.

### Bitmap Representation

A bitmap can be represented as:

- 8 bytes of binary data
- 16 hexadecimal characters

Each bit corresponds to a data element.

### Example

```text
Bitmap:
7010001102C04804
```

This bitmap indicates the presence of the following data elements:

```text
2, 3, 4, 12, 28, 32, 39, 41, 42, 50, 53, 62
```

A set bit (`1`) means the corresponding field is present, while a cleared bit (`0`) means it is absent.

---

## 6. Data Elements

Data elements contain the actual transaction information.

ISO 8583 defines up to:

- 128 data elements in the original 1987 version
- 192 data elements in later versions

Each data element has a defined:

- Field number
- Data type
- Length
- Encoding
- Meaning

Individual payment networks may customize some fields.

### 6.1 Data Types

| Type | Description |
|---|---|
| a | Alphabetic characters and spaces |
| n | Numeric characters |
| x+n | Signed numeric amount |
| s | Special characters |
| an | Alphanumeric |
| as | Alphabetic and special characters |
| ns | Numeric and special characters |
| ans | Alphabetic, numeric and special characters |
| anp | Alphabetic, numeric and pad characters |
| b | Binary data |
| p | Padding character, usually space |
| z | Track data encoding |

### 6.2 Field Length Types

| Type | Description |
|---|---|
| Fixed | Field length is predefined |
| LVAR | Variable length with one-digit length indicator |
| LLVAR | Variable length with two-digit length indicator |
| LLLVAR | Variable length with three-digit length indicator |

Variable-length fields include a length indicator before the actual field data.

### Examples

| Definition | Meaning |
|---|---|
| `n 6` | Fixed numeric field, 6 digits |
| `n.6` | Numeric variable-length field, up to 6 digits |
| `a..11` | Alphabetic LLVAR field, up to 11 characters |
| `b...999` | Binary LLLVAR field, up to 999 bytes |

---

## 7. Common Data Elements

| DE | Type | Description |
|---|---|---|
| 1 | b | Secondary Bitmap |
| 2 | n..19 | Primary Account Number (PAN) |
| 3 | n6 | Processing Code |
| 4 | n12 | Transaction Amount |
| 5 | n12 | Settlement Amount |
| 6 | n12 | Cardholder Billing Amount |
| 7 | n10 | Transmission Date and Time |
| 11 | n6 | System Trace Audit Number (STAN) |
| 12 | n6 | Local Transaction Time |
| 13 | n4 | Local Transaction Date |
| 14 | n4 | Card Expiration Date |
| 18 | n4 | Merchant Category Code (MCC) |
| 22 | n3 | POS Entry Mode |
| 25 | n2 | POS Condition Code |
| 32 | n..11 | Acquiring Institution ID |
| 35 | z..37 | Track 2 Data |
| 37 | an12 | Retrieval Reference Number (RRN) |
| 38 | an6 | Authorization Identification |
| 39 | an2 | Response Code |
| 41 | ans8 | Terminal ID |
| 42 | ans15 | Merchant ID |
| 43 | ans40 | Merchant Name and Location |
| 49 | n3 | Transaction Currency Code |
| 52 | b64 | PIN Data |
| 53 | n16 | Security Control Information |
| 54 | an...120 | Additional Amounts |
| 55 | ans...999 | ICC / EMV Data |
| 60 | ans...999 | Reserved / National Use |
| 61 | ans...999 | Reserved / Private Use |
| 62 | ans...999 | Reserved / Private Use |
| 63 | ans...999 | Reserved / Private Use |
| 64 | b64 | Message Authentication Code (MAC) |
| 70 | n3 | Network Management Information Code |
| 90 | n42 | Original Data Elements |
| 100 | n..11 | Receiving Institution ID |

> Field definitions, formats, and usage may vary by ISO version and payment network.

---

## 8. Processing Code — DE 3

DE 3 identifies the type of transaction being processed.

It is typically six digits:

```text
TT AA SS
```

| Component | Description |
|---|---|
| TT | Transaction Type |
| AA | Account Type |
| SS | Account Type / Transaction Context |

The exact interpretation of the digits depends on the payment network and implementation.

---

## 9. Response Code — DE 39

DE 39 indicates the result of a transaction or request.

It commonly identifies whether a transaction was approved, declined, or rejected for another reason.

Response code definitions vary between implementations and networks.

Examples of common interpretations include:

| Code | Meaning |
|---|---|
| 00 | Approved |
| 05 | Do not honor |
| 12 | Invalid transaction |
| 13 | Invalid amount |
| 14 | Invalid card number |
| 30 | Format error |
| 41 | Lost card |
| 43 | Stolen card |
| 51 | Insufficient funds |
| 54 | Expired card |
| 55 | Incorrect PIN |
| 57 | Transaction not permitted |
| 58 | Transaction not permitted at terminal |
| 61 | Exceeds withdrawal limit |
| 91 | Issuer or switch unavailable |
| 96 | System malfunction |

*Always confirm response codes against the specific processor or network documentation.*

---

## 10. POS Entry Mode — DE 22

DE 22 identifies how the payment information was captured.

Common entry methods include:

- Magnetic stripe
- Chip card
- Contactless card
- Manual card entry
- Electronic commerce
- Mobile payment

The actual values and interpretation depend on the network and ISO version.

---

## 11. Related Standards

ISO 8583 is commonly used alongside other financial and payment standards, including:

- EMV — Chip card payment specifications
- ISO 7813 — Track data-related specifications
- ISO 4909 — Track data-related specifications
- ISO 4217 — Currency codes

---

When implementing ISO 8583, always ensure that the message specification matches the acquiring bank, processor, payment network, or switch you are integrating with.
