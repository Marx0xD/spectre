import threading
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.primitives import hashes
from app.config.config import settings

_cached_key: bytes | None = None
_lock = threading.Lock()


SALT = settings.KDF_SALT
ITERATIONS = settings.KDF_ITERATIONS
KEY_LENGTH = settings.KEY_LENGTH_BITS
LENGTH_BITS = settings.KEY_LENGTH_BITS
NONCE_LENGTH = settings.NONCE_LEN
TAG_LENGTH = settings.TAG_LEN

def derive_key(secret:bytes)->bytes:
    global  _cached_key
    if _cached_key is not None:
        return  _cached_key
    with _lock:
        if _cached_key is not None:
            return _cached_key
        kdf = PBKDF2HMAC(
            algorithm= hashes.SHA256(),
            length=32,
            salt=SALT,
            iterations=ITERATIONS,
        )
        _cached_key = kdf.derive(secret)
        return _cached_key



