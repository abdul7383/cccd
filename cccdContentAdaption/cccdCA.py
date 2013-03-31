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
import pymongo
from pymongo.objectid import ObjectId
import gridfs

conf_file="./cccdMM.conf"
message_broker_ip="10.0.0.140"

#Setting for the logger
logger_setting={
"name": 		"cccdMM",							#logger name
"logger_filename": 	"/var/log/cccdMM.log",						#log file name
"logger_loglevel": 	"DEBUG",							#log level: "DEBUG", "WARNING", "INFO" or "ERROR"	
"logger_formatter": 	"%(asctime)s - %(name)s - %(levelname)s - %(message)s",		#format of log messages
"logger_toconsole": 	"1",								#specify whether to log to the console or not. "1" -> Yes ; "0" -> No
"logger_maxBytes": 	"10485760",							#max file size of the log file, 10485760 = 10 MB
"logger_backupCount": 	"2"								#if logger_backupCount is non-zero, the system will save old log files by appending the extensions ".1", ".2" etc., to the filename
}

########################################################################################

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
		logger.debug("created")
	logger.debug(" [x] Done, send ack to this message: %r" % (body,))
	ch.basic_ack(delivery_tag = method.delivery_tag)

####################################################################

logger=init_logger(logger_setting)

connection = pika.BlockingConnection(pika.ConnectionParameters(
        host='10.0.0.140'))
channel = connection.channel()

channel.queue_declare(queue='cccdMM', durable=True)

logger.debug(' [*] Waiting for messages.')
channel.basic_qos(prefetch_count=1)
channel.basic_consume(callback,queue='cccdMM')

channel.start_consuming()
