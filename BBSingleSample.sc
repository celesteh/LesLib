BBSingleSample {

	var <buf, max_right, >max_len, <>conductor, pbind, name, shiftarr, durarr, locator,
		micro_grains;
	
	*alloc { arg server, name, max_len = 16, max_right = 3, show=true, num_grains = 1;
	
		^super.new.init_alloc(server, name, max_len, max_right, show, num_grains);
	}
	
	*new { arg name, buf, max_len = 16, max_right = 3, show=true;

		^super.new.init(name, buf, max_len, max_right, show);
	}
	
	*grains  { arg name, buf, max_len = 16, max_right = 3, show=false;
	
		^super.new.init_grains(name, buf, max_len, max_right, show);
		
	}
	
	init_alloc {arg srv, name, max_len = 16, max_right = 3, show=true, num_grains = 1;

		var mybuf;

		mybuf = BBBufferTool.alloc(srv, /*44100 * */ max_len,1);
	
		this.init_grains(name, mybuf, max_len, max_right, show, num_grains);
	}
	
	init_grains {arg name, buf, max_len = 16, max_right = 3, show=false, num_grains = 1;
	
		var size, grain_num, grain_size;
	
		this.init(name, buf, max_len, max_right, false);
		
		(buf.grains.notNil).if ({
			size = buf.grains.size;
		} , {
			(num_grains ==1). if({
				size = 1;
			}, {
			
				grain_size = (max_len / num_grains) * buf.sampleRate;
				buf.calc_grains_size(grain_size);
				size = num_grains
			});
		});
		
		grain_num = CV.new.sp(0, 0, size - 1, 1, 'linear');
		conductor.put(\grain_num, grain_num);
		this.conductor.gui.keys = this.conductor.gui.keys ++ \grain_num;
		if (show, {conductor.show});

		
	}
	
	init {arg na, bu, len, right, show=false;
	
		max_len = len;
		max_right = right;
		name = na;
		buf = bu;
		
		//locator = Locator.new(max_right);
		
		shiftarr = [64/45, 64/63, 16/9, 10/7, 12/7, 8/7, 6/5, 8/5, 4/3, 1, 
					3/2, 5/4, 5/3, 7/4, 7/6, 7/5, 9/8, 63/32, 45/32];
					
		durarr = [1/8, 1/4, 1/2, 3/4, 1, 3/2, 2, 4];

		conductor = Conductor.make({ arg thisc, shift, ratio, amp, dur, pan, mask; //, xlocation, ylocation;
		
			

			shift.sp(6, 0, shiftarr.size - 1, 1, 'linear');
			shift.input_(0.5);
			ratio.spec_(\unipolar);
			ratio.value_(0.5);
			amp.spec_(\amp);
			amp.value_(0.9);
			pan.spec_(\pan);
			//xlocation.sp(max_right / 2, 0, max_right, 0, 'linear');
			//ylocation.sp(max_right / 2, 1, max_right, 0, 'linear');
			dur.sp(4, 0, durarr.size - 1, 1, 'linear');
			mask.sp(23, 0, 23, 1, 'linear');
			
			thisc.name_(na);
			//this.guiItems_([shift, ratio, amp, pan, location]);
			thisc.synth_( (), [freq: 440, out: 3] );
			
			thisc.useInterpolator;
			//thisc.interpKeys_( [shift, ratio]);
			//thisc.presetKeys_( [shift, ratio]);
			thisc[\preset].interpItems_([shift, ratio]);
			
		});
		//conductor.useInterpolator;
		
		if (show, {conductor.show});
		
	}
	
	shift {
		
		//^shiftarr.wrapAt(conductor[\shift].value);
		^conductor[\shift];
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
	
	mask {
	
		^conductor[\mask];
	}
	
	mask_val {
	
		^conductor[\mask].value;
	}

	/*
	xlocation {
		
		^conductor[\xlocation];
	}
	
	xlocation_val {
	
		^conductor[\xlocation].value;
	}
	
	ylocation {
		
		^conductor[\ylocation];
	}
	
	ylocation_val {
	
		^conductor[\ylocation].value;
	}
	*/
	
	dur {
		
		^durarr.wrapAt(conductor[\dur].value);
	}
	
	bufnum {
	
		^buf.bufnum
	}
	
	dur_val {
		
		^durarr.wrapAt(conductor[\dur].value);
	}
	
	grain_num {
	
		^conductor[\grain_num];
	}
	
	grain_num_val {
	
		var num;
		
		num = conductor[\grain_num];
		
		if (num.notNil, { num = num.value; });
		
		^num;
	}
	
	grain {
	
		var num, grain;
		
		num = conductor[\grain_num];
		
		if (num.notNil, { 
			 num = num.value; 
			 grain = buf.grains[num];
		}, {grain = nil });
		
		^grain;
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
				this.amp_val, "dur", this.dur_val, "pan", this.pan_val, 
				"location", this.xlocation_val, this.ylocation_val];
		stat.postln;
		^stat;
	}

	play_pbind_loc { arg instrument, timer;


	
		pbind = Ptpar([timer.tempo, Pbind (
			
			\instrument,	instrument,
			\bufnum,		buf.bufnum,
			\dur,		Pfunc({timer.phrase_len * this.dur_val}),
			\amp,		this.amp,
			\startFrame,	0,
			\interval,	this.shift,
			\ratio, 		this.ratio,
			[\signal_amp, \shift_amp], Pfunc({arg evt;
										var rat;
										rat = evt.at(\ratio);
										evt.postln;
										[rat, 1 - rat]}),
	
			[\left_distsance, \left_delay_time, \left_amp,
				\right_distance, \right_delay_time, \right_amp],
									Pfunc({ arg evt;
										locator.get_stereo_information(
												this.xlocation_val, this.ylocation_val,
												evt.at(\amp));
									})
		)]);
		Routine({pbind = pbind.play}).play(timer.tempoclock);
	}
	
	play_pbind_pan { arg instrument, timer, multiple = 1;
		pbind = Ptpar([/*timer.tempo*/ 0, Pbind (
			
			\instrument,	instrument,
			\bufnum,		buf.bufnum,
			\dur,		Pfunc({timer.phrase_len * this.dur_val * multiple}),
			\amp,		this.amp,
			\startFrame,	0,
			\interval,	this.shift,
			\ratio, 		this.ratio,
			\pan,		this.pan,
			[\signal_amp, \shift_amp], Pfunc({arg evt;
										var rat;
										rat = evt.at(\ratio);
										evt.postln;
										[rat, 1 - rat]})
	
											
		)]);
		Routine({pbind = pbind.play}).play(timer.tempoclock);
	}
	
	stop_pbind {

		(pbind.notNil).if ({
			pbind.stop;
			pbind = nil;
		});
	}
	
	play_once_loc { arg instrument, timer;
		/*
		locator.compute_stereo_information(this.xlocation_val, this.ylocation_val, this.amp_val);
	
		Routine({
			Synth(instrument, [\bufnum, buf.bufnum, \startFrame, 0, 
					\dur, timer.phrase_len * this.dur_val, \left_amp, locator.left_amp,
					\right_amp, locator.right_amp, \left_delay_time, 
					locator.left_delay_time]);
		}).play(timer.tempoclock);
		*/
		play_once_grain_loc(instrument, timer, 0);
	}
	
	play_once_pan { arg instrument, timer;
		/*
	
		Routine({
			Synth(instrument, [\bufnum, buf.bufnum, \startFrame, 0, 
					\dur, timer.phrase_len * this.dur_val, \amp, this.amp_val,
					\pan, this.pan_val]);
		}).play(timer.tempoclock);
		*/
		play_once_grain_pan(instrument, timer, 0);
	}
	

	play_once_grain_loc { arg instrument, timer, startframe;
	
		locator.compute_stereo_information(this.xlocation_val, this.ylocation_val, this.amp_val);

		(startframe.isNil).if ({ startframe = buf.startFrame});
	
		Routine({
			Synth(instrument, [\bufnum, buf.bufnum, \startFrame, 0, 
					\dur, timer.phrase_len * this.dur_val, \left_amp, locator.left_amp,
					\right_amp, locator.right_amp, \left_delay_time, 
					locator.left_delay_time, \startFrame, startframe]);
		}).play(timer.tempoclock);
	}
	
	play_once_grain_pan { arg instrument, timer, startframe;
	
		(startframe.isNil).if ({ startframe = buf.startFrame});
		
		Routine({
			Synth(instrument, [\bufnum, buf.bufnum, \startFrame, 0, 
					\dur, timer.phrase_len * this.dur_val, \amp, this.amp_val,
					\pan, this.pan_val, \startFrame, startframe]);
		}).play(timer.tempoclock);
	}

	show { arg argName, x, y, w, h;
	
		conductor.show();
		
	}
	
	front {
	
		conductor.gui[\win].front;
	}
	
	calc_grains_dur_arr { |dur_arr|
	
		micro_grains = buf.calc_grains_dur_arr(dur_arr);
	}
					
}
	