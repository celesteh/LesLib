LoopManager
{

	var loopArray;
	var server;
	var <sampleRecGroup;
	var playGroup;
	var <fxGroup;
	var window;
	var buttons;
	var guiOffset= 20;

 	*new { arg server, filenames=0, win=0;
 
 		^super.new.init(server, filenames, win);
 	}


	init { arg srv, filenames = 0, win=0;
		// filenames should be an array of filenames, but nothing is passed in
		// and we start a do loop, the 0 value will just cause the loop to be
		// skipped
		
		var fname, loop, button;
		
		if (win == 0, {
			window = SCWindow.new;
		} , {
			window = win;
		});
		window.front;
	
		buttons = Array.newClear(1);
		loopArray = Array.newClear(1);
		server = srv;
		
		sampleRecGroup = Group(addAction: \addToHead);
		playGroup = Group(addAction: \addToTail);
		fxGroup = Group(addAction: \addToTail);

		
		filenames.do { arg fname, index;
			loop = LoopHolder(server, playGroup, fname);
			loopArray = loopArray.add(loop);
			button = SCButton(window, Rect(20, guiOffset, 340, 30));
			buttons = buttons.add(button);
			//button = buttons.at(index);
			button.states = [
					[fname ++ " >", Color.red,Color.white],
					[fname ++ " []",Color.white,Color.red]
						];
						
			button.action = {
				if (buttons.at(index).value == 1, {
					loopArray.at(index).play;
				}, {
					loopArray.at(index).stop;
				});
			};
			// need a more complicated UI, w/ file name, number of repeats
			// possible pull down menu of playback methods

			//buttons = buttons.add(button);
			guiOffset = guiOffset + 40;
		};
	}
	
	prepRecord { arg length, numChannels = 1;
		var loop;
		loop = LoopHolder(server, sampleRecGroup);
		loop.getReadyToRecord(length, numChannels);
		//loopArray = loopArray.add(loop);
		
		^loop;
	}
	
	doRecord { arg loop, name="";
		
		loop.doRecording(name, playGroup);
		loopArray = loopArray.add(loop);
	}
}

LoopHolder
{
	var <>name = "";
	var <sampleRate = 44100;
	var <numFrames = -1;
	var <length;
	var <numChannels;
 	var buf;
 	var bufnum;
	var <server;
 	var diskOut;
 	var isBuf = false;
 	var <server;
 	var <synthName;
 	var synth;
 	var initted = false;
 	var group;
	
	 	*new { arg srv, group, name = nil;
 
 		^super.new.init(srv, group, name);
 	}
 
 	init { arg srv, grp, fileName = nil;

 		var soundFile, synthDef;

		server = srv;
		group = grp;
		if (fileName != nil, {
		 	// get the header information by opneing a soundfile

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
 		
 			if (length < 30, {
 				buf = Buffer.read(srv, name);
 				isBuf = true;
 				bufnum = buf.bufnum;
				this.setBufPlayer;
			}, {
				buf = Buffer.cueSoundFile(server, name, 0, numChannels);
			
				isBuf = false;
				bufnum = buf.bufnum;
			
				if (numChannels == 2, {
					this.set2ChannelDiskPlayer;
				}, {
					this.set1ChannelDiskPlayer;
				});
			});
			initted = true;
		} , {
			// else, must be planning in recording
			this.setBufRecord;
		});
	}
	
	
	setBufPlayer {
		// play a buffer, looping a particular number of times
		synthName = "loopBuf";
		 synth = SynthDef(synthName, {arg out = 0, bufnum = 0, gate =1, xPan = 0, yPan = 0, 
								amp = 1, numChannels =2, dur;
	
			// plays a buffer through forwards and at the normal rate
	
			var env, buf, outputAudio;
			//dur = BufDur.kr(bufnum) + 1;
		
			// envelope required or the ugens stay around forver
			env = EnvGen.kr(Env.linen(0.001, (dur - 0.002), 0.001, amp), gate, doneAction:2);
			buf = PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum), loop:1);
		
			// this Pan2 stuff is a hack because Pan4 is glitchy
		
			outputAudio = Pan2.ar(buf, xPan);
			outputAudio = Pan2.ar(outputAudio.at(0), yPan) ++ 
						Pan2.ar(outputAudio.at(1), yPan);
		
			Out.ar( out, outputAudio * env);
		});
		
		synth.send(server);

	}
	
	set2ChannelDiskPlayer {
		// same as above, but from DiskIn, w/ 2 channel audio file		synthName = "playDisk2Chan";
 		synth = SynthDef(synthName, {arg out = 0, bufnum = 0, gate =1, xPan = 0, 
 									yPan = 0, amp = 1, numChannels = 2, dur;
			var env, disk, outputAudio;
				
			env = EnvGen.kr(Env.linen(0.001, (dur - 0.002), 0.001, amp), gate, 
									doneAction:2);
			disk = DiskIn.ar( 2, bufnum );
			
					
			outputAudio = Pan2.ar(disk, xPan);
			outputAudio = Pan2.ar(outputAudio.at(0), yPan) ++ 
						Pan2.ar(outputAudio.at(1), yPan);
		
			Out.ar( out, outputAudio * env);


			//Out.ar(out, disk * env);
		});
		synth.send(server);

	}
	
	
	set1ChannelDiskPlayer {
		// same as above, but with 1 channel audio file
		synthName = "playDisk1Chan";
 		synth = SynthDef(synthName, {arg out = 0, bufnum = 0, gate =1, xPan = 0, 
 									yPan = 0, amp = 1, numChannels = 1, dur;
			var env, disk, outputAudio;
				
			env = EnvGen.kr(Env.linen(0.001, (dur - 0.002), 0.001, amp), gate, 
								doneAction:2);
			disk = DiskIn.ar( 1, bufnum );
					
							
			outputAudio = Pan2.ar(disk, xPan);
			outputAudio = Pan2.ar(outputAudio.at(0), yPan) ++ 
						Pan2.ar(outputAudio.at(1), yPan);
		
			Out.ar( out, outputAudio * env);


			//Out.ar(out, disk * env);
		});
		synth.send(server);

	}
	
	setBufRecord {
		// synthdef to record audio to a buffer
		synthName = "bufRecord";
		synth = SynthDef(synthName, { arg bus = 0, dur, bufnum, numChannels = 1;
			
			var env, in;
			env = EnvGen.kr(Env.linen(0.001, (dur - 0.002), 0.001, 1), 1, 
								doneAction:2);
			in = In.ar(bus, numChannels);
			RecordBuf.ar(in * env, bufnum);
		});
		synth.send(server);
		
	}
	
	getReadyToRecord { arg length, numChannels = 2;
		if ( initted == false, {
			numFrames = length * sampleRate;
			this.numChannels = numChannels;
			buf = Buffer.alloc(server, numFrames, numChannels); 
		});
	}
	
	doRecording { arg name = "", newGroup;
		// record for set length
		bufnum = buf.bufnum;
		this.name = name;
		Routine ({
			Synth(synthName, [\dur, length, \bufnum, bufnum, \numChannels, numChannels],
				group, \addToTail);
			length.yield;
			this.setBufPlayer;
			group = newGroup;
			initted = true;
		}).play;
	}
	
	play {
		"play".postln;
		this.playLoop(1);
	}
	
	stop {
	}
	
	playLoop { arg times;
	
		var dur;
		
		if (initted, {
			if (isBuf == false, {
				// need a routine to do the looping
				Routine({
					times.do{
						Synth(synthName, [\out, 0, \bufnum, bufnum, \xPan, 0, 
 									\yPan, 0, \amp, 1, \dur, length], group, \addToTail);
 						length.yield;
 					};
 				}).play;
			}, {
				// else, just do the looping with the loopBuf player
				dur = length * times;
				Synth(synthName, [\out, 0, \bufnum, bufnum, \xPan, 0, \yPan, 0, 
								\amp, 1, \dur, dur], group, \addToTail);
								
			});
		});
	}
}