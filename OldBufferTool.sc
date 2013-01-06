OldBufferTool
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
	var <dur;
	var <grains; 
	var <avgPitch;
	var <>pitch;
	//var <>grain_size, <>num_grains, <>grain_dur;
	var <numChannels;
	var <>synthDefName;
	var <>recSynthDefName;
 	var <buf;
 	var server;
 	
 	
 	
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
 	
 	
 	
 	*grain { arg srv, buffer, startFrame, endFrame, sampleRate, synthDefName, bufnum;
 	
 		^super.new.grain_init(srv, buffer, startFrame, endFrame, sampleRate, 
 							synthDefName, bufnum);
 	}
 	
 	
 	*new { arg srv, in, dur,  channels = 2, sampleRate = 44100, action;
 	
 		^super.new.rec_init(srv, in, dur, channels, sampleRate, action);
 	}
 
 	*open { arg srv, name, threshold = 0, length = 0, take_last = false, min = 4410, action;
 
 		^super.new.open_init(name, srv, action);
 	}
 	
 	grain_init { arg srv, buffer, start_frame, end_frame, sample_rate, synthDef_name, buf_num;
 	
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
	 	
 		numFrames = endFrame - startFrame;
 		dur = numFrames / sampleRate;
 	}
 	
 	rec_init { arg srv, in, bufdur,  channels = 2, sample_rate = 44100, action;
 	
 		server = srv;
 		dur = bufdur;
 		numChannels = channels;
 		sampleRate = sample_rate;
 		numFrames = dur * sampleRate;
 		//("server " ++ server ++ " dur " ++ dur ++ " sampleRate " ++ sampleRate
 		//	++ " numFrames " ++ numFrames ++ " numChannels " ++ numChannels).postln;
 		buf = Buffer.alloc(srv, numFrames, channels);
 		bufnum = buf.bufnum;
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
 		
 		if (action.notNil, {
			action.value;
		});

 	}
 
 	open_init { arg name, srv, action;
 	
 		// then read the file as a buffer ot the server
 		
 		server = srv;
 		buf = Buffer.read(srv, name, action:{
 		
 			var starts, ends;
 			
 			starts = [];
 			ends = [];
 			
	 		sampleRate = buf.sampleRate;
 			numChannels = buf.numChannels;
 			numFrames = buf.numFrames;
 			startFrame = 0;
 			endFrame = numFrames;
	 		dur = numFrames / sampleRate;
	 		
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
 	
 	
 	
 	findPitch{ arg threshold = 0.025, action;
 	
 		var amt_to_request, func;
 		
 		threshold = threshold.abs;
 		
 		func = { arg localCopy;
 		
 			var crossings, positive, length;
 			crossings = 0;
 			positive = true;
 			length = 0;
 			
 			this.doPitch(localCopy, length, crossings, positive, threshold, action);
  		};
  		if (numFrames < 1600, {
  			amt_to_request = numFrames.floor.asInt;
  		} , {
  			amt_to_request = 1600;
    		});
    		
 		buf.getn(startFrame.ceil.asInt, amt_to_request, func);

 	}
 	
 	doPitch { arg localCopy, length, crossings, positive, threshold, action;
 	
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
 				
 					this.doPitch(localC, length, crossings, positive, threshold, action);
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
		

 	prepareWords {arg threshold, length, take_last = false, min = 4410, action;
		var func, amt_to_request;
		
		func = { arg localcopy;
			var last_over, last_under, current, under, starts, ends, offset;
			current = startFrame;
		 	last_over = startFrame;
 			last_under =startFrame;
		 	under = false;
		 	//words = [];
		 	starts = [];
		 	ends = [];
			offset = startFrame;
			this.findWords(threshold, length, take_last, min, last_over, last_under,
 					/*current,*/ under, starts, ends, offset, localcopy, action);
		};
			//"preparing". postln;
   		if (numFrames < 1600, {
  			amt_to_request = numFrames.floor.asInt;
  		} , {
  			amt_to_request = 1600;
    		});
		buf.getn (startFrame, amt_to_request, func);

 	}
 	
 	findWords { arg threshold, length, take_last = false, min = 4410, last_over, last_under,
 			/*current,*/ under, starts, ends, offset, localcopy, action;
 	
 	 // var last_over, last_under, current, under, starts, ends, find_end;
 	 var size, end; //this_over, prev_over;//, current;
 	 

 	  if (localcopy != nil, {
 	  
 		size = localcopy.size;
	 	 end = offset + size;

 	    localcopy.do ({ arg current, index;
 	    
 	    	  current  = current.abs;
 	    	  index = index + offset;
 	    	  
 	    	  
 	    	  if (under == true, {
 	    	  
 	    	  	if (current > threshold, {
 	    	  
 	    	  		under = false;
 	    	  		if ((index - last_over) > length, {
 	    	  			//("start " ++ index ++ " end " ++ last_over).postln;
 	    	  			starts = starts.add(index);
 	    	  			ends = ends.add(last_over);
 	    	  		});
 	    	  		last_over = index;
 	    	  	});
 	    	  }, { // under = false;
 	    	  
 	    	  	if (current < threshold, {
 	    	  	
 	    	  		under = true;
 	    	  	});
 	    	  });
 	    	  
 	    	  
 	    });
 	    
 	    if ((end < (numFrames.floor.asInt - 1)), {
 	    		var func, endF;
 	    		func = {arg localC;
 	    		
 	    			this.findWords(threshold, length, take_last, min, last_over, last_under,
 					under, starts, ends, end , localC, action);
 			};
 			if ((end + size) > endFrame, {
 				buf.getn (end.ceil.asInt, (endFrame - end).floor.asInt, func);
 			} , {
 				buf.getn(end.ceil.asInt, size.floor.asInt, func);
 			});
 		} , {
 	  
 	  	  if ((ends.size > 0), {  
  	  	  	var last_start, last_end;
  	  	  	
  	  	  	"got something".postln;
  	  	  	
 	  
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
			
				grains = grains.add(BufferTool.grain(server, buf, last_start, start, sampleRate,
					synthDefName, bufnum));
				last_start = start;
			});
			
			if (take_last, {
				grains = grains.add(BufferTool.grain(server, buf, last_start, numFrames, 
					sampleRate, synthDefName, bufnum));
			});
			
 		     
 		      (action != nil).if({
  	  	  		action.value(this);
  	  	  	  });

 		     
 		   }); 
 		});
 	   });
	}


	calc_grains_size { arg size = 4410, max_dur = nil;
 		var start, end, total_frames, max_frames;
 	
 		if (max_dur == nil, {
 			max_dur = dur;
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

	}

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

	}
	
	calc_grains_dur { arg grain_dur;
		var size;
		size = grain_dur * sampleRate;
		this.calc_grains_size(size);
	}
	
	calc_grains_dur_range { arg min_dur, max_dur;
		var min, max;
		min = min_dur * sampleRate;
		max = max_dur * sampleRate;
		this.calc_grains_size_range(min, max);
	}


	calc_grains_num { arg num, max_dur = nil;
		var size, end;
		(max_dur.notNil).if({
			end = startFrame + (max_dur * sampleRate);
			(end > endFrame).if({end = endFrame});
		} , {
			end = endFrame;
		}); 
		size = (end - startFrame) / num;
		this.calc_grains_size(size, max_dur);
		
	}
	
	
	set_dur { arg new_dur;
	
		var size, endF;
		size = dur * sampleRate;
		endF = startFrame + size;
		if (endF <= numFrames, {
			endFrame = endF;
		});
	}
	
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


	re_word { arg fuck = 0;
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
	
      
	  	
  	free {

		// de-allocate memory on the server
  	
  		buf.free;
  		grains = nil;
  	}

}
 