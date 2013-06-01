BufferTool
{

  // a class to hold a data about a buffer, including the bufnum on the server
  // the number of frames and the samplerate

	classvar <monoSynthDefName, <stereoSynthDefName, <monoRecSynthDefName,
			<stereoRecSynthDefName;

	var <bufnum;
	var <sampleRate = 44100;
	var <>numFrames = -1;
	var <>startFrame;
	var <>endFrame;
	var <>dur;
	var <alloc_dur;
	var alloc_numFrames;
	var <grains;
	var <avgPitch;
	var <>pitch;
	//var <>grain_size, <>num_grains, <>grain_dur;
	var <numChannels;
	var <>synthDefName;
	var <>recSynthDefName;
 	var <buf;
 	var <>amp;
 	var server;
 	var <buf_class;
 	var <>rms;
 	var <path;


 	// some synthdefs for playing back buffers in 4 channels
 	// these are used as defaults, but are probably not the most wonderful synthdefs ever

 	*initClass  {

 		monoSynthDefName = \monoGrainBufferSynthDef;
 		stereoSynthDefName = \stereoGrainBufferSynthDef;
 		monoRecSynthDefName = \monoGrainBufferRecSynthDef;
 		stereoRecSynthDefName = \stereoGrainBufferRecSynthDef;

		StartUp.add({

		SynthDef.writeOnce( monoSynthDefName, { arg out = 0, bufnum = 0, xPan = 0,
 									yPan = 0, amp =1,startFrame = 0, grainDur = 1, rate = 1;

 			var env, speed, player, panner;

 			env = EnvGen.kr(Env.linen(0.001, (grainDur - 0.002), 0.001, amp), doneAction:2);
 			//env = EnvGen.kr(Env.linen(0.0001, (grainDur - 0.0002), 0.0001, 1), doneAction:2);
			speed = rate *  BufRateScale.kr(bufnum);
			player = PlayBuf.ar(1, bufnum, speed, startPos: startFrame);
			panner = Pan4.ar(player * env, xPan, yPan);
			OffsetOut.ar(out, panner);
		});


 		SynthDef.writeOnce( stereoSynthDefName, { arg out = 0, bufnum = 0,
 									xPan = 0, yPan = 0,
									 amp =1, startFrame = 0, grainDur = 1, rate = 1;

 			var env, speed, player, panner;

 			env = EnvGen.kr(Env.linen(0.001, (grainDur - 0.002), 0.001, amp), doneAction:2);
			speed = rate *  BufRateScale.kr(bufnum);
			player = PlayBuf.ar(2, bufnum, speed, startPos: startFrame);
			panner = Pan4.ar(player * env, xPan, yPan);
			OffsetOut.ar(out, panner);
		});

		SynthDef.writeOnce(monoRecSynthDefName, { arg in = 0 , bufnum = 0, gate = 1;

			var inner, env;
			env = EnvGen.kr(Env.adsr(1, 1, 1, 1), gate, doneAction:2); // this gets rid of
				// the synth once we stop recording
			inner = In.ar(in, 1);
			RecordBuf.ar(inner, bufnum, run:gate);
		});

		SynthDef.writeOnce(stereoRecSynthDefName, { arg in = 0 , bufnum = 0, gate = 1;

			var inner, env;
			env = EnvGen.kr(Env.adsr(1, 1, 1, 1), gate, doneAction:2);
			// this gets rid of
			// the synth once we stop recording
			inner = In.ar(in, 2);
			RecordBuf.ar(inner, bufnum, run:gate);
		});
		});
	}

 	*alloc { arg srv, dur,  channels = 2, sampleRate = 44100, action,
 			class = Buffer;

 		^super.new.rec_init(srv, 1, dur, channels, sampleRate, action, class);
 	}


 	*grain { arg srv, buffer, startFrame, endFrame, sampleRate, synthDefName, bufnum, bufamp,
 			class = Buffer;

 		^super.new.grain_init(srv, buffer, startFrame, endFrame, sampleRate,
 							synthDefName, bufnum, bufamp, class);
 	}


 	*new { arg srv, in, dur,  channels = 2, sampleRate = 44100, action, class = Buffer;

 		^super.new.rec_init(srv, in, dur, channels, sampleRate, action, class);
 	}

 	*open { arg srv, path, action, class = Buffer;

 		^super.new.open_init(path, srv, action, class);
 	}


  	*read { arg srv, name, action, class = Buffer;

 		^super.new.open_init(name, srv, action, class);
 	}


 	*words { arg srv, name, threshold = 0.3, length = 5000, take_last = false, min = 4410, action,
 				class = Buffer;

 		^super.new.word_init(srv, name, threshold, length, take_last, min, false, action, class);
 	}


	// open a file and break it into words

	word_init { arg srv, name, threshold = 0, length = 0, take_last = false, min = 4410,
				border_on_zero_crossing = false, action, class = Buffer;


 		var soundFile, arr, prev, offset, last_zero, last_over, last_under, zero_before,
 			under, starts, ends, last_start, last_end, func;

 		soundFile = SoundFile.new;
 		soundFile.openRead(name);
 		sampleRate = soundFile.sampleRate;
 		numFrames = soundFile.numFrames;
 		startFrame = 0;

 		alloc_dur = numFrames / sampleRate;
 		dur = alloc_dur;
 		alloc_numFrames = numFrames;

		arr = Signal.newClear(numFrames);

		offset = 0;
		last_over = startFrame;
 		last_under =startFrame;
 		last_zero = startFrame;
 		zero_before = startFrame;
 		prev = 0;
		under = false;
		 	//words = [];
		starts = [];
		ends = [];

		soundFile.readData(arr);

		{arr.size > 0}.while({


			arr.do({|current, index|

				index = index + offset;

 	    	  		// find zero crossings
 	    	  		// best case = we land right on zero!
 	    	  		if (current == 0, { last_zero = index; prev = 0;},

 	    	  			// otehrwise, if we cross zero, just fudge it
 	    	  			{ if (((current.sign != prev.sign) && (prev != 0)), {

 	    	  				// but get the one closer to zero, at least
 	    	  				if ((prev.abs < current.abs) , {

 	    	  					// we don't need to worry about negative indexes because
 	    	  					// prev is initialized to 0 and we don't excecute this code
 	    	  					// when that's the case
 	    	  					last_zero = index - 1;
 	    	  				} , {
 	    	  					last_zero = index;
 	    	  				})
 	    	  		})});

		 	    	prev = current;
 	    	 		current  = current.abs;

 	    	  		if (under == true, {

 	    	  			if (current > threshold, {

 	    	  				under = false;
 	    	  				if ((index - last_over) > length, { //"would have worked".postln; });
 	    	  				//if ((last_zero - last_over) > length, {
 	    	  					//("start " ++ index ++ " end " ++ last_over).postln;
 	    	  					//starts = starts.add(index);
 	    	  					//ends = ends.add(last_over);

 	    	  					// ok, let's recognize that zero corssings are better
 	    	  					// but if there are no recent ones, then screw it
 	    	  					(border_on_zero_crossing && (last_over - zero_before < 150)). if ({
 	    	  						ends = ends.add(zero_before);
 	    	  					} , {
 	    	  						ends = ends.add(last_over);
 	    	  					});
 	    	  					(border_on_zero_crossing && (index - last_zero < 150)). if ({
 		    	  					starts = starts.add(last_zero);
 		    	  				} , {
 		    	  					starts = starts.add(index);
 		    	  				});
 	    	  				});
 	    	  				//skip none!
 	    	  				last_over = index;
 	    	  				zero_before = last_zero;
 	    	  			});
 	    	  		}, { // under == false;

 	    	  			if (current < threshold, {

 	    	  				under = true;
 	    	  			});
 	    	  		});


 	    		});

			offset = offset + arr.size;
			soundFile.readData(arr);

		});

 		soundFile.close;


 		func = { |buf|

	 		if ((ends.size > 0), {

		 		//get rid of the first end, as it's the start
		 		last_end = ends.removeAt(0);
	 			grains = [];

 	  			//"SIZES:  YO SIZES: ".post;
				//starts.size.postln;
				//ends.size.postln;
				//starts.postln;
				//ends.postln;
				last_start = startFrame;

				starts.do ({ arg start;

					grains = grains.add(BufferTool.grain(server, buf.buf, last_start,
						start, sampleRate, synthDefName, bufnum, amp));
					last_start = start;
				});

				if (take_last, {
					grains = grains.add(BufferTool.grain(server, buf.buf, last_start, numFrames,
						sampleRate, synthDefName, bufnum, amp));
				});

				("% grains found\n").postf(grains.size);


 			}, { "got nothing. is the buffer normalized? try changing some values".postln;});

 			action.notNil.if({
 				action.value(buf);
 			});
 		};

		this.open_init(name, srv, func, class);
	}


	/*
	word_init { arg srv, name, threshold = 0, length = 0, take_last = false, min = 4410,
				border_on_zero_crossing = false, action, class = Buffer;


 		var soundFile, func;

		//arr = FloatArray(256);

		//arr = soundFile.readData(arr);


		func = {|buf|

 			var prev, offset, last_zero, last_over, last_under, zero_before,
 				under, starts, ends, last_start, last_end, startf;


			last_over = 0;
 			last_under =0;
 			last_zero = 0;
 			zero_before = 0;
 			prev = 0;
			under = false;
		 	//words = [];
			starts = [];
			ends = [];


			buf.buf.loadToFloatArray(0, -1, {|arr|

				arr.do({|current, index|

					//index = index + offset;

 	    	  			// find zero crossings
 	    	  			// best case = we land right on zero!
 	    	  			if (current == 0, { last_zero = index; prev = 0;},

 	    	  				// otehrwise, if we cross zero, just fudge it
 	    	  				{ if (((current.sign != prev.sign) && (prev != 0)), {

 	    	  					// but get the one closer to zero, at least
 	    	  					if ((prev.abs < current.abs) , {

 	    	  						// we don't need to worry about negative indexes because
 	    	  						// prev is initialized to 0 and we don't excecute this code
 	    	  						// when that's the case
 	    	  						last_zero = index - 1;
 	    	  					} , {
 	    	  						last_zero = index;
 	    	  					})
 	    	  			})});

		 	    		prev = current;
 	    	 			current  = current.abs;

 	    	  			if (under == true, {

 	    	  				if (current > threshold, {

 	    	  					under = false;
 	    	  					if ((index - last_over) > length, {
 	    	  						//"would have worked".postln; });
 	    	  					//if ((last_zero - last_over) > length, {
 	    	  						//("start " ++ index ++ " end " ++ last_over).postln;
 	    	  						//starts = starts.add(index);
 	    	  						//ends = ends.add(last_over);

 	    	  						// ok, let's recognize that zero corssings are better
 	    	  						// but if there are no recent ones, then screw it
 	    	  						( (last_over - zero_before < 150)). if ({
 	    	  							ends = ends.add(zero_before);
 	    	  						} , {
 	    	  							ends = ends.add(last_over);
 	    	  						});
 	    	  						((index - last_zero < 150)). if ({
 		    	  						starts = starts.add(last_zero);
 		    	  					} , {
 		    	  						starts = starts.add(index);
 		    	  					});
 	    	  						//last_over = index;
 	    	  						//zero_before = last_zero;
 	    	  					});
 	    	  					//skip none!
 	    	  					last_over = index;
 	    	  					zero_before = last_zero;
 	    	  				});
 	    	  			}, { // under == false;

 	    	  				if (current < threshold, {

 	    	  					under = true;
 	    	  				});
 	    	  			});


 	    			});

				//arr = soundFile.readData(arr);


	 			//soundFile.close;


 				if ((ends.size > 0), {

	 				//get rid of the first end, as it's the start
	 				last_end = ends.removeAt(0);
	 				grains = [];

 	  				//"SIZES:  YO SIZES: ".post;
					//starts.size.postln;
					//ends.size.postln;
					//starts.postln;
					//ends.postln;
					last_start = startFrame;

					starts.do ({ arg start;

						grains = grains.add(BufferTool.grain(server, buf, last_start,
							start, sampleRate, synthDefName, bufnum, amp));
						last_start = start;
					});

					if (take_last, {
						grains = grains.add(BufferTool.grain(server, buf, last_start, numFrames,
							sampleRate, synthDefName, bufnum, amp));
					});

					("% grains found\n").postf(grains.size);


 				}, { "got nothing. is the buffer normalized? try changing some values".postln;});
			});

			action.value(this);
		};

		this.open_init(name, srv, func, class);


	}
 	*/

 	// new BufferTool from an existing Buffer

 	grain_init { arg srv, buffer, start_frame, end_frame, sample_rate, synthDef_name, buf_num,
 				buf_amp, class;

 		buf = buffer;

 		(srv.notNil).if ({
	 		server = srv;
	 	} , {
	 		server = buf.server;
	 	});

 		(start_frame.notNil).if({
	 		startFrame = start_frame;
	 	} , {

			startFrame = 0;
		});

		(end_frame.notNil).if ({
	 		endFrame = end_frame;
	 	} , {
	 		endFrame = buf.numFrames;
	 	});

	 	(sample_rate.notNil). if ({
	 		sampleRate = sample_rate;
	 	} , {
	 		sampleRate = buf.sampleRate;
	 	});

	 	numChannels = buf.numChannels;

	 	(synthDef_name.notNil). if ({
	 		synthDefName = synthDef_name;
	 	}, {

	 	  	(numChannels == 1). if ({
	 			synthDefName = monoSynthDefName;
				recSynthDefName = monoRecSynthDefName;
			});
			(numChannels == 2). if ({
	 			synthDefName = stereoSynthDefName;
				recSynthDefName = stereoRecSynthDefName;
			});
		});

	 	(buf_num.notNil). if ({
	 		bufnum = buf_num;
	 	} , {
	 		bufnum = buf.bufnum;
	 	});

	 	(buf_amp.notNil). if({
	 		amp = buf_amp;
	 	} , {
	 		amp = 1;
	 	});

 		numFrames = endFrame - startFrame;
 		dur = numFrames / sampleRate;
 		buf_class = class;
 	}


	// Alloc a buffer

  	rec_init { arg srv, in, bufdur,  channels = 2, sample_rate = 44100, action, class;

 		server = srv;
 		dur = bufdur;
 		alloc_dur = dur;
 		numChannels = channels;
 		sampleRate = sample_rate;
 		numFrames = dur * sampleRate;
 		alloc_numFrames = numFrames;
 		buf_class = class;
 		//("server " ++ server ++ " dur " ++ dur ++ " sampleRate " ++ sampleRate
 		//	++ " numFrames " ++ numFrames ++ " numChannels " ++ numChannels).postln;
 		buf = class.alloc(srv, numFrames, channels, completionMessage: {|b|
	 		bufnum = b.bufnum;
 			startFrame = 0;
 			endFrame = numFrames;

 			(channels == 1). if ({
	 			synthDefName = monoSynthDefName;
				recSynthDefName = monoRecSynthDefName;
			});
			(channels == 2). if ({
	 			synthDefName = stereoSynthDefName;
				recSynthDefName = stereoRecSynthDefName;
			});

 			//this.do_synths(this.numChannels);

 			buf = b;

 			if (action.notNil, {
				action.value(this);
			});

			Server.default.sendMsg("/s_new", "default");
 		})

 	}


	// open a file

 	open_init { arg name, srv, action, class;

 		// then read the file as a buffer ot the server

 		server = srv;
 		buf_class = class;
 		path = name;

 		"open".postln;
 		class.postln;

 		name.postln;

 		buf = class.read(srv, name, action:{

 			//var starts, ends;

 			//starts = [];
 			//ends = [];
 			"read".postln;
	 		sampleRate = buf.sampleRate;
 			numChannels = buf.numChannels;
 			numFrames = buf.numFrames;
 			startFrame = 0;
 			endFrame = numFrames;
	 		dur = numFrames / sampleRate;
	 		alloc_dur = dur;
	 		alloc_numFrames = numFrames;

	 		["dur is" + dur].postln;

 			bufnum = buf.bufnum;

  			(numChannels == 1). if ({
	 			synthDefName = monoSynthDefName;
				recSynthDefName = monoRecSynthDefName;
			});
			(numChannels == 2). if ({
	 			synthDefName = stereoSynthDefName;
				recSynthDefName = stereoRecSynthDefName;
			});

			if (action.notNil, {
				action.value(this);
			});

		});



 	}

 	// read a file into a pre-existing buffer
 	read{ |path, fileStartFrame = 0, nFrames = -1, bufStartFrame = 0, action|

 		var soundFile, sfFrames, wrap_around, frames;


 		"reading".postln;

  		soundFile = SoundFile.new;
 		soundFile.openRead(path);
  		sfFrames = soundFile.numFrames;
 		soundFile.close;

		// if bufStartFrame is > 0 AND
		// if the number of frames to be read is greater than the space between the start
		// frame and alloc_numFrames THEN
		// read the remainder from the start of the buffer, stopping before bufStartFrame

		(nFrames == -1).if ({ frames = sfFrames}, { frames = nFrames});


		if (((bufStartFrame > 0) && (frames > (alloc_numFrames - bufStartFrame))), {

			wrap_around = {
				var overflow, last_read;

				last_read = alloc_numFrames - bufStartFrame;
				overflow = frames - last_read;

				(overflow > bufStartFrame).if ({ overflow = bufStartFrame});

				buf.read(path, last_read, overflow, 0, action:{
					endFrame = alloc_numFrames;
					dur = alloc_numFrames/sampleRate;
				});
			};
		} , {
			wrap_around = {"didn't wrap".postln;};
		});


		buf.read(path, fileStartFrame, nFrames, bufStartFrame, action:{

 			//var starts, ends;

 			//starts = [];
 			//ends = [];
 			"read".postln;
	 		sampleRate = buf.sampleRate;
 			numChannels = buf.numChannels;
 			//numFrames = buf.numFrames;
 			startFrame = bufStartFrame;
 			endFrame = frames + bufStartFrame;
 			numFrames = endFrame - startFrame;
	 		dur = numFrames / sampleRate;

	 		["dur is" + dur].postln;

 			bufnum = buf.bufnum;

  			(numChannels == 1). if ({
	 			synthDefName = monoSynthDefName;
				recSynthDefName = monoRecSynthDefName;
			});
			(numChannels == 2). if ({
	 			synthDefName = stereoSynthDefName;
				recSynthDefName = stereoRecSynthDefName;
			});

			wrap_around.value;

			if (action.notNil, {
				action.value(this);
			});
 		});
 	}

  	// read a file into a pre-existing buffer - useful for mapping stereo to mono
 	readChannel { |path, fileStartFrame = 0, nFrames = -1, bufStartFrame = 0, channels, action|
 		var soundFile, frames, wrap_around;


 		"reading from %\n".postf(bufStartFrame);

 		channels.isNil.if({ channels = numChannels.collect({|i| i }) });

  		soundFile = SoundFile.new;
 		soundFile.openRead(path);
  		frames = soundFile.numFrames;
		soundFile.close;

		// if bufStartFrame is > 0 AND
		// if the number of frames to be read is greater than the space between the start
		// frame and alloc_numFrames THEN
		// read the remainder from the start of the buffer, stopping before bufStartFrame

		((nFrames != -1) && nFrames.notNil).if ({ frames = nFrames});

		bufStartFrame = bufStartFrame % alloc_numFrames;

		"frames = % alled = %\n".postf(frames, alloc_numFrames);


		if (((bufStartFrame > 0) && (frames > (alloc_numFrames - bufStartFrame))), {

			wrap_around = {

				var overflow, last_read;

				"wrapping".postln;

				last_read = alloc_numFrames - bufStartFrame;
				overflow = frames - last_read;

				(overflow > bufStartFrame).if ({ overflow = bufStartFrame});

				buf.readChannel(path, last_read, overflow, 0, channels: channels, action:{
					endFrame = alloc_numFrames;
					dur = alloc_numFrames/sampleRate;
				});
			};
		} , {
			wrap_around = {"didn't wrap".postln;};
		});


		buf.readChannel(path, fileStartFrame, nFrames, bufStartFrame, channels:channels, action:{

 			//var starts, ends;

 			//starts = [];
 			//ends = [];
 			"read".postln;
	 		sampleRate = buf.sampleRate;
 			numChannels = buf.numChannels;
 			//numFrames = buf.numFrames;
 			startFrame = bufStartFrame;
 			endFrame = frames + bufStartFrame;
 			numFrames = endFrame - startFrame;
	 		dur = numFrames / sampleRate;

	 		["dur is" + dur].postln;

 			bufnum = buf.bufnum;

  			(numChannels == 1). if ({
	 			synthDefName = monoSynthDefName;
				recSynthDefName = monoRecSynthDefName;
			});
			(numChannels == 2). if ({
	 			synthDefName = stereoSynthDefName;
				recSynthDefName = stereoRecSynthDefName;
			});

			wrap_around.value;

			if (action.notNil, {
				action.value(this);
			});
 		});
 	}


 	append { |path, fileStartFrame, nFrames, channels, action|

	 	this.readChannel(path, fileStartFrame, nFrames, endFrame, channels, action);
 	}


 	findRMS{ arg action;

 	  var amt_to_request, func;

 	  func = { arg localCopy;

 	    var sum, length;

 	    sum = 0;
 	    length = 0;

 	    this.pr_doRMS(localCopy, length, sum, action);
 	  };
  		if (numFrames < 1600, {
  			amt_to_request = numFrames.floor.asInt;
  		} , {
  			amt_to_request = 1600;
    		});

 		buf.getn(startFrame.ceil.asInt, amt_to_request, func);
	}


	pr_doRMS{ arg localCopy, length, sum, action;

 		var amt_to_request, func;

 		if (localCopy != nil, {
 			length  = length + localCopy.size;
 			localCopy.do({ arg sample;

				sum = sum + (sample * sample);
			});
		});
 		if (length < (numFrames.floor.asInt -1), {
 				amt_to_request = numFrames.floor.asInt - length;
 				if (amt_to_request > 1600, {
 					amt_to_request = 1600;
 				});

 				func = {arg localC;

 					this.pr_doRMS(localC, length, sum, action);
 				};

				buf.getn((startFrame.ceil.asInt + length), amt_to_request, func);
			} , {

				sum = sum / numFrames.floor.asInt;
				rms = sum.sqrt;

				action.notNil.if({

					action.value(rms, this)
				});
			});
	}


 	// calculate the pitch of this BufferTool

 	findPitch{ arg threshold = 0.025, action;

 		var amt_to_request, func;

 		threshold = threshold.abs;

 		func = { arg localCopy;

 			var crossings, positive, length;
 			crossings = 0;
 			positive = true;
 			length = 0;

 			this.pr_doPitch(localCopy, length, crossings, positive, threshold, action);
  		};
  		if (numFrames < 1600, {
  			amt_to_request = numFrames.floor.asInt;
  		} , {
  			amt_to_request = 1600;
    		});

 		buf.getn(startFrame.ceil.asInt, amt_to_request, func);

 	}

 	pr_doPitch { arg localCopy, length, crossings, positive, threshold, action;

 		var amt_to_request, func;

 		if (localCopy != nil, {
 			length  = length + localCopy.size;
 			localCopy.do({ arg sample;
 				if (positive, {
 					if (sample < (-1 * threshold), {
 						crossings = crossings + 1;
 						positive = false;
 					});
 				} , {
 					if (sample > threshold, {
 						crossings = crossings + 1;
 						positive = true;
 					});
 				});
 			});

 			if (length < (numFrames.floor.asInt -1), {
 				amt_to_request = numFrames.floor.asInt - length;
 				if (amt_to_request > 1600, {
 					amt_to_request = 1600;
 				});

 				func = {arg localC;

 					this.pr_doPitch(localC, length, crossings, positive, threshold, action);
 				};

				buf.getn((startFrame.ceil.asInt + length), amt_to_request, func);
			} , {

				avgPitch = crossings / dur;
 				pitch = avgPitch / 2;

				(action.notNil).if({
					action.value(this);
				});
			});
		});

	}


	// find gaps in speech or other sounds and use to create grains

 	prepareWords {arg threshold = 0.3, length=800, take_last = false, min = 4410,
 				border_on_zero_crossing = false, action;
		var func, amt_to_request;

		func = { arg localcopy;
			var last_over, last_under, current, under, starts, ends, offset,
				last_zero, zero_before, prev;
			current = startFrame;
		 	last_over = startFrame;
 			last_under =startFrame;
 			last_zero = startFrame;
 			zero_before = startFrame;
 			prev = 0;
		 	under = false;
		 	//words = [];
		 	starts = [];
		 	ends = [];
			offset = startFrame;
			this.pr_findWords(threshold, length, take_last, min, last_over, last_under,
					last_zero, zero_before, prev, border_on_zero_crossing,
 					/*current,*/ under, starts, ends, offset, localcopy, action);
		};
			//"preparing". postln;
   		if (numFrames < 1600, {
  			amt_to_request = numFrames.floor.asInt;
  		} , {
  			amt_to_request = 1600;
    		});
    		//"getn".postln;
		buf.getn (startFrame, amt_to_request, func);

 	}

 	pr_findWords { arg threshold, length, take_last = false, min = 4410, last_over, last_under,
 	 		last_zero, zero_before, prev, border_on_zero_crossing,
 			/*current,*/ under, starts, ends, offset, localcopy, action;

 	 // var last_over, last_under, current, under, starts, ends, find_end;
 	 var size, end; //this_over, prev_over;//, current;

		//"in pr_findWords".postln;
 	  if (localcopy.notNil, {

 		size = localcopy.size;
	 	 end = offset + size;

 	    localcopy.do ({ arg current, index;

 	    	  index = index + offset;

 	    	  // find zero crossings
 	    	  // best case = we land right on zero!
 	    	  if (current == 0, { last_zero = index; prev = 0;},

 	    	  	// otehrwise, if we cross zero, just fudge it
 	    	  	{ if (((current.sign != prev.sign) && (prev != 0)), {

 	    	  		// but get the one closer to zero, at least
 	    	  		if ((prev.abs < current.abs) , {

 	    	  			// we don't need to worry about negative indexes because
 	    	  			// prev is initialized to 0 and we don't excecute this code
 	    	  			// when that's the case
 	    	  			last_zero = index - 1;
 	    	  		} , {
 	    	  			last_zero = index;
 	    	  		})
 	    	  })});

 	    	  prev = current;
 	    	  current  = current.abs;

 	    	  if (under == true, {

 	    	  	if (current > threshold, {

 	    	  		under = false;
 	    	  		if ((index - last_over) > length, { //"would have worked".postln; });
 	    	  		//if ((last_zero - last_over) > length, {
 	    	  			//("start " ++ index ++ " end " ++ last_over).postln;
 	    	  			//starts = starts.add(index);
 	    	  			//ends = ends.add(last_over);

 	    	  			// ok, let's recognize that zero corssings are better
 	    	  			// but if there are no recent ones, then screw it
 	    	  			(border_on_zero_crossing && (last_over - zero_before < 150)). if ({
 	    	  				ends = ends.add(zero_before);
 	    	  			} , {
 	    	  				ends = ends.add(last_over);
 	    	  			});
 	    	  			(border_on_zero_crossing && (index - last_zero < 150)). if ({
 		    	  			starts = starts.add(last_zero);
 		    	  		} , {
 		    	  			starts = starts.add(index);
 		    	  		});
 	    	  			//last_over = index;
 	    	  			//zero_before = last_zero;
 	    	  		});
 	    	  		//skip none!
 	    	  		last_over = index;
 	    	  		zero_before = last_zero;
 	    	  	});
 	    	  }, { // under == false;

 	    	  	if (current < threshold, {

 	    	  		under = true;
 	    	  	});
 	    	  });


 	    });


 	    // if we're not at the end, then recurse, downloading the next chunk of thr buffer

 	    if ((end < (numFrames.floor.asInt - 1)), {
 	    		var func, endF;
 	    		func = {arg localC;

 	    			this.pr_findWords(threshold, length, take_last, min, last_over, last_under,
 	    			 	last_zero, zero_before, prev, border_on_zero_crossing,
 					under, starts, ends, end , localC, action);
 			};
 			if ((end + size) > endFrame, {
 				buf.getn (end.ceil.asInt, (endFrame - end).floor.asInt, func);
 			} , {
 				buf.getn(end.ceil.asInt, size.floor.asInt, func);
 			});
 		} , {

 	  		//"done getting".postln;

 	  	  if ((ends.size > 0), {
  	  	  	var last_start, last_end;

  	  	  	//"got something".postln;


	 	    //get rid of the first end, as it's the start
	 	    last_end = ends.removeAt(0);
	 	    grains = [];

 	  		//"SIZES:  YO SIZES: ".post;
			//starts.size.postln;
			//ends.size.postln;
			//starts.postln;
			//ends.postln;
			last_start = startFrame;

			starts.do ({ arg start;

				grains = grains.add(BufferTool.grain(server, buf, last_start,
					start, sampleRate, synthDefName, bufnum, amp));
				last_start = start;
			});

			if (take_last, {
				grains = grains.add(BufferTool.grain(server, buf, last_start, numFrames,
					sampleRate, synthDefName, bufnum, amp));
			});

			("% grains found\n").postf(grains.size);

 		      (action != nil).if({
  	  	  		action.value(this);
  	  	  	  });


 		   }, { "got nothing. is the buffer normalized? try changing some values".postln;});
 		});
 	   });
	}


	// get rid of grans quieter than a threshold

	trim_quiet{ |cutoff = 0.08|

		grains.do({ |grain|
			grain.rms.notNil.if({
				(grain.rms < cutoff).if({
					grains.remove(grain)
				})
			} , {
				grain.findRMS({|rms|
					(rms < cutoff).if({
						grains.remove(grain)
					})
				})
			})
		})
	}


	// granulate based on size

	calc_grains_size { arg size = 4410, max_dur = nil;
 		var start, end, total_frames, max_frames;

 		if (max_dur.isNil, {
 			max_dur = dur;
 			["dur" + dur].postln;
 		});

 		max_frames = max_dur * sampleRate;

 		total_frames = 0;
 	 	start = startFrame;
 	 	end = startFrame + size;
 	 	grains = [];
 		//grains = [grain];
 		{(end < endFrame) && (total_frames < max_frames)}.while ({
 			grains = grains.add(BufferTool.grain(server, buf, start, end, sampleRate,
 		  				synthDefName, bufnum));
 			start = start + size;
 			end = end + size;
 			total_frames = total_frames + size;
 		});

		^grains;

	}

	// granulate based on a size range

	calc_grains_size_range { arg min = 4410, max =  8820, max_dur = nil;
 		var start, end, total_frames, max_frames, diff, last_size;

 		if (max_dur == nil, {
 			max_dur = dur;
 		});

 		(max < min).if ({

 			diff = 0;
 		} , {
 			diff = max - min;
 		});

 		max_frames = max_dur * sampleRate;

 		total_frames = 0;
 	 	start = startFrame;
 	 	last_size = min + diff.rand;
 	 	end = startFrame + last_size;
 	 	grains = [];
 		//grains = [grain];
 		{(end < endFrame) && (total_frames < max_frames)}.while ({
 			grains = grains.add(BufferTool.grain(server, buf, start, end, sampleRate,
 		  				synthDefName, bufnum));
 			start = start + last_size;
 			last_size = min + diff.rand;
 			end = end + last_size;
 			total_frames = total_frames + last_size;
 		});

 		^grains;

	}

	// granulate based on duration

	calc_grains_dur { arg grain_dur;
		var size;
		size = grain_dur * sampleRate;
		^this.calc_grains_size(size);
	}

	// granulate based on a duration range

	calc_grains_dur_range { arg min_dur, max_dur;
		var min, max;
		min = min_dur * sampleRate;
		max = max_dur * sampleRate;
		^this.calc_grains_size_range(min, max);
	}

	// granulate based on the desired number of grains

	calc_grains_num { arg num, max_dur = nil;
		var size, end;
		(max_dur.notNil).if({
			end = startFrame + (max_dur * sampleRate);
			(end > endFrame).if({end = endFrame});
		} , {
			end = endFrame;
		});
		size = (end - startFrame) / num;
		^this.calc_grains_size(size, max_dur);

	}

	// granulate based on an array of durations

	calc_grains_dur_arr { arg dur_arr, /*startFrame = 0,*/ recurse = 4;


		var size, start, end, new_grain, sub_grains;

	 	start = startFrame;
	 	end = start;
	 	sub_grains = [];
	 	grains = [];

		{start < endFrame}. while({

			dur_arr.do({ arg dur;

				size = dur * sampleRate;
				end = start + size;

				(end <= endFrame).if ({
					new_grain = BufferTool.grain(server, buf, start, end, sampleRate,
 		  				synthDefName, bufnum);
 				  	grains = grains.add(new_grain);
					if ((recurse > 1 ) , {
						new_grain.calc_grains_num(recurse);
 						sub_grains = sub_grains.add(new_grain.grains);
 					} , {
 						sub_grains = sub_grains.add(new_grain);
 					});
 				});
 				start = start + size;
 			});

 		});

 		^sub_grains;

	}

	set_dur { arg new_dur;

		var size, endF;
		size = dur * sampleRate;
		endF = startFrame + size;
		if (endF <= numFrames, {
			endFrame = endF;
		});
	}

	// the following uses an object that I haven't released

	calc_scaled_grains_from_Groove { arg groove, recurse = 0;

		var size, start, end, new_grain, sub_grains, dur_arr;

	 	start = startFrame;
	 	end = start;
	 	sub_grains = [];
	 	grains = [];

	 	dur_arr = groove.scaleDurs(dur);

		/*
		dur_arr.do({ arg thisdur;

			size = thisdur * sampleRate;
			end = start + size;
			new_grain = BufferTool.grain(server, buf, start, end, sampleRate,
 		  				synthDefName, bufnum);
 		  	grains = grains.add(new_grain);
			if ((recurse > 0 ) , {
				new_grain.calc_grains_from_Groove(groove, recurse - 1);
 				sub_grains = sub_grains.add(new_grain.grains);
 			} , {
 				sub_grains = sub_grains.add(new_grain);
 			});
 			start = start + size;
 		});


 		^sub_grains;
		*/

		^this.calc_grains_dur_arr(dur_arr, recurse);
	}


	// possibly useful for a pbind?
	// These methods may not be the best way of doing things

	fixed_grain_reverse { arg grain_dur = nil;

    		var rout;

    		if (grain_dur != nil, {
    			this.calc_grains_dur(grain_dur);
    		});

    		rout = Routine.new ({
    			grains.do ({ arg word;
    				var backwards_start;
    				backwards_start = word.endFrame;
    				//dur, bufnum, startPos, grainDur, rate
    				[(word.dur - 0.0002), bufnum, backwards_start, word.dur, -1].yield;
    			});
    		 });


    		^rout;
    	}

	// possibly useful for a pbind?
	// These methods may not be the best way of doing things

    	flex_grain_reverse {arg grain_dur = 0.05;

    		var rout, size;

    		//if ((grain_dur != nil) && (grain_dur != this.grain_dur), {
    		//	this.set_grain_dur(grain_dur);
    		//});

    		size = dur * sampleRate;

    		rout = Routine.new ({
			var startPos;
			startPos = startFrame;
			[(grain_dur = 0.0002), bufnum, startPos, grain_dur, -1].yield;

			{startPos <= this.endFrame}.while({
				startPos = startPos + size;
				[(grain_dur = 0.0002), bufnum, startPos, grain_dur, -1].yield;
			});
		});

		^rout;
	}

	// possibly useful for a pbind?
	// These methods may not be the best way of doing things

    speed_up { arg rate = 1.0, grain_dur = nil;

    		var pause_dur, rout, wdur;

    		if (grain_dur != nil, {
    			this.calc_grains_dur(grain_dur);
    			//pause_dur = grain_dur / rate;
    		});



    		rout = Routine.new ({
    			grains.do ({ arg word;
    				// dur, bufnum, startPos, grainDur, rate
    				wdur = word.dur;
    				pause_dur = wdur / rate;
    				[pause_dur, bufnum, word.startFrame, wdur, 1].yield;
    			});
    		 });

     	^rout;
    	}

	// possibly useful for a pbind?
	// These methods may not be the best way of doing things

    fixed_slow_down { arg rate = 1.0, grain_dur = nil;

    		var pause_dur, repeats, rout;

    		repeats = ( 1.0 / rate).ceil;

     	if ((grain_dur != nil) && (grain_dur != this.dur), {
    			this.calc_grains_dur(grain_dur);
    		});

     	if (rate >= 1, {
     		//pause_dur = ((1.0/ rate) - this.grain_dur) / (repeats -1);
     		//that is so clearly wrong...

     		pause_dur = (((1.0 / rate) - (repeats - 1)) * this.dur) / (repeats -1);
     	}, {
     		pause_dur = this.dur;
     	});

     	rout = Routine.new ({
    			grains.do ({ arg word;
    				repeats.do ({
	    				// dur, bufnum, startPos, grainDur, rate
	    				pause_dur = (((1.0 / rate) - (repeats - 1)) * word.dur) / (repeats -1);
    					[pause_dur, bufnum, word.startFrame, word.dur, 1].yield;
    				});
    			});
    		 });

     	^rout;
    	}

	// possibly useful for a pbind?
	// These methods may not be the best way of doing things

	slow_down { arg rate = 1.0, grain_dur = nil;

    		var pause_dur, repeats, rout;

    		repeats = ( 1.0 / rate).ceil;

     	if ((grain_dur != nil) && (grain_dur != this.grain_dur), {
    			this.set_grain_dur(grain_dur);
    		});

     	if (rate <= 1, {
     		//pause_dur = ((1.0/ rate) - this.grain_dur) / (repeats -1);
     		//that is so clearly wrong...

     		pause_dur = (((1.0 / rate) - (repeats - 1)) * this.grain_dur) / (repeats -1);
     	}, {
     		pause_dur = this.grain_dur;
     	});

     	rout = Routine.new ({
    			grains.do ({ arg startPos;
    				repeats.do ({
	    				// dur, bufnum, startPos, grainDur, rate
    					[pause_dur, bufnum, startPos, this.grain_dur, 1].yield;
    				});
    			});
    		 });

     	^rout;
    	}

	// possibly useful for a pbind?
	// These methods may not be the best way of doing things

  	flex_slow_down { arg rate = 1.0, grain_dur = nil;

    		var pause_dur, repeats, rout, grain_add;

    		repeats = ( 1.0 / rate).ceil;

     	if ((grain_dur != nil) && (grain_dur != this.grain_dur), {
    			this.set_grain_dur(grain_dur);
    		});

     	if (rate <= 1, {
     		//pause_dur = ((1.0/ rate) - this.grain_dur) / (repeats -1);
     		//that is so clearly wrong...

     		pause_dur = (((1.0 / rate) - (repeats - 1)) * this.grain_dur) / (repeats -1);
     		grain_add = (pause_dur * this.sampleRate) /2;
     	}, {
     		pause_dur = this.grain_dur;
     		grain_add = this.grain_size;
     	});

     	rout = Routine.new ({
     		var startPos, grain_mark;
     		startPos = 0;

     		{startPos <= this.numFrames}. while({
    			//grains.do ({ arg startPos;
    			//	repeats.do ({
	    				// dur, bufnum, startPos, grainDur, rate
    					[pause_dur, bufnum, startPos, this.grain_dur, 1].yield;
    					startPos = startPos + grain_add;
    				//});
    			});
    		 });

     	^rout;
    	}

	// possibly useful for a pbind?
	// These methods may not be the best way of doing things

	re_word { arg foo = 0;
    		var pause_dur, rout, wdur, scrambled, size, recalc;

    		//if (grain_dur != nil, {
    		//	this.calc_grains_dur(grain_dur);
    			//pause_dur = grain_dur / rate;
    		//});

    		scrambled = grains.scramble;
    		size = grains.size;
    		//size.postln;

    		rout = Routine.new ({
    			//var word;
    			//size.do({ arg ind;
    			//	ind.postln;
    			//	word = scrambled.choose;
    			scrambled.do ({ arg word;
    				// dur, bufnum, startPos, grainDur, rate
    				wdur = word.dur;
    				pause_dur = wdur + 0.1;  // 0.02;
    				recalc = word.endFrame - word.startFrame;
    				recalc = recalc / sampleRate;
    				//(" " ++ word.startFrame ++ " " ++ word.endFrame ++ " "++
    				//	wdur ++ " " ++ recalc).postln;
    				[pause_dur, bufnum, word.startFrame, wdur, 1].yield;
    				//"and".postln;
    			});
    			"done".postln;
    		 });

     	^rout;
    	}


	// possibly useful for a pbind?
	// These methods may not be the best way of doing things

	cloud { arg density = 1;
    		var pause_dur, rout, wdur, scrambled, size, recalc;

    		//if (grain_dur != nil, {
    		//	this.calc_grains_dur(grain_dur);
    			//pause_dur = grain_dur / rate;
    		//});

    		scrambled = grains.scramble;
    		size = grains.size;
    		//size.postln;

    		rout = Routine.new ({
    			//var word;
    			//size.do({ arg ind;
    			//	ind.postln;
    			//	word = scrambled.choose;
    			scrambled.do ({ arg word;
    				// dur, bufnum, startPos, grainDur, rate
    				wdur = word.dur;
    				pause_dur = wdur / density;// + 0.1;  // 0.02;
    				//recalc = word.endFrame - word.startFrame;
    				//recalc = recalc / sampleRate;
    				//(" " ++ word.startFrame ++ " " ++ word.endFrame ++ " "++
    				//	wdur ++ " " ++ recalc).postln;
    				[(pause_dur = 0.002), bufnum, word.startFrame, wdur, 1].yield;
    				//"and".postln;
    			});
    			"done".postln;
    		 });

     	^rout;
    	}


  	free { |completionMessage|

		// de-allocate memory on the server

  		buf.free(completionMessage);
  		grains = nil;
  	}

  	save{ |filename|

  		var file, count;

  		count = 0;
  		file = File.open(filename, "w");
  		//file.write(path);
  		grains.do({|grain|
  			(grain.bufnum == this.bufnum).if({
	  			file.write("" ++ grain.startFrame ++ "\t" ++ grain.endFrame ++ "\n");
	  			count = count +1;
	  		}); // make sure we don't have stray grains from other buffers
  		});

  		file.close;
  		("% grains saved\n").postf(count);
  	}

  	saveLoud{ |filename, cutoff = 0.08|

  		var file, count, rms;

  		count = 0;
  		file = File.open(filename, "w");
  		//file.write(path);
  		grains.do({|grain|
  			rms = grain.rms;
  			rms.notNil.if({ // if you haven't bothered calculating this, you're dumb
  				(rms >= cutoff).if({
		  			(grain.bufnum == this.bufnum).if({
	  					file.write("" ++ grain.startFrame ++ "\t" ++ grain.endFrame ++ "\n");
	  					count = count+1;
	  				}); // make sure we don't have stray grains from other buffers
	  			})
	  		})
  		});

  		file.close;
  		("% grains saved\n").postf(count);
  	}

  	load{ |filename|

  		var file, data, lines, bounds;
  		file = File.open(filename, "r");

  		data = file.readAllString;
  		file.close;
  		lines = data.split($\n);

  		(lines.size > 0).if({
	  		grains = [];
  			lines.do({|grain_data|

				bounds = grain_data.split($\t);
				grains = grains.add(BufferTool.grain(server, buf, bounds.first.asInteger,
						bounds.last.asInteger, sampleRate, synthDefName, bufnum, amp));
			})
		});
		("% grains read from file\n").postf(lines.size);
	}


}
 