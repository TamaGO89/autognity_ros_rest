#!/usr/bin/env python
from rospy import get_param, ROSInterruptException, logerr, logwarn, loginfo, logdebug
from roslib.packages import find_node
from rosnode import kill_nodes, get_node_names
from rosbag.bag import Bag
from subprocess import Popen
from os import listdir, remove, open, read, O_RDONLY, path, mkdir
from requests import Session, ConnectionError
from requests.models import Response
from base64 import urlsafe_b64encode as b64encode, urlsafe_b64decode as b64decode
from Crypto.Hash import SHA256
from Crypto.Random import get_random_bytes
from Crypto.Cipher import AES, PKCS1_OAEP
from Crypto.PublicKey import RSA
from argon2 import hash_password_raw
from argon2.low_level import Type
from json import loads, dumps
from yaml import load
try:
    from yaml import CLoader as Loader
except ImportError:
    from yaml import Loader
try:
    from Queue import Queue
except ImportError:
    from queue import Queue


# Get the directory for the records
def rec_path():
    # TODO : Change the directory
    temp_directory = path.dirname(path.abspath(__file__)) + get_param('ros_rest/rec_path', '/temp')
    if not path.exists(temp_directory):
        mkdir(temp_directory)
    return temp_directory


# Retrieve the Password derived Key
def kdf(password):  # type: (str) -> str
    return b64encode(hash_password_raw(time_cost=get_param('ros_rest/a2_time', 16),
                                       memory_cost=get_param('ros_rest/a2_mem', 32768),
                                       parallelism=get_param('ros_rest/a2_paral', 2),
                                       salt=b64decode(get_param('ros_rest/a2_salt')),
                                       hash_len=AES.key_size[get_param('ros_rest/aes_size', 2)],
                                       password=password.encode(),
                                       type=Type.ID))


''' RestCipher : Manage cryptography, given a key performs encryption and decryption of messages '''


class RestCipher(object):

    def __init__(self, key=None, iv=None):  # type: (str, str) -> None
        if key:
            self.__key = SHA256.new(key.encode()).digest()
        else:
            self.__key = get_random_bytes(AES.key_size[get_param('ros_rest/aes_size', 2)])
        self.__iv = iv if iv else chr(0) * AES.block_size
        self.__temp = chr(0) * AES.block_size
        self.__pad = AES.block_size

    def reset(self):  # type: () -> RestCipher
        self.__temp = self.__iv
        return self

    # Encrypt and decrypt data, get random strings
    def encrypt(self, raw):  # type: (str) -> str
        enc = AES.new(self.__key, AES.MODE_CBC, self.__temp).encrypt(raw)
        self.__temp = enc[-self.__pad:]  # TODO : change the encryption scheme, CBC sucks
        return b64encode(enc)

    def decrypt(self, enc):  # type: (str) -> str
        return self.unpad(AES.new(self.__key, AES.MODE_CBC, self.__iv).decrypt(b64decode(enc)))

    def random(self, size=None):
        return get_random_bytes(size if size else self.__pad)

    # Return the encryption key, the initialization vector and the block size
    def key(self):  # type: () -> str
        return self.__key

    def iv(self):
        return self.__iv

    def block_size(self):
        return self.__pad

    # Pad and unpad the plaintext for encryption and after decryption
    def pad(self, s):  # type: (str) -> str
        return s + (self.__pad - len(s) % self.__pad) * chr(self.__pad - len(s) % self.__pad)

    def unpad(self, s):
        if ord(s[-1]) <= self.__pad:
            return s[:-ord(s[-1])]
        else:
            return s


# Stop the current running node
def rec_stop():  # type: () -> list
    if ('/' + get_param('ros_rest/rec_node', 'rest_bag_record')) in get_node_names():
        return kill_nodes([get_param('ros_rest/rec_node', 'rest_bag_record')])


# Start a new RECORD with a given BagConfig
def rec_start(cfg):  # type: (BagConfig) -> Popen
    try:
        # Prepare the command_line, command name and output path
        cmd = [find_node('rosbag', 'record')[0]]
        cmd.extend(['--output-prefix', rec_path() + get_param('ros_rest/rec_prefix') + str(cfg.id)])
        # Specify the topics to record
        cmd.extend(['--regex', cfg.regex] if cfg.regex else
                   ['--exclude', cfg.exclude] if cfg.exclude else
                   ['--all'])
        # Set the limits for splits, duration and size
        cmd.extend(['--split',
                    '--max-splits', str(cfg.splits) if cfg.splits else get_param('ros_rest/rec_splits', 5),
                    '--size', str(cfg.size) if cfg.size else get_param('ros_rest/rec_size', '50'),
                    '--duration', str(cfg.duration) if cfg.duration else get_param('ros_rest/rec_time', '10')])
        cmd.extend(['--' + (cfg.compression if cfg.compression else get_param('ros_rest/rec_zip', 'lz4'))])
        # Set the name of the node to easily check it's state
        cmd.extend(['__name:=' + get_param('ros_rest/rec_node', 'rest_bag_record')])
        print(cmd)
        return Popen(cmd)
    except Exception as e:  # The command might be missing or the BagConfig might be None
        logerr('ros_rest : rec_start : ' + str(e))


# Return all the RECORDS newer than LAST in ascending order
def list_dir(last):  # type: (str) -> list
    dir_files = [BagFile(rec_path(), f) for f in listdir(rec_path())
                 if f.endswith('.bag') and f.split('.')[0].split('_')[2] > last]
    dir_files.sort(key=lambda g: g.bag.split('_')[2])
    return dir_files


# Delete a Record from the hard drive
def delete(path):  # type: (str) -> None
    try:
        if path.endswith('.bag'):
            remove(path)
        else:
            raise RuntimeError('The thread tried to delete a sensible file')
    except (OSError, RuntimeError) as e:
        logerr('ros_rest : delete : ' + str(e))


# Return the secure hash of the message
def digest(message):  # type: (str) -> str
    return b64encode(SHA256.new(message).digest())


# RSA encryption with the provided key
def secret_dict(keys, sec):  # type: (dict, str) -> list
    print(b64encode(sec))
    return [{'name': k['name'],
             'content': b64encode(PKCS1_OAEP.new(RSA.importKey(b64decode(k['content'].encode()))).encrypt(sec))}
            for k in keys]


''' RestSession : Manage the session with the server, the cookies, the authentication scheme and the exceptions'''


class RestError(ConnectionError):
    def __init__(self, err):  # type: (str) -> None
        super(RestError, self).__init__(err)
        pass


class RestSession(object):

    def __init__(self, key, size=1):  # type: (int) -> None
        self.__host = get_param('ros_rest/host')
        print '\n\n',key,'\n\n'
        c = RestCipher(kdf(key))
        username = c.decrypt(get_param('ros_rest/username'))
        password = c.decrypt(get_param('ros_rest/password'))
        self.__auth = (username, password)
        self.__token_name = get_param('ros_rest/token', 'Token')
        self.__session = Session()
        self.__q = Queue()
        self.__q.put(True)
        self.res_ok = [200, 201, 202, 204]
        self.res_re = [301, 303]
        self.res_er = [404, 405, 500]
        self.res_au = [401, 403, 500]
        self.__paths = loads(self.__request('GET', get_param('ros_rest/paths')).text)
        print(self.__paths)

    def get_text(self, url, **kwargs):  # type: (str, dict) -> dict
        return loads(self.__request('GET', self.__paths.get(url), **kwargs).text)

    def get_code(self, url, **kwargs):  # type: (str, dict) -> int
        return self.__request('GET', self.__paths.get(url), **kwargs).status_code

    def get_dict(self, url, **kwargs):  # type: (str, dict) -> dict
        res = self.__request('GET', self.__paths.get(url), **kwargs)
        return {'code': res.status_code, 'text': loads(res.text), 'id': res.headers.get('id')}

    # Post a resource to the server, managing potential exceptions
    def post_text(self, url, **kwargs):  # type: (str, dict) -> dict
        loginfo('post request')
        return loads(self.__request('POST', self.__paths.get(url), **kwargs).text)

    def post_code(self, url, **kwargs):  # type: (str, dict) -> int
        return self.__request('POST', self.__paths.get(url), **kwargs).status_code

    def post_dict(self, url, **kwargs):  # type: (str, dict) -> dict
        res = self.__request('POST', self.__paths.get(url), **kwargs)
        return {'code': res.status_code, 'text': loads(res.text), 'id': res.headers.get('id')}

    # Private method for post and get requests
    def __request(self, method, url, **kwargs):  # type: (str, str, dict) -> Response
        self.__q.get()
        res = None
        try:
            res = self.__session.request(method, self.__host + url,
                                         auth=None if self.__session.cookies.__contains__(self.__token_name)
                                         else self.__auth, **kwargs)
            log = "{} : {} {}".format(res.headers.get('id'), url, res.status_code)
            if res.status_code in self.res_au:
                raise RestError(log)
            if res.status_code not in self.res_ok:
                logwarn('ros_rest : ' + log)
            else:
                loginfo('ros_rest : ' + log)
        except (RestError, ConnectionError, BaseException) as err:
            self.__session = Session()
            logerr('ros_rest : ' + str(err))
        finally:
            self.__q.put(True)
            self.__q.task_done()
            return res


''' BagConfig : Rapresent the configuration of a record '''


class BagConfig(object):

    def __init__(self, data):  # type: (dict) -> None
        self.__data = loads(b64decode(data.get('content').encode()))
        self.id = data.get('id')
        self.created = data.get('created')
        self.size = self.__data.get('max_size')
        self.duration = self.__data.get('duration')
        self.regex = self.__data.get('regex')
        self.exclude = self.__data.get('exclude')
        self.splits = self.__data.get('max_splits')
        self.compression = self.__data.get('compression')

    def __eq__(self, other):  # type: (BagConfig) -> bool
        try:
            return other.id == self.id
        finally:
            return False

    def get(self, key):  # type: (str) -> object
        return self.__data.get(key)

    def contains(self, key):  # type: (str) -> bool
        return self.__data.__contains__(key)


''' BagFile : Rapresent a record on the hard drive, provides informations and allow to delete it from the hard drive '''


class BagFile(object):

    def __init__(self, path, bag):  # type: (str, str) -> None
        self.__path = path
        self.bag = bag
        self.__raw = None

    def info(self):  # type: () -> str
        # TODO : Remove this protected function and use the properties provided by the class Bag
        return dumps(load(Bag(self.__path + '/' + self.bag)._get_yaml_info(), Loader=Loader))

    def read(self, b):  # type: (int) -> str
        if not self.__raw:
            self.__load()
        return read(self.__raw, b)

    def config(self):  # type: () -> int
        return int(self.bag.split('.')[0].split('_')[1])

    def delete(self):  # type: () -> None
        delete(self.__path + '/' + self.bag)

    def reset(self):  # type: () -> BagFile
        self.__load()
        return self

    def __load(self):  # type: () -> None
        self.__raw = open(self.__path + '/' + self.bag, O_RDONLY)
