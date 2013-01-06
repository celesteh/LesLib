BBSingleSamplePan {

	var <buf, >max_len, <conductor, pbind, name, shiftarr, durarr;
	
	*alloc { arg server, name, max_len = 16;
	
		^super.new.init_alloc(server, name, max_len);
	}
	
	*new { arg name, buf, max_len = 16;

		^super.new.init(name, buf, max_len);
	}
	
	init_alloc {arg srv, name, max_len = 16;

		var mybuf;

		mybuf = BBCutBuffer.alloc(srv, /*44100 * */ max_len,1);
	
		this.init(name, mybuf, max_len);
	}
	
	init {arg na, bu, len;
	
		max_len = len;
		name = na;
		buf = bu;
		
		shiftarr = [64/45, 64/63, 16/9, 10/7, 12/7, 8/7, 6/5, 8/5, 4/3, 1, 
					3/2, 5/4, 5/3, 7/4, 7/6, 7/5, 9/8, 63/32, 45/32];
					
		durarr = [1/8, 1/4, 1/2, 3/4, 1, 3/2, 2, 4];

		conductor = Conductor.make({ arg thisc, shift, ratio, amp, dur, pan;
		
			

			shift.sp(6, 0, shiftarr.size - 1, 1, 'linear');
			shift.input_(0.5);
			ratio.spec_(\unipolar);
			ratio.value_(0.5);
			amp.spec_(\amp);
			amp.value_(0.9);
			pan.spec_(\pan);
			dur.sp(4, 0, durarr.size - 1, 1, 'linear');
			
			thisc.name_(na);
			//this.guiItems_([shift, ratio, amp, pan, location]);
			//thisc.synth_( (), [freq: 440, out: 3] );
			thisc.action_({this.play_pbind}, 
						{this.stop_pbind}, 
						{if (pbind.notNil, {pbind.pause})}, 
						{if (pbind.notNil, {pbind.resume})});
			
		});
		
		//conductor.show;
		
	}
	
	shift {
		
		^shiftarr.wrapAt(conductor[\shift].value);
	}
	
	shift_val {
	
		^shiftarr.wrapAt(conductor[\shift].value);
	}

	ratio {
		
		^conductor[\ratio];
	}
	
	ratio_val {
	
		^conductor[\ratio].value;
	}

	amp {
		
		^conductor[\amp];
	}
	
	amp_val {
	
		^conductor[\amp].value;
	}

	pan {
		
		^conductor[\pan];
	}
	
	pan_val {
	
		^conductor[\pan].value;
	}
	
	dur {
		
		^durarr.wrapAt(conductor[\dur].value);
	}
	
	dur_val {
		
		^durarr.wrapAt(conductor[\dur].value);
	}

	isPlaying {
	
		^pbind.notNil;
	}

	show_status {
	
		var text, stat;
		
		(this.isPlaying) .if ({
			text = "play";
		} , { text = "stop";
		});
	
		stat = [name, text, "shift", this.shift_val, "ratio", this.ratio_val, "amp", 
				this.amp_val, "dur", this.dur_val, "pan", this.pan_val];
		stat.postln;
		^stat;
	}

	
	play_pbind { arg instrument, timer, out = 0, group;
		pbind = Ptpar([timer.tempo, Pbind (
			
			//\instrument,	Pfunc({instrument}),
			\instrument,	Pfunc({ \PlayShift }),
			\bufnum,		buf.bufnum,
			\dur,		Pfunc({timer.phrase_len * this.dur_val}),
			\amp,		Pfunc({this.amp.value}),
			\startFrame,	0,
			\interval,	Pfunc({this.shift.value}),
			\ratio, 		Pfunc({this.ratio.value}),
			\pan,		Pfunc({this.pan.value}),
			//\pan,		this.pan,
			\out,		out,
			\group,		group,
			[\signal_amp, \shift_amp], Pfunc({arg evt;
										var rat;
										rat = evt.at(\ratio);
										//evt.postln;
										[rat, 1 - rat.value]})//,
			//\foo,		Pfunc({arg evt; evt.postln;})
	
											
		)]);
		Routine({pbind = pbind.play}).play(timer.tempoclock);
	}
	
	stop_pbind {

		(pbind.notNil).if ({
			pbind.stop;
			pbind = nil;
		});
	}
	
	
	play_once { arg instrument, timer;
	
			Routine({
			Synth(instrument, [\bufnum, buf.bufnum, \startFrame, 0, 
					\dur, timer.phrase_len * this.dur_val, \amp, this.amp_val,
					\pan, this.pan_val]);
		}).play(timer.tempoclock);
	}
	
					
}
	