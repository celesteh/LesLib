{\rtf1\ansi\ansicpg1252\cocoartf949\cocoasubrtf430
{\fonttbl\f0\fnil\fcharset0 Monaco;}
{\colortbl;\red255\green255\blue255;\red0\green0\blue191;}
\pard\tx560\tx1120\tx1680\tx2240\tx2800\tx3360\tx3920\tx4480\tx5040\tx5600\tx6160\tx6720\ql\qnatural\pardirnatural

\f0\fs18 \cf0 PVPanAZ : \cf2 MultiOutUGen\cf0  \{\
\
	*ar \{ arg chain, numchannels, center, width, trigger, lobin, hibin;\
	\
		^this.multiNew('audio', chain, numchannels, center, width, trigger, lobin, hibin);\
	\}\
	\
\}}