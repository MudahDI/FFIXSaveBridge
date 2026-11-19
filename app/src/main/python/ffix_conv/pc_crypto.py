"""Android AES bridge for the MIT FFIX conversion engine.

The conversion engine's format logic remains Python. AES/PBKDF2 is delegated to
Android's standard JCA provider, avoiding a native Python crypto dependency.
"""
from com.mudahdi.ffixsavebridge import RrCryptoBridge

class DecryptError(ValueError):
    pass

def cipher_size(plain_size):
    return plain_size + 16 - plain_size % 16

def encrypt(plain):
    return bytes(RrCryptoBridge.encrypt(bytes(plain)))

def decrypt(cipher):
    try:
        return bytes(RrCryptoBridge.decrypt(bytes(cipher)))
    except Exception as exc:
        raise DecryptError(str(exc)) from exc
