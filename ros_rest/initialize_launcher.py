#!/usr/bin/python
from xml.etree.ElementTree import parse
from sys import argv, exit
from getopt import getopt, GetoptError
from re import compile, match
from base64 import urlsafe_b64encode as b64encode

msg = '''\tstore the password in a safe place, it cannot be recovered : {0}
install the package with catkin and run it with the following command, the key parameter is required
\troslaunch ros_rest ros_rest.launch key:={0}

remember to check the dependencies before you launch the package:
\trequests : pip install requests
\tCrypto : pip install pycrypto
\targon2 : pip install argon2-cffi'''
help = '''initialize_launcher.py -u <username> -p <password> -k <key>
\t-u --username : the username of the machine : 16 characters long string : ^[A-Za-z0-9_/+-]{16}$
\t-p --password : the password of the machine : 16 characters long string : ^[A-Za-z0-9_/+-]{16}$
\t-k --key : a secret key to protect the credential on the machine from unintended manipulation
\t\tthe secret key must contain at least one uppercase character (?=.*[A-Z]),
\t\tone lowercase character (?=.*[a-z]), one digit (?=.*[0-9])
\t\tand one special character (?=.*[_/+-]), with a length between 8 and 16 {8,16}
\t\t^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[_/+-])[A-Za-z0-9_/+-]{8,16}$

the required dependencies are:
\trequests : pip install requests
\tCrypto : pip install pycrypto
\targon2 : pip install argon2-cffi'''
params = {}

try:
    from Crypto.Hash import SHA256
    from Crypto.Random import get_random_bytes
    from Crypto.Cipher import AES
    from argon2 import hash_password_raw
    from argon2.low_level import Type
except ImportError:
    print(help)
    exit(2)


def kdf(key, salt):  # type: (str, str) -> str
    return b64encode(hash_password_raw(time_cost=params.get('a2_time', 16), memory_cost=params.get('a2_mem', 32768),
                                       parallelism=params.get('a2_paral', 2), password=key.encode(),
                                       hash_len=AES.key_size[params.get('aes_size', 2)], type=Type.ID, salt=salt))


class RestCipher(object):

    def __init__(self, key=None):  # type: (str) -> None
        self.__key = SHA256.new(key.encode()).digest()
        self.__iv = chr(0) * AES.block_size
        self.__pad = AES.block_size

    # Encrypt and decrypt data, get random strings
    def encrypt(self, raw):  # type: (str) -> str
        enc = AES.new(self.__key, AES.MODE_CBC, self.__iv).encrypt(raw)  # TODO : CBC sucks, change it
        return enc

    # Pad the plaintext for encryption
    def pad(self, s):  # type: (str) -> str
        return s + (self.__pad - len(s) % self.__pad) * chr(self.__pad - len(s) % self.__pad)


def main(arguments):
    reg_up = compile(params.get('regex_user_pass', '^[A-Za-z0-9_/+-]{16}$'))
    reg_key = compile(params.get('regex_key', '^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[_/+-])[A-Za-z0-9_/+-]{8,16}$'))
    username = ''
    password = ''
    key = ''
    try:
        for opt, arg in getopt(arguments, "hu:p:k:", ["username=", "password=", "key="])[0]:
            if opt in ("-h", "--help"):
                print(help)
                exit()
            elif opt in ("-u", "--username"):
                username = arg
            elif opt in ("-p", "--password"):
                password = arg
            elif opt in ("-k", "--key"):
                key = arg
        if not (match(reg_up, username) and match(reg_up, password) and match(reg_key, key)):
            raise GetoptError('')
        launch_xml = parse('launch/ros_rest.launch')
        for node in launch_xml.findall('.//param'):
            params[node.attrib['name']] = int(node.attrib['value'])\
                if node.attrib['type'] == 'int' else node.attrib['value']
        kdf_salt = get_random_bytes(params.get('a2_salt_length', 32))
        kdf_key = kdf(key, kdf_salt)
        cipher = RestCipher(key=kdf_key)
        for node in launch_xml.findall('.//param'):
            if node.attrib['name'] == 'username':
                node.attrib['value'] = b64encode(cipher.encrypt(username))
            elif node.attrib['name'] == 'password':
                node.attrib['value'] = b64encode(cipher.encrypt(password))
            elif node.attrib['name'] == 'a2_salt':
                node.attrib['value'] = b64encode(kdf_salt)
        launch_xml.write('launch/ros_rest.launch')
        print(msg.format(key))
    except GetoptError:
        print(help)
        exit(2)


if __name__ == "__main__":
    main(argv[1:])
