import base64
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

from app.services.SymetricEncryptionService.key_derivation import derive_key

NONCE_LEN = 12
TAG_LEN = 16


class DecryptionError(RuntimeError):
    pass


def decrypt_ssh_password(encrypted_ssh_key: str, master_secret: bytes) -> str:
    try:
        raw = base64.b64decode(encrypted_ssh_key)
    except Exception:
        raise DecryptionError("Invalid base64 payload")

    # Minimum: nonce + tag + at least 1 byte ciphertext
    if len(raw) < NONCE_LEN + TAG_LEN + 1:
        raise DecryptionError("Ciphertext too short")

    nonce = raw[:NONCE_LEN]
    ciphertext_and_tag = raw[NONCE_LEN:]

    key = derive_key(master_secret)
    aesgcm = AESGCM(key)

    try:
        plaintext_bytes = aesgcm.decrypt(
            nonce=nonce,
            data=ciphertext_and_tag,
            associated_data=None,
        )
    except Exception:
        raise DecryptionError("Decryption failed")

    try:
        return plaintext_bytes.decode("utf-8")
    finally:
        del plaintext_bytes
