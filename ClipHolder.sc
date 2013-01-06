ClipHolder
{

  // a class to hold a data about a buffer, including the bufnum on the server
  // the number of frames and the samplerate

	//var <bufnum;
	var <name;
	var <sampleRate = 44100;
	var <numFrames = -1;
	var <length;
	var <numChannels;
 	var buf;
 	var bufnum;
 	var diskOut;
 	var isBuf = false;
 	var <server;
 	var <synthName;
 	var synth;
 
 	*new { arg srv, name;
 
 		^super.new.init(name, srv);
 	}
 
 	init { arg fileName, srv;
 	 	
 		// get the header information by opneing a soundfile
 		var soundFile, synthDef;

 		name = fileName;
 		soundFile = SoundFile.new;
 		soundFile.openRead(name);
 		sampleRate = soundFile.sampleRate;
 		numFrames = soundFile.numFrames;
 		numChannels = soundFile.numChannels;
 		soundFile.close;
 		
 		// then read the file as a buffer ot the server
 		
 		server = srv;
 		
 		length = numFrames / sampleRate;
 		
 		if (length < 3, {
 			buf = Buffer.read(srv, name);
 			isBuf = true;
 			bufnum = buf.bufnum;
 			
 			synthName = "playBuf";
 			synthDef = SynthDef(synthName, {arg out = 0, bufnum = 0, gate =1, xPan = 0, yPan = 0, 
								amp = 1, numChannels =2, dur;
	
				// plays a buffer through forwards and at the normal rate
	
				var env, buf, outputAudio;
				dur = BufDur.kr(bufnum) + 1;
		
				// envelope required or the ugens stay around forver
				env = EnvGen.kr(Env.linen(0.0001, (dur - 0.0002), 2, amp), gate, doneAction:2);
				buf = PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum));
		
				// this Pan2 stuff is a hack because Pan4 is glitchy
		
				outputAudio = Pan2.ar(buf, xPan);
				outputAudio = Pan2.ar(outputAudio.at(0), yPan) ++ 
							Pan2.ar(outputAudio.at(1), yPan);
		
				Out.ar( out, outputAudio * env);
			});
			synthDef.send(server);
			
		}, {
		
			buf = Buffer.cueSoundFile(server, name, 0, numChannels);
			
			isBuf = false;
			bufnum = buf.bufnum;
			
			if (numChannels == 2, {
			
				synthName = "playDisk2Chan";
 				synthDef = SynthDef(synthName, {arg out = 0, bufnum = 0, gate =1, xPan = 0, 
 									yPan = 0, amp = 1, numChannels = 2, dur;
					var env, disk;
				
					env = EnvGen.kr(Env.linen(0.0001, (dur - 0.0002), 2, amp), gate, 
									doneAction:2);
					disk = DiskIn.ar( 2, bufnum );

					Out.ar(out, disk * env);
				});
			}, {
				// go ahead, assume it has to be one channel if it's not 2
				synthName = "playDisk1Chan";
 				synthDef = SynthDef(synthName, {arg out = 0, bufnum = 0, gate =1, xPan = 0, 
 									yPan = 0, amp = 1, numChannels = 1, dur;
					var env, disk;
				
					env = EnvGen.kr(Env.linen(0.0001, (dur - 0.0002), 2, amp), gate, 
								doneAction:2);
					disk = DiskIn.ar( 1, bufnum );

					Out.ar(out, disk * env);
				});
			});
			
			synthDef.send(server);
		});
			


 	}
 	
 	play { arg out = 0, xPan = 0, yPan = 0, amp = 1;
 	
 		synth = Synth(synthName, ["out", out, "bufnum", bufnum, "xPan", xPan, "yPan", yPan, 
 						"amp", amp, "numChannels", numChannels, "dur", length]);
 						
 	}
 
 
 	stop {
 		synth.free;
 	}
 	
 	pause {
 	
 		synth.run(false);
 	}
 	
 	restart {
 	
 		synth.run(true);
 	}
  	
  	free {

		// de-allocate memory on the server
  	
  		buf.free;
  	}

}
 