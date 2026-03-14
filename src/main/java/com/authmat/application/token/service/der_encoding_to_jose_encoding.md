# Understanding DER → JOSE Conversion for ECDSA Signatures

This document explains how DER-encoded ECDSA signatures are structured and how the Java code parses them to convert into the JOSE format used by JWTs.

---

# 1. DER Signature Structure

AWS KMS returns ECDSA signatures encoded using **ASN.1 DER**.

The structure looks like this:

```
0x30 <len>
   0x02 <rLen> <r bytes>
   0x02 <sLen> <s bytes>
```

Meaning:

- `0x30` → ASN.1 **SEQUENCE tag**
- `<len>` → total number of bytes inside the sequence
- `0x02` → ASN.1 **INTEGER tag**
- `<rLen>` → length of the **R value**
- `<r bytes>` → R integer value
- `0x02` → ASN.1 **INTEGER tag**
- `<sLen>` → length of the **S value**
- `<s bytes>` → S integer value

So conceptually the structure is:

```
Sequence {
    Integer R
    Integer S
}
```

---

# 2. Example DER Byte Array

Example signature:

```
30 44 02 20 <32 bytes of R> 02 20 <32 bytes of S>
```

Array view:

```
Index:      0     1     2     3     4...
Value:     0x30 0x44  0x02  0x20  <r bytes...>
```

Explanation:

```
arr[0] = 0x30
→ ASN.1 SEQUENCE tag

arr[1] = 0x44
→ length of the sequence contents

0x44 = 68
→ meaning 68 bytes follow after index 1
```

So the array layout becomes:

```
30 <len>                  (metadata)

   02 <rLen> <r bytes>    (R integer)
   02 <sLen> <s bytes>    (S integer)
```

The first two bytes are **metadata**, not actual signature values.

---

# 3. Why `offset = 2`

The parser skips the metadata:

```
0x30
<length>
```

So parsing begins at index `2`.

```
offset = 2
```

Meaning:

```
start reading the first INTEGER (R)
```

---

# 4. Java vs DER Byte Ranges

Java bytes are **signed**.

```
Java byte range
[-128, 127]
```

DER bytes are **unsigned**.

```
DER byte range
[0, 255]
```

Example problem:

```
0xFF = 255
```

Binary:

```
11111111
```

Java interprets this as:

```
-1
```

Because Java bytes are signed.

To treat the byte as **unsigned**, the code does:

```
byteValue & 0xFF
```

Why?

```
0xFF = 11111111
```

Example:

```
11111111
&
11111111
--------
11111111
```

Result:

```
255
```

So:

```
der[i] & 0xFF
```

means:

```
interpret this byte as an unsigned value
```

---

# 5. DER Length Encoding Modes

DER supports two ways to encode lengths.

## Short-form length

Used when the length fits in **7 bits**.

Binary format:

```
0xxxxxxx
```

Example:

```
0x44 = 01000100
```

First bit:

```
0
```

Meaning:

```
this byte directly represents the length
```

---

## Long-form length

Used when length > 127.

Binary format:

```
1xxxxxxx
```

The remaining bits indicate how many bytes contain the actual length.

Example:

```
0x82
```

Binary:

```
10000010
```

Meaning:

```
2 additional length bytes follow
```

To extract the number of extra bytes:

```
0x82 & 0x7F
```

```
10000010
&
01111111
--------
00000010
```

Result:

```
2
```

---

# 6. Understanding the Java Parsing Code

Below is the Java method that parses DER and extracts `R` and `S`.

```java
private byte[] derToJoseEcdsa(byte[] der) {

    // Skip ASN.1 sequence metadata
    int offset = 2;

    // Convert signed Java byte to unsigned integer
    int lengthByte = der[1] & 0xFF;

    // Check if DER uses long-form length encoding
    if (lengthByte > 0x80) {
        offset += (der[1] & 0x7F);
    }

    // Parse R
    if (der[offset] != 0x02) {
        throw new IllegalArgumentException("Invalid DER: expected INTEGER tag for R");
    }

    int rLen = der[offset + 1] & 0xFF;

    byte[] r = Arrays.copyOfRange(
        der,
        offset + 2,
        offset + 2 + rLen
    );

    offset += 2 + rLen;

    // Parse S
    if (der[offset] != 0x02) {
        throw new IllegalArgumentException("Invalid DER: expected INTEGER tag for S");
    }

    int sLen = der[offset + 1] & 0xFF;

    byte[] s = Arrays.copyOfRange(
        der,
        offset + 2,
        offset + 2 + sLen
    );

    // R || S concatenation happens later
}
```

---

# 7. What the Code is Actually Doing

Step-by-step:

1. Skip sequence metadata

```
0x30 <len>
```

2. Handle possible long-form length encoding.

3. Verify the next tag is an INTEGER (`0x02`).

4. Read the length of `R`.

5. Extract `R`.

6. Move the offset forward.

7. Repeat the process for `S`.

---

# 8. Final Goal

DER signature format:

```
Sequence {
    Integer R
    Integer S
}
```

JWT (JOSE) requires:

```
R || S
```

Meaning the final output is simply:

```
[R bytes][S bytes]
```

This is why the DER structure must be parsed manually.