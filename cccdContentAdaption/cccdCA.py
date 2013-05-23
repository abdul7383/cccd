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
import urllib2, base64
import tempfile
import subprocess
import contextlib

working_dir="/home/ubuntu/cccd/cccdContentAdaption/hls/"
conf_file="/home/ubuntu/cccd/cccdContentAdaption/cccdCA.conf"
message_broker_ip="10.0.0.140"
adminUsername="admin"
adminPassword="superpass"
cccdAppManagementIP="10.0.0.197"
cccdCDN="10.0.0.209"

#Setting for the logger
logger_setting={
"name": 		"cccdCA",							#logger name
"logger_filename": 	"/var/log/cccdCA.log",						#log file name
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

# Unix, Windows and old Macintosh end-of-line
newlines = ['\n', '\r\n', '\r']
def unbuffered(proc, stream='stdout'):
    stream = getattr(proc, stream)
    with contextlib.closing(stream):
        while True:
            out = []
            last = stream.read(1)
            # Don't loop forever
            if last == '' and proc.poll() is not None:
                break
            while last not in newlines:
                out.append(last)
                last = stream.read(1)
            out = ''.join(out)
            yield out

def runSegmenter(tmpname):
    cmd = ['/usr/bin/ruby', '/home/ubuntu/cccd/cccdContentAdaption/hls/http_streamer.rb', tmpname]
    proc = subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        # Make all end-of-lines '\n'
        universal_newlines=True,
    )
    for line in unbuffered(proc):
        print line

def callback(ch, method, properties, body):
	logger.debug(" [x] Received %r" % (body,))
	#tmp = json.dumps(body)
	jsonBody = json.loads(body)
	if(jsonBody['status']=="created"):
		url="http://"+cccdAppManagementIP+":8080/cccd/app/"+jsonBody['appName']+"/buckets/"+jsonBody['bucketName']+"/files?objectid="+jsonBody['objectId']
		print url
		request = urllib2.Request(url)
		base64string = base64.encodestring('%s:%s' % (adminUsername, adminPassword)).replace('\n', '')
		request.add_header("Authorization", "Basic %s" % base64string)   
		f = urllib2.urlopen(request)
		#f = urllib2.urlopen(url)
		data = f.read()
		fileLink="/tmp/"+jsonBody['objectId']
		with open(fileLink, "wb") as code:
    			code.write(data)
		
		cfileStr = open(conf_file, 'r').read()
		newCfileStr=cfileStr.replace("$FileToConvert",fileLink).replace("$Encoding_Profile",jsonBody['profiles'])
		#newCfileStr=cfileStr.replace("$FileToConvert",fileLink)
		newCfileStr=newCfileStr.replace("$UploadFolder","/"+jsonBody['appName']+"/"+jsonBody['bucketName']+"/"+jsonBody['objectId'])
		newCfileStr=newCfileStr.replace("$appName",jsonBody['appName'])
		newCfileStr=newCfileStr.replace("$bucketName",jsonBody['bucketName'])
		newCfileStr=newCfileStr.replace("$objectId",jsonBody['objectId'])

		temp = tempfile.NamedTemporaryFile()
		try:
			temp.write(newCfileStr)
			temp.seek(0)
			print 'temp:', temp
			print 'temp.name:', temp.name
		finally:
			# Automatically cleans up the file
			#os.chdir(working_dir)
			#os.system("%s %s"%("http_streamer.rb", temp.name))
			#subprocess.check_output([working_dir+"http_streamer.rb", temp.name)
			print 'launching slave process...'
			#time.sleep(600)
			#subprocess.check_output("/usr/bin/ruby /home/ubuntu/cccd/cccdContentAdaption/hls/http_streamer.rb "+ temp.name, shell=True)
			#runSegmenter(temp.name)
			cmdping = "/usr/bin/ruby /home/ubuntu/cccd/cccdContentAdaption/hls/http_streamer.rb "+ temp.name
			p = subprocess.Popen(cmdping, shell=True, stderr=subprocess.PIPE)
			while True:
    				out = p.stderr.read(1)
    				if out == '' and p.poll() != None:
        				break
    				if out != '':
        				sys.stdout.write(out)
        				sys.stdout.flush()
			temp.close()
			try:
                        	os.remove(fileLink)
                	except Exception, e:
                        	logger.debug(e)
		#print newCfileStr
		#sys.exit()
		
	logger.debug(" [x] Done, send ack to this message: %r" % (body,))
	ch.basic_ack(delivery_tag = method.delivery_tag)

####################################################################

logger=init_logger(logger_setting)

connection = pika.BlockingConnection(pika.ConnectionParameters(
        host=message_broker_ip))
channel = connection.channel()

channel.queue_declare(queue='cccdCA', durable=True)

logger.debug(' [*] Waiting for messages.')
channel.basic_qos(prefetch_count=1)
channel.basic_consume(callback,queue='cccdCA')

channel.start_consuming()
