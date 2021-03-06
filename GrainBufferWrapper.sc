GrainBufferWrapper
{

  // a class to hold a data about a buffer, including the bufnum on the server
  // the number of frames and the samplerate

	var <bufnum;
	var <sampleRate = 44100;
	var <numFrames = -1;
	var <dur;
	var <grains; 
	var <>grain_size, <>num_grains, <>grain_dur;
	var <numChannels;
	var <synthDefName;
 	var buf;
 	var server;
 
 	*new { arg srv, name;
 
 		^super.new.init(name, srv);
 	}
 
 	init { arg name, srv;
 	
 		var soundFile, sdef; 		
 		
 		// get the header information by opneing a soundfile
 		soundFile = SoundFile.new;
 		soundFile.openRead(name);
 		sampleRate = soundFile.sampleRate;
 		numFrames = soundFile.numFrames;
         numChannels = soundFile.numChannels;
 		soundFile.close;
 		
 		dur = numFrames / sampleRate;
 		// then read the file as a buffer ot the server
 		
 		server = srv;
 		buf = Buffer.read(srv, name);
 		
 		bufnum = buf.bufnum;
 		
 		numChannels.postln;
 		/*
 		if ( numChannels == 1, {
 		
	 		// make sure we have synthdefs for buffers
 			synthDefName = "monoGrainBufferSynthDef";
 			sdef = SynthDef( "monoGrainBufferSynthDef", { arg out = 0, bufnum = 0, xPan = 0, 
 									yPan = 0, amp =1,startFrame = 0, grainDur = 1, rate = 1;
 									
 				var env, speed, player, panner;
 			
 				env = EnvGen.kr(Env.linen(0.001, (grainDur - 0.002), 0.001, 1), doneAction:2);
 				//env = EnvGen.kr(Env.linen(0.0001, (grainDur - 0.0002), 0.0001, 1), doneAction:2);
				speed = rate *  BufRateScale.kr(bufnum);
				player = PlayBuf.ar(1, bufnum, speed, startPos: startFrame);
				panner = Pan4.ar(player * env, xPan, yPan, amp);
				Out.ar(out, panner);
			}).writeDefFile;
			//sdef.load(srv);
			//sdef.send(srv);

		}, { if (numChannels == 2, {
 				synthDefName = "stereoGrainBufferSynthDef";
 				sdef = SynthDef( "stereoGrainBufferSynthDef", { arg out = 0, bufnum = 0, 
 									xPan = 0, yPan = 0,
									 amp =1, startFrame = 0, grainDur = 1, rate = 1;
 									
 				  var env, speed, player, panner;
 			
 				  env = EnvGen.kr(Env.linen(0.001, (grainDur - 0.002), 0.001, 1), doneAction:2);
				  speed = rate *  BufRateScale.kr(bufnum);
				  player = PlayBuf.ar(2, bufnum, speed, startPos: startFrame);
				  panner = Pan4.ar(player * env, xPan, yPan, amp);
				  Out.ar(out, panner);
				}).writeDefFile;
				//sdef.load(srv);
				//sdef.send(srv);


			});
		});*/
			
 	}
 	
 	
 	re_calc_grains {
 		var grain;
 	
 	 	grain = 0;
 		grains = [grain];
 		{grain < numFrames}.while ({
 			grain = grain + grain_size;
 			grains = grains.add(grain);
 		});
 	
 	}
 	
 	set_grain_size { arg grain_size;
 	
 		this.grain_size = grain_size;
 		num_grains = numFrames / grain_size;
  		grain_dur = grain_size / sampleRate;
  		this.re_calc_grains;	
     } 
     
     set_num_grains { arg num_grains;
     
     	this.num_grains = num_grains;
     	grain_size = numFrames /num_grains;
  		grain_dur = grain_size / sampleRate;
     	this.re_calc_grains;
     }
     
     
     set_grain_dur { arg grain_dur;
     
     	this.grain_dur = grain_dur;
     	num_grains = dur / grain_dur;
     	grain_size = numFrames /num_grains;
      	this.re_calc_grains;
     }
    
    	fixed_grain_reverse { arg grain_dur = nil;
    	
    		var rout;
    		
    		if ((grain_dur != nil) && (grain_dur != this.grain_dur), {
    			this.set_grain_dur(grain_dur);
    		});
    		
    		rout = Routine.new ({
    			grains.do ({ arg startPos;
    				var backwards_start;
    				backwards_start = startPos + this.grain_size;
    				//dur, bufnum, startPos, grainDur, rate
    				[(this.grain_dur - 0.0002), bufnum, backwards_start, this.grain_dur, -1].yield;
    			});
    		 });

    		
    		^rout;
    	}
    	
    	flex_grain_reverse {arg grain_dur = nil;
    	
    		var rout;
    		
    		if ((grain_dur != nil) && (grain_dur != this.grain_dur), {
    			this.set_grain_dur(grain_dur);
    		});
    		
    		rout = Routine.new ({
			var startPos = 0;
			[(this.grain_dur = 0.0002), bufnum, 0, this.grain_dur, -1].yield;
			
			{startPos <= this.numFrames}.while({
				startPos = startPos + this.grain_size;
				[(this.grain_dur = 0.0002), bufnum, 0, this.grain_dur, -1].yield;
			});
		});
		
		^rout;
	}
	
/*	
	formula_reverse {  arg func, grain_dur = nil;
    		var rout;
    		
    		if ((grain_dur != nil) && (grain_dur != this.grain_dur), {
    			this.set_grain_dur(grain_dur);
    		});
    		
    		rout = Routine.new ({
			var startPos = 0;
			[(this.grain_dur = 0.0002), bufnum, 0, this.grain_dur, -1].yield;
			
			{startPos <= this.numFrames}.while({
				startPos = func.value(startPos);    					[(this.grain_dur = 0.0002), bufnum, 0, this.grain_dur, -1].yield;		
			});
		});
		
		^rout;
	}


    	
    	play_tweaked { arg grain_dur = nil;
    	
    		var rout;
    		
    		if ((grain_dur != nil) && (grain_dur != this.grain_dur), {
    			this.set_grain_dur(grain_dur);
    		});
    		
    		rout = Routine.new ({
    			grains.do ({ arg startPos;
    				//dur, bufnum, startPos, grainDur, rate
    				[this.grain_dur, bufnum, startPos, this.grain_dur, -1].yield;
    			});
    		 });

    		
    		^rout;
    	}
    	
    */
    	
    speed_up { arg rate = 1.0, grain_dur = nil;
    	
    		var pause_dur, rout;
    	
     	if ((grain_dur != nil) && (grain_dur != this.grain_dur), {
    			this.set_grain_dur(grain_dur);
    		});
    		
    		pause_dur = this.grain_dur / rate;
    		
    		rout = Routine.new ({
    			grains.do ({ arg startPos;
    				// dur, bufnum, startPos, grainDur, rate
    				[pause_dur, bufnum, startPos, this.grain_dur, 1].yield;
    			});
    		 });
    		
     	^rout;
    	}
  	
  	fixed_slow_down { arg rate = 1.0, grain_dur = nil;

    		var pause_dur, repeats, rout;
    		
    		repeats = ( 1.0 / rate).ceil;
    	
     	if ((grain_dur != nil) && (grain_dur != this.grain_dur), {
    			this.set_grain_dur(grain_dur);
    		});
     	
     	if (rate >= 1, {
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
    	
    	
    	cloud { arg density = 1, grain_dur = nil, rate = 1;
    	
    		var rout, pause_dur, dur;
    		
    		if ((grain_dur != nil) && (grain_dur != this.grain_dur), {
    			this.set_grain_dur(grain_dur);
    		});
    		
    		dur = this.grain_dur * (rate.abs);
    		pause_dur = dur / density;
    		
    		rout = Routine.new ({
    			inf.do({
    				var startPos;
    				
    				startPos = this.grains.choose;

				[pause_dur, bufnum, startPos, dur, rate].yield;
			});
		});
		
		^rout;
	}
	
	cloud_once {arg density = 1, grain_dur = nil, rate = 1;
	
	    	var rout, pause_dur, dur;
    		
    		if ((grain_dur != nil) && (grain_dur != this.grain_dur), {
    			this.set_grain_dur(grain_dur);
    		});
    		
    		dur = this.grain_dur * (rate.abs);
    		pause_dur = dur / density;

    		rout = Routine.new ({
    			grains.scramble.do ({ arg startPos;
    				//dur, bufnum, startPos, grainDur, rate
    				[(pause_dur - 0.0002), bufnum, startPos, dur, rate].yield;
    			});
    		 });

    		
    		^rout;
    	}
    	
    	cloud_range { arg grains_in_range= 30, start = 0, end, step = 1, stutter = 1, 
    						density = 1, rate = 1;
    	
    		var rout, pause_dur, dur;
    		
    		if (end == nil, { end = this.num_grains;});

    		dur = this.grain_dur * (rate.abs);
    		pause_dur = dur / density;
    		
    		rout = Routine.new({
    			var current, arr, startPos;
    			current = start;
    		
    			{current < end}.while({
    				
    				arr = this.grains.copyRange(current, (current + grains_in_range));
    				stutter.do({
    					startPos = arr.choose;
    					[(pause_dur - 0.0002), bufnum, startPos, dur, rate].yield;
    				});
    				current = current + step;
    			});
    		});
    		
    		^rout;
    	}

  	
  	free {

		// de-allocate memory on the server
  	
  		buf.free;
  	}

}
 