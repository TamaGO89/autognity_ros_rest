#!/usr/bin/env python
from utils import RestCipher, RestSession, rec_start, rec_stop, digest, secret_dict, BagConfig, list_dir
from rospy import get_param, is_shutdown, init_node, sleep, loginfo, logdebug, logwarn, logerr, logfatal, DEBUG
from sys import exit, argv
from os import _exit
from datetime import datetime
from time import mktime
from threading import Thread, Event
from base64 import urlsafe_b64encode as b64encode
try:
    from Queue import Queue
except ImportError:
    from queue import Queue


# Common variables
_bags = None
_session = None
_event = Event()
_err_sleep = 60.0


# Getters for threads and daemons
def thread(thr, dmn=False, **kwargs):
    th = thr(**kwargs)
    th.daemon = dmn
    th.start()
    return th


''' RestThread : SuperClass implemented by Threads that use the shared Session '''


class RestThread(Thread):

    def __init__(self, name, slp=5.0):  # type: (str, float) -> None
        self.sleep = slp
        self._join = False
        super(RestThread, self).__init__(name=name)

    def set_join(self):
        self._join = True


''' ThreadRecord : Retrieve the configurations from the Server and start the records '''


class ThreadRecord(RestThread):

    def __init__(self, name='record', slp=60.0):  # type: (str, float) -> None
        super(ThreadRecord, self).__init__(name, slp)
        self.__created = 0
        self.__format = '%Y-%m-%d %H:%M:%S.%f'

    def run(self):  # type: () -> None
        while not self._join:
            loginfo('ros_rest : ' + self.name + ' : searching for latest configuration, restart the record')
            # Check for new settings to apply to the recording, restart the recording
            try:
                cfg = BagConfig(_session.get_text('config', params={'from': self.__created})['config'])
                self.__created = long(mktime(datetime.strptime(cfg.created, self.__format).utctimetuple()) * 1000)
                rec_start(cfg)
            except BaseException as err:
                logwarn('ros_rest : ' + self.name + ' : ' + str(err))
                _event.wait(_err_sleep)
            _event.wait(self.sleep)
        rec_stop()
        self.join()


''' ThreadInfo : Checks for new bags, put them in the queue '''


class ThreadSearch(RestThread):

    def __init__(self, name='search', slp=60.0):  # type: (str, float) -> None
        super(ThreadSearch, self).__init__(name, slp)
        self.__last = ''

    def run(self):  # type: () -> None
        while not self._join:
            loginfo('ros_rest : ' + self.name + ' : looking for new records')
            try:
                # Check for new sorted records to send, if there is send it to the server using the shared session
                for bag in list_dir(self.__last):
                    _bags.put(bag)
                    self.__last = bag.bag.split('.')[0].split('_')[-1]
            except BaseException as err:
                logwarn('ros_rest : ' + self.name + ' : ' + str(err))
                _event.wait(_err_sleep)
            _event.wait(self.sleep)
        _bags.join()
        self.join()


''' ThreadRaw : looks for new bags in the queue, upload them to the server '''


class ThreadUpload(RestThread):

    def __init__(self, name='upload', slp=0.2, sz=192, rtr=3):  # type: (str, float, int, int) -> None
        super(ThreadUpload, self).__init__(name, slp)
        self.__size = sz
        self.__retry = rtr

    def run(self):

        class Uploader(object):

            def __init__(self, info_id, rt, cph):  # type: (long, int, RestCipher) -> None
                self.__cipher = cph
                self.__info_id = info_id
                self.__dig = ''
                self.__retry = rt

            def upload(self, tmp, last=False):  # type: (str, bool) -> None
                enc = self.__cipher.encrypt(self.__cipher.pad(tmp) if last else tmp)
                self.__dig = digest(self.__dig + ":" + enc)
                res = False
                for _ in range(self.__retry):
                    if _session.post_code('raw', json={'id': self.__info_id, 'content': enc,
                                                       'hash': self.__dig, 'last': last}) in _session.res_ok:
                        res = True
                        break
                if not res:
                    raise RuntimeError('inconsistency in the chunk sequence')

        while not self._join:
            loginfo('ros_rest : ' + self.name + ' : waiting for a new record')
            # Wait for a new Record to upload from the Queue and initialize the variables
            bag = _bags.get()
            cipher = RestCipher()
            try:
                loginfo('ros_rest : ' + self.name + ' : public key retrieval, record information upload')
                # Upload the bag description: config_id, content, dictionary of secrets, get the newly created bag_id
                secrets = secret_dict(_session.get_text( 'keys', params={"id": bag.config()})['list'], cipher.key())
                info_id = _session.post_text('info', json={'id': bag.config(),
                                                           'content': cipher.encrypt(cipher.pad(bag.info())),
                                                           'secrets': secrets})['id']
                uploader = Uploader(info_id, self.__retry, cipher.reset())
                loginfo('ros_rest : ' + self.name + ' : record raw data upload')
                raw = bag.read(self.__size * cipher.block_size())
                while True:
                    _event.wait(self.sleep)
                    temp = raw
                    raw = bag.read(self.__size * cipher.block_size())
                    if raw == '':
                        break
                    uploader.upload(temp, last=False)
                uploader.upload(temp, last=True)
                # when the upload is over notify the server to finalize the Record
                loginfo('ros_rest : ' + self.name + ' : delete the record')
                bag.delete()
            except BaseException as err:
                _bags.put(bag.reset())
                logerr('ros_rest : ' + self.name + ' : ' + str(err))
            finally:
                _bags.task_done()
        self.join()


if __name__ == '__main__':
    init_node('ros_rest', log_level=DEBUG)
    t_record = t_search = None
    t_upload = []
    _bags = Queue(maxsize=get_param('ros_rest/queue_size', 10))
    _session = RestSession(argv[1], size=get_param('ros_rest/session_size', 1))
    _err_sleep = get_param('ros_rest/exception_sleep', 60.0)
    try:
        t_record = thread(ThreadRecord, dmn=False, slp=60.0, name='record')
        t_search = thread(ThreadSearch, dmn=False, slp=60.0, name='search')
        t_upload = [thread(ThreadUpload, dmn=False, slp=0.2, name='upload_'+str(i),
                           sz=get_param('ros_rest/raw_size', 192), rtr=get_param('ros_rest/post_retry', 3))
                    for i in range(get_param('ros_rest/thread_size', 5))]
        while not is_shutdown():
            logdebug('ros_rest : master : checking...')
            try:
                if not t_record.isAlive():
                    t_record = thread(ThreadRecord, dmn=False, slp=t_record.sleep, name=t_record.name)
                if not t_search.isAlive():
                    t_search = thread(ThreadSearch, dmn=False, slp=t_search.sleep, name=t_search.name)
                for t in t_upload:
                    if not t.isAlive():
                        t = thread(ThreadUpload, dmn=False, slp=t.sleep, name=t.name,
                                   sz=get_param('ros_rest/raw_size', 96), rtr=get_param('ros_rest/post_retry', 3))
            except RuntimeError as e:
                logerr('ros_rest : master : ' + str(e))
            sleep(5.0)
        raise Exception('Interrupted')
    except ValueError as e:
        logfatal('ros_rest : master : ' + str(e))
    except BaseException as e:
        logwarn('ros_rest : master : ' + str(e))
    finally:
        t_record.set_join()
        t_search.set_join()
        for t in t_upload:
            t.set_join()
        _event.set()
        try:
            exit(0)
        except SystemExit:
            _exit(0)
