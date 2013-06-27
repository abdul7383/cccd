#!/usr/bin/env python
'''
Created on 19.12.2012

@author: hamood
'''

import pika
import time
from string import Template
import sys
import os
import stat
import shutil
import simplejson as json
import logging
import logging.handlers

message_broker_ip="10.0.0.140"
locations_dir="/usr/local/nginx/locations/"
www_dir="/usr/local/nginx/html/"
nginx_bin="/usr/local/nginx/sbin/nginx"

location_tmpl = Template('location /$app/ {\n\
        secure_link $arg_st,$arg_e;\n\
        secure_link_md5 $secret$uri$arg_e;\n\
        if ($secure_link = "") {\n\
                return 403;\n\
        }\n\
        if ($secure_link = "0") {\n\
                return 403;\n\
        }\n\
}\n\
location ~ ^/$app/.*\.m3u8$ {\n\
        set $secure_word $secret;\n\
        secure_link $arg_st,$arg_e;\n\
        secure_link_md5 $secret$uri$arg_e;\n\
        if ($secure_link = "") {\n\
                return 403;\n\
        }\n\
        if ($secure_link = "0") {\n\
                return 403;\n\
        }\n\
        content_by_lua_file /usr/local/nginx/sbin/m3u8.lua;\n\
}\n\
')

#Setting for the logger
logger_setting={
"name": 		"cccdCDN",							#logger name
"logger_filename": 	"/var/log/cccdCDN.log",						#log file name
"logger_loglevel": 	"DEBUG",							#log level: "DEBUG", "WARNING", "INFO" or "ERROR"	
"logger_formatter": 	"%(asctime)s - %(name)s - %(levelname)s - %(message)s",		#format of log messages
"logger_toconsole": 	"1",								#specify whether to log to the console or not. "1" -> Yes ; "0" -> No
"logger_maxBytes": 	"10485760",							#max file size of the log file, 10485760 = 10 MB
"logger_backupCount": 	"2"								#if logger_backupCount is non-zero, the system will save old log files by appending the extensions ".1", ".2" etc., to the filename
}

########################################################################################

def handleRemoveReadonly(func, path, exc):
  excvalue = exc[1]
  if func in (os.rmdir, os.remove) and excvalue.errno == errno.EACCES:
      os.chmod(path, stat.S_IRWXU| stat.S_IRWXG| stat.S_IRWXO) # 0777
      func(path)
  else:
      raise

def init_logger(settings):
    logger=logging.getLogger(settings['name'])
    logfilename=settings['logger_filename']
    if(settings['logger_loglevel']=="DEBUG"):
        loglevel=logging.DEBUG
    elif settings['logger_loglevel']=="INFO":
        loglevel=logging.INFO
    elif settings['logger_loglevel']=="WARNING":
        loglevel=logging.WARNING
    else:
        loglevel=logging.ERROR
    
    logformatter=logging.Formatter(settings['logger_formatter'])
    logger.setLevel(loglevel)
    if(settings['logger_toconsole']=="1"):
        ch1 = logging.StreamHandler()
        ch1.setLevel(loglevel)
        ch1.setFormatter(logformatter)
        logger.addHandler(ch1)
    ch2 = logging.handlers.RotatingFileHandler(logfilename, maxBytes=int(settings['logger_maxBytes']), backupCount=int(settings['logger_backupCount']))
    ch2.setLevel(loglevel)
    ch2.setFormatter(logformatter)
    logger.addHandler(ch2) 
    return logger

def callback(ch, method, properties, body):
	logger.debug(" [x] Received %r" % (body,))
	#tmp = json.dumps(body)
	jsonBody = json.loads(body)
	if(jsonBody['status']=="created"):
		file = open(locations_dir+jsonBody['appName']+'.conf', 'w+')
		file.write(str(location_tmpl.safe_substitute(dict(app=jsonBody['appName'],secret=jsonBody['secret']))))
		file.close()
		if not os.path.exists(www_dir+jsonBody['appName']):
    			os.makedirs(www_dir+jsonBody['appName'])
		#reload the nginx server
		os.system("%s %s %s"%(nginx_bin, '-s', 'reload'))
	if(jsonBody['status']=="deleted"):
		try:
			os.remove(locations_dir+jsonBody['appName']+'.conf')
		except Exception, e:
			logger.debug(e)
		try:
			shutil.rmtree(www_dir+jsonBody['appName'], ignore_errors=False, onerror=handleRemoveReadonly)
		except Exception, e:
			logger.debug(e)
		#reload the nginx server
		os.system("%s %s %s"%(nginx_bin, '-s', 'reload'))

	logger.debug(" [x] Done, send ack to this message: %r" % (body,))
	ch.basic_ack(delivery_tag = method.delivery_tag)

####################################################################

logger=init_logger(logger_setting)

connection = pika.BlockingConnection(pika.ConnectionParameters(
        host=message_broker_ip))
channel = connection.channel()

channel.queue_declare(queue='AppEvents', durable=True)

logger.debug(' [*] Waiting for messages.')
channel.basic_qos(prefetch_count=1)
channel.basic_consume(callback,queue='AppEvents')

channel.start_consuming()
